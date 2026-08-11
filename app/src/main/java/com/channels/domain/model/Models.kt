package com.channels.domain.model

/** A long-form video surfaced for listening (Shorts are filtered out upstream). */
data class VideoItem(
    val id: String,
    val url: String,
    val title: String,
    val uploader: String,
    val uploaderUrl: String?,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val isLive: Boolean,
    val publishedAt: Long? = null, // epoch millis, when known
)

/** A YouTube channel, as returned by search or referenced by a star. */
data class ChannelItem(
    val url: String,
    val name: String,
    val thumbnailUrl: String?,
    val subscriberCount: Long, // -1 when unknown
    val description: String?,
)

/** A user playlist and how many videos it holds. */
data class Playlist(
    val id: Long,
    val name: String,
    val itemCount: Int,
)

enum class DownloadState { QUEUED, RUNNING, COMPLETED, FAILED }

/** An offline download of a video's audio, with progress. */
data class DownloadItem(
    val videoUrl: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val filePath: String?,
    val state: DownloadState,
    val bytesDownloaded: Long,
    val totalBytes: Long,
) {
    /** 0f..1f, or -1f when total size is unknown. */
    val progress: Float
        get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else -1f
}

/** A resolved, directly-playable audio-only stream for a given video. */
data class AudioTrack(
    val videoUrl: String,
    val title: String,
    val uploader: String,
    val uploaderUrl: String?,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val streamUrl: String,
    val mimeType: String?,
    val averageBitrate: Int,
)
