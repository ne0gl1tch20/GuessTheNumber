package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

    suspend fun loadLiveOpsEvents(): LiveOpsManifest = withContext(Dispatchers.IO) {
        // Live Ops remains fully usable offline and does not fetch remote data.
        try {
            val inputStream = context.assets.open("game/liveops_manifest.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<LiveOpsManifest>(jsonString)
        } catch (e: Exception) {
            GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "LIVEOPS_FALLBACK", "Failed to read bundled LiveOps manifest: ${e.message}")
            LiveOpsManifest(manifestVersion = 1, events = emptyList())
        }
    }
}
