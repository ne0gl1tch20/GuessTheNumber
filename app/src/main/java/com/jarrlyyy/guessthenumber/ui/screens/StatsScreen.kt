package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(state: GameState, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val locale = remember(state.settings.locale) { com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository(context, state.settings.locale).localeManager }
    val stats = state.statistics
    val totalGuesses = stats.totalGuesses
    val correctGuesses = stats.correctGuesses
    val failedGuesses = stats.failedGuesses
    val accuracy = if (totalGuesses > 0) (correctGuesses.toFloat() / totalGuesses.toFloat()) * 100f else 0f
    val hours = stats.playtimeSeconds / 3600
    val minutes = (stats.playtimeSeconds % 3600) / 60
    val seconds = stats.playtimeSeconds % 60
    val playtimeFormatted = if (hours > 0) {
        "$hours hr $minutes min $seconds sec"
    } else if (minutes > 0) {
        "$minutes min $seconds sec"
    } else {
        "$seconds sec"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText("Advanced Statistics"), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = locale.getString("back", "Back"))
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
            // Hero Accuracy Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(localizedText("Guessing Precision"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = locale.formatPercent(accuracy.toDouble() / 100.0),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        LinearProgressIndicator(
                            progress = { if (totalGuesses > 0) correctGuesses.toFloat() / totalGuesses.toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(locale.getString("stats_correct_guesses", "Correct: %s", locale.formatNumber(correctGuesses)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(locale.getString("stats_failed_guesses", "Failed: %s", locale.formatNumber(failedGuesses)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(locale.getString("stats_total_guesses", "Total: %s", locale.formatNumber(totalGuesses)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // Core Metrics Section Header
            item {
                Text(text = localizedText("Performance & Economy"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        modifier = Modifier.weight(1f),
                        title = locale.getString("inspector_money_earned", "Money Earned"),
                        value = stats.moneyEarned.format(),
                        icon = Icons.Default.TrendingUp,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    StatMetricCard(
                        modifier = Modifier.weight(1f),
                        title = locale.getString("inspector_money_spent", "Money Spent"),
                        value = stats.moneySpent.format(),
                        icon = Icons.Default.ShoppingCart,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        modifier = Modifier.weight(1f),
                        title = locale.getString("inspector_current_streak", "Current Streak"),
                        value = "${state.streak}",
                        icon = Icons.Default.LocalFireDepartment,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    StatMetricCard(
                        modifier = Modifier.weight(1f),
                        title = locale.getString("inspector_best_streak", "Best Streak"),
                        value = "${state.bestStreak}",
                        icon = Icons.Default.EmojiEvents,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }

            // Resets & Progression Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = localizedText("Resets & Meta Progression"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatDetailRow(
                        title = locale.getString("stats_prestige_resets", "Prestige Resets"),
                        value = "${stats.prestigesCount}",
                        icon = Icons.Default.Star,
                        description = "Times reset for Prestige tokens"
                    )
                    StatDetailRow(
                        title = locale.getString("stats_ultra_resets", "Ultra Resets"),
                        value = "${stats.ultrasCount}",
                        icon = Icons.Default.AutoAwesome,
                        description = "Times reset for Ultra tokens"
                    )
                    StatDetailRow(
                        title = locale.getString("inspector_nebula_earned", "Nebula Earned"),
                        value = "${stats.nebulaEarned}",
                        icon = Icons.Default.BrightnessHigh,
                        description = "Total Nebula crystals gathered"
                    )
                }
            }

            // Gameplay & Time Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = locale.getString("stats_session_arcade", "Session & Arcade"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatDetailRow(
                        title = locale.getString("stats_total_playtime", "Total Playtime"),
                        value = playtimeFormatted,
                        icon = Icons.Default.Schedule,
                        description = "Active game session duration"
                    )
                    StatDetailRow(
                        title = locale.getString("inspector_arcade_played", "Arcade Minigames Played"),
                        value = "${stats.arcadePlayed}",
                        icon = Icons.Default.SportsEsports,
                        description = "Total arcade rounds completed"
                    )
                    StatDetailRow(
                        title = locale.getString("stats_current_number_range", "Current Number Range"),
                        value = "${state.currentRangeMin} - ${state.currentRangeMax}",
                        icon = Icons.Default.SwapHoriz,
                        description = "Active guessing bounds"
                    )
                }
            }
        }
    }
}

@Composable
fun StatMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatDetailRow(
    title: String,
    value: String,
    icon: ImageVector,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
