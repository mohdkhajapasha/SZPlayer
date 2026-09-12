package com.shaaztechno.videoplayer.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: VideoRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMode = MutableStateFlow(SearchFilterMode.ALL)
    val filterMode: StateFlow<SearchFilterMode> = _filterMode.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.getAllVideos(), _searchQuery, _filterMode) { videos, query, filter ->
                if (query.isBlank()) {
                    SearchUiState(results = emptyList())
                } else {
                    val filtered = videos.filter { video ->
                        val matchesQuery = video.title.contains(query, ignoreCase = true) || 
                                           video.category?.contains(query, ignoreCase = true) == true
                        val matchesFilter = when (filter) {
                            SearchFilterMode.ALL -> true
                            SearchFilterMode.LOCAL -> video.type == VideoType.LOCAL || video.type == VideoType.DOWNLOADED
                            SearchFilterMode.ONLINE -> video.type == VideoType.ONLINE
                        }
                        matchesQuery && matchesFilter
                    }
                    SearchUiState(results = filtered)
                }
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFilterMode(mode: SearchFilterMode) {
        _filterMode.value = mode
    }

    class Factory(private val repository: VideoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(repository) as T
        }
    }
}

data class SearchUiState(
    val results: List<Video> = emptyList(),
    val isLoading: Boolean = false
)

enum class SearchFilterMode {
    ALL, LOCAL, ONLINE
}
