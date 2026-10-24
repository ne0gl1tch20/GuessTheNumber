package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.command.CommandExecutor
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessInspectorScreen(state: GameState, onBack: () -> Unit) {
    var playgroundState by remember { mutableStateOf(state) }
    var destination by remember { mutableStateOf("playground") }
    val executor = remember { CommandExecutor() }
    val execute: (String) -> String = { command ->
        var result = ""
        result = executor.execute(command, playgroundState) { updated -> playgroundState = updated }
        result
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (destination == "playground") "Developer Playground" else "Runtime Inspector") },
            navigationIcon = { IconButton(onClick = { if (destination == "playground") onBack() else destination = "playground" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (destination == "playground") {
                DeveloperPlaygroundScreen(state = playgroundState, onExecuteCommand = execute, onBack = onBack)
            } else {
                RuntimeInspectorScreen(state = playgroundState, onBack = { destination = "playground" })
            }
        }
    }
}
