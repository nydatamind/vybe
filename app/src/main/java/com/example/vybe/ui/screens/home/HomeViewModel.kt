package com.example.vybe.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vybe.core.model.Album
import com.example.vybe.core.model.Artist
import com.example.vybe.core.model.Track
import com.example.vybe.data.provider.CuratedAudioCatalog
import com.example.vybe.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

private fun getInitialGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..22 -> "Good evening"
        else -> "Good night"
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val greeting: String = getInitialGreeting(),
    val madeForYouTracks: List<Track> = emptyList(),
    val trendingTracks: List<Track> = CuratedAudioCatalog.tracks,
    val newReleases: List<Album> = CuratedAudioCatalog.albums,
    val popularArtists: List<Artist> = CuratedAudioCatalog.artists,
    val recommendedTracks: List<Track> = CuratedAudioCatalog.tracks.take(10),
    val madeForYouMixes: Map<String, List<Track>> = emptyMap(),
    val popularAlbums: List<Album> = CuratedAudioCatalog.albums
)

class HomeViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val recentlyPlayed = musicRepository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedTracks = musicRepository.mostPlayedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadHomeData()
        viewModelScope.launch {
            musicRepository.mostPlayedTracks.collect { tracks ->
                _uiState.update { it.copy(madeForYouTracks = tracks) }
            }
        }
    }

    fun loadHomeData() {
        val greeting = computeGreeting()
        _uiState.update { it.copy(greeting = greeting) }

        // Fetch each section in background without blocking the UI or showing full-screen skeletons
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                try {
                    val trending = musicRepository.getTrendingTracks()
                    if (trending.isNotEmpty()) {
                        _uiState.update { it.copy(trendingTracks = trending) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            launch {
                try {
                    val madeForYou = musicRepository.getMostPlayedTracks()
                    _uiState.update { it.copy(madeForYouTracks = madeForYou) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            launch {
                try {
                    val newReleases = musicRepository.getNewReleases()
                    if (newReleases.isNotEmpty()) {
                        _uiState.update { it.copy(newReleases = newReleases, popularAlbums = newReleases) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            launch {
                try {
                    val artists = musicRepository.getPopularArtists()
                    if (artists.isNotEmpty()) {
                        _uiState.update { it.copy(popularArtists = artists) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            launch {
                try {
                    val recommended = musicRepository.getPersonalizedRecommendations()
                    if (recommended.isNotEmpty()) {
                        _uiState.update { it.copy(recommendedTracks = recommended) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            launch {
                try {
                    val mixes = musicRepository.getMadeForYouMixes()
                    if (mixes.isNotEmpty()) {
                        _uiState.update { it.copy(madeForYouMixes = mixes) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun computeGreeting(): String {
        return getInitialGreeting()
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            musicRepository.toggleLike(track)
            val updated = musicRepository.getPersonalizedRecommendations()
            if (updated.isNotEmpty()) {
                _uiState.update { it.copy(recommendedTracks = updated) }
            }
        }
    }

    companion object {
        fun provideFactory(musicRepository: MusicRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(musicRepository) as T
                }
            }
    }
}
