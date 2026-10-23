package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared, controlled guess-entry UI.
 *
 * Game rules, validation, localization, persistence, and rewards remain owned
 * by the caller. This uses the Jarrlyy Compose primitives while preserving the
 * existing action callbacks, labels, enabled states, and component order.
 */
@Composable
fun SharedGuessEntryPanel(
    guessText: String,
    onGuessTextChange: (String) -> Unit,
    onSubmitGuess: () -> Unit,
    onNewGame: () -> Unit,
    feedback: String,
    guessCount: Long,
    minNumber: Long,
    maxNumber: Long,
    solved: Boolean,
    modifier: Modifier = Modifier,
    title: String = "Guess The Number",
    guessLabel: String = "Your guess",
    submitLabel: String = "Guess",
    newGameLabel: String = "New game",
    guessCountLabel: String = "Guesses"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        SharedGuessInputField(
            value = guessText,
            onValueChange = onGuessTextChange,
            onSubmit = onSubmitGuess,
            minNumber = minNumber,
            maxNumber = maxNumber,
            label = guessLabel,
            enabled = !solved,
            submitOnEnter = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            JarrlyyButton(
                onClick = onSubmitGuess,
                enabled = !solved && guessText.isNotBlank()
            ) {
                Text(submitLabel)
            }
            JarrlyySecondaryButton(onClick = onNewGame) {
                Text(newGameLabel)
            }
        }
        Text(feedback, style = MaterialTheme.typography.bodyLarge)
        Text("$guessCountLabel: $guessCount", style = MaterialTheme.typography.bodyMedium)
    }
}
