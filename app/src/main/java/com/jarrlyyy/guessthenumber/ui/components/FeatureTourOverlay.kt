package com.jarrlyyy.guessthenumber.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import kotlinx.coroutines.delay

/** Shows a feature-specific first-run tour after the destination has rendered. */
@Composable
fun FeatureTourOverlay(
    route: String?,
    state: GameState,
    onUpdateSettings: (com.jarrlyyy.guessthenumber.domain.model.GameSettings) -> Unit
) {
    val locale = LocalAppLocaleManager.current
    val feature = when (route) {
        "play" -> "play"
        "upgrade" -> "upgrade"
        "shop" -> "shop"
        "more" -> "more"
        "world_map" -> "world_map"
        "relics" -> "relics"
        "codex" -> "codex"
        "endgame" -> "endgame"
        "arcade" -> "arcade"
        "stats" -> "stats"
        "live_ops" -> "live_ops"
        "mutators" -> "mutators"
        else -> null
    } ?: return

    if (feature in state.settings.tutorialSeenFeatures) return

    var visible by remember(feature) { mutableStateOf(false) }
    LaunchedEffect(feature) {
        delay(350)
        visible = true
    }

    if (!visible) return

    fun finish() {
        onUpdateSettings(
            state.settings.copy(
                tutorialSeenFeatures = state.settings.tutorialSeenFeatures + feature
            )
        )
        visible = false
    }

    AlertDialog(
        onDismissRequest = ::finish,
        title = {
            Text(locale.getString("tour_${feature}_title", locale.getString("feature_tour_title", "${feature.replace('_', ' ').replaceFirstChar { it.uppercase() }} tour")))
        },
        text = {
            Text(locale.getString("tour_${feature}_description", "Learn what this feature does and the next useful action to try."))
        },
        confirmButton = {
            Button(onClick = ::finish) {
                Text(locale.getString("tour_done", "Got it"))
            }
        },
        dismissButton = {
            TextButton(onClick = ::finish) {
                Text(locale.getString("tour_skip", "Skip"))
            }
        }
    )
}
