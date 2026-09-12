package com.shaaztechno.videoplayer.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.local.dao.PlaylistDao
import com.shaaztechno.videoplayer.domain.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val playlistDao: PlaylistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playlistDao.getVideosInPlaylist(playlistId).collect { entities ->
                _uiState.value = PlaylistDetailUiState(
                    videos = entities.map { it.toDomain() },
                    isLoading = false
                )
            }
        }
    }

    fun removeVideoFromPlaylist(videoId: String) {
        viewModelScope.launch {
            playlistDao.removeVideoFromPlaylist(playlistId, videoId)
        }
    }

    class Factory(
        private val playlistId: Long,
        private val playlistDao: PlaylistDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlaylistDetailViewModel(playlistId, playlistDao) as T
        }
    }
}

data class PlaylistDetailUiState(
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = true
)
