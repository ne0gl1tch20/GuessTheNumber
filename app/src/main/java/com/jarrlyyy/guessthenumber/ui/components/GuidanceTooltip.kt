package com.jarrlyyy.guessthenumber.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.jarrlyyy.guessthenumber.ui.theme.GuessTheNumberTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

/**
 * Adds hover/long-press guidance to a normal, keyboard-focusable button.
 *
 * The button's content description remains available to TalkBack even when the
 * tooltip is not visible. The action itself remains a normal click action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidanceIconButton(
    guidance: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above
        ),
        tooltip = { PlainTooltip { Text(guidance) } },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Small, dependency-free preview fixtures for reviewing common screen states.
 * Keep these fixtures representative of real UI states, not runtime data.
 */
@Composable
private fun PreviewStateTile(
    title: String,
    detail: String,
    icon: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PreviewStateGallery(
    heading: String,
    stateTitle: String,
    detail: String,
    icon: String,
    themeMode: String = "System"
) {
    GuessTheNumberTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(heading, style = MaterialTheme.typography.headlineSmall)
                PreviewStateTile(stateTitle, detail, icon)
            }
        }
    }
}

@Preview(name = "Loading state", showBackground = true)
@Composable
private fun LoadingStatePreview() {
    PreviewStateGallery(
        heading = "Loading preview",
        stateTitle = "Loading save data",
        detail = "Progress feedback is visible while saved progress is being loaded.",
        icon = "⏳"
    )
}

@Preview(name = "Empty state", showBackground = true)
@Composable
private fun EmptyStatePreview() {
    PreviewStateGallery(
        heading = "Empty preview",
        stateTitle = "Nothing here yet",
        detail = "Explain why this area is empty and provide a clear next action.",
        icon = "📭"
    )
}

@Preview(name = "Error state", showBackground = true)
@Composable
private fun ErrorStatePreview() {
    PreviewStateGallery(
        heading = "Error preview",
        stateTitle = "Couldn't load this section",
        detail = "Keep the explanation readable and offer retry when retry is possible.",
        icon = "⚠️"
    )
}

@Preview(name = "Theme state", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeStatePreview() {
    PreviewStateGallery(
        heading = "Theme preview",
        stateTitle = "Night mode",
        detail = "Check text and surfaces against the active theme colors.",
        icon = "🌙",
        themeMode = "Dark"
    )
}

@Preview(name = "Light theme state", showBackground = true)
@Composable
private fun LightThemeStatePreview() {
    PreviewStateGallery(
        heading = "Theme preview",
        stateTitle = "Light mode",
        detail = "Check text and surfaces against the active theme colors.",
        icon = "☀️",
        themeMode = "Light"
    )
}

@Preview(name = "Localized state", showBackground = true)
@Composable
private fun LocalizedStatePreview() {
    PreviewStateGallery(
        heading = "Localized preview",
        stateTitle = "Handa ka na bang maglaro?",
        detail = "Tingnan kung nababasa ang mas mahahabang salin at hindi napuputol ang teksto.",
        icon = "🌐"
    )
}
