package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.scripting.LuaActivityDefinition
import com.jarrlyyy.guessthenumber.scripting.LuaAnswerResult
import com.jarrlyyy.guessthenumber.scripting.LuaEngine
import com.jarrlyyy.guessthenumber.scripting.LuaLiveOpsRefreshResult
import com.jarrlyyy.guessthenumber.scripting.LuaStartResult
import com.jarrlyyy.guessthenumber.scripting.LuaScriptEntry
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import com.jarrlyyy.guessthenumber.ui.localization.localizedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuaActivityHubScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalAppLocaleManager.current
    val engine = remember(context) { LuaEngine.get(context) }
    val scope = rememberCoroutineScope()
    var scripts by remember { mutableStateOf<List<LuaScriptEntry>>(emptyList()) }
    var activity by remember { mutableStateOf(engine.currentActivity()) }
    var selectedAnswer by remember { mutableIntStateOf(-1) }
    var answerMessage by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { scripts = withContext(Dispatchers.IO) { engine.listScripts() } }
    DisposableEffect(Unit) { onDispose { engine.stop() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText("Lua Activity Hub")) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { engine.stop(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = locale.getString("back", "Back"))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(localizedText("Script-powered activities"), style = MaterialTheme.typography.titleLarge)
                        Text(
                            localizedText("Quick minigames run offline from the app bundle. Online event scripts are accepted only after their signed bundle is verified."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${scripts.size}", style = MaterialTheme.typography.headlineMedium)
                            Text(localizedText("registered activities"), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(localizedText("Featured activity"), style = MaterialTheme.typography.titleMedium)
                        if (activity == null) {
                            val featured = scripts.firstOrNull { it.featured && it.source == "liveops" }
                                ?: scripts.firstOrNull { it.featured }
                            Text(localizedText(featured?.title ?: "No featured activity"), style = MaterialTheme.typography.headlineSmall)
                            Text(
                                localizedText(featured?.description ?: "No featured Lua activity is available."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    if (busy) return@Button
                                    busy = true
                                    feedback = null
                                    scope.launch {
                                        val result = withContext(Dispatchers.Default) { engine.startFeaturedActivity() }
                                        when (result) {
                                            is LuaStartResult.Started -> {
                                                activity = result.activity
                                                selectedAnswer = -1
                                                answerMessage = null
                                            }
                                            is LuaStartResult.Rejected -> feedback = result.reason
                                        }
                                        scripts = withContext(Dispatchers.IO) { engine.listScripts() }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (busy) CircularProgressIndicator()
                                else Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                Text(localizedText("GO"))
                            }
                        } else {
                            Text(localizedText(activity!!.title), style = MaterialTheme.typography.headlineSmall)
                            Text(localizedText(activity!!.description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider()
                            Text(localizedText(activity!!.prompt), style = MaterialTheme.typography.titleMedium)
                            activity!!.options.forEachIndexed { index, option ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = answerMessage == null) {
                                        selectedAnswer = index
                                    }.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedAnswer == index,
                                        onClick = { if (answerMessage == null) selectedAnswer = index },
                                        enabled = answerMessage == null
                                    )
                                    Text(localizedText(option), modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                            if (answerMessage == null) {
                                Button(
                                    onClick = {
                                        when (val result = engine.submitAnswer(selectedAnswer)) {
                                            is LuaAnswerResult.Answered -> answerMessage = result.message
                                            is LuaAnswerResult.Rejected -> feedback = result.reason
                                        }
                                    },
                                    enabled = selectedAnswer >= 0,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(localizedText("Submit answer")) }
                            } else {
                                Text(localizedText(answerMessage!!), style = MaterialTheme.typography.titleMedium)
                                Button(
                                    onClick = {
                                        engine.stop()
                                        activity = null
                                        answerMessage = null
                                        selectedAnswer = -1
                                        feedback = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(localizedText("Done")) }
                            }
                        }
                        feedback?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        if (busy) return@OutlinedButton
                        busy = true
                        feedback = null
                        scope.launch {
                            val result = engine.refreshLiveOps()
                            feedback = when (result) {
                                is LuaLiveOpsRefreshResult.Updated ->
                                    locale.getString("lua_liveops_updated_format", "Updated LiveOps bundle %d (%d scripts).", result.bundleVersion, result.scriptCount)
                                is LuaLiveOpsRefreshResult.Unavailable -> result.reason
                            }
                            scripts = withContext(Dispatchers.IO) { engine.listScripts() }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(localizedText("Refresh signed LiveOps bundle"))
                }
            }

            item {
                Text(localizedText("Available scripts"), style = MaterialTheme.typography.titleMedium)
            }
            items(scripts.size) { index ->
                val script = scripts[index]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(script.title, style = MaterialTheme.typography.titleMedium)
                        Text(script.description, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${script.source} · API v${script.hostApiVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
