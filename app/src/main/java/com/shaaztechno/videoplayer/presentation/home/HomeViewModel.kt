package com.shaaztechno.videoplayer.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.local.dao.PlaylistDao
import com.shaaztechno.videoplayer.data.local.entity.HistoryEntity
import com.shaaztechno.videoplayer.data.local.entity.PlaylistEntity
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: VideoRepository,
    private val playlistDao: PlaylistDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val playlistsFlow = playlistDao?.getAllPlaylists() ?: flowOf(emptyList())
            combine(
                repository.getVideosByType(VideoType.ONLINE),
                repository.getVideosByType(VideoType.LOCAL),
                repository.getPlaybackHistory(),
                playlistsFlow
            ) { online, local, history, playlists ->
                val foldersMap = local.groupBy { it.folder ?: "Internal Storage" }
                HomeUiState(
                    onlineVideos = online,
                    localVideos = local,
                    recentlyPlayed = history,
                    playlists = playlists,
                    folders = foldersMap,
                    isLoading = false
                )
            }.collect {
                _uiState.value = it
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.refreshOnlineCatalog()
            repository.refreshLocalVideos()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    class Factory(
        private val repository: VideoRepository,
        private val playlistDao: PlaylistDao? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, playlistDao) as T
        }
    }
}

data class HomeUiState(
    val onlineVideos: List<Video> = emptyList(),
    val localVideos: List<Video> = emptyList(),
    val recentlyPlayed: List<HistoryEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val folders: Map<String, List<Video>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

