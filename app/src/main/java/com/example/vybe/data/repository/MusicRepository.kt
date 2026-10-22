package com.example.vybe.data.repository

import com.example.vybe.core.database.LikedTrackDao
import com.example.vybe.core.database.LikedTrackEntity
import com.example.vybe.core.database.PlaybackHistoryDao
import com.example.vybe.core.database.PlaybackHistoryEntity
import com.example.vybe.core.database.PlaylistDao
import com.example.vybe.core.database.PlaylistEntity
import com.example.vybe.core.database.PlaylistTrackCrossRef
import com.example.vybe.core.database.RecentSearchDao
import com.example.vybe.core.database.RecentSearchEntity
import com.example.vybe.core.database.SavedArtistDao
import com.example.vybe.core.database.SavedArtistEntity
import com.example.vybe.core.database.TrackDao
import com.example.vybe.core.database.TrackEntity
import com.example.vybe.core.model.Album
import com.example.vybe.core.model.Artist
import com.example.vybe.core.model.Playlist
import com.example.vybe.core.model.SearchResult
import com.example.vybe.core.model.Track
import com.example.vybe.data.provider.AudioProvider
import com.example.vybe.data.provider.CompositeAudioProvider
import com.example.vybe.data.provider.CuratedAudioCatalog
import com.example.vybe.data.provider.OnboardingCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MusicRepository(
    private val audioProvider: AudioProvider = CompositeAudioProvider(),
    private val trackDao: TrackDao,
    private val likedTrackDao: LikedTrackDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: PlaybackHistoryDao,
    private val recentSearchDao: RecentSearchDao,
    private val savedArtistDao: SavedArtistDao? = null,
    val downloadRepository: DownloadRepository,
    val recommendationEngine: RecommendationEngine = RecommendationEngine()
) {

    private val artistCache = ConcurrentHashMap<String, Artist>()

    // Fast in-memory cache with 5-minute TTL for Home Screen
    private var cachedTrending: Pair<Long, List<Track>>? = null
    private var cachedNewReleases: Pair<Long, List<Album>>? = null
    private var cachedPopularArtists: Pair<Long, List<Artist>>? = null
    private var cachedRecommendations: Pair<Long, List<Track>>? = null
    private var cachedMixes: Pair<Long, Map<String, List<Track>>>? = null
    private val HOME_CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes

    // Liked Tracks Flow
    val likedTracks: Flow<List<Track>> = likedTrackDao.getLikedTracks()
        .map { entities -> entities.map { it.toTrack().copy(isLiked = true) } }
        .flowOn(Dispatchers.IO)

    // Saved Artists Flow
    val savedArtists: Flow<List<SavedArtistEntity>> = savedArtistDao?.getAllSavedArtists()
        ?.flowOn(Dispatchers.IO) ?: flowOf(emptyList())

    suspend fun toggleSaveArtist(artist: Artist): Boolean = withContext(Dispatchers.IO) {
        val dao = savedArtistDao ?: return@withContext false
        val isSaved = dao.isArtistSaved(artist.id)
        if (isSaved) {
            dao.removeArtist(artist.id)
            false
        } else {
            dao.insertArtist(
                SavedArtistEntity(
                    id = artist.id,
                    name = artist.name,
                    artworkUrl = artist.artworkUrl
                )
            )
            true
        }
    }

    suspend fun isArtistSaved(artistId: String): Boolean = withContext(Dispatchers.IO) {
        savedArtistDao?.isArtistSaved(artistId) ?: false
    }

    fun isArtistSavedFlow(artistId: String): Flow<Boolean> =
        savedArtistDao?.isArtistSavedFlow(artistId) ?: flowOf(false)

    // Playlists Flow
    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylistsWithCount()
        .map { list ->
            list.map { item ->
                Playlist(
                    id = item.id,
                    name = item.name,
                    description = item.description,
                    artworkUrl = item.artworkUrl,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    trackCount = item.trackCount
                )
            }
        }.flowOn(Dispatchers.IO)

    // History Flow
    val recentlyPlayed: Flow<List<Track>> = historyDao.getRecentlyPlayed(30)
        .map { entities -> entities.map { it.toTrack() } }
        .flowOn(Dispatchers.IO)

    val mostPlayedTracks: Flow<List<Track>> = historyDao.getMostPlayed(30)
        .map { entities -> entities.map { it.toTrack() } }
        .flowOn(Dispatchers.IO)

    // Recent Searches Flow
    val recentSearches: Flow<List<String>> = recentSearchDao.getRecentSearches()
        .map { list -> list.map { it.query } }
        .flowOn(Dispatchers.IO)

    suspend fun preloadCatalog() = withContext(Dispatchers.IO) {
        val entities = CuratedAudioCatalog.tracks.map { TrackEntity.fromTrack(it) }
        trackDao.insertTracks(entities)
    }

    fun prewarmSearch() {
        audioProvider.prewarm()
    }

    /**
     * Instant local & cache search (0-2ms).
     * Synchronously checks memory cache, Curated Audio Catalog, and Onboarding Catalog artists.
     */
    fun searchLocal(query: String): SearchResult {
        val cleanQ = query.replace(Regex("""[“”"‘’'–—−‐_/\\|]"""), " ").replace(Regex("""\s+"""), " ").trim().lowercase()
        if (cleanQ.isBlank()) return SearchResult()

        // 1. Instant in-memory cache check
        val cached = audioProvider.getCachedSearch(cleanQ)
        if (cached != null && !cached.isEmpty) {
            return cached
        }

        // 2. Instant curated tracks matching
        val matchingCuratedTracks = CuratedAudioCatalog.tracks.filter {
            it.title.lowercase().contains(cleanQ) ||
            it.artistName.lowercase().contains(cleanQ) ||
            it.albumName.lowercase().contains(cleanQ) ||
            it.genre.lowercase().contains(cleanQ)
        }

        // 3. Instant curated & onboarding artists matching
        val matchingCuratedArtists = CuratedAudioCatalog.artists.filter {
            it.name.lowercase().contains(cleanQ)
        }
        val matchingOnboardingArtists = OnboardingCatalog.artists.filter {
            it.name.lowercase().contains(cleanQ) || it.languageName.lowercase().contains(cleanQ)
        }.map { onb ->
            Artist(
                id = onb.id,
                name = onb.name,
                artworkUrl = onb.artworkUrl,
                bio = "${onb.languageName} Singer & Music Artist",
                genres = listOf(onb.languageName),
                monthlyListeners = 12500000
            )
        }
        val allMatchingArtists = (matchingCuratedArtists + matchingOnboardingArtists).distinctBy { it.name.lowercase() }

        return SearchResult(
            tracks = matchingCuratedTracks,
            artists = allMatchingArtists,
            albums = emptyList(),
            playlists = emptyList()
        )
    }

    suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        val cleanQ = query.replace(Regex("""[“”"‘’'–—−‐_/\\|]"""), " ").replace(Regex("""\s+"""), " ").trim()
        if (cleanQ.isBlank()) return@withContext SearchResult()

        recentSearchDao.insertSearch(RecentSearchEntity(cleanQ))

        // Check cache first for 0ms return
        val cached = audioProvider.getCachedSearch(cleanQ.lowercase())
        if (cached != null && !cached.isEmpty) {
            return@withContext cached
        }

        // Parallel execution: Room DB search + Remote AudioProvider search
        val localDeferred = async {
            try {
                trackDao.searchTracks(cleanQ).map { it.toTrack() }
            } catch (e: Exception) {
                emptyList()
            }
        }
        val remoteDeferred = async {
            audioProvider.search(query)
        }

        val localRoomTracks = localDeferred.await()
        val result = remoteDeferred.await()

        val allTracks = (result.tracks + localRoomTracks).distinctSongs()

        // Cache found tracks to DB
        if (result.tracks.isNotEmpty()) {
            trackDao.insertTracks(result.tracks.map { TrackEntity.fromTrack(it) })
        }
        // Cache found artists
        result.artists.forEach {
            artistCache[it.id] = it
            artistCache[it.name.lowercase()] = it
        }
        result.copy(tracks = allTracks)
    }

    suspend fun getTrendingTracks(): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedTrending?.let { (time, list) ->
            if (now - time < HOME_CACHE_TTL_MS && list.isNotEmpty()) return@withContext list
        }
        val tracks = audioProvider.getTrending().distinctSongs()
        if (tracks.isNotEmpty()) {
            cachedTrending = now to tracks
        }
        tracks
    }

    suspend fun getTrending(): List<Track> = getTrendingTracks()

    suspend fun getNewReleases(): List<Album> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedNewReleases?.let { (time, list) ->
            if (now - time < HOME_CACHE_TTL_MS && list.isNotEmpty()) return@withContext list
        }
        val albums = audioProvider.getNewReleases()
        if (albums.isNotEmpty()) {
            cachedNewReleases = now to albums
        }
        albums
    }

    suspend fun getPopularArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedPopularArtists?.let { (time, list) ->
            if (now - time < HOME_CACHE_TTL_MS && list.isNotEmpty()) return@withContext list
        }
        val userArtists = downloadRepository.preferencesManager.selectedArtists.firstOrNull() ?: emptySet()
        val baseArtists = audioProvider.getPopularArtists()

        // Prioritize and include artists explicitly selected by the user during onboarding
        val matchingOnboarding = OnboardingCatalog.artists.filter { onb ->
            userArtists.any { it.equals(onb.name, ignoreCase = true) }
        }.map { onb ->
            Artist(
                id = onb.id,
                name = onb.name,
                artworkUrl = onb.artworkUrl,
                bio = "${onb.languageName} Singer & Music Artist",
                genres = listOf(onb.languageName),
                monthlyListeners = 12500000
            )
        }

        val combined = (matchingOnboarding + baseArtists).distinctBy { it.name.lowercase() }
        combined.forEach {
            artistCache[it.id] = it
            artistCache[it.name.lowercase()] = it
        }
        if (combined.isNotEmpty()) {
            cachedPopularArtists = now to combined
        }
        combined
    }

    suspend fun getArtistDetails(artistId: String): Artist? = withContext(Dispatchers.IO) {
        artistCache[artistId] ?: audioProvider.getArtist(artistId)?.also {
            artistCache[it.id] = it
            artistCache[it.name.lowercase()] = it
        }
    }

    suspend fun getAlbumDetails(albumId: String): Album? = withContext(Dispatchers.IO) {
        audioProvider.getAlbum(albumId)
    }

    suspend fun getTracksForArtist(
        artistId: String,
        onProgress: ((List<Track>) -> Unit)? = null
    ): List<Track> = withContext(Dispatchers.IO) {
        val artist = getArtistDetails(artistId)
        val remoteTracks = audioProvider.getTracksForArtist(artistId, artist?.name) { batch ->
            val distinctBatch = batch.distinctSongs()
            onProgress?.invoke(distinctBatch)
        }.distinctSongs()

        if (remoteTracks.isNotEmpty()) {
            try {
                trackDao.insertTracks(remoteTracks.map { TrackEntity.fromTrack(it) })
            } catch (_: Exception) {}
            remoteTracks
        } else {
            val curated = CuratedAudioCatalog.getTracksForArtist(artistId).distinctSongs()
            onProgress?.invoke(curated)
            curated
        }
    }

    suspend fun getTracksByGenre(genre: String): List<Track> = withContext(Dispatchers.IO) {
        audioProvider.getTracksByGenre(genre)
    }

    suspend fun toggleLike(track: Track): Boolean = withContext(Dispatchers.IO) {
        cachedRecommendations = null
        cachedMixes = null
        // Ensure track is in DB
        trackDao.insertTrack(TrackEntity.fromTrack(track))
        val isLiked = likedTrackDao.isTrackLiked(track.id)
        if (isLiked) {
            likedTrackDao.removeLike(track.id)
            trackDao.updateLikedStatus(track.id, false)
            false
        } else {
            likedTrackDao.insertLike(LikedTrackEntity(track.id))
            trackDao.updateLikedStatus(track.id, true)
            true
        }
    }

    suspend fun isTrackLiked(trackId: String): Boolean = withContext(Dispatchers.IO) {
        likedTrackDao.isTrackLiked(trackId)
    }

    fun isTrackLikedFlow(trackId: String): Flow<Boolean> = likedTrackDao.isTrackLikedFlow(trackId)

    suspend fun recordPlayback(track: Track) = withContext(Dispatchers.IO) {
        trackDao.insertTrack(TrackEntity.fromTrack(track))
        historyDao.insertHistory(PlaybackHistoryEntity(trackId = track.id, playedAt = System.currentTimeMillis()))
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearHistory()
    }

    suspend fun createPlaylist(name: String, description: String = "", artworkUrl: String = ""): String = withContext(Dispatchers.IO) {
        val id = "pl_" + UUID.randomUUID().toString().take(8)
        val defaultArt = artworkUrl.ifBlank {
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1080&auto=format&fit=crop&q=85"
        }
        playlistDao.insertPlaylist(
            PlaylistEntity(
                id = id,
                name = name,
                description = description,
                artworkUrl = defaultArt
            )
        )
        id
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun renamePlaylist(playlistId: String, name: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(playlistId, name)
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: Track) = withContext(Dispatchers.IO) {
        trackDao.insertTrack(TrackEntity.fromTrack(track))
        val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
        playlistDao.addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = track.id,
                position = maxPos + 1,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun reorderPlaylistTracks(playlistId: String, trackIds: List<String>) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylistTrackOrder(playlistId, trackIds)
    }

    fun getTracksForPlaylist(playlistId: String): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { entities ->
            entities.map { it.toTrack() }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getPlaylistById(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        val count = playlistDao.getPlaylistTrackCount(playlistId).firstOrNull() ?: 0
        playlistDao.getPlaylistById(playlistId)?.let { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                artworkUrl = entity.artworkUrl,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                trackCount = count
            )
        }
    }

    suspend fun deleteSearch(query: String) = withContext(Dispatchers.IO) {
        recentSearchDao.deleteSearch(query)
    }

    suspend fun clearSearches() = withContext(Dispatchers.IO) {
        recentSearchDao.clearSearches()
    }

    suspend fun getPersonalizedRecommendations(): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedRecommendations?.let { (time, list) ->
            if (now - time < HOME_CACHE_TTL_MS && list.isNotEmpty()) return@withContext list
        }

        val likes = likedTracks.firstOrNull() ?: emptyList()
        val history = recentlyPlayed.firstOrNull() ?: emptyList()
        val userLangs = downloadRepository.preferencesManager.selectedLanguages.firstOrNull() ?: emptySet()
        val userArtists = downloadRepository.preferencesManager.selectedArtists.firstOrNull() ?: emptySet()

        val dynamicCandidates = mutableListOf<Track>()

        // Fetch live hits concurrently across artists and languages with a fast timeout
        try {
            withTimeoutOrNull(3500L) {
                coroutineScope {
                    val artistJobs = userArtists.take(8).map { artistName ->
                        async {
                            try {
                                val res = audioProvider.search(artistName)
                                res.tracks.filter { it.streamUrl.isNotBlank() || it.id.isNotBlank() }.distinctSongs().take(4)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    val langJobs = userLangs.take(3).map { lang ->
                        async {
                            try {
                                val res = audioProvider.search("top $lang hits")
                                res.tracks.filter { it.streamUrl.isNotBlank() || it.id.isNotBlank() }.distinctSongs().take(6)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    val results = (artistJobs + langJobs).awaitAll()
                    results.forEach { dynamicCandidates.addAll(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val uniqueDynamicCandidates = dynamicCandidates.distinctSongs()

        // Cache fetched tracks to DB
        if (uniqueDynamicCandidates.isNotEmpty()) {
            trackDao.insertTracks(uniqueDynamicCandidates.map { TrackEntity.fromTrack(it) })
        }

        val recs = recommendationEngine.generateRecommendations(
            likedTracks = likes,
            recentlyPlayed = history,
            preferredLanguages = userLangs,
            preferredArtists = userArtists,
            allCandidates = uniqueDynamicCandidates,
            limit = 20
        )
        if (recs.isNotEmpty()) {
            cachedRecommendations = now to recs
        }
        recs
    }

    /**
     * Context-aware Next Song Recommendation + Dynamic Autoplay Engine.
     * Evaluates seed track, artist continuity, ecosystem isolation, mood harmony, user's liked/frequently played songs,
     * onboarding preferences, and fast-skip negative feedback.
     */
    suspend fun getAutoplayQueue(
        seedTrack: Track,
        existingQueue: List<Track> = emptyList(),
        skippedTrackIds: Set<String> = emptySet(),
        skippedTitles: Set<String> = emptySet(),
        sessionPlayedTrackIds: Set<String> = emptySet(),
        sessionPlayedTitles: Set<String> = emptySet(),
        limit: Int = 20
    ): List<Track> = withContext(Dispatchers.IO) {
        val likes = likedTracks.firstOrNull() ?: emptyList()
        val history = recentlyPlayed.firstOrNull() ?: emptyList()
        val mostPlayed = getMostPlayedTracks()
        val userLangs = downloadRepository.preferencesManager.selectedLanguages.firstOrNull() ?: emptySet()
        val userArtists = downloadRepository.preferencesManager.selectedArtists.firstOrNull() ?: emptySet()

        val candidates = mutableListOf<Track>()
        val seedArtist = seedTrack.artistName.trim()
        val seedEcosystem = RecommendationEngine.detectEcosystem(seedTrack)
        val seedMood = RecommendationEngine.detectMood(seedTrack)
        val peerArtists = RecommendationEngine.getRelatedPeerArtists(seedArtist, seedMood, seedEcosystem)

        // 1. Identify ecosystem search keywords
        val ecosystemKeyword = seedEcosystem.defaultSearchKeyword

        // 2. Concurrently fetch candidates across algorithmic reco, artist hits, peers, and ecosystem top charts
        try {
            withTimeoutOrNull(4200L) {
                coroutineScope {
                    // Task A: Direct JioSaavn Algorithmic Song Radio (reco.getreco)
                    val recoJob = async {
                        if (seedTrack.id.isNotBlank()) {
                            try {
                                audioProvider.getSongRecommendations(seedTrack.id)
                                    .filter { RecommendationEngine.areEcosystemsCompatible(seedEcosystem, RecommendationEngine.detectEcosystem(it)) }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        } else emptyList()
                    }

                    // Task B: Seed Artist top hits in this ecosystem/mood + discography
                    val artistJob = async {
                        if (seedArtist.isNotBlank()) {
                            try {
                                val searchHits = audioProvider.search(seedArtist).tracks
                                val moodSearchHits = if (seedMood.searchKeywords.isNotEmpty()) {
                                    audioProvider.search("$seedArtist ${seedMood.searchKeywords.first()}").tracks
                                } else emptyList()
                                val discogHits = audioProvider.getTracksForArtist(seedTrack.artistId, seedArtist)
                                (searchHits + moodSearchHits + discogHits)
                                    .filter {
                                        it.id != seedTrack.id &&
                                        !it.title.equals(seedTrack.title, ignoreCase = true) &&
                                        RecommendationEngine.areEcosystemsCompatible(seedEcosystem, RecommendationEngine.detectEcosystem(it))
                                    }
                                    .distinctSongs()
                                    .take(15)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        } else emptyList()
                    }

                    // Task C: Top Peer Artists strictly in this ecosystem
                    val peerJobs = peerArtists.filter { !it.equals(seedArtist, ignoreCase = true) }.take(4).map { peer ->
                        async {
                            try {
                                audioProvider.search(peer)
                                    .tracks
                                    .filter {
                                        it.id != seedTrack.id &&
                                        !it.title.equals(seedTrack.title, ignoreCase = true) &&
                                        RecommendationEngine.areEcosystemsCompatible(seedEcosystem, RecommendationEngine.detectEcosystem(it))
                                    }
                                    .distinctSongs()
                                    .take(8)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }

                    // Task D: Top hits in the same mood & ecosystem
                    val moodHitsJob = async {
                        try {
                            val moodQuery = seedMood.searchKeywords.firstOrNull() ?: ecosystemKeyword
                            audioProvider.search(moodQuery)
                                .tracks
                                .filter {
                                    it.id != seedTrack.id &&
                                    !it.title.equals(seedTrack.title, ignoreCase = true) &&
                                    RecommendationEngine.areEcosystemsCompatible(seedEcosystem, RecommendationEngine.detectEcosystem(it))
                                }
                                .distinctSongs()
                                .take(15)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    // Task E: Onboarding preference tracks strictly matched to this ecosystem
                    val prefHitsJob = async {
                        val matchingLangs = userLangs.filter { it.equals(seedEcosystem.id, ignoreCase = true) || it.contains(seedEcosystem.id, ignoreCase = true) }.toSet()
                        val matchingArtists = userArtists.filter { prefArtist ->
                            peerArtists.any { it.contains(prefArtist, ignoreCase = true) || prefArtist.contains(it, ignoreCase = true) }
                        }.toSet()

                        if (matchingLangs.isNotEmpty() || matchingArtists.isNotEmpty()) {
                            OnboardingCatalog.getHitTracksForPreferences(matchingLangs, matchingArtists)
                                .filter {
                                    it.id != seedTrack.id &&
                                    RecommendationEngine.areEcosystemsCompatible(seedEcosystem, RecommendationEngine.detectEcosystem(it))
                                }
                                .take(10)
                        } else emptyList()
                    }

                    val recoTracks = recoJob.await()
                    val artistTracks = artistJob.await()
                    val moodTracks = moodHitsJob.await()
                    val peerTracks = peerJobs.awaitAll().flatten()
                    val prefTracks = prefHitsJob.await()

                    candidates.addAll(recoTracks)
                    candidates.addAll(artistTracks)
                    candidates.addAll(peerTracks)
                    candidates.addAll(moodTracks)
                    candidates.addAll(prefTracks)

                    // Compatible user history & liked tracks
                    val compatibleMostPlayed = mostPlayed.filter {
                        it.id != seedTrack.id &&
                        RecommendationEngine.areEcosystemsCompatible(seedEcosystem, RecommendationEngine.detectEcosystem(it))
                    }
                    val compatibleLikes = likes.filter {
                        it.id != seedTrack.id &&
                        RecommendationEngine.areEcosystemsCompatible(seedEcosystem, RecommendationEngine.detectEcosystem(it))
                    }
                    candidates.addAll(compatibleMostPlayed)
                    candidates.addAll(compatibleLikes)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Fallback strictly within compatible ecosystem
        if (candidates.isEmpty()) {
            val curatedMatches = (OnboardingCatalog.defaultHitTracks + CuratedAudioCatalog.tracks).filter {
                RecommendationEngine.areEcosystemsCompatible(seedEcosystem, RecommendationEngine.detectEcosystem(it)) &&
                it.id != seedTrack.id && !it.title.equals(seedTrack.title, ignoreCase = true)
            }
            candidates.addAll(curatedMatches)
        }

        // 4. Multi-signal scoring and ranking with category firewall & skip penalization
        val currentQueueIds = existingQueue.map { it.id }.filter { it.isNotBlank() }.toSet()
        val currentQueueTitles = existingQueue.map { normalizeSongTitle(it.title) }.toSet()

        val rankedResult = recommendationEngine.generateAutoplayQueue(
            seedTrack = seedTrack,
            historyTracks = history,
            mostPlayedTracks = mostPlayed,
            likedTracks = likes,
            preferredLanguages = userLangs,
            preferredArtists = userArtists,
            skippedTrackIds = skippedTrackIds,
            skippedTitles = skippedTitles,
            sessionPlayedTrackIds = sessionPlayedTrackIds,
            sessionPlayedTitles = sessionPlayedTitles,
            currentQueueIds = currentQueueIds,
            candidatePool = candidates,
            limit = limit
        )

        val finalResult = rankedResult.ifEmpty {
            candidates
                .filter {
                    it.id != seedTrack.id &&
                    !it.title.equals(seedTrack.title, ignoreCase = true) &&
                    !currentQueueIds.contains(it.id) &&
                    !currentQueueTitles.contains(normalizeSongTitle(it.title)) &&
                    RecommendationEngine.areEcosystemsCompatible(seedEcosystem, RecommendationEngine.detectEcosystem(it))
                }
                .distinctSongs()
                .take(limit)
        }

        // 5. Cache discovered tracks to Room DB in background
        if (finalResult.isNotEmpty()) {
            trackDao.insertTracks(finalResult.filter { it.id.isNotBlank() }.map { TrackEntity.fromTrack(it) })
        }

        finalResult
    }

    suspend fun getRelatedTracksForTrack(seedTrack: Track, limit: Int = 25): List<Track> = withContext(Dispatchers.IO) {
        getAutoplayQueue(seedTrack = seedTrack, limit = limit)
    }

    suspend fun getMostPlayedTracks(): List<Track> = withContext(Dispatchers.IO) {
        // Strictly return user's most played tracks from playback history
        // Returns empty list on fresh install or when no history exists
        historyDao.getMostPlayed(20).firstOrNull()?.map { it.toTrack() } ?: emptyList()
    }

    suspend fun getMadeForYouMixes(): Map<String, List<Track>> = withContext(Dispatchers.IO) {
        val likes = likedTracks.firstOrNull() ?: emptyList()
        val history = recentlyPlayed.firstOrNull() ?: emptyList()
        recommendationEngine.generateMadeForYouMixes(likes, history)
    }

    suspend fun resolveFullLengthTrack(track: Track): Track? = withContext(Dispatchers.IO) {
        if (track.id.isNotBlank()) {
            val url = audioProvider.getStreamUrl(track)
            if (!url.isNullOrBlank()) {
                return@withContext track.copy(streamUrl = url)
            }
        }
        val saavnSearch = audioProvider.search("${track.title} ${track.artistName}".trim())
        val firstMatch = saavnSearch.tracks.firstOrNull()
        if (firstMatch != null) {
            val url = audioProvider.getStreamUrl(firstMatch)
            if (!url.isNullOrBlank()) {
                return@withContext track.copy(
                    id = if (track.id.isBlank()) firstMatch.id else track.id,
                    streamUrl = url,
                    durationMs = if (firstMatch.durationMs > 0) firstMatch.durationMs else track.durationMs
                )
            }
        }
        null
    }

    suspend fun resolveFullLengthTrack(title: String, artist: String): Track? = withContext(Dispatchers.IO) {
        val dummy = Track(id = "", title = title, artistName = artist)
        resolveFullLengthTrack(dummy)
    }
}
