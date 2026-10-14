package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.scripting.LuaBudgetExceededError
import com.jarrlyyy.guessthenumber.scripting.LuaSeasonalEventCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
data class LiveOpsEvent(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val startDate: String,
    val endDate: String,
    val currencyName: String,
    val maxClaims: Int,
    val rewardNebula: Long = 10,
    val permanentBoostPerClaim: Int = 1,
    val minCorrectGuesses: Long = 0,
    val minPrestigeCount: Long = 0,
    val minUltraCount: Long = 0
)

@Serializable
data class LiveOpsManifest(
    val manifestVersion: Int,
    val events: List<LiveOpsEvent>,
    val schemaVersion: Int = 1
)

object LiveOpsContentValidator {
    fun validate(manifest: LiveOpsManifest) {
        ContentValidation.requireSupportedSchema(manifest.schemaVersion)
        require(manifest.manifestVersion in 1..2) { "Unsupported LiveOps manifest version ${manifest.manifestVersion}" }
        require(manifest.events.size <= 32) { "LiveOps manifest contains too many events" }
        ContentValidation.requireUniqueIds(manifest.events.map { it.id })
        manifest.events.forEach { event ->
            require(event.id.matches(Regex("[a-z0-9_-]{1,64}"))) { "LiveOps event has an invalid ID" }
            require(event.title.isNotBlank() && event.title.length <= 100 &&
                event.description.isNotBlank() && event.description.length <= 500 &&
                event.currencyName.isNotBlank() && event.currencyName.length <= 32
            ) { "LiveOps event ${event.id} has invalid display content" }
            require(event.status in setOf("UPCOMING", "ACTIVE", "ENDING", "ARCHIVED")) {
                "LiveOps event ${event.id} has invalid status"
            }
            val start = Instant.parse(event.startDate)
            val end = Instant.parse(event.endDate)
            require(end.isAfter(start)) { "LiveOps event ${event.id} ends before it starts" }
            require(event.maxClaims > 0 && event.rewardNebula >= 0 && event.permanentBoostPerClaim >= 0) {
                "LiveOps event ${event.id} has invalid reward limits"
            }
            require(event.minCorrectGuesses >= 0 && event.minPrestigeCount >= 0 && event.minUltraCount >= 0) {
                "LiveOps event ${event.id} has invalid eligibility requirements"
            }
        }
    }
}

class LiveOpsRepository(
    context: Context,
    private val luaManifestLoader: (() -> LiveOpsManifest)? = null
) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    private val loadLuaManifest = luaManifestLoader ?: {
        LuaSeasonalEventCatalog(appContext).loadBundled()
    }

    suspend fun loadLiveOpsEvents(): LiveOpsManifest = withContext(Dispatchers.IO) {
        try {
            val manifest = loadManifestWithFallback()
            val now = Instant.now()
            manifest.copy(events = manifest.events.map { event ->
                val start = Instant.parse(event.startDate)
                val end = Instant.parse(event.endDate)
                val status = when {
                    event.status == "ARCHIVED" -> "ARCHIVED"
                    now.isBefore(start) -> "UPCOMING"
                    !now.isBefore(end) -> "ARCHIVED"
                    event.status == "ENDING" -> "ENDING"
                    else -> "ACTIVE"
                }
                event.copy(status = status)
            })
        } catch (e: Exception) {
            GameLogger.log(LogLevel.WARN, LoggerCategory.SAVE, "LIVEOPS_FALLBACK", "Invalid bundled LiveOps catalog and JSON fallback; using empty event list (${e.javaClass.simpleName}).")
            LiveOpsManifest(manifestVersion = 2, events = emptyList())
        }
    }

    private fun loadManifestWithFallback(): LiveOpsManifest {
        try {
            return loadLuaManifest().also(LiveOpsContentValidator::validate)
        } catch (error: LuaBudgetExceededError) {
            logLuaFallback(error)
        } catch (error: Exception) {
            logLuaFallback(error)
        }

        // Keep the existing JSON manifest unchanged as a compatibility fallback.
        val jsonString = appContext.assets.open("game/liveops_manifest.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<LiveOpsManifest>(jsonString).also(LiveOpsContentValidator::validate)
    }

    private fun logLuaFallback(error: Throwable) {
        GameLogger.log(
            LogLevel.WARN,
            LoggerCategory.SAVE,
            "LIVEOPS_LUA_FALLBACK",
            "Bundled Lua seasonal catalog failed validation; using the existing JSON manifest (${error.javaClass.simpleName})."
        )
    }
}
