package com.example.vybe.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.vybe.core.util.StorageUtils
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vybe.core.designsystem.DarkBackground
import com.example.vybe.core.designsystem.DarkCard
import com.example.vybe.core.designsystem.TextMutedDark
import com.example.vybe.core.designsystem.TextSecondaryDark
import com.example.vybe.core.designsystem.VybeEmerald
import com.example.vybe.core.designsystem.VybeEmeraldBright
import com.example.vybe.core.designsystem.VybePurple
import com.example.vybe.core.designsystem.vybeNeonGlow
import com.example.vybe.core.model.AudioQuality
import com.example.vybe.core.model.ThemeMode
import com.example.vybe.ui.components.DeveloperAttributionBadge

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onEqualizerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()
    val streamWifiOnly by viewModel.isStreamWifiOnly.collectAsState()
    val auraSpatialAudio by viewModel.isAuraSpatialAudio.collectAsState()
    val autoSkipIntroOutro by viewModel.isAutoSkipIntroOutro.collectAsState()
    val downloadFolderName by viewModel.downloadFolderName.collectAsState()
    val downloadTreeUri by viewModel.downloadTreeUri.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            StorageUtils.takePersistableUriPermission(context, uri)
            val name = StorageUtils.getFriendlyFolderName(context, uri)
            viewModel.setDownloadFolderTree(uri.toString(), name)
        }
    }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearSearchConfirmation by remember { mutableStateOf(false) }
    var showClearListeningConfirmation by remember { mutableStateOf(false) }
    var showHistoryClearedToast by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateAvailableInfo by remember { mutableStateOf<com.example.vybe.core.update.AppUpdateInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
            // Header
            item {
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
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // PROMINENT DEVELOPER ATTRIBUTION CARD
            item {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0C2A38), Color(0xFF162238), Color(0xFF1E1338))
                            )
                        )
                        .border(1.dp, VybeEmerald.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = VybeEmerald,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .vybeNeonGlow(color = VybeEmerald, radius = 12.dp, alpha = 0.5f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "CRAFTED BY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VybeEmeraldBright,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    )
                                    Text(
                                        text = "Nitin Yadav",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Crafting the future of music streaming.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF00FF88)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "A pure music experience made for those who live, feel, and connect through sound. Discover more, listen freely, and make every moment yours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Audio & Playback Group
            item {
                SettingsSectionTitle("Audio & Playback")
                SettingsClickableRow(
                    icon = Icons.Default.GraphicEq,
                    title = "Audio Equalizer & Bass Boost",
                    subtitle = "5-band EQ, Bass Boost & presets",
                    onClick = onEqualizerClick
                )
                SettingsToggleRow(
                    icon = Icons.Default.SurroundSound,
                    title = "Aura 8D Spatial Audio",
                    subtitle = "Auto-deep bass & 8D holographic acoustic immersion",
                    checked = auraSpatialAudio,
                    onCheckedChange = { viewModel.setAuraSpatialAudio(it) }
                )
                SettingsToggleRow(
                    icon = Icons.Default.FastForward,
                    title = "Auto Skip Intro & Outro",
                    subtitle = "Automatically skip first 7s intro & last 7s outro",
                    checked = autoSkipIntroOutro,
                    onCheckedChange = { viewModel.setAutoSkipIntroOutro(it) }
                )
                SettingsClickableRow(
                    icon = Icons.Default.HighQuality,
                    title = "Audio Quality",
                    subtitle = "${audioQuality.title} (${audioQuality.bitrate})",
                    onClick = { showQualityDialog = true }
                )
                SettingsToggleRow(
                    icon = Icons.Default.Wifi,
                    title = "Stream on Wi-Fi Only",
                    subtitle = "Save mobile cellular data when streaming",
                    checked = streamWifiOnly,
                    onCheckedChange = { viewModel.setStreamWifiOnly(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Appearance Group
            item {
                SettingsSectionTitle("Appearance")
                SettingsClickableRow(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = themeMode.name.lowercase().capitalize(),
                    onClick = { showThemeDialog = true }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Downloads & Storage Group
            item {
                SettingsSectionTitle("Downloads & Storage")
                SettingsClickableRow(
                    icon = Icons.Default.Folder,
                    title = "Download Storage Folder",
                    subtitle = if (downloadFolderName.isNullOrBlank())
                        "Not configured (Tap to select folder in phone storage)"
                    else
                        "Saved to: $downloadFolderName (Tap to change)",
                    onClick = { folderPickerLauncher.launch(null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Data & Privacy Group
            item {
                SettingsSectionTitle("Data & Privacy")
                SettingsClickableRow(
                    icon = Icons.Default.Delete,
                    title = "Clear Search History",
                    subtitle = "Remove past search queries",
                    onClick = {
                        showClearSearchConfirmation = true
                    }
                )
                SettingsClickableRow(
                    icon = Icons.Default.Delete,
                    title = "Clear Listening History",
                    subtitle = "Reset recently played timeline",
                    onClick = {
                        showClearListeningConfirmation = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Legal & Open Source
            item {
                SettingsSectionTitle("About & Updates")
                SettingsClickableRow(
                    icon = Icons.Default.SystemUpdate,
                    title = "Check for Updates",
                    subtitle = if (isCheckingUpdates) "Checking GitHub for new releases..." else "Version 1.0.0 • Tap to check updates",
                    onClick = {
                        if (!isCheckingUpdates) {
                            isCheckingUpdates = true
                            coroutineScope.launch {
                                when (val result = com.example.vybe.core.update.AppUpdateManager.checkForUpdates()) {
                                    is com.example.vybe.core.update.UpdateCheckResult.UpdateAvailable -> {
                                        updateAvailableInfo = result.info
                                    }
                                    is com.example.vybe.core.update.UpdateCheckResult.UpToDate -> {
                                        android.widget.Toast.makeText(context, "🎉 VYBE is up to date (v1.0.0)!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    is com.example.vybe.core.update.UpdateCheckResult.Error -> {
                                        android.widget.Toast.makeText(context, "Could not check updates: ${result.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isCheckingUpdates = false
                            }
                        }
                    }
                )
                SettingsClickableRow(
                    icon = Icons.Default.Info,
                    title = "Licensing & Audio Sources",
                    subtitle = "Creative Commons, Public Domain & Jamendo open streams",
                    onClick = {}
                )
            }
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = VybeEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.name.lowercase().capitalize(), color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = DarkCard
        )
    }

    // Audio Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Streaming Audio Quality", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AudioQuality.values().forEach { quality ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAudioQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = audioQuality == quality,
                                onClick = {
                                    viewModel.setAudioQuality(quality)
                                    showQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = VybeEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = quality.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(text = quality.bitrate, color = TextSecondaryDark, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = DarkCard
        )
    }

    // Clear Search History Confirmation Dialog
    if (showClearSearchConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearSearchConfirmation = false },
            title = {
                Text(
                    text = "Clear Search History?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear your recent searches? This action cannot be undone.",
                    color = TextSecondaryDark,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearSearchHistory()
                        showClearSearchConfirmation = false
                        showHistoryClearedToast = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchConfirmation = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Clear Listening History Confirmation Dialog
    if (showClearListeningConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearListeningConfirmation = false },
            title = {
                Text(
                    text = "Clear Listening History?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to reset your recently played tracks? This action cannot be undone.",
                    color = TextSecondaryDark,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearListeningHistory()
                        showClearListeningConfirmation = false
                        showHistoryClearedToast = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearListeningConfirmation = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (updateAvailableInfo != null) {
        com.example.vybe.ui.components.UpdateDialog(
            updateInfo = updateAvailableInfo!!,
            onDismiss = { updateAvailableInfo = null }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = VybeEmeraldBright,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = VybeEmerald,
                uncheckedTrackColor = DarkCard
            )
        )
    }
}
