package com.example.vybe.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.vybe.VybeApplication
import com.example.vybe.core.designsystem.DarkBackground
import com.example.vybe.core.model.Track
import com.example.vybe.ui.components.AddToPlaylistSheet
import com.example.vybe.ui.components.FullPlayerModal
import com.example.vybe.ui.components.MiniPlayer
import com.example.vybe.ui.components.VybeBottomBar
import com.example.vybe.ui.screens.album.AlbumDetailScreen
import com.example.vybe.ui.screens.artist.ArtistDetailScreen
import com.example.vybe.ui.screens.details.TrackDetailsScreen
import com.example.vybe.ui.screens.downloads.DownloadManagerScreen
import com.example.vybe.ui.screens.history.RecentlyPlayedScreen
import com.example.vybe.ui.screens.home.HomeScreen
import com.example.vybe.ui.screens.home.HomeViewModel
import com.example.vybe.ui.screens.library.LibraryScreen
import com.example.vybe.ui.screens.library.LibraryViewModel
import com.example.vybe.ui.screens.liked.LikedSongsScreen
import com.example.vybe.ui.screens.playlist.PlaylistDetailScreen
import com.example.vybe.ui.screens.playlist.PlaylistViewModel
import com.example.vybe.ui.screens.queue.QueueScreen
import com.example.vybe.ui.screens.search.SearchScreen
import com.example.vybe.ui.screens.search.SearchViewModel
import com.example.vybe.ui.screens.settings.SettingsScreen
import com.example.vybe.ui.screens.settings.SettingsViewModel
import com.example.vybe.ui.screens.splash.SplashScreen
import com.example.vybe.ui.screens.welcome.WelcomeScreen
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.vybe.core.designsystem.DarkCard
import com.example.vybe.core.designsystem.TextMutedDark
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.vybe.core.designsystem.TextSecondaryDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.util.StorageUtils
import com.example.vybe.ui.screens.playlist.PlaylistsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Playlists : Screen("playlists")
    object Liked : Screen("liked")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    object ArtistDetail : Screen("artist/{artistId}") {
        fun createRoute(artistId: String) = "artist/$artistId"
    }
    object AlbumDetail : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }
    object Queue : Screen("queue")
    object History : Screen("history")
    object Downloads : Screen("downloads")
    object Settings : Screen("settings")
    object Equalizer : Screen("equalizer")
}

