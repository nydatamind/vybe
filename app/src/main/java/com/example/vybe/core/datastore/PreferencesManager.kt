package com.example.vybe.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.vybe.core.model.AudioQuality
import com.example.vybe.core.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vybe_preferences")

class PreferencesManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("vybe_fast_prefs", Context.MODE_PRIVATE)

    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
    private val KEY_STREAM_WIFI_ONLY = booleanPreferencesKey("stream_wifi_only")
    private val KEY_FIRST_RUN_COMPLETED = booleanPreferencesKey("first_run_completed")
    private val KEY_FADE_OUT_SLEEP_TIMER = booleanPreferencesKey("fade_out_sleep_timer")
    private val KEY_DOWNLOAD_TREE_URI = stringPreferencesKey("download_tree_uri")
    private val KEY_DOWNLOAD_FOLDER_NAME = stringPreferencesKey("download_folder_name")
    private val KEY_AURA_SPATIAL_AUDIO = booleanPreferencesKey("aura_spatial_audio")
    private val KEY_SELECTED_LANGUAGES = stringSetPreferencesKey("selected_languages")
    private val KEY_SELECTED_ARTISTS = stringSetPreferencesKey("selected_artists")
    private val KEY_AUTO_SKIP_INTRO_OUTRO = booleanPreferencesKey("auto_skip_intro_outro")
    private val KEY_AUDIO_DUCKING_ENABLED = booleanPreferencesKey("audio_ducking_enabled")
    private val KEY_AUDIO_DUCKING_VOLUME = floatPreferencesKey("audio_ducking_volume")
    private val KEY_PLAYER_THEME_INDEX = intPreferencesKey("player_theme_index")

    init {
        // Asynchronously observe and sync DataStore into fast SharedPreferences
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                context.dataStore.data.collect { preferences ->
                    val completed = preferences[KEY_FIRST_RUN_COMPLETED] == true || (preferences[KEY_SELECTED_LANGUAGES]?.isNotEmpty() == true)
                    if (completed) {
                        sharedPrefs.edit().putBoolean("first_run_completed", true).apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isFirstRunCompletedSync(): Boolean {
        if (sharedPrefs.getBoolean("first_run_completed", false)) return true
        // Fallback check to avoid any race condition on existing user data
        val oldPrefs = context.getSharedPreferences("vybe_preferences", Context.MODE_PRIVATE)
        if (oldPrefs.getBoolean("first_run_completed", false)) {
            sharedPrefs.edit().putBoolean("first_run_completed", true).apply()
            return true
        }
        return false
    }

    val playerThemeIndex: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_PLAYER_THEME_INDEX] ?: 0
    }

    val isAutoSkipIntroOutroEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_SKIP_INTRO_OUTRO] ?: true
    }

    val isAudioDuckingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUDIO_DUCKING_ENABLED] ?: true
    }

    val audioDuckingVolume: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUDIO_DUCKING_VOLUME] ?: 0.0f
    }

    val selectedLanguages: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_LANGUAGES] ?: emptySet()
    }

    val selectedArtists: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_ARTISTS] ?: emptySet()
    }

    val downloadTreeUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_DOWNLOAD_TREE_URI]?.ifBlank { null }
    }

    val downloadFolderName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_DOWNLOAD_FOLDER_NAME]?.ifBlank { null }
    }

    // Retained for backward compatibility
    val downloadFolder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_DOWNLOAD_FOLDER_NAME]?.ifBlank { null } ?: ""
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.SYSTEM.name -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK // Dark is default
        }
    }

    val audioQuality: Flow<AudioQuality> = context.dataStore.data.map { preferences ->
        when (preferences[KEY_AUDIO_QUALITY]) {
            AudioQuality.LOW.name -> AudioQuality.LOW
            AudioQuality.HIGH.name -> AudioQuality.HIGH
            AudioQuality.ULTRA.name -> AudioQuality.ULTRA
            else -> AudioQuality.HIGH // High quality default
        }
    }

    val isFirstRunCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_FIRST_RUN_COMPLETED] ?: false
    }

    val isStreamWifiOnly: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_STREAM_WIFI_ONLY] ?: false
    }

    val isAuraSpatialAudioEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AURA_SPATIAL_AUDIO] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUDIO_QUALITY] = quality.name
        }
    }

    suspend fun setFirstRunCompleted(completed: Boolean = true) {
        sharedPrefs.edit().putBoolean("first_run_completed", completed).apply()
        context.dataStore.edit { preferences ->
            preferences[KEY_FIRST_RUN_COMPLETED] = completed
        }
    }

    suspend fun setStreamWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STREAM_WIFI_ONLY] = wifiOnly
        }
    }

    suspend fun setAuraSpatialAudioEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AURA_SPATIAL_AUDIO] = enabled
        }
    }

    suspend fun setFadeOutSleepTimer(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FADE_OUT_SLEEP_TIMER] = enabled
        }
    }

    suspend fun setAutoSkipIntroOutroEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_SKIP_INTRO_OUTRO] = enabled
        }
    }

    suspend fun setAudioDuckingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUDIO_DUCKING_ENABLED] = enabled
        }
    }

    suspend fun setAudioDuckingVolume(volume: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUDIO_DUCKING_VOLUME] = volume
        }
    }

    suspend fun setDownloadFolderTree(treeUri: String, folderName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DOWNLOAD_TREE_URI] = treeUri
            preferences[KEY_DOWNLOAD_FOLDER_NAME] = folderName
        }
    }

    suspend fun clearDownloadFolder() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_DOWNLOAD_TREE_URI)
            preferences.remove(KEY_DOWNLOAD_FOLDER_NAME)
        }
    }

    suspend fun setDownloadFolder(folder: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DOWNLOAD_FOLDER_NAME] = folder
        }
    }

    suspend fun setPlayerThemeIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PLAYER_THEME_INDEX] = index
        }
    }

    suspend fun saveUserMusicPreferences(languages: Set<String>, artists: Set<String>) {
        sharedPrefs.edit().putBoolean("first_run_completed", true).apply()
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_LANGUAGES] = languages
            preferences[KEY_SELECTED_ARTISTS] = artists
            preferences[KEY_FIRST_RUN_COMPLETED] = true
        }
    }
}
