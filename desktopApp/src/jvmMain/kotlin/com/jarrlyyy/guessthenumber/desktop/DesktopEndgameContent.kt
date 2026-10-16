package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.GameState

private data class DesktopEndgameGoal(val id: String, val title: String, val complete: Boolean)

@Composable
internal fun DesktopEndgameContent(
    state: GameState,
    onBack: () -> Unit,
    onOpenWorldMap: () -> Unit
) {
    val allBosses = setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")
    val allSecrets = setOf("whispering_hollow", "shard_archive", "ashen_vault", "lost_observatory")
    val goals = listOf(
        DesktopEndgameGoal("bosses", "Defeat all four world bosses", state.defeatedBossIds.containsAll(allBosses)),
        DesktopEndgameGoal("secrets", "Discover all four secret areas", state.discoveredSecretIds.containsAll(allSecrets)),
        DesktopEndgameGoal("relics", "Collect all four boss relics", state.relicInventory.containsAll(allBosses)),
        DesktopEndgameGoal("sanctuary", "Restore the sanctuary to level 10", state.homeBaseLevel >= 10),
        DesktopEndgameGoal("rift", "Clear Endless Rift tier 10", state.endlessRiftBestTier >= 10),
        DesktopEndgameGoal(
            "mastery",
            "Reach mastery level 5 in every world",
            listOf("verdant_grove", "crystal_caverns", "ember_summit", "nebula_rift")
                .all { (state.worldMasteryLevels[it] ?: 0) >= 5 }
        )
    )
    val completed = goals.count { it.complete }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Endgame Challenges", style = MaterialTheme.typography.headlineSmall)
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Text("Beyond the final portal", style = MaterialTheme.typography.titleLarge)
                Text("Complete long-term goals to prove your mastery of every system.")
                Text("$completed / ${goals.size} challenges completed", style = MaterialTheme.typography.titleMedium)
            }
        }
        goals.forEach { goal ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (goal.complete) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (goal.complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(goal.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (goal.complete) "Completed" else "In progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Button(onClick = onOpenWorldMap, modifier = Modifier.fillMaxWidth()) {
            Text("Open World Map")
        }
    }
}
