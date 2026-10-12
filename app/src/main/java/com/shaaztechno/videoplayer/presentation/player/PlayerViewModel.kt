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
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val videoId: String,
    private val repository: VideoRepository,
    private val downloadManager: SZDownloadManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // StateFlow ensures loadNextVideo always sees the latest playlist without a re-fetch race.
    private val _playlist = MutableStateFlow<List<Video>>(emptyList())

    init {
        loadVideo(videoId)
        viewModelScope.launch {
            repository.getAllVideos().collect { _playlist.value = it }
        }
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

    fun loadNextVideo(currentPosition: Long, duration: Long) {
        val currentVideo = _uiState.value.video ?: return
        viewModelScope.launch {
            repository.updateHistory(
                video = currentVideo,
                position = currentPosition,
                duration = duration
            )

            val playlist = _playlist.value
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

    fun setError(message: String?) {
        _uiState.value = _uiState.value.copy(error = message, isLoading = false)
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
