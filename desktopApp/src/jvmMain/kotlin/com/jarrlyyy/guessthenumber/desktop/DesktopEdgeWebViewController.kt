package com.jarrlyyy.guessthenumber.desktop

import ca.weblite.webview.WebView
import ca.weblite.webview.WebViewNavigationHandler
import ca.weblite.webview.swing.WebViewComponent
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Windows-only host for the shared HTML player UI.
 *
 * SwingWebView's Windows backend embeds a native WebView2 child HWND under
 * its heavyweight AWT Canvas. The browser owns its input/focus path and runs
 * on its own STA message loop; Compose and Android never use this class.
 */
internal class DesktopEdgeWebViewController(
    initialScreen: String,
    private val unpackWebAssets: () -> Path
) {
    val panel = JPanel(BorderLayout())
    private val handler = AtomicReference<((String, (String) -> Unit) -> Unit)>()
    private val navigationPositionHandler = AtomicReference<(DesktopNavigationPosition) -> Unit>({})
    @Volatile private var disposed = false
    private var localRoot: Path? = null
    private var webView: WebViewComponent? = null
    private var readyTimer: Timer? = null

    fun setRequestHandler(value: (String, (String) -> Unit) -> Unit) {
        handler.set(value)
    }

    fun setNavigationPositionHandler(value: (DesktopNavigationPosition) -> Unit) {
        navigationPositionHandler.set(value)
    }
    fun navigateToScreen(screen: String) {
        if (disposed) return
        val encoded = JSONObject.quote(screen)
        SwingUtilities.invokeLater {
            if (!disposed) {
                runCatching {
                    webView?.eval("window.gtnActivateScreen && window.gtnActivateScreen($encoded)")
                }.onFailure { error ->
                    System.err.println("GuessTheNumber Edge screen navigation failed: ${error.message ?: error.javaClass.simpleName}")
                }
            }
        }
    }


    fun start() {
        check(SwingUtilities.isEventDispatchThread()) {
            "The Windows WebView2 host must be created on the Swing event-dispatch thread."
        }
        if (disposed || webView != null) return
        try {
            val index = unpackWebAssets()
            localRoot = index.parent.parent.toAbsolutePath().normalize()

            check(WebViewComponent.isNavigationHandlerSupported()) {
                "The bundled Edge WebView2 bridge does not support navigation restrictions."
            }
            val browser = WebViewComponent.create(WebViewComponent.Mode.HEAVYWEIGHT)
            webView = browser
            val debugToolsEnabled = javaClass.classLoader
                .getResourceAsStream("desktop-build.properties")
                ?.use { properties ->
                    java.util.Properties().apply { load(properties) }
                        .getProperty("debugToolsBuildAllowed", "false")
                        .equals("true", ignoreCase = true)
                } ?: false
            // WebView2 exposes Inspect Element only in developer-enabled builds.
            // Release packages keep the embedded game surface locked down.
            browser.setDebug(debugToolsEnabled)
            browser.setDefaultContextMenuEnabled(true)
            browser.setNavigationHandler(WebViewNavigationHandler { event ->
                allowNavigation(event.url())
            })
            browser.addJavascriptCallback("gtnDesktopPostMessage", WebView.JavascriptCallback { raw ->
                if (!disposed) handleRequest(raw)
            })
            browser.addJavascriptCallback("gtnDesktopPageReady", WebView.JavascriptCallback {
                if (!disposed) onPageReady()
            })
            browser.addOnBeforeLoad(
                """
                (function () {
                    window.gtnDesktopBridge = Object.freeze({
                        postMessage: function (raw) {
                            window.gtnDesktopPostMessage(String(raw));
                        }
                    });
                    // Keep the standard browser context menu available in every build.
                    // WebView2 developer tools are still gated by browser.setDebug(debugToolsEnabled).
                    window.addEventListener('load', function () {
                        window.gtnDesktopPageReady(location.href);
                    }, { once: true });
                })();
                """.trimIndent()
            )

            panel.add(browser, BorderLayout.CENTER)
            panel.revalidate()
            panel.repaint()

            readyTimer = Timer(25_000) {
                if (!disposed && webView === browser) {
                    showStartupFailure(
                        "Microsoft Edge WebView2 did not finish starting.",
                        "Check that the Microsoft Edge WebView2 Runtime is installed and up to date, then restart GuessTheNumber."
                    )
                }
            }.apply {
                isRepeats = false
                start()
            }
            browser.setUrl(index.toUri().toString())
        } catch (error: Throwable) {
            System.err.println("GuessTheNumber Edge WebView2 startup failed: ${error.stackTraceToString()}")
            showStartupFailure(
                "The Windows shared game interface could not start.",
                error.message ?: error.javaClass.simpleName
            )
        }
    }

    private fun allowNavigation(rawUrl: String): Boolean {
        if (rawUrl == "about:blank" || rawUrl.startsWith("about:blank#")) return true
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                val root = localRoot ?: return false
                val target = runCatching { Path.of(uri).toAbsolutePath().normalize() }.getOrNull()
                    ?: return false
                target.startsWith(root)
            }
            "http", "https" -> {
                if (!uri.host.isNullOrBlank()) {
                    CompletableFuture.runAsync {
                        runCatching {
                            if (Desktop.isDesktopSupported() &&
                                Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
                            ) {
                                Desktop.getDesktop().browse(uri)
                            }
                        }
                    }
                }
                false
            }
            else -> false
        }
    }

    private fun handleRequest(raw: String) {
        val currentHandler = handler.get()
        val normalized = DesktopBridgePayload.normalize(raw)
        val request = normalized?.let { runCatching { JSONObject(it) }.getOrNull() }
        val replyToPage: (String) -> Unit = { response ->
            if (!disposed) {
                val encoded = JSONObject.quote(response)
                SwingUtilities.invokeLater {
                    if (!disposed) {
                        runCatching {
                            webView?.eval("window.gtnHost && window.gtnHost.receive($encoded)")
                        }
                    }
                }
            }
        }
        val setting = request
            ?.takeIf { it.optInt("version", -1) == 1 && it.optString("type") == "update_setting" }
            ?.takeIf { it.length() == 5 }
        val settingKey = setting?.optString("key").orEmpty()
        val settingValue = setting?.opt("value")

        when {
            normalized == null || request == null -> {
                if (javaClass.classLoader.getResourceAsStream("desktop-build.properties")?.use { stream ->
                        java.util.Properties().apply { load(stream) }.getProperty("debugToolsBuildAllowed", "false")
                            .equals("true", ignoreCase = true)
                    } == true
                ) {
                    System.err.println("GuessTheNumber desktop bridge rejected malformed callback payload: ${raw.take(512)}")
                }
                replyToPage(JSONObject().put("version", 1)
                    .put("id", DesktopBridgePayload.recoverRequestId(raw)).put("type", "error").put("error", "invalid_request").toString())
            }
            settingKey == "desktopNavigationPosition" && settingValue is String -> {
                val position = DesktopNavigationPosition.entries.firstOrNull {
                    it.name == settingValue.uppercase()
                }
                if (position == null) {
                    replyToPage(JSONObject().put("version", 1)
                        .put("id", request?.optString("id", "").orEmpty())
                        .put("type", "error").put("error", "invalid_desktop_setting").toString())
                } else {
                    DesktopNavigationPosition.save(position)
                    SwingUtilities.invokeLater { navigationPositionHandler.get().invoke(position) }
                    replyToPage(
                        JSONObject()
                            .put("version", 1)
                            .put("id", request?.optString("id", "").orEmpty())
                            .put("type", "state")
                            .put("state", JSONObject()
                                .put("accepted", true)
                                .put("message", "Navigation position saved."))
                            .toString()
                    )
                }
            }
            settingKey == "desktopLaunchAtStartup" && settingValue is Boolean -> {
                val result = DesktopStartupLaunch.setEnabled(settingValue)
                val response = JSONObject()
                    .put("version", 1)
                    .put("id", request?.optString("id", "").orEmpty())
                if (result.isSuccess) {
                    response.put("type", "state").put("state", JSONObject()
                        .put("accepted", true)
                        .put("message", if (settingValue) "Launch at startup enabled." else "Launch at startup disabled."))
                } else {
                    response.put("type", "error").put("error", "desktop_startup_update_failed")
                        .put("message", result.exceptionOrNull()?.message ?: "Could not update Windows startup.")
                }
                replyToPage(response.toString())
            }
            currentHandler != null && normalized.length <= 16384 -> {
                try {
                    currentHandler(normalized, replyToPage)
                } catch (error: Throwable) {
                    val id = request.optString("id", "")
                    System.err.println("GuessTheNumber desktop bridge handler failed for $id: ${error.stackTraceToString()}")
                    replyToPage(JSONObject().put("version", 1).put("id", id)
                        .put("type", "error").put("error", "host_handler_failed")
                        .put("message", error.message ?: error.javaClass.simpleName).toString())
                }
            }
            else -> {
                val id = request.optString("id", "")
                replyToPage(JSONObject().put("version", 1).put("id", id)
                    .put("type", "error").put("error", "host_unavailable").toString())
            }
        }
    }

    private fun onPageReady() {
        readyTimer?.stop()
        val browser = webView ?: return
        browser.evalAsync(
            """
            return ({
                ready: document.readyState,
                hasBody: !!document.body,
                bodyChildren: document.body ? document.body.children.length : 0,
                hasScreenActivator: typeof window.gtnActivateScreen === 'function',
                hasDesktopBridge: !!(window.gtnHost && window.gtnDesktopBridge),
                url: location.href
            });
            """.trimIndent()
        ).whenComplete { result, error ->
            if (disposed) return@whenComplete
            SwingUtilities.invokeLater {
                if (disposed || webView !== browser) return@invokeLater
                val state = if (error == null) runCatching { JSONObject(result) }.getOrNull() else null
                System.err.println("GuessTheNumber Edge WebView2 loaded: ${state ?: (error?.message ?: "no document state")}")
                // Release any page requests queued before the native bridge finished attaching.
                browser.eval("window.gtnHost && window.gtnHost.flush && window.gtnHost.flush()")
                if (state == null || !state.optBoolean("hasBody") || !state.optBoolean("hasDesktopBridge") ||
                    state.optInt("bodyChildren") == 0 || !state.optBoolean("hasScreenActivator")
                ) {
                    showStartupFailure(
                        "The shared game page loaded without rendering its interface.",
                        state?.toString(2) ?: (error?.message ?: "The browser could not inspect the loaded document.")
                    )
                }
            }
        }
    }

    private fun showStartupFailure(title: String, detail: String) {
        if (disposed) return
        readyTimer?.stop()
        webView?.let { browser ->
            webView = null
            runCatching { browser.dispose() }
        }
        panel.removeAll()
        val heading = JLabel(title, SwingConstants.LEFT).apply {
            font = font.deriveFont(java.awt.Font.BOLD, 18f)
            foreground = java.awt.Color(247, 248, 255)
        }
        val message = JLabel(
            "<html><div style='width:640px;color:#d5def5'>" +
                detail.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\n", "<br>") +
                "</div></html>"
        )
        panel.background = java.awt.Color(16, 21, 34)
        panel.border = BorderFactory.createEmptyBorder(28, 28, 28, 28)
        panel.add(JPanel(BorderLayout(0, 12)).apply {
            background = panel.background
            add(heading, BorderLayout.NORTH)
            add(message, BorderLayout.CENTER)
        }, BorderLayout.CENTER)
        panel.revalidate()
        panel.repaint()
        System.err.println("GuessTheNumber Edge WebView2 startup error: $title\n$detail")
    }

    fun dispose() {
        disposed = true
        handler.set(null)
        readyTimer?.stop()
        readyTimer = null
        webView?.let { browser ->
            webView = null
            runCatching { browser.dispose() }
        }
        panel.removeAll()
        localRoot?.let(::deleteResourceTree)
        localRoot = null
    }

    private fun deleteResourceTree(root: Path) {
        runCatching {
            if (Files.exists(root)) {
                Files.walk(root).use { paths ->
                    paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
                }
            }
        }
    }
}
