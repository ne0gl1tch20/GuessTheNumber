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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameState
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameStatus

/**
 * Controlled shared guessing UI for platform hosts.
 *
 * The host owns the game state, target generation, persistence and navigation.
 * This component only renders the supplied state and emits typed actions.
 */
@Composable
fun SharedGuessGameContent(
    game: GuessGameState,
    guessText: String,
    onGuessTextChange: (String) -> Unit,
    onAction: (SharedGameAction) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Guess The Number"
) {
    val presentation = SharedGuessPresentationState.from(game)
    val feedback = when (presentation.status) {
        GuessGameStatus.READY ->
            "Guess a number from ${presentation.rangeMin} to ${presentation.rangeMax}."
        GuessGameStatus.INVALID_INPUT ->
            "Enter a whole number from ${presentation.rangeMin} to ${presentation.rangeMax}."
        GuessGameStatus.TOO_LOW -> "Too low. Try again."
        GuessGameStatus.TOO_HIGH -> "Too high. Try again."
        GuessGameStatus.CORRECT -> "Correct! You won in ${presentation.guessCount} guesses."
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
                        onGuessTextChange = onGuessTextChange,
                        onSubmitGuess = {
                            onAction(SharedGameAction.SubmitGuess(guessText.toLongOrNull()))
                        },
                        onNewGame = { onAction(SharedGameAction.StartNewGame) },
                        feedback = feedback,
                        guessCount = presentation.guessCount,
                        minNumber = presentation.rangeMin,
                        maxNumber = presentation.rangeMax,
                        solved = presentation.solved,
                        title = title
                    )
                }
            }
        }
    }
}
