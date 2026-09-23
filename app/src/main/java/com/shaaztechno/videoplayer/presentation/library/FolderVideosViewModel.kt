package com.shaaztechno.videoplayer.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.local.dao.PlaylistDao
import com.shaaztechno.videoplayer.data.local.entity.PlaylistEntity
import com.shaaztechno.videoplayer.data.local.entity.PlaylistItemEntity
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FolderVideosViewModel(
    private val folderName: String,
    private val repository: VideoRepository,
    private val playlistDao: PlaylistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderVideosUiState(folderName = folderName))
    val uiState: StateFlow<FolderVideosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getVideosByType(VideoType.LOCAL),
                repository.getPlaybackHistory(),
                playlistDao.getAllPlaylists()
            ) { videos, history, playlists ->
                val folderVideos = videos.filter { (it.folder ?: "Internal Storage") == folderName }
                val historyMap = history.associate { it.videoId to if (it.duration > 0) it.lastPosition.toFloat() / it.duration else 0f }

                _uiState.value = _uiState.value.copy(
                    videos = folderVideos,
                    playlists = playlists,
                    historyMap = historyMap,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun addVideoToPlaylist(video: Video, playlistId: Long) {
        viewModelScope.launch {
            playlistDao.insertPlaylistItem(
                PlaylistItemEntity(
                    playlistId = playlistId,
                    videoId = video.id,
                    orderIndex = 0
                )
            )
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = name,
                    dateCreated = System.currentTimeMillis()
                )
            )
        }
    }

    class Factory(
        private val folderName: String,
        private val repository: VideoRepository,
        private val playlistDao: PlaylistDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return FolderVideosViewModel(folderName, repository, playlistDao) as T
        }
    }
}

data class FolderVideosUiState(
    val folderName: String,
    val videos: List<Video> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val historyMap: Map<String, Float> = emptyMap(),
    val isLoading: Boolean = true
)