@Composable
fun VybeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val app = VybeApplication.instance
    val musicRepository = app.musicRepository
    val playbackManager = app.playbackManager
    val preferencesManager = app.preferencesManager
    val downloadRepository = app.downloadRepository

    val initialRoute = remember {
        if (preferencesManager.isFirstRunCompletedSync()) Screen.Home.route else Screen.Welcome.route
    }

    val playbackState by playbackManager.playbackState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: initialRoute

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isFirstRunCompleted by preferencesManager.isFirstRunCompleted.collectAsState(initial = preferencesManager.isFirstRunCompletedSync())
    val downloadTreeUri by preferencesManager.downloadTreeUri.collectAsState(initial = null)
    var isFullPlayerVisible by remember { mutableStateOf(false) }

    var pendingTrackToDownload by remember { mutableStateOf<Track?>(null) }
    var showFolderPromptDialog by remember { mutableStateOf(false) }
    var trackForPlaylistSheet by remember { mutableStateOf<Track?>(null) }

    val folderPickerForDownload = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            StorageUtils.takePersistableUriPermission(context, uri)
            val name = StorageUtils.getFriendlyFolderName(context, uri)
            coroutineScope.launch {
                preferencesManager.setDownloadFolderTree(uri.toString(), name)
                pendingTrackToDownload?.let { trk ->
                    downloadRepository.downloadTrack(trk)
                    pendingTrackToDownload = null
                }
            }
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Library.route,
        Screen.Playlists.route
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                VybeBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) paddingValues.calculateBottomPadding() else 0.dp)
        ) {
            NavHost(
                navController = navController,
                startDestination = initialRoute,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        isReady = isFirstRunCompleted != null,
                        onSplashFinished = {
                            val target = if (isFirstRunCompleted == true) Screen.Home.route else Screen.Welcome.route
                            navController.navigate(target) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onComplete = { languages, artists ->
                            CoroutineScope(Dispatchers.IO).launch {
                                preferencesManager.saveUserMusicPreferences(languages, artists)
                            }
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.provideFactory(musicRepository)
                    )
                    HomeScreen(
                        viewModel = homeViewModel,
                        playbackState = playbackState,
                        onTrackClick = { track, queue -> playbackManager.playTrack(track, queue) },
                        onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                        onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetail.createRoute(albumId)) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) },
                        onHistoryClick = { navController.navigate(Screen.History.route) },
                        onAddToPlaylist = { trackForPlaylistSheet = it },
                        onAddToQueue = { playbackManager.addToQueue(it) }
                    )
                }

                composable(Screen.Search.route) {
                    val searchViewModel: SearchViewModel = viewModel(
                        factory = SearchViewModel.provideFactory(musicRepository)
                    )
                    SearchScreen(
                        viewModel = searchViewModel,
                        playbackState = playbackState,
                        onTrackClick = { track, queue -> playbackManager.playTrack(track, queue) },
                        onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                        onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetail.createRoute(albumId)) }
                    )
                }

                composable(Screen.Library.route) {
                    val libraryViewModel: LibraryViewModel = viewModel(
                        factory = LibraryViewModel.provideFactory(musicRepository)
                    )
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        playbackState = playbackState,
                        onLikedSongsClick = { navController.navigate(Screen.Liked.route) },
                        onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                        onDownloadsClick = { navController.navigate(Screen.Downloads.route) }
                    )
                }

                composable(Screen.Playlists.route) {
                    PlaylistsScreen(
                        musicRepository = musicRepository,
                        playbackState = playbackState,
                        onPlaylistClick = { playlistId -> navController.navigate(Screen.PlaylistDetail.createRoute(playlistId)) }
                    )
                }

                composable(Screen.Liked.route) {
                    LikedSongsScreen(
                        musicRepository = musicRepository,
                        playbackState = playbackState,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track, queue -> playbackManager.playTrack(track, queue) }
                    )
                }

                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                    val playlistViewModel: PlaylistViewModel = viewModel(
                        factory = PlaylistViewModel.provideFactory(playlistId, musicRepository)
                    )
                    PlaylistDetailScreen(
                        viewModel = playlistViewModel,
                        playbackState = playbackState,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track, queue -> playbackManager.playTrack(track, queue) }
                    )
                }

                composable(
                    route = Screen.ArtistDetail.route,
                    arguments = listOf(navArgument("artistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                    ArtistDetailScreen(
                        artistId = artistId,
                        musicRepository = musicRepository,
                        playbackState = playbackState,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track, queue -> playbackManager.playTrack(track, queue) },
                        onAddToPlaylist = { trk -> trackForPlaylistSheet = trk },
                        onAddToQueue = { trk -> playbackManager.addToQueue(trk) }
                    )
                }

                composable(
                    route = Screen.AlbumDetail.route,
                    arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                    AlbumDetailScreen(
                        albumId = albumId,
                        musicRepository = musicRepository,
                        playbackState = playbackState,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track, queue -> playbackManager.playTrack(track, queue) }
                    )
                }

                composable(Screen.Queue.route) {
                    QueueScreen(
                        playbackState = playbackState,
                        playbackManager = playbackManager,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.History.route) {
                    RecentlyPlayedScreen(
                        musicRepository = musicRepository,
                        playbackState = playbackState,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track, queue -> playbackManager.playTrack(track, queue) }
                    )
                }

                composable(Screen.Downloads.route) {
                    DownloadManagerScreen(
                        downloadRepository = downloadRepository,
                        playbackState = playbackState,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track, queue -> playbackManager.playTrack(track, queue) }
                    )
                }

                composable(Screen.Settings.route) {
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.provideFactory(preferencesManager, musicRepository)
                    )
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBackClick = { navController.popBackStack() },
                        onEqualizerClick = { navController.navigate(Screen.Equalizer.route) }
                    )
                }

                composable(Screen.Equalizer.route) {
                    com.example.vybe.ui.screens.settings.EqualizerScreen(
                        equalizerManager = playbackManager.equalizerManager,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Persistent Mini Player immediately above bottom bar
            if (playbackState.currentTrack != null && currentRoute !in listOf(Screen.Splash.route, Screen.Welcome.route)) {
                MiniPlayer(
                    playbackState = playbackState,
                    onExpandClick = { isFullPlayerVisible = true },
                    onPlayPauseClick = { playbackManager.togglePlayPause() },
                    onNextClick = { playbackManager.skipToNext() },
                    onLikeClick = {
                        playbackState.currentTrack?.let { trk ->
                            CoroutineScope(Dispatchers.IO).launch { musicRepository.toggleLike(trk) }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // Full Player Modal
    if (isFullPlayerVisible && playbackState.currentTrack != null) {
        FullPlayerModal(
            playbackState = playbackState,
            onDismiss = { isFullPlayerVisible = false },
            onPlayPause = { playbackManager.togglePlayPause() },
            onNext = { playbackManager.skipToNext() },
            onPrevious = { playbackManager.skipToPrevious() },
            onSeek = { pos -> playbackManager.seekTo(pos) },
            onToggleShuffle = { playbackManager.toggleShuffle() },
            onToggleRepeat = { playbackManager.toggleRepeatMode() },
            onToggleLike = { trk ->
                CoroutineScope(Dispatchers.IO).launch { musicRepository.toggleLike(trk) }
            },
            onAddToPlaylist = { trk ->
                trackForPlaylistSheet = trk
            },
            onAddToQueue = { trk ->
                playbackManager.addToQueue(trk)
            },
            onOpenQueue = {
                isFullPlayerVisible = false
                navController.navigate(Screen.Queue.route)
            },
            onToggleAuraSpatialAudio = {
                playbackManager.toggleAuraSpatialAudio()
            },
            onDownloadTrack = { trk ->
                if (downloadTreeUri.isNullOrBlank()) {
                    pendingTrackToDownload = trk
                    showFolderPromptDialog = true
                } else {
                    coroutineScope.launch(Dispatchers.IO) {
                        downloadRepository.downloadTrack(trk)
                    }
                }
            },
            onSetSleepTimer = { mins ->
                if (mins > 0) playbackManager.sleepTimerManager.startTimer(mins)
                else playbackManager.sleepTimerManager.cancelTimer()
            }
        )
    }

    // Add to Playlist Sheet
    if (trackForPlaylistSheet != null) {
        AddToPlaylistSheet(
            track = trackForPlaylistSheet!!,
            musicRepository = musicRepository,
            onDismiss = { trackForPlaylistSheet = null }
        )
    }

    // Set Download Folder Prompt Dialog (When downloading without pre-set folder)
    if (showFolderPromptDialog) {
        AlertDialog(
            onDismissRequest = {
                showFolderPromptDialog = false
                pendingTrackToDownload = null
            },
            title = {
                Text(text = "Choose Download Folder", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    text = "VYBE needs a folder on your phone storage to save downloaded MP3 songs. Please select any folder in phone storage and tap 'USE THIS FOLDER'.",
                    color = TextSecondaryDark,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFolderPromptDialog = false
                        folderPickerForDownload.launch(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VybeEmerald)
                ) {
                    Text("Select Folder", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFolderPromptDialog = false
                    pendingTrackToDownload = null
                }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = DarkCard
        )
    }
}
