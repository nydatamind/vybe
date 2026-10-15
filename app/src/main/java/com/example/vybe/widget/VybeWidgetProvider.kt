package com.example.vybe.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.widget.RemoteViews
import coil.Coil
import coil.request.ImageRequest
import com.example.vybe.MainActivity
import com.example.vybe.R
import com.example.vybe.VybeApplication

class VybeWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        try {
            val playbackManager = VybeApplication.instance.playbackManager
            when (action) {
                ACTION_WIDGET_PLAY_PAUSE -> {
                    playbackManager.togglePlayPause()
                }
                ACTION_WIDGET_NEXT -> {
                    playbackManager.skipToNext()
                }
                ACTION_WIDGET_PREV -> {
                    playbackManager.skipToPrevious()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val playbackState = try {
            VybeApplication.instance.playbackManager.playbackState.value
        } catch (e: Exception) {
            null
        }

        val currentTrack = playbackState?.currentTrack
        val title = currentTrack?.title ?: "VYBE Music"
        val artist = currentTrack?.artistName ?: "Tap to play music"
        val isPlaying = playbackState?.isPlaying ?: false
        val artworkUrl = currentTrack?.artworkUrl

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, title, artist, isPlaying, artworkUrl)
        }
    }

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.example.vybe.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.example.vybe.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.example.vybe.ACTION_WIDGET_PREV"

        fun updateAllWidgets(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            artworkUrl: String? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, VybeWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id, title, artist, isPlaying, artworkUrl)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String,
            artist: String,
            isPlaying: Boolean,
            artworkUrl: String?
        ) {
            val views = RemoteViews(context.packageName, R.layout.vybe_app_widget)
            views.setTextViewText(R.id.widget_track_title, title)
            views.setTextViewText(R.id.widget_artist_name, artist)

            // Play / Pause Icon
            val playIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            views.setImageViewResource(R.id.widget_btn_play_pause, playIcon)

            // Play/Pause Action PendingIntent
            val playPauseIntent = Intent(context, VybeWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PLAY_PAUSE
            }
            val playPausePending = PendingIntent.getBroadcast(
                context, 101, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePending)

            // Previous Action PendingIntent
            val prevIntent = Intent(context, VybeWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PREV
            }
            val prevPending = PendingIntent.getBroadcast(
                context, 102, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending)

            // Next Action PendingIntent
            val nextIntent = Intent(context, VybeWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_NEXT
            }
            val nextPending = PendingIntent.getBroadcast(
                context, 103, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)

            // Open Full App Intent on clicking widget body, art, or title
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 100, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_info_container, appPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_art_container, appPendingIntent)

            // Update widget immediately with text & icons
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Load artwork asynchronously via Coil with smooth rounded corners
            if (!artworkUrl.isNullOrBlank()) {
                val imageLoader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .allowHardware(false) // Required for RemoteViews process transfer
                    .target(
                        onSuccess = { result ->
                            val bitmap = (result as? BitmapDrawable)?.bitmap
                            if (bitmap != null) {
                                val rounded = getRoundedCornerBitmap(bitmap, 36f)
                                views.setImageViewBitmap(R.id.widget_album_art, rounded)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        },
                        onError = {
                            views.setImageViewResource(R.id.widget_album_art, R.drawable.app_logo)
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    )
                    .build()
                imageLoader.enqueue(request)
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.app_logo)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun getRoundedCornerBitmap(src: Bitmap, cornerRadiusPx: Float): Bitmap {
            return try {
                val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rect = Rect(0, 0, src.width, src.height)
                val rectF = RectF(rect)
                canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(src, rect, rect, paint)
                output
            } catch (e: Exception) {
                src
            }
        }
    }
}
