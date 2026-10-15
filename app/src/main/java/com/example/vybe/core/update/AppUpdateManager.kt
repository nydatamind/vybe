package com.example.vybe.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseName: String,
    val changelog: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val publishedAt: String = ""
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult
    object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object AppUpdateManager {

    const val CURRENT_VERSION_NAME = "1.0.0"
    const val GITHUB_OWNER = "nydatamind"
    const val GITHUB_REPO = "vybe"

    const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val FALLBACK_DIRECT_DOWNLOAD_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest/download/vybe.apk"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Checks GitHub Releases API for a newer version than CURRENT_VERSION_NAME (1.0.0).
     */
    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "VYBE-Music-Android/$CURRENT_VERSION_NAME")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    // No releases published yet on this repository
                    return@withContext UpdateCheckResult.UpToDate
                }
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("Server returned code ${response.code}")
                }

                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    return@withContext UpdateCheckResult.Error("Empty response from GitHub")
                }

                val json = JSONObject(body)
                val rawTag = json.optString("tag_name", "").trim()
                val cleanTag = rawTag.removePrefix("v").removePrefix("V").trim()
                val releaseName = json.optString("name", "New Update v$cleanTag").trim()
                val changelog = json.optString("body", "Bug fixes and performance improvements.").trim()
                val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases")

                // Look for attached .apk asset
                var downloadUrl = FALLBACK_DIRECT_DOWNLOAD_URL
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", downloadUrl)
                            break
                        }
                    }
                }

                if (isVersionNewer(cleanTag, CURRENT_VERSION_NAME)) {
                    val updateInfo = AppUpdateInfo(
                        latestVersion = cleanTag,
                        releaseName = releaseName.ifBlank { "Version $cleanTag" },
                        changelog = changelog,
                        downloadUrl = downloadUrl,
                        releaseUrl = htmlUrl
                    )
                    UpdateCheckResult.UpdateAvailable(updateInfo)
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateCheckResult.Error(e.localizedMessage ?: "Failed to connect to GitHub")
        }
    }

    /**
     * Compares two semantic version strings (e.g. "1.1.0" > "1.0.0").
     */
    fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        if (remoteVersion.isBlank()) return false
        val remoteParts = remoteVersion.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }
        val currentParts = currentVersion.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val remote = remoteParts.getOrElse(i) { 0 }
            val current = currentParts.getOrElse(i) { 0 }
            if (remote > current) return true
            if (remote < current) return false
        }
        return false
    }

    /**
     * Launches browser or download manager directly to fetch the new APK.
     */
    fun openUpdateDownload(context: Context, downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
