package com.channels.ui.channel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.channels.domain.model.VideoItem
import com.channels.ui.components.CenteredNote
import com.channels.ui.components.ListRow
import com.channels.ui.components.RowDivider
import com.channels.ui.components.StarToggle
import com.channels.ui.components.durationOrLive
import com.channels.ui.components.formatSubscribers
import com.channels.ui.containerViewModelFactory
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Slate

@Composable
fun ChannelScreen(
    channelUrl: String,
    onBack: () -> Unit,
    onPlay: (List<VideoItem>, Int) -> Unit,
) {
    val vm: ChannelViewModel = viewModel(
        key = channelUrl,
        factory = containerViewModelFactory {
            ChannelViewModel(channelUrl, it.youtubeRepository, it.starredRepository)
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar: back + title + star
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.displaySmall,
                color = Ink,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 8.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = state.channel?.name ?: "Channel",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subs = state.channel?.subscriberCount?.let { formatSubscribers(it) } ?: ""
                if (subs.isNotBlank()) {
                    Text(text = subs, style = MaterialTheme.typography.bodyMedium, color = Slate)
                }
            }
            if (state.channel != null) {
                Spacer(Modifier.width(4.dp))
                StarToggle(starred = state.starred, onToggle = vm::toggleStar)
            }
        }
        RowDivider()

        when {
            state.loading -> CenteredNote("Loading…")
            state.error != null -> CenteredNote(state.error!!)
            state.uploads.isEmpty() -> CenteredNote("No long-form videos found.")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.uploads, key = { _, v -> v.url }) { index, video ->
                    ListRow(
                        title = video.title,
                        subtitle = durationOrLive(video.durationSeconds),
                        onClick = { onPlay(state.uploads, index) },
                    )
                    RowDivider()
                }
            }
        }
    }
}
