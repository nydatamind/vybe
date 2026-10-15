package com.example.vybe.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.vybe.core.designsystem.DarkCard
import com.example.vybe.core.designsystem.DarkSurface
import com.example.vybe.core.designsystem.DarkSurfaceElevated
import com.example.vybe.core.designsystem.TextMutedDark
import com.example.vybe.core.designsystem.TextSecondaryDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.designsystem.VybeEmeraldBright
import com.example.vybe.core.designsystem.VybeGradientEmerald
import com.example.vybe.core.model.Track
import kotlinx.coroutines.launch

@Composable
fun SongRecognitionModal(
    onDismiss: () -> Unit,
    onTrackFound: (Track) -> Unit,
    onQueryRecognized: ((String) -> Unit)? = null,
    searchTracks: suspend (String) -> List<Track>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isListening by remember { mutableStateOf(false) }
    var isSearchingSong by remember { mutableStateOf(false) }
    var recognizedQuery by remember { mutableStateOf("") }
    var recognizedTrack by remember { mutableStateOf<Track?>(null) }
    var otherMatches by remember { mutableStateOf<List<Track>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("Ready to listen") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Launcher for Audio Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            errorMessage = "Microphone permission is required to identify songs."
        }
    }

    // Function to search music catalog using recognized text
    fun searchWithText(rawQuery: String) {
        val cleanQuery = rawQuery.trim()
        if (cleanQuery.isBlank()) return

        recognizedQuery = cleanQuery
        isListening = false
        isSearchingSong = true
        statusMessage = "Searching \"$cleanQuery\"..."
        errorMessage = null

        coroutineScope.launch {
            try {
                var tracks = searchTracks(cleanQuery)
                if (tracks.isEmpty() && cleanQuery.contains(" ")) {
                    // Try first 3 words if full phrase returned empty
                    val shorter = cleanQuery.split(" ").take(3).joinToString(" ")
                    tracks = searchTracks(shorter)
                }

                if (tracks.isNotEmpty()) {
                    recognizedTrack = tracks.first()
                    otherMatches = tracks.drop(1).take(3)
                    statusMessage = "Song identified!"
                } else {
                    errorMessage = "Couldn't find an exact song match for \"$cleanQuery\"."
                }
            } catch (e: Exception) {
                errorMessage = "Search failed: ${e.localizedMessage}"
            } finally {
                isSearchingSong = false
            }
        }
    }

    // Google Voice Search Intent Launcher (reliable fallback)
    val voiceSearchIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val query = matches?.firstOrNull()?.trim()
            if (!query.isNullOrBlank()) {
                searchWithText(query)
            }
        }
    }

    // Function to start listening via SpeechRecognizer
    fun startListening() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        errorMessage = null
        recognizedTrack = null
        otherMatches = emptyList()
        recognizedQuery = ""
        isSearchingSong = false
        rmsLevel = 0f

        try {
            speechRecognizer?.destroy()
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer

                val listener = object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        statusMessage = "Listening to music, singing or humming..."
                    }

                    override fun onBeginningOfSpeech() {
                        statusMessage = "Sound detected! Listening..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        rmsLevel = (rmsdB.coerceIn(0f, 10f) / 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        statusMessage = "Processing audio..."
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorText = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check mic."
                            SpeechRecognizer.ERROR_CLIENT -> "Client error. Please try again."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection issue."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No audible lyrics or music detected. Please hold closer to speaker."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No sound detected. Tap to try again."
                            else -> "Could not capture audio clearly."
                        }
                        errorMessage = errorText
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val topMatch = matches?.firstOrNull()?.trim()
                        if (!topMatch.isNullOrBlank()) {
                            searchWithText(topMatch)
                        } else {
                            errorMessage = "No recognizable song or lyrics caught. Please try again."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            recognizedQuery = partial
                            statusMessage = "Heard: \"$partial\""
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }

                recognizer.setRecognitionListener(listener)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                }

                recognizer.startListening(intent)
            } else {
                // SpeechRecognizer not available directly, launch Google Voice Intent
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Sing, hum or play a song")
                }
                voiceSearchIntentLauncher.launch(intent)
            }
        } catch (e: Exception) {
            isListening = false
            errorMessage = "Could not initialize listening: ${e.localizedMessage}"
        }
    }

    // Auto-start listening on launch if permission granted
    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission && recognizedTrack == null && !isListening && errorMessage == null) {
            startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    Dialog(onDismissRequest = {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        onDismiss()
    }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(VybeEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = VybeEmeraldBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Song Finder",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    IconButton(onClick = {
                        try {
                            speechRecognizer?.stopListening()
                            speechRecognizer?.destroy()
                        } catch (_: Exception) {}
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Permission State
                if (!hasAudioPermission) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222636)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = VybeEmeraldBright,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Microphone Access Required",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "VYBE needs microphone permission to identify songs playing around you, hums, or lyrics.",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = VybeEmerald),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                // Actively Listening or Searching State
                else if (isListening || isSearchingSong) {
                    val transition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    val animatedRms by animateFloatAsState(
                        targetValue = rmsLevel,
                        animationSpec = tween(120),
                        label = "rms"
                    )

                    Box(contentAlignment = Alignment.Center) {
                        // Outer Pulsing Ring
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .scale(pulseScale + (animatedRms * 0.25f))
                                .clip(CircleShape)
                                .background(VybeEmeraldBright.copy(alpha = 0.15f))
                        )
                        // Middle Ring
                        Box(
                            modifier = Modifier
                                .size(95.dp)
                                .clip(CircleShape)
                                .background(VybeEmerald.copy(alpha = 0.35f))
                        )
                        // Center Button / Visualizer
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF0096C7), VybeEmeraldBright))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSearchingSong) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isSearchingSong) "Matching with music catalog..." else "Listening to music...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (recognizedQuery.isNotBlank()) "Heard: \"$recognizedQuery\"" else "Play the song near phone or sing the lyrics",
                        color = if (recognizedQuery.isNotBlank()) VybeEmeraldBright else TextSecondaryDark,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Cancel / Stop Button
                    OutlinedButton(
                        onClick = {
                            try {
                                speechRecognizer?.stopListening()
                            } catch (_: Exception) {}
                            isListening = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark)
                    ) {
                        Text("Stop", fontSize = 13.sp)
                    }
                }
                // Track Found State!
                else if (recognizedTrack != null) {
                    val track = recognizedTrack!!

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track.artworkUrl.isNotBlank()) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(100.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = VybeEmeraldBright,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VybeEmeraldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Song Identified! 🎉",
                            color = VybeEmeraldBright,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = track.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = track.artistName,
                        color = TextSecondaryDark,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (recognizedQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Detected audio: \"$recognizedQuery\"",
                            color = TextMutedDark,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Play Song Button
                    Button(
                        onClick = {
                            onTrackFound(track)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VybeEmerald),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Song Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // View in Search or Listen Again
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val q = if (recognizedQuery.isNotBlank()) recognizedQuery else track.title
                                onQueryRecognized?.invoke(q)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All Results", fontSize = 12.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { startListening() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Listen Again", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
                // Error / Retry State
                else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Couldn't Catch That Song",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = errorMessage ?: "Hold your phone close to the music source or sing/hum clearly.",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { startListening() },
                        colors = ButtonDefaults.buttonColors(containerColor = VybeEmerald),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tap to Listen Again", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (recognizedQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onQueryRecognized?.invoke(recognizedQuery)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VybeEmeraldBright),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search \"$recognizedQuery\" in Vibra", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Sing or speak song lyrics")
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US", "pa-IN"))
                            }
                            voiceSearchIntentLauncher.launch(intent)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Try Voice Search", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
