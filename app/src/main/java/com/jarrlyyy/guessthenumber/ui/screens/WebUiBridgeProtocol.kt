package com.jarrlyyy.guessthenumber.ui.screens

import android.net.Uri
import org.json.JSONObject

/** Parses the small local bridge protocol; the Android host executes valid actions. */
internal object WebUiBridgeProtocol {
    const val MAX_MESSAGE_LENGTH = 512
    private val dailyQuestIds = setOf("daily_guesses_10", "daily_correct_5", "daily_streak_5", "daily_upgrades_3")
    private val lootTiers = setOf("common", "rare", "epic")
    private val navigationRoutes = setOf("world_map", "prestige", "ultra", "home_base", "endgame", "relics", "codex", "talent", "mutators", "achievements")
    private val worldActions = setOf("unlock", "boss")

    sealed interface ParseResult {
        data class Valid(
            val id: String,
            val type: String,
            val guess: Long? = null,
            val questId: String? = null,
            val tier: String? = null,
            val route: String? = null,
            val worldId: String? = null,
            val worldAction: String? = null
        ) : ParseResult
        data class Invalid(val error: String, val id: String = "") : ParseResult
    }

    fun isAllowedSource(
        isMainFrame: Boolean,
        sourceOrigin: Uri,
        currentUrl: String?,
        expectedPath: String = "/assets/web-prototype/index.html"
    ): Boolean {
        if (!isMainFrame || sourceOrigin.scheme != "https" || sourceOrigin.host != "appassets.androidplatform.net") return false
        val currentUri = currentUrl?.let(Uri::parse) ?: return false
        return currentUri.scheme == "https" && currentUri.host == "appassets.androidplatform.net" &&
            currentUri.path == expectedPath && currentUri.query == null && currentUri.fragment == null &&
            currentUri.userInfo == null && currentUri.port == -1
    }

    fun parse(raw: String): ParseResult {
        if (raw.length > MAX_MESSAGE_LENGTH) return ParseResult.Invalid("message_too_large")
        val request = try { JSONObject(raw) } catch (_: Exception) { return ParseResult.Invalid("invalid_json") }
        val rawId = request.opt("id")
        val rawVersion = request.opt("version")
        val rawType = request.opt("type")
        val id = rawId as? String ?: ""
        val type = rawType as? String ?: ""
        val validId = rawId is String && id.matches(Regex("[A-Za-z0-9_-]{1,48}"))
        if (rawVersion !is Int || rawVersion != 1 || !validId || rawType !is String) return ParseResult.Invalid("invalid_request", id)
        return when (type) {
            "read_state", "increment_counter" ->
                if (request.length() != 3) ParseResult.Invalid("invalid_request", id) else ParseResult.Valid(id, type)
            "submit_guess" -> {
                if (request.length() != 4 || !request.has("guess")) return ParseResult.Invalid("invalid_request", id)
                val guessText = request.opt("guess") as? String ?: return ParseResult.Invalid("invalid_guess", id)
                if (guessText.length !in 1..20 || !guessText.matches(Regex("-?[0-9]+"))) return ParseResult.Invalid("invalid_guess", id)
                val guess = guessText.toLongOrNull() ?: return ParseResult.Invalid("invalid_guess", id)
                ParseResult.Valid(id, type, guess = guess)
            }
            "claim_daily_quest" -> {
                if (request.length() != 4 || !request.has("questId")) return ParseResult.Invalid("invalid_request", id)
                val questId = request.opt("questId") as? String ?: return ParseResult.Invalid("invalid_quest", id)
                if (questId !in dailyQuestIds) return ParseResult.Invalid("invalid_quest", id)
                ParseResult.Valid(id, type, questId = questId)
            }
            "open_loot_chest" -> {
                if (request.length() != 4 || !request.has("tier")) return ParseResult.Invalid("invalid_request", id)
                val tier = request.opt("tier") as? String ?: return ParseResult.Invalid("invalid_tier", id)
                if (tier !in lootTiers) return ParseResult.Invalid("invalid_tier", id)
                ParseResult.Valid(id, type, tier = tier)
            }
            "navigate" -> {
                if (request.length() != 4 || !request.has("route")) return ParseResult.Invalid("invalid_request", id)
                val route = request.opt("route") as? String ?: return ParseResult.Invalid("invalid_route", id)
                if (route !in navigationRoutes) return ParseResult.Invalid("invalid_route", id)
                ParseResult.Valid(id, type, route = route)
            }
            "world_action" -> {
                if (request.length() != 5 || !request.has("worldId") || !request.has("action")) return ParseResult.Invalid("invalid_request", id)
                val worldId = request.opt("worldId") as? String ?: return ParseResult.Invalid("invalid_world", id)
                val action = request.opt("action") as? String ?: return ParseResult.Invalid("invalid_world_action", id)
                if (!worldId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) return ParseResult.Invalid("invalid_world", id)
                if (action !in worldActions) return ParseResult.Invalid("invalid_world_action", id)
                ParseResult.Valid(id, type, worldId = worldId, worldAction = action)
            }
            else -> ParseResult.Invalid("unknown_action", id)
        }
    }
}

internal class WebUiRequestIdWindow(private val capacity: Int = 128) {
    private val seen = LinkedHashSet<String>()
    init { require(capacity > 0) { "capacity must be positive" } }
    fun markIfNew(id: String): Boolean {
        if (!seen.add(id)) return false
        if (seen.size > capacity) { val oldest = seen.iterator(); oldest.next(); oldest.remove() }
        return true
    }
}
