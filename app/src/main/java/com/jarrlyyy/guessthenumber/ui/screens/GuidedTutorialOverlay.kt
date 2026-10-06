package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GuidedTutorialOverlay(step: Int) {
    val title = when (step) { 0 -> "🎮 Try your first guess"; 1 -> "📈 Open Upgrades"; 2 -> "🧭 Open More"; else -> "⚙️ Open Settings" }
    val instruction = when (step) {
        0 -> "This is the real Play screen. Enter a number and press GUESS NUMBER to continue."
        1 -> "Tap the highlighted Upgrades tab in the bottom navigation."
        2 -> "Tap the highlighted More tab to explore the rest of the game."
        else -> "Inside More, open Settings. This finishes your guided tour."
    }
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 8.dp, border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Step " + (step + 1) + " of 4  •  👆 Action required", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(instruction, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}