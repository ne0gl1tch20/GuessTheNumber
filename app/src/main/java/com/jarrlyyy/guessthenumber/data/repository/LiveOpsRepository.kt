package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Serializable
data class LiveOpsEvent(
    val id: String,
    val title: String,
    val description: String,
    val status: String, // UPCOMING, ACTIVE, ENDING, ARCHIVED
    val startDate: String,
    val endDate: String,
    val currencyName: String,
    val maxClaims: Int
)

@Serializable
data class LiveOpsManifest(
    val manifestVersion: Int,
    val events: List<LiveOpsEvent>
)

class LiveOpsRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadLiveOpsEvents(): LiveOpsManifest {
        // 1. Try fetching from GitHub raw content with robust fallback to bundled assets
        val githubUrl = "https://raw.githubusercontent.com/ne0gl1tch20/GuessTheNumber/main/app/src/main/assets/game/liveops_manifest.json"
        try {
            val remoteJson = withContext(Dispatchers.IO) {
                URL(githubUrl).readText(Charsets.UTF_8)
            }
            val parsed = json.decodeFromString<LiveOpsManifest>(remoteJson)
            if (parsed.events.isNotEmpty()) {
                GameLogger.log(LogLevel.INFO, LoggerCategory.SAVE, "LIVEOPS_SYNC", "Successfully synced LiveOps events from GitHub repo: ne0gl1tch20/GuessTheNumber")
                return parsed
            }
        } catch (e: Exception) {
            GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "LIVEOPS_FALLBACK", "Failed to fetch remote LiveOps from GitHub, using bundled asset: ${e.message}")
        }

        // 2. Bundled fallback
        return try {
            val inputStream = context.assets.open("game/liveops_manifest.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<LiveOpsManifest>(jsonString)
        } catch (e: Exception) {
            LiveOpsManifest(manifestVersion = 1, events = emptyList())
        }
    }
}
