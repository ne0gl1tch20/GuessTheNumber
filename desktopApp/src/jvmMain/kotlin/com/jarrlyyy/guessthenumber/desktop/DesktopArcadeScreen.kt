package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopMinigameDef
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
internal fun DesktopArcadeScreen(
    state: GameState,
    minigames: List<DesktopMinigameDef>,
    isRewarding: Boolean,
    onEarnReward: (Long, BigNumber) -> Unit,
    onBack: () -> Unit
) {
    var activeGame by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Arcade", style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            "Play quick minigames for Nebula and Money. Rewards use the shared economy rules and are saved to your active slot.",
            style = MaterialTheme.typography.bodyLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Money", style = MaterialTheme.typography.labelLarge)
                    Text(state.money.toString(), style = MaterialTheme.typography.titleLarge)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nebula", style = MaterialTheme.typography.labelLarge)
                    Text(state.nebula.toString(), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        if (minigames.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "The minigame catalog could not be loaded. No rewards are available until the canonical catalog is restored.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            minigames.forEach { game ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(game.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text(game.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { activeGame = game.id },
                            enabled = !isRewarding,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Play ${game.name}")
                        }
                    }
                }
            }
        }
    }

    when (activeGame) {
        "quick_guess" -> QuickGuessArcadeDialog(
            onDismiss = { activeGame = null },
            isRewarding = isRewarding,
            onReward = { nebula, money -> onEarnReward(nebula, money); activeGame = null }
        )
        "reaction_test" -> ReactionTestArcadeDialog(
            onDismiss = { activeGame = null },
            isRewarding = isRewarding,
            onReward = { nebula, money -> onEarnReward(nebula, money); activeGame = null }
        )
        "lucky_number" -> LuckyNumberArcadeDialog(
            onDismiss = { activeGame = null },
            isRewarding = isRewarding,
            onReward = { nebula, money -> onEarnReward(nebula, money); activeGame = null }
        )
        "memory_match" -> MemoryMatchArcadeDialog(
            onDismiss = { activeGame = null },
            isRewarding = isRewarding,
            onReward = { nebula, money -> onEarnReward(nebula, money); activeGame = null }
        )
        "number_rush" -> NumberRushArcadeDialog(
            onDismiss = { activeGame = null },
            isRewarding = isRewarding,
            onReward = { nebula, money -> onEarnReward(nebula, money); activeGame = null }
        )
        null -> Unit
        else -> {
            // Fail closed for an unsupported catalog ID instead of inventing a reward.
            AlertDialog(
                onDismissRequest = { activeGame = null },
                title = { Text("Minigame unavailable") },
                text = { Text("This minigame ID is not supported by the Windows implementation yet.") },
                confirmButton = { TextButton(onClick = { activeGame = null }) { Text("Close") } }
            )
        }
    }
}

