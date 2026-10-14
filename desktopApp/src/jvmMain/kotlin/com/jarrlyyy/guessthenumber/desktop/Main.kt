package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jarrlyyy.guessthenumber.shared.ui.SharedGuessGameScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Guess The Number"
    ) {
        MaterialTheme {
            SharedGuessGameScreen()
        }
    }
}
