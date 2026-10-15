package com.example.vybe.data.provider

import com.example.vybe.core.model.Album
import com.example.vybe.core.model.Artist
import com.example.vybe.core.model.SearchResult
import com.example.vybe.core.model.Track

/**
 * Clean abstraction for audio content providers.
 * Decouples the UI and playback engines completely from specific music data sources.
 */
interface AudioProvider {
    val name: String

    suspend fun search(query: String): SearchResult

    suspend fun getTrack(id: String): Track?

    suspend fun getArtist(id: String): Artist?

    suspend fun getAlbum(id: String): Album?

    suspend fun getTrending(): List<Track>

    suspend fun getNewReleases(): List<Album>

    suspend fun getPopularArtists(): List<Artist>

    suspend fun getRecommendations(seedGenres: List<String> = emptyList()): List<Track>
    
    suspend fun getSongRecommendations(trackId: String): List<Track> = emptyList()

    suspend fun getTracksByGenre(genre: String): List<Track>
    
    suspend fun getTracksForArtist(
        artistId: String,
        artistName: String? = null,
        onProgress: ((List<Track>) -> Unit)? = null
    ): List<Track> = emptyList()

    fun getStreamUrl(track: Track): String?

    fun getCachedSearch(query: String): SearchResult? = null

    fun prewarm() {}
}

