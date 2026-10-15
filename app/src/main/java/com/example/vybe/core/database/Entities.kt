package com.example.vybe.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.vybe.core.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val albumId: String,
    val albumName: String,
    val artworkUrl: String,
    val durationMs: Long,
    val streamUrl: String,
    val genre: String,
    val provider: String,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadPath: String? = null,
    val license: String = "",
    val releaseDate: String = ""
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artistId = artistId,
        artistName = artistName,
        albumId = albumId,
        albumName = albumName,
        artworkUrl = artworkUrl,
        durationMs = durationMs,
        streamUrl = streamUrl,
        genre = genre,
        provider = provider,
        isLiked = isLiked,
        isDownloaded = isDownloaded,
        downloadPath = downloadPath,
        license = license,
        releaseDate = releaseDate
    )

    companion object {
        fun fromTrack(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artistId = track.artistId,
            artistName = track.artistName,
            albumId = track.albumId,
            albumName = track.albumName,
            artworkUrl = track.artworkUrl,
            durationMs = track.durationMs,
            streamUrl = track.streamUrl,
            genre = track.genre,
            provider = track.provider,
            isLiked = track.isLiked,
            isDownloaded = track.isDownloaded,
            downloadPath = track.downloadPath,
            license = track.license,
            releaseDate = track.releaseDate
        )
    }
}

@Entity(tableName = "liked_tracks")
data class LikedTrackEntity(
    @PrimaryKey val trackId: String,
    val likedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val artworkUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val trackId: String,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0L,
    val trackId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey val trackId: String,
    val localFilePath: String,
    val fileSizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_artists")
data class SavedArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artworkUrl: String = "",
    val savedAt: Long = System.currentTimeMillis()
)

