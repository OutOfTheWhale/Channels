package com.channels.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.channels.domain.model.VideoItem
import com.channels.ui.components.CenteredNote
import com.channels.ui.components.ListRow
import com.channels.ui.components.RowDivider
import com.channels.ui.components.durationOrLive
import com.channels.ui.containerViewModelFactory
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Slate

@Composable
fun HomeScreen(onPlay: (List<VideoItem>, Int) -> Unit) {
    val vm: HomeViewModel = viewModel(
        factory = containerViewModelFactory { HomeViewModel(it.feedRepository, it.starredRepository) },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Channels",
                style = MaterialTheme.typography.displaySmall,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (state.refreshing) "…" else "↻",
                style = MaterialTheme.typography.titleLarge,
                color = Slate,
                modifier = Modifier
                    .clickable(enabled = !state.refreshing, onClick = vm::refresh)
                    .padding(8.dp),
            )
        }

        when {
            state.feed.isEmpty() && state.refreshing -> CenteredNote("Loading your feed…")
            state.feed.isEmpty() && !state.hasStarred ->
                CenteredNote("Star channels in Search and their new long-form audio collects here.")
            state.feed.isEmpty() ->
                CenteredNote("No recent long-form audio yet. Pull ↻ to refresh.")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.feed, key = { _, v -> v.url }) { index, video ->
                    ListRow(
                        title = video.title,
                        subtitle = "${video.uploader}  ·  ${durationOrLive(video.durationSeconds)}",
                        onClick = { onPlay(state.feed, index) },
                    )
                    RowDivider()
                }
            }
        }
    }
}
