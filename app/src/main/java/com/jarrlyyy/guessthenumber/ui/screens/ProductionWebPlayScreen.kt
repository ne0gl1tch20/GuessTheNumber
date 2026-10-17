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

private const val WEB_UI_ORIGIN = "https://appassets.androidplatform.net"
private const val WEB_UI_PATH = "/assets/web-play/"

@Composable
fun ProductionWebPlayScreen(
    state: GameState,
    incomePerSecond: BigNumber,
    stateProvider: () -> GameState,
    onSubmitGuess: (Long) -> GuessSubmissionResult
) {
    val requestIds = remember { WebUiRequestIdWindow() }
    val currentGameState by rememberUpdatedState(state)
    val currentIncome by rememberUpdatedState(incomePerSecond)
    val readState by rememberUpdatedState(stateProvider)
    val submitGuess by rememberUpdatedState(onSubmitGuess)
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
                                        val result = if (parsed.type == "submit_guess" && parsed.guess != null) try { submitGuess(parsed.guess) } catch (_: Exception) { null } else null
                                        if (parsed.type == "submit_guess" && result == null) { replyProxy.postMessage(errorResponse(parsed.id, "guess_processing_failed")); return@post }
                                        val game = readState()
                                        val activeBossWorld = game.activeBossBattleWorldId?.let { id -> worldDefinitions.firstOrNull { it.id == id } }
                                        val bossDamage = activeBossWorld?.let { (game.bossBattleProgress[it.bossId] ?: 0).coerceIn(0, it.hp) } ?: 0
                                        val viewState = JSONObject()
                                            .put("rangeMin", result?.rangeMin ?: game.currentRangeMin)
                                            .put("rangeMax", result?.rangeMax ?: game.currentRangeMax)
                                            .put("attempts", result?.attempts ?: game.attempts)
                                            .put("correctGuesses", result?.correctGuesses ?: game.correctGuesses)
                                            .put("streak", result?.streak ?: game.streak)
                                            .put("bestStreak", result?.bestStreak ?: game.bestStreak)
                                            .put("accepted", result?.accepted ?: true)
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
                                            .put("lootCommonChests", game.lootCommonChests)
                                            .put("lootRareChests", game.lootRareChests)
                                            .put("lootEpicChests", game.lootEpicChests)
                                            .put("activeBossBattle", activeBossWorld != null)
                                            .put("activeBossName", activeBossWorld?.bossFallback ?: "")
                                            .put("activeBossHpTotal", activeBossWorld?.hp ?: 0)
                                            .put("activeBossHpRemaining", activeBossWorld?.let { it.hp - bossDamage } ?: 0)
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
