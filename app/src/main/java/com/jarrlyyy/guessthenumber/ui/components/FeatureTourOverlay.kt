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

    val featureTitle = when (feature) {
        "play" -> locale.getString("play_title", locale.getString("nav_play", "Play"))
        "upgrade" -> locale.getString("nav_upgrade", "Upgrades")
        "shop" -> locale.getString("shop_title", "Nebula Shop")
        "more" -> locale.getString("more_title_hub", locale.getString("nav_more", "More"))
        "world_map" -> locale.getString("world_map_title", "World Map")
        "relics" -> locale.getString("relics_title", "Relic Collection")
        "codex" -> locale.getString("codex_title", "Explorer's Codex")
        "endgame" -> locale.getString("endgame_title", "Endgame Challenges")
        "arcade" -> locale.getString("arcade_title", "Arcade Minigames")
        "stats" -> locale.getString("stats_title", "Statistics")
        "live_ops" -> locale.getString("live_ops_screen_title", "Seasonal Events")
        "mutators" -> locale.getString("mutators_title", "Mutators & Weekly Challenges")
        else -> feature.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    val featureDescription = when (feature) {
        "more" -> locale.getString("more_shortcuts_description", "Your shortcuts, your order.")
        "world_map" -> locale.getString("world_map_hint", "Pinch to zoom, drag to explore, and tap a world to inspect it.")
        "relics" -> locale.getString("relics_hint", "Defeat world bosses to discover relics. Equip up to four for stronger set bonuses.")
        "endgame" -> locale.getString("endgame_description", "Complete long-term goals to prove your mastery of every system.")
        "live_ops" -> locale.getString("participate_live_ops_desc", "Participate in limited-time events to claim rewards and enhance progression.")
        "mutators" -> locale.getString("challenge_builder_desc", "Combine mutators and build a custom goal.")
        else -> locale.getString("more_feature_discovery_step_1", "Explore this feature's tools and progress, then choose what to try first.")
    }

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
        title = { Text(locale.getString("tour_title_format", "%s • Quick tour", featureTitle)) },
        text = { Text(featureDescription) },
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
