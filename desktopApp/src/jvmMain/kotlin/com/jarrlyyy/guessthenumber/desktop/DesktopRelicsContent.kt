package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class DesktopRelicDefinition(
    val id: String,
    val name: String,
    val description: String,
    val setId: String,
    val setName: String,
    val rarity: String,
    val unlockBossId: String,
    val unlockBossName: String,
    val twoPieceBonus: String,
    val threePieceBonus: String,
    val fourPieceBonus: String
)

internal object DesktopRelicsCatalog {
    fun load(): List<DesktopRelicDefinition> {
        val source = DesktopRelicsCatalog::class.java.getResourceAsStream("/game/relics.json")
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Missing bundled relic catalog: /game/relics.json")
        return parse(source)
    }

    internal fun parse(source: String): List<DesktopRelicDefinition> {
        val root = Json.parseToJsonElement(source).jsonObject
        require(root["schemaVersion"]?.jsonPrimitive?.toString()?.trim('"') == "1") { "Unsupported relic catalog schema." }
        val entries = root["relics"]?.jsonArray ?: error("Relic catalog has no relics array.")
        val parsed = entries.map { entry ->
            val value = entry.jsonObject
            fun text(key: String) = value.getValue(key).jsonPrimitive.toString().trim('"')
            DesktopRelicDefinition(
                id = text("id"), name = text("name"), description = text("description"),
                setId = text("setId"), setName = text("setName"), rarity = text("rarity"),
                unlockBossId = text("unlockBossId"), unlockBossName = text("unlockBossName"),
                twoPieceBonus = text("twoPieceBonus"), threePieceBonus = text("threePieceBonus"),
                fourPieceBonus = text("fourPieceBonus")
            )
        }
        require(parsed.isNotEmpty()) { "Relic catalog is empty." }
        require(parsed.map { it.id }.distinct().size == parsed.size) { "Relic catalog contains duplicate IDs." }
        require(parsed.all { relic ->
            listOf(relic.id, relic.name, relic.description, relic.setId, relic.setName, relic.rarity,
                relic.unlockBossId, relic.unlockBossName, relic.twoPieceBonus, relic.threePieceBonus,
                relic.fourPieceBonus).all(String::isNotBlank)
        }) { "Relic catalog contains invalid entries." }
        require(parsed.groupBy { it.setId }.values.all { set ->
            set.map { it.setName }.distinct().size == 1 &&
                set.map { it.twoPieceBonus }.distinct().size == 1 &&
                set.map { it.threePieceBonus }.distinct().size == 1 &&
                set.map { it.fourPieceBonus }.distinct().size == 1
        }) { "Relic set metadata must be consistent for each set." }
        return parsed
    }
}

internal object DesktopRelicRules {
    const val MAX_EQUIPPED_RELICS = 4

    fun equip(state: GameState, relicId: String): GameState? {
        if (relicId !in state.relicInventory || relicId in state.equippedRelicIds) return null
        if (state.equippedRelicIds.size >= MAX_EQUIPPED_RELICS) return null
        return state.copy(equippedRelicIds = state.equippedRelicIds + relicId)
    }

    fun activeSetBonuses(catalog: List<DesktopRelicDefinition>, equippedIds: Set<String>): List<String> {
        val byId = catalog.associateBy { it.id }
        return equippedIds.mapNotNull(byId::get)
            .groupBy { it.setId }
            .values
            .mapNotNull { members ->
                val first = members.first()
                when (members.size) {
                    4 -> first.setName + " (4-piece): " + first.fourPieceBonus
                    3 -> first.setName + " (3-piece): " + first.threePieceBonus
                    2 -> first.setName + " (2-piece): " + first.twoPieceBonus
                    else -> null
                }
            }
            .sorted()
    }
}

@Composable
internal fun DesktopRelicsContent(
    state: GameState,
    onBack: () -> Unit,
    onRelicAction: (relicId: String, action: String) -> Unit
) {
    val catalogResult = remember { runCatching { DesktopRelicsCatalog.load() } }
    val catalog = catalogResult.getOrNull()
    var filter by remember { mutableStateOf("all") }
    val owned = catalog?.count { it.id in state.relicInventory } ?: 0
    val equipped = catalog?.count { it.id in state.equippedRelicIds } ?: 0
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Relics", style = MaterialTheme.typography.headlineSmall)
        }
        if (catalog == null) {
            Text("Relics could not be loaded: " + (catalogResult.exceptionOrNull()?.message ?: "unknown error"), color = MaterialTheme.colorScheme.error)
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Collection", style = MaterialTheme.typography.titleMedium)
                    Text("Owned: " + owned + " / " + catalog.size + " · Equipped: " + equipped + " / " + DesktopRelicRules.MAX_EQUIPPED_RELICS, style = MaterialTheme.typography.titleLarge)
                    LinearProgressIndicator(
                        progress = { if (catalog.isEmpty()) 0f else owned.toFloat() / catalog.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Defeat world bosses to claim their relics. Equip up to four for stronger set bonuses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val activeBonuses = DesktopRelicRules.activeSetBonuses(catalog, state.equippedRelicIds)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Active set bonuses", style = MaterialTheme.typography.titleMedium)
                    if (activeBonuses.isEmpty()) {
                        Text("Equip two or more boss relics to activate a set bonus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else activeBonuses.forEach { bonus -> Text(bonus) }
                    Text("These bonuses are applied by the shared Windows reward calculation after each correct guess.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "owned" to "Owned", "equipped" to "Equipped", "locked" to "Unclaimed").forEach { (id, label) ->
                    FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(label) })
                }
            }
            val visible = catalog.filter { relic ->
                when (filter) {
                    "owned" -> relic.id in state.relicInventory
                    "equipped" -> relic.id in state.equippedRelicIds
                    "locked" -> relic.id !in state.relicInventory
                    else -> true
                }
            }
            visible.forEach { relic ->
                val isOwned = relic.id in state.relicInventory
                val isEquipped = relic.id in state.equippedRelicIds
                val bossDefeated = relic.unlockBossId in state.defeatedBossIds
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(relic.name, style = MaterialTheme.typography.titleMedium)
                            Text(if (isEquipped) "Equipped" else if (isOwned) "Owned" else relic.rarity, color = if (isEquipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                        }
                        Text(relic.description)
                        Text("Set: " + relic.setName + " · " + relic.rarity, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!isOwned) {
                            Text(if (bossDefeated) "Unlocked by defeating " + relic.unlockBossName + "." else "Defeat " + relic.unlockBossName + " to unlock.", style = MaterialTheme.typography.bodySmall)
                            if (bossDefeated) Button(onClick = { onRelicAction(relic.id, "claim") }) { Text("Claim relic") }
                        } else if (isEquipped) {
                            OutlinedButton(onClick = { onRelicAction(relic.id, "unequip") }) { Text("Unequip") }
                        } else {
                            Button(
                                onClick = { onRelicAction(relic.id, "equip") },
                                enabled = state.equippedRelicIds.size < DesktopRelicRules.MAX_EQUIPPED_RELICS
                            ) { Text("Equip") }
                        }
                    }
                }
            }
        }
    }
}
