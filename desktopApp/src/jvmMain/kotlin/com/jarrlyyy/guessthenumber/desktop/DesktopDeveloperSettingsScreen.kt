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
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

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
    val scope = rememberCoroutineScope()
    val devices = remember {
        runCatching { AudioSystem.getMixerInfo().map { it.name.take(100) }.distinct() }
            .getOrDefault(emptyList())
    }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready to test the Windows audio output.") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Audio Diagnostics", style = MaterialTheme.typography.titleLarge)
            Text(
                "Inspect Java Sound output devices and play a short local test tone. This tests the desktop audio route without pretending to run Android AAudio or the native Jarrlyy DSP engine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Detected audio devices: ${devices.size}", style = MaterialTheme.typography.labelLarge)
            devices.take(8).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            if (devices.isEmpty()) Text("No Java Sound mixer devices were reported by this runtime.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        busy = true
                        status = "Playing 440 Hz test tone…"
                        try {
                            val format = AudioFormat(44_100f, 16, 1, true, false)
                            val line = AudioSystem.getSourceDataLine(format)
                            line.open(format)
                            line.start()
                            val frames = 11_025
                            val bytes = ByteArray(frames * 2)
                            for (frame in 0 until frames) {
                                val sample = (kotlin.math.sin(2.0 * Math.PI * 440.0 * frame / 44_100.0) * 8_000.0).toInt().toShort()
                                bytes[frame * 2] = (sample.toInt() and 0xff).toByte()
                                bytes[frame * 2 + 1] = ((sample.toInt() shr 8) and 0xff).toByte()
                            }
                            line.write(bytes, 0, bytes.size)
                            line.drain()
                            line.stop()
                            line.close()
                            status = "Test tone played successfully."
                        } catch (error: Exception) {
                            status = "Audio test failed: ${error.message ?: "no output device available"}"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy
            ) { Text(if (busy) "Testing…" else "Play test tone") }
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
