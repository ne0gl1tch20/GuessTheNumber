package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.awt.Dimension
import com.jarrlyyy.guessthenumber.desktop.update.WindowsBuildVersion
import com.jarrlyyy.guessthenumber.desktop.update.WindowsDebugInstaller
import com.jarrlyyy.guessthenumber.desktop.update.WindowsDebugUpdate
import com.jarrlyyy.guessthenumber.desktop.update.WindowsDebugUpdateClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun DesktopNavigationBar(
    currentDestination: DesktopDestination,
    onNavigate: (DesktopDestination) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (shouldUseCompactNavigation(maxWidth)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DesktopNavigationButtons(currentDestination, onNavigate)
                Text("Windows Desktop", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DesktopNavigationButtons(currentDestination, onNavigate)
                Spacer(modifier = Modifier.weight(1f))
                Text("Windows Desktop", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DesktopNavigationButtons(
    currentDestination: DesktopDestination,
    onNavigate: (DesktopDestination) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DesktopDestination.entries.forEach { destination ->
            var isFocused by remember(destination) { mutableStateOf(false) }
            val focusModifier = Modifier
                .onFocusChanged { isFocused = it.isFocused }
                .then(
                    if (isFocused) {
                        Modifier.border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            RoundedCornerShape(12.dp)
                        )
                    } else {
                        Modifier
                    }
                )
            Button(
                modifier = focusModifier.semantics {
                    selected = currentDestination == destination
                },
                onClick = {
                    if (currentDestination != destination) onNavigate(destination)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = desktopNavigationContainerColor(
                        selected = currentDestination == destination,
                        colorScheme = MaterialTheme.colorScheme
                    ),
                    contentColor = desktopNavigationContentColor(
                        selected = currentDestination == destination,
                        colorScheme = MaterialTheme.colorScheme
                    )
                )
            ) {
                Text(destination.label)
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Guess The Number",
        icon = painterResource("GTN_APP_LOGO.png"),
        state = remember { WindowState(size = DpSize(DesktopWindowMetrics.INITIAL_WIDTH, DesktopWindowMetrics.INITIAL_HEIGHT)) },
        resizable = true
    ) {
        val density = LocalDensity.current
        LaunchedEffect(density) {
            window.minimumSize = Dimension(
                with(density) { DesktopWindowMetrics.MIN_WIDTH.roundToPx() },
                with(density) { DesktopWindowMetrics.MIN_HEIGHT.roundToPx() }
            )
        }
        DesktopTheme(window = window) {
            val scope = rememberCoroutineScope()
            val shellFocusRequester = remember { FocusRequester() }
            val shell = remember { DesktopShellController() }
            val shellState = shell.state
            val updateClient = remember { WindowsDebugUpdateClient() }
            val installer = remember { WindowsDebugInstaller() }
            var availableUpdate by remember { mutableStateOf<WindowsDebugUpdate?>(null) }
            var status by remember { mutableStateOf("Checking for Windows Debug updates…") }
            var isBusy by remember { mutableStateOf(true) }

            suspend fun refreshUpdates() {
                isBusy = true
                status = "Checking for Windows Debug updates…"
                try {
                    availableUpdate = withContext(Dispatchers.IO) {
                        updateClient.findLatestUpdate(WindowsBuildVersion.current)
                    }
                    status = if (availableUpdate == null) {
                        "You're using the latest published Windows Debug build."
                    } else {
                        "Version ${availableUpdate!!.version} is ready to install."
                    }
                } catch (error: Exception) {
                    status = "Update check failed: ${error.message ?: "network error"}"
                } finally {
                    isBusy = false
                }
            }

            LaunchedEffect(Unit) {
                refreshUpdates()
            }
            LaunchedEffect(shellFocusRequester) {
                shellFocusRequester.requestFocus()
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            val handled = shell.handleKey(
                                key = event.key,
                                ctrlPressed = event.isCtrlPressed
                            )
                            if (handled && event.key == Key.Escape) {
                                shellFocusRequester.requestFocus()
                            }
                            handled
                        }
                        .focusRequester(shellFocusRequester)
                        .focusable(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DesktopNavigationBar(
                        currentDestination = shellState.destination,
                        onNavigate = shell::navigate
                    )

                    if (shellState.message != null) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                shellState.message.orEmpty(),
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    when (shellState.destination) {
                        DesktopDestination.PLAY,
                        DesktopDestination.PROGRESSION,
                        DesktopDestination.ECONOMY -> {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                ProductionGuessGameScreen(
                                    section = when (shellState.destination) {
                                        DesktopDestination.PROGRESSION -> DesktopGameSection.PROGRESSION
                                        DesktopDestination.ECONOMY -> DesktopGameSection.ECONOMY
                                        else -> DesktopGameSection.PLAY
                                    }
                                )
                            }
                        }

                        DesktopDestination.ABOUT -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Windows Debug updates", style = MaterialTheme.typography.titleMedium)
                                    Text("Installed version: ${WindowsBuildVersion.current}")
                                    Text(
                                        status,
                                        color = if (
                                            classifyDesktopUpdateStatus(
                                                status = status,
                                                isBusy = isBusy,
                                                hasUpdate = availableUpdate != null
                                            ) == DesktopUpdateStatusKind.ERROR
                                        ) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.semantics {
                                            liveRegion = LiveRegionMode.Polite
                                        }
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { scope.launch { refreshUpdates() } },
                                            enabled = !isBusy
                                        ) { Text("Check again") }
                                        val update = availableUpdate
                                        if (update != null) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        isBusy = true
                                                        status = "Downloading and verifying version ${update.version}…"
                                                        try {
                                                            withContext(Dispatchers.IO) {
                                                                installer.downloadVerifyAndLaunch(update)
                                                            }
                                                            status = "Windows Installer is opening. GuessTheNumber will close for the update."
                                                            exitApplication()
                                                        } catch (error: Exception) {
                                                            status = "Update failed: ${error.message ?: "installer error"}"
                                                            isBusy = false
                                                        }
                                                    }
                                                },
                                                enabled = !isBusy
                                            ) { Text("Install update") }
                                        }
                                        if (isBusy) {
                                            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = status })
                                        }
                                    }
                                    Text(
                                        "Debug builds are separate from production releases. Windows may ask for permission to install the MSI.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("About Guess The Number", style = MaterialTheme.typography.headlineSmall)
                                    Text("Windows desktop edition")
                                    Text("Designed for offline play. No ads, analytics or accounts.")
                                    Text("This Windows build is under active development. Some Android features are not available on Windows yet.")
                                    Text("Installed version: ${WindowsBuildVersion.current}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
