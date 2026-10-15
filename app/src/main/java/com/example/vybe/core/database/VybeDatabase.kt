package com.example.vybe.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class,
        LikedTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class,
        RecentSearchEntity::class,
        DownloadedTrackEntity::class,
        SavedArtistEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VybeDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun likedTrackDao(): LikedTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): PlaybackHistoryDao
    abstract fun searchHistoryDao(): RecentSearchDao
    abstract fun downloadDao(): DownloadDao
    abstract fun savedArtistDao(): SavedArtistDao

    companion object {
        @Volatile
        private var INSTANCE: VybeDatabase? = null

        fun getInstance(context: Context): VybeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VybeDatabase::class.java,
                    "vybe_music_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
