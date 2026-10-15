package com.example.vybe.core.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * High-Performance Dynamic Remote Config & Auto-Failover Engine for VYBE.
 *
 * Responsibilities:
 * - Automatically fetches updated API endpoints, mirror servers, and CDN configurations in the background.
 * - Zero APK update required when backend servers or API endpoints change.
 * - Instant 0ms cached startup from local storage.
 * - Dynamic runtime failover across multiple backup mirrors if a server experiences downtime or format change.
 */
object RemoteConfigManager {

    private const val PREFS_NAME = "vybe_remote_config_prefs"
    private const val KEY_CONFIG_JSON = "cached_config_json"
    private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"

    // Default built-in endpoints (Used immediately on 0ms startup)
    const val DEFAULT_PRIMARY_BASE_URL = "https://www.jiosaavn.com/api.php"
    val DEFAULT_BACKUP_BASE_URLS = listOf(
        "https://www.jiosaavn.com/api.php",
        "https://saavn.me/api",
        "https://jiosaavn-api-private.vercel.app"
    )
    const val DEFAULT_DES_KEY = "38346591"

    // Public Remote Config Source URLs (Free GitHub raw & jsdelivr CDN fast mirrors)
    private val REMOTE_CONFIG_ENDPOINTS = listOf(
        "https://raw.githubusercontent.com/nitinyadav-dev/vybe-config/main/config.json",
        "https://cdn.jsdelivr.net/gh/nitinyadav-dev/vybe-config@main/config.json"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private var sharedPrefs: SharedPreferences? = null

    // Thread-safe active configuration references
    private val activeBaseUrlRef = AtomicReference(DEFAULT_PRIMARY_BASE_URL)
    private val backupBaseUrlsRef = AtomicReference(DEFAULT_BACKUP_BASE_URLS)
    private val desKeyRef = AtomicReference(DEFAULT_DES_KEY)
    private val cdnQualitiesRef = AtomicReference(listOf("_320.mp4", "_160.mp4", "_96.mp4", "_320.aac", "_160.aac"))

    fun init(context: Context) {
        if (sharedPrefs == null) {
            sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadCachedConfig()
        }
    }

    fun getActiveBaseUrl(): String = activeBaseUrlRef.get()

    fun getBackupBaseUrls(): List<String> = backupBaseUrlsRef.get()

    fun getDesKey(): String = desKeyRef.get()

    fun getCdnQualities(): List<String> = cdnQualitiesRef.get()

    /**
     * Reports endpoint failure so the engine seamlessly falls back to the next available mirror.
     */
    fun reportEndpointFailure(failedUrl: String) {
        val backups = backupBaseUrlsRef.get()
        if (backups.isEmpty()) return

        val currentActive = activeBaseUrlRef.get()
        val currentIndex = backups.indexOfFirst { currentActive.startsWith(it) }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % backups.size else 0
        val newActive = backups[nextIndex]

        if (newActive != currentActive) {
            activeBaseUrlRef.set(newActive)
            android.util.Log.w("RemoteConfig", "Switched active API endpoint from $currentActive to $newActive")
        }
    }

    /**
     * Confirms that an endpoint succeeded.
     */
    fun reportEndpointSuccess(successfulUrl: String) {
        val backups = backupBaseUrlsRef.get()
        val matchingBackup = backups.firstOrNull { successfulUrl.startsWith(it) }
        if (matchingBackup != null && matchingBackup != activeBaseUrlRef.get()) {
            activeBaseUrlRef.set(matchingBackup)
        }
    }

    /**
     * Synchronizes the latest dynamic configuration in background (non-blocking).
     */
    fun syncRemoteConfigAsync(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            syncRemoteConfigInternal()
        }
    }

    private suspend fun syncRemoteConfigInternal() = withContext(Dispatchers.IO) {
        for (url in REMOTE_CONFIG_ENDPOINTS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "VYBE-RemoteConfig/1.0")
                    .header("Cache-Control", "no-cache")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrBlank() && applyConfigJson(bodyString)) {
                            saveCachedConfig(bodyString)
                            return@withContext
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next mirror silently
            }
        }
    }

    private fun applyConfigJson(jsonString: String): Boolean {
        return try {
            val element = jsonParser.parseToJsonElement(jsonString)
            if (element is JsonObject) {
                val primaryUrl = element["primary_base_url"]?.jsonPrimitive?.contentOrNull
                if (!primaryUrl.isNullOrBlank()) {
                    activeBaseUrlRef.set(primaryUrl.trim())
                }

                val backupArray = element["backup_base_urls"]?.jsonArray
                if (backupArray != null && backupArray.isNotEmpty()) {
                    val backups = backupArray.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() }.filter { it.isNotBlank() }
                    if (backups.isNotEmpty()) {
                        backupBaseUrlsRef.set(backups)
                    }
                }

                val key = element["des_key"]?.jsonPrimitive?.contentOrNull
                if (!key.isNullOrBlank()) {
                    desKeyRef.set(key.trim())
                }

                val qualities = element["cdn_qualities"]?.jsonArray
                if (qualities != null && qualities.isNotEmpty()) {
                    val qList = qualities.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() }.filter { it.isNotBlank() }
                    if (qList.isNotEmpty()) {
                        cdnQualitiesRef.set(qList)
                    }
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun loadCachedConfig() {
        val cachedJson = sharedPrefs?.getString(KEY_CONFIG_JSON, null)
        if (!cachedJson.isNullOrBlank()) {
            applyConfigJson(cachedJson)
        }
    }

    private fun saveCachedConfig(jsonString: String) {
        sharedPrefs?.edit()
            ?.putString(KEY_CONFIG_JSON, jsonString)
            ?.putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
            ?.apply()
    }
}
