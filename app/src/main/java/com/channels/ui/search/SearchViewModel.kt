package com.channels.ui.search

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

enum class SearchTab { Channels, Videos }

data class SearchUiState(
    val query: String = "",
    val tab: SearchTab = SearchTab.Channels,
    val loading: Boolean = false,
    val error: String? = null,
    val channels: List<ChannelItem> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val starredUrls: Set<String> = emptySet(),
    val hasSearched: Boolean = false,
)

class SearchViewModel(
    private val youtube: YoutubeRepository,
    private val starred: StarredRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            starred.observeStarred().collect { list ->
                _state.update { it.copy(starredUrls = list.map { c -> c.url }.toSet()) }
            }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun onTabChange(tab: SearchTab) {
        _state.update { it.copy(tab = tab) }
        if (_state.value.query.isNotBlank()) search()
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, hasSearched = true) }
            try {
                when (_state.value.tab) {
                    SearchTab.Channels -> {
                        val results = youtube.searchChannels(q)
                        _state.update { it.copy(channels = results, loading = false) }
                    }
                    SearchTab.Videos -> {
                        val results = youtube.searchVideos(q)
                        _state.update { it.copy(videos = results, loading = false) }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Something went wrong") }
            }
        }
    }

    fun toggleStar(channel: ChannelItem) {
        viewModelScope.launch {
            if (_state.value.starredUrls.contains(channel.url)) {
                starred.unstar(channel.url)
            } else {
                starred.star(channel)
            }
        }
    }
}
