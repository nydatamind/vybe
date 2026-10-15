package com.example.vybe.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artistId: String = "",
    val artistName: String,
    val albumId: String = "",
    val albumName: String = "",
    val artworkUrl: String = "",
    val durationMs: Long = 0L,
    val streamUrl: String = "",
    val genre: String = "Various",
    val provider: String = "VYBECatalog",
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadPath: String? = null,
    val license: String = "Creative Commons / Open Source",
    val releaseDate: String = ""
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val artworkUrl: String = "",
    val bio: String = "",
    val genres: List<String> = emptyList(),
    val monthlyListeners: Long = 0L
)

@Serializable
data class Album(
    val id: String,
    val title: String,
    val artistId: String = "",
    val artistName: String,
    val artworkUrl: String = "",
    val releaseDate: String = "",
    val genre: String = "",
    val tracks: List<Track> = emptyList()
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val artworkUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val trackCount: Int = 0
)

data class SearchResult(
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList()
) {
    val isEmpty: Boolean
        get() = tracks.isEmpty() && artists.isEmpty() && albums.isEmpty() && playlists.isEmpty()
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class AudioQuality(val title: String, val bitrate: String) {
    LOW("Low", "96 kbps"),
    NORMAL("Normal", "160 kbps"),
    HIGH("High", "320 kbps"),
    ULTRA("Lossless / Master", "Hi-Res FLAC")
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val sleepTimerRemainingSec: Long = 0L,
    val autoSkipIntroOutro: Boolean = true,
    val isAudioDucked: Boolean = false,
    val isAuraSpatialAudioEnabled: Boolean = false
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val positionFormatted: String
        get() {
            val totalSeconds = currentPositionMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

@Serializable
data class LyricLine(
    val timeMs: Long,
    val text: String
)

@Serializable
data class LrcDocument(
    val title: String = "",
    val artist: String = "",
    val lines: List<LyricLine> = emptyList()
)

