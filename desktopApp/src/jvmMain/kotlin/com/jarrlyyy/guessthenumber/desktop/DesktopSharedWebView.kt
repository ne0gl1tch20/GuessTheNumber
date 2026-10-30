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
import java.awt.Desktop
import java.net.URI
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import javax.swing.JPanel

/**
 * Hosts the canonical bundled web-play UI in the Windows desktop window.
 * Only local, packaged resources are loaded; game actions are routed through
 * the typed Kotlin-side request handler supplied by the game screen.
 */
@Composable
internal fun DesktopSharedWebView(
    initialScreen: String,
    onRequest: (String, (String) -> Unit) -> Unit,
    onNavigationPositionChange: (DesktopNavigationPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember(initialScreen) { DesktopSharedWebViewController(initialScreen) }
    controller.setRequestHandler(onRequest)
    controller.setNavigationPositionHandler(onNavigationPositionChange)
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    SwingPanel(
        factory = { controller.panel },
        modifier = modifier.fillMaxSize(),
        update = {
            controller.setRequestHandler(onRequest)
            controller.setNavigationPositionHandler(onNavigationPositionChange)
        }
    )
}

private class DesktopSharedWebViewController(initialScreenValue: String) {
    val panel = JPanel(BorderLayout())
    private val isWindowsDesktop = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    private val edgeController = if (isWindowsDesktop) {
        DesktopEdgeWebViewController(initialScreenValue, ::unpackWebAssets)
    } else null
    private lateinit var fxPanel: JFXPanel
    private val handler = AtomicReference<((String, (String) -> Unit) -> Unit)>()
    private val initialScreen = AtomicReference(initialScreenValue.also {
        require(it in setOf("play", "more", "changelog", "about", "music", "upgrades", "shop", "prestige", "ultra", "talent", "settings", "rewards", "worlds", "mutators", "relics", "homebase", "codex", "journey", "stats", "arcade", "stake"))
    })
    private val navigationPositionHandler = AtomicReference<(DesktopNavigationPosition) -> Unit>({})
    @Volatile private var disposed = false
    private var engine: javafx.scene.web.WebEngine? = null
    private var localIndex: Path? = null
    private var localRoot: Path? = null

    init {
        if (edgeController != null) {
            panel.add(edgeController.panel, BorderLayout.CENTER)
            SwingUtilities.invokeLater { if (!disposed) edgeController.start() }
        } else {
            fxPanel = JFXPanel()
            panel.add(fxPanel, BorderLayout.CENTER)
            Platform.runLater {
            if (disposed) return@runLater
            try {
                val index = unpackWebAssets()
                localIndex = index
                localRoot = index.parent.parent
                val webView = WebView()
                webView.isContextMenuEnabled = true
                val webEngine = webView.engine
                engine = webEngine
                webEngine.isJavaScriptEnabled = true
                val bridge = DesktopJavaScriptBridge { raw ->
                    val currentHandler = handler.get()
                    val replyToPage: (String) -> Unit = { response ->
                        Platform.runLater {
                            if (!disposed) {
                                val target = engine ?: return@runLater
                                val encoded = org.json.JSONObject.quote(response)
                                runCatching { target.executeScript("window.gtnHost && window.gtnHost.receive($encoded)") }
                            }
                        }
                    }

                    val request = runCatching { org.json.JSONObject(raw) }.getOrNull()
                    val isDesktopSetting = request
                        ?.takeIf { it.optInt("version", -1) == 1 && it.optString("type") == "update_setting" }
                        ?.takeIf { it.length() == 5 }
                    val settingKey = isDesktopSetting?.optString("key").orEmpty()
                    val settingValue = isDesktopSetting?.opt("value")

                    if (settingKey == "desktopNavigationPosition" && settingValue is String) {
                        val position = DesktopNavigationPosition.entries.firstOrNull {
                            it.name == settingValue.uppercase()
                        }
                        if (position == null) {
                            replyToPage(org.json.JSONObject().put("version", 1)
                                .put("id", request?.optString("id", "").orEmpty())
                                .put("type", "error").put("error", "invalid_desktop_setting").toString())
                        } else {
                            DesktopNavigationPosition.save(position)
                            navigationPositionHandler.get().invoke(position)
                            replyToPage(
                                org.json.JSONObject()
                                    .put("version", 1)
                                    .put("id", request?.optString("id", "").orEmpty())
                                    .put("type", "state")
                                    .put("state", org.json.JSONObject()
                                        .put("accepted", true)
                                        .put("message", "Navigation position saved."))
                                    .toString()
                            )
                        }
                    } else if (settingKey == "desktopLaunchAtStartup" && settingValue is Boolean) {
                        val result = DesktopStartupLaunch.setEnabled(settingValue)
                        val response = org.json.JSONObject()
                            .put("version", 1)
                            .put("id", request?.optString("id", "").orEmpty())
                        if (result.isSuccess) {
                            response.put("type", "state").put("state", org.json.JSONObject()
                                .put("accepted", true)
                                .put("message", if (settingValue) "Launch at startup enabled." else "Launch at startup disabled."))
                        } else {
                            response.put("type", "error").put("error", "desktop_startup_update_failed")
                                .put("message", result.exceptionOrNull()?.message ?: "Could not update Windows startup.")
                        }
                        replyToPage(response.toString())
                    } else if (currentHandler != null && raw.length <= 512) {
                        currentHandler(raw, replyToPage)
                    } else {
                        val id = request?.optString("id", "").orEmpty()
                        replyToPage(org.json.JSONObject().put("version", 1).put("id", id)
                            .put("type", "error").put("error", "host_unavailable").toString())
                    }
                }
                webEngine.loadWorker.stateProperty().addListener { _, _, state ->
                    if (disposed) return@addListener
                    when (state) {
                        Worker.State.SCHEDULED, Worker.State.RUNNING, Worker.State.SUCCEEDED -> {
                            runCatching {
                                (webEngine.executeScript("window") as JSObject).setMember("gtnDesktopBridge", bridge)
                                webEngine.executeScript("window.gtnHost && window.gtnHost.flush()")
                            }.onFailure { error ->
                                System.err.println("GuessTheNumber desktop bridge initialization failed: ${error.message ?: error.javaClass.simpleName}")
                            }
                            if (state == Worker.State.SUCCEEDED) {
                                val startupCheck = runCatching {
                                    webEngine.executeScript(
                                        "(function(){return JSON.stringify({" +
                                            "ready:document.readyState," +
                                            "hasBody:!!document.body," +
                                            "bodyChildren:document.body?document.body.children.length:0," +
                                            "hasScreenActivator:typeof window.gtnActivateScreen==='function'," +
                                            "url:location.href" +
                                        "});})()"
                                    )?.toString()
                                }.getOrNull()
                                System.err.println("GuessTheNumber desktop WebView loaded: $startupCheck")
                                if (startupCheck == null || startupCheck.contains("\"hasBody\":false") ||
                                    startupCheck.contains("\"bodyChildren\":0") || startupCheck.contains("\"hasScreenActivator\":false")) {
                                    showStartupFailure(
                                        "The shared game page loaded without rendering its interface.",
                                        startupCheck ?: "The WebView could not inspect the loaded document."
                                    )
                                }
                            }
                        }
                        Worker.State.FAILED -> {
                            val failure = webEngine.loadWorker.exception
                            val detail = failure?.let { it.message ?: it.javaClass.simpleName }
                                ?: "The browser did not provide an error message."
                            System.err.println("GuessTheNumber desktop WebView failed to load ${webEngine.location}: $detail")
                            showStartupFailure(
                                "The shared game interface could not be loaded.",
                                "$detail\n\nCheck that the Windows WebView assets are installed correctly, then restart GuessTheNumber."
                            )
                        }
                        else -> Unit
                    }
                }
                webEngine.locationProperty().addListener { _, _, location ->
                    val expected = localIndex?.toUri()?.toString()
                    if (!disposed && expected != null && location != expected && location != "about:blank") {
                        val externalUri = runCatching { URI(location) }.getOrNull()
                        if (
                            externalUri != null &&
                            externalUri.scheme in setOf("http", "https") &&
                            !externalUri.host.isNullOrBlank()
                        ) {
                            runCatching {
                                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                    Desktop.getDesktop().browse(externalUri)
                                }
                            }
                        }
                        Platform.runLater { if (!disposed) webEngine.load(expected) }
                    }
                }
                fxPanel.scene = Scene(webView)
                webEngine.load(index.toUri().toString())
            } catch (error: Throwable) {
                System.err.println("GuessTheNumber desktop WebView startup failed: ${error.stackTraceToString()}")
                showStartupFailure(
                    "The shared HTML interface could not start.",
                    "Check the bundled web assets and JavaFX runtime.\n\n" +
                        (error.message ?: error.javaClass.simpleName)
                )
            }
        }
        }
    }

    private fun showStartupFailure(title: String, detail: String) {
        if (disposed) return
        val heading = javafx.scene.control.Label(title).apply {
            isWrapText = true
            style = "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f7f8ff;"
        }
        val message = javafx.scene.control.Label(detail).apply {
            isWrapText = true
            style = "-fx-font-size: 13px; -fx-text-fill: #d5def5;"
        }
        val content = javafx.scene.layout.VBox(12.0, heading, message).apply {
            style = "-fx-background-color: #101522; -fx-padding: 32px;"
        }
        fxPanel.scene = Scene(
            javafx.scene.layout.StackPane(content).apply {
                style = "-fx-background-color: #101522;"
            }
        )
    }

    fun setRequestHandler(value: (String, (String) -> Unit) -> Unit) {
        handler.set(value)
        edgeController?.setRequestHandler(value)
    }

    fun setNavigationPositionHandler(value: (DesktopNavigationPosition) -> Unit) {
        navigationPositionHandler.set(value)
        edgeController?.setNavigationPositionHandler(value)
    }

    fun dispose() {
        disposed = true
        handler.set(null)
        if (edgeController != null) {
            SwingUtilities.invokeLater { edgeController.dispose() }
        } else {
            Platform.runLater {
                runCatching { engine?.load("about:blank") }
                engine = null
                localRoot?.let(::deleteResourceTree)
                localRoot = null
                localIndex = null
            }
        }
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

    private fun copyResourceTree(prefix: String, sentinel: String, destination: Path) {
        val classLoader = javaClass.classLoader
        val resource = classLoader.getResource(sentinel)
            ?: throw IOException("Missing bundled resource: $sentinel")
        val targetRoot = destination.resolve(prefix)
        Files.createDirectories(targetRoot)
        if (resource.protocol == "jar") {
            val connection = resource.openConnection() as java.net.JarURLConnection
            val entries = connection.jarFile.entries()
            val entryPrefix = prefix.trimEnd('/') + "/"
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.name.startsWith(entryPrefix)) continue
                val target = destination.resolve(entry.name)
                if (entry.isDirectory) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    connection.jarFile.getInputStream(entry).use { input ->
                        Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        } else {
            val suffixSegments = sentinel.removePrefix(prefix.trimEnd('/') + "/").split('/').size
            var sourceRoot = Path.of(resource.toURI())
            repeat(suffixSegments) {
                sourceRoot = sourceRoot.parent ?: throw IOException("Invalid resource root: $sentinel")
            }
            Files.walk(sourceRoot).use { paths ->
                paths.forEach { source ->
                    val relative = sourceRoot.relativize(source)
                    val target = targetRoot.resolve(relative.toString())
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target)
                    } else {
                        Files.createDirectories(target.parent)
                        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

    private fun unpackWebAssets(): Path {
        val root = Files.createTempDirectory("gtn-shared-web-play-")
        root.toFile().deleteOnExit()
        val webRoot = root.resolve("web-play")
        copyResourceTree("web-play", "web-play/index.html", root)
        copyResourceTree("web-play", "web-play/desktop-host.js", root)
        copyResourceTree("libraries", "libraries/jarrlyyuisdk/components/jarrlyy-ui.js", root)
        copyResourceTree("fonts", "fonts/Inter-VariableFont_opsz,wght.ttf", root)
        val index = webRoot.resolve("index.html")
        val debugToolsEnabled = javaClass.classLoader
            .getResourceAsStream("desktop-build.properties")
            ?.use { properties ->
                java.util.Properties().apply { load(properties) }
                    .getProperty("debugToolsBuildAllowed", "false")
                    .equals("true", ignoreCase = true)
            } ?: false
        val originalBase = Files.readString(index)
        // Developer JavaScript evaluation is permitted only in debug-tool packages.
        val original = if (debugToolsEnabled) {
            originalBase.replace("script-src 'self';", "script-src 'self' 'unsafe-eval';")
        } else originalBase
        val persistedNavigationPosition = DesktopNavigationPosition.load().name
        val launchAtStartup = DesktopStartupLaunch.isEnabled()
        val initialScreenJson = org.json.JSONObject.quote(initialScreen.get())
        val hostScript = webRoot.resolve("desktop-host.js")
        val hostOriginal = Files.readString(hostScript)
        val hostPatched = hostOriginal
            .replace(
                "window.gtnDesktopInitialScreen = window.gtnDesktopInitialScreen || 'play';",
                "window.gtnDesktopInitialScreen = $initialScreenJson;"
            )
            .replace(
                "window.gtnDesktopDebugTools = false;",
                "window.gtnDesktopDebugTools = $debugToolsEnabled;"
            )
            .replace(
                "window.gtnDesktopNavigationPosition = 'LEFT';",
                "window.gtnDesktopNavigationPosition = '$persistedNavigationPosition';"
            )
            .replace(
                "window.gtnDesktopLaunchAtStartup = false;",
                "window.gtnDesktopLaunchAtStartup = $launchAtStartup;"
            )
        Files.writeString(hostScript, hostPatched)
        val cspMarker = "Content-Security-Policy"
        val cspMarkerIndex = original.indexOf(cspMarker, ignoreCase = true)
        if (cspMarkerIndex < 0) {
            throw IOException("Shared web UI index.html has no Content Security Policy.")
        }
        val cspMetaStart = original.lastIndexOf("<meta", cspMarkerIndex, ignoreCase = true)
        val cspMetaEnd = original.indexOf('>', cspMarkerIndex)
        if (cspMetaStart < 0 || cspMetaEnd < 0) {
            throw IOException("Shared web UI Content Security Policy meta tag is malformed.")
        }
        val scriptTag = """<script src="desktop-host.js"></script>"""
        val patched = original.replaceRange(
            cspMetaStart,
            cspMetaEnd + 1,
            original.substring(cspMetaStart, cspMetaEnd + 1) +
                System.lineSeparator() + "  " + scriptTag
        )
        Files.writeString(index, patched)
        return index
    }

}

internal class DesktopJavaScriptBridge(
    private val dispatch: (String) -> Unit
) {
    fun postMessage(raw: String) {
        dispatch(raw)
    }
}
