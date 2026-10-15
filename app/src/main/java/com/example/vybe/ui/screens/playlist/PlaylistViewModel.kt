package com.example.vybe.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vybe.core.model.Playlist
import com.example.vybe.core.model.Track
import com.example.vybe.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = true
)

class PlaylistViewModel(
    private val playlistId: String,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    val tracks: StateFlow<List<Track>> = musicRepository.getTracksForPlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadPlaylist()
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val pl = musicRepository.getPlaylistById(playlistId)
            _uiState.update { it.copy(playlist = pl, isLoading = false) }
        }
    }

    fun addTrack(track: Track) {
        viewModelScope.launch {
            musicRepository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun removeTrack(trackId: String) {
        viewModelScope.launch {
            musicRepository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        val currentList = tracks.value.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) return
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        viewModelScope.launch {
            musicRepository.reorderPlaylistTracks(playlistId, currentList.map { it.id })
        }
    }

    fun moveTrackUp(trackId: String) {
        val currentList = tracks.value
        val index = currentList.indexOfFirst { it.id == trackId }
        if (index > 0) {
            moveTrack(index, index - 1)
        }
    }

    fun moveTrackDown(trackId: String) {
        val currentList = tracks.value
        val index = currentList.indexOfFirst { it.id == trackId }
        if (index in 0 until currentList.size - 1) {
            moveTrack(index, index + 1)
        }
    }

    suspend fun searchSongs(query: String): List<Track> {
        return if (query.isBlank()) {
            try {
                val trending = musicRepository.getTrending()
                if (trending.isNotEmpty()) trending else musicRepository.getPersonalizedRecommendations()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            try {
                musicRepository.search(query).tracks
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
            onDeleted()
        }
    }

    fun renamePlaylist(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            musicRepository.renamePlaylist(playlistId, newName.trim())
            loadPlaylist()
        }
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            musicRepository.toggleLike(track)
        }
    }

    companion object {
        fun provideFactory(playlistId: String, musicRepository: MusicRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlaylistViewModel(playlistId, musicRepository) as T
                }
            }
    }
}
