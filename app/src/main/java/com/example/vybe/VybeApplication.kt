package com.example.vybe

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.vybe.core.database.VybeDatabase
import com.example.vybe.core.datastore.PreferencesManager
import com.example.vybe.data.provider.CompositeAudioProvider
import com.example.vybe.data.repository.DownloadRepository
import com.example.vybe.data.repository.MusicRepository
import com.example.vybe.playback.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VybeApplication : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: VybeDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var downloadRepository: DownloadRepository
        private set

    lateinit var musicRepository: MusicRepository
        private set

    lateinit var playbackManager: PlaybackManager
        private set

    lateinit var storageCleanupManager: com.example.vybe.core.storage.StorageCleanupManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Global Crash Guard: Prevents background coroutines/threads from shutting down the app
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("VYBECrashGuard", "Uncaught exception in thread ${thread.name}", throwable)
            if (thread.name.contains("main", ignoreCase = true)) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        // Initialize Dynamic Remote Config & Auto-Failover Engine
        com.example.vybe.core.remote.RemoteConfigManager.init(this)
        com.example.vybe.core.remote.RemoteConfigManager.syncRemoteConfigAsync(applicationScope)

        database = VybeDatabase.getInstance(this)
        preferencesManager = PreferencesManager(this)

        storageCleanupManager = com.example.vybe.core.storage.StorageCleanupManager.getInstance(this)
        storageCleanupManager.scheduleMaintenance()

        downloadRepository = DownloadRepository(
            context = this,
            preferencesManager = preferencesManager,
            downloadDao = database.downloadDao(),
            trackDao = database.trackDao()
        )

        val audioProvider = CompositeAudioProvider()

        musicRepository = MusicRepository(
            audioProvider = audioProvider,
            trackDao = database.trackDao(),
            likedTrackDao = database.likedTrackDao(),
            playlistDao = database.playlistDao(),
            historyDao = database.historyDao(),
            recentSearchDao = database.searchHistoryDao(),
            savedArtistDao = database.savedArtistDao(),
            downloadRepository = downloadRepository
        )

        playbackManager = PlaybackManager(
            context = this,
            musicRepository = musicRepository
        )

        // Preload curated catalog in background
        applicationScope.launch {
            musicRepository.preloadCatalog()
        }

        // High-Speed Network Pre-Warming: Pre-resolve DNS and warm CDN connections in background
        applicationScope.launch(Dispatchers.IO) {
            try {
                musicRepository.prewarmSearch()
                val hosts = listOf(
                    "www.jiosaavn.com",
                    "aac.saavncdn.com",
                    "c.saavncdn.com",
                    "static.saavncdn.com",
                    "images.unsplash.com",
                    "is1-ssl.mzstatic.com"
                )
                for (host in hosts) {
                    launch {
                        try {
                            java.net.InetAddress.getAllByName(host)
                        } catch (e: Exception) {
                            // Non-blocking
                        }
                    }
                }
            } catch (e: Exception) {
                // Non-blocking
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(coil.intercept.Interceptor { chain ->
                    val request = chain.request
                    val result = chain.proceed(request)
                    if (result is coil.request.ErrorResult) {
                        val dataStr = request.data.toString()
                        if (dataStr.contains("500x500.jpg")) {
                            val fallbackUrl = dataStr.replace("500x500.jpg", "150x150.jpg")
                            val fallbackRequest = request.newBuilder().data(fallbackUrl).build()
                            return@Interceptor chain.proceed(fallbackRequest)
                        } else if (dataStr.contains("500x500.png")) {
                            val fallbackUrl = dataStr.replace("500x500.png", "150x150.png")
                            val fallbackRequest = request.newBuilder().data(fallbackUrl).build()
                            return@Interceptor chain.proceed(fallbackRequest)
                        }
                    }
                    result
                })
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // 15% heap limit for lean memory footprint
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(2 * 1024 * 1024L) // 2 MB strict disk cache for album artwork & artist thumbnails
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .crossfade(100)
            .build()
    }

    override fun onTerminate() {
        playbackManager.release()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: VybeApplication
            private set
    }
}
