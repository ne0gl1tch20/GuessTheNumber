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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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
        title = "Guess The Number"
    ) {
        MaterialTheme {
            val scope = rememberCoroutineScope()
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

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                    Text("Update and restart")
                                }
                            }
                            if (isBusy) {
                                Spacer(modifier = Modifier.padding(2.dp))
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
        }
    }
}
