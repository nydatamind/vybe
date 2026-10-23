package com.example.vybe.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.vybe.VybeApplication

class CancelDownloadReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "com.example.vybe.playback.ACTION_CANCEL_DOWNLOAD"
        const val EXTRA_TRACK_ID = "extra_track_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TRACK_TITLE = "extra_track_title"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL_DOWNLOAD) return
        val trackId = intent.getStringExtra(EXTRA_TRACK_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, trackId.hashCode())
        val title = intent.getStringExtra(EXTRA_TRACK_TITLE) ?: "Track"

        val app = context.applicationContext as? VybeApplication ?: VybeApplication.instance
        app.downloadRepository.cancelDownload(trackId, notifId)

        try {
            NotificationManagerCompat.from(context).cancel(notifId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(context, "Cancelled download: $title", Toast.LENGTH_SHORT).show()
    }
}
