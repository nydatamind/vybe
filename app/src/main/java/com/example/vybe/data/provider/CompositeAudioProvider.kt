package com.example.vybe.data.provider

import com.example.vybe.core.model.Album
import com.example.vybe.core.model.Artist
import com.example.vybe.core.model.SearchResult
import com.example.vybe.core.model.Track
import com.example.vybe.data.repository.distinctSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Vibra Primary Audio Provider powered natively by JioSaavn Engine.
 */
class CompositeAudioProvider(
    private val saavnProvider: AudioProvider = JioSaavnAudioProvider()
) : AudioProvider {

    override val name: String = "VYBEPrimaryProvider"

    override fun getCachedSearch(query: String): SearchResult? {
        return saavnProvider.getCachedSearch(query)
    }

    override fun prewarm() {
        saavnProvider.prewarm()
    }

    override suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        val saavnResult = try {
            saavnProvider.search(query)
        } catch (e: Exception) {
            SearchResult()
        }

        SearchResult(
            tracks = saavnResult.tracks,
            artists = saavnResult.artists,
            albums = saavnResult.albums,
            playlists = emptyList()
        )
    }

    override suspend fun getTrack(id: String): Track? = withContext(Dispatchers.IO) {
        saavnProvider.getTrack(id)
            ?: CuratedAudioCatalog.tracks.find { it.id == id }
    }

    override suspend fun getArtist(id: String): Artist? = withContext(Dispatchers.IO) {
        saavnProvider.getArtist(id)
            ?: CuratedAudioCatalog.artists.find { it.id == id }
    }

    override suspend fun getAlbum(id: String): Album? = withContext(Dispatchers.IO) {
        saavnProvider.getAlbum(id)
            ?: CuratedAudioCatalog.albums.find { it.id == id }?.let { album ->
                album.copy(tracks = CuratedAudioCatalog.getTracksForAlbum(album.id))
            }
    }

    override suspend fun getTrending(): List<Track> = withContext(Dispatchers.IO) {
        try {
            saavnProvider.getTrending().ifEmpty { CuratedAudioCatalog.tracks }
        } catch (e: Exception) {
            CuratedAudioCatalog.tracks
        }
    }

    override suspend fun getNewReleases(): List<Album> = withContext(Dispatchers.IO) {
        try {
            saavnProvider.getNewReleases().ifEmpty { CuratedAudioCatalog.albums }
        } catch (e: Exception) {
            CuratedAudioCatalog.albums
        }
    }

    override suspend fun getPopularArtists(): List<Artist> = withContext(Dispatchers.IO) {
        try {
            saavnProvider.getPopularArtists().ifEmpty { CuratedAudioCatalog.artists }
        } catch (e: Exception) {
            CuratedAudioCatalog.artists
        }
    }

    override suspend fun getRecommendations(seedGenres: List<String>): List<Track> = withContext(Dispatchers.IO) {
        try {
            saavnProvider.getRecommendations(seedGenres)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getSongRecommendations(trackId: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            saavnProvider.getSongRecommendations(trackId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTracksByGenre(genre: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            saavnProvider.getTracksByGenre(genre)
        } catch (e: Exception) {
            CuratedAudioCatalog.tracks.filter { it.genre.equals(genre, ignoreCase = true) }
        }
    }

    override suspend fun getTracksForArtist(
        artistId: String,
        artistName: String?,
        onProgress: ((List<Track>) -> Unit)?
    ): List<Track> = withContext(Dispatchers.IO) {
        val curated = CuratedAudioCatalog.getTracksForArtist(artistId)
        val saavn = try {
            saavnProvider.getTracksForArtist(artistId, artistName) { partial ->
                val combinedPartial = (partial + curated).distinctSongs()
                onProgress?.invoke(combinedPartial)
            }
        } catch (e: Exception) {
            emptyList()
        }
        val combined = (saavn + curated).distinctSongs()
        if (combined.isNotEmpty()) combined else curated
    }

    override fun getStreamUrl(track: Track): String? {
        return saavnProvider.getStreamUrl(track)
    }
}
