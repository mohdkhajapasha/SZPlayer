package com.shaaztechno.videoplayer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.local.SettingsDataStore
import com.shaaztechno.videoplayer.data.local.UserSettings
import com.shaaztechno.videoplayer.domain.model.UpdateInfo
import com.shaaztechno.videoplayer.domain.repository.UpdateRepository
import com.shaaztechno.videoplayer.domain.repository.UpdateStatus
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class UpdateUIState {
    object Idle : UpdateUIState()
    object Checking : UpdateUIState()
    data class UpToDate(val isManual: Boolean) : UpdateUIState()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateUIState()
    data class Error(val message: String) : UpdateUIState()
    data class Downloading(val updateInfo: UpdateInfo, val progress: Int) : UpdateUIState()
    data class Downloaded(val updateInfo: UpdateInfo, val apkFile: File) : UpdateUIState()
}

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val repository: VideoRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _updateState = MutableStateFlow<UpdateUIState>(UpdateUIState.Idle)
    val updateState: StateFlow<UpdateUIState> = _updateState.asStateFlow()
    
    private var downloadJob: Job? = null

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = UpdateUIState.Checking
            updateRepository.checkForUpdates()
                .onSuccess { status ->
                    _updateState.value = when (status) {
                        is UpdateStatus.UpdateAvailable -> UpdateUIState.UpdateAvailable(status.updateInfo)
                        is UpdateStatus.UpToDate -> UpdateUIState.UpToDate(isManual)
                    }
                }
                .onFailure {
                    _updateState.value = UpdateUIState.Error(it.message ?: "Failed to check for updates")
                }
        }
    }

    fun startDownload(updateInfo: UpdateInfo) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _updateState.value = UpdateUIState.Downloading(updateInfo, 0)
            
            val progressCollectionJob = launch {
                updateRepository.getDownloadProgress().collect { progress ->
                    _updateState.value = UpdateUIState.Downloading(updateInfo, progress)
                }
            }

            val fileName = "SZPlayer_${updateInfo.versionName}.apk"
            updateRepository.downloadApk(updateInfo.apkUrl, fileName)
                .onSuccess { file ->
                    progressCollectionJob.cancel()
                    _updateState.value = UpdateUIState.Downloaded(updateInfo, file)
                }
                .onFailure {
                    progressCollectionJob.cancel()
                    _updateState.value = UpdateUIState.Error(it.message ?: "Download failed")
                }
        }
    }

    fun resetUpdateState() {
        downloadJob?.cancel()
        _updateState.value = UpdateUIState.Idle
    }

    /** Launches a [settingsDataStore] write on the ViewModel scope — shared by all update functions. */
    private fun updateSetting(block: suspend SettingsDataStore.() -> Unit) {
        viewModelScope.launch { settingsDataStore.block() }
    }

    fun updateAutoPlayNext(value: Boolean) = updateSetting { updateAutoPlayNext(value) }

    fun updateResumePlayback(value: Boolean) = updateSetting { updateResumePlayback(value) }

    fun updateKeepScreenAwake(value: Boolean) = updateSetting { updateKeepScreenAwake(value) }

    fun updateDarkMode(value: String) = updateSetting { updateDarkMode(value) }

    fun updateDefaultOrientation(value: String) = updateSetting { updateDefaultOrientation(value) }

    fun updateBrightnessGestureEnabled(value: Boolean) = updateSetting { updateBrightnessGestureEnabled(value) }

    fun updateVolumeGestureEnabled(value: Boolean) = updateSetting { updateVolumeGestureEnabled(value) }

    fun updateSeekingGestureEnabled(value: Boolean) = updateSetting { updateSeekingGestureEnabled(value) }

    fun updateBackgroundPlaybackEnabled(value: Boolean) = updateSetting { updateBackgroundPlaybackEnabled(value) }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun clearOnlineCatalog() {
        viewModelScope.launch { repository.clearOnlineCatalog() }
    }

    class Factory(
        private val settingsDataStore: SettingsDataStore,
        private val repository: VideoRepository,
        private val updateRepository: UpdateRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsDataStore, repository, updateRepository) as T
        }
    }
}
