package com.shaaztechno.videoplayer.presentation.instagram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.downloader.SZDownloadManager
import com.shaaztechno.videoplayer.domain.usecase.GetInstagramReelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class InstagramViewModel(
    private val getInstagramReelUseCase: GetInstagramReelUseCase,
    private val downloadManager: SZDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<InstagramUiState>(InstagramUiState.Idle)
    val uiState: StateFlow<InstagramUiState> = _uiState.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    fun onUrlChange(newUrl: String) {
        _urlInput.value = newUrl
    }

    fun fetchReel() {
        val url = _urlInput.value
        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.value = InstagramUiState.Loading
            getInstagramReelUseCase(url)
                .onSuccess { reel ->
                    _uiState.value = InstagramUiState.ReelReady(reel)
                }
                .onFailure { error ->
                    _uiState.value = InstagramUiState.Error(error.message ?: "Failed to fetch Reel")
                }
        }
    }

    fun downloadReel() {
        val currentState = _uiState.value
        if (currentState is InstagramUiState.ReelReady) {
            val reel = currentState.reel
            val downloadId = UUID.randomUUID().toString()
            
            downloadManager.startDownload(
                id = downloadId,
                url = reel.videoUrl,
                title = reel.caption ?: "Instagram Reel by ${reel.username ?: "Unknown"}",
                mimeType = "video/mp4"
            )

            observeDownloadProgress(downloadId)
        }
    }

    private fun observeDownloadProgress(downloadId: String) {
        viewModelScope.launch {
            downloadManager.downloads.collectLatest { downloads ->
                val download = downloads.find { it.id == downloadId }
                if (download != null) {
                    if (download.isCompleted) {
                        _uiState.value = InstagramUiState.DownloadComplete(downloadId)
                    } else if (download.isFailed) {
                        _uiState.value = InstagramUiState.Error("Download failed")
                    } else {
                        _uiState.value = InstagramUiState.Downloading(
                            progress = download.percentage / 100f,
                            downloaded = download.bytesDownloaded,
                            total = download.totalBytes
                        )
                    }
                }
            }
        }
    }

    fun reset() {
        _uiState.value = InstagramUiState.Idle
        _urlInput.value = ""
    }

    class Factory(
        private val getInstagramReelUseCase: GetInstagramReelUseCase,
        private val downloadManager: SZDownloadManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InstagramViewModel(getInstagramReelUseCase, downloadManager) as T
        }
    }
}
