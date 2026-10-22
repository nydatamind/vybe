package com.example.vybe.ui.screens.search

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vybe.core.designsystem.DarkBackground
import com.example.vybe.core.designsystem.DarkCard
import com.example.vybe.core.designsystem.TextMutedDark
import com.example.vybe.core.designsystem.TextSecondaryDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.designsystem.VybeEmeraldBright
import com.example.vybe.core.model.Album
import com.example.vybe.core.model.Artist
import com.example.vybe.core.model.PlaybackState
import com.example.vybe.core.model.Track
import com.example.vybe.ui.components.AlbumCard
import com.example.vybe.ui.components.ArtistCard
import com.example.vybe.ui.components.CategoryCard
import com.example.vybe.ui.components.EmptyStateView
import com.example.vybe.ui.components.SkeletonLoader
import com.example.vybe.ui.components.TrackItem

private fun isArtistQueryMatch(query: String, artistName: String): Boolean {
    val q = query.trim().lowercase()
    val a = artistName.trim().lowercase()
    if (q.isBlank() || a.isBlank()) return false

    // Strip generic music/song/genre keywords
    val cleanQ = q.replace(
        Regex("""\b(songs?|gana|geet|hits?|mp3|all|new|old|latest|remix|mashup|music|best|album|track|dj|bhojpuri|haryanvi|punjabi|bollywood|hindi|english)\b""", RegexOption.IGNORE_CASE),
        ""
    ).trim()

    // If query was just a genre/song keyword (e.g. "old haryanvi songs"), it is NOT an artist search!
    if (cleanQ.length < 3) return false

    // 1. Exact match with cleaned query or full query
    if (a == cleanQ || a == q) return true

    // 2. Artist name starts with query or query starts with artist name
    if (a.startsWith(cleanQ) || (cleanQ.length >= 4 && cleanQ.startsWith(a))) return true

    // 3. Multi-word artist match (e.g. "arijit singh", "masoom sharma", "kishore kumar")
    val qWords = cleanQ.split(Regex("""\s+""")).filter { it.length >= 2 }
    val aWords = a.split(Regex("""\s+""")).filter { it.length >= 2 }
    if (qWords.isNotEmpty() && qWords.all { qw -> aWords.any { aw -> aw.startsWith(qw) || qw.startsWith(aw) } }) {
        return true
    }

    return false
}

