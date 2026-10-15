package com.example.vybe.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import com.example.vybe.MainActivity
import com.example.vybe.R
import com.example.vybe.core.database.DownloadDao
import com.example.vybe.core.database.DownloadedTrackEntity
import com.example.vybe.core.database.TrackDao
import com.example.vybe.core.datastore.PreferencesManager
import com.example.vybe.core.model.Track
import com.example.vybe.playback.PlayNowReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class DownloadRepository(
    private val context: Context,
    val preferencesManager: PreferencesManager,
    private val downloadDao: DownloadDao,
    private val trackDao: TrackDao
) {
    private val httpClient = OkHttpClient()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "vibra_song_downloads"
        const val NOTIFICATION_CHANNEL_NAME = "Song Downloads"
        const val ERROR_FOLDER_NOT_SET = "DOWNLOAD_FOLDER_NOT_SET"
    }

    init {
        createNotificationChannel()
    }

    val downloadedTracks: Flow<List<Track>> = downloadDao.getDownloadedTracks().map { list ->
        list.map { it.toTrack() }
    }

    suspend fun isDownloadFolderConfigured(): Boolean {
        val tree = preferencesManager.downloadTreeUri.first()
        return !tree.isNullOrBlank()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows downloading progress and completion for songs"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateProgressNotification(
        notificationId: Int,
        title: String,
        artist: String,
        progress: Int,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L,
        etaSeconds: Int? = null,
        batchInfo: String? = null,
        indeterminate: Boolean = false
    ) {
        try {
            val etaText = if (etaSeconds != null && etaSeconds > 0) " • ${etaSeconds}s left" else ""
            val sizeText = if (downloadedBytes > 0 && totalBytes > 0) {
                val dlMb = downloadedBytes / (1024.0 * 1024.0)
                val totMb = totalBytes / (1024.0 * 1024.0)
                " (%.1f / %.1f MB)".format(dlMb, totMb)
            } else ""

            val progressText = when {
                indeterminate -> "Downloading... • $artist"
                batchInfo != null -> "$batchInfo$sizeText$etaText"
                else -> "Downloading $progress%$sizeText$etaText • $artist"
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(progressText)
                .setContentIntent(openAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress.coerceIn(0, 100), indeterminate)

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun completeNotification(
        notificationId: Int,
        track: Track,
        savedFolder: String,
        savedUriString: String? = null
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

            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText("100% • ${track.title} • ${track.artistName}")
                .setContentIntent(openAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false)
                .addAction(
                    R.drawable.ic_widget_play,
                    "▶ Play Now",
                    playPendingIntent
                )

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun failNotification(notificationId: Int, title: String) {
        try {
            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Download Failed")
                .setContentText("Could not download $title")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false)

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
                    "Downloading $currentNum of $totalTracks songs • $overallPercent% overall"
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
            "Vibra Music"
        }

        try {
            if (track.streamUrl.isBlank()) {
                failNotification(notificationId, track.title)
                return@withContext Result.failure(IllegalArgumentException("Invalid stream URL"))
            }

            // Initial notification (0%)
            val initialBatch = batchInfoProvider?.invoke(0)
            updateProgressNotification(
                notificationId = notificationId,
                title = track.title,
                artist = track.artistName,
                progress = 0,
                batchInfo = initialBatch
            )

            var newDocFile: DocumentFile? = null
            var fallbackFile: File? = null
            val outputStream: OutputStream

            if (isCustomFolder) {
                val treeUri = Uri.parse(treeUriString)
                val rootDir = DocumentFile.fromTreeUri(context, treeUri)
                    ?: run {
                        failNotification(notificationId, track.title)
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
                        failNotification(notificationId, track.title)
                        return@withContext Result.failure(IllegalStateException("Could not create file in $folderDisplayName"))
                    }
                newDocFile = created
                outputStream = context.contentResolver.openOutputStream(created.uri)
                    ?: run {
                        created.delete()
                        failNotification(notificationId, track.title)
                        return@withContext Result.failure(IllegalStateException("Cannot open output stream for download"))
                    }
            } else {
                val fallbackDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: context.filesDir, "Vibra_Downloads").apply { mkdirs() }
                val targetFile = File(fallbackDir, fileName)
                if (targetFile.exists()) {
                    try { targetFile.delete() } catch (_: Exception) {}
                }
                targetFile.createNewFile()
                fallbackFile = targetFile
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
                failNotification(notificationId, track.title)
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
                        trackTitle = track.title,
                        artist = track.artistName,
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
                completeNotification(notificationId, track, folderDisplayName, savedUriString)
            }
            Result.success(savedUriString)
        } catch (e: Exception) {
            e.printStackTrace()
            failNotification(notificationId, track.title)
            Result.failure(e)
        }
    }

    private fun copyStreamWithProgress(
        inputStream: InputStream,
        outputStream: OutputStream,
        totalBytes: Long,
        notificationId: Int,
        trackTitle: String,
        artist: String,
        batchInfoProvider: ((percent: Int) -> String)? = null
    ) {
        val buffer = ByteArray(16 * 1024)
        var bytesRead: Int
        var totalRead = 0L
        var lastReportedPercent = -1
        var lastReportedTime = 0L
        val startTime = System.currentTimeMillis()

        // Immediate 0% initial update
        val initialBatch = batchInfoProvider?.invoke(0)
        updateProgressNotification(
            notificationId = notificationId,
            title = trackTitle,
            artist = artist,
            progress = 0,
            downloadedBytes = 0L,
            totalBytes = totalBytes,
            batchInfo = initialBatch
        )
        lastReportedPercent = 0
        lastReportedTime = startTime

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalRead += bytesRead

            val currentTime = System.currentTimeMillis()
            val elapsedTimeMs = currentTime - startTime

            if (totalBytes > 0) {
                // Keep progress within 0..98 while downloading so 100% is only shown upon completion
                val percent = ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 98)

                val etaSec = if (elapsedTimeMs > 400 && totalRead > 10240) {
                    val bytesPerSec = (totalRead * 1000L) / elapsedTimeMs
                    if (bytesPerSec > 0) {
                        val remainingBytes = (totalBytes - totalRead).coerceAtLeast(0)
                        (remainingBytes / bytesPerSec).toInt().coerceAtMost(3600)
                    } else null
                } else null

                if (percent > lastReportedPercent && (percent - lastReportedPercent >= 3 || currentTime - lastReportedTime >= 200)) {
                    lastReportedPercent = percent
                    lastReportedTime = currentTime
                    val currentBatch = batchInfoProvider?.invoke(percent)
                    updateProgressNotification(
                        notificationId = notificationId,
                        title = trackTitle,
                        artist = artist,
                        progress = percent,
                        downloadedBytes = totalRead,
                        totalBytes = totalBytes,
                        etaSeconds = etaSec,
                        batchInfo = currentBatch
                    )
                }
            } else {
                if (currentTime - lastReportedTime >= 300) {
                    lastReportedTime = currentTime
                    val currentBatch = batchInfoProvider?.invoke(0)
                    updateProgressNotification(
                        notificationId = notificationId,
                        title = trackTitle,
                        artist = artist,
                        progress = 0,
                        downloadedBytes = totalRead,
                        totalBytes = 0L,
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
