package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
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
import com.jarrlyyy.guessthenumber.shared.ui.SharedGuessGameScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Guess The Number",
        state = remember { WindowState(size = DpSize(1100.dp, 760.dp)) },
        resizable = true
    ) {
        val density = LocalDensity.current
        LaunchedEffect(density) {
            window.minimumSize = Dimension(
                with(density) { 800.dp.roundToPx() },
                with(density) { 600.dp.roundToPx() }
            )
        }
        DesktopTheme {
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

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
                    .focusRequester(shellFocusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }
                    val destination = DesktopDestination.fromShortcut(
                        key = event.key,
                        ctrlPressed = event.isCtrlPressed
                    ) ?: return@onPreviewKeyEvent false
                    shell.navigate(destination)
                    true
                },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopDestination.entries.forEach { destination ->
                        Button(
                            onClick = { shell.navigate(destination) },
                            enabled = shellState.destination != destination
                        ) {
                            Text(destination.label)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Windows Desktop", style = MaterialTheme.typography.labelLarge)
                }

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
                    DesktopDestination.PLAY -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Windows Debug updates", style = MaterialTheme.typography.titleMedium)
                                Text("Installed version: ${WindowsBuildVersion.current}")
                                Text(status)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { scope.launch { refreshUpdates() } },
                                        enabled = !isBusy
                                    ) {
                                        Text("Check again")
                                    }
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
                                        ) {
                                            Text("Install update")
                                        }
                                    }
                                    if (isBusy) {
                                        CircularProgressIndicator()
                                    }
                                }
                                Text(
                                    "Debug builds are separate from production releases. Windows may ask for permission to install the MSI.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            SharedGuessGameScreen()
                        }
                    }

                    DesktopDestination.ABOUT -> {
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
