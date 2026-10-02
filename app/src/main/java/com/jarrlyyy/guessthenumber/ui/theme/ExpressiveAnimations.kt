package com.jarrlyyy.guessthenumber.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * Material Expressive spring configurations for bouncy and organic animations.
 */
object ExpressiveSprings {
    val Bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * Adds an expressive scale-down press effect with bouncy spring physics.
 */
fun Modifier.expressiveClickable(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    scaleDownFactor: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDownFactor else 1f,
        animationSpec = ExpressiveSprings.Bouncy,
        label = "ExpressiveScale"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}
