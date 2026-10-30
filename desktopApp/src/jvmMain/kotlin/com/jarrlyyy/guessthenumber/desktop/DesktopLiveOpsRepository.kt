package com.jarrlyyy.guessthenumber.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
internal data class DesktopLiveOpsEvent(
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
internal data class DesktopLiveOpsManifest(
    val manifestVersion: Int,
    val events: List<DesktopLiveOpsEvent>,
    val schemaVersion: Int = 1
)

@Serializable
private data class DesktopFeatureFlagConfig(
    val schemaVersion: Int = 1,
    val flags: Map<String, Boolean> = emptyMap()
)

internal data class DesktopLiveOpsSnapshot(
    val enabled: Boolean,
    val events: List<DesktopLiveOpsEvent>,
    val error: String? = null
)

internal class DesktopLiveOpsRepository {
    private val remoteLiveOps = DesktopLuaLiveOpsRepository()
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): DesktopLiveOpsSnapshot = try {
        val flags = loadFlags()
        val enabled = flags["live_ops"] == true && flags["seasonal_rewards"] == true
        val raw = javaClass.classLoader.getResourceAsStream("game/liveops_manifest.json")
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("The canonical LiveOps manifest is missing.")
        val bundledManifest = json.decodeFromString<DesktopLiveOpsManifest>(raw)
        val manifest = remoteLiveOps.loadVerifiedCachedEventManifest() ?: bundledManifest
        validate(manifest)
        val now = Instant.now()
        val events = manifest.events.map { event ->
            val start = Instant.parse(event.startDate)
            val end = Instant.parse(event.endDate)
            event.copy(
                status = when {
                    event.status == "ARCHIVED" -> "ARCHIVED"
                    now.isBefore(start) -> "UPCOMING"
                    !now.isBefore(end) -> "ARCHIVED"
                    event.status == "ENDING" -> "ENDING"
                    else -> "ACTIVE"
                }
            )
        }
        DesktopLiveOpsSnapshot(enabled, events)
    } catch (error: Exception) {
        DesktopLiveOpsSnapshot(
            enabled = false,
            events = emptyList(),
            error = error.message?.take(240) ?: "LiveOps configuration could not be loaded."
        )
    }

    private fun loadFlags(): Map<String, Boolean> {
        val defaults = mapOf(
            "live_ops" to true,
            "seasonal_rewards" to true,
            "world_map" to true,
            "challenge_builder" to true,
            "dev_console" to true,
            "lua_scripting" to true,
            "shared_web_play" to true
        )
        val raw = javaClass.classLoader.getResourceAsStream("game/feature_flags.json")
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: return defaults
        val config = json.decodeFromString<DesktopFeatureFlagConfig>(raw)
        require(config.schemaVersion == 1) { "Unsupported feature flag schema." }
        return defaults.mapValues { (key, default) -> config.flags[key] ?: default }
    }

    fun refreshRemoteEvents(): DesktopLuaLiveOpsRefreshResult = remoteLiveOps.refresh()

    internal fun validateRemoteEvents(manifestVersion: Int, events: List<DesktopLiveOpsEvent>) {
        validate(DesktopLiveOpsManifest(manifestVersion = manifestVersion, events = events))
    }

    private fun validate(manifest: DesktopLiveOpsManifest) {
        require(manifest.schemaVersion == 1 && manifest.manifestVersion in 1..2) {
            "Unsupported LiveOps manifest version."
        }
        require(manifest.events.size <= 32) { "LiveOps manifest contains too many events." }
        require(manifest.events.map { it.id }.distinct().size == manifest.events.size) {
            "LiveOps manifest contains duplicate event IDs."
        }
        manifest.events.forEach { event ->
            require(event.id.matches(Regex("[a-z0-9_-]{1,64}"))) { "LiveOps event ID is invalid." }
            require(event.title.isNotBlank() && event.title.length <= 100 &&
                event.description.isNotBlank() && event.description.length <= 500 &&
                event.currencyName.isNotBlank() && event.currencyName.length <= 32
            ) { "LiveOps event content is invalid." }
            require(event.status in setOf("UPCOMING", "ACTIVE", "ENDING", "ARCHIVED")) {
                "LiveOps event status is invalid."
            }
            val start = Instant.parse(event.startDate)
            val end = Instant.parse(event.endDate)
            require(end.isAfter(start)) { "LiveOps event dates are invalid." }
            require(event.maxClaims in 1..10_000 && event.rewardNebula >= 0 &&
                event.permanentBoostPerClaim >= 0 && event.minCorrectGuesses >= 0 &&
                event.minPrestigeCount >= 0 && event.minUltraCount >= 0
            ) { "LiveOps event reward or eligibility values are invalid." }
        }
    }
}
