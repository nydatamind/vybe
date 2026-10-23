package com.example.vybe.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.AnnotatedString
import com.example.vybe.playback.LyricsManager
import com.example.vybe.core.model.LrcDocument
import com.example.vybe.core.model.LyricLine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.vybe.core.datastore.PreferencesManager
import com.example.vybe.core.designsystem.DarkBackground
import com.example.vybe.core.designsystem.DarkCard
import com.example.vybe.core.designsystem.DarkSurfaceElevated
import com.example.vybe.core.designsystem.TextMutedDark
import com.example.vybe.core.designsystem.TextSecondaryDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.designsystem.VybeEmeraldBright
import com.example.vybe.core.designsystem.VybePurple
import com.example.vybe.core.model.PlaybackState
import com.example.vybe.core.model.RepeatMode
import com.example.vybe.core.model.Track
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FullPlayerModal(
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onToggleAuraSpatialAudio: () -> Unit = {},
    onDownloadTrack: (Track) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    // 0 = Original Multi-Layer Aura, 1 = Audio Wave, 2 = Liquid Flow, 3 = Floating Particles, 4 = Dynamic Orb Pulse
    val savedAnimationIndex by preferencesManager.playerThemeIndex.collectAsState(initial = 0)
    val currentAnimationIndex = savedAnimationIndex % 5

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderPosition by remember { mutableFloatStateOf(0f) }

    val isPlaying = playbackState.isPlaying
    val infiniteTransition = rememberInfiniteTransition(label = "playerVisualTransitions")

    // Slow ambient rotation
    val auraRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "auraRot1"
    )

    val auraRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "auraRot2"
    )

    // Dynamic Voice/Beat Low-Up Pulse
    val voiceScaleWide by infiniteTransition.animateFloat(
        initialValue = 1.04f,
        targetValue = if (isPlaying) 1.34f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "voiceScaleWide"
    )

    val voiceScaleMid by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.18f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "voiceScaleMid"
    )

    val voiceLightAlpha by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 0.50f else 0.18f,
        targetValue = if (isPlaying) 0.95f else 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "voiceLightAlpha"
    )

    val posterBreathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.025f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "posterBreath"
    )

    // ==========================================
    // --- ANIMATION 2 (Index 1): Cyber Soundstage & Dynamic Equalizer Spectrum ---
    // ==========================================
    val equalizerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 2000 else 6000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "equalizerPhase"
    )
    val stagePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = if (isPlaying) 0.95f else 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "stagePulseAlpha"
    )

    // ==========================================
    // --- ANIMATION 3 (Index 2): Celestial Galaxy Vortex & Stardust Phase ---
    // ==========================================
    val galaxySpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 11000 else 22000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "galaxySpin"
    )
    val stardustPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 4000 else 8500, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "stardustPhase"
    )

    // ==========================================
    // --- THEMES 4 & 5 (Indices 3 & 4): Synced & Karaoke Live Lyrics Engine ---
    // ==========================================
    var activeThemeNotice by remember { mutableStateOf<String?>(null) }
    var noticeTimerJob by remember { mutableStateOf<Job?>(null) }

    var lrcDocument by remember(track.id) { mutableStateOf<LrcDocument?>(null) }
    var isLoadingLyrics by remember(track.id) { mutableStateOf(false) }

    LaunchedEffect(track.id, currentAnimationIndex) {
        if (currentAnimationIndex >= 3 && lrcDocument == null && !isLoadingLyrics) {
            isLoadingLyrics = true
            try {
                lrcDocument = LyricsManager.getLyricsForTrack(track)
            } catch (e: Exception) {
                lrcDocument = null
            } finally {
                isLoadingLyrics = false
            }
        }
    }

    val currentPosition = playbackState.currentPositionMs
    val lyricLines = lrcDocument?.lines.orEmpty()
    val currentLineIndex = remember(currentPosition, lyricLines) {
        if (lyricLines.isEmpty()) -1
        else {
            val idx = lyricLines.indexOfLast { it.timeMs <= currentPosition }
            if (idx == -1) 0 else idx
        }
    }

    val mainScrollState = rememberScrollState()

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .then(if (currentAnimationIndex < 3) Modifier.verticalScroll(mainScrollState) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Cycles through 3 visual themes + 2 live synced/karaoke lyrics themes
                        IconButton(onClick = {
                            val nextIndex = (currentAnimationIndex + 1) % 5
                            coroutineScope.launch {
                                preferencesManager.setPlayerThemeIndex(nextIndex)
                            }
                            activeThemeNotice = when (nextIndex) {
                                0 -> "✨ Velvet Aura Prism"
                                1 -> "📊 Cyber Spectrum Stage"
                                2 -> "🌌 Celestial Galaxy Vortex"
                                3 -> "🎵 Synced Lyrics (Line-by-Line)"
                                4 -> "🎤 Karaoke Lyrics (Word-by-Word)"
                                else -> "Player Theme"
                            }
                            noticeTimerJob?.cancel()
                            noticeTimerJob = coroutineScope.launch {
                                kotlinx.coroutines.delay(2000)
                                activeThemeNotice = null
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Switch Animation Layout",
                                tint = VybeEmeraldBright,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Sleep Timer Button
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (playbackState.sleepTimerRemainingSec > 0) VybeEmeraldBright else TextSecondaryDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Sleek animated indicator showing switched theme name
                AnimatedVisibility(
                    visible = activeThemeNotice != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = Color(0xEE141722),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, VybeEmerald.copy(alpha = 0.6f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = activeThemeNotice ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3 VISUAL THEMES + 2 LIVE LYRICS THEMES ---
                when (currentAnimationIndex) {
                    // Animation 1 — Existing Original: Multi-layer Rotating Ambient Light Aura
                    0 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Layer 1: Wide Diffuse Ambient Glow (Rotates clockwise)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(voiceScaleWide)
                                    .rotate(auraRotation1)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF8A2BE2).copy(alpha = voiceLightAlpha * 0.75f), // Neon Purple
                                                Color(0xFF00E5FF).copy(alpha = voiceLightAlpha * 0.55f), // Electric Cyan
                                                Color(0xFF00FF88).copy(alpha = voiceLightAlpha * 0.35f), // Vibra Emerald
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Layer 2: Counter-Rotating Vivid Voice-Reactive Aura
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(voiceScaleMid)
                                    .rotate(auraRotation2)
                                    .clip(RoundedCornerShape(42.dp))
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(
                                                Color(0xFF00F0FF).copy(alpha = voiceLightAlpha * 0.75f),
                                                Color(0xFFE040FB).copy(alpha = voiceLightAlpha * 0.8f),
                                                Color(0xFF00E5FF).copy(alpha = voiceLightAlpha * 0.7f),
                                                Color(0xFF3D5AFE).copy(alpha = voiceLightAlpha * 0.75f),
                                                Color(0xFF00F0FF).copy(alpha = voiceLightAlpha * 0.75f)
                                            )
                                        )
                                    )
                            )

                            // Layer 3: Inner Soundstage Center Halo
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.95f)
                                    .scale(voiceScaleMid)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF00F0FF).copy(alpha = voiceLightAlpha * 0.55f),
                                                Color(0xFF9D00FF).copy(alpha = voiceLightAlpha * 0.45f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Poster Card Presentation (Large square 0.88f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(posterBreathScale)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(1.5.dp, Brush.linearGradient(listOf(Color(0x99FFFFFF), Color(0x22FFFFFF))), RoundedCornerShape(24.dp))
                                    .shadow(24.dp, RoundedCornerShape(24.dp))
                            ) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    // =========================================================================
                    // Animation 2 (Index 1) — Cyber Soundstage & Dynamic Equalizer Spectrum
                    // =========================================================================
                    1 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Multi-layered Stage Glow Backdrop
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(1.30f)
                                    .scale(voiceScaleWide)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF00E5FF).copy(alpha = stagePulseAlpha * 0.45f), // Cyan
                                                Color(0xFF8A2BE2).copy(alpha = stagePulseAlpha * 0.35f), // Purple
                                                Color(0xFF00FF88).copy(alpha = stagePulseAlpha * 0.20f), // Emerald
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Dynamic Equalizer Spectrum & Floating Audio Flares Canvas
                            Canvas(modifier = Modifier.fillMaxSize(1.42f)) {
                                val w = size.width
                                val h = size.height
                                val midX = w / 2f
                                val midY = h / 2f

                                // 1. Sleek Floating Audio Visualizer Spectrum Bars (20 bars across bottom)
                                val barCount = 20
                                val spectrumWidth = w * 0.84f
                                val startSpectrumX = (w - spectrumWidth) / 2f
                                val barSpacing = spectrumWidth / barCount
                                val barW = barSpacing * 0.65f
                                val maxBarH = if (isPlaying) 44.dp.toPx() else 18.dp.toPx()
                                val baselineY = h * 0.90f

                                for (i in 0 until barCount) {
                                    val distFromCenter = kotlin.math.abs(i - (barCount / 2f)) / (barCount / 2f)
                                    val centerWeight = 1.0f - (distFromCenter * 0.45f)
                                    val dynamicFactor = (sin((equalizerPhase * 2.2 + i * 0.42).toDouble()).toFloat() * 0.5f + 0.5f) * centerWeight
                                    val currentBarH = (maxBarH * dynamicFactor.coerceIn(0.18f, 1.0f))

                                    val barX = startSpectrumX + i * barSpacing + (barSpacing - barW) / 2f
                                    val barTopY = baselineY - currentBarH

                                    val barGradient = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF00FF88), // Top Emerald
                                            Color(0xFF00E5FF), // Mid Cyan
                                            Color(0xFF8A2BE2)  // Bottom Violet
                                        ),
                                        startY = barTopY,
                                        endY = baselineY
                                    )

                                    drawRoundRect(
                                        brush = barGradient,
                                        topLeft = Offset(barX, barTopY),
                                        size = androidx.compose.ui.geometry.Size(barW, currentBarH),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f),
                                        alpha = if (isPlaying) 0.90f else 0.50f
                                    )

                                    // Top Glow Cap
                                    drawCircle(
                                        color = Color.White.copy(alpha = if (isPlaying) 0.85f else 0.4f),
                                        radius = barW * 0.45f,
                                        center = Offset(barX + barW / 2f, barTopY + barW * 0.45f)
                                    )
                                }

                                // 2. Dual Chromatic Top Laser Beams
                                val topAmp = if (isPlaying) 14.dp.toPx() else 6.dp.toPx()
                                val topWaveY = h * 0.08f
                                val pathTop = Path().apply {
                                    moveTo(0f, topWaveY)
                                    for (x in 0..w.toInt() step 6) {
                                        val y = topWaveY + sin((x / w * 3 * Math.PI + equalizerPhase).toDouble()).toFloat() * topAmp
                                        lineTo(x.toFloat(), y)
                                    }
                                }
                                drawPath(
                                    path = pathTop,
                                    brush = Brush.horizontalGradient(listOf(Color(0xFF00FF88), Color(0xFF00E5FF), Color(0xFFE040FB))),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // 3. Floating Ambient Sound Flares
                                for (f in 0 until 12) {
                                    val flareX = ((f * 37) % 100) / 100f * w
                                    val flareBaseY = ((f * 53) % 100) / 100f * h
                                    val floatOffset = sin((equalizerPhase * 1.5 + f).toDouble()).toFloat() * 10.dp.toPx()
                                    val flareY = (flareBaseY + floatOffset).coerceIn(0f, h)
                                    val flareRadius = (2.5f + (f % 3) * 1.5f).dp.toPx()
                                    val flareColor = when (f % 3) {
                                        0 -> Color(0xFF00FF88)
                                        1 -> Color(0xFF00E5FF)
                                        else -> Color(0xFFFF007F)
                                    }
                                    drawCircle(
                                        color = flareColor.copy(alpha = if (isPlaying) 0.65f else 0.35f),
                                        radius = flareRadius,
                                        center = Offset(flareX, flareY)
                                    )
                                }
                            }

                            // Cyber Stage Poster Card
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(posterBreathScale)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                Color(0xFF00FF88),
                                                Color(0xFF00E5FF),
                                                Color(0xFF8A2BE2),
                                                Color(0xFFFF007F),
                                                Color(0xFF00FF88)
                                            )
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .shadow(28.dp, RoundedCornerShape(24.dp))
                            ) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Inner glass rim highlight
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                Brush.linearGradient(listOf(Color(0x77FFFFFF), Color(0x11FFFFFF)))
                                            ),
                                            RoundedCornerShape(24.dp)
                                        )
                                    )
                            }
                        }
                    }

                    // =========================================================================
                    // Visual Theme 3 (Index 2) — Celestial Galaxy Vortex & Starlight Canvas
                    // =========================================================================
                    2 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Deep Space Swirling Accretion Disk & 36 Spiral Stardust Particles Canvas
                            Canvas(modifier = Modifier.fillMaxSize(1.48f)) {
                                val w = size.width
                                val h = size.height
                                val midX = w / 2f
                                val midY = h / 2f

                                // Central Nebula Glow Accretion Disk
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF8A2BE2).copy(alpha = 0.45f),
                                            Color(0xFF00E5FF).copy(alpha = 0.35f),
                                            Color(0xFF00FF88).copy(alpha = 0.20f),
                                            Color.Transparent
                                        ),
                                        center = Offset(midX, midY),
                                        radius = w * 0.65f
                                    ),
                                    center = Offset(midX, midY),
                                    radius = w * 0.65f
                                )

                                // 36 Spiral Galaxy Stardust Particles
                                val particleCount = 36
                                for (i in 0 until particleCount) {
                                    val fraction = i.toFloat() / particleCount
                                    val spiralRadius = (w * 0.22f) + (fraction * w * 0.38f)
                                    val angle = (galaxySpin * Math.PI / 180f) + (fraction * 4f * Math.PI)
                                    val px = midX + cos(angle).toFloat() * spiralRadius
                                    val py = midY + sin(angle).toFloat() * (spiralRadius * 0.85f)

                                    val pColor = when (i % 4) {
                                        0 -> Color(0xFFFFD700) // Starlight Gold
                                        1 -> Color(0xFF00E5FF) // Cosmic Cyan
                                        2 -> Color(0xFF00FF88) // Emerald Star
                                        else -> Color(0xFFE040FB) // Violet Nebula
                                    }
                                    val pRadius = (2.dp + ((i % 3) * 1.5f).dp).toPx()
                                    val pAlpha = (0.4f + ((i % 4) * 0.18f)).coerceIn(0.2f, 0.95f)

                                    drawCircle(
                                        color = pColor.copy(alpha = pAlpha),
                                        radius = pRadius,
                                        center = Offset(px, py)
                                    )
                                }

                                // 3 Shooting Star Trails across background
                                for (s in 0 until 3) {
                                    val sProgress = ((stardustPhase * 1.5f + s * 0.33f) % 1f)
                                    val startX = w * (0.1f + s * 0.3f) + sProgress * (w * 0.5f)
                                    val startY = h * (0.05f + s * 0.25f) + sProgress * (h * 0.45f)
                                    val tailLen = 30.dp.toPx()
                                    val endX = startX - tailLen * 0.7f
                                    val endY = startY - tailLen * 0.7f
                                    val sAlpha = (sin((sProgress * Math.PI).toDouble()).toFloat() * 0.75f).coerceIn(0f, 0.8f)

                                    drawLine(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color.White.copy(alpha = sAlpha), Color(0xFF00E5FF).copy(alpha = 0f)),
                                            start = Offset(startX, startY),
                                            end = Offset(endX, endY)
                                        ),
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }

                            // Celestial Frameless Card (0.80f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.80f)
                                    .aspectRatio(1f)
                                    .scale(posterBreathScale)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                Color(0xFF00E5FF).copy(alpha = 0.9f),
                                                Color(0xFFFFD700).copy(alpha = 0.8f),
                                                Color(0xFF00FF88).copy(alpha = 0.9f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .shadow(32.dp, RoundedCornerShape(24.dp))
                            ) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // =========================================================================
                    // Live Lyrics Theme 1 (Index 3) — Line-by-Line Synced Lyrics
                    // =========================================================================
                    3 -> {
                        val lyricsListState = rememberLazyListState()

                        LaunchedEffect(currentLineIndex) {
                            if (currentLineIndex in lyricLines.indices) {
                                lyricsListState.animateScrollToItem(
                                    index = (currentLineIndex - 1).coerceAtLeast(0)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xE0131726), Color(0xF00A0D14))))
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF00FF88).copy(alpha = 0.8f),
                                                Color(0xFF00E5FF).copy(alpha = 0.6f),
                                                Color(0xFF8A2BE2).copy(alpha = 0.4f)
                                            )
                                        )
                                    ),
                                    RoundedCornerShape(28.dp)
                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Mini Player Header inside Lyrics view
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFF00FF88).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    ) {
                                        AsyncImage(
                                            model = track.artworkUrl,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artistName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0x3300FF88),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0x8800FF88))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color(0xFF00FF88),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LINE SYNC",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF00FF88)
                                        )
                                    }
                                }
                            }

                            // Synced Lyrics LazyColumn
                            if (isLoadingLyrics) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Searching synced lyrics... ✨",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (lyricLines.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color(0x8800FF88),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No timed lyrics available for this track",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Enjoy the rhythm & visuals! 🎧",
                                            color = TextSecondaryDark,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = lyricsListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(lyricLines) { index, line ->
                                        val isCurrent = index == currentLineIndex
                                        val isPast = index < currentLineIndex

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .then(
                                                    if (isCurrent) {
                                                        Modifier
                                                            .background(
                                                                Brush.horizontalGradient(
                                                                    listOf(
                                                                        Color(0x3300FF88),
                                                                        Color(0x2200E5FF)
                                                                    )
                                                                )
                                                            )
                                                            .border(
                                                                BorderStroke(1.dp, Color(0x8800FF88)),
                                                                RoundedCornerShape(14.dp)
                                                            )
                                                    } else Modifier
                                                )
                                                .clickable { onSeek(line.timeMs) }
                                                .padding(horizontal = 14.dp, vertical = if (isCurrent) 12.dp else 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isCurrent) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00FF88),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text(
                                                    text = line.text,
                                                    fontSize = if (isCurrent) 19.sp else 15.sp,
                                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = when {
                                                        isCurrent -> Color.White
                                                        isPast -> Color(0x66FFFFFF)
                                                        else -> Color(0xAAFFFFFF)
                                                    },
                                                    lineHeight = if (isCurrent) 26.sp else 22.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // Live Lyrics Theme 2 (Index 4) — Word-by-Word Live Karaoke Stage
                    // =========================================================================
                    4 -> {
                        val currentLine = lyricLines.getOrNull(currentLineIndex)
                        val prevLine = if (currentLineIndex > 0) lyricLines.getOrNull(currentLineIndex - 1) else null
                        val nextLine = lyricLines.getOrNull(currentLineIndex + 1)

                        // Calculate dynamic word-by-word progress within active line
                        val lineStart = currentLine?.timeMs ?: 0L
                        val lineEnd = nextLine?.timeMs ?: (lineStart + 3500L)
                        val lineDuration = (lineEnd - lineStart).coerceAtLeast(1000L)
                        val lineElapsed = (currentPosition - lineStart).coerceIn(0L, lineDuration)
                        val lineProgress = (lineElapsed.toFloat() / lineDuration.toFloat()).coerceIn(0f, 1f)

                        val words = remember(currentLine?.text) {
                            currentLine?.text?.split(" ")?.filter { it.isNotBlank() }.orEmpty()
                        }
                        val activeWordCount = (lineProgress * (words.size + 1)).toInt().coerceIn(0, words.size)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xF0181528), Color(0xF5090C17))))
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFFFD700),
                                                Color(0xFF00FF88),
                                                Color(0xFF00E5FF),
                                                Color(0xFFE040FB),
                                                Color(0xFFFFD700)
                                            )
                                        )
                                    ),
                                    RoundedCornerShape(28.dp)
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Bar: Track mini info & glowing Karaoke badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                                    ) {
                                        AsyncImage(
                                            model = track.artworkUrl,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artistName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0x33FF007F),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFF007F))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color(0xFFFF4081),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LIVE KARAOKE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFF4081)
                                        )
                                    }
                                }
                            }

                            // Middle Karaoke Stage: Previous Line + Active Line (Word-by-word) + Progress
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (prevLine != null) {
                                    Text(
                                        text = prevLine.text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0x44FFFFFF),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                if (currentLine != null && words.isNotEmpty()) {
                                    val karaokeAnnotated = buildAnnotatedString {
                                        words.forEachIndexed { wIdx, word ->
                                            if (wIdx < activeWordCount) {
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color(0xFF00FF88), // Singing word highlight
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 23.sp
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                            } else {
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color(0x66FFFFFF), // Upcoming word
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 23.sp
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                            }
                                            if (wIdx < words.size - 1) append(" ")
                                        }
                                    }

                                    Text(
                                        text = karaokeAnnotated,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 30.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Line Singing Progress Indicator Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x33FFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(lineProgress)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            Color(0xFFFFD700),
                                                            Color(0xFF00FF88),
                                                            Color(0xFF00E5FF)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                } else if (isLoadingLyrics) {
                                    Text(
                                        text = "Loading karaoke lyrics... 🎤",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Sing Along with VYBE! 🎵",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Lyrics in sync will appear here when available",
                                            color = TextSecondaryDark,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Bottom Card: Next Line Preview & Fast Line Skip Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = Color(0x2200E5FF),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0x5500E5FF)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "NEXT ❯ ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF00E5FF)
                                        )
                                        Text(
                                            text = nextLine?.text ?: "Instrumental / Outro",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (currentLineIndex > 0) {
                                    IconButton(
                                        onClick = {
                                            val target = lyricLines.getOrNull(currentLineIndex - 1)
                                            if (target != null) onSeek(target.timeMs)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "Previous Line",
                                            tint = Color(0xFF00FF88),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (nextLine != null) {
                                    IconButton(
                                        onClick = { onSeek(nextLine.timeMs) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "Next Line",
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Track Title & Artist Info & Like Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = track.artistName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onToggleLike(track) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (track.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (track.isLiked) VybeEmerald else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    val currentProgress = if (isDraggingSlider) dragSliderPosition else playbackState.progressFraction
                    Slider(
                        value = currentProgress,
                        onValueChange = {
                            isDraggingSlider = true
                            dragSliderPosition = it
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            val targetMs = (dragSliderPosition * playbackState.durationMs).toLong()
                            onSeek(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = VybeEmerald,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayMs = if (isDraggingSlider) {
                            (dragSliderPosition * playbackState.durationMs).toLong()
                        } else {
                            playbackState.currentPositionMs
                        }
                        Text(
                            text = formatTime(displayMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = formatTime(playbackState.durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Button
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleEnabled) VybeEmeraldBright else TextSecondaryDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Replay 10s Button
                    IconButton(onClick = { onSeek((playbackState.currentPositionMs - 10000L).coerceAtLeast(0L)) }) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Replay 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Previous Track Button
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play/Pause Main FAB
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(68.dp)
                            .clickable { onPlayPause() },
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Next Track Button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Forward 10s Button
                    IconButton(onClick = { onSeek((playbackState.currentPositionMs + 10000L).coerceAtMost(playbackState.durationMs)) }) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat Button
                    IconButton(onClick = onToggleRepeat) {
                        val repeatIcon = when (playbackState.repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        }
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != RepeatMode.OFF) VybeEmeraldBright else TextSecondaryDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Extra Action Bar (Queue, Add to playlist, Download)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onAddToPlaylist(track) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { onDownloadTrack(track) }) {
                        Icon(
                            imageVector = if (track.isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                            contentDescription = "Download",
                            tint = if (track.isDownloaded) VybeEmeraldBright else TextSecondaryDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onToggleAuraSpatialAudio()
                            val isEnabled = !playbackState.isAuraSpatialAudioEnabled
                            Toast.makeText(
                                context,
                                if (isEnabled) "Aura 8D Audio: ON" else "Aura 8D Audio: OFF",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SurroundSound,
                                    contentDescription = if (playbackState.isAuraSpatialAudioEnabled) "8D Audio ON" else "8D Audio OFF",
                                    tint = if (playbackState.isAuraSpatialAudioEnabled) VybeEmeraldBright else TextSecondaryDark,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "8D",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (playbackState.isAuraSpatialAudioEnabled) VybeEmeraldBright else TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modern Sleep Timer Dialog with Quick Options + Custom Hours & Minutes
    if (showSleepTimerDialog) {
        var customHours by remember { mutableIntStateOf(0) }
        var customMinutes by remember { mutableIntStateOf(30) }

        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = VybeEmeraldBright,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sleep Timer",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Active Countdown Status & Cancel Button
                    if (playbackState.sleepTimerRemainingSec > 0) {
                        val remSec = playbackState.sleepTimerRemainingSec
                        val remHrs = remSec / 3600
                        val remMins = (remSec % 3600) / 60
                        val remSecs = remSec % 60
                        val formattedTime = if (remHrs > 0) {
                            "${remHrs}h ${remMins}m ${remSecs}s"
                        } else {
                            "${remMins}m ${remSecs}s"
                        }

                        Surface(
                            color = Color(0x2200FF88),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VybeEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Timer Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VybeEmeraldBright,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$formattedTime remaining",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = Color(0x33FF5252),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetSleepTimer(0)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Turn off timer",
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Quick Options: 15 min, 30 min, 45 min (60 min removed)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45).forEach { mins ->
                            Surface(
                                color = DarkCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onSetSleepTimer(mins)
                                        showSleepTimerDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                            ) {
                                Text(
                                    text = "$mins m",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Custom Sleep Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Interactive Hours & Minutes Steppers
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hours Stepper
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "HOURS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (customHours > 0) customHours-- },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Minus hour",
                                                tint = Color.White
                                            )
                                        }
                                        Text(
                                            text = "$customHours hr",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = { if (customHours < 12) customHours++ },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Plus hour",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }

                                // Minutes Stepper
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "MINUTES",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (customMinutes >= 5) customMinutes -= 5
                                                else if (customMinutes > 0) customMinutes = 0
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Minus 5 min",
                                                tint = Color.White
                                            )
                                        }
                                        Text(
                                            text = "$customMinutes min",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (customMinutes <= 50) customMinutes += 5
                                                else customMinutes = 59
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Plus 5 min",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Increment Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("+5m" to 5, "+15m" to 15, "+30m" to 30, "+1h" to 60).forEach { (label, addMins) ->
                                    Surface(
                                        color = Color(0x22FFFFFF),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val total = (customHours * 60 + customMinutes) + addMins
                                                customHours = (total / 60).coerceIn(0, 12)
                                                customMinutes = (total % 60).coerceIn(0, 59)
                                            }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val totalCustomMinutes = customHours * 60 + customMinutes

                    // Start Custom Timer Button
                    Button(
                        onClick = {
                            if (totalCustomMinutes > 0) {
                                onSetSleepTimer(totalCustomMinutes)
                                showSleepTimerDialog = false
                            }
                        },
                        enabled = totalCustomMinutes > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VybeEmerald,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val durationText = if (customHours > 0) {
                            "$customHours hr $customMinutes min"
                        } else {
                            "$customMinutes min"
                        }
                        Text(
                            text = "Set Custom Timer ($durationText)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close", color = VybeEmeraldBright)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
