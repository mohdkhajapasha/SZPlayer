package com.shaaztechno.videoplayer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val SEEK_DURATION = longPreferencesKey("seek_duration")
        val DARK_MODE = stringPreferencesKey("dark_mode") // "light", "dark", "system"
        val DEFAULT_ORIENTATION = stringPreferencesKey("default_orientation") // "auto", "landscape", "portrait"
        val BRIGHTNESS_GESTURE_ENABLED = booleanPreferencesKey("brightness_gesture_enabled")
        val VOLUME_GESTURE_ENABLED = booleanPreferencesKey("volume_gesture_enabled")
        val SEEKING_GESTURE_ENABLED = booleanPreferencesKey("seeking_gesture_enabled")
        val BACKGROUND_PLAYBACK_ENABLED = booleanPreferencesKey("background_playback_enabled")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            autoPlayNext = preferences[AUTO_PLAY_NEXT] ?: true,
            resumePlayback = preferences[RESUME_PLAYBACK] ?: true,
            defaultPlaybackSpeed = preferences[DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
            keepScreenAwake = preferences[KEEP_SCREEN_AWAKE] ?: true,
            seekDuration = preferences[SEEK_DURATION] ?: 10000L,
            darkMode = preferences[DARK_MODE] ?: "system",
            defaultOrientation = preferences[DEFAULT_ORIENTATION] ?: "auto",
            brightnessGestureEnabled = preferences[BRIGHTNESS_GESTURE_ENABLED] ?: true,
            volumeGestureEnabled = preferences[VOLUME_GESTURE_ENABLED] ?: true,
            seekingGestureEnabled = preferences[SEEKING_GESTURE_ENABLED] ?: true,
            backgroundPlaybackEnabled = preferences[BACKGROUND_PLAYBACK_ENABLED] ?: false
        )
    }

    suspend fun updateAutoPlayNext(value: Boolean) {
        context.dataStore.edit { it[AUTO_PLAY_NEXT] = value }
    }

    suspend fun updateResumePlayback(value: Boolean) {
        context.dataStore.edit { it[RESUME_PLAYBACK] = value }
    }

    suspend fun updatePlaybackSpeed(value: Float) {
        context.dataStore.edit { it[DEFAULT_PLAYBACK_SPEED] = value }
    }

    suspend fun updateKeepScreenAwake(value: Boolean) {
        context.dataStore.edit { it[KEEP_SCREEN_AWAKE] = value }
    }

    suspend fun updateSeekDuration(value: Long) {
        context.dataStore.edit { it[SEEK_DURATION] = value }
    }

    suspend fun updateDarkMode(value: String) {
        context.dataStore.edit { it[DARK_MODE] = value }
    }

    suspend fun updateDefaultOrientation(value: String) {
        context.dataStore.edit { it[DEFAULT_ORIENTATION] = value }
    }

    suspend fun updateBrightnessGestureEnabled(value: Boolean) {
        context.dataStore.edit { it[BRIGHTNESS_GESTURE_ENABLED] = value }
    }

    suspend fun updateVolumeGestureEnabled(value: Boolean) {
        context.dataStore.edit { it[VOLUME_GESTURE_ENABLED] = value }
    }

    suspend fun updateSeekingGestureEnabled(value: Boolean) {
        context.dataStore.edit { it[SEEKING_GESTURE_ENABLED] = value }
    }

    suspend fun updateBackgroundPlaybackEnabled(value: Boolean) {
        context.dataStore.edit { it[BACKGROUND_PLAYBACK_ENABLED] = value }
    }
}

data class UserSettings(
    val autoPlayNext: Boolean,
    val resumePlayback: Boolean,
    val defaultPlaybackSpeed: Float,
    val keepScreenAwake: Boolean,
    val seekDuration: Long,
    val darkMode: String,
    val defaultOrientation: String,
    val brightnessGestureEnabled: Boolean,
    val volumeGestureEnabled: Boolean,
    val seekingGestureEnabled: Boolean,
    val backgroundPlaybackEnabled: Boolean
)
