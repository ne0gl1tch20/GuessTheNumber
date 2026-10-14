package com.jarrlyyy.guessthenumber.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

object ExpressiveMotion {
    val standardSpring = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow
    )
    val emphasizedSpring = spring<Float>(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessMedium
    )
    val fastEasing = FastOutSlowInEasing
}

@Composable
fun ExpressiveScreen(enabled: Boolean = true, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(ExpressiveMotion.standardSpring) +
            scaleIn(initialScale = 0.97f, animationSpec = ExpressiveMotion.standardSpring),
        exit = fadeOut() + scaleOut(targetScale = 0.985f)
    ) {
        Box { content() }
    }
}

@Composable
fun Modifier.expressiveSelection(selected: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = ExpressiveMotion.emphasizedSpring,
        label = "expressive-selection-scale"
    )
    return this.scale(scale)
}
