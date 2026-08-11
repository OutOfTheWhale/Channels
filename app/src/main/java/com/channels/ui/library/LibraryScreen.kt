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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.channels.domain.model.DownloadItem
import com.channels.domain.model.DownloadState
import com.channels.domain.model.VideoItem
import com.channels.ui.components.CenteredNote
import com.channels.ui.components.ListRow
import com.channels.ui.components.RowDivider
import com.channels.ui.components.SectionLabel
import com.channels.ui.components.StarToggle
import com.channels.ui.components.durationOrLive
import com.channels.ui.components.formatSubscribers
import com.channels.ui.containerViewModelFactory
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Slate

@Composable
fun LibraryScreen(
    onOpenChannel: (String) -> Unit,
    onPlay: (List<VideoItem>, Int) -> Unit,
) {
    val vm: LibraryViewModel = viewModel(
        factory = containerViewModelFactory { LibraryViewModel(it.starredRepository, it.downloadRepository) },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        )

        if (state.loaded && state.starred.isEmpty() && state.downloads.isEmpty()) {
            CenteredNote("Star a channel from Search, or download a show to keep it offline.")
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (state.starred.isNotEmpty()) {
                item { SectionLabel("Starred channels") }
                items(state.starred, key = { it.url }) { channel ->
                    ListRow(
                        title = channel.name,
                        subtitle = formatSubscribers(channel.subscriberCount),
                        onClick = { onOpenChannel(channel.url) },
                        trailing = { StarToggle(starred = true, onToggle = { vm.unstar(channel.url) }) },
                    )
                    RowDivider()
                }
            }

            if (state.downloads.isNotEmpty()) {
                item { SectionLabel("Downloads") }
                items(state.downloads, key = { it.videoUrl }) { dl ->
                    ListRow(
                        title = dl.title,
                        subtitle = downloadSubtitle(dl),
                        onClick = {
                            if (dl.state == DownloadState.COMPLETED) onPlay(listOf(dl.toVideoItem()), 0)
                        },
                        trailing = {
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.titleLarge,
                                color = Slate,
                                modifier = Modifier
                                    .clickable { vm.deleteDownload(dl.videoUrl) }
                                    .padding(8.dp),
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
