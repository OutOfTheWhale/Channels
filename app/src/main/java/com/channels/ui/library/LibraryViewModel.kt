package com.channels.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.channels.data.StarredRepository
import com.channels.data.download.DownloadRepository
import com.channels.domain.model.ChannelItem
import com.channels.domain.model.DownloadItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val downloads: List<DownloadItem> = emptyList(),
    val starred: List<ChannelItem> = emptyList(),
    val loaded: Boolean = false,
)

class LibraryViewModel(
    private val starred: StarredRepository,
    private val downloads: DownloadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                starred.observeStarred(),
                downloads.observeDownloads(),
            ) { channels, dls -> channels to dls }
                .collect { (channels, dls) ->
                    _state.update { it.copy(starred = channels, downloads = dls, loaded = true) }
                }
        }
    }

    fun unstar(url: String) {
        viewModelScope.launch { starred.unstar(url) }
    }

    fun deleteDownload(url: String) {
        viewModelScope.launch { downloads.delete(url) }
    }
}
