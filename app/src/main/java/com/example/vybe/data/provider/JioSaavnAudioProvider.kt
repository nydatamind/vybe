package com.example.vybe.data.provider

import com.example.vybe.core.model.Album
import com.example.vybe.core.model.Artist
import com.example.vybe.core.model.SearchResult
import com.example.vybe.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * 100% Native High-Performance JioSaavn Engine.
 *
 * Features:
 * - Direct Akamai/Saavn CDN streams (320kbps / 160kbps MP4/AAC).
 * - Standard DES-ECB decryption with zero external proxy dependencies.
 * - Comprehensive Search (Songs, Albums, Artists).
 * - Full Album and Artist tracklists with HD 500x500 Cover Artworks.
 * - In-memory stream caching for instant zero-latency playback.
 * - Powered by pure kotlinx.serialization (JVM testable & Android runtime ready).
 */
class JioSaavnAudioProvider : AudioProvider {

    override val name: String = "JioSaavn"

    private val connectionPool = okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES)
    private val dispatcher = okhttp3.Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 24
    }

    private val httpClient = OkHttpClient.Builder()
        .connectionPool(connectionPool)
        .dispatcher(dispatcher)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        val BASE_URL: String
            get() = com.example.vybe.core.remote.RemoteConfigManager.getActiveBaseUrl()

        /**
         * Global in-memory cache for decrypted stream URLs (trackId -> streamUrl)
         */
        val streamCache = ConcurrentHashMap<String, String>()

        /**
         * Global in-memory cache for search results (normalized query -> SearchResult)
         * Provides 0ms instant response on repeat searches and backspacing.
         */
        val searchCache = ConcurrentHashMap<String, SearchResult>()

        /**
         * Global in-memory cache for Artist Details (artistId -> Artist)
         */
        val artistCache = ConcurrentHashMap<String, Artist>()

        /**
         * Global in-memory cache for Album Details (albumId -> Album)
         */
        val albumCache = ConcurrentHashMap<String, Album>()

        /**
         * Global in-memory cache for Track Details (trackId -> Track)
         */
        val trackCache = ConcurrentHashMap<String, Track>()

        /**
         * Global in-memory cache for Full Artist Discographies (artistId -> List<Track>)
         */
        val artistTracksCache = ConcurrentHashMap<String, List<Track>>()

        /**
         * Decodes Base64 safely in both Android Runtime and JVM Unit Tests.
         */
        fun decodeBase64(input: String): ByteArray {
            return try {
                java.util.Base64.getDecoder().decode(input.trim())
            } catch (e: Throwable) {
                try {
                    android.util.Base64.decode(input.trim(), android.util.Base64.DEFAULT)
                } catch (e2: Throwable) {
                    ByteArray(0)
                }
            }
        }

        /**
         * Decrypts encrypted_media_url from JioSaavn and upgrades to 320kbps AAC/MP4.
         */
        fun decryptMediaUrl(encryptedMediaUrl: String): String {
            if (encryptedMediaUrl.isBlank()) return ""
            return try {
                val desKey = com.example.vybe.core.remote.RemoteConfigManager.getDesKey()
                val keySpec = SecretKeySpec(desKey.toByteArray(Charsets.UTF_8), "DES")
                val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec)
                val decodedBytes = decodeBase64(encryptedMediaUrl)
                if (decodedBytes.isEmpty()) return ""
                val decrypted = cipher.doFinal(decodedBytes)
                val rawUrl = String(decrypted, Charsets.UTF_8).trim()
                upgradeQuality(rawUrl)
            } catch (e: Exception) {
                ""
            }
        }

        /**
         * Upgrades stream bitrate to 320kbps MP4 / M4A and forces HTTPS.
         */
        fun upgradeQuality(url: String): String {
            if (url.isBlank()) return ""
            val qualities = com.example.vybe.core.remote.RemoteConfigManager.getCdnQualities()
            val target = qualities.firstOrNull() ?: "_320.mp4"
            return url
                .replace("http://", "https://")
                .replace("preview.saavncdn.com", "aac.saavncdn.com")
                .replace("_96_p.mp4", target)
                .replace("_96.mp4", target)
                .replace("_160.mp4", target)
                .replace("_96.m4a", target.replace(".mp4", ".m4a"))
                .replace("_160.m4a", target.replace(".mp4", ".m4a"))
        }

        /**
         * Upgrades artwork resolution from 150x150 to 500x500 HD.
         */
        fun cleanArtworkUrl(url: String?): String {
            if (url.isNullOrBlank()) return ""
            return url
                .replace("http://", "https://")
                .replace("50x50.jpg", "500x500.jpg")
                .replace("150x150.jpg", "500x500.jpg")
                .replace("50x50.png", "500x500.png")
                .replace("150x150.png", "500x500.png")
                .replace("50x50.webp", "500x500.webp")
                .replace("150x150.webp", "500x500.webp")
        }

        /**
         * Decodes HTML entities commonly returned in Saavn text fields.
         */
        fun cleanText(text: String?): String {
            if (text.isNullOrBlank()) return ""
            return text
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#039;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .trim()
        }
    }

    override fun getCachedSearch(query: String): SearchResult? {
        val key = query.trim().lowercase()
        return if (key.isNotBlank()) searchCache[key] else null
    }

    override fun prewarm() {
        try {
            val request = Request.Builder()
                .url("$BASE_URL?__call=autocomplete.get&_format=json&query=hit")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()
            httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.close()
                }
            })
        } catch (_: Exception) {}
    }

    private fun executeRequest(url: String): String? {
        val result = tryExecute(url)
        if (result != null) {
            com.example.vybe.core.remote.RemoteConfigManager.reportEndpointSuccess(url)
            return result
        }

        // If primary active endpoint failed, attempt fallback mirror endpoints
        val activeBase = com.example.vybe.core.remote.RemoteConfigManager.getActiveBaseUrl()
        if (url.startsWith(activeBase)) {
            val backups = com.example.vybe.core.remote.RemoteConfigManager.getBackupBaseUrls()
            for (backup in backups) {
                if (backup != activeBase) {
                    val fallbackUrl = url.replace(activeBase, backup)
                    val fallbackResult = tryExecute(fallbackUrl)
                    if (fallbackResult != null) {
                        com.example.vybe.core.remote.RemoteConfigManager.reportEndpointSuccess(backup)
                        return fallbackResult
                    }
                }
            }
            com.example.vybe.core.remote.RemoteConfigManager.reportEndpointFailure(activeBase)
        }
        return null
    }

    private fun tryExecute(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return@withContext SearchResult()

        val normalizedKey = cleanQ.lowercase()
        val cached = searchCache[normalizedKey]
        if (cached != null && !cached.isEmpty) {
            return@withContext cached
        }

        val encodedQ = try { URLEncoder.encode(cleanQ, "UTF-8") } catch (e: Exception) { cleanQ }

        coroutineScope {
            val songsDeferred = async {
                val url = "$BASE_URL?__call=search.getResults&_format=json&_marker=0&cc=in&includeMetaTags=1&q=$encodedQ&p=1&n=25"
                parseSongList(executeRequest(url))
            }
            val albumsDeferred = async {
                val url = "$BASE_URL?__call=search.getAlbumResults&_format=json&_marker=0&cc=in&q=$encodedQ&p=1&n=8"
                parseAlbumList(executeRequest(url))
            }
            val artistsDeferred = async {
                val url = "$BASE_URL?__call=search.getArtistResults&_format=json&_marker=0&cc=in&q=$encodedQ&p=1&n=8"
                parseArtistList(executeRequest(url))
            }

            val songs = songsDeferred.await()
            val albums = albumsDeferred.await()
            val artists = artistsDeferred.await()

            val finalSongs = if (songs.isEmpty()) {
                val autoUrl = "$BASE_URL?__call=autocomplete.get&_format=json&_marker=0&cc=in&includeMetaTags=1&query=$encodedQ"
                parseAutocompleteSongs(executeRequest(autoUrl))
            } else songs

            val result = SearchResult(
                tracks = finalSongs,
                albums = albums,
                artists = artists,
                playlists = emptyList()
            )

            if (!result.isEmpty) {
                if (searchCache.size > 200) {
                    searchCache.clear()
                }
                searchCache[normalizedKey] = result
            }

            result
        }
    }

    override suspend fun getTrack(id: String): Track? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        val cached = trackCache[id]
        if (cached != null) return@withContext cached

        val url = "$BASE_URL?__call=song.getDetails&pids=$id&_format=json&_marker=0&ctx=android"
        val response = executeRequest(url) ?: return@withContext null
        try {
            val element = jsonParser.parseToJsonElement(response)
            if (element is JsonObject) {
                val songObj = element[id]?.let { if (it is JsonObject) it else null }
                    ?: element["songs"]?.let { if (it is JsonArray && it.isNotEmpty() && it[0] is JsonObject) it[0].jsonObject else null }
                    ?: element.values.firstOrNull { it is JsonObject }?.jsonObject

                val parsed = songObj?.let { parseSingleTrack(it) }
                if (parsed != null) {
                    if (trackCache.size > 300) trackCache.clear()
                    trackCache[id] = parsed
                }
                parsed
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getArtist(id: String): Artist? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        val cached = artistCache[id]
        if (cached != null) return@withContext cached

        val url = "$BASE_URL?__call=artist.getArtistPageDetails&artistId=$id&_format=json&_marker=0&ctx=android&n_song=50&n_album=20"
        val response = executeRequest(url) ?: return@withContext null
        try {
            val element = jsonParser.parseToJsonElement(response)
            if (element is JsonObject) {
                val artistName = cleanText(element.str("name").ifBlank { element.str("title") })
                val artworkUrl = cleanArtworkUrl(element.str("image"))
                val bio = cleanText(element.str("bio").ifBlank { element.str("subtitle") })
                val listeners = element.long("fan_count", 12500000L).coerceAtLeast(1000000L)

                val topSongs = element.arr("topSongs")
                if (topSongs != null) {
                    for (i in 0 until topSongs.size) {
                        val item = topSongs[i].let { if (it is JsonObject) it else null } ?: continue
                        val track = parseSingleTrack(item)
                        if (track.streamUrl.isNotBlank()) {
                            streamCache[track.id] = track.streamUrl
                        }
                    }
                }

                val artist = Artist(
                    id = id,
                    name = artistName,
                    artworkUrl = artworkUrl,
                    bio = bio.ifBlank { "Top featured artist on JioSaavn" },
                    genres = listOf("Bollywood", "Indian Pop"),
                    monthlyListeners = listeners
                )
                if (artistCache.size > 100) artistCache.clear()
                artistCache[id] = artist
                artistCache[artistName.lowercase()] = artist
                artist
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getAlbum(id: String): Album? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        val cached = albumCache[id]
        if (cached != null) return@withContext cached

        val url = "$BASE_URL?__call=content.getAlbumDetails&albumid=$id&_format=json&_marker=0&ctx=android"
        val response = executeRequest(url) ?: return@withContext null
        try {
            val element = jsonParser.parseToJsonElement(response)
            if (element is JsonObject) {
                val albumTitle = cleanText(element.str("title").ifBlank { element.str("name") })
                val artistName = cleanText(element.str("primary_artists").ifBlank { element.str("artist") })
                val artistId = element.str("primary_artists_id")
                val artworkUrl = cleanArtworkUrl(element.str("image"))
                val year = element.str("year")

                val tracks = mutableListOf<Track>()
                val songs = element.arr("songs")
                if (songs != null) {
                    for (i in 0 until songs.size) {
                        val songObj = songs[i].let { if (it is JsonObject) it else null } ?: continue
                        tracks.add(parseSingleTrack(songObj))
                    }
                }

                val album = Album(
                    id = id,
                    title = albumTitle,
                    artistId = artistId,
                    artistName = artistName,
                    artworkUrl = artworkUrl,
                    releaseDate = year,
                    genre = "Bollywood",
                    tracks = tracks
                )
                if (albumCache.size > 100) albumCache.clear()
                albumCache[id] = album
                album
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getTrending(): List<Track> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL?__call=webapi.get&api_version=4&_format=json&_marker=0&ctx=android"
        val response = executeRequest(url)
        val trendingSongs = mutableListOf<Track>()

        if (response != null) {
            try {
                val element = jsonParser.parseToJsonElement(response)
                if (element is JsonObject) {
                    val newTrending = element.arr("new_trending")
                    if (newTrending != null) {
                        for (i in 0 until newTrending.size) {
                            val item = newTrending[i].let { if (it is JsonObject) it else null } ?: continue
                            val type = item.str("type")
                            if (type == "song" || item.hasKey("encrypted_media_url") || item.hasKey("more_info")) {
                                trendingSongs.add(parseSingleTrack(item))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (trendingSongs.size < 10) {
            val chartUrl = "$BASE_URL?__call=search.getResults&_format=json&_marker=0&cc=in&includeMetaTags=1&q=trending+hindi+hits&p=1&n=30"
            val fallbackSongs = parseSongList(executeRequest(chartUrl))
            (trendingSongs + fallbackSongs).distinctBy { it.id }
        } else {
            trendingSongs
        }
    }

    override suspend fun getNewReleases(): List<Album> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL?__call=webapi.get&api_version=4&_format=json&_marker=0&ctx=android"
        val response = executeRequest(url)
        val albums = mutableListOf<Album>()

        if (response != null) {
            try {
                val element = jsonParser.parseToJsonElement(response)
                if (element is JsonObject) {
                    val newAlbums = element.arr("new_albums")
                    if (newAlbums != null) {
                        for (i in 0 until newAlbums.size) {
                            val item = newAlbums[i].let { if (it is JsonObject) it else null } ?: continue
                            albums.add(parseSingleAlbum(item))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (albums.isEmpty()) {
            val searchUrl = "$BASE_URL?__call=search.getAlbumResults&_format=json&_marker=0&cc=in&q=latest+bollywood+albums&p=1&n=15"
            parseAlbumList(executeRequest(searchUrl))
        } else {
            albums
        }
    }

    override suspend fun getPopularArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val queries = listOf("Arijit Singh", "A.R. Rahman", "Shreya Ghoshal", "Anirudh Ravichander", "Diljit Dosanjh", "Pritam", "Badshah", "Sid Sriram")
        coroutineScope {
            val deferred = queries.map { name ->
                async {
                    val encoded = try { URLEncoder.encode(name, "UTF-8") } catch (e: Exception) { name }
                    val url = "$BASE_URL?__call=search.getArtistResults&_format=json&_marker=0&cc=in&q=$encoded&p=1&n=1"
                    parseArtistList(executeRequest(url)).firstOrNull()
                }
            }
            deferred.mapNotNull { it.await() }
        }
    }

    override suspend fun getRecommendations(seedGenres: List<String>): List<Track> = withContext(Dispatchers.IO) {
        val query = if (seedGenres.isNotEmpty()) "${seedGenres.first()} top songs" else "superhit songs"
        val encoded = try { URLEncoder.encode(query, "UTF-8") } catch (e: Exception) { query }
        val url = "$BASE_URL?__call=search.getResults&_format=json&_marker=0&cc=in&includeMetaTags=1&q=$encoded&p=1&n=25"
        parseSongList(executeRequest(url))
    }

    override suspend fun getSongRecommendations(trackId: String): List<Track> = withContext(Dispatchers.IO) {
        if (trackId.isBlank()) return@withContext emptyList()
        val url = "$BASE_URL?__call=reco.getreco&_format=json&_marker=0&cc=in&pid=$trackId"
        val response = executeRequest(url)
        parseRecoTracks(response)
    }

    override suspend fun getTracksByGenre(genre: String): List<Track> = withContext(Dispatchers.IO) {
        val query = "$genre hits"
        val encoded = try { URLEncoder.encode(query, "UTF-8") } catch (e: Exception) { query }
        val url = "$BASE_URL?__call=search.getResults&_format=json&_marker=0&cc=in&includeMetaTags=1&q=$encoded&p=1&n=30"
        parseSongList(executeRequest(url))
    }

    override suspend fun getTracksForArtist(
        artistId: String,
        artistName: String?,
        onProgress: ((List<Track>) -> Unit)?
    ): List<Track> = withContext(Dispatchers.IO) {
        val cacheKey = if (artistId.isNotBlank()) artistId.trim() else artistName?.lowercase()?.trim().orEmpty()
        if (cacheKey.isNotBlank()) {
            val cached = artistTracksCache[cacheKey]
            if (cached != null && cached.isNotEmpty()) {
                onProgress?.invoke(cached)
                return@withContext cached
            }
        }

        val collectedTracks = ArrayList<Track>()
        val seenTrackKeys = HashSet<String>()

        fun addTrackIfNew(track: Track): Boolean {
            val norm = com.example.vybe.data.repository.normalizeSongTitle(track.title)
            val cleanArtist = track.artistName.lowercase().trim()
            val key = if (norm.length >= 3) "${norm}_${cleanArtist.take(12)}" else track.id.ifBlank { track.title.lowercase().trim() }
            return if (seenTrackKeys.add(key)) {
                collectedTracks.add(track)
                true
            } else false
        }

        var resolvedId = artistId.trim()
        var resolvedName = artistName?.trim().orEmpty()

        // 0. If artistId is not numeric (e.g. from local search or text), look up artistId via search
        if (resolvedId.isNotBlank() && resolvedId.toIntOrNull() == null && resolvedName.isBlank()) {
            resolvedName = resolvedId
        }
        if (resolvedId.isBlank() || resolvedId.toIntOrNull() == null) {
            if (resolvedName.isNotBlank()) {
                val encoded = try { URLEncoder.encode(resolvedName, "UTF-8") } catch (_: Exception) { resolvedName }
                val searchUrl = "$BASE_URL?__call=search.getArtistResults&_format=json&_marker=0&cc=in&q=$encoded&p=1&n=1"
                val searchRes = parseArtistList(executeRequest(searchUrl))
                val match = searchRes.firstOrNull()
                if (match != null && match.id.isNotBlank()) {
                    resolvedId = match.id
                    if (resolvedName.isBlank()) resolvedName = match.name
                }
            }
        }

        // 1. Initial High-Speed Fetch: artist.getArtistPageDetails
        // Extracts topSongs (up to 70 hits), album metadata, and official name
        if (resolvedId.isNotBlank()) {
            val url = "$BASE_URL?__call=artist.getArtistPageDetails&artistId=$resolvedId&_format=json&_marker=0&ctx=android&n_song=100&n_album=50"
            val response = executeRequest(url)
            if (response != null) {
                try {
                    val element = jsonParser.parseToJsonElement(response)
                    if (element is JsonObject) {
                        if (resolvedName.isBlank()) {
                            resolvedName = cleanText(element.str("name").ifBlank { element.str("title") })
                        }
                        val topSongsArray = when (val topElement = element["topSongs"]) {
                            is JsonArray -> topElement
                            is JsonObject -> topElement.arr("songs")
                            else -> null
                        }
                        if (topSongsArray != null && topSongsArray.isNotEmpty()) {
                            for (i in 0 until topSongsArray.size) {
                                val item = topSongsArray[i].let { if (it is JsonObject) it else null } ?: continue
                                val track = parseSingleTrack(item)
                                addTrackIfNew(track)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Instant Progressive Update: If we have topSongs, emit them right now (<300ms)
        if (collectedTracks.isNotEmpty()) {
            onProgress?.invoke(ArrayList(collectedTracks))
        }

        // 2. Full Discography Pagination: artist.getArtistMoreSong across pages
        if (resolvedId.isNotBlank()) {
            var currentPage = 0
            var consecutiveEmptyPages = 0
            var reachedLastPage = false

            while (!reachedLastPage && consecutiveEmptyPages < 2 && currentPage < 30) {
                val batchPages = (currentPage until (currentPage + 4)).toList()
                currentPage += 4

                val batchResults = coroutineScope {
                    batchPages.map { page ->
                        async {
                            val pageUrl = "$BASE_URL?__call=artist.getArtistMoreSong&artistId=$resolvedId&page=$page&size=50&_format=json&_marker=0&ctx=android"
                            val pageResp = executeRequest(pageUrl) ?: return@async Pair(page, emptyList<Track>())
                            val tracks = mutableListOf<Track>()
                            try {
                                val element = jsonParser.parseToJsonElement(pageResp)
                                if (element is JsonObject) {
                                    val topSongsObj = element.obj("topSongs")
                                    val isLp = topSongsObj?.bool("last_page") ?: element.bool("last_page")
                                    if (isLp) reachedLastPage = true

                                    val songsArr = when (val topEl = element["topSongs"]) {
                                        is JsonArray -> topEl
                                        is JsonObject -> topEl.arr("songs")
                                        else -> null
                                    }
                                    if (songsArr != null) {
                                        for (i in 0 until songsArr.size) {
                                            val item = songsArr[i].let { if (it is JsonObject) it else null } ?: continue
                                            tracks.add(parseSingleTrack(item))
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                            Pair(page, tracks)
                        }
                    }.map { it.await() }
                }

                var newlyAddedInBatch = 0
                for ((_, tracks) in batchResults) {
                    if (tracks.isEmpty()) {
                        consecutiveEmptyPages++
                    } else {
                        consecutiveEmptyPages = 0
                        for (trk in tracks) {
                            if (addTrackIfNew(trk)) {
                                newlyAddedInBatch++
                            }
                        }
                    }
                }

                if (newlyAddedInBatch > 0) {
                    onProgress?.invoke(ArrayList(collectedTracks))
                }

                if (newlyAddedInBatch == 0 && consecutiveEmptyPages >= 2) {
                    reachedLastPage = true
                }
            }
        }

        // 3. Multi-Page Search Complement by Artist Name (Soundtracks, collaborations & singles)
        if (resolvedName.isNotBlank()) {
            val encoded = try { URLEncoder.encode(resolvedName, "UTF-8") } catch (_: Exception) { resolvedName }
            val cleanNameLower = resolvedName.lowercase().trim()

            val searchBatches = coroutineScope {
                (1..4).map { p ->
                    async {
                        val searchUrl = "$BASE_URL?__call=search.getResults&_format=json&_marker=0&cc=in&includeMetaTags=1&q=$encoded&p=$p&n=50"
                        parseSongList(executeRequest(searchUrl))
                    }
                }.map { it.await() }
            }

            var newlyAddedFromSearch = 0
            for (list in searchBatches) {
                for (trk in list) {
                    val trkArtist = trk.artistName.lowercase()
                    if (trkArtist.contains(cleanNameLower) || cleanNameLower.contains(trkArtist)) {
                        if (addTrackIfNew(trk)) {
                            newlyAddedFromSearch++
                        }
                    }
                }
            }

            if (newlyAddedFromSearch > 0) {
                onProgress?.invoke(ArrayList(collectedTracks))
            }
        }

        if (collectedTracks.isEmpty() && resolvedName.isNotBlank()) {
            val encoded = try { URLEncoder.encode(resolvedName, "UTF-8") } catch (_: Exception) { resolvedName }
            val fallbackUrl = "$BASE_URL?__call=search.getResults&_format=json&_marker=0&cc=in&includeMetaTags=1&q=$encoded&p=1&n=50"
            val fallback = parseSongList(executeRequest(fallbackUrl)).distinctBy { it.id }
            if (fallback.isNotEmpty() && cacheKey.isNotBlank()) {
                if (artistTracksCache.size > 50) artistTracksCache.clear()
                artistTracksCache[cacheKey] = fallback
            }
            return@withContext fallback
        }

        if (collectedTracks.isNotEmpty() && cacheKey.isNotBlank()) {
            if (artistTracksCache.size > 50) artistTracksCache.clear()
            artistTracksCache[cacheKey] = collectedTracks
            if (resolvedId.isNotBlank()) artistTracksCache[resolvedId] = collectedTracks
            if (resolvedName.isNotBlank()) artistTracksCache[resolvedName.lowercase()] = collectedTracks
        }

        collectedTracks
    }

    override fun getStreamUrl(track: Track): String? {
        if (track.streamUrl.isNotBlank() && track.streamUrl.startsWith("http")) {
            return track.streamUrl
        }
        val cached = streamCache[track.id]
        if (!cached.isNullOrBlank()) {
            return cached
        }
        val url = "$BASE_URL?__call=song.getDetails&pids=${track.id}&_format=json&_marker=0&ctx=android"
        val response = executeRequest(url) ?: return null
        return try {
            val element = jsonParser.parseToJsonElement(response)
            if (element is JsonObject) {
                val songObj = element[track.id]?.let { if (it is JsonObject) it else null }
                    ?: element["songs"]?.let { if (it is JsonArray && it.isNotEmpty() && it[0] is JsonObject) it[0].jsonObject else null }
                    ?: element.values.firstOrNull { it is JsonObject }?.jsonObject

                val moreInfo = songObj?.obj("more_info")
                val encryptedUrl = songObj?.str("encrypted_media_url").orEmpty().ifBlank {
                    moreInfo?.str("encrypted_media_url").orEmpty()
                }
                val decrypted = decryptMediaUrl(encryptedUrl)
                if (decrypted.isNotBlank()) {
                    streamCache[track.id] = decrypted
                    decrypted
                } else {
                    val previewUrl = songObj?.str("media_preview_url").orEmpty().ifBlank {
                        moreInfo?.str("media_preview_url").orEmpty()
                    }
                    upgradeQuality(previewUrl).takeIf { it.isNotBlank() }?.also {
                        streamCache[track.id] = it
                    }
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // --- JSON PARSING HELPERS ---

    private fun parseSongList(response: String?): List<Track> {
        if (response.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Track>()
        try {
            val element = jsonParser.parseToJsonElement(response)
            if (element is JsonObject) {
                val results = element.arr("results") ?: element.arr("songs") ?: element.arr("data")
                if (results != null) {
                    for (i in 0 until results.size) {
                        val item = results[i].let { if (it is JsonObject) it else null } ?: continue
                        list.add(parseSingleTrack(item))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseAutocompleteSongs(response: String?): List<Track> {
        if (response.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Track>()
        try {
            val element = jsonParser.parseToJsonElement(response)
            if (element is JsonObject) {
                val songsObj = element.obj("songs")
                val data = songsObj?.arr("data")
                if (data != null) {
                    for (i in 0 until data.size) {
                        val item = data[i].let { if (it is JsonObject) it else null } ?: continue
                        list.add(parseSingleTrack(item))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseRecoTracks(response: String?): List<Track> {
        if (response.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Track>()
        try {
            val element = jsonParser.parseToJsonElement(response)
            if (element is JsonObject) {
                for (key in element.keys) {
                    val arr = element.arr(key)
                    if (arr != null) {
                        for (i in 0 until arr.size) {
                            val item = arr[i].let { if (it is JsonObject) it else null } ?: continue
                            list.add(parseSingleTrack(item))
                        }
                    }
                }
                if (list.isEmpty()) {
                    val results = element.arr("results") ?: element.arr("songs") ?: element.arr("data")
                    if (results != null) {
                        for (i in 0 until results.size) {
                            val item = results[i].let { if (it is JsonObject) it else null } ?: continue
                            list.add(parseSingleTrack(item))
                        }
                    }
                }
            } else if (element is JsonArray) {
                for (i in 0 until element.size) {
                    val item = element[i].let { if (it is JsonObject) it else null } ?: continue
                    list.add(parseSingleTrack(item))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseSingleTrack(item: JsonObject): Track {
        val moreInfo = item.obj("more_info")
        val id = item.str("id").ifBlank {
            item.str("song_id").ifBlank {
                item.str("pid")
            }
        }
        val title = cleanText(item.str("song").ifBlank { item.str("title") })
        val artistName = cleanText(
            item.str("primary_artists").ifBlank {
                item.str("singers").ifBlank {
                    item.str("artist").ifBlank {
                        item.str("music").ifBlank {
                            moreInfo?.str("primary_artists").orEmpty().ifBlank {
                                moreInfo?.str("singers").orEmpty().ifBlank {
                                    moreInfo?.str("music").orEmpty()
                                }
                            }
                        }
                    }
                }
            }
        )
        val albumName = cleanText(
            item.str("album").ifBlank {
                moreInfo?.str("album").orEmpty()
            }
        )
        val albumId = item.str("albumid").ifBlank {
            moreInfo?.str("album_id").orEmpty()
        }
        val artworkUrl = cleanArtworkUrl(
            item.str("image").ifBlank {
                moreInfo?.str("image").orEmpty()
            }
        )
        val durationSec = item.long("duration", 0L).let { if (it > 0) it else moreInfo?.long("duration", 0L) ?: 0L }
        val durationMs = durationSec * 1000L

        val encryptedMediaUrl = item.str("encrypted_media_url").ifBlank {
            moreInfo?.str("encrypted_media_url").orEmpty()
        }

        var streamUrl = decryptMediaUrl(encryptedMediaUrl)
        if (streamUrl.isBlank()) {
            val preview = item.str("media_preview_url").ifBlank {
                moreInfo?.str("media_preview_url").orEmpty()
            }
            streamUrl = upgradeQuality(preview)
        }

        if (streamUrl.isNotBlank() && id.isNotBlank()) {
            streamCache[id] = streamUrl
        }

        val year = item.str("year").ifBlank { moreInfo?.str("year").orEmpty() }
        val language = item.str("language").ifBlank { moreInfo?.str("language").orEmpty() }

        return Track(
            id = id,
            title = title.ifBlank { "Untitled Track" },
            artistId = item.str("primary_artists_id"),
            artistName = artistName.ifBlank { "JioSaavn Artist" },
            albumId = albumId,
            albumName = albumName.ifBlank { "Single" },
            artworkUrl = artworkUrl,
            durationMs = durationMs,
            streamUrl = streamUrl,
            genre = language.ifBlank { "Bollywood" },
            provider = "JioSaavn",
            license = "JioSaavn Official Stream",
            releaseDate = year
        )
    }

    private fun parseAlbumList(response: String?): List<Album> {
        if (response.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Album>()
        try {
            val element = jsonParser.parseToJsonElement(response)
            if (element is JsonObject) {
                val results = element.arr("results") ?: element.arr("albums") ?: element.arr("data")
                if (results != null) {
                    for (i in 0 until results.size) {
                        val item = results[i].let { if (it is JsonObject) it else null } ?: continue
                        list.add(parseSingleAlbum(item))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseSingleAlbum(item: JsonObject): Album {
        val moreInfo = item.obj("more_info")
        val id = item.str("id").ifBlank { item.str("albumid") }
        val title = cleanText(item.str("title").ifBlank { item.str("name") })
        val artist = cleanText(
            item.str("artist").ifBlank {
                item.str("primary_artists").ifBlank {
                    moreInfo?.str("artist").orEmpty()
                }
            }
        )
        val artworkUrl = cleanArtworkUrl(item.str("image"))
        val year = item.str("year").ifBlank { moreInfo?.str("year").orEmpty() }
        val language = item.str("language").ifBlank { "Bollywood" }

        return Album(
            id = id,
            title = title.ifBlank { "Album" },
            artistId = item.str("primary_artists_id"),
            artistName = artist.ifBlank { "Various Artists" },
            artworkUrl = artworkUrl,
            releaseDate = year,
            genre = language
        )
    }

    private fun parseArtistList(response: String?): List<Artist> {
        if (response.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Artist>()
        try {
            val element = jsonParser.parseToJsonElement(response)
            if (element is JsonObject) {
                val results = element.arr("results") ?: element.arr("artists") ?: element.arr("data")
                if (results != null) {
                    for (i in 0 until results.size) {
                        val item = results[i].let { if (it is JsonObject) it else null } ?: continue
                        val id = item.str("id").ifBlank { item.str("artistid") }
                        val name = cleanText(item.str("name").ifBlank { item.str("title") })
                        val artworkUrl = cleanArtworkUrl(item.str("image"))
                        val role = cleanText(item.str("role").ifBlank { item.str("description") })

                        list.add(
                            Artist(
                                id = id,
                                name = name.ifBlank { "Artist" },
                                artworkUrl = artworkUrl,
                                bio = role.ifBlank { "Featured Artist on JioSaavn" },
                                genres = listOf("Bollywood", "Indian Pop"),
                                monthlyListeners = 10000000L
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // --- JSON OBJECT EXTENSION UTILS ---

    private fun JsonObject.str(key: String): String {
        val element = this[key] ?: return ""
        return if (element is kotlinx.serialization.json.JsonPrimitive) {
            element.contentOrNull ?: ""
        } else if (element is JsonArray) {
            element.mapNotNull { item ->
                if (item is kotlinx.serialization.json.JsonPrimitive) item.contentOrNull
                else if (item is JsonObject) item.str("name").ifBlank { item.str("title") }
                else null
            }.joinToString(", ")
        } else if (element is JsonObject) {
            element.str("name").ifBlank { element.str("title") }
        } else ""
    }

    private fun JsonObject.long(key: String, default: Long = 0L): Long {
        val element = this[key] ?: return default
        return if (element is kotlinx.serialization.json.JsonPrimitive) {
            element.longOrNull ?: element.contentOrNull?.toLongOrNull() ?: default
        } else default
    }

    private fun JsonObject.obj(key: String): JsonObject? =
        this[key]?.let { if (it is JsonObject) it else null }

    private fun JsonObject.arr(key: String): JsonArray? =
        this[key]?.let { if (it is JsonArray) it else null }

    private fun JsonObject.bool(key: String, default: Boolean = false): Boolean {
        val element = this[key] ?: return default
        return if (element is kotlinx.serialization.json.JsonPrimitive) {
            element.contentOrNull?.toBooleanStrictOrNull() ?: default
        } else default
    }

    private fun JsonObject.hasKey(key: String): Boolean =
        this.containsKey(key)
}
