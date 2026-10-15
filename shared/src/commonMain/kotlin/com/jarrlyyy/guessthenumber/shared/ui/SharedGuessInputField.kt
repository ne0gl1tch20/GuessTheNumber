package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Shared controlled numeric guess input.
 *
 * The caller owns validation, localization, submission behavior, and game
 * state. This keeps the interaction contract in commonMain while allowing
 * platform screens to preserve their existing surrounding layout and styling.
 */
@Composable
fun SharedGuessInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    minNumber: Long,
    maxNumber: Long,
    modifier: Modifier = Modifier,
    label: String = "Your guess",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label ($minNumber–$maxNumber)") },
        singleLine = singleLine,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = modifier.onKeyEvent { event ->
            if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                onSubmit()
                true
            } else {
                false
            }
        }
    )
}
