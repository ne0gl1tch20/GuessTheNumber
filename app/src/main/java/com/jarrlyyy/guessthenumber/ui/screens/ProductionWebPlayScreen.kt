package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.BuildConfig
import android.net.Uri
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.jarrlyyy.guessthenumber.data.repository.UpdateChannelChecker
import com.jarrlyyy.guessthenumber.data.repository.ApkUpdateInstaller
import com.jarrlyyy.guessthenumber.data.repository.WebPlayAssetSyncer
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
import com.jarrlyyy.guessthenumber.data.audio.BackgroundMusicManager
import com.jarrlyyy.guessthenumber.traits.BuiltInTraits
import com.jarrlyyy.guessthenumber.traits.ActiveTraitCatalog
import com.jarrlyyy.guessthenumber.traits.TraitGameplayCatalog
import com.jarrlyyy.guessthenumber.traits.TraitActivationContext
import com.jarrlyyy.guessthenumber.traits.TraitActivationEngine
import com.jarrlyyy.guessthenumber.traits.TraitRewardPolicy

/** Debug-only WebView console shared with Developer Settings. */
object WebViewDebugConsole {
    val entries = androidx.compose.runtime.mutableStateListOf<String>()
    var evaluator: ((String) -> Unit)? = null
    fun add(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        entries.add("[$timestamp] $message")
        while (entries.size > 300) entries.removeAt(0)
    }
    fun clear() = entries.clear()
    fun runJavascript(source: String) {
        val activeEvaluator = evaluator
        if (activeEvaluator == null) add("WARN: WebView is not currently attached") else activeEvaluator(source)
    }
}
private const val WEB_UI_ORIGIN = "https://appassets.androidplatform.net"
private const val WEB_UI_PATH = "/assets/web-play/"

