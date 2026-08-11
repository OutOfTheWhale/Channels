package com.channels.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.channels.data.StarredRepository
import com.channels.domain.model.ChannelItem
import com.channels.ui.components.BackHeader
import com.channels.ui.components.CenteredNote
import com.channels.ui.components.ChannelAvatar
import com.channels.ui.components.ListRow
import com.channels.ui.components.RowDivider
import com.channels.ui.components.StarToggle
import com.channels.ui.components.formatSubscribers
import com.channels.ui.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StarredViewModel(private val starred: StarredRepository) : ViewModel() {
    private val _channels = MutableStateFlow<List<ChannelItem>>(emptyList())
    val channels = _channels.asStateFlow()

    init {
        viewModelScope.launch { starred.observeStarred().collect { _channels.value = it } }
    }

    fun unstar(url: String) = viewModelScope.launch { starred.unstar(url) }.let {}
}

@Composable
fun StarredChannelsScreen(onBack: () -> Unit, onOpenChannel: (String) -> Unit) {
    val vm: StarredViewModel = viewModel(
        factory = containerViewModelFactory { StarredViewModel(it.starredRepository) },
    )
    val channels by vm.channels.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader("Starred Channels", onBack)
        if (channels.isEmpty()) {
            CenteredNote("No starred channels yet. Star one from Search to follow it.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(channels, key = { it.url }) { channel ->
                    ListRow(
                        title = channel.name,
                        subtitle = formatSubscribers(channel.subscriberCount),
                        onClick = { onOpenChannel(channel.url) },
                        leading = { ChannelAvatar(channel.thumbnailUrl) },
                        trailing = { StarToggle(starred = true, onToggle = { vm.unstar(channel.url) }) },
                    )
                    RowDivider()
                }
            }
        }
    }
}
