package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameStatus

/**
 * Shared status-driven feedback container for host-provided, localized feedback.
 *
 * The host owns feedback wording and platform-specific icon assets. This component
 * owns the common status-to-color mapping and feedback-card layout.
 */
@Composable
fun SharedGuessFeedbackCard(
    status: GuessGameStatus,
    message: String,
    leadingContent: @Composable (Color) -> Unit,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = when (status) {
        GuessGameStatus.READY,
        GuessGameStatus.INVALID_INPUT -> MaterialTheme.colorScheme.surfaceVariant
        GuessGameStatus.TOO_LOW -> MaterialTheme.colorScheme.secondaryContainer
        GuessGameStatus.TOO_HIGH -> MaterialTheme.colorScheme.primaryContainer
        GuessGameStatus.CORRECT -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (status) {
        GuessGameStatus.READY,
        GuessGameStatus.INVALID_INPUT -> MaterialTheme.colorScheme.onSurfaceVariant
        GuessGameStatus.TOO_LOW -> MaterialTheme.colorScheme.onSecondaryContainer
        GuessGameStatus.TOO_HIGH -> MaterialTheme.colorScheme.onPrimaryContainer
        GuessGameStatus.CORRECT -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = if (reducedMotion) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            leadingContent(contentColor)
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
