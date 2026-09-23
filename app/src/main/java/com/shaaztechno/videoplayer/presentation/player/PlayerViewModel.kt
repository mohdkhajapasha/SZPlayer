package com.shaaztechno.videoplayer.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.downloader.SZDownloadManager
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val videoId: String,
    private val repository: VideoRepository,
    private val downloadManager: SZDownloadManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var playlist: List<Video> = emptyList()

    init {
        loadVideo(videoId)
        loadPlaylist()
    }

    private fun loadVideo(id: String) {
        viewModelScope.launch {
            val video = repository.getVideoById(id)
            val history = repository.getHistoryForVideo(id)
            if (video != null) {
                _uiState.value = _uiState.value.copy(
                    video = video,
                    isLoading = false,
                    initialPosition = history?.lastPosition ?: 0L,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(error = "Video not found", isLoading = false)
            }
        }
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            // In a real app, we might pass a folder ID or playlist ID.
            // For now, we fetch all videos but cache them in the ViewModel to avoid O(N) repeated fetches.
            playlist = repository.getAllVideos().first()
        }
    }

    fun loadNextVideo(currentPosition: Long, duration: Long) {
        val currentVideo = _uiState.value.video ?: return
        viewModelScope.launch {
            repository.updateHistory(
                video = currentVideo,
                position = currentPosition,
                duration = duration
            )

            if (playlist.isEmpty()) {
                playlist = repository.getAllVideos().first()
            }

            val currentIndex = playlist.indexOfFirst { it.id == currentVideo.id }
            if (currentIndex != -1 && currentIndex < playlist.size - 1) {
                val nextVideo = playlist[currentIndex + 1]
                loadVideo(nextVideo.id)
            }
        }
    }

    fun updatePlaybackHistory(position: Long, duration: Long) {
        val video = _uiState.value.video ?: return
        viewModelScope.launch {
            repository.updateHistory(
                video = video,
                position = position,
                duration = duration
            )
        }
    }

    fun deleteVideo(onDeleted: () -> Unit) {
        val video = _uiState.value.video ?: return
        viewModelScope.launch {
            try {
                downloadManager?.cancelDownload(video.id)
            } catch (_: Exception) {}
            repository.deleteVideo(video.id)
            onDeleted()
        }
    }

    class Factory(
        private val videoId: String,
        private val repository: VideoRepository,
        private val downloadManager: SZDownloadManager? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(videoId, repository, downloadManager) as T
        }
    }
}

data class PlayerUiState(
    val video: Video? = null,
    val initialPosition: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null
)
