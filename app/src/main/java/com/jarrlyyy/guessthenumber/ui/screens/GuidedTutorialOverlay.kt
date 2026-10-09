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
            targetState = step,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn() togetherWith fadeOut()
                } else {
                    (scaleIn(initialScale = 0.96f) + fadeIn()) togetherWith
                        (scaleOut(targetScale = 0.98f) + fadeOut())
                }
            },
            label = "tutorialStepTransition"
        ) { animatedStep ->
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (animatedStep == step) stepLabel else stepLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(instruction, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}