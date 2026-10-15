package com.example.vybe.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import coil.Coil
import coil.request.ImageRequest
import com.example.vybe.MainActivity
import com.example.vybe.R
import com.example.vybe.VybeApplication
import com.example.vybe.core.model.LrcDocument
import com.example.vybe.core.model.PlaybackState
import com.example.vybe.core.model.RepeatMode
import com.example.vybe.core.model.Track
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class VybeMediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var notificationCallback: MediaNotification.Provider.Callback? = null
    private var lastMediaNotification: MediaNotification? = null

    private val serviceExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + serviceExceptionHandler)

    private var currentArtworkUrl: String? = null
    private var currentBitmap: Bitmap? = null
    private var currentAccentColor: Int = 0xFF00E5FF.toInt()

    // Real-time synced lyrics & dynamic notification tracker
    private var currentLyricsDoc: LrcDocument? = null
    private var lastLyricsTrackId: String? = null
    private var lyricsTickerJob: Job? = null
    private var lastRenderedSubtext: String? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        val app = application as VybeApplication
        val player = app.playbackManager.exoPlayer

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        createNotificationChannel()

        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_PREPARE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_SET_SHUFFLE_MODE)
                    .add(Player.COMMAND_SET_REPEAT_MODE)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_PREPARE,
                    Player.COMMAND_STOP,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_SET_SHUFFLE_MODE,
                    Player.COMMAND_SET_REPEAT_MODE,
                    Player.COMMAND_SEEK_BACK,
                    Player.COMMAND_SEEK_FORWARD -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun seekTo(positionMs: Long) {
                app.playbackManager.seekTo(positionMs)
            }

            override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
                app.playbackManager.seekTo(positionMs)
            }

            override fun seekToNext() {
                app.playbackManager.skipToNext()
            }

            override fun seekToNextMediaItem() {
                app.playbackManager.skipToNext()
            }

            override fun seekToPrevious() {
                app.playbackManager.skipToPrevious()
            }

            override fun seekToPreviousMediaItem() {
                app.playbackManager.skipToPrevious()
            }

            override fun play() {
                app.playbackManager.resume()
                startLyricsTicker()
            }

            override fun pause() {
                app.playbackManager.pause()
                stopLyricsTicker()
            }

            override fun stop() {
                app.playbackManager.pause()
                stopLyricsTicker()
            }

            override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
                app.playbackManager.toggleShuffle()
                updateCustomLayout()
            }

            override fun setRepeatMode(repeatMode: Int) {
                app.playbackManager.toggleRepeatMode()
                updateCustomLayout()
            }

            override fun seekBack() {
                app.playbackManager.seekRelative(-10000L)
            }

            override fun seekForward() {
                app.playbackManager.seekRelative(10000L)
            }
        }

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_DOWNLOAD, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_TOGGLE_REPEAT, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_CLOSE_SERVICE, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_SEEK_BACK_10, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_SEEK_FORWARD_10, Bundle.EMPTY))
                    .build()

                val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_PREPARE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_SET_SHUFFLE_MODE)
                    .add(Player.COMMAND_SET_REPEAT_MODE)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                val appInstance = application as? VybeApplication
                when (customCommand.customAction) {
                    ACTION_TOGGLE_LIKE -> {
                        appInstance?.playbackManager?.toggleFavoriteCurrentTrack()
                    }
                    ACTION_DOWNLOAD -> {
                        appInstance?.playbackManager?.downloadCurrentTrack()
                    }
                    ACTION_TOGGLE_SHUFFLE -> {
                        appInstance?.playbackManager?.toggleShuffle()
                        updateCustomLayout()
                    }
                    ACTION_TOGGLE_REPEAT -> {
                        appInstance?.playbackManager?.toggleRepeatMode()
                        updateCustomLayout()
                    }
                    ACTION_CLOSE_SERVICE -> {
                        stopPlaybackAndService()
                    }
                    ACTION_SEEK_BACK_10 -> {
                        appInstance?.playbackManager?.seekRelative(-10000L)
                    }
                    ACTION_SEEK_FORWARD_10 -> {
                        appInstance?.playbackManager?.seekRelative(10000L)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: Int
            ): Int {
                val appInstance = application as? VybeApplication
                when (playerCommand) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                        appInstance?.playbackManager?.skipToNext()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                        appInstance?.playbackManager?.skipToPrevious()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_PLAY_PAUSE -> {
                        appInstance?.playbackManager?.togglePlayPause()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_STOP -> {
                        appInstance?.playbackManager?.pause()
                        return SessionResult.RESULT_SUCCESS
                    }
                }
                return super.onPlayerCommandRequest(session, controller, playerCommand)
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .build()
        mediaSession?.let { addSession(it) }

        val defaultProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(PLAYBACK_CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .setNotificationId(PLAYBACK_NOTIFICATION_ID)
            .build()

        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                notificationCallback = onNotificationChangedCallback
                val mediaNotification = defaultProvider.createNotification(
                    mediaSession, customLayout, actionFactory, onNotificationChangedCallback
                )
                val appInstance = application as? VybeApplication
                val state = appInstance?.playbackManager?.playbackState?.value
                val isPlaying = state?.isPlaying == true
                val currentTrack = state?.currentTrack

                if (currentTrack != null) {
                    if (currentTrack.artworkUrl.isNotBlank()) {
                        loadArtworkBitmapAndColor(currentTrack.artworkUrl)
                    }
                    fetchLyricsForNotification(currentTrack)
                }

                val subText = computeDynamicSubText(state)
                lastRenderedSubtext = subText

                // Build PendingIntents for interactive notification shade buttons
                val likeIntent = Intent(this@VybeMediaService, VybeMediaService::class.java).apply { action = ACTION_TOGGLE_LIKE }
                val likePendingIntent = PendingIntent.getService(this@VybeMediaService, 101, likeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val prevIntent = Intent(this@VybeMediaService, VybeMediaService::class.java).apply { action = ACTION_PREV }
                val prevPendingIntent = PendingIntent.getService(this@VybeMediaService, 102, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val playPauseIntent = Intent(this@VybeMediaService, VybeMediaService::class.java).apply { action = ACTION_PLAY_PAUSE }
                val playPausePendingIntent = PendingIntent.getService(this@VybeMediaService, 103, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val nextIntent = Intent(this@VybeMediaService, VybeMediaService::class.java).apply { action = ACTION_NEXT }
                val nextPendingIntent = PendingIntent.getService(this@VybeMediaService, 104, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val downloadIntent = Intent(this@VybeMediaService, VybeMediaService::class.java).apply { action = ACTION_DOWNLOAD }
                val downloadPendingIntent = PendingIntent.getService(this@VybeMediaService, 105, downloadIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val closeIntent = Intent(this@VybeMediaService, VybeMediaService::class.java).apply { action = ACTION_CLOSE_SERVICE }
                val closePendingIntent = PendingIntent.getService(this@VybeMediaService, 106, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val isLiked = currentTrack?.isLiked == true
                val isDownloaded = currentTrack?.isDownloaded == true

                val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(1, 2, 3) // Compact view shows: Prev, Play/Pause, Next
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(closePendingIntent)

                val customNotification = NotificationCompat.Builder(this@VybeMediaService, mediaNotification.notification)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setStyle(mediaStyle)
                    .setColor(currentAccentColor)
                    .setColorized(true)
                    .setSmallIcon(R.drawable.app_logo)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setOngoing(isPlaying)
                    .apply {
                        currentBitmap?.let { setLargeIcon(it) }
                        if (currentTrack != null) {
                            setContentTitle(currentTrack.title)
                            setContentText(currentTrack.artistName)
                        }
                        setSubText(subText)

                        // Action 0: Like / Unlike Toggle
                        addAction(
                            if (isLiked) R.drawable.ic_notif_favorite else R.drawable.ic_notif_favorite_border,
                            if (isLiked) "Unlike" else "Like",
                            likePendingIntent
                        )
                        // Action 1: Previous
                        addAction(
                            R.drawable.ic_widget_skip_previous,
                            "Previous",
                            prevPendingIntent
                        )
                        // Action 2: Play/Pause Toggle
                        addAction(
                            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                            if (isPlaying) "Pause" else "Play",
                            playPausePendingIntent
                        )
                        // Action 3: Next
                        addAction(
                            R.drawable.ic_widget_skip_next,
                            "Next",
                            nextPendingIntent
                        )
                        // Action 4: Download / Downloaded Toggle
                        addAction(
                            if (isDownloaded) R.drawable.ic_notif_download_done else R.drawable.ic_notif_download,
                            if (isDownloaded) "Downloaded" else "Download",
                            downloadPendingIntent
                        )
                    }
                    .build()

                val finalNotif = MediaNotification(mediaNotification.notificationId, customNotification)
                lastMediaNotification = finalNotif

                // Keep MediaSession custom layout in sync with current state
                updateCustomLayout()

                return finalNotif
            }

            override fun handleCustomCommand(
                mediaSession: MediaSession,
                action: String,
                extras: Bundle
            ): Boolean {
                return defaultProvider.handleCustomCommand(mediaSession, action, extras)
            }
        })

        // Start initial foreground state
        startInitialForeground()

        // Start live lyrics ticker if already playing
        if (player.isPlaying) {
            startLyricsTicker()
        }
    }

    private fun startInitialForeground() {
        val session = mediaSession ?: return
        try {
            val openAppIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val initialNotif = NotificationCompat.Builder(this, PLAYBACK_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_logo)
                .setContentIntent(openAppIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(MediaStyleNotificationHelper.MediaStyle(session))
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    PLAYBACK_NOTIFICATION_ID,
                    initialNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(PLAYBACK_NOTIFICATION_ID, initialNotif)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? VybeApplication
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                app?.playbackManager?.togglePlayPause()
                if (app?.playbackManager?.playbackState?.value?.isPlaying == true) {
                    startLyricsTicker()
                } else {
                    stopLyricsTicker()
                }
            }
            ACTION_NEXT -> app?.playbackManager?.skipToNext()
            ACTION_PREV -> app?.playbackManager?.skipToPrevious()
            ACTION_TOGGLE_LIKE -> app?.playbackManager?.toggleFavoriteCurrentTrack()
            ACTION_DOWNLOAD -> app?.playbackManager?.downloadCurrentTrack()
            ACTION_TOGGLE_SHUFFLE -> {
                app?.playbackManager?.toggleShuffle()
                updateCustomLayout()
            }
            ACTION_TOGGLE_REPEAT -> {
                app?.playbackManager?.toggleRepeatMode()
                updateCustomLayout()
            }
            ACTION_CLOSE_SERVICE -> stopPlaybackAndService()
            ACTION_SEEK_BACK_10 -> app?.playbackManager?.seekRelative(-10000L)
            ACTION_SEEK_FORWARD_10 -> app?.playbackManager?.seekRelative(10000L)
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    fun stopPlaybackAndService() {
        val app = application as? VybeApplication
        app?.playbackManager?.pause()
        stopLyricsTicker()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(PLAYBACK_NOTIFICATION_ID)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val app = application as? VybeApplication
        val isPlaying = app?.playbackManager?.playbackState?.value?.isPlaying == true
        if (isPlaying) {
            updateNotificationNow()
        } else {
            stopPlaybackAndService()
        }
    }

    /**
     * Updates Media3 CustomLayout buttons for Android 13+ quick settings media card
     */
    fun updateCustomLayout() {
        val session = mediaSession ?: return
        val app = application as? VybeApplication ?: return
        val state = app.playbackManager.playbackState.value
        val track = state.currentTrack

        val likeBtn = CommandButton.Builder()
            .setDisplayName(if (track?.isLiked == true) "Liked" else "Like")
            .setIconResId(if (track?.isLiked == true) R.drawable.ic_notif_favorite else R.drawable.ic_notif_favorite_border)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
            .setEnabled(track != null)
            .build()

        val downloadBtn = CommandButton.Builder()
            .setDisplayName(if (track?.isDownloaded == true) "Downloaded" else "Download")
            .setIconResId(if (track?.isDownloaded == true) R.drawable.ic_notif_download_done else R.drawable.ic_notif_download)
            .setSessionCommand(SessionCommand(ACTION_DOWNLOAD, Bundle.EMPTY))
            .setEnabled(track != null && !track.isDownloaded)
            .build()

        val shuffleBtn = CommandButton.Builder()
            .setDisplayName(if (state.shuffleEnabled) "Shuffle On" else "Shuffle Off")
            .setIconResId(R.drawable.ic_notif_shuffle)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
            .build()

        val repeatIcon = when (state.repeatMode) {
            RepeatMode.ONE -> R.drawable.ic_notif_repeat_one
            else -> R.drawable.ic_notif_repeat
        }
        val repeatBtn = CommandButton.Builder()
            .setDisplayName(when (state.repeatMode) {
                RepeatMode.ONE -> "Repeat One"
                RepeatMode.ALL -> "Repeat All"
                RepeatMode.OFF -> "Repeat Off"
            })
            .setIconResId(repeatIcon)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_REPEAT, Bundle.EMPTY))
            .build()

        val closeBtn = CommandButton.Builder()
            .setDisplayName("Stop")
            .setIconResId(R.drawable.ic_notif_close)
            .setSessionCommand(SessionCommand(ACTION_CLOSE_SERVICE, Bundle.EMPTY))
            .build()

        session.setCustomLayout(ImmutableList.of(likeBtn, downloadBtn, shuffleBtn, repeatBtn, closeBtn))
    }

    private fun fetchLyricsForNotification(track: Track) {
        if (track.id == lastLyricsTrackId && currentLyricsDoc != null) return
        lastLyricsTrackId = track.id
        serviceScope.launch(Dispatchers.IO) {
            try {
                val doc = LyricsManager.getLyricsForTrack(track)
                currentLyricsDoc = doc
            } catch (e: Exception) {
                currentLyricsDoc = null
            }
        }
    }

    private fun startLyricsTicker() {
        lyricsTickerJob?.cancel()
        lyricsTickerJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(1200L)
                val app = application as? VybeApplication ?: continue
                val state = app.playbackManager.playbackState.value
                if (!state.isPlaying) continue

                val newSubText = computeDynamicSubText(state)
                if (newSubText != lastRenderedSubtext) {
                    lastRenderedSubtext = newSubText
                    updateNotificationSubtextOnly(newSubText)
                }
            }
        }
    }

    private fun stopLyricsTicker() {
        lyricsTickerJob?.cancel()
        lyricsTickerJob = null
    }

    private fun computeDynamicSubText(state: PlaybackState?): String {
        if (state == null) return "VYBE Music"
        val remainingSec = state.sleepTimerRemainingSec
        val track = state.currentTrack
        val isPlaying = state.isPlaying

        // 1. Check if synced lyrics line is active right now
        val currentPos = state.currentPositionMs
        val activeLine = currentLyricsDoc?.lines?.lastOrNull {
            it.timeMs <= currentPos && (currentPos - it.timeMs) <= 8500L
        }
        val lyricSnippet = activeLine?.text?.takeIf { it.isNotBlank() }?.let { "🎤 \"$it\"" }

        // 2. Check next queue preview
        val nextTrack = state.queue.getOrNull(state.queueIndex + 1)
        val nextSnippet = nextTrack?.let { "⏭️ Next: ${it.title}" }

        // 3. Audio profile badge
        val qualityBadge = if (state.isAuraSpatialAudioEnabled) {
            "✨ 320kbps HD • Aura 3D ✨"
        } else {
            "✨ VYBE Studio HD • 320kbps ✨"
        }

        return when {
            remainingSec > 0L -> {
                val mins = (remainingSec + 59) / 60
                if (lyricSnippet != null) "⏳ ${mins}m • $lyricSnippet" else "⏳ Sleep timer: $mins min left"
            }
            lyricSnippet != null -> lyricSnippet
            nextSnippet != null -> nextSnippet
            isPlaying -> qualityBadge
            else -> "VYBE Music • Paused"
        }
    }

    private fun updateNotificationSubtextOnly(newSubText: String) {
        val lastNotif = lastMediaNotification ?: return
        val session = mediaSession ?: return
        try {
            val updated = NotificationCompat.Builder(this, lastNotif.notification)
                .setSubText(newSubText)
                .apply {
                    setStyle(MediaStyleNotificationHelper.MediaStyle(session))
                }
                .build()
            val newMediaNotif = MediaNotification(lastNotif.notificationId, updated)
            lastMediaNotification = newMediaNotif
            notificationCallback?.onNotificationChanged(newMediaNotif)
        } catch (_: Exception) {}
    }

    private fun extractSubtleDynamicColor(bitmap: Bitmap): Int {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            var bestColor = 0xFF00E5FF.toInt()
            var maxScore = -1f

            val hsv = FloatArray(3)
            val stepX = (width / 6).coerceAtLeast(1)
            val stepY = (height / 6).coerceAtLeast(1)

            for (x in stepX until width - stepX step stepX) {
                for (y in stepY until height - stepY step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    Color.colorToHSV(pixel, hsv)
                    val sat = hsv[1]
                    val lum = hsv[2]
                    if (sat > 0.35f && lum > 0.25f && lum < 0.90f) {
                        val score = sat * 2.2f + (1f - abs(lum - 0.55f))
                        if (score > maxScore) {
                            maxScore = score
                            bestColor = pixel
                        }
                    }
                }
            }
            bestColor
        } catch (e: Exception) {
            0xFF00E5FF.toInt()
        }
    }

    private fun loadArtworkBitmapAndColor(artworkUrl: String) {
        if (artworkUrl.isBlank() || (artworkUrl == currentArtworkUrl && currentBitmap != null)) return
        currentArtworkUrl = artworkUrl
        serviceScope.launch(Dispatchers.IO) {
            try {
                val loader = Coil.imageLoader(this@VybeMediaService)
                val request = ImageRequest.Builder(this@VybeMediaService)
                    .data(artworkUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    val bmp = drawable.bitmap
                    currentBitmap = bmp
                    currentAccentColor = extractSubtleDynamicColor(bmp)
                    withContext(Dispatchers.Main) {
                        updateNotificationNow()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateNotificationNow() {
        val lastNotif = lastMediaNotification ?: return
        try {
            val appInstance = application as? VybeApplication
            val state = appInstance?.playbackManager?.playbackState?.value
            val isPlaying = state?.isPlaying == true
            val currentTrack = state?.currentTrack
            val session = mediaSession ?: return

            if (currentTrack != null) {
                if (currentTrack.artworkUrl.isNotBlank() && currentTrack.artworkUrl != currentArtworkUrl) {
                    loadArtworkBitmapAndColor(currentTrack.artworkUrl)
                }
                fetchLyricsForNotification(currentTrack)
            }

            val subText = computeDynamicSubText(state)
            lastRenderedSubtext = subText

            val isLiked = currentTrack?.isLiked == true
            val isDownloaded = currentTrack?.isDownloaded == true

            val likeIntent = Intent(this, VybeMediaService::class.java).apply { action = ACTION_TOGGLE_LIKE }
            val likePendingIntent = PendingIntent.getService(this, 101, likeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val prevIntent = Intent(this, VybeMediaService::class.java).apply { action = ACTION_PREV }
            val prevPendingIntent = PendingIntent.getService(this, 102, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val playPauseIntent = Intent(this, VybeMediaService::class.java).apply { action = ACTION_PLAY_PAUSE }
            val playPausePendingIntent = PendingIntent.getService(this, 103, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val nextIntent = Intent(this, VybeMediaService::class.java).apply { action = ACTION_NEXT }
            val nextPendingIntent = PendingIntent.getService(this, 104, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val downloadIntent = Intent(this, VybeMediaService::class.java).apply { action = ACTION_DOWNLOAD }
            val downloadPendingIntent = PendingIntent.getService(this, 105, downloadIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val closeIntent = Intent(this, VybeMediaService::class.java).apply { action = ACTION_CLOSE_SERVICE }
            val closePendingIntent = PendingIntent.getService(this, 106, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val mediaStyle = MediaStyleNotificationHelper.MediaStyle(session)
                .setShowActionsInCompactView(1, 2, 3)
                .setShowCancelButton(true)
                .setCancelButtonIntent(closePendingIntent)

            val updated = NotificationCompat.Builder(this, lastNotif.notification)
                .setColor(currentAccentColor)
                .setColorized(true)
                .setSmallIcon(R.drawable.app_logo)
                .setStyle(mediaStyle)
                .setOngoing(isPlaying)
                .apply {
                    currentBitmap?.let { setLargeIcon(it) }
                    if (currentTrack != null) {
                        setContentTitle(currentTrack.title)
                        setContentText(currentTrack.artistName)
                    }
                    setSubText(subText)

                    // Clear previous and re-add fresh action states
                    clearActions()
                    addAction(
                        if (isLiked) R.drawable.ic_notif_favorite else R.drawable.ic_notif_favorite_border,
                        if (isLiked) "Unlike" else "Like",
                        likePendingIntent
                    )
                    addAction(
                        R.drawable.ic_widget_skip_previous,
                        "Previous",
                        prevPendingIntent
                    )
                    addAction(
                        if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                        if (isPlaying) "Pause" else "Play",
                        playPausePendingIntent
                    )
                    addAction(
                        R.drawable.ic_widget_skip_next,
                        "Next",
                        nextPendingIntent
                    )
                    addAction(
                        if (isDownloaded) R.drawable.ic_notif_download_done else R.drawable.ic_notif_download,
                        if (isDownloaded) "Downloaded" else "Download",
                        downloadPendingIntent
                    )
                }
                .build()

            val newMediaNotif = MediaNotification(lastNotif.notificationId, updated)
            lastMediaNotification = newMediaNotif
            notificationCallback?.onNotificationChanged(newMediaNotif)

            updateCustomLayout()

            if (isPlaying && lyricsTickerJob == null) {
                startLyricsTicker()
            } else if (!isPlaying) {
                stopLyricsTicker()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshSleepTimerNotification() {
        val session = mediaSession ?: return
        val appInstance = application as? VybeApplication ?: return
        val remainingSec = appInstance.playbackManager.playbackState.value.sleepTimerRemainingSec
        val currentTrack = appInstance.playbackManager.playbackState.value.currentTrack
        val mins = (remainingSec + 59) / 60
        val subText = if (remainingSec > 0L) "⏳ Sleep timer: $mins min left" else null

        if (currentTrack != null) {
            try {
                val updatedMetadata = session.player.mediaMetadata.buildUpon()
                    .setSubtitle(subText ?: currentTrack.albumName)
                    .setDescription(subText)
                    .build()
                session.player.playlistMetadata = updatedMetadata
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        updateNotificationNow()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        instance = null
        stopLyricsTicker()
        serviceScope.cancel()
        currentBitmap = null
        try {
            mediaSession?.let {
                removeSession(it)
                it.release()
            }
            mediaSession = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "VYBE Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing VYBE song controls on Lock Screen and Notification Shade"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "vybe_media_playback"
        const val PLAYBACK_NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.vybe.playback.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.vybe.playback.ACTION_NEXT"
        const val ACTION_PREV = "com.example.vybe.playback.ACTION_PREV"
        const val ACTION_TOGGLE_LIKE = "com.example.vybe.playback.ACTION_TOGGLE_LIKE"
        const val ACTION_DOWNLOAD = "com.example.vybe.playback.ACTION_DOWNLOAD"
        const val ACTION_TOGGLE_SHUFFLE = "com.example.vybe.playback.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.example.vybe.playback.ACTION_TOGGLE_REPEAT"
        const val ACTION_CLOSE_SERVICE = "com.example.vybe.playback.ACTION_CLOSE_SERVICE"
        const val ACTION_SEEK_BACK_10 = "com.example.vybe.playback.ACTION_SEEK_BACK_10"
        const val ACTION_SEEK_FORWARD_10 = "com.example.vybe.playback.ACTION_SEEK_FORWARD_10"

        var instance: VybeMediaService? = null
            private set
    }
}
