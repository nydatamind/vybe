package com.example.vybe.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.vybe.VybeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayNowReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PLAY_NOW = "com.example.vybe.playback.ACTION_PLAY_NOW"
        const val EXTRA_TRACK_ID = "extra_track_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_LOCAL_URI = "extra_local_uri"
        const val EXTRA_TRACK_TITLE = "extra_track_title"
        const val EXTRA_TRACK_ARTIST = "extra_track_artist"
        const val EXTRA_TRACK_ARTWORK = "extra_track_artwork"
        const val EXTRA_TRACK_ALBUM = "extra_track_album"
        const val EXTRA_TRACK_DURATION = "extra_track_duration"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PLAY_NOW) return
        val trackId = intent.getStringExtra(EXTRA_TRACK_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val localUri = intent.getStringExtra(EXTRA_LOCAL_URI)
        val title = intent.getStringExtra(EXTRA_TRACK_TITLE) ?: ""
        val artist = intent.getStringExtra(EXTRA_TRACK_ARTIST) ?: ""
        val artwork = intent.getStringExtra(EXTRA_TRACK_ARTWORK) ?: ""
        val album = intent.getStringExtra(EXTRA_TRACK_ALBUM) ?: ""
        val duration = intent.getLongExtra(EXTRA_TRACK_DURATION, 0L)

        if (notifId != -1) {
            try {
                NotificationManagerCompat.from(context).cancel(notifId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val app = context.applicationContext as? VybeApplication ?: VybeApplication.instance

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val trackEntity = app.database.trackDao().getTrackById(trackId)
                var track: com.example.vybe.core.model.Track? = null
                if (trackEntity != null) {
                    track = trackEntity.toTrack().copy(
                        isDownloaded = true,
                        downloadPath = localUri ?: trackEntity.downloadPath
                    )
                } else if (title.isNotBlank()) {
                    track = com.example.vybe.core.model.Track(
                        id = trackId,
                        title = title,
                        artistName = artist.ifBlank { "Various Artists" },
                        albumName = album.ifBlank { "Single" },
                        artworkUrl = artwork,
                        durationMs = duration,
                        streamUrl = localUri ?: "",
                        downloadPath = localUri,
                        isDownloaded = true
                    )
                }

                if (track != null) {
                    withContext(Dispatchers.Main) {
                        app.playbackManager.playTrack(track)
                        Toast.makeText(app, "▶ Playing: ${track.title}", Toast.LENGTH_SHORT).show()
                        try {
                            val openIntent = Intent(context, com.example.vybe.MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            context.startActivity(openIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
