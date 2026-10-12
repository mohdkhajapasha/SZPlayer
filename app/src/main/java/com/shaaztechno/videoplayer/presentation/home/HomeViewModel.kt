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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: VideoRepository,
    private val playlistDao: PlaylistDao? = null
) : ViewModel() {

    private val playlistsFlow = playlistDao?.getAllPlaylists() ?: flowOf(emptyList())

    // Uses stateIn(WhileSubscribed) — the idiomatic, lifecycle-safe way to expose a combined
    // flow as StateFlow. Automatically stops/restarts collection with the subscriber lifecycle.
    val uiState: StateFlow<HomeUiState> = combine(
        repository.getVideosByType(VideoType.ONLINE),
        repository.getVideosByType(VideoType.LOCAL),
        repository.getPlaybackHistory(),
        playlistsFlow
    ) { online, local, history, playlists ->
        val foldersMap = local.groupBy { it.folder ?: "Internal Storage" }
        val historyMap = history.associate {
            it.videoId to if (it.duration > 0) it.lastPosition.toFloat() / it.duration else 0f
        }
        HomeUiState(
            onlineVideos = online,
            localVideos = local,
            recentlyPlayed = history,
            historyMap = historyMap,
            playlists = playlists,
            folders = foldersMap,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshOnlineCatalog()
            repository.refreshLocalVideos()
        }
    }

    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            repository.deleteVideo(video.id)
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
    val historyMap: Map<String, Float> = emptyMap(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val folders: Map<String, List<Video>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)
