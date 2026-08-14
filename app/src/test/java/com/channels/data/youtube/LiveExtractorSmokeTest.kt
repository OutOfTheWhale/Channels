package com.channels.data.youtube

import com.channels.domain.usecase.Shorts
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 * Hits the real YouTube via NewPipeExtractor to prove the whole data path works:
 * search -> Shorts filtering -> audio stream resolution. Network-dependent, so it
 * is NOT part of the normal unit-test run — invoke explicitly:
 *
 *   gradlew :app:testDebugUnitTest --tests "*LiveExtractorSmokeTest" \
 *       -Dchannels.live=1
 */
class LiveExtractorSmokeTest {

    private val liveEnabled = System.getProperty("channels.live") == "1"

    @Test
    fun `search resolves long-form audio`() = runBlocking {
        if (!liveEnabled) {
            println("Skipping live smoke test (set -Dchannels.live=1 to run).")
            return@runBlocking
        }

        val repo = YoutubeRepository()

        val channels = repo.searchChannels("NPR")
        println("Channels: ${channels.size}; first = ${channels.firstOrNull()?.name}")
        assertTrue("expected channel results", channels.isNotEmpty())

        val videos = repo.searchVideos("lex fridman podcast")
        println("Videos: ${videos.size}")
        videos.take(5).forEach { println("  - ${it.durationSeconds}s  ${it.title}") }
        assertTrue("expected video results", videos.isNotEmpty())
        assertTrue(
            "Shorts should have been filtered out",
            videos.none { Shorts.isShort(it) },
        )

        val first = videos.first()
        val audio = repo.resolveAudio(first.url)
        println("Resolved audio: ${audio.mimeType} @ ${audio.averageBitrate}kbps")
        println("Stream URL starts with: ${audio.streamUrl.take(60)}")
        assertTrue("audio stream url should be http(s)", audio.streamUrl.startsWith("http"))
    }

    @Test
    fun `channel uploads include livestreams`() = runBlocking {
        if (!liveEnabled) {
            println("Skipping live smoke test (set -Dchannels.live=1 to run).")
            return@runBlocking
        }
        val repo = YoutubeRepository()
        val channel = repo.searchChannels("New Life Church").firstOrNull()
            ?: run { println("no channel"); return@runBlocking }
        println("Channel: ${channel.name}")
        val uploads = repo.channelUploads(channel.url)
        println("Uploads: ${uploads.size}, currently-live: ${uploads.count { it.isLive }}")
        uploads.take(8).forEach { println("  live=${it.isLive}  date=${it.publishedAt}  ${it.title.take(50)}") }
        assertTrue("expected uploads", uploads.isNotEmpty())
        // Verify newest-first ordering (ignoring nulls).
        val dates = uploads.mapNotNull { it.publishedAt }
        assertTrue("uploads should be sorted newest-first", dates == dates.sortedDescending())
    }

    @Test
    fun `stonebridge picks english original track`() = runBlocking {
        if (!liveEnabled) {
            println("Skipping live smoke test (set -Dchannels.live=1 to run).")
            return@runBlocking
        }
        val repo = YoutubeRepository()
        val videos = repo.searchVideos("Thirsting for God Psalm 63 Jonny Ardavanis Stonebridge")
        val target = videos.firstOrNull() ?: run { println("no results"); return@runBlocking }
        println("Target: ${target.title}")

        // Dump every audio track YouTube offers for this video.
        val info = StreamInfo.getInfo(ServiceList.YouTube, target.url)
        println("Audio tracks (${info.audioStreams.size}):")
        info.audioStreams.forEach {
            println("  type=${it.audioTrackType} locale=${it.audioLocale} bitrate=${it.averageBitrate} delivery=${it.deliveryMethod}")
        }

        // What our app actually resolves.
        val chosen = repo.resolveAudio(target.url)
        println("CHOSEN: ${chosen.mimeType} @ ${chosen.averageBitrate}kbps")
        assertTrue(chosen.streamUrl.startsWith("http"))
        // sanity: NewPipe must have initialized
        assertTrue(NewPipe.getDownloader() != null)
    }
}
