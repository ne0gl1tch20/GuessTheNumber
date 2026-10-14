package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jarrlyyy.guessthenumber.shared.domain.DifficultyRules
import com.jarrlyyy.guessthenumber.shared.domain.GameState
import com.jarrlyyy.guessthenumber.shared.engine.GameEngine
import com.jarrlyyy.guessthenumber.shared.rules.GuessFeedback
import kotlin.random.Random

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Guess The Number"
    ) {
        MaterialTheme {
            DesktopGameScreen()
        }
    }
}

@Composable
private fun DesktopGameScreen() {
    val engine = remember { GameEngine() }
    val saveStore = remember { DesktopSaveStore() }
    var gameState by remember { mutableStateOf(saveStore.loadOrDefault()) }
    var guess by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("Guess a number to earn money.") }
    var saveStatus by remember { mutableStateOf("Loading local save…") }
    var luaActivity by remember { mutableStateOf<DesktopLuaActivity?>(null) }
    var luaResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gameState) {
        saveStatus = if (saveStore.save(gameState)) {
            "Saved locally with recovery backup"
        } else {
            "Couldn't save to disk. Check folder permissions or free space."
        }
    }

    val submitGuess = {
        val parsedGuess = guess.toLongOrNull()
        when {
            parsedGuess == null -> {
                feedback = "Enter a whole number within the current range."
            }
            parsedGuess !in gameState.currentRangeMin..gameState.currentRangeMax -> {
                feedback = "Choose a number from ${gameState.currentRangeMin} to ${gameState.currentRangeMax}."
            }
            else -> {
                val result = engine.processGuess(gameState, parsedGuess)
                gameState = result.newState
                feedback = when (result.feedback) {
                    GuessFeedback.TOO_LOW -> "Too low. Try again."
                    GuessFeedback.TOO_HIGH -> "Too high. Try again."
                    GuessFeedback.CORRECT -> "Correct! Earned ${result.reward.format()} money."
                }
                if (result.feedback == GuessFeedback.CORRECT) guess = ""
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Guess The Number", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text("Shared Kotlin game engine · offline desktop build")
                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DifficultyButton("Classic", DifficultyRules.CLASSIC, gameState.difficultyId) {
                        gameState = changeDifficulty(gameState, it, engine)
                        feedback = "Difficulty changed to Classic."
                    }
                    DifficultyButton("Hard", DifficultyRules.HARD, gameState.difficultyId) {
                        gameState = changeDifficulty(gameState, it, engine)
                        feedback = "Difficulty changed to Hard."
                    }
                    DifficultyButton("Extreme", DifficultyRules.EXTREME, gameState.difficultyId) {
                        gameState = changeDifficulty(gameState, it, engine)
                        feedback = "Difficulty changed to Extreme."
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        runCatching {
                            DesktopLuaActivity.load("scripts/minigames/number_rush.lua")
                        }.onSuccess {
                            luaActivity = it
                            luaResult = null
                        }.onFailure {
                            feedback = "Lua activity couldn't start: ${it.message ?: "unknown error"}"
                        }
                    }
                ) {
                    Text("Play Number Rush (Lua)")
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Range: ${gameState.currentRangeMin}–${gameState.currentRangeMax}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = guess,
                    onValueChange = { guess = it.filter(Char::isDigit).take(12) },
                    label = { Text("Your guess") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = submitGuess, enabled = guess.isNotBlank()) {
                        Text("Guess")
                    }
                    Button(
                        onClick = {
                            gameState = gameState.copy(
                                targetNumber = Random.nextLong(
                                    gameState.currentRangeMin,
                                    gameState.currentRangeMax + 1L
                                )
                            )
                            guess = ""
                            feedback = "A new target was chosen. Your progression is kept."
                        }
                    ) {
                        Text("New target")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(feedback, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(20.dp))
                Text("Money: ${gameState.money.format()}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("Streak: ${gameState.streak}   ·   Best: ${gameState.bestStreak}")
                Text("Total guesses: ${gameState.statistics.totalGuesses}")
                Text("Correct guesses: ${gameState.correctGuesses}")
                Spacer(Modifier.height(8.dp))
                Text(saveStatus, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("More progression screens and localization are still being ported.")

                luaActivity?.let { activity ->
                    Spacer(Modifier.height(24.dp))
                    Text(activity.title, style = MaterialTheme.typography.titleLarge)
                    Text(activity.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(activity.prompt, style = MaterialTheme.typography.titleMedium)
                    activity.options.forEachIndexed { index, option ->
                        Button(onClick = {
                            luaResult = if (index == activity.correctOption) {
                                activity.successMessage
                            } else {
                                activity.failureMessage
                            }
                        }) {
                            Text(option)
                        }
                    }
                    luaResult?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    Button(onClick = {
                        luaActivity = null
                        luaResult = null
                    }) {
                        Text("Close activity")
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyButton(
    label: String,
    difficultyId: String,
    selectedDifficulty: String,
    onSelect: (String) -> Unit
) {
    Button(
        onClick = { onSelect(difficultyId) },
        enabled = difficultyId != selectedDifficulty
    ) {
        Text(label)
    }
}

private fun changeDifficulty(
    state: GameState,
    difficultyId: String,
    engine: GameEngine
): GameState {
    val max = engine.getInitialRangeMax(difficultyId)
    return state.copy(
        difficultyId = difficultyId,
        currentRangeMin = 1L,
        currentRangeMax = max,
        targetNumber = Random.nextLong(1L, max + 1L),
        streak = 0
    )
}
