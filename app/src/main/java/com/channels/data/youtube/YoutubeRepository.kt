package com.channels.data.youtube

import com.channels.domain.model.AudioTrack
import com.channels.domain.model.ChannelItem
import com.channels.domain.model.VideoItem
import com.channels.domain.usecase.Shorts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

/**
 * The app's single gateway to YouTube data via NewPipeExtractor. All calls are
 * suspend + run on IO. Shorts are filtered centrally through [Shorts] so callers
 * never have to think about it.
 */
class YoutubeRepository {

    private val youtube get() = ServiceList.YouTube

    /** Search channels by name/keyword. */
    suspend fun searchChannels(query: String): List<ChannelItem> = withContext(Dispatchers.IO) {
        ensureInitialized()
        val extractor = youtube.getSearchExtractor(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.CHANNELS),
            "",
        )
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<ChannelInfoItem>()
            .map { it.toChannelItem() }
    }

    /** Search long-form videos (Shorts removed). */
    suspend fun searchVideos(query: String, includeShorts: Boolean = false): List<VideoItem> =
        withContext(Dispatchers.IO) {
            ensureInitialized()
            val extractor = youtube.getSearchExtractor(
                query,
                listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
                "",
            )
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .map { it.toVideoItem() }
                .filterShorts(includeShorts)
        }

    /** Fetch channel metadata (for storing a star). */
    suspend fun getChannel(channelUrl: String): ChannelItem = withContext(Dispatchers.IO) {
        ensureInitialized()
        val info = ChannelInfo.getInfo(youtube, channelUrl)
        ChannelItem(
            url = info.url,
            name = info.name,
            thumbnailUrl = bestImage(info.avatars),
            subscriberCount = info.subscriberCount,
            description = info.description,
        )
    }

    /** A channel's recent uploads and livestreams (long-form only by default). */
    suspend fun channelUploads(channelUrl: String, includeShorts: Boolean = false): List<VideoItem> =
        withContext(Dispatchers.IO) {
            ensureInitialized()
            val info = ChannelInfo.getInfo(youtube, channelUrl)
            // Regular uploads live in the Videos tab; current/past live streams have their
            // own Livestreams tab — pull both so live services show up too.
            val tabs: List<ListLinkHandler> = info.tabs.filter {
                it.contentFilters.contains(ChannelTabs.VIDEOS) ||
                    it.contentFilters.contains(ChannelTabs.LIVESTREAMS)
            }
            tabs
                .flatMap { tab ->
                    runCatching { ChannelTabInfo.getInfo(youtube, tab).relatedItems }
                        .getOrDefault(emptyList())
                }
                .filterIsInstance<StreamInfoItem>()
                .map { it.toVideoItem() }
                .distinctBy { it.url }
                .filterShorts(includeShorts)
                // Newest first, so recent livestreams from the Livestreams tab aren't buried
                // below every regular upload — and so they make it into the Home feed, which
                // only keeps each channel's most-recent items. Sort purely by date: NewPipe
                // marks ended streams as "live" too, so we must not pin on isLive.
                .sortedByDescending { it.publishedAt ?: Long.MIN_VALUE }
        }

    /** Resolve a directly-playable audio stream for a video, or the live manifest for a live stream. */
    suspend fun resolveAudio(videoUrl: String): AudioTrack = withContext(Dispatchers.IO) {
        ensureInitialized()
        val info = StreamInfo.getInfo(youtube, videoUrl)
        val audio = pickBestAudio(info.audioStreams)

        // Non-live videos expose audio-only streams. Live streams don't — they're delivered
        // as an HLS or DASH manifest, which ExoPlayer plays with the media3 hls/dash modules.
        val (streamUrl, mimeType, bitrate) = when {
            audio != null -> Triple(audio.content, audio.format?.mimeType, audio.averageBitrate)
            info.hlsUrl.isNotBlank() -> Triple(info.hlsUrl, MIME_HLS, 0)
            info.dashMpdUrl.isNotBlank() -> Triple(info.dashMpdUrl, MIME_DASH, 0)
            else -> error("No audio stream available for $videoUrl")
        }
        AudioTrack(
            videoUrl = info.url,
            title = info.name,
            uploader = info.uploaderName ?: "",
            uploaderUrl = info.uploaderUrl,
            durationSeconds = info.duration,
            thumbnailUrl = bestImage(info.thumbnails),
            streamUrl = streamUrl,
            mimeType = mimeType,
            averageBitrate = bitrate,
        )
    }

    // --- mapping helpers ---

    private fun StreamInfoItem.toVideoItem() = VideoItem(
        id = url.substringAfterLast("v=", url.substringAfterLast('/')),
        url = url,
        title = name,
        uploader = uploaderName ?: "",
        uploaderUrl = uploaderUrl,
        durationSeconds = duration,
        thumbnailUrl = bestImage(thumbnails),
        isLive = streamType == StreamType.LIVE_STREAM || streamType == StreamType.AUDIO_LIVE_STREAM,
        publishedAt = uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli(),
    )

    private fun ChannelInfoItem.toChannelItem() = ChannelItem(
        url = url,
        name = name,
        thumbnailUrl = bestImage(thumbnails),
        subscriberCount = subscriberCount,
        description = description,
    )

    private fun List<VideoItem>.filterShorts(includeShorts: Boolean): List<VideoItem> =
        if (includeShorts) this else filterNot { Shorts.isShort(it) }

    private fun pickBestAudio(streams: List<AudioStream>?): AudioStream? {
        if (streams.isNullOrEmpty()) return null
        val progressive = streams.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val pool = progressive.ifEmpty { streams }

        // YouTube may serve dubbed audio tracks alongside the original; always prefer
        // the ORIGINAL track so we don't accidentally play a machine-dubbed language.
        // Untagged streams (single-track videos) are treated as original.
        val original = pool.filter {
            it.audioTrackType == AudioTrackType.ORIGINAL || it.audioTrackType == null
        }
        val preferred = original.ifEmpty { pool }
        return preferred.maxByOrNull { it.averageBitrate }
    }

    private fun bestImage(images: List<Image>?): String? {
        if (images.isNullOrEmpty()) return null
        return images.maxByOrNull { it.height }?.url ?: images.last().url
    }

    companion object {
        const val MIME_HLS = "application/x-mpegURL"
        const val MIME_DASH = "application/dash+xml"

        @Volatile private var initialized = false

        private fun ensureInitialized() {
            if (initialized) return
            synchronized(this) {
                if (!initialized) {
                    NewPipe.init(NewPipeDownloader.instance)
                    initialized = true
                }
            }
        }
    }
}
