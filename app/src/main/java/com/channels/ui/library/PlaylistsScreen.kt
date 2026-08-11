package com.channels.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.channels.data.PlaylistRepository
import com.channels.domain.model.Playlist
import com.channels.ui.components.BackHeader
import com.channels.ui.components.CenteredNote
import com.channels.ui.components.ListRow
import com.channels.ui.components.RowDivider
import com.channels.ui.containerViewModelFactory
import com.channels.ui.theme.Ash
import com.channels.ui.theme.Hairline
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Paper
import com.channels.ui.theme.Slate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistsViewModel(private val repo: PlaylistRepository) : ViewModel() {
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    init {
        viewModelScope.launch { repo.observePlaylists().collect { _playlists.value = it } }
    }

    fun create(name: String) = viewModelScope.launch { repo.create(name) }.let {}
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }.let {}
}

@Composable
fun PlaylistsScreen(onBack: () -> Unit, onOpenPlaylist: (Long) -> Unit) {
    val vm: PlaylistsViewModel = viewModel(
        factory = containerViewModelFactory { PlaylistsViewModel(it.playlistRepository) },
    )
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader("Playlists", onBack)

        ListRow(
            title = "＋ New playlist",
            subtitle = null,
            onClick = { showCreate = true },
        )
        RowDivider()

        if (playlists.isEmpty()) {
            CenteredNote("No playlists yet. Create one, then add videos with ＋.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = { it.id }) { pl ->
                    ListRow(
                        title = pl.name,
                        subtitle = "${pl.itemCount} ${if (pl.itemCount == 1) "video" else "videos"}",
                        onClick = { onOpenPlaylist(pl.id) },
                        trailing = {
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.titleLarge,
                                color = Slate,
                                modifier = Modifier.clickable { vm.delete(pl.id) }.padding(8.dp),
                            )
                        },
                    )
                    RowDivider()
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            confirmButton = {
                Text(
                    text = "Create",
                    color = if (name.isBlank()) Slate else Ink,
                    modifier = Modifier
                        .clickable(enabled = name.isNotBlank()) {
                            vm.create(name)
                            showCreate = false
                        }
                        .padding(8.dp),
                )
            },
            dismissButton = {
                Text(
                    text = "Cancel",
                    color = Slate,
                    modifier = Modifier.clickable { showCreate = false }.padding(8.dp),
                )
            },
            title = { Text("New playlist", color = Ink) },
            containerColor = Paper,
            text = {
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
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}
