package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    state: GameState,
    onMakeGuess: (Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var guessInput by remember { mutableStateOf("") }
    var lastFeedback by remember { mutableStateOf("Guess a number between ${state.currentRangeMin} and ${state.currentRangeMax}") }

    val feedbackRoot = remember {
        com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository(context).loadFeedbackMessages()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "moe_pulse")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guess The Number", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Material 3 Expressive Hero Card with Organic Pulsing
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scalePulse),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = "Money", tint = MoneyGold, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                AnimatedContent(targetState = state.money.format(), label = "MoneyAnimation") { moneyStr ->
                                    Text(
                                        text = "Money: $moneyStr",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                            val autoClickerRate = if (state.autoClickerActive) state.autoClickerSpeed * 500.0 else 0.0
                            val upgradeMult = 1.0 + ((state.upgradeLevels["reward_multiplier"] ?: 0) * 0.25)
                            val prestigeMult = 1.0 + (state.prestige.value.toDouble() * 0.5)
                            val ultraMult = 1.0 + (state.ultra.value.toDouble() * 2.0)
                            val totalIncomePerSec = (autoClickerRate * upgradeMult * prestigeMult * ultraMult).toLong()

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    text = "+${totalIncomePerSec}/s",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Uncramped stats row with distinct distinct surface containers for Prestige, Ultra, and Nebula
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Prestige",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.prestige.format(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Ultra",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.ultra.format(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Nebula",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.nebula.format(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Google-style M3 Expressive Action Card Container
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Range: ${state.currentRangeMin} - ${state.currentRangeMax}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Streak: ${state.streak} (Best: ${state.bestStreak})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        AnimatedContent(targetState = lastFeedback, label = "FeedbackAnimation") { feedback ->
                            Text(
                                text = feedback,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = guessInput,
                            onValueChange = { guessInput = it },
                            label = { Text("Enter your guess") },
                            shape = MaterialTheme.shapes.medium,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            keyboardActions = KeyboardActions(onDone = {
                                val g = guessInput.toLongOrNull()
                                if (g != null) {
                                    onMakeGuess(g)
                                    lastFeedback = if (g < state.targetNumber) {
                                        val lowMsg = feedbackRoot.tooLowMessages.randomOrNull() ?: "📈 Too Low!"
                                        val tipMsg = feedbackRoot.guessTipsAndClues.randomOrNull() ?: ""
                                        "$lowMsg\n\n$tipMsg"
                                    } else if (g > state.targetNumber) {
                                        feedbackRoot.tooHighMessages.randomOrNull() ?: "📉 Too High!"
                                    } else {
                                        "✨ Correct! Earned reward!"
                                    }
                                    guessInput = ""
                                }
                            }),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val g = guessInput.toLongOrNull()
                                if (g != null) {
                                    onMakeGuess(g)
                                    lastFeedback = if (g < state.targetNumber) {
                                        val lowMsg = feedbackRoot.tooLowMessages.randomOrNull() ?: "📈 Too Low!"
                                        val tipMsg = feedbackRoot.guessTipsAndClues.randomOrNull() ?: ""
                                        "$lowMsg\n\n$tipMsg"
                                    } else if (g > state.targetNumber) {
                                        feedbackRoot.tooHighMessages.randomOrNull() ?: "📉 Too High!"
                                    } else {
                                        "✨ Correct! Earned reward!"
                                    }
                                    guessInput = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GUESS NUMBER", style = MaterialTheme.typography.titleMedium)
                        }

                        if (BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "You are using the debug build. It is not for normal use.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
