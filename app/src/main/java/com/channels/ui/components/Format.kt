package com.channels.ui.components

import java.util.Locale

/** Formats a duration in seconds as M:SS or H:MM:SS. Non-positive => 0:00. */
fun formatDuration(seconds: Long): String {
    val total = seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

/** Duration label for list rows: shows LIVE for live/unknown-length items. */
fun durationOrLive(seconds: Long): String =
    if (seconds <= 0) "LIVE" else formatDuration(seconds)

/** Formats a subscriber count like 1.2M, 12K, 950. -1 => blank. */
fun formatSubscribers(count: Long): String {
    if (count < 0) return ""
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM subscribers", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK subscribers", count / 1_000.0)
        else -> "$count subscribers"
    }
}
