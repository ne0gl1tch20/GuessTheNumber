package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    state: GameState,
    onBuyUpgrade: (String, BigNumber) -> Unit
) {
    val context = LocalContext.current
    val upgrades = remember {
        JsonConfigRepository(context).loadUpgrades()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Upgrades") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(upgrades) { upgrade ->
                val level = state.upgradeLevels[upgrade.id] ?: 0
                val cost = BigNumber(upgrade.baseCost) * BigNumber(upgrade.costMultiplier).pow(level)
                val canAfford = state.money >= cost

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(upgrade.name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                            Text(upgrade.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Level: $level / ${upgrade.maxLevel}", fontSize = 12.sp)
                            Text("Cost: ${cost.format()}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = { onBuyUpgrade(upgrade.id, cost) },
                            enabled = canAfford && level < upgrade.maxLevel
                        ) {
                            Text("Buy")
                        }
                    }
                }
            }
        }
    }
}
