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

    /** A channel's recent uploads (long-form only by default). */
    suspend fun channelUploads(channelUrl: String, includeShorts: Boolean = false): List<VideoItem> =
        withContext(Dispatchers.IO) {
            ensureInitialized()
            val info = ChannelInfo.getInfo(youtube, channelUrl)
            val videosTab: ListLinkHandler = info.tabs.firstOrNull {
                it.contentFilters.contains(ChannelTabs.VIDEOS)
            } ?: return@withContext emptyList()

            val tabInfo = ChannelTabInfo.getInfo(youtube, videosTab)
            tabInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toVideoItem() }
                .filterShorts(includeShorts)
        }

    /** Resolve the best directly-playable audio-only stream for a video. */
    suspend fun resolveAudio(videoUrl: String): AudioTrack = withContext(Dispatchers.IO) {
        ensureInitialized()
        val info = StreamInfo.getInfo(youtube, videoUrl)
        val audio = pickBestAudio(info.audioStreams)
            ?: error("No audio stream available for $videoUrl")
        AudioTrack(
            videoUrl = info.url,
            title = info.name,
            uploader = info.uploaderName ?: "",
            uploaderUrl = info.uploaderUrl,
            durationSeconds = info.duration,
            thumbnailUrl = bestImage(info.thumbnails),
            streamUrl = audio.content, // the resolved URL for PROGRESSIVE_HTTP streams
            mimeType = audio.format?.mimeType,
            averageBitrate = audio.averageBitrate,
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
