package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlin.random.Random

/**
 * Windows presentation for the production engine. Persistence and the wider
 * economy UI are introduced in their dedicated Stage 2 work packages.
 */
@Composable
fun ProductionGuessGameScreen(modifier: Modifier = Modifier) {
    val engine = remember { GameEngine() }
    var state by remember {
        mutableStateOf(
            GameState(
                targetNumber = Random.nextLong(1L, 101L),
                currentRangeMin = 1L,
                currentRangeMax = 100L
            )
        )
    }
    var guessText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("Guess a number from 1 to 100.") }

    Card(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Guess The Number", style = MaterialTheme.typography.headlineMedium)
            Text("Find the hidden number between ${state.currentRangeMin} and ${state.currentRangeMax}.")
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Money", style = MaterialTheme.typography.labelMedium)
                    Text(state.money.toString(), style = MaterialTheme.typography.titleLarge)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Attempts", style = MaterialTheme.typography.labelMedium)
                    Text(state.attempts.toString(), style = MaterialTheme.typography.titleLarge)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Streak", style = MaterialTheme.typography.labelMedium)
                    Text(state.streak.toString(), style = MaterialTheme.typography.titleLarge)
                }
            }
            OutlinedTextField(
                value = guessText,
                onValueChange = { input -> guessText = input.filter(Char::isDigit).take(18) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your guess") },
                singleLine = true
            )
            Button(
                onClick = {
                    val guess = guessText.toLongOrNull()
                    if (guess == null) {
                        feedback = "Enter a whole number first."
                    } else if (guess !in state.currentRangeMin..state.currentRangeMax) {
                        feedback = "Choose a number within the displayed range."
                    } else {
                        val result = engine.processGuess(state, guess)
                        state = result.newState
                        feedback = when (result.feedback) {
                            GuessFeedback.TOO_LOW -> "Too low. Try again!"
                            GuessFeedback.TOO_HIGH -> "Too high. Try again!"
                            GuessFeedback.CORRECT -> "Correct! You earned ${result.reward} Money."
                        }
                        guessText = ""
                    }
                },
                enabled = guessText.isNotBlank()
            ) {
                Text("Submit guess")
            }
            Text(feedback, style = MaterialTheme.typography.bodyLarge)
            Text(
                "This screen now uses the production game engine. Desktop saving and the full shop/progression UI are coming in later migration steps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
