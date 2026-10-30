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
    private val fxPanel = JFXPanel()
    private val handler = AtomicReference<((String, (String) -> Unit) -> Unit)>()
    private val initialScreen = AtomicReference(initialScreenValue.also {
        require(it in setOf("play", "more", "changelog", "about", "music", "upgrades", "shop", "prestige", "ultra", "talent", "settings", "rewards", "worlds", "mutators", "relics", "homebase", "codex", "journey", "stats", "arcade", "stake"))
    })
    private val navigationPositionHandler = AtomicReference<(DesktopNavigationPosition) -> Unit>({})
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
                    if (state == Worker.State.SCHEDULED || state == Worker.State.RUNNING || state == Worker.State.SUCCEEDED) {
                        runCatching {
                            (webEngine.executeScript("window") as JSObject).setMember("gtnDesktopBridge", bridge)
                            webEngine.executeScript("window.gtnHost && window.gtnHost.flush()")
                        }
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

    fun setNavigationPositionHandler(value: (DesktopNavigationPosition) -> Unit) {
        navigationPositionHandler.set(value)
    }

    fun dispose() {
        disposed = true
        handler.set(null)
        Platform.runLater {
            runCatching { engine?.load("about:blank") }
            engine = null
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
        copyResourceTree("libraries", "libraries/jarrlyyuisdk/components/jarrlyy-ui.js", root)
        copyResourceTree("fonts", "fonts/Inter-VariableFont_opsz,wght.ttf", root)
        val index = webRoot.resolve("index.html")
        val original = Files.readString(index)
        val persistedNavigationPosition = DesktopNavigationPosition.load().name
        val launchAtStartup = DesktopStartupLaunch.isEnabled()
        val initialScreenJson = org.json.JSONObject.quote(initialScreen.get())
        val shim = """
            <script>
            // Capabilities are injected by the Windows host only.
            window.gtnDesktopInitialScreen = $initialScreenJson;
            // Android never sees this flag.
            window.gtnDesktopFeatures = Object.freeze({
              navigationPosition: true,
              launchAtStartup: true
            });
            window.gtnDesktopNavigationPosition = '$persistedNavigationPosition';
            window.gtnDesktopLaunchAtStartup = $launchAtStartup;
            window.gtnHost = {
              onmessage: null,
              queue: [],
              postMessage: function(raw) {
                var message = String(raw);
                if (window.gtnDesktopBridge) window.gtnDesktopBridge.postMessage(message);
                else this.queue.push(message);
              },
              flush: function() {
                if (!window.gtnDesktopBridge) return;
                while (this.queue.length) window.gtnDesktopBridge.postMessage(this.queue.shift());
              },
              receive: function(raw) {
                var parsed = null;
                try { parsed = JSON.parse(String(raw)); } catch (_) {}
                if (parsed) window.dispatchEvent(new CustomEvent('gtn-desktop-settings-response', {detail: parsed}));
                if (typeof this.onmessage === 'function') this.onmessage({data: String(raw)});
              }
            };
            (function() {
              function applyNavigationPosition(position) {
                var value = String(position || 'LEFT').toUpperCase();
                if (!['LEFT','RIGHT','TOP','BOTTOM'].includes(value)) value = 'LEFT';
                document.documentElement.dataset.desktopNavigationPosition = value;
                if (document.body) document.body.dataset.desktopNavigationPosition = value;
              }
              function requestPosition(value) {
                applyNavigationPosition(value);
                window.gtnHost.postMessage(JSON.stringify({
                  version: 1,
                  id: 'desktop-nav-' + Date.now(),
                  type: 'update_setting',
                  key: 'desktopNavigationPosition',
                  value: String(value).toUpperCase()
                }));
              }
              function installDesktopSettings() {
                if (!window.gtnDesktopFeatures) return;
                var screen = document.querySelector('[data-screen="settings"]');
                if (!screen || screen.querySelector('[data-desktop-capabilities-setting]')) return;
                var group = document.createElement('div');
                group.className = 'settings-group desktop-capabilities-setting';
                group.dataset.desktopCapabilitiesSetting = 'true';
                group.innerHTML = '<h3>Windows Desktop</h3>' +
                  '<p class="settings-description">These options are available only in the Windows desktop app.</p>' +
                  '<div data-desktop-navigation-setting>' +
                    '<label class="jui-field-label" for="desktopNavigationPosition">Navigation Bar Position</label>' +
                    '<select id="desktopNavigationPosition" class="jui-field" aria-label="Navigation Bar Position">' +
                    '<option value="LEFT">Left (default)</option><option value="TOP">Top</option><option value="RIGHT">Right</option><option value="BOTTOM">Bottom</option>' +
                    '</select>' +
                  '</div>' +
                  '<label class="desktop-startup-setting" for="desktopLaunchAtStartup">' +
                    '<input id="desktopLaunchAtStartup" type="checkbox" /> Launch GuessTheNumber when Windows starts' +
                  '</label>' +
                  '<p class="settings-description">Uses your Windows account startup registration. You can turn it off here at any time.</p>';
                var target = screen.querySelector('.catalog-panel') || screen;
                target.appendChild(group);
                var select = group.querySelector('#desktopNavigationPosition');
                select.value = String(window.gtnDesktopNavigationPosition || 'LEFT').toUpperCase();
                select.addEventListener('change', function() { requestPosition(select.value); });
                var startup = group.querySelector('#desktopLaunchAtStartup');
                startup.checked = window.gtnDesktopLaunchAtStartup === true;
                startup.addEventListener('change', function() {
                  window.gtnHost.postMessage(JSON.stringify({
                    version: 1,
                    id: 'desktop-startup-' + Date.now(),
                    type: 'update_setting',
                    key: 'desktopLaunchAtStartup',
                    value: startup.checked
                  }));
                });
                window.addEventListener('gtn-desktop-settings-response', function(event) {
                  var response = event.detail || {};
                  if (response.id && response.id.indexOf('desktop-startup-') === 0 && response.type === 'error') {
                    startup.checked = !startup.checked;
                    startup.setAttribute('aria-invalid', 'true');
                    startup.title = response.message || 'Could not update Windows startup setting.';
                  } else if (response.id && response.id.indexOf('desktop-startup-') === 0 && response.type === 'state') {
                    startup.removeAttribute('aria-invalid');
                    startup.title = response.state && response.state.message || '';
                  }
                });
              }
              applyNavigationPosition(window.gtnDesktopNavigationPosition);
              if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', installDesktopSettings);
              else installDesktopSettings();
              new MutationObserver(installDesktopSettings).observe(document.documentElement, {childList:true,subtree:true});
              window.gtnDesktopApplyNavigationPosition = applyNavigationPosition;
              document.addEventListener('DOMContentLoaded', function() {
                if (typeof window.gtnActivateScreen === 'function') {
                  window.gtnActivateScreen(window.gtnDesktopInitialScreen || 'play');
                }
              }, {once:true});
            })();
            </script>
        """.trimIndent()
        val head = Regex("(?i)<head[^>]*>").find(original)
            ?: throw IOException("Shared web UI index.html has no head element.")
        val patched = original.replaceRange(head.range, head.value + shim)
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
