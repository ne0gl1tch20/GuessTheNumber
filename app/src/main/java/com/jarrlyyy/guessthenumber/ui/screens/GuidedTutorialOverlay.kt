package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
fun GuidedTutorialOverlay(
    step: Int,
    title: String,
    instruction: String,
    stepLabel: String,
    reducedMotion: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = if (reducedMotion) 2.dp else 8.dp,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        AnimatedContent(
            targetState = TutorialStepContent(step, title, instruction, stepLabel),
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn() togetherWith fadeOut()
                } else {
                    (scaleIn(initialScale = 0.96f) + fadeIn()) togetherWith
                        (scaleOut(targetScale = 0.98f) + fadeOut())
                }
            },
            label = "tutorialStepTransition"
        ) { content ->
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(content.stepLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(content.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(content.instruction, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

private data class TutorialStepContent(
    val step: Int,
    val title: String,
    val instruction: String,
    val stepLabel: String
)