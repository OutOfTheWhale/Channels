package com.channels.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import com.channels.domain.model.VideoItem
import com.channels.ui.rememberAppContainer
import com.channels.ui.theme.Ash
import com.channels.ui.theme.Hairline
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Paper
import com.channels.ui.theme.Slate
import kotlinx.coroutines.launch

/** A "＋" affordance that opens a picker to add [video] to a playlist (or create one). */
@Composable
fun AddToPlaylistButton(video: VideoItem, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Text(
        text = "＋",
        style = MaterialTheme.typography.headlineMedium,
        color = Slate,
        modifier = modifier
            .clickable { open = true }
            .padding(8.dp),
    )
    if (open) AddToPlaylistDialog(video = video, onDismiss = { open = false })
}

@Composable
private fun AddToPlaylistDialog(video: VideoItem, onDismiss: () -> Unit) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val playlists by container.playlistRepository.observePlaylists()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            Text(
                text = "Close",
                color = Slate,
                modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
            )
        },
        title = { Text("Add to playlist", color = Ink) },
        containerColor = Paper,
        text = {
            Column {
                playlists.forEach { pl ->
                    Text(
                        text = "${pl.name}   ·   ${pl.itemCount}",
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { container.playlistRepository.addVideo(pl.id, video) }
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                    )
                }
                if (creating) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        placeholder = { Text("Playlist name", color = Ash) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Ink,
                            unfocusedBorderColor = Hairline,
                            cursorColor = Ink,
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Text(
                        text = "Create & add",
                        color = if (name.isBlank()) Slate else Ink,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = name.isNotBlank()) {
                                scope.launch {
                                    val id = container.playlistRepository.create(name)
                                    container.playlistRepository.addVideo(id, video)
                                }
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                    )
                } else {
                    Text(
                        text = "＋ New playlist",
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { creating = true }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
    )
}
