package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeInspectorScreen(state: GameState, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Runtime Inspector") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Active Background Processes & Loops", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProcessCardRow("Auto-Clicker Loop", state.autoClickerActive, "Speed: ${state.autoClickerSpeed} clicks/sec", Icons.Default.Autorenew)
                HorizontalDivider(); ProcessCardRow("Auto-Save Worker", true, "Interval: Every 30 seconds", Icons.Default.Save)
                HorizontalDivider(); ProcessCardRow("Playtime Timer", true, "Elapsed: ${state.statistics.playtimeSeconds}s", Icons.Default.Schedule)
                HorizontalDivider(); ProcessCardRow("WorkManager Notifications", state.settings.notificationsEnabled, "Frequency: ${state.settings.notificationIntervalHours}h", Icons.Default.Notifications)
            } } }
            item { Text("Complete Runtime Game State Variables", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            item { VariableGroupCard("Currencies & Economy", Icons.Default.MonetizationOn) { VariableRowItem("Money", state.money.format()); VariableRowItem("Prestige", state.prestige.format()); VariableRowItem("Ultra", state.ultra.format()); VariableRowItem("Nebula", state.nebula.format()); VariableRowItem("Money Earned", state.statistics.moneyEarned.format()); VariableRowItem("Money Spent", state.statistics.moneySpent.format()) } }
            item { VariableGroupCard("Guessing Engine & Ranges", Icons.Default.Casino) { VariableRowItem("Current Range Min", state.currentRangeMin.toString()); VariableRowItem("Current Range Max", state.currentRangeMax.toString()); VariableRowItem("Target Number", state.targetNumber.toString()); VariableRowItem("Total Attempts", state.attempts.toString()); VariableRowItem("Correct Guesses", state.correctGuesses.toString()); VariableRowItem("Current Streak", state.streak.toString()); VariableRowItem("Best Streak", state.bestStreak.toString()); VariableRowItem("Buy Multiplier", state.buyMultiplier); VariableRowItem("Auto-Clicker Active", state.autoClickerActive.toString()); VariableRowItem("Auto-Clicker Speed", state.autoClickerSpeed.toString()); VariableRowItem("Tutorial Completed", state.tutorialCompleted.toString()) } }
            item { VariableGroupCard("Statistics & Counters", Icons.Default.Analytics) { VariableRowItem("Total Guesses", state.statistics.totalGuesses.toString()); VariableRowItem("Correct Guesses", state.statistics.correctGuesses.toString()); VariableRowItem("Failed Guesses", state.statistics.failedGuesses.toString()); VariableRowItem("Prestiges Count", state.statistics.prestigesCount.toString()); VariableRowItem("Ultras Count", state.statistics.ultrasCount.toString()); VariableRowItem("Nebula Earned", state.statistics.nebulaEarned.toString()); VariableRowItem("Playtime Seconds", "${state.statistics.playtimeSeconds}s"); VariableRowItem("Arcade Played", state.statistics.arcadePlayed.toString()); VariableRowItem("Arcade Best Scores", state.statistics.arcadeBestScores.toString()) } }
            item { VariableGroupCard("Settings & Preferences", Icons.Default.Settings) { VariableRowItem("Music Enabled", state.settings.musicEnabled.toString()); VariableRowItem("Sound Enabled", state.settings.soundEnabled.toString()); VariableRowItem("Vibration Enabled", state.settings.vibrationEnabled.toString()); VariableRowItem("Notifications Enabled", state.settings.notificationsEnabled.toString()); VariableRowItem("Theme Mode", state.settings.themeMode); VariableRowItem("Number Notation", state.settings.numberNotation); VariableRowItem("Reduced Motion", state.settings.reducedMotion.toString()); VariableRowItem("Save Logs to Storage", state.settings.saveLogsToStorage.toString()) } }
            item { VariableGroupCard("Collections & Upgrades", Icons.Default.Category) { VariableRowItem("Upgrade Levels", state.upgradeLevels.toString()); VariableRowItem("Prestige Upgrades", state.prestigeUpgradeLevels.toString()); VariableRowItem("Ultra Upgrades", state.ultraUpgradeLevels.toString()); VariableRowItem("Shop Purchases", state.shopPurchases.toString()); VariableRowItem("Prestige Purchases", state.prestigeShopPurchases.toString()); VariableRowItem("Ultra Purchases", state.ultraShopPurchases.toString()); VariableRowItem("Achievements", state.achievements.toString()); VariableRowItem("Challenges", state.completedChallenges.toString()) } }
            item { VariableGroupCard("Meta Timestamps & Anti-Cheat", Icons.Default.Security) { VariableRowItem("Last Save Timestamp", state.lastSaveTimestamp.toString()); VariableRowItem("Last Saved Version", state.lastSavedVersion); VariableRowItem("Time Travel Penalty Until", state.timeTravelPenaltyUntil.toString()); VariableRowItem("Prestige Count", state.prestigeCount.toString()); VariableRowItem("Ultra Count", state.ultraCount.toString()) } }
        }
    }
}

@Composable
private fun ProcessCardRow(name: String, active: Boolean, detail: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(36.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) } }
            Column { Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text(if (active) "RUNNING" else "STOPPED", style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun VariableGroupCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)); Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }; content() } }
}

@Composable
private fun VariableRowItem(key: String, value: String) { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace) } }
