package com.example.vybe.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vybe.core.designsystem.DarkBackground
import com.example.vybe.core.designsystem.DarkCard
import com.example.vybe.core.designsystem.TextMutedDark
import com.example.vybe.core.designsystem.TextSecondaryDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.model.PlaybackState
import com.example.vybe.core.model.Track
import com.example.vybe.data.repository.MusicRepository
import com.example.vybe.ui.components.EmptyStateView
import com.example.vybe.ui.components.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RecentlyPlayedScreen(
    musicRepository: MusicRepository,
    playbackState: PlaybackState,
    onBackClick: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    modifier: Modifier = Modifier
) {
    val historyTracks by musicRepository.recentlyPlayed.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "Listening History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (historyTracks.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = TextSecondaryDark)
                    }
                } else {
                    Spacer(modifier = Modifier.padding(24.dp))
                }
            }

            if (historyTracks.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.History,
                    title = "No History Yet",
                    subtitle = "Music you stream will appear here in chronological order."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(historyTracks, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            isPlaying = playbackState.isPlaying,
                            isCurrent = playbackState.currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, historyTracks) },
                            onLikeClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    musicRepository.toggleLike(track)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will clear all your recently played songs record.", color = TextSecondaryDark) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        CoroutineScope(Dispatchers.IO).launch { musicRepository.clearHistory() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = DarkCard
        )
    }
}
