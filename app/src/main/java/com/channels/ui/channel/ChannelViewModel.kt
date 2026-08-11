package com.channels.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.channels.data.StarredRepository
import com.channels.data.youtube.YoutubeRepository
import com.channels.domain.model.ChannelItem
import com.channels.domain.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChannelUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val channel: ChannelItem? = null,
    val uploads: List<VideoItem> = emptyList(),
    val starred: Boolean = false,
)

class ChannelViewModel(
    private val channelUrl: String,
    private val youtube: YoutubeRepository,
    private val starred: StarredRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChannelUiState())
    val state = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            starred.observeIsStarred(channelUrl).collect { isStarred ->
                _state.update { it.copy(starred = isStarred) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val channel = youtube.getChannel(channelUrl)
                val uploads = youtube.channelUploads(channelUrl)
                _state.update { it.copy(loading = false, channel = channel, uploads = uploads) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Couldn't load channel") }
            }
        }
    }

    fun toggleStar() {
        val channel = _state.value.channel ?: return
        viewModelScope.launch {
            if (_state.value.starred) starred.unstar(channel.url) else starred.star(channel)
        }
    }
}
