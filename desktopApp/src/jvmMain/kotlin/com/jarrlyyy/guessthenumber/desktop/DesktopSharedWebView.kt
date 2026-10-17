package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import java.awt.BorderLayout
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel

/**
 * Hosts the canonical bundled web-play UI in the Windows desktop window.
 * Only local, packaged resources are loaded; game actions are routed through
 * the typed Kotlin-side request handler supplied by the game screen.
 */
@Composable
internal fun DesktopSharedWebView(
    onRequest: (String, (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember { DesktopSharedWebViewController() }
    controller.setRequestHandler(onRequest)
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    SwingPanel(
        factory = { controller.panel },
        modifier = modifier.fillMaxSize(),
        update = { controller.setRequestHandler(onRequest) }
    )
}

private class DesktopSharedWebViewController {
    val panel = JPanel(BorderLayout())
    private val fxPanel = JFXPanel()
    private val handler = AtomicReference<((String, (String) -> Unit) -> Unit)>()
    @Volatile private var disposed = false
    private var engine: javafx.scene.web.WebEngine? = null
    private var localIndex: Path? = null

    init {
        panel.add(fxPanel, BorderLayout.CENTER)
        Platform.runLater {
            if (disposed) return@runLater
            try {
                val index = unpackWebAssets()
                localIndex = index
                val webView = WebView()
                webView.isContextMenuEnabled = true
                val webEngine = webView.engine
                engine = webEngine
                webEngine.isJavaScriptEnabled = true
                val bridge = DesktopJavaScriptBridge { raw ->
                    val currentHandler = handler.get()
                    if (currentHandler != null && raw.length <= 512) {
                        currentHandler(raw) { response ->
                            Platform.runLater {
                                if (!disposed) {
                                    val target = engine ?: return@runLater
                                    val encoded = org.json.JSONObject.quote(response)
                                    runCatching { target.executeScript("window.gtnHost && window.gtnHost.receive($encoded)") }
                                }
                            }
                        }
                    }
                }
                webEngine.loadWorker.stateProperty().addListener { _, _, state ->
                    if (disposed) return@addListener
                    if (state == Worker.State.SCHEDULED || state == Worker.State.RUNNING || state == Worker.State.SUCCEEDED) {
                        runCatching {
                            (webEngine.executeScript("window") as JSObject).setMember("gtnDesktopBridge", bridge)
                        }
                    }
                }
                webEngine.locationProperty().addListener { _, _, location ->
                    val expected = localIndex?.toUri()?.toString()
                    if (!disposed && expected != null && location != expected && location != "about:blank") {
                        Platform.runLater { if (!disposed) webEngine.load(expected) }
                    }
                }
                fxPanel.scene = Scene(webView)
                webEngine.load(index.toUri().toString())
            } catch (error: Exception) {
                val fallback = javafx.scene.control.Label(
                    "The shared HTML interface could not start. Check the bundled web assets and JavaFX runtime. " +
                        (error.message ?: error.javaClass.simpleName)
                )
                fxPanel.scene = Scene(javafx.scene.layout.StackPane(fallback))
            }
        }
    }

    fun setRequestHandler(value: (String, (String) -> Unit) -> Unit) {
        handler.set(value)
    }

    fun dispose() {
        disposed = true
        handler.set(null)
        Platform.runLater {
            runCatching { engine?.load("about:blank") }
            engine = null
        }
    }

    private fun unpackWebAssets(): Path {
        val root = Files.createTempDirectory("gtn-shared-web-play-")
        root.toFile().deleteOnExit()
        val files = listOf("index.html", "app.js", "styles.css", "icons/numberverse.svg", "icons/sparkle.svg", "icons/star.svg")
        for (relative in files) {
            val resource = "/web-play/$relative"
            val input = javaClass.getResourceAsStream(resource)
                ?: throw IOException("Missing bundled web UI resource: $resource")
            val output = root.resolve(relative)
            Files.createDirectories(output.parent)
            input.use { Files.copy(it, output) }
            output.toFile().deleteOnExit()
        }
        val index = root.resolve("index.html")
        val original = Files.readString(index)
        val shim = """
            <script>
            window.gtnHost = {
              onmessage: null,
              postMessage: function(raw) {
                if (window.gtnDesktopBridge) window.gtnDesktopBridge.postMessage(String(raw));
              },
              receive: function(raw) {
                if (typeof this.onmessage === 'function') this.onmessage({data: String(raw)});
              }
            };
            </script>
        """.trimIndent()
        val patched = original.replaceFirst(Regex("(?i)<head[^>]*>")) { it.value + shim }
        if (patched == original) throw IOException("Shared web UI index.html has no head element.")
        Files.writeString(index, patched)
        return index
    }

}

private class DesktopJavaScriptBridge(
    private val dispatch: (String) -> Unit
) {
    fun postMessage(raw: String) {
        dispatch(raw)
    }
}