@Composable
private fun QuickGuessArcadeDialog(
    onDismiss: () -> Unit,
    isRewarding: Boolean,
    onReward: (Long, BigNumber) -> Unit
) {
    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(10) }
    var target by remember { mutableStateOf(Random.nextLong(1, 10)) }
    var input by remember { mutableStateOf("") }
    var gameOver by remember { mutableStateOf(false) }

    LaunchedEffect(gameOver) {
        if (!gameOver) {
            while (timeLeft > 0) {
                delay(1_000)
                timeLeft--
            }
            gameOver = true
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Guess") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!gameOver) {
                    Text("Time left: ${timeLeft}s", color = MaterialTheme.colorScheme.primary)
                    Text("Score: $score", style = MaterialTheme.typography.titleLarge)
                    Text("Guess the target from 1 to 9: $target")
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.filter(Char::isDigit).take(1) },
                        label = { Text("Your guess") },
                        singleLine = true
                    )
                } else {
                    Text("Time's up! Score: $score", style = MaterialTheme.typography.titleLarge)
                    Text("Reward: 1 Nebula and ${score * 500} Money")
                }
            }
        },
        confirmButton = {
            if (!gameOver) {
                Button(onClick = {
                    if (input.toLongOrNull() == target) {
                        score++
                        target = Random.nextLong(1, 10)
                    }
                    input = ""
                }) { Text("Guess") }
            } else {
                Button(onClick = { onReward(1L, BigNumber((score * 500).toLong())) }, enabled = !isRewarding) {
                    Text(if (isRewarding) "Saving…" else "Claim reward")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ReactionTestArcadeDialog(
    onDismiss: () -> Unit,
    isRewarding: Boolean,
    onReward: (Long, BigNumber) -> Unit
) {
    var phase by remember { mutableStateOf("wait") }
    var startTime by remember { mutableStateOf(0L) }
    var reactionTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        delay(Random.nextLong(1_000, 3_000))
        if (phase == "wait") {
            phase = "ready"
            startTime = System.currentTimeMillis()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reaction Test") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (phase) {
                    "wait" -> Text("Wait for green…", color = MaterialTheme.colorScheme.error, fontSize = 20.sp)
                    "ready" -> Text("TAP NOW!", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp)
                    else -> Text(if (reactionTime == 9_999L) "Too early!" else "Reaction: ${reactionTime}ms", fontSize = 20.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (phase) {
                        "wait" -> { reactionTime = 9_999L; phase = "clicked" }
                        "ready" -> { reactionTime = System.currentTimeMillis() - startTime; phase = "clicked" }
                        else -> onReward(1L, BigNumber(2_000))
                    }
                },
                enabled = phase != "clicked" || !isRewarding
            ) { Text(if (phase == "clicked") if (isRewarding) "Saving…" else "Claim reward" else "Tap") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun LuckyNumberArcadeDialog(
    onDismiss: () -> Unit,
    isRewarding: Boolean,
    onReward: (Long, BigNumber) -> Unit
) {
    var chosenBox by remember { mutableStateOf<Int?>(null) }
    val winningBox = remember { Random.nextInt(1, 4) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lucky Number Box") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (chosenBox == null) {
                    Text("Choose one of three mystery boxes to find the jackpot Nebula prize.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { box ->
                            Button(onClick = { chosenBox = box }, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                                Text("Box $box")
                            }
                        }
                    }
                } else if (chosenBox == winningBox) {
                    Text("🎉 Jackpot! Box $winningBox won 1 Nebula and 5,000 Money.", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Box $chosenBox was empty. The winning box was Box $winningBox.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (chosenBox != null) {
                Button(
                    onClick = { if (chosenBox == winningBox) onReward(1L, BigNumber(5_000)) else onDismiss() },
                    enabled = !isRewarding
                ) { Text(if (chosenBox == winningBox) if (isRewarding) "Saving…" else "Claim reward" else "Close") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MemoryMatchArcadeDialog(
    onDismiss: () -> Unit,
    isRewarding: Boolean,
    onReward: (Long, BigNumber) -> Unit
) {
    val sequence = remember { List(3) { Random.nextInt(1, 10) } }
    var showDigits by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        delay(2_000)
        showDigits = false
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Memory Digit Sequence") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (showDigits) {
                    Text("Memorize: ${sequence.joinToString(" ")}", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                } else if (result == null) {
                    Text("Enter the three digits in order.")
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.filter(Char::isDigit).take(3) },
                        label = { Text("Sequence") },
                        singleLine = true
                    )
                } else {
                    Text(if (result == true) "Correct! Won 1 Nebula." else "Incorrect. Expected ${sequence.joinToString("")}")
                }
            }
        },
        confirmButton = {
            if (!showDigits && result == null) {
                Button(onClick = { result = input == sequence.joinToString("") }) { Text("Submit") }
            } else if (result != null) {
                Button(
                    onClick = { if (result == true) onReward(1L, BigNumber(10_000)) else onDismiss() },
                    enabled = !isRewarding
                ) { Text(if (result == true) if (isRewarding) "Saving…" else "Claim reward" else "Close") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NumberRushArcadeDialog(
    onDismiss: () -> Unit,
    isRewarding: Boolean,
    onReward: (Long, BigNumber) -> Unit
) {
    var currentTarget by remember { mutableStateOf(1) }
    val numbers = remember { (1..5).shuffled() }
    var finished by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Number Rush") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!finished) {
                    Text("Tap numbers in ascending order: $currentTarget to 5")
                    numbers.chunked(3).forEach { rowNumbers ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowNumbers.forEach { number ->
                                Button(onClick = {
                                    if (number == currentTarget) {
                                        if (currentTarget == 5) finished = true else currentTarget++
                                    }
                                }) { Text(number.toString()) }
                            }
                        }
                    }
                } else {
                    Text("Rush completed! Won 1 Nebula.", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            if (finished) {
                Button(onClick = { onReward(1L, BigNumber(7_500)) }, enabled = !isRewarding) {
                    Text(if (isRewarding) "Saving…" else "Claim reward")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
