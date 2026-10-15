package com.example.vybe

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.vybe.core.designsystem.VybeTheme
import com.example.vybe.core.model.ThemeMode
import com.example.vybe.ui.navigation.VybeNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.setBackgroundColor(0xFF0D0E12.toInt())
        window.statusBarColor = 0xFF0D0E12.toInt()
        window.navigationBarColor = 0xFF0D0E12.toInt()

        val preferencesManager = (application as VybeApplication).preferencesManager

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = ThemeMode.DARK)
            val isSystemDark = isSystemInDarkTheme()

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemDark
                else -> true // Dark by default for sleek Spotify-style experience
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            val requiredStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val storagePermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Handled gracefully */ }

            val requestRequiredPermissions: () -> Unit = {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        requiredStoragePermission
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        storagePermissionLauncher.launch(requiredStoragePermission)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 1. First time app open: Request Notification Permission FIRST on Android 13+
            // 2. Only after Notification permission is handled, request genuinely required media/storage permission
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) {
                requestRequiredPermissions()
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            requestRequiredPermissions()
                        }
                    } else {
                        requestRequiredPermissions()
                    }
                } else {
                    requestRequiredPermissions()
                }
            }

            VybeTheme(darkTheme = isDarkTheme) {
                var updateAvailableInfo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.vybe.core.update.AppUpdateInfo?>(null) }

                LaunchedEffect(Unit) {
                    val result = com.example.vybe.core.update.AppUpdateManager.checkForUpdates()
                    if (result is com.example.vybe.core.update.UpdateCheckResult.UpdateAvailable) {
                        updateAvailableInfo = result.info
                    }
                }

                if (updateAvailableInfo != null) {
                    com.example.vybe.ui.components.UpdateDialog(
                        updateInfo = updateAvailableInfo!!,
                        onDismiss = { updateAvailableInfo = null }
                    )
                }

                val navController = rememberNavController()
                VybeNavGraph(navController = navController)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (application as? VybeApplication)?.storageCleanupManager?.scheduleMaintenance()
    }
}
