package com.channels.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.channels.data.FeedRepository
import com.channels.data.StarredRepository
import com.channels.data.download.DownloadRepository
import com.channels.domain.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val feed: List<VideoItem> = emptyList(),
    val downloadedUrls: Set<String> = emptySet(),
    val refreshing: Boolean = false,
    val hasStarred: Boolean = true,
    val error: String? = null,
)

class HomeViewModel(
    private val feedRepo: FeedRepository,
    private val starred: StarredRepository,
    private val downloads: DownloadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            feedRepo.observeFeed().collect { feed -> _state.update { it.copy(feed = feed) } }
        }
        viewModelScope.launch {
            starred.observeStarred().collect { list ->
                _state.update { it.copy(hasStarred = list.isNotEmpty()) }
            }
        }
        viewModelScope.launch {
            downloads.observeCompletedUrls().collect { urls ->
                _state.update { it.copy(downloadedUrls = urls) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            try {
                feedRepo.refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Couldn't refresh") }
            } finally {
                _state.update { it.copy(refreshing = false) }
            }
        }
    }
}