private val BrowseCategories = listOf(
    Pair("Lo-Fi & Chill", Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF8E24AA)))),
    Pair("Synthwave", Brush.linearGradient(listOf(Color(0xFF00695C), Color(0xFF00B0FF)))),
    Pair("EDM & Dance", Brush.linearGradient(listOf(Color(0xFFE65100), Color(0xFFFF9100)))),
    Pair("Cinematic", Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF3949AB)))),
    Pair("Acoustic", Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A)))),
    Pair("Electronic", Brush.linearGradient(listOf(Color(0xFF880E4F), Color(0xFFD81B60)))),
    Pair("Classical", Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF78909C)))),
    Pair("Pop", Brush.linearGradient(listOf(Color(0xFFBF360C), Color(0xFFFF7043))))
)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    playbackState: PlaybackState,
    onTrackClick: (Track, List<Track>) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            var showSongRecognition by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val keyboardController = LocalSoftwareKeyboardController.current

            // Voice Search launcher for directly speaking song or artist name
            val voiceSearchLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = matches?.firstOrNull()?.trim()
                    if (!spokenText.isNullOrBlank()) {
                        viewModel.onQueryChanged(spokenText)
                    }
                }
            }

            fun launchVoiceSearch() {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak song or artist name...")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US", "pa-IN"))
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }
                    voiceSearchLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Voice search is not supported on this device", Toast.LENGTH_SHORT).show()
                }
            }

            // Search Header Title with Song Voice / Lyrics Recognition Button on top-right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Song Voice / Audio Line Detection Button (Replaced the white mic at top-right corner)
                Surface(
                    shape = CircleShape,
                    color = DarkCard,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { showSongRecognition = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Detect Song by Voice / Lines",
                            tint = VybeEmeraldBright,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (showSongRecognition) {
                com.example.vybe.ui.components.SongRecognitionModal(
                    onDismiss = { showSongRecognition = false },
                    onTrackFound = { track ->
                        onTrackClick(track, listOf(track))
                    },
                    onQueryRecognized = { query ->
                        viewModel.onQueryChanged(query)
                    },
                    searchTracks = { query ->
                        viewModel.searchDirect(query).tracks
                    }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = {
                    Text(
                        text = "Search songs, artists, albums, genres...",
                        color = TextMutedDark,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (uiState.query.isNotBlank()) VybeEmeraldBright else TextMutedDark,
                        modifier = Modifier.clickable(enabled = uiState.query.isNotBlank()) {
                            keyboardController?.hide()
                            viewModel.triggerImmediateSearch()
                        }
                    )
                },
                trailingIcon = {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(
                            color = VybeEmerald,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondaryDark
                            )
                        }
                    } else {
                        IconButton(onClick = { launchVoiceSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Search",
                                tint = VybeEmeraldBright
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VybeEmerald,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        viewModel.triggerImmediateSearch()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Content: Categories / Recent Searches OR Results
            if (uiState.query.isBlank()) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Recent Searches
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                TextButton(onClick = { viewModel.clearAllRecentSearches() }) {
                                    Text("Clear", color = TextSecondaryDark, fontSize = 12.sp)
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(recentSearches.take(6)) { term ->
                                    Surface(
                                        color = DarkCard,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.clickable { viewModel.onQueryChanged(term) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = TextSecondaryDark,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = term,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    // Browse Categories
                    item {
                        Text(
                            text = "Browse All Genres",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BrowseCategories.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    row.forEach { (cat, grad) ->
                                        CategoryCard(
                                            title = cat,
                                            gradient = grad,
                                            onClick = { viewModel.searchCategory(cat) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Filter Tabs (All, Songs, Artists, Albums)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFilter.values().forEach { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = filter.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VybeEmerald,
                                containerColor = DarkCard
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Results list
                val res = uiState.searchResult
                val rawQuery = uiState.query.trim()
                val topArtist = res.artists.firstOrNull { isArtistQueryMatch(rawQuery, it.name) }
                    ?: res.tracks.firstOrNull()?.let { track ->
                        if (isArtistQueryMatch(rawQuery, track.artistName)) {
                            Artist(
                                id = track.artistId,
                                name = track.artistName,
                                artworkUrl = track.artworkUrl,
                                bio = "Artist",
                                genres = listOf(track.genre),
                                monthlyListeners = 1500000L
                            )
                        } else null
                    }

                if (!uiState.isSearching && uiState.hasCompletedSearch && res.isEmpty) {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "No results found",
                        subtitle = "We couldn't find matches for \"${uiState.query}\". Try checking the spelling or search another artist/genre."
                    )
                } else if (uiState.isSearching && res.isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        CircularProgressIndicator(
                            color = VybeEmerald,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Top Artist Card (Shown first when ALL filter is active and artist is matched)
                        if (uiState.selectedFilter == SearchFilter.ALL && topArtist != null) {
                            item {
                                Text(
                                    text = "Top Result",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                                Surface(
                                    color = Color(0xFF141824),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                VybeEmerald.copy(alpha = 0.5f),
                                                Color(0x22FFFFFF)
                                            )
                                        )
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { onArtistClick(topArtist.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(68.dp)) {
                                            AsyncImage(
                                                model = topArtist.artworkUrl.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1080" },
                                                contentDescription = topArtist.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, VybeEmerald, CircleShape)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Artist",
                                                    color = VybeEmeraldBright,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Verified",
                                                    tint = VybeEmeraldBright,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = topArtist.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Tap to view all songs & albums",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondaryDark
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "View Artist",
                                            tint = TextSecondaryDark,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        // Songs Section
                        if (uiState.selectedFilter == SearchFilter.ALL || uiState.selectedFilter == SearchFilter.SONGS) {
                            if (res.tracks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Songs",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                }
                                items(res.tracks) { track ->
                                    TrackItem(
                                        track = track,
                                        isPlaying = playbackState.isPlaying,
                                        isCurrent = playbackState.currentTrack?.id == track.id,
                                        onTrackClick = { onTrackClick(track, listOf(track)) },
                                        onLikeClick = { viewModel.toggleLike(track) },
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }

                        // Artists Section (If ARTISTS filter selected or more artists available)
                        if (uiState.selectedFilter == SearchFilter.ARTISTS) {
                            if (res.artists.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Artists",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                }
                                items(res.artists) { artist ->
                                    Surface(
                                        color = DarkCard,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onArtistClick(artist.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = artist.artworkUrl.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1080" },
                                                contentDescription = artist.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, VybeEmerald, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = artist.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Artist",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondaryDark
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = TextSecondaryDark
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (uiState.selectedFilter == SearchFilter.ALL && res.artists.size > 1) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "More Artists",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(res.artists.drop(1)) { artist ->
                                        ArtistCard(
                                            artist = artist,
                                            onClick = { onArtistClick(artist.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Albums Section
                        if (uiState.selectedFilter == SearchFilter.ALL || uiState.selectedFilter == SearchFilter.ALBUMS) {
                            if (res.albums.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Albums",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(res.albums) { album ->
                                            AlbumCard(
                                                album = album,
                                                onClick = { onAlbumClick(album.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
