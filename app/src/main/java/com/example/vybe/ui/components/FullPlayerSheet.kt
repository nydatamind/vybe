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
import androidx.compose.material.icons.filled.PlaylistAdd
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
import androidx.compose.material3.CircularProgressIndicator
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
    // --- ANIMATION 3 (Index 2): Ethereal Aurora Borealis & Bioluminescent Silk Mesh ---
    // ==========================================
    val auroraWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 4000 else 10000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "auroraWavePhase"
    )
    val auroraLevitate by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = if (isPlaying) 6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "auroraLevitate"
    )
    val fireflyPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 3500 else 7500, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "fireflyPhase"
    )

    // ==========================================
    // --- ANIMATION 4 (Index 3): Celestial Galaxy Vortex & Stardust Phase ---
    // (Preserved exactly as is - user preference)
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
    // --- ANIMATION 5 (Index 4): Sonic Bass Titan & Cyber Shockwave Portal ---
    // ==========================================
    val bassShockwaveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isPlaying) 1.55f else 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1100 else 2400, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "bassShockwaveScale1"
    )
    val bassShockwaveAlpha1 by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 0.90f else 0.40f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1100 else 2400, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "bassShockwaveAlpha1"
    )
    val bassShockwaveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isPlaying) 1.40f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1100 else 2400, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "bassShockwaveScale2"
    )
    val bassShockwaveAlpha2 by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 0.85f else 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1100 else 2400, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "bassShockwaveAlpha2"
    )
    val cyberBeamRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 7000 else 18000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "cyberBeamRotation"
    )

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
                    .verticalScroll(rememberScrollState()),
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
                        // 2-Arrow Button: Cycles through 5 visual background & layout animation styles (No text/labels)
                        IconButton(onClick = {
                            val nextIndex = (currentAnimationIndex + 1) % 5
                            coroutineScope.launch {
                                preferencesManager.setPlayerThemeIndex(nextIndex)
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

                Spacer(modifier = Modifier.height(16.dp))

                // --- 5 DYNAMIC ANIMATION & POSTER LAYOUT PRESENTATIONS ---
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
                    // Animation 3 (Index 2) — Ethereal Aurora Borealis & Bioluminescent Silk Mesh
                    // =========================================================================
                    2 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Rotating Chromatic Aurora Halo
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(1.35f)
                                    .scale(voiceScaleWide)
                                    .rotate(auraRotation1 * 0.7f)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(
                                                Color(0xFF00FF88).copy(alpha = 0.50f), // Emerald
                                                Color(0xFF00F5D4).copy(alpha = 0.55f), // Turquoise
                                                Color(0xFF00E5FF).copy(alpha = 0.60f), // Cyan
                                                Color(0xFF4D00FF).copy(alpha = 0.50f), // Indigo
                                                Color(0xFFE040FB).copy(alpha = 0.55f), // Magenta
                                                Color(0xFFFF5252).copy(alpha = 0.45f), // Coral
                                                Color(0xFF00FF88).copy(alpha = 0.50f)  // Emerald
                                            )
                                        )
                                    )
                            )

                            // Hypnotic Undulating Aurora Waves & Ascending Fireflies Canvas
                            Canvas(modifier = Modifier.fillMaxSize(1.48f)) {
                                val w = size.width
                                val h = size.height
                                val midX = w / 2f
                                val midY = h / 2f

                                // 1. Soft Layered Translucent Fluid Aurora Ribbons
                                val waveColors = listOf(
                                    listOf(Color(0xFF00FF88), Color(0xFF00E5FF)),
                                    listOf(Color(0xFF00E5FF), Color(0xFF8A2BE2)),
                                    listOf(Color(0xFFE040FB), Color(0xFF00F5D4))
                                )
                                for (wIndex in 0 until 3) {
                                    val yBase = h * (0.28f + wIndex * 0.24f)
                                    val waveAmp = if (isPlaying) (24.dp.toPx() + wIndex * 6.dp.toPx()) else 12.dp.toPx()
                                    val waveFreq = 2.0 + wIndex * 0.8
                                    val phaseOffset = auroraWavePhase + (wIndex * 1.8f)

                                    val wavePath = Path().apply {
                                        moveTo(0f, yBase)
                                        for (x in 0..w.toInt() step 5) {
                                            val y = yBase + sin((x / w * waveFreq * Math.PI + phaseOffset).toDouble()).toFloat() * waveAmp
                                            lineTo(x.toFloat(), y)
                                        }
                                    }

                                    drawPath(
                                        path = wavePath,
                                        brush = Brush.horizontalGradient(waveColors[wIndex]),
                                        style = Stroke(width = (4f + wIndex * 1.5f).dp.toPx()),
                                        alpha = if (isPlaying) 0.65f else 0.35f
                                    )
                                }

                                // 2. 16 Rising Bioluminescent Fireflies
                                for (i in 0 until 16) {
                                    val seedX = ((i * 31) % 100) / 100f
                                    val fireflyY = (1f - ((fireflyPhase * 1.1f + i * 0.0625f) % 1f)) * h
                                    val sway = sin((fireflyPhase * 2.5 * Math.PI + i * 1.2).toDouble()).toFloat() * 16.dp.toPx()
                                    val fireflyX = (seedX * w + sway).coerceIn(0f, w)
                                    val fColor = when (i % 4) {
                                        0 -> Color(0xFF00FF88) // Emerald
                                        1 -> Color(0xFF00F5D4) // Turquoise
                                        2 -> Color(0xFFFFD700) // Golden Glow
                                        else -> Color(0xFF00E5FF) // Cyan
                                    }
                                    val fRadius = (2.2f + (i % 3) * 1.4f).dp.toPx()
                                    val fAlpha = (sin((fireflyY / h * Math.PI).toDouble()).toFloat() * 0.85f).coerceIn(0.15f, 0.95f)

                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(fColor.copy(alpha = fAlpha), Color.Transparent),
                                            center = Offset(fireflyX, fireflyY),
                                            radius = fRadius * 2.2f
                                        ),
                                        radius = fRadius * 2.2f,
                                        center = Offset(fireflyX, fireflyY)
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = fAlpha),
                                        radius = fRadius * 0.5f,
                                        center = Offset(fireflyX, fireflyY)
                                    )
                                }
                            }

                            // Levitating Frosted Glass Poster Card
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(y = auroraLevitate.dp)
                                    .scale(posterBreathScale)
                                    .clip(RoundedCornerShape(28.dp))
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color(0xFF00FF88),
                                                Color(0xFF00F5D4),
                                                Color(0xFF00E5FF),
                                                Color(0xFF8A2BE2),
                                                Color(0xFFFF5252),
                                                Color(0xFF00FF88)
                                            )
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .shadow(32.dp, RoundedCornerShape(28.dp))
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
                    // Animation 4 (Index 3) — Celestial Galaxy Spiral Vortex & Starlight (Preserved)
                    // =========================================================================
                    3 -> {
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
                    // Animation 5 (Index 4) — Sonic Bass Titan & Cyber Shockwave Portal
                    // =========================================================================
                    4 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Multi-Ring Expanding Sonic Shockwaves & Sweeping Laser Beams Canvas
                            Canvas(modifier = Modifier.fillMaxSize(1.52f)) {
                                val w = size.width
                                val h = size.height
                                val midX = w / 2f
                                val midY = h / 2f
                                val baseCardHalfSize = w * 0.44f

                                // 1. Sweeping 360° Volumetric Laser Spotlight Beams (4 Quadrants)
                                for (b in 0 until 4) {
                                    val beamAngle = (cyberBeamRotation * Math.PI / 180f) + (b * (Math.PI / 2f))
                                    val beamEndX = midX + cos(beamAngle).toFloat() * (w * 0.70f)
                                    val beamEndY = midY + sin(beamAngle).toFloat() * (h * 0.70f)

                                    val beamColor = when (b % 4) {
                                        0 -> Color(0xFF00FF88) // Emerald
                                        1 -> Color(0xFF00E5FF) // Cyan
                                        2 -> Color(0xFF8A2BE2) // Purple
                                        else -> Color(0xFFFF007F) // Hot Pink
                                    }

                                    drawLine(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                beamColor.copy(alpha = if (isPlaying) 0.60f else 0.25f),
                                                Color.Transparent
                                            ),
                                            start = Offset(midX, midY),
                                            end = Offset(beamEndX, beamEndY)
                                        ),
                                        start = Offset(midX, midY),
                                        end = Offset(beamEndX, beamEndY),
                                        strokeWidth = 3.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                }

                                // 2. Concentric Pulsing Sonic Shockwave Rounded Rings (Bursting outwards on bass beats)
                                val shockwaveCornerRadius = 32.dp.toPx()
                                
                                // Shockwave 1
                                val size1 = baseCardHalfSize * 2f * bassShockwaveScale1
                                val left1 = midX - (size1 / 2f)
                                val top1 = midY - (size1 / 2f)
                                drawRoundRect(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF00FF88).copy(alpha = bassShockwaveAlpha1),
                                            Color(0xFF00E5FF).copy(alpha = bassShockwaveAlpha1),
                                            Color(0xFFFFD700).copy(alpha = bassShockwaveAlpha1),
                                            Color(0xFF00FF88).copy(alpha = bassShockwaveAlpha1)
                                        ),
                                        center = Offset(midX, midY)
                                    ),
                                    topLeft = Offset(left1, top1),
                                    size = androidx.compose.ui.geometry.Size(size1, size1),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(shockwaveCornerRadius * bassShockwaveScale1, shockwaveCornerRadius * bassShockwaveScale1),
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                // Shockwave 2 (Offset phase)
                                val size2 = baseCardHalfSize * 2f * bassShockwaveScale2
                                val left2 = midX - (size2 / 2f)
                                val top2 = midY - (size2 / 2f)
                                drawRoundRect(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF00E5FF).copy(alpha = bassShockwaveAlpha2),
                                            Color(0xFFE040FB).copy(alpha = bassShockwaveAlpha2),
                                            Color(0xFF00FF88).copy(alpha = bassShockwaveAlpha2),
                                            Color(0xFF00E5FF).copy(alpha = bassShockwaveAlpha2)
                                        ),
                                        center = Offset(midX, midY)
                                    ),
                                    topLeft = Offset(left2, top2),
                                    size = androidx.compose.ui.geometry.Size(size2, size2),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(shockwaveCornerRadius * bassShockwaveScale2, shockwaveCornerRadius * bassShockwaveScale2),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // 3. 24 Radial Kinetic Energy Particles bursting outwards
                                for (p in 0 until 24) {
                                    val pAngle = (p.toFloat() / 24f) * 2f * Math.PI.toFloat() + (cyberBeamRotation * 0.05f)
                                    val pDist = (baseCardHalfSize * 0.95f) + ((stardustPhase * 1.3f + p * 0.0416f) % 1f) * (w * 0.35f)
                                    val px = midX + cos(pAngle.toDouble()).toFloat() * pDist
                                    val py = midY + sin(pAngle.toDouble()).toFloat() * pDist
                                    val pProgress = ((stardustPhase * 1.3f + p * 0.0416f) % 1f)
                                    val pAlpha = (sin((pProgress * Math.PI).toDouble()).toFloat() * 0.85f).coerceIn(0f, 0.95f)

                                    val pCol = when (p % 3) {
                                        0 -> Color(0xFF00FF88)
                                        1 -> Color(0xFF00E5FF)
                                        else -> Color(0xFFFFD700)
                                    }

                                    drawCircle(
                                        color = pCol.copy(alpha = pAlpha),
                                        radius = (2.2f + (p % 3)).dp.toPx(),
                                        center = Offset(px, py)
                                    )
                                }
                            }

                            // Bass Titan Cyber Energy Poster Card
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(posterBreathScale)
                                    .clip(RoundedCornerShape(26.dp))
                                    .border(
                                        width = 2.5.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFFFD700), // Gold
                                                Color(0xFF00FF88), // Emerald
                                                Color(0xFF00E5FF), // Cyan
                                                Color(0xFF8A2BE2), // Violet
                                                Color(0xFFFFD700)  // Gold
                                            )
                                        ),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .shadow(32.dp, RoundedCornerShape(26.dp))
                            ) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // High-Tech Cyber Corner Energy Accents
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                Brush.linearGradient(listOf(Color(0x99FFFFFF), Color(0x22FFFFFF)))
                                            ),
                                            RoundedCornerShape(26.dp)
                                        )
                                )
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
                            imageVector = Icons.Default.PlaylistAdd,
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
