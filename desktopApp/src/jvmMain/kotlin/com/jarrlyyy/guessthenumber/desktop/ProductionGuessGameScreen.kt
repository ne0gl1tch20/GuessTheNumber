package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveStore
import com.jarrlyyy.guessthenumber.desktop.persistence.defaultDesktopSaveDirectory
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Windows presentation backed by the production engine and desktop save adapter. */
@Composable
fun ProductionGuessGameScreen(modifier: Modifier = Modifier) {
    val engine = remember { GameEngine() }
    val saveStore = remember { DesktopSaveStore(defaultDesktopSaveDirectory()) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<GameState?>(null) }
    var guessText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("Loading your saved game…") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { saveStore.load(1) }
        if (loaded != null) {
            state = loaded
            feedback = "Saved game loaded."
        } else {
            val fresh = GameState(
                targetNumber = Random.nextLong(1L, 101L),
                currentRangeMin = 1L,
                currentRangeMax = 100L
            )
            val created = withContext(Dispatchers.IO) { saveStore.save(1, fresh) }
            if (created) {
                state = fresh
                feedback = "New game created."
            } else {
                feedback = "Could not create the desktop save. Try again."
            }
        }
    }

    val currentState = state
    Card(modifier = modifier.fillMaxSize()) {
        if (currentState == null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(feedback)
            }
            return@Card
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Guess The Number", style = MaterialTheme.typography.headlineMedium)
            Text("Find the hidden number between ${currentState.currentRangeMin} and ${currentState.currentRangeMax}.")
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Money", style = MaterialTheme.typography.labelMedium)
                    Text(currentState.money.toString(), style = MaterialTheme.typography.titleLarge)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Attempts", style = MaterialTheme.typography.labelMedium)
                    Text(currentState.attempts.toString(), style = MaterialTheme.typography.titleLarge)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Streak", style = MaterialTheme.typography.labelMedium)
                    Text(currentState.streak.toString(), style = MaterialTheme.typography.titleLarge)
                }
            }
            OutlinedTextField(
                value = guessText,
                onValueChange = { input -> guessText = input.filter(Char::isDigit).take(18) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your guess") },
                singleLine = true,
                enabled = !isSaving
            )
            Button(
                onClick = {
                    val guess = guessText.toLongOrNull()
                    if (guess == null) {
                        feedback = "Enter a whole number first."
                    } else if (guess !in currentState.currentRangeMin..currentState.currentRangeMax) {
                        feedback = "Choose a number within the displayed range."
                    } else {
                        val result = engine.processGuess(currentState, guess)
                        scope.launch {
                            isSaving = true
                            val saved = withContext(Dispatchers.IO) {
                                saveStore.save(1, result.newState)
                            }
                            if (saved) {
                                state = result.newState
                                feedback = when (result.feedback) {
                                    GuessFeedback.TOO_LOW -> "Too low. Try again!"
                                    GuessFeedback.TOO_HIGH -> "Too high. Try again!"
                                    GuessFeedback.CORRECT -> "Correct! You earned ${result.reward} Money."
                                }
                                guessText = ""
                            } else {
                                feedback = "Could not save that guess. Your previous saved state is still intact."
                            }
                            isSaving = false
                        }
                    }
                },
                enabled = guessText.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "Saving…" else "Submit guess")
            }
            Text(feedback, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Your Windows run is saved locally after each accepted guess. A backup is kept for recovery if the primary save is damaged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
