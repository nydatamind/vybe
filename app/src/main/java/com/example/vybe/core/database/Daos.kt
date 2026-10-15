package com.example.vybe.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: String): TrackEntity?

    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET isLiked = :isLiked WHERE id = :trackId")
    suspend fun updateLikedStatus(trackId: String, isLiked: Boolean)

    @Query("UPDATE tracks SET isDownloaded = :isDownloaded, downloadPath = :downloadPath WHERE id = :trackId")
    suspend fun updateDownloadStatus(trackId: String, isDownloaded: Boolean, downloadPath: String?)

    @Query("""
        SELECT * FROM tracks 
        WHERE title LIKE '%' || :query || '%' 
           OR artistName LIKE '%' || :query || '%' 
           OR albumName LIKE '%' || :query || '%' 
           OR genre LIKE '%' || :query || '%'
        LIMIT 40
    """)
    suspend fun searchTracks(query: String): List<TrackEntity>
}

@Dao
interface LikedTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(likedTrack: LikedTrackEntity)

    @Query("DELETE FROM liked_tracks WHERE trackId = :trackId")
    suspend fun removeLike(trackId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_tracks WHERE trackId = :trackId)")
    suspend fun isTrackLiked(trackId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM liked_tracks WHERE trackId = :trackId)")
    fun isTrackLikedFlow(trackId: String): Flow<Boolean>

    @Query("""
        SELECT t.* FROM tracks t 
        INNER JOIN liked_tracks l ON t.id = l.trackId 
        ORDER BY l.likedAt DESC
    """)
    fun getLikedTracks(): Flow<List<TrackEntity>>
}

data class PlaylistWithCount(
    val id: String,
    val name: String,
    val description: String,
    val artworkUrl: String,
    val createdAt: Long,
    val updatedAt: Long,
    val trackCount: Int
)

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: String, name: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("""
        SELECT p.id, p.name, p.description, p.artworkUrl, p.createdAt, p.updatedAt,
               COUNT(pt.trackId) AS trackCount
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId
        GROUP BY p.id
        ORDER BY p.updatedAt DESC
    """)
    fun getAllPlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("""
        SELECT t.* FROM tracks t 
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId 
        WHERE pt.playlistId = :playlistId 
        ORDER BY pt.position ASC, pt.addedAt ASC
    """)
    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistTrackCount(playlistId: String): Flow<Int>

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updateTrackPosition(playlistId: String, trackId: String, position: Int)

    @Transaction
    suspend fun updatePlaylistTrackOrder(playlistId: String, trackIds: List<String>) {
        trackIds.forEachIndexed { index, trackId ->
            updateTrackPosition(playlistId, trackId, index)
        }
    }
}

@Dao
interface PlaybackHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity)

    @Query("""
        SELECT t.* FROM tracks t 
        INNER JOIN (
            SELECT trackId, MAX(playedAt) as maxPlayedAt 
            FROM playback_history 
            GROUP BY trackId 
            ORDER BY maxPlayedAt DESC 
            LIMIT :limit
        ) h ON t.id = h.trackId
        ORDER BY h.maxPlayedAt DESC
    """)
    fun getRecentlyPlayed(limit: Int = 30): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t 
        INNER JOIN (
            SELECT trackId, COUNT(*) as playCount, MAX(playedAt) as lastPlayed 
            FROM playback_history 
            GROUP BY trackId 
            HAVING COUNT(*) >= 2
            ORDER BY playCount DESC, lastPlayed DESC 
            LIMIT :limit
        ) h ON t.id = h.trackId
        ORDER BY h.playCount DESC, h.lastPlayed DESC
    """)
    fun getMostPlayed(limit: Int = 30): Flow<List<TrackEntity>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

@Dao
interface RecentSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: RecentSearchEntity)

    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearSearches()
}

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun deleteDownload(trackId: String)

    @Query("""
        SELECT t.* FROM tracks t 
        INNER JOIN downloaded_tracks d ON t.id = d.trackId 
        ORDER BY d.downloadedAt DESC
    """)
    fun getDownloadedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun getDownload(trackId: String): DownloadedTrackEntity?

    @Query("SELECT SUM(fileSizeBytes) FROM downloaded_tracks")
    suspend fun getTotalStorageUsedBytes(): Long?

    @Query("SELECT * FROM downloaded_tracks")
    suspend fun getAllDownloads(): List<DownloadedTrackEntity>
}

@Dao
interface SavedArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: SavedArtistEntity)

    @Query("DELETE FROM saved_artists WHERE id = :artistId")
    suspend fun removeArtist(artistId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_artists WHERE id = :artistId)")
    suspend fun isArtistSaved(artistId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM saved_artists WHERE id = :artistId)")
    fun isArtistSavedFlow(artistId: String): Flow<Boolean>

    @Query("SELECT * FROM saved_artists ORDER BY savedAt DESC")
    fun getAllSavedArtists(): Flow<List<SavedArtistEntity>>
}

