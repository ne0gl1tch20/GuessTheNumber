package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val minigames = remember {
        JsonConfigRepository(context).loadMinigames()
    }

    var activeMinigame by remember { mutableStateOf<MinigameDef?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arcade Minigames") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(minigames) { minigame ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(minigame.name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                        Text(minigame.description, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { activeMinigame = minigame }) {
                            Text("Play ${minigame.name}")
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
        title = { Text("Quick Guess Minigame") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!gameOver) {
                    Text("Time left: ${timeLeft}s", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Score: $score", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Guess number (1-9): Target $target", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Enter number") },
                        singleLine = true
                    )
                } else {
                    Text("Game Over! Score: $score", fontSize = 20.sp)
                    Text("Earned: 1 Nebula & ${score * 500} Money")
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
                    Text("Guess")
                }
            } else {
                Button(onClick = {
                    onReward(1L, BigNumber((score * 500).toLong()))
                    onDismiss()
                }) {
                    Text("Claim Reward")
                }
            }
        }
    )
}

@Composable
fun ReactionTestDialog(onDismiss: () -> Unit, onReward: (Long, BigNumber) -> Unit) {
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
        title = { Text("Reaction Test") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (state) {
                    "wait" -> Text("Wait for green...", fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                    "ready" -> Text("TAP NOW!", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                    "clicked" -> Text("Reaction: ${reactionTime}ms", fontSize = 20.sp)
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
                Text(if (state == "clicked") "Claim Reward" else "TAP")
            }
        }
    )
}

@Composable
fun LuckyNumberDialog(onDismiss: () -> Unit, onReward: (Long, BigNumber) -> Unit) {
    var chosen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lucky Number Box") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!chosen) {
                    Text("Choose one of three mystery boxes:", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 1..3) {
                            Button(onClick = {
                                chosen = true
                            }) {
                                Text("Box $i")
                            }
                        }
                    }
                } else {
                    Text("You opened the box and won 1 Nebula!", fontSize = 18.sp)
                }
            }
        },
        confirmButton = {
            if (chosen) {
                Button(onClick = {
                    onReward(1L, BigNumber(5000))
                    onDismiss()
                }) {
                    Text("Collect")
                }
            }
        }
    )
}

@Composable
fun MemoryMatchDialog(onDismiss: () -> Unit, onReward: (Long, BigNumber) -> Unit) {
    val sequence = remember { List(3) { Random.nextInt(1, 10) } }
    var showDigits by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        delay(2000)
        showDigits = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Memory Digit Sequence") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showDigits) {
                    Text("Memorize: ${sequence.joinToString(" ")}", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                } else if (resultText.isEmpty()) {
                    Text("Enter the 3 digits (e.g. 123):", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Sequence") },
                        singleLine = true
                    )
                } else {
                    Text(resultText, fontSize = 18.sp)
                }
            }
        },
        confirmButton = {
            if (!showDigits && resultText.isEmpty()) {
                Button(onClick = {
                    val expected = sequence.joinToString("")
                    if (input.trim() == expected) {
                        resultText = "Correct! Won 1 Nebula!"
                    } else {
                        resultText = "Incorrect! Expected $expected"
                    }
                }) {
                    Text("Submit")
                }
            } else if (resultText.isNotEmpty()) {
                Button(onClick = {
                    if (resultText.contains("Correct")) {
                        onReward(1L, BigNumber(10000))
                    }
                    onDismiss()
                }) {
                    Text("Done")
                }
            }
        }
    )
}

@Composable
fun NumberRushDialog(onDismiss: () -> Unit, onReward: (Long, BigNumber) -> Unit) {
    var currentTarget by remember { mutableStateOf(1) }
    val numbers = remember { (1..5).shuffled() }
    var finished by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Number Rush") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!finished) {
                    Text("Tap numbers in order: $currentTarget to 5", fontSize = 16.sp)
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
                    Text("Rush Completed Successfully! Won 1 Nebula!", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            if (finished) {
                Button(onClick = {
                    onReward(1L, BigNumber(7500))
                    onDismiss()
                }) {
                    Text("Claim Reward")
                }
            }
        }
    )
}
