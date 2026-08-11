package com.channels.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.channels.playback.PlayerState
import com.channels.ui.theme.Hairline
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Slate

/** Persistent now-playing strip shown above the bottom bar. Tap to open the player. */
@Composable
fun MiniPlayer(
    state: PlayerState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.track?.title ?: state.loadingTitle ?: return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawLine(Hairline, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2f)
            }
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.track?.uploader ?: "Loading…",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (state.isPlaying) "❚❚" else "▶",
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            modifier = Modifier
                .clickable(onClick = onPlayPause)
                .padding(8.dp),
        )
    }
}
