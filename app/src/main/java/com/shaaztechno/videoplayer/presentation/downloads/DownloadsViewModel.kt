package com.shaaztechno.videoplayer.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.downloader.DownloadProgressItem
import com.shaaztechno.videoplayer.data.downloader.SZDownloadManager
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val repository: VideoRepository,
    private val downloadManager: SZDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getVideosByType(VideoType.DOWNLOADED),
                downloadManager.downloads
            ) { dbVideos, activeDownloads ->
                val completedIds = activeDownloads.filter { it.isCompleted }.map { it.id }.toSet()
                val activeOrFailed = activeDownloads.filter { !it.isCompleted }

                // Deduplicate videos: show active/failed items in activeDownloads, completed in dbVideos
                DownloadsUiState(
                    completedVideos = dbVideos.filter { !activeDownloads.any { active -> active.id == it.id && !active.isCompleted } },
                    activeDownloads = activeOrFailed,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun pauseDownload(id: String) {
        downloadManager.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        downloadManager.resumeDownload(id)
    }

    fun cancelDownload(id: String) {
        downloadManager.cancelDownload(id)
    }

    fun deleteDownload(video: Video) {
        viewModelScope.launch {
            downloadManager.cancelDownload(video.id)
            repository.deleteVideo(video.id)
        }
    }

    class Factory(
        private val repository: VideoRepository,
        private val downloadManager: SZDownloadManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DownloadsViewModel(repository, downloadManager) as T
        }
    }
}

data class DownloadsUiState(
    val completedVideos: List<Video> = emptyList(),
    val activeDownloads: List<DownloadProgressItem> = emptyList(),
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean get() = completedVideos.isEmpty() && activeDownloads.isEmpty()
}
