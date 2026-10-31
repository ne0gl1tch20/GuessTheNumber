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
        audioToolsContent = { WindowsAudioAdapterCard() }
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
                "Uses the verified Windows Debug manifest, archive checksum and MSI installer.",
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Desktop Audio Adapter", style = MaterialTheme.typography.titleLarge)
            Text(
                "The Android Jarrlyy DSP test panel uses AAudio and is platform-specific. " +
                    "Windows keeps its desktop audio path separate; this screen does not pretend to run the Android native test engine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
