package com.example.vybe.ui.screens.downloads

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vybe.core.designsystem.DarkBackground
import com.example.vybe.core.designsystem.DarkCard
import com.example.vybe.core.designsystem.TextMutedDark
import com.example.vybe.core.designsystem.TextSecondaryDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.designsystem.VybeEmeraldBright
import com.example.vybe.core.model.PlaybackState
import com.example.vybe.core.model.Track
import com.example.vybe.core.util.StorageUtils
import com.example.vybe.data.repository.DownloadRepository
import com.example.vybe.ui.components.EmptyStateView
import com.example.vybe.ui.components.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DownloadManagerScreen(
    downloadRepository: DownloadRepository,
    playbackState: PlaybackState,
    onBackClick: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadedTracks by downloadRepository.downloadedTracks.collectAsState(initial = emptyList())
    val downloadTreeUri by downloadRepository.preferencesManager.downloadTreeUri.collectAsState(initial = null)
    val downloadFolderName by downloadRepository.preferencesManager.downloadFolderName.collectAsState(initial = null)
    var storageUsed by remember { mutableStateOf("0 MB") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // SAF folder picker launcher ("USE THIS FOLDER")
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            StorageUtils.takePersistableUriPermission(context, uri)
            val friendlyName = StorageUtils.getFriendlyFolderName(context, uri)
            scope.launch {
                downloadRepository.preferencesManager.setDownloadFolderTree(uri.toString(), friendlyName)
            }
        }
    }

    LaunchedEffect(downloadedTracks) {
        storageUsed = downloadRepository.getStorageUsedFormatted()
    }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "Offline Downloads",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Storage Target Folder Card
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (downloadTreeUri.isNullOrBlank()) Color(0x33FFA000) else VybeEmerald.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (downloadTreeUri.isNullOrBlank()) Color(0x22FFA000) else VybeEmerald.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (downloadTreeUri.isNullOrBlank()) Icons.Default.CreateNewFolder else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (downloadTreeUri.isNullOrBlank()) Color(0xFFFFB300) else VybeEmeraldBright,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (downloadTreeUri.isNullOrBlank()) "No Download Folder Set" else "Folder: ${downloadFolderName ?: "Phone Storage"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (downloadTreeUri.isNullOrBlank())
                                    "Select any folder in phone storage to save offline songs"
                                else
                                    "Songs are saved directly as MP3 files here",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (downloadTreeUri.isNullOrBlank()) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = VybeEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Phone Storage Folder", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VybeEmerald),
                            border = BorderStroke(1.dp, VybeEmerald.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = VybeEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Download Folder", color = VybeEmerald, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Storage Gauge Card
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = VybeEmerald.copy(alpha = 0.2f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.SdCard, contentDescription = null, tint = VybeEmeraldBright, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Storage Used: $storageUsed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${downloadedTracks.size} offline tracks available without internet",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            if (downloadedTracks.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Download,
                    title = "No Offline Downloads",
                    subtitle = if (downloadTreeUri.isNullOrBlank())
                        "Set your storage folder above and tap download on any song to listen offline."
                    else
                        "Tap the download icon on any song to save it directly into '${downloadFolderName ?: "your folder"}' for offline listening."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(downloadedTracks) { track ->
                        TrackItem(
                            track = track,
                            isPlaying = playbackState.isPlaying,
                            isCurrent = playbackState.currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, downloadedTracks) },
                            onLikeClick = {},
                            onOptionsClick = {
                                scope.launch(Dispatchers.IO) {
                                    downloadRepository.deleteDownload(track.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
