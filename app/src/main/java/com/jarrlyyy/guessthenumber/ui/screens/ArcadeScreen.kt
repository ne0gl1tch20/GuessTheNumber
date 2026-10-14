package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import androidx.compose.animation.animateContentSize

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.jarrlyyy.guessthenumber.ui.theme.expressivePressScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository
import com.jarrlyyy.guessthenumber.data.repository.MinigameDef
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcadeScreen(
    onEarnReward: (Long, BigNumber) -> Unit,
    onBack: () -> Unit,
    reducedMotion: Boolean = false
) {
    val context = LocalContext.current
    val locale = LocalAppLocaleManager.current
    val minigames = remember { JsonConfigRepository(context).loadMinigames() }

    var activeMinigame by remember { mutableStateOf<MinigameDef?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locale.getString("arcade_title", "Arcade Minigames • Expressive")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = locale.getString("back", "Back"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
            items(minigames, key = { it.id }) { minigame ->
                val launchInteractionSource = remember(minigame.id) { MutableInteractionSource() }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (reducedMotion) Modifier
                            else Modifier.animateItem(
                                placementSpec = androidx.compose.animation.core.spring(
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                                )
                            )
                        )
                        .animateContentSize(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(locale.getString("minigame_${minigame.id}_name", minigame.name), fontSize = 20.sp, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(locale.getString("minigame_${minigame.id}_desc", minigame.description), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { activeMinigame = minigame },
                            shape = MaterialTheme.shapes.medium,
                            interactionSource = launchInteractionSource,
                            modifier = Modifier
                                .expressivePressScale(
                                    interactionSource = launchInteractionSource,
                                    reducedMotion = reducedMotion
                                )
                                .fillMaxWidth()
                        ) {
                            Text(locale.getString("play_minigame_format", "Play %s", locale.getString("minigame_${minigame.id}_name", minigame.name)), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    // Minigame dialogs
    activeMinigame?.let { game ->
        when (game.id) {
            "quick_guess" -> QuickGuessDialog(onDismiss = { activeMinigame = null }, onReward = onEarnReward)
            "reaction_test" -> ReactionTestDialog(onDismiss = { activeMinigame = null }, onReward = onEarnReward)
            "lucky_number" -> LuckyNumberDialog(onDismiss = { activeMinigame = null }, onReward = onEarnReward)
            "memory_match" -> MemoryMatchDialog(onDismiss = { activeMinigame = null }, onReward = onEarnReward)
            "number_rush" -> NumberRushDialog(onDismiss = { activeMinigame = null }, onReward = onEarnReward)
            else -> QuickGuessDialog(onDismiss = { activeMinigame = null }, onReward = onEarnReward)
        }
    }
}

@Composable
fun QuickGuessDialog(onDismiss: () -> Unit, onReward: (Long, BigNumber) -> Unit) {
    val locale = LocalAppLocaleManager.current
    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(10) }
    var target by remember { mutableStateOf(Random.nextLong(1, 10)) }
    var input by remember { mutableStateOf("") }
    var gameOver by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = gameOver) {
        if (!gameOver) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            gameOver = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedText("Quick Guess Minigame")) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!gameOver) {
                    Text(locale.getString("time_left_format", "Time left: %ds", timeLeft), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(locale.getString("score_format", "Score: %d", score), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(locale.getString("guess_number_target_format", "Guess number (1-9): Target %d", target), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text(localizedText("Enter number")) },
                        singleLine = true
                    )
                } else {
                    Text(locale.getString("game_over_format", "Game Over! Score: %d", score), fontSize = 20.sp)
                    Text(locale.getString("earned_minigame_format", "Earned: 1 Nebula & %s Money", BigNumber((score * 500).toLong()).format()))
                }
            }
        },
        confirmButton = {
            if (!gameOver) {
                Button(onClick = {
                    val g = input.toLongOrNull()
                    if (g == target) {
                        score++
                        target = Random.nextLong(1, 10)
                    }
                    input = ""
                }) {
                    Text(locale.getString("guess_button", "Guess"))
                }
            } else {
                Button(onClick = {
                    onReward(1L, BigNumber((score * 500).toLong()))
                    onDismiss()
                }) {
                    Text(localizedText(locale.getString("claim_reward_button", "Claim Reward")))
                }
            }
        }
    )
}

@Composable
fun ReactionTestDialog(onDismiss: () -> Unit, onReward: (Long, BigNumber) -> Unit) {
    val locale = LocalAppLocaleManager.current
    var state by remember { mutableStateOf("wait") } // wait, ready, clicked
    var startTime by remember { mutableStateOf(0L) }
    var reactionTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        delay(Random.nextLong(1000, 3000))
        if (state == "wait") {
            state = "ready"
            startTime = System.currentTimeMillis()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedText("Reaction Test")) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (state) {
                    "wait" -> Text(localizedText("Wait for green..."), fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                    "ready" -> Text(localizedText("TAP NOW!"), fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                    "clicked" -> Text(locale.getString("reaction_time_format", "Reaction: %dms", reactionTime), fontSize = 20.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (state == "ready") {
                        reactionTime = System.currentTimeMillis() - startTime
                        state = "clicked"
                    } else if (state == "wait") {
                        reactionTime = 9999L
                        state = "clicked"
                    } else {
                        onReward(1L, BigNumber(2000))
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state == "ready") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(if (state == "clicked") locale.getString("claim_reward_button", "Claim Reward") else locale.getString("tap", "TAP"))
            }
        }
    )
}

@Composable
fun LuckyNumberDialog(onDismiss: () -> Unit, onReward: (Long, BigNumber) -> Unit) {
    val locale = LocalAppLocaleManager.current
    var chosenBox by remember { mutableStateOf<Int?>(null) }
    val winningBox = remember { Random.nextInt(1, 4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedText("Lucky Number Box")) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (chosenBox == null) {
                    Text(localizedText("Choose one of three mystery boxes to find the jackpot Nebula prize:"), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                        for (i in 1..3) {
                            Button(
                                onClick = {
                                    chosenBox = i
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(locale.getString("box_format", "Box %d", i), fontSize = 16.sp)
                            }
                        }
                    }
                } else {
                    if (chosenBox == winningBox) {
                        Text(locale.getString("jackpot_win_format", "🎉 Jackpot! Box %d was the lucky box! Won 1 Nebula & 5,000 Money!", chosenBox!!), fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(locale.getString("empty_box_format", "❌ Box %d was empty! The winning box was Box %d. Better luck next time!", chosenBox!!, winningBox), fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (chosenBox != null) {
                Button(onClick = {
                    if (chosenBox == winningBox) {
                        onReward(1L, BigNumber(5000))
                    }
                    onDismiss()
                }) {
                    Text(if (chosenBox == winningBox) locale.getString("claim_reward_button", "Claim Reward") else locale.getString("close_button", "Close"))
                }
            }
        }
    )
}

@Composable
fun MemoryMatchDialog(onDismiss: () -> Unit, onReward: (Long, BigNumber) -> Unit) {
    val locale = LocalAppLocaleManager.current
    val sequence = remember { List(3) { Random.nextInt(1, 10) } }
    var showDigits by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var answerCorrect by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000)
        showDigits = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedText("Memory Digit Sequence")) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showDigits) {
                    Text(locale.getString("memorize_format", "Memorize: %s", sequence.joinToString(" ")), fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                } else if (resultText.isEmpty()) {
                    Text(localizedText("Enter the 3 digits (e.g. 123):"), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text(localizedText("Sequence")) },
                        singleLine = true
                    )
                } else {
                    androidx.compose.animation.AnimatedContent(
                        targetState = resultText,
                        label = "memoryMatchResult"
                    ) { result ->
                        Text(result, fontSize = 18.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (!showDigits && resultText.isEmpty()) {
                Button(onClick = {
                    val expected = sequence.joinToString("")
                    if (input.trim() == expected) {
                        answerCorrect = true
                        resultText = locale.getString("memory_correct", "Correct! Won 1 Nebula!")
                    } else {
                        answerCorrect = false
                        resultText = locale.getString("memory_incorrect_format", "Incorrect! Expected %s", expected)
                    }
                }) {
                    Text(localizedText("Submit"))
                }
            } else if (resultText.isNotEmpty()) {
                Button(onClick = {
                    if (answerCorrect) {
                        onReward(1L, BigNumber(10000))
                    }
                    onDismiss()
                }) {
                    Text(locale.getString("done_button", "Done"))
                }
            }
        }
    )
}

@Composable
fun NumberRushDialog(onDismiss: () -> Unit, onReward: (Long, BigNumber) -> Unit) {
    val locale = LocalAppLocaleManager.current
    var currentTarget by remember { mutableStateOf(1) }
    val numbers = remember { (1..5).shuffled() }
    var finished by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedText("Number Rush")) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!finished) {
                    Text(locale.getString("tap_numbers_order_format", "Tap numbers in order: %d to 5", currentTarget), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    // Use a vertical arrangement or wrap layout with generous touch targets and padding
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Arrange in 2 rows or flexible grid to prevent horizontal cramped clipping
                        val chunks = numbers.chunked(3)
                        chunks.forEach { rowNums ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowNums.forEach { num ->
                                    Button(
                                        onClick = {
                                            if (num == currentTarget) {
                                                if (currentTarget == 5) {
                                                    finished = true
                                                } else {
                                                    currentTarget++
                                                }
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Text("$num", fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(localizedText("Rush Completed Successfully! Won 1 Nebula!"), fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            if (finished) {
                Button(onClick = {
                    onReward(1L, BigNumber(7500))
                    onDismiss()
                }) {
                    Text(locale.getString("claim_reward_button", "Claim Reward"))
                }
            }
        }
    )
}
