package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

internal data class DesktopWorldDefinition(
    val id: String, val name: String, val bossId: String, val bossName: String,
    val x: Float, val y: Float, val hp: Int, val secretId: String, val secretAt: Long,
    val unlockBossId: String?, val unlockCorrectGuesses: Long, val unlockUpgradeCount: Int,
    val unlockPrestigeCount: Long, val unlockUltraCount: Long, val fightCorrectGuesses: Long,
    val fightUpgradeCount: Int, val fightPrestigeCount: Long, val fightUltraCount: Long,
    val baseMoneyReward: String, val baseNebulaReward: Long
)

internal object DesktopWorldCatalog {
    fun load(): List<DesktopWorldDefinition> {
        val source = DesktopWorldCatalog::class.java.getResourceAsStream("/game/worlds.json")
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Missing bundled world catalog: /game/worlds.json")
        val root = Json.parseToJsonElement(source).jsonObject
        require(root["schemaVersion"]?.jsonPrimitive?.toString() == "1") { "Unsupported world catalog schema." }
        val worlds = root["worlds"]?.jsonArray ?: error("World catalog has no worlds array.")
        val parsed = worlds.map { entry ->
            val value = entry.jsonObject
            fun text(key: String) = value.getValue(key).jsonPrimitive.toString().trim('"')
            fun long(key: String) = text(key).toLong()
            fun int(key: String) = text(key).toInt()
            DesktopWorldDefinition(
                id = text("id"), name = text("fallback"), bossId = text("bossId"), bossName = text("bossFallback"),
                x = text("x").toFloat(), y = text("y").toFloat(), hp = int("hp"), secretId = text("secretId"),
                secretAt = long("secretAt"), unlockBossId = value["unlockBossId"]?.takeUnless { it.toString() == "null" }?.jsonPrimitive?.toString()?.trim('"'),
                unlockCorrectGuesses = long("unlockCorrectGuesses"), unlockUpgradeCount = int("unlockUpgradeCount"),
                unlockPrestigeCount = long("unlockPrestigeCount"), unlockUltraCount = long("unlockUltraCount"),
                fightCorrectGuesses = long("fightCorrectGuesses"), fightUpgradeCount = int("fightUpgradeCount"),
                fightPrestigeCount = long("fightPrestigeCount"), fightUltraCount = long("fightUltraCount"),
                baseMoneyReward = text("baseMoneyReward"), baseNebulaReward = long("baseNebulaReward")
            )
        }
        require(parsed.isNotEmpty() && parsed.map { it.id }.distinct().size == parsed.size) { "World catalog is empty or has duplicate IDs." }
        require(parsed.all { it.id.isNotBlank() && it.name.isNotBlank() && it.bossId.isNotBlank() && it.hp > 0 && it.secretAt >= 0 }) { "World catalog contains invalid entries." }
        return parsed
    }
}

@Composable
internal fun DesktopWorldMapContent(state: GameState, onBack: () -> Unit, onWorldAction: (String, String) -> Unit) {
    val catalogResult = remember { runCatching { DesktopWorldCatalog.load() } }
    val worlds = catalogResult.getOrNull()
    var selectedId by remember(state.activeWorldId) { mutableStateOf(state.activeWorldId) }
    val selected = worlds?.firstOrNull { it.id == selectedId } ?: worlds?.firstOrNull()
    val totalUpgrades = state.upgradeLevels.values.sum()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("World Map", style = MaterialTheme.typography.headlineSmall)
        }
        Text("Select a world to inspect its unlock requirements, boss progress and secret area.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (worlds == null) {
            Text("World catalog could not be loaded: ${catalogResult.exceptionOrNull()?.message ?: "unknown error"}", color = MaterialTheme.colorScheme.error)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                worlds.forEach { world ->
                    val unlocked = world.id in state.unlockedWorldIds
                    OutlinedButton(onClick = { selectedId = world.id }, modifier = Modifier.weight(1f)) {
                        Text((if (world.id == selectedId) "● " else "") + world.name + if (unlocked) " ✓" else " 🔒")
                    }
                }
            }
            worlds.forEach { world ->
                val unlocked = world.id in state.unlockedWorldIds
                val defeated = world.bossId in state.defeatedBossIds && !state.endlessRiftActive
                val damage = state.bossBattleProgress[world.bossId] ?: 0
                val bossHp = world.hp + if (state.endlessRiftActive) ((state.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0
                val canUnlock = (world.unlockBossId == null || world.unlockBossId in state.defeatedBossIds) &&
                    state.correctGuesses >= world.unlockCorrectGuesses && totalUpgrades >= world.unlockUpgradeCount &&
                    state.prestigeCount >= world.unlockPrestigeCount && state.ultraCount >= world.unlockUltraCount
                val canFight = state.endlessRiftActive || (state.correctGuesses >= world.fightCorrectGuesses &&
                    totalUpgrades >= world.fightUpgradeCount && state.prestigeCount >= world.fightPrestigeCount && state.ultraCount >= world.fightUltraCount)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(world.name + if (world.id == state.activeWorldId) " · Active world" else "", style = MaterialTheme.typography.titleLarge)
                        Text("Boss: ${world.bossName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(if (unlocked) "World unlocked" else "World locked", style = MaterialTheme.typography.labelLarge, color = if (unlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!unlocked) {
                            Text("Unlock requires: ${world.unlockCorrectGuesses} correct guesses, ${world.unlockUpgradeCount} upgrade levels, ${world.unlockPrestigeCount} Prestige, ${world.unlockUltraCount} Ultra" + (world.unlockBossId?.let { ", defeat $it" } ?: ""))
                            Button(onClick = { onWorldAction(world.id, "unlock") }, enabled = canUnlock) { Text(if (canUnlock) "Unlock world" else "Requirements not met") }
                        } else {
                            if (world.id != state.activeWorldId) Button(onClick = { onWorldAction(world.id, "select") }, enabled = !state.endlessRiftActive) { Text("Make active world") }
                            if (defeated) {
                                Text("Boss defeated · World mastery ${state.worldMasteryLevels[world.id] ?: 1}")
                            } else {
                                Text("Boss HP: ${(bossHp - damage).coerceAtLeast(0)} / $bossHp")
                                LinearProgressIndicator(progress = { (damage.toFloat() / bossHp.toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                Text("Battle requirements: ${world.fightCorrectGuesses} correct guesses, ${world.fightUpgradeCount} upgrade levels, ${world.fightPrestigeCount} Prestige, ${world.fightUltraCount} Ultra")
                                Text(
                                    if (state.activeBossBattleWorldId == world.id) "Battle active. Return to Play and guess the number to damage the boss."
                                    else if (canFight) "Requirements met. Start the battle to make correct guesses damage this boss."
                                    else "Keep progressing to meet the boss requirements.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { onWorldAction(world.id, "boss") },
                                    enabled = canFight || state.activeBossBattleWorldId == world.id
                                ) {
                                    Text(if (state.activeBossBattleWorldId == world.id) "Leave battle" else "Start boss battle")
                                }
                            }
                            Text("Secret area: ${world.secretId} · appears at ${world.secretAt} correct guesses", style = MaterialTheme.typography.bodySmall)
                            Text(if (world.secretId in state.discoveredSecretIds) "Secret discovered" else if (state.correctGuesses >= world.secretAt) "Secret threshold reached" else "Secret not discovered", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
                        }
                        if (world.id == selected?.id) Text("Selected for inspection", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
