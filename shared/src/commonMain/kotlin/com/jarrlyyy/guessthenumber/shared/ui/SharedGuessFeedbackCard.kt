package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameStatus

/**
 * Shared status-driven feedback container for host-provided, localized feedback.
 *
 * Status-to-color behavior and message ownership remain unchanged. The visual
 * container now uses the same rounded, bordered card primitive as Jarrlyy UI.
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

    JarrlyyCard(
        modifier = modifier.fillMaxWidth().then(
            if (reducedMotion) Modifier else Modifier.animateContentSize()
        ),
        containerColor = containerColor,
        borderColor = contentColor.copy(alpha = 0.22f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        tonalElevation = if (reducedMotion) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(0.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            leadingContent(contentColor)
            androidx.compose.material3.Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
