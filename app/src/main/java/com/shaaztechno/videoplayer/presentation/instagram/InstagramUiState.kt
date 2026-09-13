package com.shaaztechno.videoplayer.presentation.instagram

import com.shaaztechno.videoplayer.domain.model.InstagramReel

sealed interface InstagramUiState {
    object Idle : InstagramUiState
    object Loading : InstagramUiState
    data class ReelReady(val reel: InstagramReel) : InstagramUiState
    data class Downloading(val progress: Float, val downloaded: Long, val total: Long) : InstagramUiState
    data class DownloadComplete(val videoId: String) : InstagramUiState
    data class Error(val message: String) : InstagramUiState
}
