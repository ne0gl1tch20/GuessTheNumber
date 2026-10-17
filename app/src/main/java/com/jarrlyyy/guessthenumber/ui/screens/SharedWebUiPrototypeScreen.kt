package com.jarrlyyy.guessthenumber.ui.screens

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.viewmodel.GuessSubmissionResult

private const val WEB_UI_ORIGIN = "https://appassets.androidplatform.net"
private const val WEB_UI_PATH = "/assets/web-prototype/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedWebUiPrototypeScreen(state: GameState, onSubmitGuess: (Long) -> GuessSubmissionResult, onBack: () -> Unit) {
    var counter by remember { mutableIntStateOf(0) }
    val currentGameState by rememberUpdatedState(state)
    val submitGuess by rememberUpdatedState(onSubmitGuess)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val supportsMessageListener = remember {
        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
    }

    // Keep one disposal effect for this screen instance. Keying this effect to
    // webViewRef would dispose the just-created WebView when AndroidView's
    // factory updates the state from null to the new view.
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { webView ->
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shared Web UI • Guessing preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Debug-only preview using your current game state. Guess processing stays in Kotlin.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!supportsMessageListener) {
                Text(
                    "This WebView does not support the origin-restricted message bridge. The prototype is unavailable on this device.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { viewContext ->
                    val assetLoader = WebViewAssetLoader.Builder()
                        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(viewContext))
                        .build()
                    WebView(viewContext).apply {
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
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ) = assetLoader.shouldInterceptRequest(request.url)

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val uri = request.url
                                return !(uri.scheme == "https" &&
                                    uri.host == "appassets.androidplatform.net" &&
                                    uri.path == "${WEB_UI_PATH}index.html" &&
                                    uri.query == null &&
                                    uri.fragment == null)
                            }
                        }

                        if (supportsMessageListener) {
                            WebViewCompat.addWebMessageListener(
                                this,
                                "gtnHost",
                                setOf(WEB_UI_ORIGIN),
                                object : WebViewCompat.WebMessageListener {
                                    override fun onPostMessage(
                                        view: WebView,
                                        message: WebMessageCompat,
                                        sourceOrigin: Uri,
                                        isMainFrame: Boolean,
                                        replyProxy: JavaScriptReplyProxy
                                    ) {
                                        view.post {
                                            val raw = message.data ?: ""
                                            if (!WebUiBridgeProtocol.isAllowedSource(
                                                    isMainFrame = isMainFrame,
                                                    sourceOrigin = sourceOrigin,
                                                    currentUrl = view.url
                                                )
                                            ) {
                                                replyProxy.postMessage(errorResponse("", "origin_not_allowed"))
                                                return@post
                                            }
                                            when (val parsed = WebUiBridgeProtocol.parse(raw)) {
                                                is WebUiBridgeProtocol.ParseResult.Invalid -> {
                                                    replyProxy.postMessage(errorResponse(parsed.id, parsed.error))
                                                    return@post
                                                }
                                                is WebUiBridgeProtocol.ParseResult.Valid -> {
                                                    val result = if (parsed.type == "submit_guess" && parsed.guess != null) {
                                                        try { submitGuess(parsed.guess) } catch (_: Exception) { null }
                                                    } else null
                                                    if (parsed.type == "increment_counter" && counter < Int.MAX_VALUE) counter++
                                                    if (parsed.type == "submit_guess" && result == null) {
                                                        replyProxy.postMessage(errorResponse(parsed.id, "guess_processing_failed"))
                                                    } else {
                                                        val game = currentGameState
                                                        val viewState = JSONObject()
                                                            .put("rangeMin", result?.rangeMin ?: game.currentRangeMin)
                                                            .put("rangeMax", result?.rangeMax ?: game.currentRangeMax)
                                                            .put("attempts", result?.attempts ?: game.attempts)
                                                            .put("correctGuesses", result?.correctGuesses ?: game.correctGuesses)
                                                            .put("streak", result?.streak ?: game.streak)
                                                            .put("bestStreak", result?.bestStreak ?: game.bestStreak)
                                                            .put("accepted", result?.accepted ?: true)
                                                        if (result != null) viewState.put("lastGuessStatus", result.status ?: "rate_limited")
                                                        replyProxy.postMessage(JSONObject().put("version", 1).put("id", parsed.id).put("type", "state").put("state", viewState).toString())
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        loadUrl("$WEB_UI_ORIGIN${WEB_UI_PATH}index.html")
                    }
                }
            )
        }
    }
}

private fun errorResponse(id: String, code: String): String =
    JSONObject()
        .put("version", 1)
        .put("id", id)
        .put("type", "error")
        .put("error", code)
        .toString()
