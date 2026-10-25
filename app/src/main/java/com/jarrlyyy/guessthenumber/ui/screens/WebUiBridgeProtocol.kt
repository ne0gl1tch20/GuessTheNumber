package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.traits.TraitGameplayCatalog

import com.jarrlyyy.guessthenumber.BuildConfig
import android.net.Uri
import org.json.JSONObject

/** Parses the small local bridge protocol; the Android host executes valid actions. */
internal object WebUiBridgeProtocol {
    const val MAX_MESSAGE_LENGTH = 2048
    private val dailyQuestIds = setOf("daily_guesses_10", "daily_correct_5", "daily_streak_5", "daily_upgrades_3")
    private val lootTiers = setOf("common", "rare", "epic")
    private val navigationRoutes = setOf("world_map", "prestige", "ultra", "home_base", "endgame", "relics", "codex", "talent", "mutators", "achievements", "save_slots", "player_profile", "color_picker_primary", "color_picker_secondary", "color_picker_tertiary", "traits")
    private val worldActions = setOf("unlock", "boss")
    private val buyMultipliers = setOf("1", "10", "100", "MAX")
    private val relicActions = setOf("equip", "unequip")
    private val settingKeys = setOf("musicEnabled", "soundEnabled", "vibrationEnabled", "notificationsEnabled", "notificationRemindersEnabled", "notificationProgressionEnabled", "notificationEventsEnabled", "notificationIntervalHours", "themeMode", "numberNotation", "locale", "reducedMotion", "volume", "sfxVolume", "reduceFlashes", "themePreset", "customPrimaryColor", "customSecondaryColor", "customTertiaryColor", "webMenuStudioLayout")
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
            val settingValue: String? = null,
            val mutatorId: String? = null,
            val mutatorActive: Boolean? = null,
            val arcadeGameId: String? = null,
            val arcadeScore: Int? = null,
            val stakeAmount: String? = null,
            val musicAction: String? = null,
            val musicTrackId: String? = null,
            val musicIndex: Int? = null,
            val musicToIndex: Int? = null,
            val musicPosition: Int? = null,
            val musicName: String? = null,
            val traitId: String? = null,
            val armId: String? = null,
            val creativeRangeMin: Long? = null,
            val creativeRangeMax: Long? = null
        ) : ParseResult
        data class Invalid(val error: String, val id: String = "") : ParseResult
    }

    fun isAllowedSource(isMainFrame: Boolean, sourceOrigin: Uri, currentUrl: String?, expectedPath: String = "/assets/web-prototype/index.html"): Boolean {
        if (!isMainFrame || sourceOrigin.scheme != "https" || sourceOrigin.host != "appassets.androidplatform.net") return false
        val currentUri = currentUrl?.let(Uri::parse) ?: return false
        return currentUri.scheme == "https" && currentUri.host == "appassets.androidplatform.net" && currentUri.path == expectedPath && currentUri.query == null && currentUri.fragment == null && currentUri.userInfo == null && currentUri.port == -1
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
            "read_state", "read_changelog", "read_profile", "read_onboarding_state", "complete_nimmy_tutorial", "reset_everything", "music_state", "increment_counter", "homebase_upgrade", "codex_reward", "prestige_reset", "ultra_reset", "check_updates", "install_update", "open_update_page", "claim_daily_reward", "dismiss_offline_gains", "open_arcade" -> if (request.length() != 3) ParseResult.Invalid("invalid_request", id) else ParseResult.Valid(id, type)
            "read_feature_tutorial", "complete_feature_tutorial" -> {
                if (request.length() != 4 || !request.has("featureId")) return ParseResult.Invalid("invalid_request", id)
                val featureId = request.optString("featureId")
                if (!featureId.matches(Regex("[A-Za-z0-9_-]{1,48}"))) return ParseResult.Invalid("invalid_feature", id)
                ParseResult.Valid(id, type, itemId = featureId)
            }
            "creative_update_value" -> {
                if (request.length() != 5 || !request.has("key") || !request.has("value")) return ParseResult.Invalid("invalid_request", id)
                val key = request.opt("key") as? String ?: return ParseResult.Invalid("invalid_creative_value", id)
                val value = request.opt("value") as? String ?: return ParseResult.Invalid("invalid_creative_value", id)
                val allowedKeys = setOf("money", "prestige", "ultra", "nebula", "attempts", "correctGuesses", "streak", "bestStreak")
                val upgradeLevelKey = key.matches(Regex("(?:upgradeLevel|prestigeUpgradeLevel|ultraUpgradeLevel):[A-Za-z0-9_-]{1,64}"))
                if (key !in allowedKeys && !upgradeLevelKey) return ParseResult.Invalid("invalid_creative_value", id)
                val valid = if (upgradeLevelKey) value.toIntOrNull()?.let { it in 0..999_999 } == true else when (key) {
                    "money", "prestige", "ultra", "nebula" -> value.length in 1..100 && Regex("[0-9]+(?:\\.[0-9]+)?(?:[A-Za-z]{1,4})?").matches(value)
                    "attempts", "correctGuesses" -> value.toLongOrNull()?.let { it in 0L..1_000_000_000_000L } == true
                    "streak", "bestStreak" -> value.toIntOrNull()?.let { it in 0..1_000_000 } == true
                    else -> false
                }
                if (!valid) return ParseResult.Invalid("invalid_creative_value", id)
                ParseResult.Valid(id, type, settingKey = key, settingValue = value)
            }
            "creative_update_range" -> {
                if (request.length() != 5 || !request.has("rangeMin") || !request.has("rangeMax")) return ParseResult.Invalid("invalid_request", id)
                val minText = request.opt("rangeMin") as? String ?: return ParseResult.Invalid("invalid_creative_range", id)
                val maxText = request.opt("rangeMax") as? String ?: return ParseResult.Invalid("invalid_creative_range", id)
                val min = minText.toLongOrNull() ?: return ParseResult.Invalid("invalid_creative_range", id)
                val max = maxText.toLongOrNull() ?: return ParseResult.Invalid("invalid_creative_range", id)
                if (min !in 1L..1_000_000L || max !in min..1_000_000L) return ParseResult.Invalid("invalid_creative_range", id)
                ParseResult.Valid(id, type, creativeRangeMin = min, creativeRangeMax = max)
            }
            "update_setting" -> {
                if (request.length() != 5) return ParseResult.Invalid("invalid_request", id)
                val key = request.opt("key") as? String ?: return ParseResult.Invalid("invalid_setting", id)
                val value = request.opt("value") as? String ?: return ParseResult.Invalid("invalid_setting_value", id)
                if (key !in settingKeys) return ParseResult.Invalid("invalid_setting", id)
                val valid = when (key) {
                    "musicEnabled", "soundEnabled", "vibrationEnabled", "notificationsEnabled", "notificationRemindersEnabled", "notificationProgressionEnabled", "notificationEventsEnabled", "reducedMotion", "reduceFlashes" -> value in setOf("true", "false")
                    "themeMode" -> value in setOf("System", "Dark", "Light", "AMOLED")
                    "numberNotation" -> value in setOf("Standard", "Scientific", "Engineering")
                    "locale" -> value.matches(Regex("[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?"))
                    "notificationIntervalHours" -> value in setOf("12", "24", "48")
                    "volume", "sfxVolume" -> value.toFloatOrNull()?.let { it in 0f..1f } == true
                    "themePreset" -> value in setOf("Neon", "Sunset", "Ocean", "Forest", "Custom")
                    "customPrimaryColor", "customSecondaryColor", "customTertiaryColor" -> value.matches(Regex("#[0-9A-Fa-f]{6}"))
                    "webMenuStudioLayout" -> value.length in 1..1600 && runCatching { JSONObject(value) }.isSuccess
                    else -> false
                }
                if (!valid) ParseResult.Invalid("invalid_setting_value", id) else ParseResult.Valid(id, type, settingKey = key, settingValue = value)
            }
            "submit_guess" -> {
                if (request.length() != 4 || !request.has("guess")) return ParseResult.Invalid("invalid_request", id)
                val guessText = request.opt("guess") as? String ?: return ParseResult.Invalid("invalid_guess", id)
                if (guessText.length !in 1..20 || !guessText.matches(Regex("-?[0-9]+"))) return ParseResult.Invalid("invalid_guess", id)
                val guess = guessText.toLongOrNull() ?: return ParseResult.Invalid("invalid_guess", id)
                ParseResult.Valid(id, type, guess = guess)
            }
            else -> ParseResult.Valid(id, type)
        }
    }
}
