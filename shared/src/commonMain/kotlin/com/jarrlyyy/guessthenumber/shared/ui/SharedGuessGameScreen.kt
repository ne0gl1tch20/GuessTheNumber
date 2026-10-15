package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameReducer
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameStatus
import kotlin.random.Random

/**
 * Shared Compose pilot backed by a platform-independent state reducer.
 *
 * The responsive shell and guess-entry controls are common UI components.
 * Production economy/save state remains owned by the existing game engine.
 */
@Composable
fun SharedGuessGameScreen(
    modifier: Modifier = Modifier,
    minNumber: Long = 1L,
    maxNumber: Long = 100L,
    onSolved: (attempts: Int) -> Unit = {}
) {
    require(maxNumber >= minNumber) { "maxNumber must be >= minNumber" }

    var game by remember(minNumber, maxNumber) {
        mutableStateOf(
            GuessGameReducer.newGame(
                targetNumber = Random.nextLong(minNumber, maxNumber + 1L),
                rangeMin = minNumber,
                rangeMax = maxNumber
            )
        )
    }
    var guessText by remember { mutableStateOf("") }

    fun submitGuess() {
        if (game.solved) return
        game = GuessGameReducer.submitGuess(game, guessText.toLongOrNull())
        if (game.solved) onSolved(game.attempts.toInt())
    }

    val feedback = when (game.status) {
        GuessGameStatus.READY -> "Guess a number from $minNumber to $maxNumber."
        GuessGameStatus.INVALID_INPUT -> "Enter a whole number from $minNumber to $maxNumber."
        GuessGameStatus.TOO_LOW -> "Too low. Try again."
        GuessGameStatus.TOO_HIGH -> "Too high. Try again."
        GuessGameStatus.CORRECT -> "Correct! You won in ${game.attempts} guesses."
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
                    Text(
                        "One game, shared rules. Layout adapts to your window.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SharedGuessEntryPanel(
                        guessText = guessText,
                        onGuessTextChange = { guessText = it.filter(Char::isDigit).take(maxNumber.toString().length) },
                        onSubmitGuess = { submitGuess() },
                        onNewGame = {
                            game = GuessGameReducer.newGame(
                                targetNumber = Random.nextLong(minNumber, maxNumber + 1L),
                                rangeMin = minNumber,
                                rangeMax = maxNumber
                            )
                            guessText = ""
                        },
                        feedback = feedback,
                        guessCount = game.attempts,
                        minNumber = minNumber,
                        maxNumber = maxNumber,
                        solved = game.solved
                    )
                }
            }
        }
    }
}
