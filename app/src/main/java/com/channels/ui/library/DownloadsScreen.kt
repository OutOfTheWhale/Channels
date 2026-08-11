package com.channels.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.channels.data.download.DownloadRepository
import com.channels.domain.model.DownloadItem
import com.channels.domain.model.DownloadState
import com.channels.domain.model.VideoItem
import com.channels.ui.components.BackHeader
import com.channels.ui.components.CenteredNote
import com.channels.ui.components.ListRow
import com.channels.ui.components.RowDivider
import com.channels.ui.components.VideoThumb
import com.channels.ui.components.durationOrLive
import com.channels.ui.containerViewModelFactory
import com.channels.ui.theme.Slate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadsViewModel(private val downloads: DownloadRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items = _items.asStateFlow()

    init {
        viewModelScope.launch { downloads.observeDownloads().collect { _items.value = it } }
    }

    fun delete(url: String) = viewModelScope.launch { downloads.delete(url) }.let {}
}

@Composable
fun DownloadsScreen(onBack: () -> Unit, onPlay: (List<VideoItem>, Int) -> Unit) {
    val vm: DownloadsViewModel = viewModel(
        factory = containerViewModelFactory { DownloadsViewModel(it.downloadRepository) },
    )
    val items by vm.items.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader("Downloads", onBack)
        if (items.isEmpty()) {
            CenteredNote("No downloads yet. Open a show and tap “Download for offline”.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.videoUrl }) { dl ->
                    ListRow(
                        title = dl.title,
                        subtitle = downloadSubtitle(dl),
                        onClick = { if (dl.state == DownloadState.COMPLETED) onPlay(listOf(dl.toVideoItem()), 0) },
                        leading = { VideoThumb(dl.thumbnailUrl) },
                        trailing = {
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.titleLarge,
                                color = Slate,
                                modifier = Modifier.clickable { vm.delete(dl.videoUrl) }.padding(8.dp),
                            )
                        },
                    )
                    RowDivider()
                }
            }
        }
    }
}

private fun downloadSubtitle(dl: DownloadItem): String = when (dl.state) {
    DownloadState.COMPLETED -> "${dl.uploader}  ·  ${durationOrLive(dl.durationSeconds)}"
    DownloadState.RUNNING ->
        if (dl.progress >= 0f) "Downloading ${(dl.progress * 100).toInt()}%" else "Downloading…"
    DownloadState.QUEUED -> "Queued…"
    DownloadState.FAILED -> "Download failed"
}

private fun DownloadItem.toVideoItem() = VideoItem(
    id = videoUrl.substringAfterLast("v=", videoUrl.substringAfterLast('/')),
    url = videoUrl,
    title = title,
    uploader = uploader,
    uploaderUrl = null,
    durationSeconds = durationSeconds,
    thumbnailUrl = thumbnailUrl,
    isLive = false,
)
