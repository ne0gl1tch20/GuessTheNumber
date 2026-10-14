package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.shared.rules.GuessFeedback
import com.jarrlyyy.guessthenumber.shared.rules.GuessRules
import kotlin.random.Random

/**
 * First shared Compose gameplay surface.
 *
 * This is deliberately a small UI pilot, not a replacement for the Android
 * production PlayScreen. Keep game rules in shared domain code and add
 * platform-specific navigation/state adapters before migrating more screens.
 */
@Composable
fun SharedGuessGameScreen(
    modifier: Modifier = Modifier,
    minNumber: Long = 1L,
    maxNumber: Long = 100L,
    onSolved: (attempts: Int) -> Unit = {}
) {
    require(maxNumber >= minNumber) { "maxNumber must be >= minNumber" }

    var target by remember(minNumber, maxNumber) {
        mutableLongStateOf(Random.nextLong(minNumber, maxNumber + 1L))
    }
    var guessText by remember { mutableStateOf("") }
    var attempts by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    var feedback by remember(minNumber, maxNumber) {
        mutableStateOf("Guess a number from $minNumber to $maxNumber.")
    }

    fun submitGuess() {
        val guess = guessText.toLongOrNull()
        if (solved) return
        if (guess == null || guess !in minNumber..maxNumber) {
            feedback = "Enter a whole number from $minNumber to $maxNumber."
            return
        }

        attempts += 1
        when (GuessRules.resolveFeedback(target, guess, minNumber, maxNumber)) {
            GuessFeedback.TOO_LOW -> feedback = "Too low. Try again."
            GuessFeedback.TOO_HIGH -> feedback = "Too high. Try again."
            GuessFeedback.CORRECT -> {
                solved = true
                feedback = "Correct! You won in $attempts guesses."
                onSolved(attempts)
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val compact = maxWidth < 680.dp
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (compact) 1f else 0.78f)
                    .widthIn(max = 760.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(if (compact) 20.dp else 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp)
                ) {
                    Text("Guess The Number", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "One game, shared rules. Layout adapts to your window.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = guessText,
                        onValueChange = { guessText = it.filter(Char::isDigit).take(maxNumber.toString().length) },
                        label = { Text("Your guess ($minNumber–$maxNumber)") },
                        singleLine = true,
                        enabled = !solved,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitGuess() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                                    submitGuess()
                                    true
                                } else false
                            }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        Button(onClick = { submitGuess() }, enabled = !solved && guessText.isNotBlank()) {
                            Text("Guess")
                        }
                        Button(onClick = {
                            target = Random.nextLong(minNumber, maxNumber + 1L)
                            guessText = ""
                            attempts = 0
                            solved = false
                            feedback = "New number chosen. Guess from $minNumber to $maxNumber."
                        }) {
                            Text("New game")
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(feedback, style = MaterialTheme.typography.bodyLarge)
                    Text("Guesses: $attempts", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
