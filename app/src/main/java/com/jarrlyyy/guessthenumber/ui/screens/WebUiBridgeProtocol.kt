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
    private val buyMultipliers = setOf("1", "10", "100", "MAX")
    private val relicActions = setOf("equip", "unequip")
    private val settingKeys = setOf("musicEnabled", "soundEnabled", "vibrationEnabled", "reducedMotion", "reduceFlashes")
    private val mutatorIds = setOf("mut_hardcore", "mut_speed", "mut_blind", "mut_tax")

    sealed interface ParseResult {
        data class Valid(
            val id: String,
            val type: String,
            val guess: Long? = null,
            val questId: String? = null,
            val tier: String? = null,
            val route: String? = null,
            val worldId: String? = null,
            val worldAction: String? = null,
            val itemId: String? = null,
            val multiplier: String? = null,
            val relicId: String? = null,
            val relicAction: String? = null,
            val settingKey: String? = null,
            val settingValue: Boolean? = null,
            val mutatorId: String? = null,
            val mutatorActive: Boolean? = null,
            val arcadeGameId: String? = null,
            val arcadeScore: Int? = null,
            val stakeAmount: String? = null
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
            "read_state", "increment_counter", "homebase_upgrade", "codex_reward", "prestige_reset", "ultra_reset" ->
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
            "relic_action" -> {
                if (request.length() != 5 || !request.has("relicId") || !request.has("action")) return ParseResult.Invalid("invalid_request", id)
                val relicId = request.opt("relicId") as? String ?: return ParseResult.Invalid("invalid_relic", id)
                val action = request.opt("action") as? String ?: return ParseResult.Invalid("invalid_relic_action", id)
                if (relicId !in setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")) return ParseResult.Invalid("invalid_relic", id)
                if (action !in relicActions) return ParseResult.Invalid("invalid_relic_action", id)
                ParseResult.Valid(id, type, relicId = relicId, relicAction = action)
            }
            "buy_talent" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id)
                else {
                    val talentId = request.opt("talentId") as? String ?: return ParseResult.Invalid("invalid_talent", id)
                    if (!talentId.matches(Regex("talent_[A-Za-z0-9_-]{1,48}"))) ParseResult.Invalid("invalid_talent", id)
                    else ParseResult.Valid(id, type, itemId = talentId)
                }
            }
            "set_mutator" -> {
                if (request.length() != 5) return ParseResult.Invalid("invalid_request", id)
                val mutatorId = request.opt("mutatorId") as? String ?: return ParseResult.Invalid("invalid_mutator", id)
                val active = request.opt("active") as? String ?: return ParseResult.Invalid("invalid_mutator_state", id)
                if (mutatorId !in mutatorIds) ParseResult.Invalid("invalid_mutator", id)
                else if (active !in setOf("true", "false")) ParseResult.Invalid("invalid_mutator_state", id)
                else ParseResult.Valid(id, type, mutatorId = mutatorId, mutatorActive = active == "true")
            }
            "arcade_reward" -> {
                if (request.length() != 5) return ParseResult.Invalid("invalid_request", id)
                val gameId = request.opt("gameId") as? String ?: return ParseResult.Invalid("invalid_arcade_game", id)
                val scoreText = request.opt("score") as? String ?: return ParseResult.Invalid("invalid_arcade_score", id)
                val score = scoreText.toIntOrNull() ?: return ParseResult.Invalid("invalid_arcade_score", id)
                if (gameId !in setOf("quick_guess", "reaction_test", "lucky_number", "memory_match", "number_rush")) return ParseResult.Invalid("invalid_arcade_game", id)
                if (score !in 0..9_999) return ParseResult.Invalid("invalid_arcade_score", id)
                ParseResult.Valid(id, type, arcadeGameId = gameId, arcadeScore = score)
            }
            "settle_stake" -> {
                if (request.length() != 4) return ParseResult.Invalid("invalid_request", id)
                val amount = request.opt("amount") as? String ?: return ParseResult.Invalid("invalid_stake", id)
                if (!amount.matches(Regex("[0-9]{1,18}")) || amount.toLongOrNull()?.let { it > 0L } != true) return ParseResult.Invalid("invalid_stake", id)
                ParseResult.Valid(id, type, stakeAmount = amount)
            }
            "update_setting" -> {
                if (request.length() != 5) return ParseResult.Invalid("invalid_request", id)
                val key = request.opt("key") as? String ?: return ParseResult.Invalid("invalid_setting", id)
                val value = request.opt("value") as? String ?: return ParseResult.Invalid("invalid_setting_value", id)
                if (key !in settingKeys) ParseResult.Invalid("invalid_setting", id)
                else if (value !in setOf("true", "false")) ParseResult.Invalid("invalid_setting_value", id)
                else ParseResult.Valid(id, type, settingKey = key, settingValue = value == "true")
            }
            "update_multiplier" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id)
                else {
                    val multiplier = request.opt("multiplier") as? String ?: return ParseResult.Invalid("invalid_multiplier", id)
                    if (multiplier !in buyMultipliers) ParseResult.Invalid("invalid_multiplier", id)
                    else ParseResult.Valid(id, type, multiplier = multiplier)
                }
            }
            "buy_upgrade" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id)
                else {
                    val itemId = request.opt("upgradeId") as? String ?: return ParseResult.Invalid("invalid_upgrade", id)
                    if (!itemId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) ParseResult.Invalid("invalid_upgrade", id)
                    else ParseResult.Valid(id, type, itemId = itemId)
                }
            }
            "buy_shop_item", "equip_cosmetic" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id)
                else {
                    val itemId = request.opt("itemId") as? String ?: return ParseResult.Invalid("invalid_shop_item", id)
                    if (!itemId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) ParseResult.Invalid("invalid_shop_item", id)
                    else ParseResult.Valid(id, type, itemId = itemId)
                }
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
