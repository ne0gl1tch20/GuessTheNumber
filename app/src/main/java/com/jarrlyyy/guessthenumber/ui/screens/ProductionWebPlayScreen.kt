package com.jarrlyyy.guessthenumber.ui.screens

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.ui.viewmodel.GuessSubmissionResult
import com.jarrlyyy.guessthenumber.data.repository.WorldConfigRepository
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository
import com.jarrlyyy.guessthenumber.data.repository.LocaleManager

private const val WEB_UI_ORIGIN = "https://appassets.androidplatform.net"
private const val WEB_UI_PATH = "/assets/web-play/"

@Composable
fun ProductionWebPlayScreen(
    state: GameState,
    incomePerSecond: BigNumber,
    localeManager: LocaleManager,
    stateProvider: () -> GameState,
    onSubmitGuess: (Long) -> GuessSubmissionResult,
    onClaimDailyQuest: (String) -> Unit,
    onOpenLootChest: (String) -> Unit,
    onNavigateProgression: (String) -> Unit,
    onWorldAction: (String, String) -> Unit,
    onBuyUpgrade: (String, BigNumber) -> Unit,
    onBuyShopItem: (String, Long) -> Unit,
    onEquipCosmetic: (String) -> Unit,
    onUpdateMultiplier: (String) -> Unit,
    onUpdateSettings: (GameSettings) -> Unit,
    onPrestigeReset: () -> Unit,
    onUltraReset: () -> Unit,
    onBuyTalent: (String, Long) -> Unit,
    onSetMutatorActive: (String, Boolean) -> Unit,
    onArcadeReward: (String, Int) -> Boolean,
    onSettleStake: (String) -> Boolean
) {
    val requestIds = remember { WebUiRequestIdWindow() }
    val progressionEngine = remember { GameEngine() }
    val currentGameState by rememberUpdatedState(state)
    val currentIncome by rememberUpdatedState(incomePerSecond)
    val readState by rememberUpdatedState(stateProvider)
    val submitGuess by rememberUpdatedState(onSubmitGuess)
    val claimDailyQuest by rememberUpdatedState(onClaimDailyQuest)
    val openLootChest by rememberUpdatedState(onOpenLootChest)
    val navigateProgression by rememberUpdatedState(onNavigateProgression)
    val dispatchWorldAction by rememberUpdatedState(onWorldAction)
    val buyUpgrade by rememberUpdatedState(onBuyUpgrade)
    val buyShopItem by rememberUpdatedState(onBuyShopItem)
    val equipCosmetic by rememberUpdatedState(onEquipCosmetic)
    val updateMultiplier by rememberUpdatedState(onUpdateMultiplier)
    val updateSettings by rememberUpdatedState(onUpdateSettings)
    val buyTalent by rememberUpdatedState(onBuyTalent)
    val setMutatorActive by rememberUpdatedState(onSetMutatorActive)
    val awardArcadeReward by rememberUpdatedState(onArcadeReward)
    val settleStake by rememberUpdatedState(onSettleStake)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var bridgeErrorDetails by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current
    fun reportBridgeException(action: String, requestId: String, error: Exception) {
        Log.e("GTNWebBridge", "Bridge request failed: action=$action requestId=$requestId", error)
        val trace = Log.getStackTraceString(error).take(12000)
        bridgeErrorDetails = "Guess The Number bridge error\nAction: $action\nRequest ID: $requestId\nException: ${error.javaClass.name}: ${error.message ?: "(no message)"}\n\nJava/Kotlin stack trace:\n$trace"
    }
    val supportsMessageListener = remember { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) }

    DisposableEffect(Unit) {
        onDispose { webViewRef?.let { it.stopLoading(); it.destroy() } }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val worldDefinitions = WorldConfigRepository(context).loadWorlds()
            val configRepository = JsonConfigRepository(context)
            val upgradeDefinitions = configRepository.loadUpgrades()
            val shopDefinitions = configRepository.loadShopItems()
            val talentDefinitions = configRepository.loadTalents()
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()
            WebView(context).apply {
                webViewRef = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.safeBrowsingEnabled = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) = assetLoader.shouldInterceptRequest(request.url)
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        return !(uri.scheme == "https" && uri.host == "appassets.androidplatform.net" && uri.path == "${WEB_UI_PATH}index.html" && uri.query == null && uri.fragment == null)
                    }
                }
                if (supportsMessageListener) {
                    WebViewCompat.addWebMessageListener(this, "gtnHost", setOf(WEB_UI_ORIGIN), object : WebViewCompat.WebMessageListener {
                        override fun onPostMessage(view: WebView, message: WebMessageCompat, sourceOrigin: Uri, isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy) {
                            view.post {
                                if (!WebUiBridgeProtocol.isAllowedSource(isMainFrame, sourceOrigin, view.url, "${WEB_UI_PATH}index.html")) {
                                    replyProxy.postMessage(errorResponse("", "origin_not_allowed")); return@post
                                }
                                when (val parsed = WebUiBridgeProtocol.parse(message.data ?: "")) {
                                    is WebUiBridgeProtocol.ParseResult.Invalid -> replyProxy.postMessage(errorResponse(parsed.id, parsed.error))
                                    is WebUiBridgeProtocol.ParseResult.Valid -> {
                                        if (!requestIds.markIfNew(parsed.id)) { replyProxy.postMessage(errorResponse(parsed.id, "duplicate_request")); return@post }
                                        val beforeAction = try {
                                            readState()
                                        } catch (exception: Exception) {
                                            reportBridgeException(parsed.type, parsed.id, exception)
                                            replyProxy.postMessage(errorResponse(parsed.id, "state_read_failed"))
                                            return@post
                                        }
                                        val result = if (parsed.type == "submit_guess" && parsed.guess != null) try { submitGuess(parsed.guess) } catch (exception: Exception) { reportBridgeException(parsed.type, parsed.id, exception); null } else null
                                        if (parsed.type == "submit_guess" && result == null) { replyProxy.postMessage(errorResponse(parsed.id, "guess_processing_failed")); return@post }
                                        try {
                                            when (parsed.type) {
                                                "claim_daily_quest" -> parsed.questId?.let(claimDailyQuest)
                                                "open_loot_chest" -> parsed.tier?.let(openLootChest)
                                                "navigate" -> parsed.route?.let(navigateProgression)
                                                "homebase_upgrade" -> navigateProgression("home_upgrade")
                                                "codex_reward" -> navigateProgression("claim_codex")
                                                "prestige_reset" -> onPrestigeReset()
                                                "ultra_reset" -> onUltraReset()
                                                "buy_talent" -> parsed.itemId?.let { talentId ->
                                                    val talent = talentDefinitions.firstOrNull { it.id == talentId } ?: throw IllegalArgumentException("Unknown talent")
                                                    buyTalent(talent.id, talent.cost)
                                                }
                                                "set_mutator" -> if (parsed.mutatorId != null && parsed.mutatorActive != null) setMutatorActive(parsed.mutatorId, parsed.mutatorActive)
                                                "relic_action" -> parsed.relicId?.let { navigateProgression("equip_relic:$it") }
                                                "world_action" -> if (parsed.worldId != null && parsed.worldAction != null) dispatchWorldAction(parsed.worldId, parsed.worldAction)
                                                "arcade_reward" -> {
                                                    val gameId = parsed.arcadeGameId
                                                    val score = parsed.arcadeScore
                                                    if (gameId == null || score == null || !awardArcadeReward(gameId, score)) {
                                                        throw IllegalArgumentException("Arcade reward was rejected.")
                                                    }
                                                }
                                                "settle_stake" -> {
                                                    val amount = parsed.stakeAmount
                                                    if (amount == null || !settleStake(amount)) {
                                                        throw IllegalArgumentException("Lucky Stake was rejected.")
                                                    }
                                                }
                                                "update_setting" -> {
                                                    val key = parsed.settingKey
                                                    val value = parsed.settingValue
                                                    if (key != null && value != null) {
                                                        val settings = readState().settings
                                                        val updated = when (key) {
                                                            "musicEnabled" -> settings.copy(musicEnabled = value)
                                                            "soundEnabled" -> settings.copy(soundEnabled = value)
                                                            "vibrationEnabled" -> settings.copy(vibrationEnabled = value)
                                                            "reducedMotion" -> settings.copy(reducedMotion = value)
                                                            "reduceFlashes" -> settings.copy(reduceFlashes = value)
                                                            else -> settings
                                                        }
                                                        updateSettings(updated)
                                                    }
                                                }
                                                "update_multiplier" -> parsed.multiplier?.let(updateMultiplier)
                                                "buy_upgrade" -> parsed.itemId?.let { upgradeId ->
                                                    val upgrade = upgradeDefinitions.firstOrNull { it.id == upgradeId } ?: throw IllegalArgumentException("Unknown upgrade")
                                                    val game = readState()
                                                    val level = game.upgradeLevels[upgrade.id] ?: 0
                                                    if (level >= upgrade.maxLevel) throw IllegalArgumentException("Upgrade maxed")
                                                    val base = BigNumber(upgrade.baseCost)
                                                    val mult = BigNumber(upgrade.costMultiplier)
                                                    val amount = when (game.buyMultiplier) {
                                                        "10" -> minOf(10, upgrade.maxLevel - level)
                                                        "100" -> minOf(100, upgrade.maxLevel - level)
                                                        else -> 1
                                                    }
                                                    var total = BigNumber.ZERO
                                                    for (i in 0 until amount) total += base * mult.pow(level + i)
                                                    if (game.buyMultiplier == "MAX") {
                                                        total = BigNumber.ZERO
                                                        var count = 0
                                                        while (level + count < upgrade.maxLevel) {
                                                            val cost = base * mult.pow(level + count)
                                                            if (game.money < total + cost) break
                                                            total += cost
                                                            count++
                                                        }
                                                        if (count == 0) throw IllegalArgumentException("Cannot afford upgrade")
                                                    }
                                                    if (game.money < total) throw IllegalArgumentException("Cannot afford upgrade")
                                                    buyUpgrade(upgradeId, total)
                                                }
                                                "buy_shop_item" -> parsed.itemId?.let { itemId ->
                                                    val item = shopDefinitions.firstOrNull { it.id == itemId } ?: throw IllegalArgumentException("Unknown shop item")
                                                    val game = readState()
                                                    if (item.id in game.shopPurchases) {
                                                        if (item.isCosmetic) equipCosmetic(itemId) else throw IllegalArgumentException("Already purchased")
                                                    } else {
                                                        if (game.nebula < BigNumber(item.nebulaCost)) throw IllegalArgumentException("Cannot afford shop item")
                                                        buyShopItem(itemId, item.nebulaCost)
                                                    }
                                                }
                                                "equip_cosmetic" -> parsed.itemId?.let { itemId ->
                                                    val item = shopDefinitions.firstOrNull { it.id == itemId && it.isCosmetic } ?: throw IllegalArgumentException("Unknown cosmetic")
                                                    if (itemId !in readState().shopPurchases) throw IllegalArgumentException("Cosmetic not owned")
                                                    equipCosmetic(item.id)
                                                }
                                            }
                                        } catch (exception: Exception) {
                                            reportBridgeException(parsed.type, parsed.id, exception)
                                            replyProxy.postMessage(errorResponse(parsed.id, "action_processing_failed")); return@post
                                        }
                                        val game = try {
                                            readState()
                                        } catch (exception: Exception) {
                                            reportBridgeException(parsed.type, parsed.id, exception)
                                            replyProxy.postMessage(errorResponse(parsed.id, "state_read_failed"))
                                            return@post
                                        }
                                        val actionAccepted = when (parsed.type) {
                                            "claim_daily_quest" -> parsed.questId != null && parsed.questId in game.claimedDailyQuests && parsed.questId !in beforeAction.claimedDailyQuests
                                            "open_loot_chest" -> when (parsed.tier) {
                                                "common" -> game.lootCommonChests < beforeAction.lootCommonChests
                                                "rare" -> game.lootRareChests < beforeAction.lootRareChests
                                                "epic" -> game.lootEpicChests < beforeAction.lootEpicChests
                                                else -> false
                                            }
                                            "world_action" -> when (parsed.worldAction) {
                                                "unlock" -> parsed.worldId != null && parsed.worldId in game.unlockedWorldIds && parsed.worldId !in beforeAction.unlockedWorldIds
                                                "boss" -> parsed.worldId != null && (game.activeBossBattleWorldId != beforeAction.activeBossBattleWorldId) && (game.activeBossBattleWorldId == parsed.worldId || beforeAction.activeBossBattleWorldId == parsed.worldId)
                                                else -> false
                                            }
                                            "homebase_upgrade" -> game.homeBaseLevel > beforeAction.homeBaseLevel
                                            "codex_reward" -> game.codexRewardClaimed && !beforeAction.codexRewardClaimed
                                            "relic_action" -> parsed.relicId != null && (
                                                (parsed.relicAction == "equip" && parsed.relicId in game.equippedRelicIds && parsed.relicId !in beforeAction.equippedRelicIds) ||
                                                (parsed.relicAction == "unequip" && parsed.relicId !in game.equippedRelicIds && parsed.relicId in beforeAction.equippedRelicIds)
                                            )
                                            else -> true
                                        }
                                        val activeBossWorld = game.activeBossBattleWorldId?.let { id -> worldDefinitions.firstOrNull { it.id == id } }
                                        val activeBossHp = activeBossWorld?.let { it.hp + if (game.endlessRiftActive) ((game.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0 } ?: 0
                                        val bossDamage = activeBossWorld?.let { (game.bossBattleProgress[it.bossId] ?: 0).coerceIn(0, activeBossHp) } ?: 0
                                        val totalUpgradeLevels = game.upgradeLevels.values.sum()
                                        val worldsJson = org.json.JSONArray()
                                        worldDefinitions.forEach { world ->
                                            val unlocked = world.id in game.unlockedWorldIds
                                            val defeated = world.bossId in game.defeatedBossIds && !game.endlessRiftActive
                                            val canUnlock = world.unlockBossId?.let { requiredBossId -> requiredBossId in game.defeatedBossIds && game.correctGuesses >= world.unlockCorrectGuesses && totalUpgradeLevels >= world.unlockUpgradeCount && game.prestigeCount >= world.unlockPrestigeCount && game.ultraCount >= world.unlockUltraCount } ?: true
                                            val canFight = game.endlessRiftActive || (game.correctGuesses >= world.fightCorrectGuesses && totalUpgradeLevels >= world.fightUpgradeCount && game.prestigeCount >= world.fightPrestigeCount && game.ultraCount >= world.fightUltraCount)
                                            val hp = world.hp + if (game.endlessRiftActive) ((game.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0
                                            val damage = (game.bossBattleProgress[world.bossId] ?: 0).coerceIn(0, hp)
                                            worldsJson.put(JSONObject().put("id", world.id).put("name", world.fallback).put("bossId", world.bossId).put("bossName", world.bossFallback).put("hp", hp).put("damage", damage).put("unlocked", unlocked).put("defeated", defeated).put("canUnlock", canUnlock).put("canFight", canFight).put("active", game.activeWorldId == world.id).put("activeBoss", game.activeBossBattleWorldId == world.id))
                                        }
                                        val upgradesJson = org.json.JSONArray()
                                        upgradeDefinitions.forEach { upgrade ->
                                            val level = game.upgradeLevels[upgrade.id] ?: 0
                                            val maxed = level >= upgrade.maxLevel
                                            val base = BigNumber(upgrade.baseCost)
                                            val mult = BigNumber(upgrade.costMultiplier)
                                            var amount = when (game.buyMultiplier) {
                                                "10" -> minOf(10, upgrade.maxLevel - level)
                                                "100" -> minOf(100, upgrade.maxLevel - level)
                                                "MAX" -> 0
                                                else -> if (maxed) 0 else 1
                                            }
                                            var cost = BigNumber.ZERO
                                            if (game.buyMultiplier == "MAX" && !maxed) {
                                                while (level + amount < upgrade.maxLevel) {
                                                    val nextCost = base * mult.pow(level + amount)
                                                    if (game.money < cost + nextCost) break
                                                    cost += nextCost
                                                    amount++
                                                }
                                                if (amount == 0) {
                                                    val firstCost = base * mult.pow(level)
                                                    if (game.money >= firstCost) { cost = firstCost; amount = 1 }
                                                }
                                            } else {
                                                for (i in 0 until amount) cost += base * mult.pow(level + i)
                                            }
                                            upgradesJson.put(JSONObject().put("id", upgrade.id).put("name", upgrade.name).put("description", upgrade.description)
                                                .put("level", level).put("maxLevel", upgrade.maxLevel).put("nextCost", cost.format())
                                                .put("maxed", maxed).put("affordable", !maxed && amount > 0 && game.money >= cost))
                                        }
                                        val shopJson = org.json.JSONArray()
                                        shopDefinitions.forEach { item ->
                                            val owned = item.id in game.shopPurchases
                                            shopJson.put(JSONObject().put("id", item.id).put("name", item.name).put("description", item.description)
                                                .put("cost", item.nebulaCost).put("category", item.category).put("cosmetic", item.isCosmetic)
                                                .put("owned", owned).put("equipped", game.equippedCosmeticId == item.id)
                                                .put("affordable", game.nebula >= BigNumber(item.nebulaCost)))
                                        }
                                        val talentsJson = org.json.JSONArray()
                                        talentDefinitions.forEach { talent ->
                                            val owned = talent.id in game.prestigeShopPurchases
                                            val canUnlock = talent.requiredParentId == null || talent.requiredParentId in game.prestigeShopPurchases
                                            val affordable = game.prestige >= BigNumber(talent.cost)
                                            talentsJson.put(JSONObject().put("id", talent.id).put("name", talent.name).put("description", talent.description)
                                                .put("cost", talent.cost).put("owned", owned).put("canUnlock", canUnlock).put("affordable", affordable)
                                                .put("requirement", if (!canUnlock) "Unlock ${talent.requiredParentId} first" else if (!affordable) "Need ${talent.cost} Prestige" else "Ready to unlock"))
                                        }
                                        val viewState = JSONObject()
                                            .put("buyMultiplier", game.buyMultiplier)
                                            .put("prestigeRequirement", progressionEngine.calculatePrestigeRequirement(game.prestigeCount).format())
                                            .put("prestigeReward", progressionEngine.calculatePrestigeReward(game.money, game.prestigeCount).format())
                                            .put("canPrestige", game.money >= progressionEngine.calculatePrestigeRequirement(game.prestigeCount) && progressionEngine.calculatePrestigeReward(game.money, game.prestigeCount) > BigNumber.ZERO)
                                            .put("ultraRequirement", progressionEngine.calculateUltraRequirement(game.ultraCount).format())
                                            .put("ultraReward", progressionEngine.calculateUltraReward(game.money, game.ultraCount).format())
                                            .put("canUltra", game.money >= progressionEngine.calculateUltraRequirement(game.ultraCount) && game.prestige >= BigNumber(1_000) && progressionEngine.calculateUltraReward(game.money, game.ultraCount) > BigNumber.ZERO)
                                            .put("settings", JSONObject().put("musicEnabled", game.settings.musicEnabled).put("soundEnabled", game.settings.soundEnabled).put("vibrationEnabled", game.settings.vibrationEnabled).put("reducedMotion", game.settings.reducedMotion).put("reduceFlashes", game.settings.reduceFlashes))
                                            .put("uiStrings", JSONObject().apply { listOf("stats", "statistics", "arcade", "arcade_title", "minigame_quick_guess_name", "minigame_quick_guess_desc", "minigame_reaction_test_name", "minigame_reaction_test_desc", "minigame_lucky_number_name", "minigame_lucky_number_desc", "minigame_memory_match_name", "minigame_memory_match_desc", "minigame_number_rush_name", "minigame_number_rush_desc", "stats_total_guesses", "stats_correct_guesses", "stats_failed_guesses", "stats_prestige_resets", "stats_ultra_resets", "stats_total_playtime", "guessing_precision_heading", "inspector_money_earned", "inspector_money_spent", "inspector_nebula_earned", "inspector_arcade_played", "inspector_current_streak", "inspector_best_streak", "available_money_heading", "multiplier_format", "guess_button", "play_minigame_format", "claim_reward_button", "close_button", "guess_number_target_format", "score_format", "earned_minigame_format", "reaction_test_title", "reaction_time_format", "lucky_number_title", "lucky_number_desc", "jackpot_win_format", "memory_match_title", "memory_correct", "memory_incorrect_format", "number_rush_title", "staking_flip_button", "status_playing", "status_paused", "correct_guess", "too_low", "too_high", "loading_save_data", "cancel", "close", "inspector_money", "inspector_buy_multiplier", "guess_prompt", "duration_seconds_format", "duration_ms_format", "duration_hms_format", "play", "random_event_active", "settings", "game_preferences", "background_music", "sound_effects", "vibration_label", "reduced_motion_label", "reduce_flashes_label", "settings_desc_sound_fx", "settings_desc_vibration", "reduced_motion_description", "settings_desc_reduce_flashes", "game_temporarily_unavailable", "settings_synced_status", "stake_invalid_amount", "stake_flipping_status", "lucky_stake_title", "minigame_catalog_unavailable", "action_in_progress", "action_saving_reward", "reaction_wait", "reaction_tap_now", "action_failed_retry").forEach { key -> put(key, localeManager.getString(key)) } })
                                            .put("upgrades", upgradesJson)
                                            .put("shopItems", shopJson)
                                            .put("rangeMin", (result?.rangeMin ?: game.currentRangeMin).toString())
                                            .put("rangeMax", (result?.rangeMax ?: game.currentRangeMax).toString())
                                            .put("attempts", result?.attempts ?: game.attempts)
                                            .put("correctGuesses", result?.correctGuesses ?: game.correctGuesses)
                                            .put("streak", result?.streak ?: game.streak)
                                            .put("bestStreak", result?.bestStreak ?: game.bestStreak)
                                            .put("accepted", result?.accepted ?: actionAccepted)
                                            .put("money", game.money.format())
                                            .put("incomePerSecond", currentIncome.format())

                                            .put("prestige", game.prestige.format())
                                            .put("ultra", game.ultra.format())
                                            .put("nebula", game.nebula.format())
                                            .put("prestigeCount", game.prestigeCount)
                                            .put("ultraCount", game.ultraCount)
                                            .put("activeWorldId", game.activeWorldId)
                                            .put("talents", talentsJson)
                                            .put("activeMutators", org.json.JSONArray(game.activeMutators.toList()))
                                            .put("unlockedWorldCount", game.unlockedWorldIds.size)
                                            .put("defeatedBossCount", game.defeatedBossIds.size)
                                            .put("worldBossVictories", game.worldBossVictories)
                                            .put("homeBaseLevel", game.homeBaseLevel)
                                            .put("relicInventory", org.json.JSONArray(game.relicInventory.toList()))
                                            .put("equippedRelicIds", org.json.JSONArray(game.equippedRelicIds.toList()))
                                            .put("codexEntries", org.json.JSONArray(game.codexEntries.toList()))
                                            .put("codexRewardClaimed", game.codexRewardClaimed)
                                            .put("endlessRiftBestTier", game.endlessRiftBestTier)
                                            .put("dailyQuestGuesses", game.dailyQuestGuesses)
                                            .put("dailyQuestCorrectGuesses", game.dailyQuestCorrectGuesses)
                                            .put("dailyQuestBestStreak", game.dailyQuestBestStreak)
                                            .put("claimedDailyQuestCount", game.claimedDailyQuests.size)
                                            .put("claimedDailyQuests", org.json.JSONArray(game.claimedDailyQuests.toList()))
                                            .put("dailyQuestUpgradePurchases", game.dailyQuestUpgradePurchases)
                                            .put("lootCommonChests", game.lootCommonChests)
                                            .put("lootRareChests", game.lootRareChests)
                                            .put("lootEpicChests", game.lootEpicChests)
                                            .put("activeBossBattle", activeBossWorld != null)
                                            .put("activeBossName", activeBossWorld?.bossFallback ?: "")
                                            .put("activeBossHpTotal", activeBossHp)
                                            .put("activeBossHpRemaining", activeBossHp - bossDamage)
                                            .put("worlds", worldsJson)
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
                                        if (result != null) viewState.put("lastGuessStatus", result.status ?: "rate_limited")
                                        replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id).put("type", "state").put("state", viewState).toString())
                                    }
                                }
                            }
                        }
                    })
                }
                loadUrl("$WEB_UI_ORIGIN${WEB_UI_PATH}index.html")
            }
        }
    )

    bridgeErrorDetails?.let { details ->
        AlertDialog(
            onDismissRequest = { bridgeErrorDetails = null },
            title = { Text("Bridge error detected") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())
                ) {
                    Text("The web UI request failed in the Android bridge. Copy the details below to share the error and Java/Kotlin logs.")
                    SelectionContainer {
                        Text(
                            text = details,
                            modifier = Modifier.heightIn(min = 48.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { clipboardManager.setText(AnnotatedString(details)) }) {
                    Text("Copy error + logs")
                }
            },
            dismissButton = {
                TextButton(onClick = { bridgeErrorDetails = null }) { Text("Close") }
            }
        )
    }
}

private fun errorResponse(id: String, code: String): String = JSONObject().put("version", 1).put("id", id).put("type", "error").put("error", code).toString()
