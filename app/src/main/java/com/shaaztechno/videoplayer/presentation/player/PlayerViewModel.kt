package com.shaaztechno.videoplayer.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val videoId: String,
    private val repository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadVideo()
    }

    private fun loadVideo() {
        viewModelScope.launch {
            val video = repository.getVideoById(videoId)
            val history = repository.getHistoryForVideo(videoId)
            if (video != null) {
                _uiState.value = _uiState.value.copy(
                    video = video,
                    isLoading = false,
                    initialPosition = history?.lastPosition ?: 0L
                )
            } else {
                _uiState.value = _uiState.value.copy(error = "Video not found", isLoading = false)
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

    class Factory(
        private val videoId: String,
        private val repository: VideoRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(videoId, repository) as T
        }
    }
}

data class PlayerUiState(
    val video: Video? = null,
    val initialPosition: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null
)
