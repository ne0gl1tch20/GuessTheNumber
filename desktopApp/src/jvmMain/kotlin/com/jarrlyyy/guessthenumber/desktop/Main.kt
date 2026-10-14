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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jarrlyyy.guessthenumber.shared.rules.GuessFeedback
import com.jarrlyyy.guessthenumber.shared.rules.GuessRules
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
    var targetNumber by remember { mutableStateOf(Random.nextLong(1L, 101L)) }
    var guess by remember { mutableStateOf("") }
    var attempts by remember { mutableStateOf(0) }
    var hasWon by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("Guess a number from 1 to 100.") }

    val submitGuess = {
        val parsedGuess = guess.toIntOrNull()
        when {
            hasWon -> Unit
            parsedGuess == null || parsedGuess !in 1..100 -> {
                feedback = "Enter a whole number from 1 to 100."
            }
            else -> {
                attempts += 1
                when (
                    GuessRules.resolveFeedback(
                        targetNumber = targetNumber,
                        guess = parsedGuess.toLong(),
                        rangeMin = 1L,
                        rangeMax = 100L
                    )
                ) {
                    GuessFeedback.TOO_LOW -> feedback = "Too low. Try again."
                    GuessFeedback.TOO_HIGH -> feedback = "Too high. Try again."
                    GuessFeedback.CORRECT -> {
                        hasWon = true
                        feedback = "Correct! You won in $attempts guesses."
                    }
                }
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
                    .widthIn(max = 560.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Guess The Number", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(12.dp))
                Text("A first Windows desktop shell powered by the shared Kotlin rules.")
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = guess,
                    onValueChange = { guess = it.filter(Char::isDigit).take(3) },
                    label = { Text("Your guess (1–100)") },
                    singleLine = true,
                    enabled = !hasWon,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = submitGuess, enabled = !hasWon && guess.isNotBlank()) {
                        Text("Guess")
                    }
                    Button(
                        onClick = {
                            targetNumber = Random.nextLong(1L, 101L)
                            guess = ""
                            attempts = 0
                            hasWon = false
                            feedback = "New number chosen. Guess from 1 to 100."
                        }
                    ) {
                        Text("New game")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(feedback, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("Guesses: $attempts", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
