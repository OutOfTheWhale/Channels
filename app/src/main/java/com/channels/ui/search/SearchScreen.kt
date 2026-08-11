package com.channels.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.channels.domain.model.VideoItem
import com.channels.ui.components.CenteredNote
import com.channels.ui.components.LightSearchField
import com.channels.ui.components.ListRow
import com.channels.ui.components.RowDivider
import com.channels.ui.components.StarToggle
import com.channels.ui.components.durationOrLive
import com.channels.ui.components.formatSubscribers
import com.channels.ui.containerViewModelFactory
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Slate

@Composable
fun SearchScreen(
    onOpenChannel: (String) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
) {
    val vm: SearchViewModel = viewModel(
        factory = containerViewModelFactory { SearchViewModel(it.youtubeRepository, it.starredRepository) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        LightSearchField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            onSubmit = {
                vm.search()
                focusManager.clearFocus() // drop focus + hide the soft keyboard
            },
            placeholder = "Search channels or videos",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            TabLabel("Channels", state.tab == SearchTab.Channels) { vm.onTabChange(SearchTab.Channels) }
            Spacer(Modifier.width(20.dp))
            TabLabel("Videos", state.tab == SearchTab.Videos) { vm.onTabChange(SearchTab.Videos) }
        }
        Spacer(Modifier.padding(top = 4.dp))

        when {
            state.loading -> CenteredNote("Searching…")
            state.error != null -> CenteredNote(state.error!!)
            state.tab == SearchTab.Channels -> {
                if (state.hasSearched && state.channels.isEmpty()) {
                    CenteredNote("No channels found.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.channels, key = { it.url }) { channel ->
                            ListRow(
                                title = channel.name,
                                subtitle = formatSubscribers(channel.subscriberCount),
                                onClick = { onOpenChannel(channel.url) },
                                trailing = {
                                    StarToggle(
                                        starred = state.starredUrls.contains(channel.url),
                                        onToggle = { vm.toggleStar(channel) },
                                    )
                                },
                            )
                            RowDivider()
                        }
                    }
                }
            }
            else -> {
                if (state.hasSearched && state.videos.isEmpty()) {
                    CenteredNote("No videos found.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.videos, key = { it.url }) { video ->
                            ListRow(
                                title = video.title,
                                subtitle = "${video.uploader}  ·  ${durationOrLive(video.durationSeconds)}",
                                onClick = { onPlayVideo(video) },
                            )
                            RowDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) Ink else Slate,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}
