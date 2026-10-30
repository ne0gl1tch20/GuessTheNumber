package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DesktopActivityHubScreen(
    onBack: () -> Unit,
    engine: DesktopLuaActivityEngine = remember { DesktopLuaActivityEngine() }
) {
    var activity by remember { mutableStateOf(engine.currentActivity()) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var answered by remember { mutableStateOf(false) }
    val scripts = remember { runCatching { engine.listScripts() } }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                engine.stop()
                activity = null
                feedback = null
                answered = false
                onBack()
            }) { Text("Back to More") }
            Text("Activity Hub", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Small offline activities run in a restricted Lua sandbox. Scripts can provide activity content, but cannot edit saves, currency or rewards.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (scripts.isFailure) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Activity catalog unavailable: ${scripts.exceptionOrNull()?.message ?: "unknown error"}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (activity == null) {
            scripts.getOrNull().orEmpty().forEach { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(entry.title, style = MaterialTheme.typography.titleMedium)
                        Text(entry.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Bundled · Host API v${entry.hostApiVersion}", style = MaterialTheme.typography.labelSmall)
                        Button(
                            onClick = {
                                when (val result = engine.start(entry.id)) {
                                    is DesktopLuaStartResult.Started -> {
                                        activity = result.activity
                                        feedback = null
                                        answered = false
                                    }
                                    is DesktopLuaStartResult.Rejected -> feedback = result.reason
                                }
                            }
                        ) { Text("Start activity") }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(activity!!.title, style = MaterialTheme.typography.titleLarge)
                    Text(activity!!.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(activity!!.prompt, style = MaterialTheme.typography.titleMedium)
                    activity!!.options.forEachIndexed { index, option ->
                        OutlinedButton(
                            onClick = {
                                val result = engine.submitAnswer(index)
                                if (result != null) {
                                    feedback = result.message
                                    answered = true
                                }
                            },
                            enabled = !answered,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(option) }
                    }
                    feedback?.let {
                        Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (answered) {
                            Button(onClick = {
                                engine.stop()
                                activity = null
                                feedback = null
                                answered = false
                            }) { Text("Finish activity") }
                        }
                        OutlinedButton(onClick = {
                            engine.stop()
                            activity = null
                            feedback = null
                            answered = false
                        }) { Text("Stop") }
                    }
                }
            }
        }
        feedback?.takeIf { activity == null }?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
