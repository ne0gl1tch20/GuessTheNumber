package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold

data class MutatorDef(
    val id: String,
    val name: String,
    val description: String,
    val rewardMultiplier: Double
)

data class SeededChallengeDef(
    val seed: Long,
    val name: String,
    val description: String,
    val rewardNebula: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutatorsScreen(
    state: GameState,
    onSelectMutator: (String) -> Unit,
    onCompleteChallenge: (String, Long) -> Unit,
    onBack: () -> Unit
) {
    val mutators = listOf(
        MutatorDef("mut_hardcore", "Hardcore Range", "Guessing range is doubled. Rewards $\\times 2$.", 2.0),
        MutatorDef("mut_speed", "Hyper Speed", "Auto-clicker speed $\\times 3$, but earnings halved.", 1.5),
        MutatorDef("mut_blind", "Blindfolded Oracle", "No lower/higher feedback indicators. Rewards $\\times 5$.", 5.0)
    )

    val challenges = listOf(
        SeededChallengeDef(20251001L, "Daily Seed #1", "Guess 25 numbers correctly under Hardcore Mutator.", 50),
        SeededChallengeDef(20251002L, "Daily Seed #2", "Achieve a 10-guess streak on Seed 20251002.", 75)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mutators & Seeded Challenges") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Gameplay Mutators", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(mutators) { mutator ->
                val active = state.shopPurchases.contains(mutator.id)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mutator.name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                            Text(mutator.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reward Multiplier: ${mutator.rewardMultiplier}x", color = MoneyGold, fontSize = 14.sp)
                        }
                        Button(
                            onClick = { onSelectMutator(mutator.id) }
                        ) {
                            Text(if (active) "Disable" else "Activate")
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("Daily Seeded Challenges", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(challenges) { challenge ->
                val completed = state.completedChallenges.contains(challenge.seed.toString())
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(challenge.name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                            Text(challenge.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reward: +${challenge.rewardNebula} Nebula", color = MoneyGold, fontSize = 14.sp)
                        }
                        Button(
                            onClick = { onCompleteChallenge(challenge.seed.toString(), challenge.rewardNebula) },
                            enabled = !completed
                        ) {
                            Text(if (completed) "Completed" else "Claim")
                        }
                    }
                }
            }
        }
    }
}
