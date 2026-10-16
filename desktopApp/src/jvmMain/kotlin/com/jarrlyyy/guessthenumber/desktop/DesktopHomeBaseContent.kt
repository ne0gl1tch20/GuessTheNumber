package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState

/** Windows presentation of Android's Home Base progression, backed by the active saved GameState. */
@Composable
internal fun DesktopHomeBaseContent(
    state: GameState,
    onBack: () -> Unit,
    onUpgrade: () -> Unit
) {
    val level = state.homeBaseLevel.coerceIn(0, 20)
    val cost = BigNumber(50_000L * (level + 1L))
    val progress = level / 20f
    val buildings = listOf(
        "Portal Chamber" to 1,
        "Relic Forge" to 3,
        "Star Archive" to 5,
        "Cosmic Observatory" to 10
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Home Base", style = MaterialTheme.typography.headlineSmall)
        }

        Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.padding(4.dp))
                Text("Sanctuary Level $level", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Restore your cosmic sanctuary as your journey grows.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text("Sanctuary buildings", style = MaterialTheme.typography.titleLarge)
        buildings.forEach { (name, requiredLevel) ->
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (level >= requiredLevel) "Restored" else "Unlocks at level $requiredLevel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (level >= requiredLevel) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (level >= requiredLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Button(
            onClick = onUpgrade,
            enabled = state.money >= cost && level < 20,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (level >= 20) "Sanctuary fully restored" else "Restore next level • ${cost.format()} Money")
        }
        Text(
            "Each level adds 2% Money to correct-guess rewards, up to 40%.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary
        )
        Text(
            "Each restoration costs more than the previous one. Your level and building milestones are saved with this slot.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
