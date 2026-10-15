package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameReducer
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameState
import kotlin.random.Random

/**
 * Standalone pilot host for the controlled shared guessing UI.
 *
 * Production hosts should call SharedGuessGameContent with their own state and
 * action handler so platform-specific services remain outside commonMain.
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
        mutableStateOf(newGame(minNumber, maxNumber))
    }
    var guessText by remember { mutableStateOf("") }

    fun dispatch(action: SharedGameAction) {
        when (action) {
            is SharedGameAction.SubmitGuess -> {
                if (game.solved) return
                game = GuessGameReducer.submitGuess(game, action.guess)
                if (game.solved) onSolved(game.attempts.toInt())
            }
            SharedGameAction.StartNewGame -> {
                game = newGame(minNumber, maxNumber)
                guessText = ""
            }
            is SharedGameAction.Navigate -> Unit
        }
    }

    SharedGuessGameContent(
        game = game,
        guessText = guessText,
        onGuessTextChange = { guessText = it.filter(Char::isDigit).take(maxNumber.toString().length) },
        onAction = ::dispatch,
        modifier = modifier
    )
}

private fun newGame(minNumber: Long, maxNumber: Long): GuessGameState =
    GuessGameReducer.newGame(
        targetNumber = Random.nextLong(minNumber, maxNumber + 1L),
        rangeMin = minNumber,
        rangeMax = maxNumber
    )
