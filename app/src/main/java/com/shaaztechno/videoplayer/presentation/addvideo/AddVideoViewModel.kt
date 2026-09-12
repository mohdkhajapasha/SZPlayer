package com.shaaztechno.videoplayer.presentation.addvideo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.local.entity.HistoryEntity
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class AddVideoViewModel(private val repository: VideoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AddVideoUiState())
    val uiState: StateFlow<AddVideoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPlaybackHistory()
                .map { history -> 
                    history.filter { it.type == VideoType.ONLINE.name }
                        .take(5) // Show last 5 URLs
                }
                .collect { history ->
                    _uiState.value = _uiState.value.copy(urlHistory = history)
                }
        }
    }

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(url = url, error = null)
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onThumbnailUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(thumbnailUrl = url)
    }

    fun saveVideo(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.url.isBlank()) {
            _uiState.value = state.copy(error = "URL cannot be empty")
            return
        }
        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "Title cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val video = Video(
                    id = UUID.randomUUID().toString(),
                    title = state.title,
                    url = state.url,
                    thumbnailUrl = state.thumbnailUrl.ifBlank { null },
                    type = VideoType.ONLINE,
                    dateAdded = System.currentTimeMillis()
                )
                repository.addVideo(video)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to save: ${e.message}")
            }
        }
    }

    class Factory(private val repository: VideoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddVideoViewModel(repository) as T
        }
    }
}

data class AddVideoUiState(
    val url: String = "",
    val title: String = "",
    val thumbnailUrl: String = "",
    val urlHistory: List<HistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
