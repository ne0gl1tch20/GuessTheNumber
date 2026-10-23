package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
 * state. The Jarrlyy text-field primitive owns the consistent control shape
 * and minimum target size on Android and desktop.
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
    keyboardType: KeyboardType = KeyboardType.Number,
    imeAction: ImeAction = ImeAction.Default,
    submitOnEnter: Boolean = false
) {
    JarrlyyTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label ($minNumber–$maxNumber)", style = MaterialTheme.typography.bodyMedium) },
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = modifier.onKeyEvent { event ->
            if (submitOnEnter && event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                onSubmit()
                true
            } else {
                false
            }
        }
    )
}
