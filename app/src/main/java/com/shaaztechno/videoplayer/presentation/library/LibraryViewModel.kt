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
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: VideoRepository,
    private val playlistDao: PlaylistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var allLocalVideos: List<Video> = emptyList()

    init {
        viewModelScope.launch {
            repository.getVideosByType(VideoType.LOCAL).collect { videos ->
                allLocalVideos = videos
                updateSortedVideos()
            }
        }
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collect { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
            }
        }
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(isFolderView = !_uiState.value.isFolderView)
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = sortOrder)
        updateSortedVideos()
    }

    private fun updateSortedVideos() {
        val order = _uiState.value.sortOrder
        val sorted = when (order) {
            SortOrder.NAME -> allLocalVideos.sortedBy { it.title.lowercase() }
            SortOrder.DATE -> allLocalVideos.sortedByDescending { it.dateAdded }
            SortOrder.SIZE -> allLocalVideos.sortedByDescending { it.size }
        }
        _uiState.value = _uiState.value.copy(
            videos = sorted,
            folders = sorted.groupBy { it.folder ?: "Internal Storage" },
            isLoading = false
        )
    }

    fun renameVideo(video: Video, newTitle: String) {
        viewModelScope.launch {
            val updatedVideo = video.copy(title = newTitle)
            repository.addVideo(updatedVideo)
        }
    }

    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            repository.deleteVideo(video.id)
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.refreshLocalVideos()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    class Factory(
        private val repository: VideoRepository,
        private val playlistDao: PlaylistDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository, playlistDao) as T
        }
    }
}

data class LibraryUiState(
    val videos: List<Video> = emptyList(),
    val folders: Map<String, List<Video>> = emptyMap(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val isFolderView: Boolean = true,
    val sortOrder: SortOrder = SortOrder.NAME,
    val isLoading: Boolean = true
)

enum class SortOrder {
    NAME, DATE, SIZE
}
