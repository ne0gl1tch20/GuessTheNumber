package com.jarrlyyy.guessthenumber.ui.screens

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.ui.viewmodel.GuessSubmissionResult
import com.jarrlyyy.guessthenumber.data.repository.WorldConfigRepository
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository

private const val WEB_UI_ORIGIN = "https://appassets.androidplatform.net"
private const val WEB_UI_PATH = "/assets/web-play/"

@Composable
fun ProductionWebPlayScreen(
    state: GameState,
    incomePerSecond: BigNumber,
    stateProvider: () -> GameState,
    onSubmitGuess: (Long) -> GuessSubmissionResult,
    onClaimDailyQuest: (String) -> Unit,
    onOpenLootChest: (String) -> Unit,
    onNavigateProgression: (String) -> Unit,
    onWorldAction: (String, String) -> Unit,
    onBuyUpgrade: (String, BigNumber) -> Unit,
    onBuyShopItem: (String, Long) -> Unit,
    onEquipCosmetic: (String) -> Unit,
    onUpdateMultiplier: (String) -> Unit
) {
    val requestIds = remember { WebUiRequestIdWindow() }
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
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
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
                                        val beforeAction = readState()
                                        val result = if (parsed.type == "submit_guess" && parsed.guess != null) try { submitGuess(parsed.guess) } catch (_: Exception) { null } else null
                                        if (parsed.type == "submit_guess" && result == null) { replyProxy.postMessage(errorResponse(parsed.id, "guess_processing_failed")); return@post }
                                        try {
                                            when (parsed.type) {
                                                "claim_daily_quest" -> parsed.questId?.let(claimDailyQuest)
                                                "open_loot_chest" -> parsed.tier?.let(openLootChest)
                                                "navigate" -> parsed.route?.let(navigateProgression)
                                                "world_action" -> if (parsed.worldId != null && parsed.worldAction != null) dispatchWorldAction(parsed.worldId, parsed.worldAction)
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
                                        } catch (_: Exception) {
                                            replyProxy.postMessage(errorResponse(parsed.id, "action_processing_failed")); return@post
                                        }
                                        val game = readState()
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
                                        val viewState = JSONObject()
                                            .put("buyMultiplier", game.buyMultiplier)
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
                                            .put("unlockedWorldCount", game.unlockedWorldIds.size)
                                            .put("defeatedBossCount", game.defeatedBossIds.size)
                                            .put("worldBossVictories", game.worldBossVictories)
                                            .put("homeBaseLevel", game.homeBaseLevel)
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
}

private fun errorResponse(id: String, code: String): String = JSONObject().put("version", 1).put("id", id).put("type", "error").put("error", code).toString()
