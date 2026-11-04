package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.desktop.update.WindowsBuildVersion
import com.jarrlyyy.guessthenumber.desktop.update.WindowsDebugInstaller
import com.jarrlyyy.guessthenumber.desktop.update.WindowsDebugUpdate
import com.jarrlyyy.guessthenumber.desktop.update.WindowsDebugUpdateClient
import com.jarrlyyy.guessthenumber.shared.ui.SharedDeveloperSettingsScreen
import com.jarrlyyy.guessthenumber.shared.ui.SeasonalThemeSimulatorCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DesktopDeveloperSettingsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    SharedDeveloperSettingsScreen(
        onBack = onBack,
        onNavigate = onNavigate,
        updateToolsContent = { WindowsUpdateToolsCard() },
        audioToolsContent = { WindowsAudioAdapterCard() },
        seasonSimulatorContent = { SeasonalThemeSimulatorCard() }
    )
}

@Composable
private fun WindowsUpdateToolsCard() {
    val scope = rememberCoroutineScope()
    val updateClient = remember { WindowsDebugUpdateClient() }
    val installer = remember { WindowsDebugInstaller() }
    var availableUpdate by remember { mutableStateOf<WindowsDebugUpdate?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Check the Windows Debug channel for a newer desktop build.") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Windows Desktop Updater", style = MaterialTheme.typography.titleLarge)
            Text(
                "Uses the verified Windows Debug manifest, archive checksum and Inno Setup installer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        availableUpdate = null
                        status = "Checking the Windows Debug channel…"
                        try {
                            availableUpdate = withContext(Dispatchers.IO) {
                                updateClient.findLatestUpdate(WindowsBuildVersion.current)
                            }
                            status = if (availableUpdate == null) {
                                "You're using the latest available Windows Debug build."
                            } else {
                                "Version ${availableUpdate!!.version} is ready to install."
                            }
                        } catch (error: Exception) {
                            status = "Update check failed: ${error.message ?: "network error"}"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy
            ) {
                Text(if (busy) "Checking…" else "Check for updates")
            }
            availableUpdate?.let { update ->
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = "Downloading, verifying and launching version ${update.version}…"
                            try {
                                withContext(Dispatchers.IO) { installer.downloadVerifyAndLaunch(update) }
                                status = "The verified installer was launched."
                            } catch (error: Exception) {
                                status = "Install launch failed: ${error.message ?: "unknown error"}"
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy
                ) {
                    Text("Install ${update.version}")
                }
            }
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WindowsAudioAdapterCard() {
    var status by remember { mutableStateOf("JarrlyyDSP is prewarmed in the background while the app runs.") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("JarrlyyDSP Audio", style = MaterialTheme.typography.titleLarge)
            Text(
                "Windows uses the desktop JarrlyyDSP engine. Preset PCM and audio clips are prepared at app startup, then button sounds start from the warm pool without allocating audio devices or synthesizing sound on the input thread.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    status = if (DesktopJarrlyyDsp.play(preset = 1, volume = 1f)) {
                        "JarrlyyDSP test sound queued. Playback runs asynchronously."
                    } else {
                        "JarrlyyDSP could not play this preset. Check the Windows audio output device."
                    }
                }
            ) { Text("Play DSP test sound") }
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
