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
import androidx.compose.foundation.layout.weight
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

private const val WEB_UI_ORIGIN = "https://appassets.androidplatform.net"
private const val WEB_UI_PATH = "/assets/web-prototype/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedWebUiPrototypeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var counter by rememberSaveable { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val supportsMessageListener = remember {
        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
    }

    DisposableEffect(webViewRef) {
        val webView = webViewRef
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shared Web UI • Prototype") },
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
                "Debug-only experiment. The counter lives in Kotlin and does not touch game saves.",
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
                                    uri.path?.startsWith(WEB_UI_PATH) == true)
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
                                            if (!isMainFrame ||
                                                sourceOrigin.scheme != "https" ||
                                                sourceOrigin.host != "appassets.androidplatform.net"
                                            ) {
                                                replyProxy.postMessage(errorResponse("", "origin_not_allowed"))
                                                return@post
                                            }
                                            if (raw.length > 512) {
                                                replyProxy.postMessage(errorResponse("", "message_too_large"))
                                                return@post
                                            }

                                            try {
                                                val request = JSONObject(raw)
                                                val id = request.optString("id")
                                                val type = request.optString("type")
                                                if (
                                                    request.length() != 3 ||
                                                    request.optInt("version", -1) != 1 ||
                                                    !id.matches(Regex("[A-Za-z0-9_-]{1,48}"))
                                                ) {
                                                    replyProxy.postMessage(errorResponse(id, "invalid_request"))
                                                    return@post
                                                }

                                                when (type) {
                                                    "read_state" -> Unit
                                                    "increment_counter" -> if (counter < Int.MAX_VALUE) counter++
                                                    else -> {
                                                        replyProxy.postMessage(errorResponse(id, "unknown_action"))
                                                        return@post
                                                    }
                                                }
                                                replyProxy.postMessage(
                                                    JSONObject()
                                                        .put("version", 1)
                                                        .put("id", id)
                                                        .put("type", "state")
                                                        .put("state", JSONObject().put("counter", counter))
                                                        .toString()
                                                )
                                            } catch (_: Exception) {
                                                replyProxy.postMessage(errorResponse("", "invalid_json"))
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
