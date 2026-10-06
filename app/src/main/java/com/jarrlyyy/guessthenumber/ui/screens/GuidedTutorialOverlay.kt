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
fun GuidedTutorialOverlay(step: Int, title: String, instruction: String, stepLabel: String) {
    val displayTitle = title
    val displayInstruction = instruction
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 8.dp, border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stepLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(displayTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(displayInstruction, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}