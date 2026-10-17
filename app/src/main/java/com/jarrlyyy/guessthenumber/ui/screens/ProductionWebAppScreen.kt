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
import com.jarrlyyy.guessthenumber.data.repository.WorldConfigRepository
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.viewmodel.GuessSubmissionResult
import org.json.JSONArray
import org.json.JSONObject

private const val WEB_APP_ORIGIN = "https://appassets.androidplatform.net"
private const val WEB_APP_PATH = "/assets/web-app/index.html"

@Composable
fun ProductionWebAppScreen(
    state: GameState,
    incomePerSecond: BigNumber,
    stateProvider: () -> GameState,
    onSubmitGuess: (Long) -> GuessSubmissionResult,
    onClaimDailyQuest: (String) -> Unit,
    onOpenLootChest: (String) -> Unit,
    onWorldAction: (String, String) -> Unit
) {
    val requestIds = remember { WebUiRequestIdWindow() }
    val readState by rememberUpdatedState(stateProvider)
    val submitGuess by rememberUpdatedState(onSubmitGuess)
    val claimDailyQuest by rememberUpdatedState(onClaimDailyQuest)
    val openLootChest by rememberUpdatedState(onOpenLootChest)
    val worldAction by rememberUpdatedState(onWorldAction)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val supportsMessageListener = remember { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) }

    DisposableEffect(Unit) {
        onDispose { webViewRef?.let { it.stopLoading(); it.destroy() } }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val worldDefinitions = WorldConfigRepository(context).loadWorlds()
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
                        return !(uri.scheme == "https" && uri.host == "appassets.androidplatform.net" && uri.path == WEB_APP_PATH && uri.query == null && uri.fragment != null)
                    }
                }
                if (supportsMessageListener) {
                    WebViewCompat.addWebMessageListener(this, "gtnHost", setOf(WEB_APP_ORIGIN), object : WebViewCompat.WebMessageListener {
                        override fun onPostMessage(view: WebView, message: WebMessageCompat, sourceOrigin: Uri, isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy) {
                            view.post {
                                if (!WebUiBridgeProtocol.isAllowedSource(isMainFrame, sourceOrigin, view.url, WEB_APP_PATH)) {
                                    replyProxy.postMessage(errorResponse("", "origin_not_allowed")); return@post
                                }
                                when (val parsed = WebUiBridgeProtocol.parse(message.data ?: "")) {
                                    is WebUiBridgeProtocol.ParseResult.Invalid -> replyProxy.postMessage(errorResponse(parsed.id, parsed.error))
                                    is WebUiBridgeProtocol.ParseResult.Valid -> {
                                        if (!requestIds.markIfNew(parsed.id)) { replyProxy.postMessage(errorResponse(parsed.id, "duplicate_request")); return@post }
                                        val result = if (parsed.type == "submit_guess" && parsed.guess != null) try { submitGuess(parsed.guess) } catch (_: Exception) { null } else null
                                        if (parsed.type == "submit_guess" && result == null) { replyProxy.postMessage(errorResponse(parsed.id, "guess_processing_failed")); return@post }
                                        try {
                                            when (parsed.type) {
                                                "claim_daily_quest" -> parsed.questId?.let(claimDailyQuest)
                                                "open_loot_chest" -> parsed.tier?.let(openLootChest)
                                                "world_action" -> if (parsed.worldId != null && parsed.worldAction != null) worldAction(parsed.worldId, parsed.worldAction)
                                                "navigate", "read_state", "submit_guess" -> Unit
                                            }
                                        } catch (_: Exception) {
                                            replyProxy.postMessage(errorResponse(parsed.id, "action_processing_failed")); return@post
                                        }
                                        val game = readState()
                                        val activeBossWorld = game.activeBossBattleWorldId?.let { id -> worldDefinitions.firstOrNull { it.id == id } }
                                        val activeBossHp = activeBossWorld?.let { it.hp + if (game.endlessRiftActive) ((game.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0 } ?: 0
                                        val bossDamage = activeBossWorld?.let { (game.bossBattleProgress[it.bossId] ?: 0).coerceIn(0, activeBossHp) } ?: 0
                                        val totalUpgradeLevels = game.upgradeLevels.values.sum()
                                        val worldsJson = JSONArray()
                                        worldDefinitions.forEach { world ->
                                            val unlocked = world.id in game.unlockedWorldIds
                                            val defeated = world.bossId in game.defeatedBossIds && !game.endlessRiftActive
                                            val canUnlock = world.unlockBossId?.let { required -> required in game.defeatedBossIds && game.correctGuesses >= world.unlockCorrectGuesses && totalUpgradeLevels >= world.unlockUpgradeCount && game.prestigeCount >= world.unlockPrestigeCount && game.ultraCount >= world.unlockUltraCount } ?: true
                                            val canFight = game.endlessRiftActive || (game.correctGuesses >= world.fightCorrectGuesses && totalUpgradeLevels >= world.fightUpgradeCount && game.prestigeCount >= world.fightPrestigeCount && game.ultraCount >= world.fightUltraCount)
                                            val hp = world.hp + if (game.endlessRiftActive) ((game.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0
                                            val damage = (game.bossBattleProgress[world.bossId] ?: 0).coerceIn(0, hp)
                                            worldsJson.put(JSONObject().put("id", world.id).put("name", world.fallback).put("bossName", world.bossFallback).put("hp", hp).put("damage", damage).put("unlocked", unlocked).put("defeated", defeated).put("canUnlock", canUnlock).put("canFight", canFight).put("active", game.activeWorldId == world.id).put("activeBoss", game.activeBossBattleWorldId == world.id))
                                        }
                                        val viewState = JSONObject()
                                            .put("rangeMin", (result?.rangeMin ?: game.currentRangeMin).toString())
                                            .put("rangeMax", (result?.rangeMax ?: game.currentRangeMax).toString())
                                            .put("attempts", result?.attempts ?: game.attempts)
                                            .put("correctGuesses", result?.correctGuesses ?: game.correctGuesses)
                                            .put("streak", result?.streak ?: game.streak)
                                            .put("bestStreak", result?.bestStreak ?: game.bestStreak)
                                            .put("money", game.money.format())
                                            .put("incomePerSecond", incomePerSecond.format())
                                            .put("prestige", game.prestige.format())
                                            .put("ultra", game.ultra.format())
                                            .put("nebula", game.nebula.format())
                                            .put("prestigeCount", game.prestigeCount)
                                            .put("ultraCount", game.ultraCount)
                                            .put("homeBaseLevel", game.homeBaseLevel)
                                            .put("endlessRiftBestTier", game.endlessRiftBestTier)
                                            .put("unlockedWorldCount", game.unlockedWorldIds.size)
                                            .put("defeatedBossCount", game.defeatedBossIds.size)
                                            .put("worldBossVictories", game.worldBossVictories)
                                            .put("activeWorldId", game.activeWorldId)
                                            .put("activeBossBattle", activeBossWorld != null)
                                            .put("activeBossName", activeBossWorld?.bossFallback ?: "")
                                            .put("activeBossHpTotal", activeBossHp)
                                            .put("activeBossHpRemaining", activeBossHp - bossDamage)
                                            .put("dailyQuestGuesses", game.dailyQuestGuesses)
                                            .put("dailyQuestCorrectGuesses", game.dailyQuestCorrectGuesses)
                                            .put("dailyQuestBestStreak", game.dailyQuestBestStreak)
                                            .put("claimedDailyQuestCount", game.claimedDailyQuests.size)
                                            .put("worlds", worldsJson)
                                            .put("achievementCorrectProgress", game.correctGuesses.coerceIn(0L, 10L))
                                            .put("achievementStreakProgress", game.bestStreak.coerceIn(0, 5))
                                            .put("achievementMoneyProgress", (game.money.value.toDouble() / 1_000_000.0).coerceIn(0.0, 1.0))
                                        if (result != null) viewState.put("lastGuessStatus", result.status ?: "rate_limited")
                                        replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id).put("type", "state").put("state", viewState).toString())
                                    }
                                }
                            }
                        }
                    })
                }
                loadUrl("$WEB_APP_ORIGIN$WEB_APP_PATH")
            }
        }
    )
}

private fun errorResponse(id: String, code: String): String = JSONObject().put("version", 1).put("id", id).put("type", "error").put("error", code).toString()
