package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class DesktopParityScreenInfo(
    val purpose: String,
    val androidBehavior: String,
    val windowsWork: String
)

private fun parityInfo(destination: DesktopDestination): DesktopParityScreenInfo = when (destination) {
    DesktopDestination.ACHIEVEMENTS -> DesktopParityScreenInfo("Track milestones and rewards earned during play.", "Android shows achievement progress, unlock conditions and reward feedback.", "Canonical catalog, saved unlocks, progress, tier/reward display and filters are connected; visual/runtime parity remains to be verified.")
    DesktopDestination.WORLD_MAP -> DesktopParityScreenInfo("Explore progression areas and boss encounters.", "Android presents world nodes, unlock requirements and boss navigation.", "World catalog, saved unlocks, selection and unlock actions are now connected. Boss battle start/leave actions and the interactive map canvas remain to be ported.")
    DesktopDestination.MUTATORS -> DesktopParityScreenInfo("Configure challenge rules for a run.", "Android includes mutator toggles, a challenge builder and weekly challenges.", "Mutator toggles use the shared game engine; builder activation and challenge rewards update saved state. Localization and runtime parity remain to be verified.")
    DesktopDestination.SAVE_SLOTS -> DesktopParityScreenInfo("Manage local save slots and recover previous saves.", "Android supports ten slots, active-slot switching, duplication and backup restore.", "Windows now lists ten slots, persists the active slot, supports creating and duplicating saves, switching slots and restoring a valid backup. Import/export and destructive slot reset remain pending.")
    DesktopDestination.RELICS -> DesktopParityScreenInfo("Collect relics from defeated world bosses, equip up to four and preview set bonuses.", "GameState stores relic inventory and equipped relic IDs alongside world boss victories.", "Windows relic catalog, boss-gated claiming, equipment persistence and 2-/3-/4-piece set-bonus previews are connected. Passive gameplay effects remain outside this UI batch.")
    DesktopDestination.HOME_BASE -> DesktopParityScreenInfo("Review and upgrade the player home base.", "Android exposes home-base progression and its unlocks.", "Saved sanctuary level, Android cost progression, restored/locked building milestones and the shared upgrade transaction are connected; localization and runtime parity remain to be verified.")
    DesktopDestination.CODEX -> DesktopParityScreenInfo("Browse discovered game content and reference entries.", "Android includes codex entries for progression and discoveries.", "Route is connected. Entry data, discovery state and search remain to be ported.")
    DesktopDestination.ENDGAME -> DesktopParityScreenInfo("Access late-game progression systems.", "Android presents endgame progression and related unlocks.", "Route is connected. Endgame rules and interactions remain to be ported.")
    DesktopDestination.LUCKY_STAKE -> DesktopParityScreenInfo("Open the Lucky Stake minigame.", "Android applies the minigame stake, result and reward rules.", "Route is connected. Stake validation, random outcomes and rewards must reuse authoritative game rules before this screen is considered playable.")
    DesktopDestination.DEVELOPER_SETTINGS -> DesktopParityScreenInfo("Inspect development-only switches and diagnostics.", "Android separates developer settings from player settings.", "Route is connected. Debug-only setting storage and gating remain to be ported.")
    DesktopDestination.DEVELOPER_CONSOLE -> DesktopParityScreenInfo("Run supported development commands.", "Android validates commands and displays their results.", "Route is connected. The shared command executor and output history remain to be wired.")
    DesktopDestination.PROCESS_INSPECTOR -> DesktopParityScreenInfo("Inspect runtime and process diagnostics.", "Android exposes diagnostic information for development.", "Route is connected. JVM diagnostics, refresh behavior and privacy-conscious output remain to be ported.")
    DesktopDestination.MUSIC_PLAYER -> DesktopParityScreenInfo("Choose and control background music.", "Android supports selected music, playback, seeking and track metadata.", "Route is connected. Windows file selection and playback controls remain to be ported.")
    DesktopDestination.ARCADE -> DesktopParityScreenInfo("Play optional minigames and earn their rewards.", "Android lists minigames and routes rewards through game rules.", "Route is connected. Minigame content and reward callbacks remain to be ported.")
    DesktopDestination.ACTIVITY_HUB -> DesktopParityScreenInfo("Open temporary and live activity experiences.", "Android exposes eligible activities and respects feature flags.", "Route is connected. Activity discovery, payload validation and feature-flag handling remain to be ported.")
    DesktopDestination.LUA_TESTING_HUB -> DesktopParityScreenInfo("Inspect development-only Lua activity scripts.", "Android offers a sandboxed testing hub for approved scripts.", "Route is connected. The JVM port must reuse sandbox limits and payload validation before executing any script.")
    DesktopDestination.CHANGELOG -> DesktopParityScreenInfo("Review recent changes and version history.", "Android presents changelog content with the installed version.", "Scrollable Markdown viewer loads the canonical Android changelog asset; installed-version labeling and exact Android visual parity remain to be verified.")
    DesktopDestination.LIVE_OPS -> DesktopParityScreenInfo("View eligible live events and claim available rewards.", "Android gates live events through feature flags and validates claims.", "Route is connected. Event refresh and reward claims must reuse the shared policy and feature flags.")
    else -> DesktopParityScreenInfo("Windows feature route.", "Android is the source of truth for screen content and behavior.", "This route is not a completed parity implementation.")
}

@Composable
internal fun DesktopParityScreen(
    destination: DesktopDestination,
    onBack: () -> Unit,
    onNavigate: (DesktopDestination) -> Unit
) {
    val info = parityInfo(destination)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            OutlinedButton(onClick = { onNavigate(DesktopDestination.SETTINGS) }) { Text("Settings") }
        }
        Text(destination.label, style = MaterialTheme.typography.headlineMedium)
        Text(info.purpose, style = MaterialTheme.typography.bodyLarge)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Android reference behavior", style = MaterialTheme.typography.titleMedium)
                Text(info.androidBehavior)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Windows port status", style = MaterialTheme.typography.titleMedium)
                Text(info.windowsWork, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("This is a navigation and parity-tracking surface, not a claim that this feature is complete. The next implementation pass must connect real data and actions, then verify appearance and behavior against Android.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { onNavigate(DesktopDestination.PLAY) }) { Text("Return to Play") }
    }
}
