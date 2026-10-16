package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

internal data class DesktopAchievementDefinition(
    val id: String,
    val name: String,
    val description: String,
    val rewardNebula: Long,
    val tier: String
)

internal object DesktopAchievementsCatalog {
    fun load(): List<DesktopAchievementDefinition> {
        val source = DesktopAchievementsCatalog::class.java.getResourceAsStream("/game/achievements.json")
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Missing bundled achievement catalog: /game/achievements.json")
        return parse(source)
    }

    internal fun parse(source: String): List<DesktopAchievementDefinition> {
        val root = Json.parseToJsonElement(source).jsonObject
        require(root["schemaVersion"]?.jsonPrimitive?.toString()?.trim('"') == "1") { "Unsupported achievement catalog schema." }
        val entries = root["achievements"]?.jsonArray ?: error("Achievement catalog has no achievements array.")
        val parsed = entries.map { item ->
            val value = item.jsonObject
            DesktopAchievementDefinition(
                id = value.getValue("id").jsonPrimitive.toString().trim('"'),
                name = value.getValue("name").jsonPrimitive.toString().trim('"'),
                description = value.getValue("description").jsonPrimitive.toString().trim('"'),
                rewardNebula = value.getValue("rewardNebula").jsonPrimitive.toString().toLong(),
                tier = value.getValue("tier").jsonPrimitive.toString().trim('"').lowercase()
            )
        }
        require(parsed.isNotEmpty()) { "Achievement catalog is empty." }
        require(parsed.map { it.id }.distinct().size == parsed.size) { "Achievement catalog contains duplicate IDs." }
        require(parsed.all { it.id.isNotBlank() && it.name.isNotBlank() && it.description.isNotBlank() && it.rewardNebula >= 0 }) { "Achievement catalog contains invalid entries." }
        return parsed
    }
}

@Composable
internal fun DesktopAchievementsContent(state: GameState, onBack: () -> Unit) {
    val catalogResult = remember { runCatching { DesktopAchievementsCatalog.load() } }
    val catalog = catalogResult.getOrNull()
    var filter by remember { mutableStateOf("all") }
    val unlockedCount = catalog?.count { it.id in state.achievements } ?: 0
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Achievements & Perks", style = MaterialTheme.typography.headlineSmall)
        }
        if (catalog == null) {
            Text("Achievements could not be loaded: ${catalogResult.exceptionOrNull()?.message ?: "unknown error"}", color = MaterialTheme.colorScheme.error)
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Achievement Progress", style = MaterialTheme.typography.titleMedium)
                    Text("Unlocked: $unlockedCount / ${catalog.size}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(
                        progress = { if (catalog.isEmpty()) 0f else unlockedCount.toFloat() / catalog.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Unlocked achievements grant their Nebula rewards through the game engine. This screen only displays the saved unlock state.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "unlocked" to "Unlocked", "locked" to "Locked").forEach { (id, label) ->
                    FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(label) })
                }
            }
            val visible = catalog.filter { achievement ->
                when (filter) {
                    "unlocked" -> achievement.id in state.achievements
                    "locked" -> achievement.id !in state.achievements
                    else -> true
                }
            }
            Text("${visible.size} achievements", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                visible.forEach { achievement ->
                    val unlocked = achievement.id in state.achievements
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(achievement.name, style = MaterialTheme.typography.titleMedium)
                                Text(if (unlocked) "Unlocked" else "Locked", color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                            }
                            Text(achievement.description, style = MaterialTheme.typography.bodyMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tier: ${achievement.tier.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Reward: ${achievement.rewardNebula} Nebula", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}
