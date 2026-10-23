package com.example.vybe.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import coil.Coil
import coil.request.ImageRequest
import com.example.vybe.MainActivity
import com.example.vybe.R
import com.example.vybe.core.database.DownloadDao
import com.example.vybe.core.database.DownloadedTrackEntity
import com.example.vybe.core.database.TrackDao
import com.example.vybe.core.datastore.PreferencesManager
import com.example.vybe.core.model.Track
import com.example.vybe.playback.CancelDownloadReceiver
import com.example.vybe.playback.PlayNowReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class DownloadRepository(
    private val context: Context,
    val preferencesManager: PreferencesManager,
    private val downloadDao: DownloadDao,
    private val trackDao: TrackDao
) {
    private val httpClient = OkHttpClient()

    // Active download task references for 1-tap notification cancellation & cleanup
    private val activeDownloadJobs = ConcurrentHashMap<String, Job>()
    private val activeDocFiles = ConcurrentHashMap<String, DocumentFile>()
    private val activeFallbackFiles = ConcurrentHashMap<String, File>()

    companion object {
        const val CHANNEL_ACTIVE_DOWNLOADS_ID = "vybe_downloads_active"
        const val CHANNEL_ACTIVE_DOWNLOADS_NAME = "Active Downloads"

        const val CHANNEL_COMPLETED_DOWNLOADS_ID = "vybe_downloads_complete"
        const val CHANNEL_COMPLETED_DOWNLOADS_NAME = "Completed Downloads"

        const val ERROR_FOLDER_NOT_SET = "DOWNLOAD_FOLDER_NOT_SET"
        private const val BRAND_CYAN = 0xFF00E5FF.toInt()
        private const val ERROR_RED = 0xFFFF3366.toInt()
    }

    init {
        createNotificationChannels()
    }

    val downloadedTracks: Flow<List<Track>> = downloadDao.getDownloadedTracks().map { list ->
        list.map { it.toTrack() }
    }

    suspend fun isDownloadFolderConfigured(): Boolean {
        val tree = preferencesManager.downloadTreeUri.first()
        return !tree.isNullOrBlank()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // Clean up legacy channel if present
            try {
                manager?.deleteNotificationChannel("vibra_song_downloads")
            } catch (_: Exception) {}

            // Channel 1: Real-time progress (Silent, LOW importance, no annoying vibrations/sound per percent)
            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE_DOWNLOADS_ID,
                CHANNEL_ACTIVE_DOWNLOADS_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time download progress, download speed, and ETA"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            // Channel 2: Completed downloads (DEFAULT importance, subtle sound/vibration notification)
            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED_DOWNLOADS_ID,
                CHANNEL_COMPLETED_DOWNLOADS_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when offline songs or albums have finished downloading"
                setShowBadge(true)
                enableVibration(true)
            }

            manager?.createNotificationChannel(activeChannel)
            manager?.createNotificationChannel(completedChannel)
        }
    }

    /**
     * Cancels an ongoing download and cleans up temporary partial files.
     */
    fun cancelDownload(trackId: String, notificationId: Int = trackId.hashCode()) {
        val job = activeDownloadJobs.remove(trackId)
        job?.cancel()

        activeDocFiles.remove(trackId)?.let {
            try {
                if (it.exists()) it.delete()
            } catch (_: Exception) {}
        }

        activeFallbackFiles.remove(trackId)?.let {
            try {
                if (it.exists()) it.delete()
            } catch (_: Exception) {}
        }

        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (_: Exception) {}
    }

    private suspend fun loadTrackArtworkBitmap(artworkUrl: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (artworkUrl.isNullOrBlank()) return@withContext null
        try {
            val loader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .allowHardware(false)
                .size(300, 300)
                .build()
            val result = loader.execute(request)
            val drawable = result.drawable
            if (drawable is BitmapDrawable) {
                val bmp = drawable.bitmap
                if (bmp.width > 300 || bmp.height > 300) {
                    Bitmap.createScaledBitmap(bmp, 300, 300, true)
                } else bmp
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun updateProgressNotification(
        notificationId: Int,
        track: Track,
        progress: Int,
        artworkBitmap: Bitmap? = null,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L,
        speedBytesPerSec: Long = 0L,
        etaSeconds: Int? = null,
        batchInfo: String? = null,
        indeterminate: Boolean = false
    ) {
        try {
            val speedText = if (speedBytesPerSec > 0) {
                val speedKb = speedBytesPerSec / 1024.0
                if (speedKb >= 1024.0) {
                    "⚡ %.1f MB/s".format(speedKb / 1024.0)
                } else {
                    "⚡ %.0f KB/s".format(speedKb)
                }
            } else ""

            val etaText = if (etaSeconds != null && etaSeconds > 0) {
                if (etaSeconds >= 60) " • ${etaSeconds / 60}m ${etaSeconds % 60}s left" else " • ${etaSeconds}s left"
            } else ""

            val sizeText = if (downloadedBytes > 0 && totalBytes > 0) {
                val dlMb = downloadedBytes / (1024.0 * 1024.0)
                val totMb = totalBytes / (1024.0 * 1024.0)
                " (%.1f / %.1f MB)".format(dlMb, totMb)
            } else ""

            val detailsText = when {
                indeterminate -> "Downloading... • ${track.artistName}"
                batchInfo != null -> "$batchInfo$sizeText • $speedText$etaText"
                speedText.isNotBlank() -> "$progress% • $speedText$sizeText$etaText • ${track.artistName}"
                else -> "Downloading $progress%$sizeText$etaText • ${track.artistName}"
            }

            val subText = if (batchInfo != null) "VYBE Downloads • Batch" else "VYBE Downloads • 320kbps HD"

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cancelIntent = Intent(context, CancelDownloadReceiver::class.java).apply {
                action = CancelDownloadReceiver.ACTION_CANCEL_DOWNLOAD
                putExtra(CancelDownloadReceiver.EXTRA_TRACK_ID, track.id)
                putExtra(CancelDownloadReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(CancelDownloadReceiver.EXTRA_TRACK_TITLE, track.title)
            }
            val cancelPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVE_DOWNLOADS_ID)
                .setSmallIcon(R.drawable.ic_notif_download)
                .setContentTitle(track.title)
                .setContentText(detailsText)
                .setSubText(subText)
                .setContentIntent(openAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setColor(BRAND_CYAN)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress.coerceIn(0, 100), indeterminate)
                .addAction(
                    R.drawable.ic_notif_close,
                    "Cancel",
                    cancelPendingIntent
                )

            artworkBitmap?.let { builder.setLargeIcon(it) }

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun completeNotification(
        notificationId: Int,
        track: Track,
        savedFolder: String,
        savedUriString: String? = null,
        artworkBitmap: Bitmap? = null
    ) {
        try {
            val playIntent = Intent(context, PlayNowReceiver::class.java).apply {
                action = PlayNowReceiver.ACTION_PLAY_NOW
                putExtra(PlayNowReceiver.EXTRA_TRACK_ID, track.id)
                putExtra(PlayNowReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(PlayNowReceiver.EXTRA_TRACK_TITLE, track.title)
                putExtra(PlayNowReceiver.EXTRA_TRACK_ARTIST, track.artistName)
                putExtra(PlayNowReceiver.EXTRA_TRACK_ARTWORK, track.artworkUrl)
                putExtra(PlayNowReceiver.EXTRA_TRACK_ALBUM, track.albumName)
                putExtra(PlayNowReceiver.EXTRA_TRACK_DURATION, track.durationMs)
                if (!savedUriString.isNullOrBlank()) {
                    putExtra(PlayNowReceiver.EXTRA_LOCAL_URI, savedUriString)
                }
            }
            val playPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETED_DOWNLOADS_ID)
                .setSmallIcon(R.drawable.ic_notif_download_done)
                .setContentTitle("Download Complete")
                .setContentText("${track.title} • ${track.artistName}")
                .setSubText("Saved in $savedFolder • 320kbps HD")
                .setContentIntent(openAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setColor(BRAND_CYAN)
                .setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false)
                .addAction(
                    R.drawable.ic_widget_play,
                    "▶ Play Now",
                    playPendingIntent
                )
                .addAction(
                    R.drawable.ic_notif_download_done,
                    "Library",
                    openAppPendingIntent
                )

            artworkBitmap?.let { builder.setLargeIcon(it) }

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun failNotification(notificationId: Int, title: String, artworkBitmap: Bitmap? = null) {
        try {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVE_DOWNLOADS_ID)
                .setSmallIcon(R.drawable.ic_notif_close)
                .setContentTitle("Download Failed")
                .setContentText("Could not download $title")
                .setSubText("VYBE Downloads")
                .setContentIntent(openAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(ERROR_RED)
                .setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false)

            artworkBitmap?.let { builder.setLargeIcon(it) }

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun downloadTrack(track: Track): Result<String> = downloadTrackInternal(track)

    suspend fun downloadPlaylist(
        playlistTitle: String,
        tracks: List<Track>
    ): List<Result<String>> = withContext(Dispatchers.IO) {
        val totalTracks = tracks.size
        val results = mutableListOf<Result<String>>()
        val batchNotificationId = ("batch_" + playlistTitle.hashCode()).hashCode()

        tracks.forEachIndexed { index, track ->
            val currentNum = index + 1
            val baseOverallPercent = (index * 100) / totalTracks
            val result = downloadTrackInternal(
                track = track,
                customNotificationId = batchNotificationId,
                batchInfoProvider = { currentSongPercent ->
                    val songContribution = (currentSongPercent.toFloat() / 100f * (100f / totalTracks)).toInt()
                    val overallPercent = (baseOverallPercent + songContribution).coerceIn(0, 100)
                    "Song $currentNum of $totalTracks • $overallPercent% overall"
                },
                showCompletionNotification = (index == tracks.lastIndex)
            )
            results.add(result)
        }
        results
    }

    private suspend fun downloadTrackInternal(
        track: Track,
        customNotificationId: Int? = null,
        batchInfoProvider: ((percent: Int) -> String)? = null,
        showCompletionNotification: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val notificationId = customNotificationId ?: track.id.hashCode()
        val safeTitle = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val safeArtist = track.artistName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val fileName = "$safeTitle - $safeArtist.mp3"

        val treeUriString = preferencesManager.downloadTreeUri.first()
        val isCustomFolder = !treeUriString.isNullOrBlank()
        val folderDisplayName = if (isCustomFolder) {
            preferencesManager.downloadFolderName.first() ?: "Selected Folder"
        } else {
            "VYBE Music"
        }

        // Pre-load artwork bitmap for rich notification presentation
        val artworkBitmap = loadTrackArtworkBitmap(track.artworkUrl)

        // Register active job for 1-tap notification cancellation
        val currentJob = coroutineContext[Job]
        if (currentJob != null) {
            activeDownloadJobs[track.id] = currentJob
        }

        var newDocFile: DocumentFile? = null
        var fallbackFile: File? = null

        try {
            if (track.streamUrl.isBlank()) {
                failNotification(notificationId, track.title, artworkBitmap)
                return@withContext Result.failure(IllegalArgumentException("Invalid stream URL"))
            }

            // Initial progress notification (0%)
            val initialBatch = batchInfoProvider?.invoke(0)
            updateProgressNotification(
                notificationId = notificationId,
                track = track,
                progress = 0,
                artworkBitmap = artworkBitmap,
                batchInfo = initialBatch
            )

            val outputStream: OutputStream

            if (isCustomFolder) {
                val treeUri = Uri.parse(treeUriString)
                val rootDir = DocumentFile.fromTreeUri(context, treeUri)
                    ?: run {
                        failNotification(notificationId, track.title, artworkBitmap)
                        return@withContext Result.failure(IllegalStateException("Cannot access selected folder: $folderDisplayName"))
                    }

                // Remove existing file with the same name if present
                val existing = rootDir.findFile(fileName)
                if (existing != null && existing.exists()) {
                    try {
                        existing.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val created = rootDir.createFile("audio/mpeg", fileName)
                    ?: run {
                        failNotification(notificationId, track.title, artworkBitmap)
                        return@withContext Result.failure(IllegalStateException("Could not create file in $folderDisplayName"))
                    }
                newDocFile = created
                activeDocFiles[track.id] = created
                outputStream = context.contentResolver.openOutputStream(created.uri)
                    ?: run {
                        created.delete()
                        failNotification(notificationId, track.title, artworkBitmap)
                        return@withContext Result.failure(IllegalStateException("Cannot open output stream for download"))
                    }
            } else {
                val fallbackDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: context.filesDir, "VYBE_Downloads").apply { mkdirs() }
                val targetFile = File(fallbackDir, fileName)
                if (targetFile.exists()) {
                    try { targetFile.delete() } catch (_: Exception) {}
                }
                targetFile.createNewFile()
                fallbackFile = targetFile
                activeFallbackFiles[track.id] = targetFile
                outputStream = java.io.FileOutputStream(targetFile)
            }

            val request = Request.Builder()
                .url(track.streamUrl)
                .header("Accept-Encoding", "identity")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                newDocFile?.delete()
                fallbackFile?.delete()
                failNotification(notificationId, track.title, artworkBitmap)
                return@withContext Result.failure(Exception("Failed to download audio file"))
            }

            val body = response.body!!
            var totalBytes = body.contentLength()
            if (totalBytes <= 0L) {
                totalBytes = if (track.durationMs > 0) {
                    (track.durationMs / 1000L) * 20000L
                } else {
                    4L * 1024 * 1024
                }
            }

            outputStream.use { out ->
                body.byteStream().use { inputStream ->
                    copyStreamWithProgress(
                        inputStream = inputStream,
                        outputStream = out,
                        totalBytes = totalBytes,
                        notificationId = notificationId,
                        track = track,
                        artworkBitmap = artworkBitmap,
                        batchInfoProvider = batchInfoProvider
                    )
                }
            }

            val savedUriString = newDocFile?.uri?.toString() ?: fallbackFile?.let { Uri.fromFile(it).toString() } ?: ""
            val fileSizeBytes = newDocFile?.length() ?: fallbackFile?.length() ?: 0L

            val entity = DownloadedTrackEntity(
                trackId = track.id,
                localFilePath = savedUriString,
                fileSizeBytes = fileSizeBytes,
                downloadedAt = System.currentTimeMillis()
            )
            downloadDao.insertDownload(entity)
            trackDao.updateDownloadStatus(track.id, true, savedUriString)

            if (showCompletionNotification) {
                completeNotification(notificationId, track, folderDisplayName, savedUriString, artworkBitmap)
            }

            Result.success(savedUriString)
        } catch (e: CancellationException) {
            // User cancelled download from notification action
            newDocFile?.let { try { if (it.exists()) it.delete() } catch (_: Exception) {} }
            fallbackFile?.let { try { if (it.exists()) it.delete() } catch (_: Exception) {} }
            try { NotificationManagerCompat.from(context).cancel(notificationId) } catch (_: Exception) {}
            Result.failure(e)
        } catch (e: Exception) {
            e.printStackTrace()
            failNotification(notificationId, track.title, artworkBitmap)
            Result.failure(e)
        } finally {
            activeDownloadJobs.remove(track.id)
            activeDocFiles.remove(track.id)
            activeFallbackFiles.remove(track.id)
        }
    }

    private suspend fun copyStreamWithProgress(
        inputStream: InputStream,
        outputStream: OutputStream,
        totalBytes: Long,
        notificationId: Int,
        track: Track,
        artworkBitmap: Bitmap?,
        batchInfoProvider: ((percent: Int) -> String)? = null
    ) {
        val buffer = ByteArray(32 * 1024)
        var bytesRead: Int
        var totalRead = 0L
        var lastReportedPercent = -1
        var lastReportedTime = System.currentTimeMillis()
        var lastReportedBytes = 0L
        var smoothedSpeed = 0L

        // Immediate 0% initial update
        val initialBatch = batchInfoProvider?.invoke(0)
        updateProgressNotification(
            notificationId = notificationId,
            track = track,
            progress = 0,
            artworkBitmap = artworkBitmap,
            downloadedBytes = 0L,
            totalBytes = totalBytes,
            batchInfo = initialBatch
        )

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Download cancelled by user")
            }

            outputStream.write(buffer, 0, bytesRead)
            totalRead += bytesRead

            val currentTime = System.currentTimeMillis()
            val timeSinceLastReportMs = currentTime - lastReportedTime

            if (totalBytes > 0) {
                // Keep progress within 0..98 while downloading so 100% is only shown upon verified completion
                val percent = ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 98)

                if (timeSinceLastReportMs >= 250 || (percent - lastReportedPercent >= 3)) {
                    val bytesSinceLastReport = totalRead - lastReportedBytes
                    if (timeSinceLastReportMs > 50) {
                        val instantSpeed = (bytesSinceLastReport * 1000L) / timeSinceLastReportMs
                        smoothedSpeed = if (smoothedSpeed == 0L) instantSpeed else ((smoothedSpeed * 6 + instantSpeed * 4) / 10)
                    }

                    val etaSec = if (smoothedSpeed > 1024L) {
                        val remainingBytes = (totalBytes - totalRead).coerceAtLeast(0)
                        (remainingBytes / smoothedSpeed).toInt().coerceAtMost(3600)
                    } else null

                    lastReportedPercent = percent
                    lastReportedTime = currentTime
                    lastReportedBytes = totalRead

                    val currentBatch = batchInfoProvider?.invoke(percent)
                    updateProgressNotification(
                        notificationId = notificationId,
                        track = track,
                        progress = percent,
                        artworkBitmap = artworkBitmap,
                        downloadedBytes = totalRead,
                        totalBytes = totalBytes,
                        speedBytesPerSec = smoothedSpeed,
                        etaSeconds = etaSec,
                        batchInfo = currentBatch
                    )
                }
            } else {
                if (timeSinceLastReportMs >= 350) {
                    val bytesSinceLastReport = totalRead - lastReportedBytes
                    if (timeSinceLastReportMs > 50) {
                        val instantSpeed = (bytesSinceLastReport * 1000L) / timeSinceLastReportMs
                        smoothedSpeed = if (smoothedSpeed == 0L) instantSpeed else ((smoothedSpeed * 6 + instantSpeed * 4) / 10)
                    }
                    lastReportedTime = currentTime
                    lastReportedBytes = totalRead

                    val currentBatch = batchInfoProvider?.invoke(0)
                    updateProgressNotification(
                        notificationId = notificationId,
                        track = track,
                        progress = 0,
                        artworkBitmap = artworkBitmap,
                        downloadedBytes = totalRead,
                        totalBytes = 0L,
                        speedBytesPerSec = smoothedSpeed,
                        batchInfo = currentBatch,
                        indeterminate = true
                    )
                }
            }
        }
        outputStream.flush()
    }

    suspend fun deleteDownload(trackId: String) = withContext(Dispatchers.IO) {
        val existing = downloadDao.getDownload(trackId)
        if (existing != null) {
            try {
                if (existing.localFilePath.startsWith("content://")) {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(existing.localFilePath))
                    doc?.delete()
                } else {
                    val file = File(existing.localFilePath)
                    if (file.exists()) file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            downloadDao.deleteDownload(trackId)
            trackDao.updateDownloadStatus(trackId, false, null)
        }
    }

    suspend fun getStorageUsedFormatted(): String = withContext(Dispatchers.IO) {
        try {
            val totalBytes = downloadDao.getTotalStorageUsedBytes() ?: 0L
            val mb = totalBytes / (1024.0 * 1024.0)
            "%.1f MB".format(mb)
        } catch (e: Exception) {
            "0 MB"
        }
    }
}
