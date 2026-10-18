package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyCatalog
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyTransactions
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyRepository
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopTalentDef
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopMinigameDef
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import org.json.JSONArray
import org.json.JSONObject


private val webUiStringKeys = listOf("stats", "statistics", "arcade", "arcade_title", "minigame_quick_guess_name", "minigame_quick_guess_desc", "minigame_reaction_test_name", "minigame_reaction_test_desc", "minigame_lucky_number_name", "minigame_lucky_number_desc", "minigame_memory_match_name", "minigame_memory_match_desc", "minigame_number_rush_name", "minigame_number_rush_desc", "stats_total_guesses", "stats_correct_guesses", "stats_failed_guesses", "stats_prestige_resets", "stats_ultra_resets", "stats_total_playtime", "guessing_precision_heading", "inspector_money_earned", "inspector_money_spent", "inspector_nebula_earned", "inspector_arcade_played", "inspector_current_streak", "inspector_best_streak", "available_money_heading", "multiplier_format", "guess_button", "play_minigame_format", "claim_reward_button", "close_button", "guess_number_target_format", "score_format", "earned_minigame_format", "reaction_test_title", "reaction_time_format", "lucky_number_title", "lucky_number_desc", "jackpot_win_format", "memory_match_title", "memory_correct", "memory_incorrect_format", "number_rush_title", "staking_flip_button", "status_playing", "status_paused", "correct_guess", "too_low", "too_high", "loading_save_data", "cancel", "close", "inspector_money", "inspector_buy_multiplier", "guess_prompt", "duration_seconds_format", "duration_ms_format", "duration_hms_format", "play", "random_event_active", "settings", "game_preferences", "background_music", "sound_effects", "vibration_label", "reduced_motion_label", "reduce_flashes_label", "settings_desc_sound_fx", "settings_desc_vibration", "reduced_motion_description", "settings_desc_reduce_flashes")
private val webUiLocaleFiles = mapOf(
    "en-US" to "en_us.json", "en-GB" to "en_gb.json", "fil-PH" to "fil_ph.json",
    "zh-CN" to "zh_cn.json", "zh-TW" to "zh_tw.json", "ja-JP" to "ja.json",
    "ko-KR" to "ko.json", "es-ES" to "es.json", "fr-FR" to "fr.json",
    "de-DE" to "de.json", "it-IT" to "it.json", "pt-PT" to "pt_pt.json",
    "pt-BR" to "pt_br.json", "ru-RU" to "ru.json", "hi-IN" to "hi_in.json",
    "id-ID" to "id.json", "th-TH" to "th.json", "vi-VN" to "vi.json",
    "tr-TR" to "tr.json", "pl-PL" to "pl.json", "uk-UA" to "uk.json",
    "nl-NL" to "nl.json", "ar-SA" to "ar.json"
)

private fun desktopWebUiStrings(localeTag: String): JSONObject {
    val file = webUiLocaleFiles[localeTag] ?: "en_us.json"
    fun read(name: String): JSONObject? = runCatching {
        Thread.currentThread().contextClassLoader.getResourceAsStream("locales/$name")?.bufferedReader(Charsets.UTF_8)?.use {
            JSONObject(it.readText())
        }
    }.getOrNull()
    val merged = read("en_us.json") ?: JSONObject()
    if (file != "en_us.json") read(file)?.let { overlay ->
        val iterator = overlay.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            merged.put(key, overlay.get(key))
        }
    }
    return JSONObject().apply {
        webUiStringKeys.forEach { key -> put(key, merged.optString(key, key)) }
    }
}


