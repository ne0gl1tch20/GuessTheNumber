package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessInspectorScreen(
    state: GameState,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Variable & Process Inspector") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Active Background Processes & Loops", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E1E1E),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProcessRow(name = "Auto-Clicker Loop", active = state.autoClickerActive, detail = "Speed: ${state.autoClickerSpeed} clicks/sec")
                        ProcessRow(name = "Auto-Save Worker", active = true, detail = "Interval: Every 30 seconds")
                        ProcessRow(name = "Playtime Timer", active = true, detail = "Elapsed: ${state.statistics.playtimeSeconds}s")
                        ProcessRow(name = "WorkManager Notifications", active = state.settings.notificationsEnabled, detail = "Frequency: ${state.settings.notificationIntervalHours}h")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Runtime Game State Variables", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E1E1E),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VariableRow(key = "Money", value = state.money.format())
                        VariableRow(key = "Prestige", value = state.prestige.format())
                        VariableRow(key = "Ultra", value = state.ultra.format())
                        VariableRow(key = "Nebula", value = state.nebula.format())
                        VariableRow(key = "Current Range", value = "${state.currentRangeMin} .. ${state.currentRangeMax}")
                        VariableRow(key = "Target Number", value = state.targetNumber.toString())
                        VariableRow(key = "Current Streak", value = state.streak.toString())
                        VariableRow(key = "Best Streak", value = state.bestStreak.toString())
                        VariableRow(key = "Buy Multiplier", value = state.buyMultiplier)
                        VariableRow(key = "Number Notation", value = state.settings.numberNotation)
                        VariableRow(key = "Theme Mode", value = state.settings.themeMode)
                        VariableRow(key = "Sound Enabled", value = state.settings.soundEnabled.toString())
                        VariableRow(key = "Vibration Enabled", value = state.settings.vibrationEnabled.toString())
                        VariableRow(key = "Total Guesses", value = state.statistics.totalGuesses.toString())
                        VariableRow(key = "Correct Guesses", value = state.statistics.correctGuesses.toString())
                        VariableRow(key = "Prestiges Count", value = state.statistics.prestigesCount.toString())
                        VariableRow(key = "Ultras Count", value = state.statistics.ultrasCount.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessRow(name: String, active: Boolean, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, color = Color(0xFF50FA7B), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text(detail, color = Color(0xFF888888), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Text(
            text = if (active) "[RUNNING]" else "[STOPPED]",
            color = if (active) Color(0xFF50FA7B) else Color(0xFFFF5555),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
fun VariableRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$key:", color = Color(0xFF8BE9FD), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(value, color = Color(0xFFF8F8F2), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}
