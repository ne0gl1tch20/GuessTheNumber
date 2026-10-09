package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import androidx.compose.animation.*
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.delay
import com.jarrlyyy.guessthenumber.ui.theme.expressivePressScale
import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    state: GameState,
    onMakeGuess: (Long) -> Unit,
    incomePerSecond: com.jarrlyyy.guessthenumber.domain.model.BigNumber = com.jarrlyyy.guessthenumber.domain.model.BigNumber.ZERO
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var guessInput by remember { mutableStateOf("") }
    val configRepository = remember(state.settings.locale) { com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository(context, state.settings.locale) }
    val locale = configRepository.localeManager
    var lastFeedback by remember { mutableStateOf(locale.getString("guess_prompt", "Guess a number between ${state.currentRangeMin} and ${state.currentRangeMax}")) }
    var showCorrectCelebration by remember { mutableStateOf(false) }
    var observedCorrectGuesses by remember { mutableStateOf(state.correctGuesses) }
    val guessInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(state.correctGuesses, state.settings.reducedMotion) {
        val foundNewNumber = state.correctGuesses > observedCorrectGuesses
        observedCorrectGuesses = state.correctGuesses
        if (foundNewNumber) {
            showCorrectCelebration = true
            delay(if (state.settings.reducedMotion) 700L else 1800L)
            showCorrectCelebration = false
        }
    }

    val feedbackRoot = remember {
        configRepository.loadFeedbackMessages()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "moe_pulse")
    val animatedScalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val scalePulse = if (state.settings.reducedMotion) 1f else animatedScalePulse

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locale.getString("play_title", "Guess The Number"), style = MaterialTheme.typography.titleMedium) },
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
                        .animateContentSize()
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
                                AnimatedContent(
                                    targetState = state.money.format(),
                                    transitionSpec = {
                                        (slideInVertically { it / 3 } + fadeIn()) togetherWith
                                            (slideOutVertically { -it / 3 } + fadeOut())
                                    },
                                    label = "MoneyAnimation"
                                ) { moneyStr ->
                                    Text(text = localizedText("Money: $moneyStr"),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                            // This is the actual money gained during the last one-second measurement window.
                            // It intentionally uses GameViewModel's observed state delta instead of a theoretical formula.
                            val formattedIncome = incomePerSecond.format()

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                                tonalElevation = 2.dp
                            ) {
                                Text(text = localizedText("+$formattedIncome/s"),
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
                                    Text(text = localizedText("Prestige"),
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
                                    Text(text = localizedText("Ultra"),
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
                                    Text(text = localizedText("Nebula"),
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
                AnimatedVisibility(
                    visible = showCorrectCelebration,
                    enter = if (state.settings.reducedMotion) fadeIn() else scaleIn() + fadeIn(),
                    exit = if (state.settings.reducedMotion) fadeOut() else scaleOut() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        tonalElevation = if (state.settings.reducedMotion) 0.dp else 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = locale.getString("play_correct_celebration_title", "Nice guess!"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = locale.getString("play_correct_celebration_desc", "Secret number found. Your streak keeps growing."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            state.lastRandomEventId?.let { eventId ->
                item {
                    val eventName = locale.getString(
                        "random_event_" + eventId + "_name",
                        eventId
                    )
                    val eventDescription = locale.getString(
                        "random_event_" + eventId + "_desc",
                        ""
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        tonalElevation = 5.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = locale.getString("random_event_active", "⚡ Random Event: %s", eventName),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = eventDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
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
                            text = locale.getString("play_range_format", "Range: %d - %d", state.currentRangeMin, state.currentRangeMax),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = locale.getString("play_streak_format", "Streak: %d (Best: %d)", state.streak, state.bestStreak),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.streak >= GameEngine.FRENZY_THRESHOLD) {
                            val frenzyMultiplier = GameEngine().frenzyMultiplier(state.streak).format()
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = locale.getString("frenzy_format", "🔥 Frenzy x%s", frenzyMultiplier),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        AnimatedContent(
                            targetState = lastFeedback,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(180)) + slideInVertically { it / 5 }) togetherWith
                                    (fadeOut(animationSpec = tween(120)) + slideOutVertically { -it / 5 })
                            },
                            label = "FeedbackAnimation"
                        ) { feedback ->
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
                            label = { Text(locale.getString("play_enter_guess", "Enter your guess")) },
                            shape = MaterialTheme.shapes.medium,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            keyboardActions = KeyboardActions(onDone = {
                                val g = guessInput.toLongOrNull()
                                if (g != null) {
                                    onMakeGuess(g)
                                    lastFeedback = if ("mut_blind" in state.activeMutators) {
                                        locale.getString("blind_feedback", "Feedback hidden by Blindfolded Oracle.")
                                    } else if (g < state.targetNumber) {
                                        val lowMsg = feedbackRoot.tooLowMessages.randomOrNull() ?: "📈 Too Low!"
                                        val randomTipTemplate = feedbackRoot.guessTipsAndClues.randomOrNull() ?: "💡 Binary search midpoint: %d"
                                        val midpoint = (state.currentRangeMin + state.currentRangeMax) / 2
                                        val diff = kotlin.math.abs(state.targetNumber - g)
                                        val parity = if (state.targetNumber % 2L == 0L) "even" else "odd"
                                        val mod10 = (state.targetNumber % 10L).toInt()
                                        val sqrtVal = kotlin.math.sqrt(state.targetNumber.toDouble()).toInt()
                                        val formattedTip = try {
                                            String.format(
                                                java.util.Locale.US,
                                                randomTipTemplate,
                                                midpoint,
                                                state.currentRangeMin,
                                                state.currentRangeMax,
                                                midpoint,
                                                diff.toInt(),
                                                if (g < midpoint) "lower" else "upper",
                                                parity,
                                                3,
                                                mod10,
                                                diff.toInt(),
                                                sqrtVal
                                            )
                                        } catch (e: Exception) {
                                            randomTipTemplate
                                        }
                                        "$lowMsg\n\n$formattedTip"
                                    } else if (g > state.targetNumber) {
                                        val highMsg = feedbackRoot.tooHighMessages.randomOrNull() ?: "📉 Too High!"
                                        val randomTipTemplate = feedbackRoot.guessTipsAndClues.randomOrNull() ?: "💡 Binary search midpoint: %d"
                                        val midpoint = (state.currentRangeMin + state.currentRangeMax) / 2
                                        val diff = kotlin.math.abs(state.targetNumber - g)
                                        val parity = if (state.targetNumber % 2L == 0L) "even" else "odd"
                                        val mod10 = (state.targetNumber % 10L).toInt()
                                        val sqrtVal = kotlin.math.sqrt(state.targetNumber.toDouble()).toInt()
                                        val formattedTip = try {
                                            String.format(
                                                java.util.Locale.US,
                                                randomTipTemplate,
                                                midpoint,
                                                state.currentRangeMin,
                                                state.currentRangeMax,
                                                midpoint,
                                                diff.toInt(),
                                                if (g < midpoint) "lower" else "upper",
                                                parity,
                                                3,
                                                mod10,
                                                diff.toInt(),
                                                sqrtVal
                                            )
                                        } catch (e: Exception) {
                                            randomTipTemplate
                                        }
                                        "$highMsg\n\n$formattedTip"
                                    } else {
                                        "✨ Correct! You guessed the secret number!"
                                    }
                                    guessInput = ""
                                }
                            }),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            interactionSource = guessInteractionSource,
                            onClick = {
                                val g = guessInput.toLongOrNull()
                                if (g != null) {
                                    onMakeGuess(g)
                                    lastFeedback = if (g < state.targetNumber) {
                                        val lowMsg = feedbackRoot.tooLowMessages.randomOrNull() ?: "📈 Too Low!"
                                        val randomTipTemplate = feedbackRoot.guessTipsAndClues.randomOrNull() ?: "💡 Binary search midpoint: %d"
                                        val midpoint = (state.currentRangeMin + state.currentRangeMax) / 2
                                        val diff = kotlin.math.abs(state.targetNumber - g)
                                        val parity = if (state.targetNumber % 2L == 0L) "even" else "odd"
                                        val mod10 = (state.targetNumber % 10L).toInt()
                                        val sqrtVal = kotlin.math.sqrt(state.targetNumber.toDouble()).toInt()
                                        val formattedTip = try {
                                            String.format(
                                                java.util.Locale.US,
                                                randomTipTemplate,
                                                midpoint,
                                                state.currentRangeMin,
                                                state.currentRangeMax,
                                                midpoint,
                                                diff.toInt(),
                                                if (g < midpoint) "lower" else "upper",
                                                parity,
                                                3,
                                                mod10,
                                                diff.toInt(),
                                                sqrtVal
                                            )
                                        } catch (e: Exception) {
                                            randomTipTemplate
                                        }
                                        "$lowMsg\n\n$formattedTip"
                                    } else if (g > state.targetNumber) {
                                        val highMsg = feedbackRoot.tooHighMessages.randomOrNull() ?: "📉 Too High!"
                                        val randomTipTemplate = feedbackRoot.guessTipsAndClues.randomOrNull() ?: "💡 Binary search midpoint: %d"
                                        val midpoint = (state.currentRangeMin + state.currentRangeMax) / 2
                                        val diff = kotlin.math.abs(state.targetNumber - g)
                                        val parity = if (state.targetNumber % 2L == 0L) "even" else "odd"
                                        val mod10 = (state.targetNumber % 10L).toInt()
                                        val sqrtVal = kotlin.math.sqrt(state.targetNumber.toDouble()).toInt()
                                        val formattedTip = try {
                                            String.format(
                                                java.util.Locale.US,
                                                randomTipTemplate,
                                                midpoint,
                                                state.currentRangeMin,
                                                state.currentRangeMax,
                                                midpoint,
                                                diff.toInt(),
                                                if (g < midpoint) "lower" else "upper",
                                                parity,
                                                3,
                                                mod10,
                                                diff.toInt(),
                                                sqrtVal
                                            )
                                        } catch (e: Exception) {
                                            randomTipTemplate
                                        }
                                        "$highMsg\n\n$formattedTip"
                                    } else {
                                        locale.getString("play_correct_guess", "✨ Correct! You guessed the secret number!")
                                    }
                                    guessInput = ""
                                }
                            },
                            modifier = Modifier
                                .expressivePressScale(
                                    interactionSource = guessInteractionSource,
                                    reducedMotion = state.settings.reducedMotion
                                )
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(locale.getString("play_guess_button", "GUESS NUMBER"), style = MaterialTheme.typography.titleMedium)
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
                                        text = locale.getString("play_debug_warning", "You are using the debug build. It is not for normal use."),
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