/** Produces only display state; purchase costs and ownership are always revalidated in Kotlin. */
internal fun desktopWebPlayState(
    game: GameState,
    catalog: DesktopEconomyCatalog,
    accepted: Boolean = true,
    lastGuessStatus: String? = null,
    message: String? = null,
    talents: List<DesktopTalentDef> = DesktopEconomyRepository().talents(),
    engine: GameEngine = GameEngine(),
    initialScreen: String = "play",
    minigames: List<DesktopMinigameDef> = DesktopEconomyRepository().minigames()
): JSONObject {
    val upgrades = JSONArray()
    catalog.upgrades.forEach { upgrade ->
        val level = game.upgradeLevels[upgrade.id] ?: 0
        val maxed = level >= upgrade.maxLevel
        val cost = DesktopEconomyTransactions.upgradeBatchCost(
            upgrade, level, game.buyMultiplier, game.money
        )
        val count = when (game.buyMultiplier) {
            "10" -> minOf(10, upgrade.maxLevel - level)
            "100" -> minOf(100, upgrade.maxLevel - level)
            "MAX" -> upgrade.maxLevel - level
            else -> if (maxed) 0 else 1
        }
        upgrades.put(JSONObject()
            .put("id", upgrade.id)
            .put("name", upgrade.name)
            .put("description", upgrade.description)
            .put("level", level)
            .put("maxLevel", upgrade.maxLevel)
            .put("nextCost", cost.format())
            .put("maxed", maxed)
            .put("affordable", !maxed && count > 0 && cost > com.jarrlyyy.guessthenumber.domain.model.BigNumber.ZERO && game.money >= cost))
    }
    val talentsJson = JSONArray()
    talents.forEach { talent ->
        val owned = talent.id in game.prestigeShopPurchases
        val canUnlock = talent.requiredParentId == null || talent.requiredParentId in game.prestigeShopPurchases
        val affordable = game.prestige >= com.jarrlyyy.guessthenumber.domain.model.BigNumber(talent.cost)
        talentsJson.put(JSONObject().put("id", talent.id).put("name", talent.name).put("description", talent.description)
            .put("cost", talent.cost).put("owned", owned).put("canUnlock", canUnlock).put("affordable", affordable)
            .put("requirement", if (!canUnlock) "Unlock ${talent.requiredParentId} first" else if (!affordable) "Need ${talent.cost} Prestige" else "Ready to unlock"))
    }
    val minigamesJson = JSONArray()
    minigames.forEach { game ->
        minigamesJson.put(JSONObject().put("id", game.id).put("name", game.name)
            .put("description", game.description).put("rewardMultiplier", game.rewardMultiplier))
    }
    val statistics = JSONObject()
        .put("totalGuesses", game.statistics.totalGuesses)
        .put("correctGuesses", game.statistics.correctGuesses)
        .put("failedGuesses", game.statistics.failedGuesses)
        .put("moneyEarned", game.statistics.moneyEarned.format())
        .put("moneySpent", game.statistics.moneySpent.format())
        .put("prestigesCount", game.statistics.prestigesCount)
        .put("ultrasCount", game.statistics.ultrasCount)
        .put("nebulaEarned", game.statistics.nebulaEarned)
        .put("playtimeSeconds", game.statistics.playtimeSeconds)
        .put("arcadePlayed", game.statistics.arcadePlayed)
        .put("arcadeBestScores", JSONObject(game.statistics.arcadeBestScores))
        .put("randomEventsTriggered", game.statistics.randomEventsTriggered)
    val worlds = JSONArray()
    DesktopEconomyTransactions.worldsForUi(game).forEach { world ->
        worlds.put(JSONObject()
            .put("id", world.id).put("name", world.name).put("bossName", world.bossName)
            .put("hp", world.hp).put("damage", world.damage)
            .put("active", world.active).put("unlocked", world.unlocked)
            .put("defeated", world.defeated).put("canUnlock", world.canUnlock)
            .put("canFight", world.canFight).put("activeBoss", world.activeBoss))
    }
    val bossStatus = DesktopEconomyTransactions.activeBossBattleStatus(game)
    val shopItems = JSONArray()
    catalog.shopItems.forEach { item ->
        val owned = item.id in game.shopPurchases
        shopItems.put(JSONObject()
            .put("id", item.id)
            .put("name", item.name)
            .put("description", item.description)
            .put("cost", item.nebulaCost)
            .put("category", item.category)
            .put("cosmetic", item.isCosmetic)
            .put("owned", owned)
            .put("equipped", game.equippedCosmeticId == item.id)
            .put("affordable", game.nebula >= com.jarrlyyy.guessthenumber.domain.model.BigNumber(item.nebulaCost)))
    }
    return JSONObject()
        .put("initialScreen", initialScreen)
        .put("uiStrings", desktopWebUiStrings(game.settings.locale))
        .put("buyMultiplier", game.buyMultiplier)
        .put("prestigeRequirement", engine.calculatePrestigeRequirement(game.prestigeCount).format())
        .put("prestigeReward", engine.calculatePrestigeReward(game.money, game.prestigeCount).format())
        .put("canPrestige", game.money >= engine.calculatePrestigeRequirement(game.prestigeCount) && engine.calculatePrestigeReward(game.money, game.prestigeCount) > com.jarrlyyy.guessthenumber.domain.model.BigNumber.ZERO)
        .put("ultraRequirement", engine.calculateUltraRequirement(game.ultraCount).format())
        .put("ultraReward", engine.calculateUltraReward(game.money, game.ultraCount).format())
        .put("canUltra", game.money >= engine.calculateUltraRequirement(game.ultraCount) && game.prestige >= com.jarrlyyy.guessthenumber.domain.model.BigNumber(1_000) && engine.calculateUltraReward(game.money, game.ultraCount) > com.jarrlyyy.guessthenumber.domain.model.BigNumber.ZERO)
        .put("settings", JSONObject().put("musicEnabled", game.settings.musicEnabled).put("soundEnabled", game.settings.soundEnabled).put("vibrationEnabled", game.settings.vibrationEnabled).put("reducedMotion", game.settings.reducedMotion).put("reduceFlashes", game.settings.reduceFlashes))
        .put("statistics", statistics)
        .put("minigames", minigamesJson)
        .put("streak", game.streak)
        .put("bestStreak", game.bestStreak)
        .put("upgrades", upgrades)
        .put("shopItems", shopItems)
        .put("rangeMin", game.currentRangeMin.toString())
        .put("rangeMax", game.currentRangeMax.toString())
        .put("attempts", game.attempts)
        .put("correctGuesses", game.correctGuesses)
        .put("streak", game.streak)
        .put("bestStreak", game.bestStreak)
        .put("accepted", accepted)
        .put("money", game.money.format())
        .put("incomePerSecond", "0")
        .put("prestige", game.prestige.format())
        .put("ultra", game.ultra.format())
        .put("nebula", game.nebula.format())
        .put("prestigeCount", game.prestigeCount)
        .put("ultraCount", game.ultraCount)
        .put("activeWorldId", game.activeWorldId)
        .put("talents", talentsJson)
        .put("activeMutators", JSONArray(game.activeMutators.toList()))
        .put("unlockedWorldCount", game.unlockedWorldIds.size)
        .put("defeatedBossCount", game.defeatedBossIds.size)
        .put("worldBossVictories", game.worldBossVictories)
        .put("homeBaseLevel", game.homeBaseLevel)
        .put("relicInventory", JSONArray(game.relicInventory.toList()))
        .put("equippedRelicIds", JSONArray(game.equippedRelicIds.toList()))
        .put("codexEntries", JSONArray(game.codexEntries.toList()))
        .put("codexRewardClaimed", game.codexRewardClaimed)
        .put("endlessRiftBestTier", game.endlessRiftBestTier)
        .put("dailyQuestGuesses", game.dailyQuestGuesses)
        .put("dailyQuestCorrectGuesses", game.dailyQuestCorrectGuesses)
        .put("dailyQuestBestStreak", game.dailyQuestBestStreak)
        .put("dailyQuestUpgradePurchases", game.dailyQuestUpgradePurchases)
        .put("claimedDailyQuestCount", game.claimedDailyQuests.size)
        .put("claimedDailyQuests", JSONArray(game.claimedDailyQuests.toList()))
        .put("lootCommonChests", game.lootCommonChests)
        .put("lootRareChests", game.lootRareChests)
        .put("lootEpicChests", game.lootEpicChests)
        .put("activeBossBattle", game.activeBossBattleWorldId != null)
        .put("activeBossName", bossStatus?.bossName ?: "")
        .put("activeBossHpTotal", bossStatus?.hp ?: 0)
        .put("activeBossHpRemaining", bossStatus?.let { (it.hp - it.damage).coerceAtLeast(0) } ?: 0)
        .put("worlds", worlds)
        .put("activeBossWorldId", game.activeBossBattleWorldId ?: "")
        .put("endlessRiftActive", game.endlessRiftActive)
        .put("lastRandomEventId", game.lastRandomEventId ?: "")
        .put("randomEventsTriggered", game.statistics.randomEventsTriggered)
        .put("lootDropsFound", game.lootDropsFound)
        .put("lootChestsOpened", game.lootChestsOpened)
        .put("achievementCorrectProgress", game.correctGuesses.coerceIn(0L, 10L))
        .put("achievementCorrectUnlocked", game.correctGuesses >= 10L)
        .put("achievementStreakProgress", game.bestStreak.coerceIn(0, 5))
        .put("achievementStreakUnlocked", game.bestStreak >= 5)
        .put("achievementMoneyProgress", (game.money.value.toDouble() / 1_000_000.0).coerceIn(0.0, 1.0))
        .put("achievementMoneyUnlocked", game.money >= com.jarrlyyy.guessthenumber.domain.model.BigNumber(1_000_000))
        .put("lastGuessStatus", lastGuessStatus ?: "")
        .put("message", message ?: "")
}
