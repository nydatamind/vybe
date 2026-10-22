package com.example.vybe.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.vybe.VybeApplication
import com.example.vybe.core.datastore.PreferencesManager
import com.example.vybe.core.model.PlaybackState
import com.example.vybe.core.model.RepeatMode
import com.example.vybe.core.model.Track
import com.example.vybe.data.provider.JioSaavnAudioProvider
import com.example.vybe.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AudioCacheHolder {
    private var simpleCache: androidx.media3.datasource.cache.SimpleCache? = null
    const val MAX_AUDIO_CACHE_BYTES = 8 * 1024 * 1024L // 8 MB strict hard limit

    @Synchronized
    fun getCache(context: Context): androidx.media3.datasource.cache.SimpleCache? {
        if (simpleCache == null) {
            try {
                val cacheDir = java.io.File(context.cacheDir, "audio_cache")
                val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(MAX_AUDIO_CACHE_BYTES)
                val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context)
                simpleCache = androidx.media3.datasource.cache.SimpleCache(cacheDir, evictor, databaseProvider)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val cacheDir = java.io.File(context.cacheDir, "audio_cache")
                    cacheDir.deleteRecursively()
                    val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(MAX_AUDIO_CACHE_BYTES)
                    val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context)
                    simpleCache = androidx.media3.datasource.cache.SimpleCache(cacheDir, evictor, databaseProvider)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    simpleCache = null
                }
            }
        }
        return simpleCache
    }

    @Synchronized
    fun getCacheSizeBytes(context: Context): Long {
        val cache = simpleCache
        if (cache != null) {
            try {
                return cache.cacheSpace
            } catch (_: Exception) {}
        }
        val cacheDir = java.io.File(context.cacheDir, "audio_cache")
        return if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L
    }

    /**
     * Safely purges temporary cache for specific track keys (e.g. skipped or played tracks).
     * Guaranteed to never affect any key in [preserveKeys].
     */
    @Synchronized
    fun removeTrackCache(keysToRemove: Collection<String>, preserveKeys: Set<String> = emptySet()) {
        val cache = simpleCache ?: return
        try {
            for (key in keysToRemove) {
                if (key.isNotBlank() && !preserveKeys.contains(key)) {
                    cache.removeResource(key)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Performs automatic LRU pruning if audio cache exceeds [targetBytes].
     * Preserves [preserveKeys] so current playback buffer is NEVER interrupted.
     */
    @Synchronized
    fun pruneOldestAudio(targetBytes: Long = 6 * 1024 * 1024L, preserveKeys: Set<String> = emptySet()) {
        val cache = simpleCache ?: return
        try {
            var currentSpace = cache.cacheSpace
            if (currentSpace <= targetBytes) return

            // Collect all non-preserved keys and their oldest touch timestamps
            val keyTimestamps = mutableListOf<Pair<String, Long>>()
            for (key in cache.keys) {
                if (preserveKeys.contains(key)) continue
                val spans = cache.getCachedSpans(key)
                val oldestTouch = spans.minOfOrNull { it.lastTouchTimestamp } ?: 0L
                keyTimestamps.add(key to oldestTouch)
            }

            // Sort oldest first for LRU eviction
            keyTimestamps.sortBy { it.second }

            for ((key, _) in keyTimestamps) {
                if (currentSpace <= targetBytes) break
                val spans = cache.getCachedSpans(key)
                val keySize = spans.sumOf { it.length }
                cache.removeResource(key)
                currentSpace -= keySize
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class PlaybackManager(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val preferencesManager: PreferencesManager = (context.applicationContext as? VybeApplication)?.preferencesManager ?: PreferencesManager(context)
) {
    private val coroutineExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + coroutineExceptionHandler)

    // Ultra-low latency playback: Start playing after only 5ms buffer with 15s back-buffer for instant replay
    private val defaultLoadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            20000, // minBufferMs (20s continuous buffer)
            60000, // maxBufferMs (60s buffer ahead)
            5,     // bufferForPlaybackMs (instant zero-delay play <5ms)
            25     // bufferForPlaybackAfterRebufferMs
        )
        .setBackBuffer(15000, true) // 15s instant rewind buffer
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(8000)
        .setReadTimeoutMs(15000)

    private val cacheDataSourceFactory = AudioCacheHolder.getCache(context)?.let { cache ->
        androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private val dataSourceFactory = if (cacheDataSourceFactory != null) {
        androidx.media3.datasource.DefaultDataSource.Factory(context, cacheDataSourceFactory)
    } else {
        androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val wakeLock: PowerManager.WakeLock? = try {
        powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VYBE:AudioPlaybackWakeLock")?.apply {
            setReferenceCounted(false)
        }
    } catch (e: Exception) {
        null
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val wifiLock: WifiManager.WifiLock? = try {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiManager?.createWifiLock(mode, "VYBE:AudioPlaybackWifiLock")?.apply {
            setReferenceCounted(false)
        }
    } catch (e: Exception) {
        null
    }

    private fun acquireLocks() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours maximum timeout
            }
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory))
        .setLoadControl(defaultLoadControl)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true // Automatic audio focus handling
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var progressJob: Job? = null
    private var activePlayJob: Job? = null
    private var retryCount = 0
    private var lastFailedTrackId: String? = null
    private var originalQueue: List<Track> = emptyList()

    private var trackPlayStartTimeMs: Long = 0L
    private val skippedTrackIds = java.util.Collections.synchronizedSet(LinkedHashSet<String>())
    private val skippedTrackTitles = java.util.Collections.synchronizedSet(LinkedHashSet<String>())
    private val sessionPlayedIds = java.util.Collections.synchronizedSet(LinkedHashSet<String>())
    private val sessionPlayedTitles = java.util.Collections.synchronizedSet(LinkedHashSet<String>())
    private var recalculateQueueJob: Job? = null
    private val isExtendingQueue = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun recordSessionPlayed(track: Track?) {
        if (track == null) return
        if (track.id.isNotBlank()) {
            sessionPlayedIds.add(track.id)
            if (sessionPlayedIds.size > 200) {
                sessionPlayedIds.firstOrNull()?.let { sessionPlayedIds.remove(it) }
            }
        }
        val norm = com.example.vybe.data.repository.normalizeSongTitle(track.title)
        if (norm.isNotBlank()) {
            sessionPlayedTitles.add(norm)
            if (sessionPlayedTitles.size > 200) {
                sessionPlayedTitles.firstOrNull()?.let { sessionPlayedTitles.remove(it) }
            }
        }
    }

    val audioAnalysisManager = AudioAnalysisManager(context)
    private var currentTrackIntroSkipMs: Long = 0L
    private var currentTrackOutroSkipMs: Long = 0L
    private var isAutoSkipIntroOutroEnabled = true
    private var hasSkippedOutroForCurrentTrack = false

    private var isAudioDuckingEnabled = true
    private var duckingVolume = 0.0f
    private var isDucked = false
    private var duckingJob: Job? = null
    private var normalVolume = 1.0f

    val sleepTimerManager: SleepTimerManager = SleepTimerManager(
        coroutineScope = scope,
        onTimerFinished = { pause() },
        onFadeVolume = { factor -> if (!isDucked) exoPlayer.volume = factor }
    )

    private fun duckAudio(targetVolume: Float = 0.0f) {
        if (!isDucked) {
            isDucked = true
            _playbackState.update { it.copy(isAudioDucked = true) }
        }
        fadeVolumeTo(targetVolume, durationMs = 250L)
    }

    private fun restoreAudio() {
        if (isDucked) {
            isDucked = false
            _playbackState.update { it.copy(isAudioDucked = false) }
        }
        fadeVolumeTo(normalVolume, durationMs = 350L)
    }

    private fun fadeVolumeTo(target: Float, durationMs: Long) {
        duckingJob?.cancel()
        duckingJob = scope.launch {
            val start = exoPlayer.volume
            val steps = 15
            val stepDelay = (durationMs / steps).coerceAtLeast(10L)
            for (i in 1..steps) {
                val fraction = i / steps.toFloat()
                exoPlayer.volume = start + (target - start) * fraction
                delay(stepDelay)
            }
            exoPlayer.volume = target
        }
    }

    // Audio Focus handling
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var playbackDelayed = false
    private var resumeOnFocusGain = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                restoreAudio()
                if (playbackDelayed || resumeOnFocusGain) {
                    playbackDelayed = false
                    resumeOnFocusGain = false
                    resume()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Instagram Reels, Stories, TikTok, YouTube Shorts, WhatsApp audio, etc.
                // DO NOT pause, DO NOT seek, DO NOT restart, DO NOT change playback position!
                // Music continuously plays in the background at 0% (silent) volume!
                duckAudio(0.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent focus loss (e.g. user explicitly started another full music player)
                resumeOnFocusGain = false
                playbackDelayed = false
                pause()
                if (isDucked) {
                    isDucked = false
                    exoPlayer.volume = normalVolume
                    _playbackState.update { it.copy(isAudioDucked = false) }
                }
            }
        }
    }

    // Becoming Noisy (Headphones disconnected)
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
            updateAuraAudioState()
        }
    }

    val equalizerManager: EqualizerManager = EqualizerManager()
    private var isAuraEnabled = false

    private fun isHeadsetOrBluetoothConnected(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.any { device ->
                val type = device.type
                type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
        }
    }

    fun updateAuraAudioState() {
        val isConnected = isHeadsetOrBluetoothConnected()
        equalizerManager.setAuraSpatialAudio(isAuraEnabled, isConnected)
    }

    init {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        exoPlayer.setAudioAttributes(audioAttributes, false)

        setupPlayerListener()
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        }
        context.registerReceiver(becomingNoisyReceiver, filter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.registerAudioDeviceCallback(object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    updateAuraAudioState()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    updateAuraAudioState()
                }
            }, Handler(Looper.getMainLooper()))
        }

        // Observe Aura Spatial Audio toggle from settings
        scope.launch {
            preferencesManager.isAuraSpatialAudioEnabled.collect { enabled ->
                isAuraEnabled = enabled
                _playbackState.update { it.copy(isAuraSpatialAudioEnabled = enabled) }
                updateAuraAudioState()
            }
        }

        // Observe sleep timer state
        scope.launch {
            var lastMinute = -1L
            sleepTimerManager.remainingSeconds.collect { sec ->
                _playbackState.update { it.copy(sleepTimerRemainingSec = sec) }
                val currentMinute = if (sec > 0) (sec + 59) / 60 else 0L
                if (currentMinute != lastMinute) {
                    lastMinute = currentMinute
                    VybeMediaService.instance?.refreshSleepTimerNotification()
                }
            }
        }

        // Observe Auto Skip Intro/Outro preference
        scope.launch {
            preferencesManager.isAutoSkipIntroOutroEnabled.collect { enabled ->
                isAutoSkipIntroOutroEnabled = enabled
                _playbackState.update { it.copy(autoSkipIntroOutro = enabled) }
            }
        }

        // Observe Audio Ducking preference
        scope.launch {
            preferencesManager.isAudioDuckingEnabled.collect { enabled ->
                isAudioDuckingEnabled = enabled
                if (!enabled && isDucked) {
                    restoreAudio()
                }
            }
        }
    }

    fun toggleAuraSpatialAudio() {
        val next = !isAuraEnabled
        scope.launch {
            preferencesManager.setAuraSpatialAudioEnabled(next)
        }
    }

    fun toggleAutoSkipIntroOutro() {
        val next = !isAutoSkipIntroOutroEnabled
        scope.launch {
            preferencesManager.setAutoSkipIntroOutroEnabled(next)
        }
    }

    fun toggleAudioDucking() {
        val next = !isAudioDuckingEnabled
        scope.launch {
            preferencesManager.setAudioDuckingEnabled(next)
        }
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                val currentTrack = _playbackState.value.currentTrack
                if (currentTrack != null) {
                    com.example.vybe.widget.VybeWidgetProvider.updateAllWidgets(
                        context, currentTrack.title, currentTrack.artistName, isPlaying, currentTrack.artworkUrl
                    )
                }
                VybeMediaService.instance?.updateNotificationNow()
                if (isPlaying) {
                    acquireLocks()
                    startProgressTracker()
                } else {
                    releaseLocks()
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        _playbackState.update { it.copy(isBuffering = true) }
                    }
                    Player.STATE_READY -> {
                        _playbackState.update {
                            it.copy(
                                isBuffering = false,
                                durationMs = exoPlayer.duration.coerceAtLeast(0L),
                                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        _playbackState.update { it.copy(isBuffering = false) }
                        sleepTimerManager.onTrackEnded()
                        handleTrackEnded()
                    }
                    Player.STATE_IDLE -> {
                        _playbackState.update { it.copy(isBuffering = false) }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                val current = _playbackState.value.currentTrack
                if (current != null) {
                    val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    // Invalidate stale stream cache
                    if (current.id.isNotBlank()) {
                        JioSaavnAudioProvider.streamCache.remove(current.id)
                    }

                    if (lastFailedTrackId == current.id && retryCount >= 2) {
                        // Max retries reached for this specific track -> skip to next track
                        retryCount = 0
                        lastFailedTrackId = null
                        scope.launch(Dispatchers.Main) {
                            skipToNext()
                        }
                        return
                    }

                    lastFailedTrackId = current.id
                    retryCount++

                    // Dynamic live stream re-resolution on failure with backoff
                    activePlayJob?.cancel()
                    activePlayJob = scope.launch(Dispatchers.IO) {
                        try {
                            delay(400L * retryCount)
                            val liveResolved = musicRepository.resolveFullLengthTrack(current)
                            if (liveResolved != null && liveResolved.streamUrl.isNotBlank()) {
                                val resolvedTrack = current.copy(
                                    streamUrl = liveResolved.streamUrl,
                                    durationMs = liveResolved.durationMs
                                )
                                withContext(Dispatchers.Main) {
                                    _playbackState.update { it.copy(currentTrack = resolvedTrack, durationMs = resolvedTrack.durationMs) }
                                    playMediaItem(resolvedTrack, currentPos)
                                }
                                return@launch
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        withContext(Dispatchers.Main) {
                            skipToNext()
                        }
                    }
                    return
                }
                _playbackState.update { it.copy(isBuffering = false, isPlaying = false) }
            }
        })
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun recordFastSkipIfApplicable(track: Track?) {
        if (track == null) return
        val elapsedMs = System.currentTimeMillis() - trackPlayStartTimeMs
        val currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        val durationMs = exoPlayer.duration.coerceAtLeast(0L)
        // If skipped within 20s or played less than 20% of the song
        if (elapsedMs in 1L..20000L || (durationMs > 30000L && currentPosMs < durationMs * 0.20f)) {
            if (track.id.isNotBlank()) {
                skippedTrackIds.add(track.id)
                if (skippedTrackIds.size > 100) {
                    skippedTrackIds.firstOrNull()?.let { skippedTrackIds.remove(it) }
                }
            }
            val normTitle = com.example.vybe.data.repository.normalizeSongTitle(track.title)
            if (normTitle.isNotBlank()) {
                skippedTrackTitles.add(normTitle)
                if (skippedTrackTitles.size > 100) {
                    skippedTrackTitles.firstOrNull()?.let { skippedTrackTitles.remove(it) }
                }
            }
        }
    }

    /**
     * Dynamic Recalculation Engine:
     * Recalculates upcoming recommendations whenever:
     * 1. A song starts or finishes automatically (Autoplay),
     * 2. Auto outro-skip triggers,
     * 3. User manually taps the Next button.
     * Re-anchors upcoming queue around [anchorTrack] as the new recommendation context.
     * Executes asynchronously on Dispatchers.IO with zero playback interruption.
     */
    fun recalculateUpcomingQueue(anchorTrack: Track) {
        // Instant Zero-Lag Queue Priming: If queue has only 1 track remaining, immediately prime with compatible fallback tracks
        val stateNow = _playbackState.value
        val currentIdxNow = stateNow.queueIndex
        if (stateNow.queue.size - currentIdxNow <= 1) {
            val instantQueue = musicRepository.recommendationEngine.getInstantFallbackQueue(anchorTrack, limit = 6)
            if (instantQueue.isNotEmpty()) {
                val base = if (currentIdxNow in stateNow.queue.indices) stateNow.queue.take(currentIdxNow + 1) else listOf(anchorTrack)
                val primed = base + instantQueue
                originalQueue = primed
                _playbackState.update { it.copy(queue = primed) }
            }
        }

        recalculateQueueJob?.cancel()
        recalculateQueueJob = scope.launch(Dispatchers.IO) {
            try {
                val state = _playbackState.value
                val queue = state.queue
                val currentIndex = state.queueIndex
                val historyToKeep = if (currentIndex in queue.indices) {
                    queue.take(currentIndex + 1)
                } else {
                    listOf(anchorTrack)
                }

                val freshCandidates = musicRepository.getAutoplayQueue(
                    seedTrack = anchorTrack,
                    existingQueue = historyToKeep,
                    skippedTrackIds = skippedTrackIds.toSet(),
                    skippedTitles = skippedTrackTitles.toSet(),
                    sessionPlayedTrackIds = sessionPlayedIds.toSet(),
                    sessionPlayedTitles = sessionPlayedTitles.toSet(),
                    limit = 20
                )

                if (freshCandidates.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val currentState = _playbackState.value
                        // Ensure we are still playing or parked on the anchor track
                        if (currentState.currentTrack?.id == anchorTrack.id ||
                            currentState.currentTrack?.title.equals(anchorTrack.title, ignoreCase = true)) {
                            val existingIds = historyToKeep.map { it.id }.filter { it.isNotBlank() }.toSet()
                            val existingTitles = historyToKeep.map { com.example.vybe.data.repository.normalizeSongTitle(it.title) }.toSet()
                            val distinctFresh = freshCandidates.filter {
                                !existingIds.contains(it.id) && !existingTitles.contains(com.example.vybe.data.repository.normalizeSongTitle(it.title))
                            }
                            if (distinctFresh.isNotEmpty()) {
                                val updatedQueue = historyToKeep + distinctFresh
                                originalQueue = if (originalQueue.isNotEmpty()) historyToKeep + distinctFresh else updatedQueue
                                _playbackState.update { it.copy(queue = updatedQueue) }
                            }
                        }
                    }

                    // Pre-resolve stream for the immediate next song in queue for 0ms transition
                    val nextPos = _playbackState.value.queueIndex + 1
                    val nextUpcoming = _playbackState.value.queue.getOrNull(nextPos)
                    if (nextUpcoming != null && nextUpcoming.streamUrl.isBlank()) {
                        try {
                            musicRepository.resolveFullLengthTrack(nextUpcoming)
                        } catch (e: Exception) {
                            // Non-blocking pre-fetch
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun checkAndExtendAutoplayQueue() {
        val state = _playbackState.value
        val queue = state.queue
        val currentIndex = state.queueIndex
        val currentTrack = state.currentTrack ?: queue.getOrNull(currentIndex) ?: return

        // Proactively recalculate upcoming recommendations when 4 or fewer songs remain
        if (queue.size - currentIndex <= 4) {
            recalculateUpcomingQueue(currentTrack)
        }
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        activePlayJob?.cancel()
        retryCount = 0
        lastFailedTrackId = null

        val targetQueue = if (newQueue.isEmpty()) listOf(track) else newQueue
        originalQueue = targetQueue
        val activeQueue = if (_playbackState.value.shuffleEnabled) {
            listOf(track) + (targetQueue - track).shuffled()
        } else {
            targetQueue
        }
        val targetIndex = activeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        val introSkip = if (isAutoSkipIntroOutroEnabled && (track.durationMs == 0L || track.durationMs > AudioAnalysisManager.MIN_DURATION_FOR_SKIP_MS)) AudioAnalysisManager.FIXED_INTRO_MS else 0L
        val outroSkip = if (isAutoSkipIntroOutroEnabled && (track.durationMs == 0L || track.durationMs > AudioAnalysisManager.MIN_DURATION_FOR_SKIP_MS)) AudioAnalysisManager.FIXED_OUTRO_MS else 0L
        currentTrackIntroSkipMs = introSkip
        currentTrackOutroSkipMs = outroSkip
        hasSkippedOutroForCurrentTrack = false

        val initialPosition = introSkip

        // Check if stream URL is already pre-cached in memory for instant zero-lag playback
        val cachedStream = if (track.id.isNotBlank()) JioSaavnAudioProvider.streamCache[track.id] else null
        val directUrl = if (track.streamUrl.isNotBlank()) track.streamUrl else (cachedStream ?: "")
        val initialPlayableTrack = if (directUrl.isNotBlank() && track.streamUrl.isBlank()) {
            track.copy(streamUrl = directUrl)
        } else track

        // Discard cache of previous track if manually skipped
        val previousTrack = _playbackState.value.currentTrack
        if (previousTrack != null && previousTrack.id != track.id) {
            recordFastSkipIfApplicable(previousTrack)
            triggerBackgroundCachePrune(activeTrack = initialPlayableTrack, skippedTrack = previousTrack)
        }

        // 1. Optimistic UI update: instantly update track, duration, queue, and playing state (<5ms)
        recordSessionPlayed(initialPlayableTrack)
        _playbackState.update {
            it.copy(
                currentTrack = initialPlayableTrack,
                queue = activeQueue,
                queueIndex = targetIndex,
                currentPositionMs = initialPosition,
                durationMs = track.durationMs,
                isPlaying = true,
                isBuffering = directUrl.isBlank()
            )
        }

        val hasDirectUrl = directUrl.isNotBlank() && 
            directUrl.startsWith("http") &&
            !directUrl.contains("itunes.apple.com") &&
            !directUrl.endsWith(".jpg") &&
            !directUrl.endsWith(".png") &&
            !directUrl.endsWith(".webp")

        // 2. If track has direct or pre-cached streaming URL, start playback INSTANTLY (<50ms)
        if (hasDirectUrl) {
            playMediaItem(initialPlayableTrack)
        }

        // 3. Resolve stream in background if needed & populate dynamic Autoplay Queue
        activePlayJob = scope.launch(Dispatchers.IO) {
            var playableTrack = initialPlayableTrack
            if (!hasDirectUrl) {
                val resolvedFull = musicRepository.resolveFullLengthTrack(playableTrack)
                if (resolvedFull != null && resolvedFull.streamUrl.isNotBlank()) {
                    playableTrack = playableTrack.copy(
                        streamUrl = resolvedFull.streamUrl,
                        durationMs = resolvedFull.durationMs
                    )
                    withContext(Dispatchers.Main) {
                        _playbackState.update { state ->
                            if (state.currentTrack?.id == track.id) {
                                state.copy(currentTrack = playableTrack, durationMs = playableTrack.durationMs)
                            } else state
                        }
                        playMediaItem(playableTrack)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _playbackState.update { it.copy(isBuffering = false) }
                    }
                }
            }

            // Next Song Recommendation + Autoplay Engine: Generate dynamic queue
            if (targetQueue.size <= 1) {
                recalculateUpcomingQueue(playableTrack)
            } else if (targetQueue.size <= 4) {
                checkAndExtendAutoplayQueue()
            }

            // Proactively resolve next track in queue so next track starts instantly with 0ms delay
            val nextTrack = _playbackState.value.queue.getOrNull(targetIndex + 1) ?: activeQueue.getOrNull(targetIndex + 1)
            if (nextTrack != null && nextTrack.streamUrl.isBlank()) {
                try {
                    musicRepository.resolveFullLengthTrack(nextTrack)
                } catch (e: Exception) {
                    // Non-blocking pre-fetch
                }
            }

            musicRepository.recordPlayback(playableTrack)
            val liked = musicRepository.isTrackLiked(playableTrack.id)
            if (liked != playableTrack.isLiked) {
                withContext(Dispatchers.Main) {
                    _playbackState.update { state ->
                        state.copy(currentTrack = state.currentTrack?.copy(isLiked = liked))
                    }
                }
            }
        }
    }

    private fun preAnalyzeNextTrack() {
        val state = _playbackState.value
        val nextIndex = state.queueIndex + 1
        if (nextIndex < state.queue.size) {
            val nextTrack = state.queue[nextIndex]
            audioAnalysisManager.analyzeTrack(nextTrack)
        }
    }

    fun ensureMediaServiceStarted() {
        if (VybeMediaService.instance != null) return
        try {
            val intent = Intent(context, VybeMediaService::class.java)
            try {
                context.startService(intent)
            } catch (e: IllegalStateException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerBackgroundCachePrune(activeTrack: Track, skippedTrack: Track? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeKeys = setOfNotNull(
                    activeTrack.id.takeIf { it.isNotBlank() },
                    activeTrack.streamUrl.takeIf { it.isNotBlank() }
                )
                if (skippedTrack != null && !skippedTrack.isDownloaded) {
                    val skippedKeys = listOfNotNull(
                        skippedTrack.id.takeIf { it.isNotBlank() },
                        skippedTrack.streamUrl.takeIf { it.isNotBlank() }
                    )
                    if (skippedKeys.isNotEmpty()) {
                        AudioCacheHolder.removeTrackCache(skippedKeys, preserveKeys = activeKeys)
                    }
                }
                // LRU pruning if total audio cache exceeds ~6 MB, strictly preserving active track buffer
                AudioCacheHolder.pruneOldestAudio(targetBytes = 6 * 1024 * 1024L, preserveKeys = activeKeys)
                // Schedule general storage cleanup (temp files, partial chunks, limits)
                com.example.vybe.core.storage.StorageCleanupManager.getInstance(context).scheduleMaintenance(activeKeys)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playMediaItem(track: Track, explicitPositionMs: Long? = null) {
        ensureMediaServiceStarted()
        if (!isDucked) {
            requestAudioFocus()
        }
        hasSkippedOutroForCurrentTrack = false
        trackPlayStartTimeMs = System.currentTimeMillis()
        recordSessionPlayed(track)

        val mediaUri = if (track.isDownloaded && !track.downloadPath.isNullOrBlank()) {
            Uri.parse(track.downloadPath)
        } else {
            Uri.parse(track.streamUrl)
        }

        val sleepTimerSec = _playbackState.value.sleepTimerRemainingSec
        val subtitleText = if (sleepTimerSec > 0) "Sleep timer: ${(sleepTimerSec + 59) / 60} min left" else track.albumName

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistName)
            .setAlbumTitle(track.albumName)
            .setSubtitle(subtitleText)
            .setDescription(if (sleepTimerSec > 0) subtitleText else null)
            .setArtworkUri(if (track.artworkUrl.isNotBlank()) Uri.parse(track.artworkUrl) else null)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(mediaUri)
            .setCustomCacheKey(track.id.ifBlank { track.streamUrl })
            .setMediaMetadata(mediaMetadata)
            .build()

        val introSkip = if (isAutoSkipIntroOutroEnabled && (track.durationMs == 0L || track.durationMs > AudioAnalysisManager.MIN_DURATION_FOR_SKIP_MS)) AudioAnalysisManager.FIXED_INTRO_MS else 0L
        val outroSkip = if (isAutoSkipIntroOutroEnabled && (track.durationMs == 0L || track.durationMs > AudioAnalysisManager.MIN_DURATION_FOR_SKIP_MS)) AudioAnalysisManager.FIXED_OUTRO_MS else 0L
        currentTrackIntroSkipMs = introSkip
        currentTrackOutroSkipMs = outroSkip
        hasSkippedOutroForCurrentTrack = false

        val startPosition = explicitPositionMs ?: introSkip

        exoPlayer.setMediaItem(mediaItem, startPosition)
        exoPlayer.prepare()
        exoPlayer.play()

        if (startPosition > 0L) {
            _playbackState.update { it.copy(currentPositionMs = startPosition) }
        }

        // Bind Equalizer to Audio Session
        try {
            val sessionId = exoPlayer.audioSessionId
            if (sessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) {
                equalizerManager.bindAudioSession(sessionId)
                updateAuraAudioState()
            }
        } catch (e: Exception) {
            // Non-blocking
        }

        com.example.vybe.widget.VybeWidgetProvider.updateAllWidgets(
            context, track.title, track.artistName, true, track.artworkUrl
        )

        preAnalyzeNextTrack()
        triggerBackgroundCachePrune(activeTrack = track)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        ensureMediaServiceStarted()
        if (requestAudioFocus()) {
            if (isDucked) {
                restoreAudio()
            }
            exoPlayer.play()
        }
    }

    fun skipToNext() {
        activePlayJob?.cancel()
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        val skipped = state.currentTrack
        recordFastSkipIfApplicable(skipped)

        val nextIndex = state.queueIndex + 1
        if (nextIndex < queue.size) {
            val nextTrack = queue[nextIndex]
            _playbackState.update { it.copy(currentTrack = nextTrack, queueIndex = nextIndex) }
            playMediaItem(nextTrack)
            recordSessionPlayed(nextTrack)
            scope.launch { musicRepository.recordPlayback(nextTrack) }
            triggerBackgroundCachePrune(activeTrack = nextTrack, skippedTrack = skipped)
            // Dynamic Context Switch: The newly playing song becomes the NEW RECOMMENDATION CONTEXT!
            recalculateUpcomingQueue(nextTrack)
        } else if (state.repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
            val firstTrack = queue[0]
            _playbackState.update { it.copy(currentTrack = firstTrack, queueIndex = 0) }
            playMediaItem(firstTrack)
            recordSessionPlayed(firstTrack)
            scope.launch { musicRepository.recordPlayback(firstTrack) }
            triggerBackgroundCachePrune(activeTrack = firstTrack, skippedTrack = skipped)
            recalculateUpcomingQueue(firstTrack)
        } else {
            // Reached end of queue: immediately transition using local instant fallback so music never stops!
            val current = state.currentTrack ?: queue.lastOrNull()
            if (current != null) {
                val instantTracks = musicRepository.recommendationEngine.getInstantFallbackQueue(current, limit = 6)
                if (instantTracks.isNotEmpty()) {
                    val nextTrack = instantTracks.first()
                    val immediateQueue = queue + instantTracks
                    _playbackState.update {
                        it.copy(
                            queue = immediateQueue,
                            queueIndex = queue.size,
                            currentTrack = nextTrack
                        )
                    }
                    playMediaItem(nextTrack)
                    recordSessionPlayed(nextTrack)
                    scope.launch { musicRepository.recordPlayback(nextTrack) }
                    triggerBackgroundCachePrune(activeTrack = nextTrack, skippedTrack = skipped)
                    recalculateUpcomingQueue(nextTrack)
                } else {
                    scope.launch(Dispatchers.IO) {
                        val freshBatch = musicRepository.getAutoplayQueue(
                            seedTrack = current,
                            existingQueue = queue,
                            skippedTrackIds = skippedTrackIds.toSet(),
                            skippedTitles = skippedTrackTitles.toSet(),
                            sessionPlayedTrackIds = sessionPlayedIds.toSet(),
                            sessionPlayedTitles = sessionPlayedTitles.toSet(),
                            limit = 20
                        )
                        if (freshBatch.isNotEmpty()) {
                            val nextTrack = freshBatch.first()
                            val updatedQueue = queue + freshBatch
                            withContext(Dispatchers.Main) {
                                _playbackState.update {
                                    it.copy(
                                        queue = updatedQueue,
                                        queueIndex = queue.size,
                                        currentTrack = nextTrack
                                    )
                                }
                                playMediaItem(nextTrack)
                                recordSessionPlayed(nextTrack)
                                scope.launch { musicRepository.recordPlayback(nextTrack) }
                                triggerBackgroundCachePrune(activeTrack = nextTrack, skippedTrack = skipped)
                                recalculateUpcomingQueue(nextTrack)
                            }
                        }
                    }
                }
            }
        }
    }

    fun skipToPrevious() {
        activePlayJob?.cancel()
        val state = _playbackState.value
        val introSkip = if (isAutoSkipIntroOutroEnabled && (state.currentTrack?.durationMs ?: 0L) > AudioAnalysisManager.MIN_DURATION_FOR_SKIP_MS) AudioAnalysisManager.FIXED_INTRO_MS else 0L
        val threshold = introSkip + 2000L
        if (exoPlayer.currentPosition > threshold) {
            hasSkippedOutroForCurrentTrack = false
            seekTo(introSkip)
            return
        }

        val prevIndex = state.queueIndex - 1
        if (prevIndex >= 0 && prevIndex < state.queue.size) {
            val skipped = state.currentTrack
            recordFastSkipIfApplicable(skipped)
            val prevTrack = state.queue[prevIndex]
            _playbackState.update { it.copy(currentTrack = prevTrack, queueIndex = prevIndex) }
            playMediaItem(prevTrack)
            scope.launch { musicRepository.recordPlayback(prevTrack) }
            triggerBackgroundCachePrune(activeTrack = prevTrack, skippedTrack = skipped)
        } else {
            hasSkippedOutroForCurrentTrack = false
            seekTo(introSkip)
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = exoPlayer.duration
        if (isAutoSkipIntroOutroEnabled && duration > AudioAnalysisManager.MIN_DURATION_FOR_SKIP_MS) {
            // If user manually seeks into the last 7 seconds, let them listen to it without instantly triggering auto-skip
            hasSkippedOutroForCurrentTrack = positionMs >= (duration - AudioAnalysisManager.FIXED_OUTRO_MS)
        }
        exoPlayer.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun toggleShuffle() {
        val currentShuffle = _playbackState.value.shuffleEnabled
        val newShuffle = !currentShuffle
        val currentTrack = _playbackState.value.currentTrack

        val newQueue = if (newShuffle && currentTrack != null) {
            listOf(currentTrack) + (originalQueue - currentTrack).shuffled()
        } else {
            originalQueue
        }
        val newIndex = if (currentTrack != null) newQueue.indexOfFirst { it.id == currentTrack.id } else 0

        _playbackState.update {
            it.copy(
                shuffleEnabled = newShuffle,
                queue = newQueue,
                queueIndex = newIndex.coerceAtLeast(0)
            )
        }
        VybeMediaService.instance?.updateNotificationNow()
    }

    fun toggleRepeatMode() {
        val current = _playbackState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.update { it.copy(repeatMode = next) }
        VybeMediaService.instance?.updateNotificationNow()
    }

    fun seekRelative(offsetMs: Long) {
        val current = exoPlayer.currentPosition
        val duration = exoPlayer.duration
        val target = (current + offsetMs).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        seekTo(target)
    }

    fun toggleFavoriteCurrentTrack() {
        val track = _playbackState.value.currentTrack ?: return
        scope.launch(Dispatchers.IO) {
            val newLiked = musicRepository.toggleLike(track)
            withContext(Dispatchers.Main) {
                _playbackState.update { state ->
                    val updatedTrack = state.currentTrack?.let { if (it.id == track.id) it.copy(isLiked = newLiked) else it }
                    state.copy(currentTrack = updatedTrack)
                }
                VybeMediaService.instance?.updateNotificationNow()
            }
        }
    }

    fun downloadCurrentTrack() {
        val track = _playbackState.value.currentTrack ?: return
        if (track.isDownloaded) return
        val app = context.applicationContext as? VybeApplication ?: return
        scope.launch(Dispatchers.IO) {
            val result = app.downloadRepository.downloadTrack(track)
            if (result.isSuccess) {
                val savedPath = result.getOrNull()
                withContext(Dispatchers.Main) {
                    _playbackState.update { state ->
                        val updatedTrack = state.currentTrack?.let {
                            if (it.id == track.id) it.copy(isDownloaded = true, downloadPath = savedPath) else it
                        }
                        state.copy(currentTrack = updatedTrack)
                    }
                    VybeMediaService.instance?.updateNotificationNow()
                }
            }
        }
    }

    fun addToQueue(track: Track) {
        _playbackState.update { state ->
            val updated = state.queue + track
            state.copy(queue = updated)
        }
        originalQueue = originalQueue + track
    }

    fun addNextInQueue(track: Track) {
        _playbackState.update { state ->
            val currentIndex = state.queueIndex
            val updated = state.queue.toMutableList()
            if (currentIndex in updated.indices) {
                updated.add(currentIndex + 1, track)
            } else {
                updated.add(track)
            }
            state.copy(queue = updated)
        }
    }

    fun removeFromQueue(index: Int) {
        _playbackState.update { state ->
            val updated = state.queue.toMutableList()
            if (index in updated.indices && index != state.queueIndex) {
                updated.removeAt(index)
                val newIndex = if (index < state.queueIndex) state.queueIndex - 1 else state.queueIndex
                state.copy(queue = updated, queueIndex = newIndex)
            } else {
                state
            }
        }
    }

    fun clearQueue() {
        val current = _playbackState.value.currentTrack
        _playbackState.update { state ->
            state.copy(
                queue = if (current != null) listOf(current) else emptyList(),
                queueIndex = if (current != null) 0 else -1
            )
        }
    }

    private fun handleTrackEnded() {
        when (_playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                val state = _playbackState.value
                val introSkip = if (isAutoSkipIntroOutroEnabled && (state.currentTrack?.durationMs ?: 0L) > AudioAnalysisManager.MIN_DURATION_FOR_SKIP_MS) AudioAnalysisManager.FIXED_INTRO_MS else 0L
                hasSkippedOutroForCurrentTrack = false
                seekTo(introSkip)
                resume()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                skipToNext()
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var hasCheckedQueueExtension = false
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val duration = exoPlayer.duration.coerceAtLeast(0L)

                    _playbackState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = duration
                        )
                    }

                    // Proactively ensure queue has plenty of upcoming songs
                    if (duration > 10000L && currentPos >= duration / 2 && !hasCheckedQueueExtension) {
                        hasCheckedQueueExtension = true
                        checkAndExtendAutoplayQueue()
                    }

                    // Fixed 7s Outro Skip: Automatically skip ending 7 seconds and play next song
                    if (isAutoSkipIntroOutroEnabled && duration > AudioAnalysisManager.MIN_DURATION_FOR_SKIP_MS && currentPos >= (duration - AudioAnalysisManager.FIXED_OUTRO_MS) && !hasSkippedOutroForCurrentTrack) {
                        hasSkippedOutroForCurrentTrack = true
                        handleTrackEnded()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        try {
            context.unregisterReceiver(becomingNoisyReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        releaseLocks()
        duckingJob?.cancel()
        stopProgressTracker()
        sleepTimerManager.cancelTimer()
        exoPlayer.release()
        com.example.vybe.core.storage.StorageCleanupManager.getInstance(context).scheduleMaintenance()
    }
}
