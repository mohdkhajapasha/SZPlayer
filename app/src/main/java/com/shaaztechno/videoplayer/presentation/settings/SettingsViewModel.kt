package com.shaaztechno.videoplayer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.data.local.SettingsDataStore
import com.shaaztechno.videoplayer.data.local.UserSettings
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val repository: VideoRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateAutoPlayNext(value: Boolean) {
        viewModelScope.launch { settingsDataStore.updateAutoPlayNext(value) }
    }

    fun updateResumePlayback(value: Boolean) {
        viewModelScope.launch { settingsDataStore.updateResumePlayback(value) }
    }

    fun updateKeepScreenAwake(value: Boolean) {
        viewModelScope.launch { settingsDataStore.updateKeepScreenAwake(value) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun updateDarkMode(value: String) {
        viewModelScope.launch { settingsDataStore.updateDarkMode(value) }
    }

    fun updateDefaultOrientation(value: String) {
        viewModelScope.launch { settingsDataStore.updateDefaultOrientation(value) }
    }

    fun updateBrightnessGestureEnabled(value: Boolean) {
        viewModelScope.launch { settingsDataStore.updateBrightnessGestureEnabled(value) }
    }

    fun updateVolumeGestureEnabled(value: Boolean) {
        viewModelScope.launch { settingsDataStore.updateVolumeGestureEnabled(value) }
    }

    fun updateSeekingGestureEnabled(value: Boolean) {
        viewModelScope.launch { settingsDataStore.updateSeekingGestureEnabled(value) }
    }

    fun updateBackgroundPlaybackEnabled(value: Boolean) {
        viewModelScope.launch { settingsDataStore.updateBackgroundPlaybackEnabled(value) }
    }

    class Factory(
        private val settingsDataStore: SettingsDataStore,
        private val repository: VideoRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsDataStore, repository) as T
        }
    }
}
