package com.example.vybe.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vybe.core.model.SearchResult
import com.example.vybe.core.model.Track
import com.example.vybe.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchFilter {
    ALL,
    SONGS,
    ARTISTS,
    ALBUMS
}

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val hasCompletedSearch: Boolean = false,
    val searchResult: SearchResult = SearchResult(),
    val selectedFilter: SearchFilter = SearchFilter.ALL
)

class SearchViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val recentSearches = musicRepository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    init {
        // High-Speed Pre-warm search engine connections in background
        viewModelScope.launch(Dispatchers.IO) {
            musicRepository.prewarmSearch()
        }
    }

    fun onQueryChanged(newQuery: String) {
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    query = "",
                    isSearching = false,
                    hasCompletedSearch = false,
                    searchResult = SearchResult()
                )
            }
            return
        }

        // Instant local/cache lookup (0ms perceived latency)
        val instantMatch = musicRepository.searchLocal(newQuery)
        val hasInstant = !instantMatch.isEmpty

        _uiState.update {
            it.copy(
                query = newQuery,
                isSearching = true,
                hasCompletedSearch = hasInstant,
                searchResult = if (hasInstant) instantMatch else it.searchResult
            )
        }

        searchJob = viewModelScope.launch {
            delay(100) // Ultra-snappy 100ms debounce
            val result = musicRepository.search(newQuery)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    hasCompletedSearch = true,
                    searchResult = result
                )
            }
        }
    }

    fun triggerImmediateSearch() {
        val currentQuery = _uiState.value.query.trim()
        if (currentQuery.isBlank()) return
        searchJob?.cancel()
        _uiState.update { it.copy(isSearching = true, hasCompletedSearch = false) }
        searchJob = viewModelScope.launch {
            val result = musicRepository.search(currentQuery)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    hasCompletedSearch = true,
                    searchResult = result
                )
            }
        }
    }

    fun onFilterSelected(filter: SearchFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun searchCategory(category: String) {
        onQueryChanged(category)
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                isSearching = false,
                hasCompletedSearch = false,
                searchResult = SearchResult()
            )
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            musicRepository.deleteSearch(query)
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            musicRepository.clearSearches()
        }
    }

    suspend fun searchDirect(query: String): SearchResult {
        return musicRepository.search(query)
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            musicRepository.toggleLike(track)
        }
    }

    companion object {
        fun provideFactory(musicRepository: MusicRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(musicRepository) as T
                }
            }
    }
}
