package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager

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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localizedText("Back"))
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
                        ProcessCardRow(name = localizedText("Auto-Clicker Loop"), active = state.autoClickerActive, detail = LocalAppLocaleManager.current.getString("inspector_speed_format", "Speed: %s clicks/sec", state.autoClickerSpeed), icon = Icons.Default.Autorenew)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProcessCardRow(name = localizedText("Auto-Save Worker"), active = true, detail = localizedText("Interval: Every 30 seconds"), icon = Icons.Default.Save)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProcessCardRow(name = localizedText("Playtime Timer"), active = true, detail = LocalAppLocaleManager.current.getString("inspector_elapsed_format", "Elapsed: %ss", state.statistics.playtimeSeconds), icon = Icons.Default.Schedule)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProcessCardRow(name = localizedText("WorkManager Notifications"), active = state.settings.notificationsEnabled, detail = LocalAppLocaleManager.current.getString("inspector_frequency_format", "Frequency: %sh", state.settings.notificationIntervalHours), icon = Icons.Default.Notifications)
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
                    VariableGroupCard(title = localizedText("Currencies & Economy"), icon = Icons.Default.MonetizationOn) {
                        VariableRowItem(key = localizedText("Money"), value = state.money.format())
                        VariableRowItem(key = localizedText("Prestige"), value = state.prestige.format())
                        VariableRowItem(key = localizedText("Ultra"), value = state.ultra.format())
                        VariableRowItem(key = localizedText("Nebula"), value = state.nebula.format())
                        VariableRowItem(key = localizedText("Money Earned"), value = state.statistics.moneyEarned.format())
                        VariableRowItem(key = localizedText("Money Spent"), value = state.statistics.moneySpent.format())
                    }

                    // Guessing Engine & Game State
                    VariableGroupCard(title = localizedText("Guessing Engine & Ranges"), icon = Icons.Default.Casino) {
                        VariableRowItem(key = localizedText("Current Range Min"), value = state.currentRangeMin.toString())
                        VariableRowItem(key = localizedText("Current Range Max"), value = state.currentRangeMax.toString())
                        VariableRowItem(key = localizedText("Target Number"), value = state.targetNumber.toString())
                        VariableRowItem(key = localizedText("Total Attempts"), value = state.attempts.toString())
                        VariableRowItem(key = localizedText("Correct Guesses (State)"), value = state.correctGuesses.toString())
                        VariableRowItem(key = localizedText("Current Streak"), value = state.streak.toString())
                        VariableRowItem(key = localizedText("Best Streak"), value = state.bestStreak.toString())
                        VariableRowItem(key = localizedText("Buy Multiplier"), value = state.buyMultiplier)
                        VariableRowItem(key = localizedText("Auto-Clicker Active"), value = state.autoClickerActive.toString())
                        VariableRowItem(key = localizedText("Auto-Clicker Speed"), value = "${state.autoClickerSpeed}")
                        VariableRowItem(key = localizedText("Tutorial Completed"), value = state.tutorialCompleted.toString())
                    }

                    // Statistics & Counts
                    VariableGroupCard(title = localizedText("Statistics & Counters"), icon = Icons.Default.Analytics) {
                        VariableRowItem(key = localizedText("Total Guesses (Stats)"), value = state.statistics.totalGuesses.toString())
                        VariableRowItem(key = localizedText("Correct Guesses (Stats)"), value = state.statistics.correctGuesses.toString())
                        VariableRowItem(key = localizedText("Failed Guesses"), value = state.statistics.failedGuesses.toString())
                        VariableRowItem(key = localizedText("Prestiges Count"), value = state.statistics.prestigesCount.toString())
                        VariableRowItem(key = localizedText("Ultras Count"), value = state.statistics.ultrasCount.toString())
                        VariableRowItem(key = localizedText("Nebula Earned"), value = state.statistics.nebulaEarned.toString())
                        VariableRowItem(key = localizedText("Playtime Seconds"), value = "${state.statistics.playtimeSeconds}s")
                        VariableRowItem(key = localizedText("Arcade Played"), value = "${state.statistics.arcadePlayed}")
                        VariableRowItem(key = localizedText("Arcade Best Scores"), value = state.statistics.arcadeBestScores.toString())
                    }

                    // Settings & Preferences
                    VariableGroupCard(title = localizedText("Settings & Preferences"), icon = Icons.Default.Settings) {
                        VariableRowItem(key = localizedText("Music Enabled"), value = state.settings.musicEnabled.toString())
                        VariableRowItem(key = localizedText("Sound Enabled"), value = state.settings.soundEnabled.toString())
                        VariableRowItem(key = localizedText("Vibration Enabled"), value = state.settings.vibrationEnabled.toString())
                        VariableRowItem(key = localizedText("Notifications Enabled"), value = state.settings.notificationsEnabled.toString())
                        VariableRowItem(key = localizedText("Notification Interval"), value = "${state.settings.notificationIntervalHours}h")
                        VariableRowItem(key = localizedText("Theme Mode"), value = state.settings.themeMode)
                        VariableRowItem(key = localizedText("Number Notation"), value = state.settings.numberNotation)
                        VariableRowItem(key = localizedText("Reduced Motion"), value = state.settings.reducedMotion.toString())
                        VariableRowItem(key = localizedText("Volume Level"), value = "${state.settings.volume}")
                        VariableRowItem(key = localizedText("Reduce Flashes"), value = state.settings.reduceFlashes.toString())
                        VariableRowItem(key = localizedText("Save Logs to Storage"), value = state.settings.saveLogsToStorage.toString())
                        VariableRowItem(key = localizedText("Background Music Path"), value = state.settings.backgroundMusicPath ?: localizedText("None"))
                    }

                    // Collections & Maps
                    VariableGroupCard(title = localizedText("Collections & Upgrades"), icon = Icons.Default.Category) {
                        VariableRowItem(key = localizedText("Upgrade Levels Count"), value = "${state.upgradeLevels.size} items\n${state.upgradeLevels.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }}")
                        VariableRowItem(key = localizedText("Prestige Upgrades Count"), value = "${state.prestigeUpgradeLevels.size} items\n${state.prestigeUpgradeLevels.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }}")
                        VariableRowItem(key = localizedText("Ultra Upgrades Count"), value = "${state.ultraUpgradeLevels.size} items\n${state.ultraUpgradeLevels.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }}")
                        VariableRowItem(key = localizedText("Shop Purchases"), value = "${state.shopPurchases.size} items\n${state.shopPurchases}")
                        VariableRowItem(key = localizedText("Prestige Purchases"), value = "${state.prestigeShopPurchases.size} items\n${state.prestigeShopPurchases}")
                        VariableRowItem(key = localizedText("Ultra Purchases"), value = "${state.ultraShopPurchases.size} items\n${state.ultraShopPurchases}")
                        VariableRowItem(key = localizedText("Achievements Unlocked"), value = "${state.achievements.size} items\n${state.achievements}")
                        VariableRowItem(key = localizedText("Completed Challenges"), value = "${state.completedChallenges.size} items\n${state.completedChallenges}")
                    }

                    // Meta Timestamps & Anti-Cheat
                    VariableGroupCard(title = localizedText("Meta Timestamps & Anti-Cheat"), icon = Icons.Default.Security) {
                        VariableRowItem(key = localizedText("Last Save Timestamp"), value = state.lastSaveTimestamp.toString())
                        VariableRowItem(key = localizedText("Last Saved Version"), value = state.lastSavedVersion)
                        VariableRowItem(key = localizedText("Time Travel Penalty Until"), value = state.timeTravelPenaltyUntil.toString())
                        VariableRowItem(key = localizedText("Prestige Count (Root)"), value = state.prestigeCount.toString())
                        VariableRowItem(key = localizedText("Ultra Count (Root)"), value = state.ultraCount.toString())
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
                text = if (active) localizedText("RUNNING") else localizedText("STOPPED"),
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
