package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager

private data class RelicInfo(val id: String, val titleKey: String, val title: String, val descKey: String, val description: String)
private val relics = listOf(
    RelicInfo("verdant_guardian", "relic_verdant_name", "Heartwood Sigil", "relic_verdant_desc", "A living charm from the Verdant Guardian."),
    RelicInfo("crystal_golem", "relic_crystal_name", "Prism Core", "relic_crystal_desc", "A crystal lens that remembers every guess."),
    RelicInfo("ember_dragon", "relic_ember_name", "Ember Scale", "relic_ember_desc", "A warm scale from the Ember Dragon."),
    RelicInfo("nebula_titan", "relic_nebula_name", "Starbound Fragment", "relic_nebula_desc", "A fragment of the Nebula Titan's armor.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelicsScreen(state: GameState, onAction: (String, String) -> Unit, onBack: () -> Unit) {
    val locale = LocalAppLocaleManager.current
    Scaffold(topBar = { TopAppBar(title = { Text(locale.getString("relics_title", "Relic Collection")) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = locale.getString("back", "Back")) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(38.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(locale.getString("relics_title", "Relic Collection"), style = MaterialTheme.typography.titleLarge)
                            Text(locale.getString("relic_equipped_count", "%d / 2 equipped", state.equippedRelicIds.size), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(locale.getString("relics_hint", "Defeat world bosses to discover relics. Equip up to two to customize your collection."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(relics, key = { it.id }) { relic ->
                val owned = relic.id in state.relicInventory
                val equipped = relic.id in state.equippedRelicIds
                Card {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (owned) Icons.Default.Diamond else Icons.Default.Lock, contentDescription = null, tint = if (owned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(locale.getString(relic.titleKey, relic.title), style = MaterialTheme.typography.titleMedium)
                            Text(locale.getString(relic.descKey, relic.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (owned) locale.getString("relic_owned", "Discovered") else locale.getString("relic_boss_locked", "Defeat its world boss to discover"), style = MaterialTheme.typography.labelSmall)
                        }
                        Button(onClick = { onAction("equip_relic", relic.id) }, enabled = owned || equipped) {
                            Text(if (equipped) locale.getString("relic_equipped", "Equipped") else locale.getString("relic_equip", "Equip"))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBaseScreen(state: GameState, onAction: (String, String) -> Unit, onBack: () -> Unit) {
    val locale = LocalAppLocaleManager.current
    val cost = BigNumber(50_000L * (state.homeBaseLevel + 1L))
    val buildings = listOf(
        locale.getString("base_building_portal", "Portal Chamber") to (state.homeBaseLevel >= 1),
        locale.getString("base_building_forge", "Relic Forge") to (state.homeBaseLevel >= 3),
        locale.getString("base_building_archive", "Star Archive") to (state.homeBaseLevel >= 5),
        locale.getString("base_building_observatory", "Cosmic Observatory") to (state.homeBaseLevel >= 10)
    )
    Scaffold(topBar = { TopAppBar(title = { Text(locale.getString("home_base_title", "Home Base")) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = locale.getString("back", "Back")) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(56.dp))
                    Text(locale.getString("home_base_level", "Sanctuary Level %d", state.homeBaseLevel), style = MaterialTheme.typography.headlineSmall)
                    Text(locale.getString("home_base_description", "Restore your cosmic sanctuary as your journey grows."), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(locale.getString("home_base_buildings", "Sanctuary buildings"), style = MaterialTheme.typography.titleLarge)
            buildings.forEach { (name, built) ->
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(if (built) locale.getString("base_building_restored", "Restored") else locale.getString("base_building_locked", "Unlock through sanctuary upgrades")) },
                    leadingContent = { Icon(if (built) Icons.Default.CheckCircle else Icons.Default.Lock, contentDescription = null) }
                )
            }
            Button(onClick = { onAction("home_upgrade", "sanctuary") }, enabled = state.money >= cost && state.homeBaseLevel < 20, modifier = Modifier.fillMaxWidth()) {
                Text(locale.getString("home_base_upgrade", "Restore next level • %s Money", cost.format()))
            }
            Text(locale.getString("home_base_cost_hint", "Each restoration costs more than the previous one. Your level and building milestones are saved with this slot."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodexScreen(state: GameState, onAction: (String, String) -> Unit, onBack: () -> Unit) {
    val locale = LocalAppLocaleManager.current
    val entries = listOf(
        Triple("world:verdant_grove", "codex_verdant_title", "Verdant Grove"),
        Triple("boss:verdant_guardian", "codex_guardian_title", "Verdant Guardian"),
        Triple("world:crystal_caverns", "codex_crystal_title", "Crystal Caverns"),
        Triple("boss:crystal_golem", "codex_golem_title", "Crystal Golem"),
        Triple("world:ember_summit", "codex_ember_title", "Ember Summit"),
        Triple("boss:ember_dragon", "codex_dragon_title", "Ember Dragon"),
        Triple("world:nebula_rift", "codex_nebula_title", "Nebula Rift"),
        Triple("boss:nebula_titan", "codex_titan_title", "Nebula Titan"),
        Triple("secret:whispering_hollow", "codex_secret_hollow", "Whispering Hollow"),
        Triple("secret:shard_archive", "codex_secret_shards", "Shard Archive"),
        Triple("secret:ashen_vault", "codex_secret_ashen", "Ashen Vault"),
        Triple("secret:lost_observatory", "codex_secret_observatory", "Lost Observatory")
    )
    Scaffold(topBar = { TopAppBar(title = { Text(locale.getString("codex_title", "Explorer's Codex")) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = locale.getString("back", "Back")) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(locale.getString("codex_progress", "%d / %d entries discovered", state.codexEntries.size, entries.size), style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress = { (state.codexEntries.size.toFloat() / entries.size).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Button(onClick = { onAction("claim_codex", "complete") }, enabled = state.codexEntries.size >= entries.size && !state.codexRewardClaimed, modifier = Modifier.fillMaxWidth()) {
                    Text(locale.getString(if (state.codexRewardClaimed) "codex_reward_claimed" else "codex_claim_reward", if (state.codexRewardClaimed) "Completion reward claimed" else "Claim 100 Nebula completion reward"))
                }
            }
            items(entries, key = { it.first }) { entry ->
                val found = entry.first in state.codexEntries
                Card {
                    ListItem(
                        leadingContent = { Icon(if (found) Icons.Default.MenuBook else Icons.Default.Lock, contentDescription = null, tint = if (found) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) },
                        headlineContent = { Text(locale.getString(entry.second, entry.third)) },
                        supportingContent = { Text(if (found) locale.getString("codex_discovered", "Entry discovered") else locale.getString("codex_unknown", "Undiscovered • explore worlds and defeat bosses")) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndgameScreen(state: GameState, onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val locale = LocalAppLocaleManager.current
    val bossesDefeated = state.defeatedBossIds.size.coerceAtMost(4)
    val goals = listOf(
        locale.getString("endgame_goal_bosses", "Defeat all four world bosses") to (bossesDefeated == 4),
        locale.getString("endgame_goal_secrets", "Discover all four secret areas") to (state.discoveredSecretIds.size >= 4),
        locale.getString("endgame_goal_relics", "Collect all four boss relics") to (state.relicInventory.size >= 4),
        locale.getString("endgame_goal_base", "Restore the sanctuary to level 10") to (state.homeBaseLevel >= 10)
    )
    Scaffold(topBar = { TopAppBar(title = { Text(locale.getString("endgame_title", "Endgame Challenges")) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = locale.getString("back", "Back")) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(42.dp))
                    Text(locale.getString("endgame_subtitle", "Beyond the final portal"), style = MaterialTheme.typography.headlineSmall)
                    Text(locale.getString("endgame_description", "Complete long-term goals to prove your mastery of every system."), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(locale.getString("endgame_goals", "Legacy goals"), style = MaterialTheme.typography.titleLarge)
            goals.forEach { (title, done) ->
                ListItem(
                    leadingContent = { Icon(if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) },
                    headlineContent = { Text(title) },
                    supportingContent = { Text(if (done) locale.getString("endgame_complete", "Complete") else locale.getString("endgame_incomplete", "In progress")) }
                )
            }
            Button(onClick = { onNavigate("world_map") }, modifier = Modifier.fillMaxWidth()) { Text(locale.getString("world_map_open", "Explore World Map")) }
            OutlinedButton(onClick = { onNavigate("relics") }, modifier = Modifier.fillMaxWidth()) { Text(locale.getString("relics_open", "Open Relic Collection")) }
        }
    }
}
