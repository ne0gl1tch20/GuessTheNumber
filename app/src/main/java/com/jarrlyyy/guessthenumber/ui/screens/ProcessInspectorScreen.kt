package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text(localizedText("Variable & Process Inspector"), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                Text(localizedText("Active Background Processes & Loops"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProcessCardRow(name = "Auto-Clicker Loop", active = state.autoClickerActive, detail = "Speed: ${state.autoClickerSpeed} clicks/sec", icon = Icons.Default.Autorenew)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProcessCardRow(name = "Auto-Save Worker", active = true, detail = "Interval: Every 30 seconds", icon = Icons.Default.Save)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProcessCardRow(name = "Playtime Timer", active = true, detail = "Elapsed: ${state.statistics.playtimeSeconds}s", icon = Icons.Default.Schedule)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProcessCardRow(name = "WorkManager Notifications", active = state.settings.notificationsEnabled, detail = "Frequency: ${state.settings.notificationIntervalHours}h", icon = Icons.Default.Notifications)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(localizedText("Complete Runtime Game State Variables"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            // Grouped Material 3 Variable Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Currencies & Resets
                    VariableGroupCard(title = "Currencies & Economy", icon = Icons.Default.MonetizationOn) {
                        VariableRowItem(key = "Money", value = state.money.format())
                        VariableRowItem(key = "Prestige", value = state.prestige.format())
                        VariableRowItem(key = "Ultra", value = state.ultra.format())
                        VariableRowItem(key = "Nebula", value = state.nebula.format())
                        VariableRowItem(key = "Money Earned", value = state.statistics.moneyEarned.format())
                        VariableRowItem(key = "Money Spent", value = state.statistics.moneySpent.format())
                    }

                    // Guessing Engine & Game State
                    VariableGroupCard(title = "Guessing Engine & Ranges", icon = Icons.Default.Casino) {
                        VariableRowItem(key = "Current Range Min", value = state.currentRangeMin.toString())
                        VariableRowItem(key = "Current Range Max", value = state.currentRangeMax.toString())
                        VariableRowItem(key = "Target Number", value = state.targetNumber.toString())
                        VariableRowItem(key = "Total Attempts", value = state.attempts.toString())
                        VariableRowItem(key = "Correct Guesses (State)", value = state.correctGuesses.toString())
                        VariableRowItem(key = "Current Streak", value = state.streak.toString())
                        VariableRowItem(key = "Best Streak", value = state.bestStreak.toString())
                        VariableRowItem(key = "Buy Multiplier", value = state.buyMultiplier)
                        VariableRowItem(key = "Auto-Clicker Active", value = state.autoClickerActive.toString())
                        VariableRowItem(key = "Auto-Clicker Speed", value = "${state.autoClickerSpeed}")
                        VariableRowItem(key = "Tutorial Completed", value = state.tutorialCompleted.toString())
                    }

                    // Statistics & Counts
                    VariableGroupCard(title = "Statistics & Counters", icon = Icons.Default.Analytics) {
                        VariableRowItem(key = "Total Guesses (Stats)", value = state.statistics.totalGuesses.toString())
                        VariableRowItem(key = "Correct Guesses (Stats)", value = state.statistics.correctGuesses.toString())
                        VariableRowItem(key = "Failed Guesses", value = state.statistics.failedGuesses.toString())
                        VariableRowItem(key = "Prestiges Count", value = state.statistics.prestigesCount.toString())
                        VariableRowItem(key = "Ultras Count", value = state.statistics.ultrasCount.toString())
                        VariableRowItem(key = "Nebula Earned", value = state.statistics.nebulaEarned.toString())
                        VariableRowItem(key = "Playtime Seconds", value = "${state.statistics.playtimeSeconds}s")
                        VariableRowItem(key = "Arcade Played", value = "${state.statistics.arcadePlayed}")
                        VariableRowItem(key = "Arcade Best Scores", value = state.statistics.arcadeBestScores.toString())
                    }

                    // Settings & Preferences
                    VariableGroupCard(title = "Settings & Preferences", icon = Icons.Default.Settings) {
                        VariableRowItem(key = "Music Enabled", value = state.settings.musicEnabled.toString())
                        VariableRowItem(key = "Sound Enabled", value = state.settings.soundEnabled.toString())
                        VariableRowItem(key = "Vibration Enabled", value = state.settings.vibrationEnabled.toString())
                        VariableRowItem(key = "Notifications Enabled", value = state.settings.notificationsEnabled.toString())
                        VariableRowItem(key = "Notification Interval", value = "${state.settings.notificationIntervalHours}h")
                        VariableRowItem(key = "Theme Mode", value = state.settings.themeMode)
                        VariableRowItem(key = "Number Notation", value = state.settings.numberNotation)
                        VariableRowItem(key = "Reduced Motion", value = state.settings.reducedMotion.toString())
                        VariableRowItem(key = "Volume Level", value = "${state.settings.volume}")
                        VariableRowItem(key = "Reduce Flashes", value = state.settings.reduceFlashes.toString())
                        VariableRowItem(key = "Save Logs to Storage", value = state.settings.saveLogsToStorage.toString())
                        VariableRowItem(key = "Background Music Path", value = state.settings.backgroundMusicPath ?: "None")
                    }

                    // Collections & Maps
                    VariableGroupCard(title = "Collections & Upgrades", icon = Icons.Default.Category) {
                        VariableRowItem(key = "Upgrade Levels Count", value = "${state.upgradeLevels.size} items\n${state.upgradeLevels.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }}")
                        VariableRowItem(key = "Prestige Upgrades Count", value = "${state.prestigeUpgradeLevels.size} items\n${state.prestigeUpgradeLevels.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }}")
                        VariableRowItem(key = "Ultra Upgrades Count", value = "${state.ultraUpgradeLevels.size} items\n${state.ultraUpgradeLevels.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }}")
                        VariableRowItem(key = "Shop Purchases", value = "${state.shopPurchases.size} items\n${state.shopPurchases}")
                        VariableRowItem(key = "Prestige Purchases", value = "${state.prestigeShopPurchases.size} items\n${state.prestigeShopPurchases}")
                        VariableRowItem(key = "Ultra Purchases", value = "${state.ultraShopPurchases.size} items\n${state.ultraShopPurchases}")
                        VariableRowItem(key = "Achievements Unlocked", value = "${state.achievements.size} items\n${state.achievements}")
                        VariableRowItem(key = "Completed Challenges", value = "${state.completedChallenges.size} items\n${state.completedChallenges}")
                    }

                    // Meta Timestamps & Anti-Cheat
                    VariableGroupCard(title = "Meta Timestamps & Anti-Cheat", icon = Icons.Default.Security) {
                        VariableRowItem(key = "Last Save Timestamp", value = state.lastSaveTimestamp.toString())
                        VariableRowItem(key = "Last Saved Version", value = state.lastSavedVersion)
                        VariableRowItem(key = "Time Travel Penalty Until", value = state.timeTravelPenaltyUntil.toString())
                        VariableRowItem(key = "Prestige Count (Root)", value = state.prestigeCount.toString())
                        VariableRowItem(key = "Ultra Count (Root)", value = state.ultraCount.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessCardRow(name: String, active: Boolean, detail: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                text = if (active) "RUNNING" else "STOPPED",
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun VariableGroupCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
fun VariableRowItem(key: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}
