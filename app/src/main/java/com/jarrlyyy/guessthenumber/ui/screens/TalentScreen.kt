package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.PrestigeBlue

data class TalentNode(
    val id: String,
    val name: String,
    val description: String,
    val cost: Long,
    val requiredParentId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalentScreen(
    state: GameState,
    onBuyTalent: (String, Long) -> Unit,
    onBack: () -> Unit
) {
    val talents = listOf(
        TalentNode("talent_speed_1", "Velocity Boost", "Increases Auto-Clicker speed by +25%", 10),
        TalentNode("talent_crit_1", "Critical Precision", "Increases Critical Guess chance by +5%", 25, "talent_speed_1"),
        TalentNode("talent_reward_1", "Nebula Resonance", "Boosts all Money earnings by +50%", 50, "talent_crit_1"),
        TalentNode("talent_master_1", "Omniscient Oracle", "Guarantees next guess hints", 100, "talent_reward_1")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prestige Talent Web") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Prestige Balance: ${state.prestige.format()}", color = PrestigeBlue, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Unlock interconnected talent nodes using Prestige to acquire permanent gameplay buffs.", fontSize = 13.sp)
                    }
                }
            }

            items(talents) { talent ->
                val unlocked = state.prestigeShopPurchases.contains(talent.id)
                val parentUnlocked = talent.requiredParentId == null || state.prestigeShopPurchases.contains(talent.requiredParentId)
                val canAfford = state.prestige >= BigNumber(talent.cost)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (unlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(talent.name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                            Text(talent.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cost: ${talent.cost} Prestige", color = PrestigeBlue, fontSize = 14.sp)
                            if (talent.requiredParentId != null && !parentUnlocked) {
                                Text("Locked: Requires parent talent", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Button(
                            onClick = { onBuyTalent(talent.id, talent.cost) },
                            enabled = !unlocked && parentUnlocked && canAfford,
                            colors = ButtonDefaults.buttonColors(containerColor = PrestigeBlue)
                        ) {
                            Text(if (unlocked) "Unlocked" else "Unlock")
                        }
                    }
                }
            }
        }
    }
}
