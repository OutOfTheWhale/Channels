package com.channels.domain.usecase

import com.channels.domain.model.VideoItem

/**
 * The one place that decides "is this a Short?". Every list (search, channel,
 * feed) runs through [isShort] so the "no Shorts" rule is consistent and tunable.
 */
object Shorts {

    /** Videos at or under this duration are treated as Shorts. */
    const val SHORTS_MAX_SECONDS: Long = 62

    /**
     * A video is a Short if it is served from the `/shorts/` path, or if it has a
     * known, non-zero duration at or below [maxSeconds]. Live streams and items
     * with unknown duration (0 or negative) are NOT Shorts.
     */
    fun isShort(
        durationSeconds: Long,
        url: String?,
        maxSeconds: Long = SHORTS_MAX_SECONDS,
    ): Boolean {
        if (url != null && url.contains("/shorts/")) return true
        return durationSeconds in 1..maxSeconds
    }

    fun isShort(item: VideoItem, maxSeconds: Long = SHORTS_MAX_SECONDS): Boolean =
        isShort(item.durationSeconds, item.url, maxSeconds)

    /** Convenience: keep only long-form items. */
    fun List<VideoItem>.withoutShorts(maxSeconds: Long = SHORTS_MAX_SECONDS): List<VideoItem> =
        filterNot { isShort(it, maxSeconds) }
}
