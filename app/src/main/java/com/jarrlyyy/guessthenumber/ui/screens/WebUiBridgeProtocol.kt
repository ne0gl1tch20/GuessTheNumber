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

    private fun isValidMenuStudioLayout(value: String): Boolean {
        if (value.length !in 1..1600) return false
        return runCatching {
            val root = JSONObject(value)
            val rootKeys = root.keys().asSequence().toSet()
            if (rootKeys.any { it !in setOf("areas", "orders", "colors") }) return false
            val allowedId = Regex("[A-Za-z0-9_-]{1,64}")
            val areas = root.optJSONObject("areas") ?: return false
            if (areas.length() > 100) return false
            val areaKeys = areas.keys().asSequence().toList()
            if (areaKeys.any { !allowedId.matches(it) || areas.opt(it) !in setOf("nav", "more") }) return false
            val orders = root.optJSONObject("orders") ?: return false
            if (orders.keys().asSequence().any { it !in setOf("nav", "more") }) return false
            for (area in listOf("nav", "more")) {
                val order = orders.optJSONArray(area) ?: return false
                if (order.length() > 100) return false
                val seen = mutableSetOf<String>()
                for (i in 0 until order.length()) {
                    val id = order.opt(i) as? String ?: return false
                    if (!allowedId.matches(id) || !seen.add(id)) return false
                }
            }
            val colors = root.optJSONObject("colors")
            if (root.has("colors") && colors == null) return false
            if (colors != null) {
                if (colors.length() > 100) return false
                val hex = Regex("#[0-9A-Fa-f]{6}")
                val keys = colors.keys().asSequence().toList()
                if (keys.any { !allowedId.matches(it) || !hex.matches(colors.optString(it)) }) return false
            }
            true
        }.getOrDefault(false)
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
                    "webMenuStudioLayout" -> isValidMenuStudioLayout(value)
                    else -> false
                }
                if (!valid) ParseResult.Invalid("invalid_setting_value", id) else ParseResult.Valid(id, type, settingKey = key, settingValue = value)
            }
            "update_multiplier" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id) else {
                    val multiplier = request.opt("multiplier") as? String ?: return ParseResult.Invalid("invalid_multiplier", id)
                    if (multiplier !in buyMultipliers) ParseResult.Invalid("invalid_multiplier", id) else ParseResult.Valid(id, type, multiplier = multiplier)
                }
            }
            "buy_prestige_upgrade", "buy_ultra_upgrade" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id) else {
                    val itemId = request.opt("upgradeId") as? String ?: return ParseResult.Invalid("invalid_upgrade", id)
                    if (!itemId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) ParseResult.Invalid("invalid_upgrade", id) else ParseResult.Valid(id, type, itemId = itemId)
                }
            }
            "buy_prestige_shop_item", "buy_ultra_shop_item" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id) else {
                    val itemId = request.opt("itemId") as? String ?: return ParseResult.Invalid("invalid_shop_item", id)
                    if (!itemId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) ParseResult.Invalid("invalid_shop_item", id) else ParseResult.Valid(id, type, itemId = itemId)
                }
            }
            "buy_upgrade" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id) else {
                    val itemId = request.opt("upgradeId") as? String ?: return ParseResult.Invalid("invalid_upgrade", id)
                    if (!itemId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) ParseResult.Invalid("invalid_upgrade", id) else ParseResult.Valid(id, type, itemId = itemId)
                }
            }
            "buy_shop_item", "equip_cosmetic" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id) else {
                    val itemId = request.opt("itemId") as? String ?: return ParseResult.Invalid("invalid_shop_item", id)
                    if (!itemId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) ParseResult.Invalid("invalid_shop_item", id) else ParseResult.Valid(id, type, itemId = itemId)
                }
            }
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
                if (questId !in dailyQuestIds) ParseResult.Invalid("invalid_quest", id) else ParseResult.Valid(id, type, questId = questId)
            }
            "open_loot_chest" -> {
                if (request.length() != 4 || !request.has("tier")) ParseResult.Invalid("invalid_request", id)
                else {
                    val tier = request.opt("tier") as? String ?: return ParseResult.Invalid("invalid_tier", id)
                    if (tier !in lootTiers) ParseResult.Invalid("invalid_tier", id) else ParseResult.Valid(id, type, tier = tier)
                }
            }
            "navigate" -> {
                if (request.length() != 4 || !request.has("route")) ParseResult.Invalid("invalid_request", id)
                else {
                    val route = request.opt("route") as? String ?: return ParseResult.Invalid("invalid_route", id)
                    if (route !in navigationRoutes && !(BuildConfig.DEBUG && route == "dev_settings") && !route.matches(Regex("menu_color_[A-Za-z0-9_-]{1,32}"))) ParseResult.Invalid("invalid_route", id) else ParseResult.Valid(id, type, route = route)
                }
            }
            "select_trait_arm" -> {
                if (request.length() != 5 || !request.has("traitId") || !request.has("armId")) ParseResult.Invalid("invalid_request", id)
                else {
                    val traitId = request.opt("traitId") as? String ?: return ParseResult.Invalid("invalid_trait", id)
                    val armId = request.opt("armId") as? String ?: return ParseResult.Invalid("invalid_arm", id)
                    val trait = TraitGameplayCatalog.find(traitId) ?: return ParseResult.Invalid("invalid_trait", id)
                    if (trait.arms.isEmpty()) ParseResult.Invalid("invalid_trait", id) else if (!TraitGameplayCatalog.allowsArm(traitId, armId)) ParseResult.Invalid("invalid_arm", id) else ParseResult.Valid(id, type, traitId = traitId, armId = armId)
                }
            }
            "world_action" -> {
                if (request.length() != 5 || !request.has("worldId") || !request.has("action")) ParseResult.Invalid("invalid_request", id)
                else {
                    val worldId = request.opt("worldId") as? String ?: return ParseResult.Invalid("invalid_world", id)
                    val action = request.opt("action") as? String ?: return ParseResult.Invalid("invalid_world_action", id)
                    if (!worldId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) ParseResult.Invalid("invalid_world", id) else if (action !in worldActions) ParseResult.Invalid("invalid_world_action", id) else ParseResult.Valid(id, type, worldId = worldId, worldAction = action)
                }
            }
            "relic_action" -> {
                if (request.length() != 5 || !request.has("relicId") || !request.has("action")) ParseResult.Invalid("invalid_request", id)
                else {
                    val relicId = request.opt("relicId") as? String ?: return ParseResult.Invalid("invalid_relic", id)
                    val action = request.opt("action") as? String ?: return ParseResult.Invalid("invalid_relic_action", id)
                    if (relicId !in setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")) ParseResult.Invalid("invalid_relic", id) else if (action !in relicActions) ParseResult.Invalid("invalid_relic_action", id) else ParseResult.Valid(id, type, relicId = relicId, relicAction = action)
                }
            }
            "buy_talent" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id) else {
                    val talentId = request.opt("talentId") as? String ?: return ParseResult.Invalid("invalid_talent", id)
                    if (!talentId.matches(Regex("talent_[A-Za-z0-9_-]{1,48}"))) ParseResult.Invalid("invalid_talent", id) else ParseResult.Valid(id, type, itemId = talentId)
                }
            }
            "set_mutator" -> {
                if (request.length() != 5) ParseResult.Invalid("invalid_request", id) else {
                    val mutatorId = request.opt("mutatorId") as? String ?: return ParseResult.Invalid("invalid_mutator", id)
                    val active = request.opt("active") as? String ?: return ParseResult.Invalid("invalid_mutator_state", id)
                    if (mutatorId !in mutatorIds) ParseResult.Invalid("invalid_mutator", id) else if (active !in setOf("true", "false")) ParseResult.Invalid("invalid_mutator_state", id) else ParseResult.Valid(id, type, mutatorId = mutatorId, mutatorActive = active == "true")
                }
            }
            "arcade_reward" -> {
                if (request.length() != 5) ParseResult.Invalid("invalid_request", id) else {
                    val gameId = request.opt("gameId") as? String ?: return ParseResult.Invalid("invalid_arcade_game", id)
                    val scoreText = request.opt("score") as? String ?: return ParseResult.Invalid("invalid_arcade_score", id)
                    val score = scoreText.toIntOrNull() ?: return ParseResult.Invalid("invalid_arcade_score", id)
                    if (gameId !in setOf("quick_guess", "reaction_test", "lucky_number", "memory_match", "number_rush")) ParseResult.Invalid("invalid_arcade_game", id) else if (score !in 0..9_999) ParseResult.Invalid("invalid_arcade_score", id) else ParseResult.Valid(id, type, arcadeGameId = gameId, arcadeScore = score)
                }
            }
            "settle_stake" -> {
                if (request.length() != 4) ParseResult.Invalid("invalid_request", id) else {
                    val amount = request.opt("amount") as? String ?: return ParseResult.Invalid("invalid_stake", id)
                    if (!amount.matches(Regex("[0-9]{1,18}")) || amount.toLongOrNull()?.let { it > 0L } != true) ParseResult.Invalid("invalid_stake", id) else ParseResult.Valid(id, type, stakeAmount = amount)
                }
            }
            "music_action" -> {
                val allowedFields = setOf("version", "id", "type", "action", "trackId", "index", "toIndex", "position", "name")
                if (request.keys().asSequence().any { it !in allowedFields }) return ParseResult.Invalid("invalid_request", id)
                val action = request.opt("action") as? String ?: return ParseResult.Invalid("invalid_music_action", id)
                val actions = setOf("import", "play", "pause", "stop", "next", "previous", "shuffle", "repeat", "play_track", "enqueue", "remove_track", "add_to_playlist", "remove_from_playlist", "clear_queue", "remove_queue", "move_queue", "create_playlist", "delete_playlist", "play_playlist", "seek")
                if (action !in actions) return ParseResult.Invalid("invalid_music_action", id)
                val trackId = request.opt("trackId") as? String
                val index = if (request.has("index")) request.optInt("index", -1) else null
                val toIndex = if (request.has("toIndex")) request.optInt("toIndex", -1) else null
                val position = if (request.has("position")) request.optInt("position", -1) else null
                val name = request.opt("name") as? String
                if (action in setOf("play_track", "enqueue", "remove_track", "add_to_playlist", "remove_from_playlist") && (trackId == null || !trackId.matches(Regex("track_[A-Za-z0-9_-]{1,64}")))) return ParseResult.Invalid("invalid_music_track", id)
                if (action in setOf("remove_queue", "move_queue") && (index == null || index !in 0..1000)) return ParseResult.Invalid("invalid_music_index", id)
                if (action == "move_queue" && (toIndex == null || toIndex !in 0..1000)) return ParseResult.Invalid("invalid_music_index", id)
                if (action == "seek" && (position == null || position !in 0..86_400_000)) return ParseResult.Invalid("invalid_music_position", id)
                if (action in setOf("create_playlist", "delete_playlist", "play_playlist", "add_to_playlist", "remove_from_playlist") && (name == null || name.trim().length !in 1..60 || name.any { it.isISOControl() })) return ParseResult.Invalid("invalid_music_playlist", id)
                val extraCount = listOfNotNull(trackId.takeIf { action in setOf("play_track", "enqueue", "remove_track", "add_to_playlist", "remove_from_playlist") }, index.takeIf { action in setOf("remove_queue", "move_queue") }, toIndex.takeIf { action == "move_queue" }, position.takeIf { action == "seek" }, name.takeIf { action in setOf("create_playlist", "delete_playlist", "play_playlist", "add_to_playlist", "remove_from_playlist") }).size
                if (request.length() != 4 + extraCount) return ParseResult.Invalid("invalid_request", id)
                ParseResult.Valid(id, type, musicAction = action, musicTrackId = trackId, musicIndex = index, musicToIndex = toIndex, musicPosition = position, musicName = name?.trim())
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