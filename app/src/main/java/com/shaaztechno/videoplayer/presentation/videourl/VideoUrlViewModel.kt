package com.shaaztechno.videoplayer.presentation.videourl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.downloader.SZDownloadManager
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoMediaInfo
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import com.shaaztechno.videoplayer.domain.usecase.CheckVideoUrlUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface VideoUrlUiState {
    data object Idle : VideoUrlUiState
    data object Checking : VideoUrlUiState
    data class Success(val mediaInfo: VideoMediaInfo) : VideoUrlUiState
    data class Error(val message: String) : VideoUrlUiState
}

class VideoUrlViewModel(
    private val checkVideoUrlUseCase: CheckVideoUrlUseCase,
    private val videoRepository: VideoRepository,
    private val downloadManager: SZDownloadManager
) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _uiState = MutableStateFlow<VideoUrlUiState>(VideoUrlUiState.Idle)
    val uiState: StateFlow<VideoUrlUiState> = _uiState.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
        if (_uiState.value !is VideoUrlUiState.Idle) {
            _uiState.value = VideoUrlUiState.Idle
        }
    }

    fun onClearUrl() {
        _urlInput.value = ""
        _uiState.value = VideoUrlUiState.Idle
    }

    fun onPasteUrl(pastedText: String) {
        _urlInput.value = pastedText.trim()
        if (_uiState.value !is VideoUrlUiState.Idle) {
            _uiState.value = VideoUrlUiState.Idle
        }
    }

    fun checkUrl() {
        val currentUrl = _urlInput.value.trim()
        if (currentUrl.isBlank()) return
        if (_uiState.value is VideoUrlUiState.Checking) return // Prevent duplicate concurrent checks

        _uiState.value = VideoUrlUiState.Checking

        viewModelScope.launch {
            val result = checkVideoUrlUseCase(currentUrl)
            result.onSuccess { info ->
                _uiState.value = VideoUrlUiState.Success(info)
            }.onFailure { error ->
                val errorMessage = error.message ?: "Unable to access video. The URL may be invalid or expired."
                _uiState.value = VideoUrlUiState.Error(errorMessage)
            }
        }
    }

    fun playVideo(onNavigateToPlayer: (String) -> Unit) {
        val successState = _uiState.value as? VideoUrlUiState.Success ?: return
        val info = successState.mediaInfo

        viewModelScope.launch {
            val videoId = UUID.randomUUID().toString()
            val video = Video(
                id = videoId,
                title = info.title,
                url = info.url,
                duration = info.duration ?: 0L,
                size = info.fileSize ?: 0L,
                type = VideoType.ONLINE,
                dateAdded = System.currentTimeMillis()
            )
            videoRepository.addVideo(video)
            onNavigateToPlayer(videoId)
        }
    }

    fun downloadVideo(onNavigateToDownloads: () -> Unit) {
        val successState = _uiState.value as? VideoUrlUiState.Success ?: return
        val info = successState.mediaInfo
        if (!info.isDownloadable) return

        val videoId = UUID.randomUUID().toString()
        downloadManager.startDownload(
            id = videoId,
            url = info.url,
            title = info.title,
            mimeType = info.mimeType
        )
        onNavigateToDownloads()
    }

    class Factory(
        private val checkVideoUrlUseCase: CheckVideoUrlUseCase,
        private val videoRepository: VideoRepository,
        private val downloadManager: SZDownloadManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VideoUrlViewModel(checkVideoUrlUseCase, videoRepository, downloadManager) as T
        }
    }
}
