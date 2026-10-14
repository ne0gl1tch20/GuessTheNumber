package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText
import com.jarrlyyy.guessthenumber.data.repository.WorldConfigRepository

import androidx.compose.animation.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.jarrlyyy.guessthenumber.ui.theme.expressivePressScale
import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold
import com.jarrlyyy.guessthenumber.ui.components.ExpressiveMilestoneBanner

private enum class GuessFeedbackTone { Neutral, TooLow, TooHigh, Correct }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    state: GameState,
    onMakeGuess: (Long) -> Unit,
    onClaimDailyQuest: (String) -> Unit = {},
    onOpenLootChest: (String) -> Unit = {},
    onWorldAction: (String, String) -> Unit = { _, _ -> },
    onNavigateProgression: (String) -> Unit = {},
    incomePerSecond: com.jarrlyyy.guessthenumber.domain.model.BigNumber = com.jarrlyyy.guessthenumber.domain.model.BigNumber.ZERO
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val worldDefinitions = remember { WorldConfigRepository(context).loadWorlds() }
    var guessInput by remember { mutableStateOf("") }
    var showMathStrategy by remember { mutableStateOf(false) }
    val configRepository = remember(state.settings.locale) { com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository(context, state.settings.locale) }
    val locale = configRepository.localeManager
    var lastFeedback by remember { mutableStateOf(locale.getString("guess_prompt", "Guess a number between ${state.currentRangeMin} and ${state.currentRangeMax}")) }
    var feedbackTone by remember { mutableStateOf(GuessFeedbackTone.Neutral) }
    var showCorrectCelebration by remember { mutableStateOf(false) }
    var observedCorrectGuesses by remember { mutableStateOf(state.correctGuesses) }
    val completedEarlyGoals = buildSet {
        if (state.correctGuesses >= 10) add("correct_10")
        if (state.bestStreak >= 5) add("streak_5")
        if (state.money >= com.jarrlyyy.guessthenumber.domain.model.BigNumber(1_000_000)) add("millionaire")
    }
    var observedEarlyGoalIds by remember { mutableStateOf(completedEarlyGoals) }
    val earlyGoalCelebrationQueue = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var showEarlyGoalCelebration by remember { mutableStateOf(false) }
    var earlyGoalCelebrationText by remember { mutableStateOf("") }
    val guessInteractionSource = remember { MutableInteractionSource() }
    val isGuessPressed by guessInteractionSource.collectIsPressedAsState()
    val guessCornerRadius by animateDpAsState(
        targetValue = if (isGuessPressed && !state.settings.reducedMotion) 20.dp else 28.dp,
        animationSpec = if (state.settings.reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "guessButtonShape"
    )

    LaunchedEffect(completedEarlyGoals) {
        val newlyCompleted = completedEarlyGoals - observedEarlyGoalIds
        observedEarlyGoalIds = completedEarlyGoals
        val goalNameKeys = listOf(
            "correct_10" to "achievement_correct_10_name",
            "streak_5" to "achievement_streak_5_name",
            "millionaire" to "achievement_millionaire_name"
        )
        goalNameKeys.filter { it.first in newlyCompleted }.forEach { (_, nameKey) ->
            val goalName = locale.getString(nameKey, "Progress milestone")
            val message = locale.getString("achievement_unlocked_banner", "Unlocked: %s", goalName)
            if (message !in earlyGoalCelebrationQueue && message != earlyGoalCelebrationText) {
                earlyGoalCelebrationQueue.add(message)
            }
        }
    }

    LaunchedEffect(earlyGoalCelebrationQueue.firstOrNull(), state.settings.reducedMotion) {
        val nextCelebration = earlyGoalCelebrationQueue.firstOrNull() ?: return@LaunchedEffect
        earlyGoalCelebrationText = nextCelebration
        showEarlyGoalCelebration = true
        delay(if (state.settings.reducedMotion) 700L else 2200L)
        showEarlyGoalCelebration = false
        delay(if (state.settings.reducedMotion) 100L else 220L)
        if (earlyGoalCelebrationQueue.firstOrNull() == nextCelebration) {
            earlyGoalCelebrationQueue.removeAt(0)
        }
    }

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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.activeBossBattleWorldId != null) {
                item(key = "active_boss_battle") {
                    val bossWorld = worldDefinitions.firstOrNull { it.id == state.activeBossBattleWorldId }
                        ?: worldDefinitions.first()
                    val bossNameKey = bossWorld.bossKey
                    val bossFallback = bossWorld.bossFallback
                    val bossId = bossWorld.bossId
                    val bossHp = bossWorld.hp
                    val bossDamage = (state.bossBattleProgress[bossId] ?: 0).coerceAtMost(bossHp)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SportsMma, contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(locale.getString("boss_battle_active_title", "Boss Battle Active"), style = MaterialTheme.typography.titleMedium)
                                    Text(locale.getString(bossNameKey, bossFallback), style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(locale.getString("boss_hp_progress", "%d / %d HP", bossHp - bossDamage, bossHp), style = MaterialTheme.typography.labelMedium)
                            }
                            LinearProgressIndicator(progress = { bossDamage.toFloat() / bossHp.toFloat() }, modifier = Modifier.fillMaxWidth())
                            Text(locale.getString("boss_guess_rule", "Correct guesses deal damage. Every third wrong guess restores one HP."), style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { onWorldAction(state.activeBossBattleWorldId!!, "boss") }, modifier = Modifier.fillMaxWidth()) {
                                Text(locale.getString("boss_battle_stop", "Leave battle"))
                            }
                        }
                    }
                }
            }
            item(key = "beyond_numbers_shortcut") {
                FilledTonalButton(
                    onClick = {
                        val hubIndex = 4 +
                            (if (state.activeBossBattleWorldId != null) 1 else 0) +
                            (if (state.lastRandomEventId != null) 1 else 0)
                        scrollScope.launch {
                            listState.animateScrollToItem(hubIndex)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(locale.getString("beyond_numbers_title", "Beyond the Numbers"))
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
                }
            }
            item(key = "play_hero_stats") {
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
                                Icon(imageVector = Icons.Default.Star, contentDescription = locale.getString("inspector_money", "Money"), tint = MoneyGold, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                AnimatedContent(
                                    targetState = state.money.format(),
                                    transitionSpec = {
                                        (slideInVertically { it / 3 } + fadeIn()) togetherWith
                                            (slideOutVertically { -it / 3 } + fadeOut())
                                    },
                                    label = "MoneyAnimation"
                                ) { moneyStr ->
                                    Text(text = locale.getString("money_balance_format", "Money: %s", moneyStr),
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
                                Text(text = locale.getString("income_rate_format", "+%s/s", formattedIncome),
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

            item(key = "correct_guess_celebration") {
                val streakMilestoneNameKey = when (state.streak) {
                    5 -> "achievement_streak_5_name"
                    10 -> "achievement_streak_10_name"
                    25 -> "achievement_streak_25_name"
                    50 -> "achievement_streak_50_name"
                    else -> null
                }
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
                                    text = if (streakMilestoneNameKey != null) {
                                        locale.getString("achievement_unlocked_title", "Achievement unlocked!")
                                    } else {
                                        locale.getString("play_correct_celebration_title", "Nice guess!")
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = if (streakMilestoneNameKey != null) {
                                        locale.getString(streakMilestoneNameKey, "Streak milestone")
                                    } else {
                                        locale.getString("play_correct_celebration_desc", "Secret number found. Your streak keeps growing.")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            state.lastRandomEventId?.let { eventId ->
                item(key = "random_event_${state.statistics.randomEventsTriggered}_${eventId}") {
                    val eventName = locale.getString(
                        "random_event_" + eventId + "_name",
                        eventId
                    )
                    val eventDescription = locale.getString(
                        "random_event_" + eventId + "_desc",
                        ""
                    )
                    val eventIcon = when (eventId) {
                        "cash_burst" -> Icons.Default.CardGiftcard
                        "nebula_rain" -> Icons.Default.AutoAwesome
                        "jackpot" -> Icons.Default.EmojiEvents
                        "streak_surge" -> Icons.Default.LocalFireDepartment
                        "range_shuffle" -> Icons.Default.Explore
                        "tax_refund" -> Icons.Default.CardGiftcard
                        "cosmic_gift" -> Icons.Default.Diamond
                        else -> Icons.Default.AutoAwesome
                    }
                    val eventContainerColor = when (eventId) {
                        "cash_burst", "streak_surge" -> MaterialTheme.colorScheme.primaryContainer
                        "nebula_rain", "tax_refund" -> MaterialTheme.colorScheme.secondaryContainer
                        "jackpot", "cosmic_gift" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val eventContentColor = when (eventId) {
                        "cash_burst", "streak_surge" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "nebula_rain", "tax_refund" -> MaterialTheme.colorScheme.onSecondaryContainer
                        "jackpot", "cosmic_gift" -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    AnimatedVisibility(
                        visible = true,
                        enter = if (state.settings.reducedMotion) {
                            fadeIn(animationSpec = tween(100))
                        } else {
                            fadeIn(animationSpec = tween(220)) +
                                scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = eventContainerColor,
                            tonalElevation = 5.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = eventContentColor.copy(alpha = 0.10f)
                                ) {
                                    Icon(
                                        imageVector = eventIcon,
                                        contentDescription = null,
                                        tint = eventContentColor,
                                        modifier = Modifier.padding(10.dp).size(28.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = locale.getString("random_event_active", "⚡ Random Event: %s", eventName),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = eventContentColor
                                    )
                                    Text(
                                        text = eventDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = eventContentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "math_strategy_lab") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showMathStrategy = !showMathStrategy },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                locale.getString("math_strategy_title", "🧠 Math Strategy"),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                if (showMathStrategy) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(
                            visible = showMathStrategy,
                            enter = if (state.settings.reducedMotion) fadeIn() else expandVertically() + fadeIn(),
                            exit = if (state.settings.reducedMotion) fadeOut() else shrinkVertically() + fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    locale.getString(
                                        "math_strategy_description",
                                        "The midpoint sits halfway between the lowest and highest numbers. Try it, then use the higher/lower clue to guide your next guess."
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                val strategyMidpoint = state.currentRangeMin +
                                    (state.currentRangeMax - state.currentRangeMin) / 2
                                FilledTonalButton(
                                    onClick = { guessInput = strategyMidpoint.toString() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        locale.getString(
                                            "math_strategy_try_midpoint",
                                            "Try midpoint: %s",
                                            locale.formatNumber(strategyMidpoint)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(key = "number_guessing_panel") {
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
                                if (state.settings.reducedMotion) {
                                    fadeIn(animationSpec = tween(100)) togetherWith fadeOut(animationSpec = tween(100))
                                } else {
                                    (fadeIn(animationSpec = tween(180)) + slideInVertically { it / 5 }) togetherWith
                                        (fadeOut(animationSpec = tween(120)) + slideOutVertically { -it / 5 })
                                }
                            },
                            label = "FeedbackAnimation"
                        ) { feedback ->
                            val feedbackContainerColor = when (feedbackTone) {
                                GuessFeedbackTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
                                GuessFeedbackTone.TooLow -> MaterialTheme.colorScheme.secondaryContainer
                                GuessFeedbackTone.TooHigh -> MaterialTheme.colorScheme.primaryContainer
                                GuessFeedbackTone.Correct -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                            val feedbackContentColor = when (feedbackTone) {
                                GuessFeedbackTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                                GuessFeedbackTone.TooLow -> MaterialTheme.colorScheme.onSecondaryContainer
                                GuessFeedbackTone.TooHigh -> MaterialTheme.colorScheme.onPrimaryContainer
                                GuessFeedbackTone.Correct -> MaterialTheme.colorScheme.onTertiaryContainer
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth().animateContentSize(),
                                shape = MaterialTheme.shapes.large,
                                color = feedbackContainerColor,
                                tonalElevation = if (state.settings.reducedMotion) 0.dp else 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = when (feedbackTone) {
                                            GuessFeedbackTone.Neutral -> Icons.Default.TipsAndUpdates
                                            GuessFeedbackTone.TooLow -> Icons.Default.ArrowUpward
                                            GuessFeedbackTone.TooHigh -> Icons.Default.ArrowDownward
                                            GuessFeedbackTone.Correct -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = feedbackContentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = feedback,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = feedbackContentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
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
                                    feedbackTone = when {
                                        "mut_blind" in state.activeMutators -> GuessFeedbackTone.Neutral
                                        g < state.targetNumber -> GuessFeedbackTone.TooLow
                                        g > state.targetNumber -> GuessFeedbackTone.TooHigh
                                        else -> GuessFeedbackTone.Correct
                                    }
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
                                        locale.getString("play_correct_guess", "✨ Correct! You guessed the secret number!")
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
                                    feedbackTone = when {
                                        "mut_blind" in state.activeMutators -> GuessFeedbackTone.Neutral
                                        g < state.targetNumber -> GuessFeedbackTone.TooLow
                                        g > state.targetNumber -> GuessFeedbackTone.TooHigh
                                        else -> GuessFeedbackTone.Correct
                                    }
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
                            shape = RoundedCornerShape(guessCornerRadius),
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
                                        contentDescription = locale.getString("accessibility_warning", "Warning"),
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
            item(key = "early_goal_celebration") {
                ExpressiveMilestoneBanner(
                    visible = showEarlyGoalCelebration,
                    title = locale.getString("achievement_unlocked_title", "Milestone reached!"),
                    description = earlyGoalCelebrationText,
                    reducedMotion = state.settings.reducedMotion
                )
            }
            item(key = "early_goal_board") {
                EarlyGoalBoard(
                    state = state,
                    locale = locale
                )
            }
            item(key = "beyond_numbers_hub") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(locale.getString("beyond_numbers_title", "Beyond the Numbers"), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    locale.getString(
                                        "world_progress_summary",
                                        "Worlds unlocked: %d / 4 • Bosses defeated: %d / 4",
                                        state.unlockedWorldIds.size.coerceAtMost(4),
                                        state.defeatedBossIds.size.coerceAtMost(4)
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { onNavigateProgression("world_map") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(locale.getString("world_map_title", "World Map"), maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = { onNavigateProgression("relics") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(locale.getString("relics_title", "Relics"), maxLines = 1)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onNavigateProgression("home_base") },
                                modifier = Modifier.weight(1f)
                            ) { Text(locale.getString("home_base_title", "Home Base"), maxLines = 1) }
                            OutlinedButton(
                                onClick = { onNavigateProgression("codex") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(locale.getString("codex_title", "Codex"), maxLines = 1)
                            }
                        }
                    }
                }
            }
            item(key = "daily_quest_board") {
                DailyQuestBoard(
                    state = state,
                    locale = locale,
                    onClaim = onClaimDailyQuest
                )
            }
            item(key = "loot_chest_board") {
                LootChestBoard(
                    state = state,
                    locale = locale,
                    onOpen = onOpenLootChest
                )
            }
            item(key = "world_progression_board") {
                WorldProgressionBoard(
                    state = state,
                    locale = locale,
                    onAction = onWorldAction
                )
            }
        }
    }
}


@Composable
private fun EarlyGoalBoard(
    state: GameState,
    locale: com.jarrlyyy.guessthenumber.data.repository.LocaleManager
) {
    data class Goal(
        val titleKey: String,
        val descriptionKey: String,
        val progressLabel: String,
        val progressFraction: Float,
        val complete: Boolean,
        val reward: Long
    )

    val correctProgress = state.correctGuesses.coerceIn(0, 10).toInt()
    val streakProgress = state.bestStreak.coerceIn(0, 5)
    val moneyGoal = com.jarrlyyy.guessthenumber.domain.model.BigNumber(1_000_000)
    val moneyProgress = (state.money.value.toDouble() / 1_000_000.0).coerceIn(0.0, 1.0).toFloat()
    val goals = listOf(
        Goal(
            titleKey = "achievement_correct_10_name",
            descriptionKey = "achievement_correct_10_desc",
            progressLabel = locale.getString("achievement_progress_format", correctProgress, 10),
            progressFraction = correctProgress / 10f,
            complete = state.correctGuesses >= 10,
            reward = 10L
        ),
        Goal(
            titleKey = "achievement_streak_5_name",
            descriptionKey = "achievement_streak_5_desc",
            progressLabel = locale.getString("achievement_progress_format", streakProgress, 5),
            progressFraction = streakProgress / 5f,
            complete = state.bestStreak >= 5,
            reward = 25L
        ),
        Goal(
            titleKey = "achievement_millionaire_name",
            descriptionKey = "achievement_millionaire_desc",
            progressLabel = state.money.format(state.settings.numberNotation),
            progressFraction = moneyProgress,
            complete = state.money >= moneyGoal,
            reward = 50L
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(locale.getString("achievement_progress_title"), style = MaterialTheme.typography.titleLarge)
                    Text(
                        locale.getString("achievement_progress_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            goals.forEach { goal ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(locale.getString(goal.titleKey), style = MaterialTheme.typography.titleSmall)
                            Text(
                                locale.getString(goal.descriptionKey),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                locale.getString(if (goal.complete) "unlocked" else "locked"),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (goal.complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                locale.getString("reward_format", goal.reward.toString()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { goal.progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (goal.complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(goal.progressLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}


@Composable
private fun DailyQuestBoard(
    state: GameState,
    locale: com.jarrlyyy.guessthenumber.data.repository.LocaleManager,
    onClaim: (String) -> Unit
) {
    data class Quest(
        val id: String,
        val titleKey: String,
        val descriptionKey: String,
        val progress: Int,
        val target: Int,
        val rewardKey: String
    )
    val quests = listOf(
        Quest("daily_guesses_10", "daily_quest_guesses_name", "daily_quest_guesses_desc", state.dailyQuestGuesses, 10, "daily_quest_reward_money"),
        Quest("daily_correct_5", "daily_quest_correct_name", "daily_quest_correct_desc", state.dailyQuestCorrectGuesses, 5, "daily_quest_reward_nebula_25"),
        Quest("daily_streak_5", "daily_quest_streak_name", "daily_quest_streak_desc", state.dailyQuestBestStreak, 5, "daily_quest_reward_nebula_50"),
        Quest("daily_upgrades_3", "daily_quest_upgrades_name", "daily_quest_upgrades_desc", state.dailyQuestUpgradePurchases, 3, "daily_quest_reward_nebula_35")
    )
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(locale.getString("daily_quests_title"), style = MaterialTheme.typography.titleLarge)
                    Text(locale.getString("daily_quests_subtitle"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(locale.getString("daily_quests_reset"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            quests.forEach { quest ->
                val claimed = quest.id in state.claimedDailyQuests
                val progress = quest.progress.coerceIn(0, quest.target)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(locale.getString(quest.titleKey), style = MaterialTheme.typography.titleSmall)
                            Text(locale.getString(quest.descriptionKey), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(locale.getString("daily_quest_progress", progress, quest.target), style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(
                        progress = { progress.toFloat() / quest.target.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (claimed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(locale.getString(quest.rewardKey), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                        Button(
                            onClick = { onClaim(quest.id) },
                            enabled = progress >= quest.target && !claimed,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(locale.getString(if (claimed) "daily_quest_claimed" else if (progress >= quest.target) "daily_quest_claim" else "daily_quest_in_progress"))
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun LootChestBoard(
    state: GameState,
    locale: com.jarrlyyy.guessthenumber.data.repository.LocaleManager,
    onOpen: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.padding(12.dp).size(26.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(locale.getString("loot_title"), style = MaterialTheme.typography.titleLarge)
                    Text(locale.getString("loot_subtitle"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(state.lootDropsFound.toString(), style = MaterialTheme.typography.titleMedium)
                    Text(locale.getString("loot_found"), style = MaterialTheme.typography.labelSmall)
                    Text(locale.getString("loot_opened_count", state.lootChestsOpened), style = MaterialTheme.typography.labelSmall)
                }
            }
            if (state.lastLootDropTier != null && state.lootEventId > 0) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            locale.getString("loot_drop_message", "A %s chest dropped!", locale.getString("loot_tier_" + state.lastLootDropTier)),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Text(locale.getString("loot_drop_rates"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            val tiers = listOf(
                Triple("common", locale.getString("loot_tier_common"), state.lootCommonChests),
                Triple("rare", locale.getString("loot_tier_rare"), state.lootRareChests),
                Triple("epic", locale.getString("loot_tier_epic"), state.lootEpicChests)
            )
            tiers.forEach { (id, label, count) ->
                val chestInteraction = remember(id) { MutableInteractionSource() }
                val color = when (id) {
                    "epic" -> MaterialTheme.colorScheme.tertiary
                    "rare" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = when (id) {
                            "epic" -> Icons.Default.Diamond
                            "rare" -> Icons.Default.Inventory2
                            else -> Icons.Default.CardGiftcard
                        },
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        AnimatedContent(targetState = count, label = "lootChestCount") { animatedCount ->
                            Text(locale.getString("loot_chest_count", animatedCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text(locale.getString("loot_rewards_" + id), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Button(
                        onClick = { onOpen(id) },
                        enabled = count > 0,
                        interactionSource = chestInteraction,
                        modifier = Modifier.expressivePressScale(interactionSource = chestInteraction, reducedMotion = state.settings.reducedMotion)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(locale.getString("loot_open"))
                    }
                }
            }
        }
    }
}


@Composable
private fun WorldProgressionBoard(
    state: GameState,
    locale: com.jarrlyyy.guessthenumber.data.repository.LocaleManager,
    onAction: (String, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val worlds = remember { WorldConfigRepository(context).loadWorlds() }
    val totalUpgradeLevels = state.upgradeLevels.values.sum()
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(locale.getString("worlds_title"), style = MaterialTheme.typography.titleLarge)
                    Text(locale.getString("worlds_subtitle"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(locale.getString("worlds_boss_count", state.worldBossVictories), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            worlds.forEachIndexed { index, world ->
                val unlocked = world.id in state.unlockedWorldIds
                val defeated = world.bossId in state.defeatedBossIds && !state.endlessRiftActive
                val canUnlock = world.unlockBossId?.let { requiredBossId ->
                    requiredBossId in state.defeatedBossIds &&
                        state.correctGuesses >= world.unlockCorrectGuesses &&
                        totalUpgradeLevels >= world.unlockUpgradeCount &&
                        state.prestigeCount >= world.unlockPrestigeCount &&
                        state.ultraCount >= world.unlockUltraCount
                } ?: false
                val canFight = state.endlessRiftActive || (
                    state.correctGuesses >= world.fightCorrectGuesses &&
                        totalUpgradeLevels >= world.fightUpgradeCount &&
                        state.prestigeCount >= world.fightPrestigeCount &&
                        state.ultraCount >= world.fightUltraCount
                )
                val bossHp = world.hp + if (state.endlessRiftActive) ((state.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0
                val bossDamage = (state.bossBattleProgress[world.bossId] ?: 0).coerceIn(0, bossHp)
                val bossHpRemaining = bossHp - bossDamage
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (unlocked) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (world.id == state.activeWorldId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Forest
                                    1 -> Icons.Default.Diamond
                                    2 -> Icons.Default.LocalFireDepartment
                                    else -> Icons.Default.AutoAwesome
                                },
                                contentDescription = null,
                                tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(locale.getString(world.nameKey), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    locale.getString(if (unlocked) "world_status_unlocked" else "world_status_locked"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (defeated) Icon(Icons.Default.CheckCircle, contentDescription = locale.getString("boss_defeated"), tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Text(locale.getString(world.worldRequirementKey), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!unlocked && index > 0) {
                            Button(onClick = { onAction(world.id, "unlock") }, enabled = canUnlock, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(locale.getString(if (canUnlock) "world_unlock" else "world_requirements_pending"))
                            }
                        }
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text(locale.getString(world.bossKey), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        }
                        Text(locale.getString(world.bossRequirementKey), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (unlocked && !defeated) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(locale.getString("boss_hp_label"), style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.weight(1f))
                                Text(locale.getString("boss_hp_progress", bossHpRemaining, bossHp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                            LinearProgressIndicator(
                                progress = { bossHpRemaining.toFloat() / bossHp.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.error,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        Text(locale.getString(world.bossRewardKey), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        Button(
                            onClick = { onAction(world.id, "boss") },
                            enabled = unlocked && canFight && !defeated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (defeated) Icons.Default.EmojiEvents else Icons.Default.SportsMma, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(locale.getString(if (defeated) "boss_defeated" else if (!unlocked) "world_locked_boss" else if (canFight) "boss_attack" else "boss_requirements_pending"))
                        }
                    }
                }
            }
        }
    }
}