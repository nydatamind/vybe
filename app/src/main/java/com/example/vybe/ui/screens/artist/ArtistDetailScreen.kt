package com.example.vybe.ui.screens.artist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.vybe.core.designsystem.DarkBackground
import com.example.vybe.core.designsystem.DarkCard
import com.example.vybe.core.designsystem.TextMutedDark
import com.example.vybe.core.designsystem.TextSecondaryDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.designsystem.VybeEmeraldBright
import com.example.vybe.core.model.Artist
import com.example.vybe.core.model.PlaybackState
import com.example.vybe.core.model.Track
import com.example.vybe.data.repository.MusicRepository
import com.example.vybe.ui.components.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ArtistDetailScreen(
    artistId: String,
    musicRepository: MusicRepository,
    playbackState: PlaybackState,
    onBackClick: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (Track) -> Unit = {},
    onAddToQueue: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var artist by remember { mutableStateOf<Artist?>(null) }
    var artistTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val isSaved by musicRepository.isArtistSavedFlow(artistId).collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(artistId) {
        isLoading = true
        isLoadingMore = true
        artist = musicRepository.getArtistDetails(artistId)
        val fullList = musicRepository.getTracksForArtist(artistId) { progressiveList ->
            artistTracks = progressiveList
            isLoading = false
        }
        artistTracks = fullList
        isLoading = false
        isLoadingMore = false
    }

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Image & Info
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    AsyncImage(
                        model = artist?.artworkUrl ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1080",
                        contentDescription = artist?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x55000000), Color(0xDD0D0E12), DarkBackground)
                                )
                            )
                    )

                    // Top Back Button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x66000000),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    }

                    // Artist Name & Badges
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Artist",
                                tint = VybeEmeraldBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Verified Artist",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = artist?.name ?: "Artist",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "${"%,d".format(artist?.monthlyListeners ?: 340000)} monthly listeners",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            // Play, Shuffle & Follow Action Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (artistTracks.isNotEmpty()) {
                        Button(
                            onClick = { onTrackClick(artistTracks.first(), artistTracks) },
                            colors = ButtonDefaults.buttonColors(containerColor = VybeEmerald),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val shuffled = artistTracks.shuffled()
                                onTrackClick(shuffled.first(), shuffled)
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Follow / Save Artist Button
                    Button(
                        onClick = {
                            val art = artist ?: Artist(
                                id = artistId,
                                name = artistTracks.firstOrNull()?.artistName ?: "Artist",
                                artworkUrl = artistTracks.firstOrNull()?.artworkUrl ?: ""
                            )
                            coroutineScope.launch {
                                musicRepository.toggleSaveArtist(art)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSaved) Color(0x33FFFFFF) else VybeEmerald.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, if (isSaved) Color(0x66FFFFFF) else VybeEmerald),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isSaved) Color.White else VybeEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSaved) "Following" else "Follow",
                            color = if (isSaved) Color.White else VybeEmeraldBright,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // All Songs Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Songs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (artistTracks.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    color = VybeEmeraldBright,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "${artistTracks.size} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }

            // Loading state
            if (isLoading && artistTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VybeEmerald)
                    }
                }
            } else if (artistTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No songs found for this artist.",
                            color = TextSecondaryDark,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(artistTracks, key = { it.id }) { track ->
                    TrackItem(
                        track = track,
                        isPlaying = playbackState.isPlaying,
                        isCurrent = playbackState.currentTrack?.id == track.id,
                        onTrackClick = { onTrackClick(track, artistTracks) },
                        onLikeClick = {
                            coroutineScope.launch {
                                musicRepository.toggleLike(track)
                            }
                        },
                        onOptionsClick = { onAddToPlaylist(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                if (isLoadingMore) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = VybeEmeraldBright,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading all songs from catalog...",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // About Card
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = artist?.bio ?: "Independent music artist producing high-quality open music under Creative Commons licenses.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Genres: ${artist?.genres?.joinToString(", ") ?: "Electronic, Lo-Fi"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = VybeEmeraldBright
                            )
                        }
                    }
                }
            }
        }
    }
}