@Composable
fun ProductionWebPlayScreen(
    state: GameState,
    dailyRewardTestRequest: Int = 0,
    incomePerSecond: BigNumber,
    localeManager: LocaleManager,
    stateProvider: () -> GameState,
    offlineGains: BigNumber?,
    onDismissOfflineGains: () -> Unit,
    onClaimDailyReward: () -> Boolean,
    onSubmitGuess: (Long) -> GuessSubmissionResult,
    onClaimDailyQuest: (String) -> Unit,
    onOpenLootChest: (String) -> Unit,
    onNavigateProgression: (String) -> Unit,
    onSelectTraitArm: (String, String) -> Boolean,
    onWorldAction: (String, String) -> Unit,
    onBuyUpgrade: (String, BigNumber) -> Unit,
    onBuyShopItem: (String, Long) -> Unit,
    onBuyPrestigeUpgrade: (String, BigNumber) -> Unit,
    onBuyPrestigeShopItem: (String, Long) -> Unit,
    onBuyUltraUpgrade: (String, BigNumber) -> Unit,
    onBuyUltraShopItem: (String, Long) -> Unit,
    onEquipCosmetic: (String) -> Unit,
    onUpdateMultiplier: (String) -> Unit,
    onUpdateSettings: (GameSettings) -> Unit,
    onPrestigeReset: () -> Unit,
    onUltraReset: () -> Unit,
    onBuyTalent: (String, Long) -> Unit,
    onSetMutatorActive: (String, Boolean) -> Unit,
    onArcadeReward: (String, Int) -> Boolean,
    onSettleStake: (String) -> Boolean,
    onResetData: () -> Unit
) {
    val requestIds = remember { WebUiRequestIdWindow() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val updateScope = rememberCoroutineScope()
    val webPlayAssetSyncer = remember(context) { WebPlayAssetSyncer(context) }
    var latestAvailableUpdate by remember { mutableStateOf<UpdateChannelChecker.AvailableUpdate?>(null) }
    val progressionEngine = remember { GameEngine() }
    val currentGameState by rememberUpdatedState(state)
    val activeLocaleManager by rememberUpdatedState(localeManager)
    val currentIncome by rememberUpdatedState(incomePerSecond)
    val readState by rememberUpdatedState(stateProvider)
    val currentOfflineGains by rememberUpdatedState(offlineGains)
    val dismissOfflineGains by rememberUpdatedState(onDismissOfflineGains)
    val claimDailyReward by rememberUpdatedState(onClaimDailyReward)
    val submitGuess by rememberUpdatedState(onSubmitGuess)
    val claimDailyQuest by rememberUpdatedState(onClaimDailyQuest)
    val openLootChest by rememberUpdatedState(onOpenLootChest)
    val navigateProgression by rememberUpdatedState(onNavigateProgression)
    val selectTraitArm by rememberUpdatedState(onSelectTraitArm)
    val dispatchWorldAction by rememberUpdatedState(onWorldAction)
    val buyUpgrade by rememberUpdatedState(onBuyUpgrade)
    val buyShopItem by rememberUpdatedState(onBuyShopItem)
    val buyPrestigeUpgrade by rememberUpdatedState(onBuyPrestigeUpgrade)
    val buyPrestigeShopItem by rememberUpdatedState(onBuyPrestigeShopItem)
    val buyUltraUpgrade by rememberUpdatedState(onBuyUltraUpgrade)
    val buyUltraShopItem by rememberUpdatedState(onBuyUltraShopItem)
    val equipCosmetic by rememberUpdatedState(onEquipCosmetic)
    val updateMultiplier by rememberUpdatedState(onUpdateMultiplier)
    val updateSettings by rememberUpdatedState(onUpdateSettings)
    val buyTalent by rememberUpdatedState(onBuyTalent)
    val setMutatorActive by rememberUpdatedState(onSetMutatorActive)
    val awardArcadeReward by rememberUpdatedState(onArcadeReward)
    val settleStake by rememberUpdatedState(onSettleStake)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var rollbackAttempted by remember { mutableStateOf(false) }
    val musicManager = remember { BackgroundMusicManager(context.applicationContext) }
    LaunchedEffect(state.settings.volume) { musicManager.setVolume(state.settings.volume) }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { musicManager.importTrack(uri)?.let { musicManager.playTrack(it.id) } }
        webViewRef?.post { webViewRef?.evaluateJavascript("window.gtnReadState && window.gtnReadState()", null) }
    }
    LaunchedEffect(state.settings.customPrimaryColor, state.settings.customSecondaryColor, state.settings.customTertiaryColor) {
        webViewRef?.postDelayed({
            webViewRef?.evaluateJavascript("window.gtnReadState && window.gtnReadState()", null)
        }, 120L)
    }
    var bridgeErrorDetails by remember { mutableStateOf<String?>(null) }
    var offlineRewardDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(offlineGains) { if (offlineGains != null) offlineRewardDismissed = false }
    val clipboardManager = LocalClipboardManager.current
    fun reportBridgeException(action: String, requestId: String, error: Exception) {
        Log.e("GTNWebBridge", "Bridge request failed: action=$action requestId=$requestId", error)
        if (BuildConfig.DEBUG) WebViewDebugConsole.add("Kotlin bridge error ($action/$requestId): ${error.javaClass.simpleName}: ${error.message}")
        val trace = Log.getStackTraceString(error).take(12000)
        bridgeErrorDetails = "Guess The Number bridge error\nAction: $action\nRequest ID: $requestId\nException: ${error.javaClass.name}: ${error.message ?: "(no message)"}\n\nJava/Kotlin stack trace:\n$trace"
    }
    val supportsMessageListener = remember { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) }

    DisposableEffect(Unit) {
        onDispose {
            WebViewDebugConsole.evaluator = null
            webViewRef?.let { it.stopLoading(); it.destroy() }
        }
    }

    LaunchedEffect(dailyRewardTestRequest) {
        if (dailyRewardTestRequest > 0) {
            webViewRef?.postDelayed({
                webViewRef?.evaluateJavascript("window.gtnShowDailyRewardTest && window.gtnShowDailyRewardTest()", null)
            }, 150L)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val worldDefinitions = WorldConfigRepository(context).loadWorlds()
            val configRepository = JsonConfigRepository(context)
            val upgradeDefinitions = configRepository.loadUpgrades()
            val shopDefinitions = configRepository.loadShopItems()
            val prestigeUpgradeDefinitions = configRepository.loadPrestigeUpgrades()
            val prestigeShopDefinitions = configRepository.loadPrestigeShopItems()
            val ultraUpgradeDefinitions = configRepository.loadUltraUpgrades()
            val ultraShopDefinitions = configRepository.loadUltraShopItems()
            val talentDefinitions = configRepository.loadTalents()
            val minigameDefinitions = configRepository.loadMinigames()
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/web-play/", webPlayAssetSyncer.pathHandler("web-play"))
                .addPathHandler("/assets/fonts/", webPlayAssetSyncer.pathHandler("fonts"))
                .addPathHandler("/assets/locales/", webPlayAssetSyncer.pathHandler("locales"))
                .addPathHandler("/assets/libraries/", webPlayAssetSyncer.pathHandler("libraries"))
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()
            WebView(context).apply {
                webViewRef = this
                if (BuildConfig.DEBUG) {
                    WebView.setWebContentsDebuggingEnabled(true)
                    WebViewDebugConsole.evaluator = { source ->
                        post { evaluateJavascript(source) { result -> WebViewDebugConsole.add("JS result: $result") } }
                    }
                    WebViewDebugConsole.add("WebView attached; Chrome DevTools debugging enabled")
                }
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
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                        if (BuildConfig.DEBUG) WebViewDebugConsole.add("${message.messageLevel()}: ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        if (url == "$WEB_UI_ORIGIN${WEB_UI_PATH}index.html") {
                            view.evaluateJavascript("(typeof window.gtnReadState === 'function')") { result ->
                                if (result != "true" && !rollbackAttempted) {
                                    rollbackAttempted = true
                                    if (webPlayAssetSyncer.rollbackToPrevious()) {
                                        view.post { view.loadUrl("$WEB_UI_ORIGIN${WEB_UI_PATH}index.html") }
                                    }
                                }
                            }
                        }
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                        if (request.isForMainFrame && !rollbackAttempted) {
                            rollbackAttempted = true
                            if (webPlayAssetSyncer.rollbackToPrevious()) {
                                view.post { view.loadUrl("$WEB_UI_ORIGIN${WEB_UI_PATH}index.html") }
                            }
                        }
                    }
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
                                if (BuildConfig.DEBUG) WebViewDebugConsole.add("Bridge <- ${message.data.orEmpty().take(1000)}")
                                when (val parsed = WebUiBridgeProtocol.parse(message.data ?: "")) {
                                    is WebUiBridgeProtocol.ParseResult.Invalid -> replyProxy.postMessage(errorResponse(parsed.id, parsed.error))
                                    is WebUiBridgeProtocol.ParseResult.Valid -> {
                                        if (!requestIds.markIfNew(parsed.id)) { replyProxy.postMessage(errorResponse(parsed.id, "duplicate_request")); return@post }
                                        if (parsed.type == "read_feature_tutorial" || parsed.type == "complete_feature_tutorial") {
                                            val featureId = parsed.itemId.orEmpty()
                                            val prefs = context.applicationContext.getSharedPreferences("web_play_onboarding", android.content.Context.MODE_PRIVATE)
                                            val completed = parsed.type == "complete_feature_tutorial"
                                            if (completed) prefs.edit().putBoolean("nimmy_feature_$featureId", true).apply()
                                            replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id).put("type", "feature_tutorial_state").put("featureId", featureId).put("completed", completed || prefs.getBoolean("nimmy_feature_$featureId", false)).toString())
                                            return@post
                                        }
                                        if (parsed.type == "reset_everything") {
                                            onResetData(); replyProxy.postMessage(JSONObject().put("version",1).put("id",parsed.id).put("type","state").put("state",JSONObject().put("accepted",true)).toString())
                                            webViewRef?.postDelayed({ context.packageManager.getLaunchIntentForPackage(context.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)?.let(context::startActivity) },1600L)
                                            return@post
                                        }
                                        if (parsed.type == "read_onboarding_state") {
                                            val onboardingPrefs = context.applicationContext.getSharedPreferences("web_play_onboarding", android.content.Context.MODE_PRIVATE)
                                            val firstLaunchAlreadySeen = onboardingPrefs.getBoolean("daily_reward_first_launch_seen", false)
                                            if (!firstLaunchAlreadySeen) onboardingPrefs.edit().putBoolean("daily_reward_first_launch_seen", true).apply()
                                            val tutorialCompleted = onboardingPrefs.getBoolean("nimmy_tutorial_completed", false)
                                            replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id)
                                                .put("type", "onboarding_state")
                                                .put("dailyRewardFirstLaunchSeen", firstLaunchAlreadySeen)
                                                .put("nimmyTutorialCompleted", tutorialCompleted).toString())
                                            return@post
                                        }
                                        if (parsed.type == "complete_nimmy_tutorial") {
                                            context.applicationContext.getSharedPreferences("web_play_onboarding", android.content.Context.MODE_PRIVATE)
                                                .edit().putBoolean("nimmy_tutorial_completed", true).apply()
                                            replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id)
                                                .put("type", "onboarding_saved").toString())
                                            return@post
                                        }
                                        if (parsed.type == "read_profile") {
                                            updateScope.launch {
                                                val storedProfile = runCatching {
                                                    PlayerProfileStore(context.applicationContext).profile.first()
                                                }.getOrNull()
                                                val response = JSONObject().put("version", 1).put("id", parsed.id)
                                                    .put("type", "profile_state")
                                                    .put("username", storedProfile?.username.orEmpty())
                                                    .put("bio", storedProfile?.bio.orEmpty())
                                                    .put("avatarId", storedProfile?.avatarId ?: "star")
                                                    .put("customAvatarData", if (storedProfile?.avatarId == "custom" && !storedProfile.customAvatarPath.isNullOrBlank()) runCatching { val file=java.io.File(storedProfile.customAvatarPath); val mime=when(file.extension.lowercase()){"png"->"image/png";"webp"->"image/webp";else->"image/jpeg"}; "data:$mime;base64,"+android.util.Base64.encodeToString(file.readBytes(),android.util.Base64.NO_WRAP) }.getOrNull() else null)
                                                    .put("setupComplete", storedProfile?.setupComplete == true)
                                                replyProxy.postMessage(response.toString())
                                            }
                                            return@post
                                        }
                                        if (parsed.type == "read_changelog") {
                                            val markdown = try {
                                                context.assets.open("changelogs.md").bufferedReader(Charsets.UTF_8).use { it.readText() }
                                            } catch (_: Exception) {
                                                replyProxy.postMessage(errorResponse(parsed.id, "changelog_unavailable"))
                                                return@post
                                            }
                                            replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id)
                                                .put("type", "changelog").put("content", markdown).toString())
                                            return@post
                                        }
                                        if (parsed.type == "music_state") {
                                            val musicState = JSONObject()
                                                .put("library", org.json.JSONArray().apply { musicManager.library.value.forEach { track -> put(JSONObject().put("id", track.id).put("title", track.title).put("artist", track.artist).put("album", track.album).put("duration", track.duration)) } })
                                                .put("queue", org.json.JSONArray(musicManager.queue.value))
                                                .put("playlists", JSONObject().apply { musicManager.playlists.value.forEach { (name, ids) -> put(name, org.json.JSONArray(ids)) } })
                                                .put("currentTrackId", musicManager.currentTrackId.value ?: "")
                                                .put("currentPosition", musicManager.currentPosition.value)
                                                .put("duration", musicManager.duration.value)
                                                .put("isPlaying", musicManager.isPlaying.value)
                                                .put("shuffle", musicManager.shuffle.value)
                                                .put("repeatMode", musicManager.repeatMode.value.name)
                                            replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id).put("type", "music_state").put("musicPlayer", musicState).toString())
                                            return@post
                                        }
                                        if (parsed.type in setOf("check_updates", "install_update", "open_update_page")) {
                                            when (parsed.type) {
                                                "check_updates" -> updateScope.launch {
                                                    val response = JSONObject().put("version", 1).put("id", parsed.id).put("type", "update_result").put("installedVersion", BuildConfig.VERSION_NAME).put("installedVersionCode", BuildConfig.VERSION_CODE).put("channel", BuildConfig.UPDATE_CHANNEL)
                                                    when (val updateResult = runCatching { UpdateChannelChecker.check() }.getOrElse { UpdateChannelChecker.Result.Unavailable }) {
                                                        is UpdateChannelChecker.Result.Available -> {
                                                            latestAvailableUpdate = updateResult.update
                                                            response.put("status", "available").put("title", updateResult.update.title).put("details", updateResult.update.details).put("message", activeLocaleManager.getString("update_available", "An update is available."))
                                                        }
                                                        UpdateChannelChecker.Result.UpToDate -> {
                                                            latestAvailableUpdate = null
                                                            response.put("status", "up_to_date").put("message", activeLocaleManager.getString("update_up_to_date", "You're up to date."))
                                                        }
                                                        UpdateChannelChecker.Result.Unavailable -> {
                                                            latestAvailableUpdate = null
                                                            response.put("status", "unavailable").put("message", activeLocaleManager.getString("update_unavailable", "Couldn't check for updates. Try again later."))
                                                        }
                                                        is UpdateChannelChecker.Result.Failed -> {
                                                            latestAvailableUpdate = null
                                                            response.put("status", "unavailable")
                                                                .put("message", activeLocaleManager.getString("update_unavailable", "Couldn't check for updates. Try again later."))
                                                                .put("diagnostics", updateResult.diagnostics)
                                                        }
                                                    }
                                                    replyProxy.postMessage(response.toString())
                                                }
                                                "install_update" -> {
                                                    val update = latestAvailableUpdate
                                                    if (update == null) {
                                                        replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id).put("type", "update_result").put("status", "unavailable").put("message", activeLocaleManager.getString("update_unavailable", "Check for updates first.")).toString())
                                                    } else if (!context.packageManager.canRequestPackageInstalls()) {
                                                        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                                        replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id).put("type", "update_result").put("status", "permission_required").put("message", activeLocaleManager.getString("update_permission_required", "Allow this app to install updates, then return and tap Install again.")).toString())
                                                    } else updateScope.launch {
                                                        val installResult = withContext(Dispatchers.IO) { ApkUpdateInstaller(context).install(update.downloadUrl, requireChecksum = BuildConfig.UPDATE_CHANNEL == "debug", expectedCommitSha = update.expectedCommitSha) }
                                                        val response = JSONObject().put("version", 1).put("id", parsed.id).put("type", "update_result")
                                                        if (installResult.isSuccess) {
                                                            response.put("status", "install_started").put("message", activeLocaleManager.getString("update_install_started", "Installation started. Confirm the Android installer prompt."))
                                                        } else {
                                                            response.put("status", "install_failed").put("message", activeLocaleManager.getString("update_install_failed", "Update failed: %s", installResult.exceptionOrNull()?.message ?: "unknown error"))
                                                        }
                                                        replyProxy.postMessage(response.toString())
                                                    }
                                                }
                                                "open_update_page" -> {
                                                    val url = latestAvailableUpdate?.url
                                                    if (url != null && Uri.parse(url).let { it.scheme == "https" && it.host == "github.com" }) {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                                        replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id).put("type", "update_result").put("status", "page_opened").put("message", activeLocaleManager.getString("update_open", "Opened update page.")).toString())
                                                    } else replyProxy.postMessage(errorResponse(parsed.id, "update_page_unavailable"))
                                                }
                                            }
                                            return@post
                                        }
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
                                                 "claim_daily_reward" -> claimDailyReward()
                                                 "dismiss_offline_gains" -> { dismissOfflineGains(); offlineRewardDismissed = true }
                                                "open_loot_chest" -> parsed.tier?.let(openLootChest)
                                                "navigate" -> parsed.route?.let(navigateProgression)
                                                 "select_trait_arm" -> if (parsed.traitId != null && parsed.armId != null) selectTraitArm(parsed.traitId, parsed.armId) else false
                                                "open_arcade" -> navigateProgression("arcade")
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
                                                "music_action" -> {
                                                    when (parsed.musicAction) {
                                                        "import" -> musicPicker.launch(arrayOf("audio/*"))
                                                        "play" -> musicManager.play()
                                                        "pause" -> musicManager.pause()
                                                        "stop" -> musicManager.stop()
                                                        "next" -> musicManager.next()
                                                        "previous" -> musicManager.previous()
                                                        "shuffle" -> musicManager.setShuffle(!musicManager.shuffle.value)
                                                        "repeat" -> musicManager.setRepeatMode(BackgroundMusicManager.RepeatMode.entries[(musicManager.repeatMode.value.ordinal + 1) % BackgroundMusicManager.RepeatMode.entries.size])
                                                        "play_track" -> musicManager.playTrack(parsed.musicTrackId ?: throw IllegalArgumentException("Missing track"))
                                                        "enqueue" -> musicManager.enqueue(parsed.musicTrackId ?: throw IllegalArgumentException("Missing track"))
                                                        "remove_track" -> musicManager.removeTrack(parsed.musicTrackId ?: throw IllegalArgumentException("Missing track"))
                                                        "add_to_playlist" -> if (!musicManager.addToPlaylist(parsed.musicName ?: throw IllegalArgumentException("Missing playlist"), parsed.musicTrackId ?: throw IllegalArgumentException("Missing track"))) throw IllegalArgumentException("Could not add track to playlist")
                                                        "remove_from_playlist" -> musicManager.removeFromPlaylist(parsed.musicName ?: throw IllegalArgumentException("Missing playlist"), parsed.musicTrackId ?: throw IllegalArgumentException("Missing track"))
                                                        "clear_queue" -> musicManager.clearQueue()
                                                        "remove_queue" -> musicManager.removeFromQueue(parsed.musicIndex ?: throw IllegalArgumentException("Missing queue index"))
                                                        "move_queue" -> musicManager.moveQueue(parsed.musicIndex ?: throw IllegalArgumentException("Missing queue index"), parsed.musicToIndex ?: throw IllegalArgumentException("Missing destination index"))
                                                        "create_playlist" -> if (!musicManager.createPlaylist(parsed.musicName ?: "")) throw IllegalArgumentException("Playlist name is invalid or already used")
                                                        "delete_playlist" -> musicManager.deletePlaylist(parsed.musicName ?: throw IllegalArgumentException("Missing playlist name"))
                                                        "play_playlist" -> musicManager.playQueue(musicManager.playlists.value[parsed.musicName] ?: throw IllegalArgumentException("Playlist not found"))
                                                        "seek" -> musicManager.seekTo(parsed.musicPosition ?: throw IllegalArgumentException("Missing playback position"))
                                                        else -> throw IllegalArgumentException("Unsupported music action")
                                                    }
                                                }
                                                "update_setting" -> {
                                                    val key = parsed.settingKey
                                                    val value = parsed.settingValue
                                                    if (key != null && value != null) {
                                                        val settings = readState().settings
                                                        val updated = when (key) {
                                                            "musicEnabled" -> settings.copy(musicEnabled = value.toBoolean())
                                                            "soundEnabled" -> settings.copy(soundEnabled = value.toBoolean())
                                                            "vibrationEnabled" -> settings.copy(vibrationEnabled = value.toBoolean())
                                                            "notificationsEnabled" -> settings.copy(notificationsEnabled = value.toBoolean())
                                                            "notificationRemindersEnabled" -> settings.copy(notificationRemindersEnabled = value.toBoolean())
                                                            "notificationProgressionEnabled" -> settings.copy(notificationProgressionEnabled = value.toBoolean())
                                                            "notificationEventsEnabled" -> settings.copy(notificationEventsEnabled = value.toBoolean())
                                                            "notificationIntervalHours" -> settings.copy(notificationIntervalHours = value.toLong())
                                                            "themeMode" -> settings.copy(themeMode = value)
                                                            "numberNotation" -> settings.copy(numberNotation = value)
                                                            "locale" -> {
                                                                if (!LocaleManager.supportedLocales.any { it.tag == value }) throw IllegalArgumentException("Unsupported locale")
                                                                settings.copy(locale = value)
                                                            }
                                                            "reducedMotion" -> settings.copy(reducedMotion = value.toBoolean())
                                                            "reduceFlashes" -> settings.copy(reduceFlashes = value.toBoolean())
                                                            "volume" -> settings.copy(volume = value.toFloat())
                                                            "sfxVolume" -> settings.copy(sfxVolume = value.toFloat())
                                                            "themePreset" -> {
                                                                val colors = when (value) {
                                                                    "Neon" -> listOf("#7C4DFF", "#00BCD4", "#00C853")
                                                                    "Sunset" -> listOf("#FF4081", "#FF9800", "#FFC107")
                                                                    "Ocean" -> listOf("#2196F3", "#00BCD4", "#3F51B5")
                                                                    "Forest" -> listOf("#4CAF50", "#009688", "#8BC34A")
                                                                    else -> null
                                                                }
                                                                if (colors == null) settings.copy(themePreset = "Custom")
                                                                else settings.copy(themePreset = value, customPrimaryColor = colors[0], customSecondaryColor = colors[1], customTertiaryColor = colors[2])
                                                            }
                                                            "customPrimaryColor" -> settings.copy(themePreset = "Custom", customPrimaryColor = value)
                                                            "customSecondaryColor" -> settings.copy(themePreset = "Custom", customSecondaryColor = value)
                                                            "customTertiaryColor" -> settings.copy(themePreset = "Custom", customTertiaryColor = value)
                                                            else -> throw IllegalArgumentException("Unsupported setting")
                                                        }
                                                        updateSettings(updated)
                                                        if (key == "locale") {
                                                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(value))
                                                            updateScope.launch {
                                                                activeLocaleManager.loadLocaleForTag(value)
                                                                webViewRef?.postDelayed({
                                                                    webViewRef?.evaluateJavascript("window.gtnReadState && window.gtnReadState()", null)
                                                                }, 120L)
                                                            }
                                                        }
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
                                                "buy_prestige_upgrade" -> parsed.itemId?.let { id ->
                                                    val upgrade = prestigeUpgradeDefinitions.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Unknown Prestige upgrade")
                                                    buyPrestigeUpgrade(id, BigNumber(upgrade.baseCost))
                                                }
                                                "buy_prestige_shop_item" -> parsed.itemId?.let { id ->
                                                    val item = prestigeShopDefinitions.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Unknown Prestige shop item")
                                                    buyPrestigeShopItem(id, item.cost)
                                                }
                                                "buy_ultra_upgrade" -> parsed.itemId?.let { id ->
                                                    val upgrade = ultraUpgradeDefinitions.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Unknown Ultra upgrade")
                                                    buyUltraUpgrade(id, BigNumber(upgrade.baseCost))
                                                }
                                                "buy_ultra_shop_item" -> parsed.itemId?.let { id ->
                                                    val item = ultraShopDefinitions.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Unknown Ultra shop item")
                                                    buyUltraShopItem(id, item.cost)
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
                                        try {
                                            val game = try {
                                                readState()
                                            } catch (exception: Exception) {
                                            reportBridgeException(parsed.type, parsed.id, exception)
                                            replyProxy.postMessage(errorResponse(parsed.id, "state_read_failed"))
                                            return@post
                                        }
                                        val actionAccepted = when (parsed.type) {
                                            "claim_daily_quest" -> parsed.questId != null && parsed.questId in game.claimedDailyQuests && parsed.questId !in beforeAction.claimedDailyQuests
                                             "claim_daily_reward" -> game.lastDailyRewardDate != beforeAction.lastDailyRewardDate
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
                                            val requiredBossDefeated = world.unlockBossId?.let { it in game.defeatedBossIds } ?: true
                                            val canUnlock = requiredBossDefeated && game.correctGuesses >= world.unlockCorrectGuesses && totalUpgradeLevels >= world.unlockUpgradeCount && game.prestigeCount >= world.unlockPrestigeCount && game.ultraCount >= world.unlockUltraCount
                                            val canFight = game.endlessRiftActive || (game.correctGuesses >= world.fightCorrectGuesses && totalUpgradeLevels >= world.fightUpgradeCount && game.prestigeCount >= world.fightPrestigeCount && game.ultraCount >= world.fightUltraCount)
                                            val hp = world.hp + if (game.endlessRiftActive) ((game.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0
                                            val damage = (game.bossBattleProgress[world.bossId] ?: 0).coerceIn(0, hp)
                                            worldsJson.put(JSONObject().put("id", world.id).put("name", world.fallback).put("bossId", world.bossId).put("bossName", world.bossFallback).put("hp", hp).put("damage", damage).put("unlocked", unlocked).put("defeated", defeated).put("canUnlock", canUnlock).put("canFight", canFight).put("active", game.activeWorldId == world.id).put("activeBoss", game.activeBossBattleWorldId == world.id)
                                                .put("unlockRequirements", org.json.JSONArray().apply {
                                                    world.unlockBossId?.let { requiredBossId ->
                                                        if (requiredBossId !in game.defeatedBossIds) put("Defeat ${worldDefinitions.firstOrNull { it.bossId == requiredBossId }?.bossFallback ?: requiredBossId}")
                                                    }
                                                    if (game.correctGuesses < world.unlockCorrectGuesses) put("Correct guesses: ${game.correctGuesses} / ${world.unlockCorrectGuesses}")
                                                    if (totalUpgradeLevels < world.unlockUpgradeCount) put("Upgrade levels: $totalUpgradeLevels / ${world.unlockUpgradeCount}")
                                                    if (game.prestigeCount < world.unlockPrestigeCount) put("Prestige resets: ${game.prestigeCount} / ${world.unlockPrestigeCount}")
                                                    if (game.ultraCount < world.unlockUltraCount) put("Ultra resets: ${game.ultraCount} / ${world.unlockUltraCount}")
                                                })
                                                .put("fightRequirements", org.json.JSONArray().apply {
                                                    if (game.correctGuesses < world.fightCorrectGuesses) put("Correct guesses: ${game.correctGuesses} / ${world.fightCorrectGuesses}")
                                                    if (totalUpgradeLevels < world.fightUpgradeCount) put("Upgrade levels: $totalUpgradeLevels / ${world.fightUpgradeCount}")
                                                    if (game.prestigeCount < world.fightPrestigeCount) put("Prestige resets: ${game.prestigeCount} / ${world.fightPrestigeCount}")
                                                    if (game.ultraCount < world.fightUltraCount) put("Ultra resets: ${game.ultraCount} / ${world.fightUltraCount}")
                                                }))
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
                                        fun progressionUpgradeJson(definitions: List<com.jarrlyyy.guessthenumber.domain.model.UpgradeDef>, levels: Map<String, Int>, currency: BigNumber): org.json.JSONArray =
                                            org.json.JSONArray().apply {
                                                definitions.forEach { upgrade ->
                                                    val level = levels[upgrade.id] ?: 0
                                                    val maxed = level >= upgrade.maxLevel
                                                    val cost = BigNumber(upgrade.baseCost) * BigNumber(upgrade.costMultiplier).pow(level)
                                                    put(JSONObject().put("id", upgrade.id).put("name", upgrade.name).put("description", upgrade.description)
                                                        .put("level", level).put("maxLevel", upgrade.maxLevel).put("nextCost", cost.format())
                                                        .put("maxed", maxed).put("affordable", !maxed && currency >= cost))
                                                }
                                            }
                                        val prestigeUpgradesJson = progressionUpgradeJson(prestigeUpgradeDefinitions, game.prestigeUpgradeLevels, game.prestige)
                                        val ultraUpgradesJson = progressionUpgradeJson(ultraUpgradeDefinitions, game.ultraUpgradeLevels, game.ultra)
                                        val prestigeShopJson = org.json.JSONArray().apply {
                                            prestigeShopDefinitions.forEach { item ->
                                                val owned = item.id in game.prestigeShopPurchases
                                                put(JSONObject().put("id", item.id).put("name", item.name).put("description", item.description)
                                                    .put("cost", item.cost).put("owned", owned).put("affordable", !owned && game.prestige >= BigNumber(item.cost)))
                                            }
                                        }
                                        val ultraShopJson = org.json.JSONArray().apply {
                                            ultraShopDefinitions.forEach { item ->
                                                val owned = item.id in game.ultraShopPurchases
                                                put(JSONObject().put("id", item.id).put("name", item.name).put("description", item.description)
                                                    .put("cost", item.cost).put("owned", owned).put("affordable", !owned && game.ultra >= BigNumber(item.cost)))
                                            }
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
                                        val traitRegistry = ActiveTraitCatalog.current()
                                         val activeGameplayTraitIds = TraitActivationEngine(traitRegistry).evaluate(
                                             TraitActivationContext(game, playerLevel = 1, activeSeasonId = TraitRewardPolicy.currentSeasonId(), discoveredTraitIds = game.discoveredTraitIds)
                                         ).activeTraitIds
                                          val gameplayTraitsJson = org.json.JSONArray()
                                          TraitGameplayCatalog.forRegistry(traitRegistry).forEach { trait ->
                                              val definition = traitRegistry.find(trait.id) ?: return@forEach
                                              val armsJson = org.json.JSONArray()
                                              trait.arms.forEach { arm ->
                                                  armsJson.put(JSONObject()
                                                      .put("id", arm.id)
                                                      .put("label", activeLocaleManager.getString(arm.labelKey, arm.fallbackLabel))
                                                      .put("description", activeLocaleManager.getString(arm.descriptionKey, arm.fallbackDescription)))
                                              }
                                              gameplayTraitsJson.put(JSONObject()
                                                  .put("id", trait.id)
                                                  .put("title", activeLocaleManager.getString("trait_${trait.id}_title", trait.fallbackTitle))
                                                  .put("description", activeLocaleManager.getString("trait_${trait.id}_description", trait.fallbackDescription))
                                                  .put("active", trait.id in activeGameplayTraitIds)
                                                  .put("requiredCorrectGuesses", definition.activation.minimumCorrectGuesses)
                                                  .put("selectedArm", game.selectedTraitArms[trait.id] ?: trait.defaultArmId.orEmpty())
                                                  .put("arms", armsJson))
                                          }
                                         val viewState = JSONObject()
                                        .put("appVersion", BuildConfig.VERSION_NAME)
                                            .put("appVersionCode", BuildConfig.VERSION_CODE)
                                            .put("updateChannel", BuildConfig.UPDATE_CHANNEL)
                                            .put("debugBuild", BuildConfig.DEBUG)
                                            .put("buyMultiplier", game.buyMultiplier)
                                            .put("prestigeRequirement", progressionEngine.calculatePrestigeRequirement(game.prestigeCount).format())
                                            .put("prestigeReward", progressionEngine.calculatePrestigeReward(game.money, game.prestigeCount).format())
                                            .put("canPrestige", game.money >= progressionEngine.calculatePrestigeRequirement(game.prestigeCount) && progressionEngine.calculatePrestigeReward(game.money, game.prestigeCount) > BigNumber.ZERO)
                                            .put("prestigeMoneyMet", game.money >= progressionEngine.calculatePrestigeRequirement(game.prestigeCount))
                                            .put("prestigeRewardPositive", progressionEngine.calculatePrestigeReward(game.money, game.prestigeCount) > BigNumber.ZERO)
                                            .put("ultraRequirement", progressionEngine.calculateUltraRequirement(game.ultraCount).format())
                                            .put("ultraReward", progressionEngine.calculateUltraReward(game.money, game.ultraCount).format())
                                            .put("canUltra", game.money >= progressionEngine.calculateUltraRequirement(game.ultraCount) && game.prestige >= BigNumber(1_000) && progressionEngine.calculateUltraReward(game.money, game.ultraCount) > BigNumber.ZERO)
                                            .put("ultraMoneyMet", game.money >= progressionEngine.calculateUltraRequirement(game.ultraCount))
                                            .put("ultraPrestigeMet", game.prestige >= BigNumber(1_000))
                                            .put("ultraRewardPositive", progressionEngine.calculateUltraReward(game.money, game.ultraCount) > BigNumber.ZERO)
                                            .put("settings", JSONObject()
                                                .put("musicEnabled", game.settings.musicEnabled)
                                                .put("soundEnabled", game.settings.soundEnabled)
                                                .put("vibrationEnabled", game.settings.vibrationEnabled)
                                                .put("notificationsEnabled", game.settings.notificationsEnabled)
                                                .put("notificationRemindersEnabled", game.settings.notificationRemindersEnabled)
                                                .put("notificationProgressionEnabled", game.settings.notificationProgressionEnabled)
                                                .put("notificationEventsEnabled", game.settings.notificationEventsEnabled)
                                                .put("notificationIntervalHours", game.settings.notificationIntervalHours)
                                                .put("themeMode", game.settings.themeMode)
                                                .put("numberNotation", game.settings.numberNotation)
                                                .put("locale", game.settings.locale)
                                                .put("locales", org.json.JSONArray().apply { LocaleManager.supportedLocales.forEach { put(JSONObject().put("tag", it.tag).put("name", it.displayName)) } })
                                                .put("reducedMotion", game.settings.reducedMotion)
                                                .put("volume", game.settings.volume.toDouble())
                                                .put("sfxVolume", game.settings.sfxVolume.toDouble())
                                                .put("reduceFlashes", game.settings.reduceFlashes)
                                                .put("themePreset", game.settings.themePreset)
                                                .put("customPrimaryColor", game.settings.customPrimaryColor)
                                                .put("customSecondaryColor", game.settings.customSecondaryColor)
                                                .put("customTertiaryColor", game.settings.customTertiaryColor))
                                            .put("uiStrings", JSONObject().apply { listOf("stats", "statistics", "arcade", "arcade_title", "minigame_quick_guess_name", "minigame_quick_guess_desc", "minigame_reaction_test_name", "minigame_reaction_test_desc", "minigame_lucky_number_name", "minigame_lucky_number_desc", "minigame_memory_match_name", "minigame_memory_match_desc", "minigame_number_rush_name", "minigame_number_rush_desc", "stats_total_guesses", "stats_correct_guesses", "stats_failed_guesses", "stats_prestige_resets", "stats_ultra_resets", "stats_total_playtime", "guessing_precision_heading", "inspector_money_earned", "inspector_money_spent", "inspector_nebula_earned", "inspector_arcade_played", "inspector_current_streak", "inspector_best_streak", "available_money_heading", "multiplier_format", "guess_button", "play_minigame_format", "claim_reward_button", "close_button", "guess_number_target_format", "score_format", "earned_minigame_format", "reaction_test_title", "reaction_time_format", "lucky_number_title", "lucky_number_desc", "jackpot_win_format", "memory_match_title", "memory_correct", "memory_incorrect_format", "number_rush_title", "staking_flip_button", "status_playing", "status_paused", "correct_guess", "too_low", "too_high", "loading_save_data", "cancel", "close", "inspector_money", "inspector_buy_multiplier", "guess_prompt", "duration_seconds_format", "duration_ms_format", "duration_hms_format", "play", "random_event_active", "settings", "game_preferences", "background_music", "incorrect_guess_low_1", "incorrect_guess_low_2", "incorrect_guess_low_3", "incorrect_guess_high_1", "incorrect_guess_high_2", "incorrect_guess_high_3", "daily_reward_title", "daily_reward_subtitle", "daily_reward_claim", "daily_reward_streak", "daily_reward_claimed", "offline_gains_amount", "welcome_back_title", "offline_gains_text", "collect", "changelog_title", "changelog_subtitle", "changelog_eyebrow", "changelog_intro", "changelog_current_badge", "changelog_category_ui", "changelog_current_title", "changelog_current_arcade", "changelog_current_updates", "changelog_current_locale", "changelog_current_music", "changelog_current_feedback", "changelog_progress_badge", "changelog_category_gameplay", "changelog_progress_title", "changelog_progress_stats", "changelog_progress_worlds", "changelog_progress_saves", "changelog_foundation_badge", "changelog_category_system", "changelog_foundation_title", "changelog_foundation_update", "changelog_foundation_icon", "changelog_foundation_offline", "music_player_title", "music_player_subtitle", "music_player_eyebrow", "music_background_track", "music_background_track_desc", "music_player_toggle_desc", "music_player_status", "master_volume", "sound_effects", "vibration_label", "reduced_motion_label", "reduce_flashes_label", "settings_desc_sound_fx", "settings_desc_vibration", "reduced_motion_description", "settings_desc_reduce_flashes", "game_temporarily_unavailable", "settings_synced_status", "about_title", "about_licenses", "about_header", "about_version_format", "about_description", "about_licenses_title", "about_web_subtitle", "about_installed_version", "about_update_description", "about_update_idle", "about_updates_eyebrow", "about_licenses_eyebrow", "about_licenses_description", "about_library_author_format", "about_library_license_format", "update_channel_label", "update_channel_debug", "update_channel_release", "update_check", "update_checking", "update_available", "update_up_to_date", "update_unavailable", "update_install", "update_open", "update_downloading", "update_install_started", "update_install_failed", "update_permission_required", "stake_invalid_amount", "stake_flipping_status", "lucky_stake_title", "minigame_catalog_unavailable", "action_in_progress", "action_saving_reward", "reaction_wait", "reaction_tap_now", "action_failed_retry", "rift_best_tier", "loot_title", "loot_opened_count", "worlds_boss_count", "inspector_arcade_best_scores", "your_game", "more", "more_subtitle", "rewards", "rewards_subtitle", "journey", "journey_subtitle", "shop", "shop_subtitle", "home_base", "home_base_subtitle", "relics", "relics_subtitle", "codex", "codex_subtitle", "save_slots", "save_slots_subtitle", "settings_subtitle", "prestige", "prestige_subtitle", "ultra", "ultra_subtitle", "talents", "talents_subtitle", "mutators", "mutators_subtitle", "dev_settings", "dev_settings_subtitle", "lucky_stake_subtitle", "music_no_track", "music_add_audio", "music_add_to_playlist", "music_play", "music_pause", "music_shuffle", "music_repeat", "music_repeat_off", "nimmy_name", "nimmy_settings_heading", "nimmy_settings_description", "nimmy_rerun_button", "nimmy_step_counter", "nimmy_step_guess_title", "nimmy_step_guess_body", "nimmy_step_submit_title", "nimmy_step_submit_body", "nimmy_step_upgrades_title", "nimmy_step_upgrades_body", "nimmy_step_more_title", "nimmy_step_more_body", "nimmy_tour_arcade_body_1", "nimmy_tour_arcade_body_2", "nimmy_tour_arcade_title_1", "nimmy_tour_arcade_title_2", "nimmy_tour_changelog_body_1", "nimmy_tour_changelog_body_2", "nimmy_tour_changelog_title_1", "nimmy_tour_changelog_title_2", "nimmy_tour_codex_body_1", "nimmy_tour_codex_body_2", "nimmy_tour_codex_title_1", "nimmy_tour_codex_title_2", "nimmy_tour_homebase_body_1", "nimmy_tour_homebase_body_2", "nimmy_tour_homebase_title_1", "nimmy_tour_homebase_title_2", "nimmy_tour_journey_body_1", "nimmy_tour_journey_body_2", "nimmy_tour_journey_title_1", "nimmy_tour_journey_title_2", "nimmy_tour_music_body_1", "nimmy_tour_music_body_2", "nimmy_tour_music_title_1", "nimmy_tour_music_title_2", "nimmy_tour_mutators_body_1", "nimmy_tour_mutators_body_2", "nimmy_tour_mutators_title_1", "nimmy_tour_mutators_title_2", "nimmy_tour_prestige_body_1", "nimmy_tour_prestige_body_2", "nimmy_tour_prestige_title_1", "nimmy_tour_prestige_title_2", "nimmy_tour_relics_body_1", "nimmy_tour_relics_body_2", "nimmy_tour_relics_title_1", "nimmy_tour_relics_title_2", "nimmy_tour_rewards_body_1", "nimmy_tour_rewards_body_2", "nimmy_tour_rewards_title_1", "nimmy_tour_rewards_title_2", "nimmy_tour_shop_body_1", "nimmy_tour_shop_body_2", "nimmy_tour_shop_title_1", "nimmy_tour_shop_title_2", "nimmy_tour_stake_body_1", "nimmy_tour_stake_body_2", "nimmy_tour_stake_title_1", "nimmy_tour_stake_title_2", "nimmy_tour_stats_body_1", "nimmy_tour_stats_body_2", "nimmy_tour_stats_title_1", "nimmy_tour_stats_title_2", "nimmy_tour_talent_body_1", "nimmy_tour_talent_body_2", "nimmy_tour_talent_title_1", "nimmy_tour_talent_title_2", "nimmy_tour_ultra_body_1", "nimmy_tour_ultra_body_2", "nimmy_tour_ultra_title_1", "nimmy_tour_ultra_title_2", "nimmy_tour_upgrades_body_1", "nimmy_tour_upgrades_body_2", "nimmy_tour_upgrades_title_1", "nimmy_tour_upgrades_title_2", "nimmy_tour_worlds_body_1", "nimmy_tour_worlds_body_2", "nimmy_tour_worlds_title_1", "nimmy_tour_worlds_title_2", "nimmy_next", "nimmy_skip", "nimmy_finish", "music_repeat_one", "music_repeat_all", "music_enqueue", "music_remove", "music_move_up", "music_move_down", "music_delete_playlist", "music_empty_library", "music_empty_queue", "music_empty_playlists", "music_queue", "music_library", "music_playlists", "music_queue_position", "music_playlist_tracks", "music_playlist_name", "music_create_playlist", "on", "off").forEach { key -> put(key, activeLocaleManager.getString(key)) }; (0..29).forEach { index ->
                                                put("feedback_toolow_$index", activeLocaleManager.getString("feedback_toolow_$index"))
                                                put("feedback_toohigh_$index", activeLocaleManager.getString("feedback_toohigh_$index"))
                                            } })
                                            .put("upgrades", upgradesJson)
                                            .put("shopItems", shopJson)
                                            .put("prestigeUpgrades", prestigeUpgradesJson)
                                            .put("prestigeShopItems", prestigeShopJson)
                                            .put("ultraUpgrades", ultraUpgradesJson)
                                            .put("ultraShopItems", ultraShopJson)
                                            .put("minigames", org.json.JSONArray().apply {
                                                minigameDefinitions.forEach { minigame ->
                                                    put(JSONObject().put("id", minigame.id).put("name", minigame.name)
                                                        .put("description", minigame.description).put("rewardMultiplier", minigame.rewardMultiplier))
                                                }
                                            })
                                            .put("rangeMin", (result?.rangeMin ?: game.currentRangeMin).toString())
                                            .put("rangeMax", (result?.rangeMax ?: game.currentRangeMax).toString())
                                            .put("attempts", result?.attempts ?: game.attempts)
                                            .put("correctGuesses", result?.correctGuesses ?: game.correctGuesses)
                                             .put("gameplayTraits", gameplayTraitsJson)
                                             .put("selectedTraitArms", JSONObject().apply { game.selectedTraitArms.forEach { (traitId, armId) -> put(traitId, armId) } })
                                            .put("streak", result?.streak ?: game.streak)
                                            .put("bestStreak", result?.bestStreak ?: game.bestStreak)
                                            .put("accepted", result?.accepted ?: actionAccepted)
                                             .put("offlineGains", if (offlineRewardDismissed) "" else currentOfflineGains?.format() ?: "")
                                             .put("dailyRewardAvailable", game.lastDailyRewardDate != java.time.LocalDate.now().toString())
                                             .put("dailyRewardStreak", game.dailyRewardStreak)
                                             .put("dailyRewardAmount", (com.jarrlyyy.guessthenumber.domain.model.DailyRewardPolicy.nextReward(game.lastDailyRewardDate, game.dailyRewardStreak, java.time.LocalDate.now().toString()) * if ("nebula_boost" in game.shopPurchases) 2L else 1L).toString())
                                             .put("dailyRewardNextStreak", com.jarrlyyy.guessthenumber.domain.model.DailyRewardPolicy.nextStreak(game.lastDailyRewardDate, game.dailyRewardStreak, java.time.LocalDate.now().toString()))
                                             .put("dailyRewardLastClaimDate", game.lastDailyRewardDate)
                                             .put("incorrectGuessMessages", JSONObject().apply {
                                                 put("tooLow", org.json.JSONArray().apply {
                                                     for (index in 0..29) {
                                                         val key = "feedback_toolow_$index"
                                                         val message = activeLocaleManager.getString(key)
                                                         if (message.isNotBlank() && message != key) put(message)
                                                     }
                                                 })
                                                 put("tooHigh", org.json.JSONArray().apply {
                                                     for (index in 0..29) {
                                                         val key = "feedback_toohigh_$index"
                                                         val message = activeLocaleManager.getString(key)
                                                         if (message.isNotBlank() && message != key) put(message)
                                                     }
                                                 })
                                             })
                                            .put("money", game.money.format())
                                            .put("incomePerSecond", currentIncome.format())
                                            .put("statistics", JSONObject()
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
                                                .put("arcadeBestScores", JSONObject().apply { game.statistics.arcadeBestScores.forEach { (gameId, score) -> put(gameId, score) } })
                                                .put("randomEventsTriggered", game.statistics.randomEventsTriggered)
                                                .put("lootDropsFound", game.lootDropsFound)
                                                .put("lootChestsOpened", game.lootChestsOpened)
                                                .put("worldBossVictories", game.worldBossVictories)
                                                .put("endlessRiftBestTier", game.endlessRiftBestTier))

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
                                        viewState.put("musicPlayer", JSONObject()
                                            .put("library", org.json.JSONArray().apply { musicManager.library.value.forEach { track -> put(JSONObject().put("id", track.id).put("title", track.title).put("artist", track.artist).put("album", track.album).put("duration", track.duration)) } })
                                            .put("queue", org.json.JSONArray(musicManager.queue.value))
                                            .put("playlists", JSONObject().apply { musicManager.playlists.value.forEach { (name, ids) -> put(name, org.json.JSONArray(ids)) } })
                                            .put("currentTrackId", musicManager.currentTrackId.value ?: "")
                                            .put("currentPosition", musicManager.currentPosition.value)
                                            .put("duration", musicManager.duration.value)
                                            .put("isPlaying", musicManager.isPlaying.value)
                                            .put("shuffle", musicManager.shuffle.value)
                                            .put("repeatMode", musicManager.repeatMode.value.name))
                                        if (result != null) viewState.put("lastGuessStatus", result.status ?: "rate_limited")
                                        replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id).put("type", "state").put("state", viewState).toString())
                                        } catch (exception: Exception) {
                                            reportBridgeException(parsed.type, parsed.id, exception)
                                            replyProxy.postMessage(errorResponse(parsed.id, "state_response_failed"))
                                        }
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

    LaunchedEffect(Unit) {
        // If startup sync is still running, serialize behind it and refresh only when the active version changes.
        val versionBefore = webPlayAssetSyncer.activeVersion()
        webPlayAssetSyncer.sync()
        if (webPlayAssetSyncer.activeVersion() != versionBefore) {
            webViewRef?.post { webViewRef?.loadUrl("$WEB_UI_ORIGIN${WEB_UI_PATH}index.html") }
        }
    }

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
