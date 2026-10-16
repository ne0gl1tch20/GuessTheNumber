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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.GameState

private data class DesktopCodexEntry(val id: String, val title: String)

private val desktopCodexEntries = listOf(
    DesktopCodexEntry("world:verdant_grove", "Verdant Grove"),
    DesktopCodexEntry("boss:verdant_guardian", "Verdant Guardian"),
    DesktopCodexEntry("world:crystal_caverns", "Crystal Caverns"),
    DesktopCodexEntry("boss:crystal_golem", "Crystal Golem"),
    DesktopCodexEntry("world:ember_summit", "Ember Summit"),
    DesktopCodexEntry("boss:ember_dragon", "Ember Dragon"),
    DesktopCodexEntry("world:nebula_rift", "Nebula Rift"),
    DesktopCodexEntry("boss:nebula_titan", "Nebula Titan"),
    DesktopCodexEntry("secret:whispering_hollow", "Whispering Hollow"),
    DesktopCodexEntry("secret:shard_archive", "Shard Archive"),
    DesktopCodexEntry("secret:ashen_vault", "Ashen Vault"),
    DesktopCodexEntry("secret:lost_observatory", "Lost Observatory")
)

@Composable
internal fun DesktopCodexContent(
    state: GameState,
    onBack: () -> Unit,
    onClaimCompletionReward: () -> Unit
) {
    val discovered = desktopCodexEntries.count { it.id in state.codexEntries }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Explorer's Codex", style = MaterialTheme.typography.headlineSmall)
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$discovered / ${desktopCodexEntries.size} entries discovered", style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(
                    progress = { discovered.toFloat() / desktopCodexEntries.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Explore the worlds, defeat their bosses and discover hidden areas to complete the Codex.")
                Button(
                    onClick = onClaimCompletionReward,
                    enabled = discovered == desktopCodexEntries.size && !state.codexRewardClaimed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.codexRewardClaimed) "Completion reward claimed" else "Claim 100 Nebula completion reward")
                }
            }
        }
        desktopCodexEntries.forEach { entry ->
            val found = entry.id in state.codexEntries
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (found) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (found) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(entry.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (found) "Entry discovered" else "Undiscovered · explore worlds and defeat bosses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
