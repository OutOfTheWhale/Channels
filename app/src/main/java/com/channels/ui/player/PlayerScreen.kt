package com.channels.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.channels.domain.model.AudioTrack
import com.channels.domain.model.DownloadState
import com.channels.domain.model.VideoItem
import com.channels.playback.PlayerController
import com.channels.ui.components.AddToPlaylistButton
import com.channels.ui.components.CenteredNote
import com.channels.ui.components.formatDuration
import com.channels.ui.rememberAppContainer
import com.channels.ui.theme.Hairline
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Paper
import com.channels.ui.theme.Slate
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    controller: PlayerController,
    onBack: () -> Unit,
    onOpenChannel: (String) -> Unit,
) {
    val state by controller.state.collectAsStateWithLifecycle()

    var dragging by remember { mutableStateOf(false) }
    var dragMs by remember { mutableLongStateOf(0L) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "‹",
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (!state.hasContent) {
            CenteredNote("Nothing playing yet. Pick something to listen to.")
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val title = state.track?.title ?: state.loadingTitle ?: ""
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            val uploaderUrl = state.track?.uploaderUrl
            Text(
                text = state.track?.uploader ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = if (uploaderUrl != null) Ink else Slate, // brighter = tappable
                textAlign = TextAlign.Center,
                modifier = if (uploaderUrl != null) {
                    Modifier.clickable { onOpenChannel(uploaderUrl) }
                } else {
                    Modifier
                },
            )

            Spacer(Modifier.height(28.dp))

            when {
                state.error != null -> Text(
                    text = state.error!!,
                    color = Slate,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                state.track == null -> Text(
                    text = "Loading audio…",
                    color = Slate,
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> PlaybackControls(
                    positionMs = if (dragging) dragMs else state.positionMs,
                    durationMs = state.durationMs,
                    isPlaying = state.isPlaying,
                    isBuffering = state.isBuffering,
                    hasNext = state.hasNext,
                    hasPrevious = state.hasPrevious,
                    onSeekChange = { dragging = true; dragMs = it },
                    onSeekFinished = { controller.seekTo(dragMs); dragging = false },
                    onPlayPause = controller::togglePlayPause,
                    onSkipBack = { controller.skip(-15_000) },
                    onSkipForward = { controller.skip(15_000) },
                    onPrevious = controller::skipToPrevious,
                    onNext = controller::skipToNext,
                )
            }
        }

        // Bottom action row (left→right): download, playback speed, add-to-playlist.
        state.track?.let { track ->
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DownloadControl(track = track)
                Spacer(Modifier.width(24.dp))
                SpeedControl(speed = state.speed, onCycle = controller::cycleSpeed)
                Spacer(Modifier.width(24.dp))
                AddToPlaylistButton(track.toVideoItem())
            }
        }
    }
}

@Composable
private fun PlaybackControls(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onSeekChange: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val hasDuration = durationMs > 0
    Slider(
        value = if (hasDuration) positionMs.coerceIn(0, durationMs).toFloat() else 0f,
        onValueChange = { onSeekChange(it.toLong()) },
        onValueChangeFinished = onSeekFinished,
        valueRange = 0f..(if (hasDuration) durationMs.toFloat() else 1f),
        enabled = hasDuration,
        colors = SliderDefaults.colors(
            thumbColor = Ink,
            activeTrackColor = Ink,
            inactiveTrackColor = Hairline,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatDuration(positionMs / 1000), style = MaterialTheme.typography.labelMedium, color = Slate)
        Text(
            if (hasDuration) formatDuration(durationMs / 1000) else "—",
            style = MaterialTheme.typography.labelMedium,
            color = Slate,
        )
    }

    Spacer(Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(text = "|◀", onClick = onPrevious, enabled = hasPrevious)
        Spacer(Modifier.size(6.dp))
        GlyphButton(text = "−15", onClick = onSkipBack)
        Spacer(Modifier.size(10.dp))
        BigPlayButton(isPlaying = isPlaying, isBuffering = isBuffering, onClick = onPlayPause)
        Spacer(Modifier.size(10.dp))
        GlyphButton(text = "+15", onClick = onSkipForward)
        Spacer(Modifier.size(6.dp))
        GlyphButton(text = "▶|", onClick = onNext, enabled = hasNext)
    }
}

@Composable
private fun BigPlayButton(isPlaying: Boolean, isBuffering: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .border(2.dp, Ink, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val glyph = when {
            isBuffering && !isPlaying -> "…"
            isPlaying -> "❚❚"
            else -> "▶"
        }
        Text(text = glyph, style = MaterialTheme.typography.headlineMedium, color = Ink)
    }
}

@Composable
private fun GlyphButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = if (enabled) Ink else Slate,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
    )
}

private fun formatSpeed(speed: Float): String {
    val s = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "${s}×"
}

/** A round control: outlined normally, filled (inverted) when [filled]. */
@Composable
private fun IconPill(
    glyph: String,
    filled: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentColor: androidx.compose.ui.graphics.Color = Ink,
) {
    val bg = if (filled) Ink else androidx.compose.ui.graphics.Color.Transparent
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (filled) Modifier else Modifier.border(1.dp, Hairline, CircleShape))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium,
            color = if (filled) Paper else contentColor,
        )
    }
}

/** Playback-speed pill, tap to cycle. Sized to line up with the round buttons. */
@Composable
private fun SpeedControl(speed: Float, onCycle: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .border(1.dp, Hairline, CircleShape)
            .clickable(onClick = onCycle)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = formatSpeed(speed), style = MaterialTheme.typography.titleMedium, color = Ink)
    }
}

/**
 * Download control as a single arrow. Outlined = not downloaded, dim while downloading,
 * and filled/highlighted once downloaded (tap then to remove).
 */
@Composable
private fun DownloadControl(track: AudioTrack) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val download by container.downloadRepository
        .observeDownload(track.videoUrl)
        .collectAsStateWithLifecycle(initialValue = null)

    when (download?.state) {
        DownloadState.COMPLETED -> IconPill(
            glyph = "⤓",
            filled = true, // highlighted = downloaded
            onClick = { scope.launch { container.downloadRepository.delete(track.videoUrl) } },
        )
        DownloadState.QUEUED, DownloadState.RUNNING -> IconPill(
            glyph = "⤓",
            filled = false,
            enabled = false,
            contentColor = Slate, // dim while downloading
            onClick = {},
        )
        else -> IconPill(
            glyph = "⤓",
            filled = false,
            onClick = { scope.launch { container.downloadRepository.enqueue(track.toVideoItem()) } },
        )
    }
}

private fun AudioTrack.toVideoItem() = VideoItem(
    id = videoUrl.substringAfterLast("v=", videoUrl.substringAfterLast('/')),
    url = videoUrl,
    title = title,
    uploader = uploader,
    uploaderUrl = uploaderUrl,
    durationSeconds = durationSeconds,
    thumbnailUrl = thumbnailUrl,
    isLive = false,
)
