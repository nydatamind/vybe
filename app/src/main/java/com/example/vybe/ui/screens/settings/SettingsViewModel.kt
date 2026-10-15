package com.example.vybe.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vybe.core.datastore.PreferencesManager
import com.example.vybe.core.model.AudioQuality
import com.example.vybe.core.model.ThemeMode
import com.example.vybe.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val audioQuality: StateFlow<AudioQuality> = preferencesManager.audioQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioQuality.HIGH)

    val isStreamWifiOnly: StateFlow<Boolean> = preferencesManager.isStreamWifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAuraSpatialAudio: StateFlow<Boolean> = preferencesManager.isAuraSpatialAudioEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val downloadFolderName: StateFlow<String?> = preferencesManager.downloadFolderName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val downloadTreeUri: StateFlow<String?> = preferencesManager.downloadTreeUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val downloadFolder: StateFlow<String> = preferencesManager.downloadFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setDownloadFolderTree(treeUri: String, folderName: String) {
        viewModelScope.launch {
            preferencesManager.setDownloadFolderTree(treeUri, folderName)
        }
    }

    fun setDownloadFolder(folder: String) {
        viewModelScope.launch {
            preferencesManager.setDownloadFolder(folder)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            preferencesManager.setAudioQuality(quality)
        }
    }

    fun setStreamWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            preferencesManager.setStreamWifiOnly(wifiOnly)
        }
    }

    val isAutoSkipIntroOutro: StateFlow<Boolean> = preferencesManager.isAutoSkipIntroOutroEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setAuraSpatialAudio(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAuraSpatialAudioEnabled(enabled)
        }
    }

    fun setAutoSkipIntroOutro(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoSkipIntroOutroEnabled(enabled)
        }
    }

    val isAudioDucking: StateFlow<Boolean> = preferencesManager.isAudioDuckingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setAudioDucking(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAudioDuckingEnabled(enabled)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            musicRepository.clearSearches()
        }
    }

    fun clearListeningHistory() {
        viewModelScope.launch {
            musicRepository.clearHistory()
        }
    }

    companion object {
        fun provideFactory(
            preferencesManager: PreferencesManager,
            musicRepository: MusicRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(preferencesManager, musicRepository) as T
                }
            }
    }
}
