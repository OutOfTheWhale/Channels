package com.channels.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.channels.data.PlaylistRepository
import com.channels.data.download.DownloadRepository
import com.channels.domain.model.VideoItem
import com.channels.ui.components.BackHeader
import com.channels.ui.components.CenteredNote
import com.channels.ui.components.ListRow
import com.channels.ui.components.RowDivider
import com.channels.ui.components.VideoThumb
import com.channels.ui.components.durationOrLive
import com.channels.ui.containerViewModelFactory
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Slate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val repo: PlaylistRepository,
    private val downloads: DownloadRepository,
) : ViewModel() {
    private val _name = MutableStateFlow("Playlist")
    val name = _name.asStateFlow()
    private val _items = MutableStateFlow<List<VideoItem>>(emptyList())
    val items = _items.asStateFlow()
    private val _downloadedUrls = MutableStateFlow<Set<String>>(emptySet())
    val downloadedUrls = _downloadedUrls.asStateFlow()

    init {
        viewModelScope.launch { repo.observeName(playlistId).collect { it?.let { n -> _name.value = n } } }
        viewModelScope.launch { repo.observeItems(playlistId).collect { _items.value = it } }
        viewModelScope.launch { downloads.observeCompletedUrls().collect { _downloadedUrls.value = it } }
    }

    fun remove(videoUrl: String) = viewModelScope.launch { repo.removeVideo(playlistId, videoUrl) }.let {}

    /** Queue downloads for every video in the playlist that isn't already downloaded. */
    fun downloadAll() = viewModelScope.launch {
        val done = _downloadedUrls.value
        _items.value.filterNot { done.contains(it.url) }.forEach { downloads.enqueue(it) }
    }.let {}
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    onPlay: (List<VideoItem>, Int) -> Unit,
) {
    val vm: PlaylistDetailViewModel = viewModel(
        key = "playlist-$playlistId",
        factory = containerViewModelFactory {
            PlaylistDetailViewModel(playlistId, it.playlistRepository, it.downloadRepository)
        },
    )
    val name by vm.name.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val downloadedUrls by vm.downloadedUrls.collectAsStateWithLifecycle()

    val allDownloaded = items.isNotEmpty() && items.all { downloadedUrls.contains(it.url) }

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(
            title = name,
            onBack = onBack,
            trailing = {
                if (items.isNotEmpty()) {
                    if (allDownloaded) {
                        Text(
                            text = "Downloaded ✓",
                            style = MaterialTheme.typography.labelLarge,
                            color = Slate,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    } else {
                        Text(
                            text = "⤓ Download all",
                            style = MaterialTheme.typography.labelLarge,
                            color = Ink,
                            modifier = Modifier
                                .clickable { vm.downloadAll() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            },
        )
        if (items.isEmpty()) {
            CenteredNote("This playlist is empty. Add videos with the ＋ next to any show.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(items, key = { _, v -> v.url }) { index, video ->
                    ListRow(
                        title = video.title,
                        subtitle = video.uploader,
                        onClick = { onPlay(items, index) },
                        leading = { VideoThumb(video.thumbnailUrl) },
                        downloaded = downloadedUrls.contains(video.url),
                        endText = durationOrLive(video.durationSeconds),
                        trailing = {
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.titleLarge,
                                color = Slate,
                                modifier = Modifier.clickable { vm.remove(video.url) }.padding(8.dp),
                            )
                        },
                    )
                    RowDivider()
                }
            }
        }
    }
}
