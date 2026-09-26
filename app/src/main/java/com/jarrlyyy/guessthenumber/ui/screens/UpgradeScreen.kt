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
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    state: GameState,
    onBuyUpgrade: (String, BigNumber) -> Unit,
    onUpdateMultiplier: (String) -> Unit = {}
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
            // Balance / Currency Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available Balance", style = MaterialTheme.typography.titleMedium)
                        Text("${state.money.format()} Money", color = MoneyGold, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Multiplier Selector Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Buy Multiplier:", style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        listOf("1", "10", "100", "MAX").forEach { mult ->
                            FilterChip(
                                selected = state.buyMultiplier == mult,
                                onClick = { onUpdateMultiplier(mult) },
                                label = { Text(mult, fontSize = 10.sp) },
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 28.dp)
                                    .padding(horizontal = 0.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
            }

            items(upgrades) { upgrade ->
                val level = state.upgradeLevels[upgrade.id] ?: 0
                val multCount = when (state.buyMultiplier) {
                    "10" -> 10
                    "100" -> 100
                    "MAX" -> 100
                    else -> 1
                }
                val effectiveLevels = minOf(multCount, upgrade.maxLevel - level)
                
                // Calculate total cost for effectiveLevels
                var totalCost = BigNumber.ZERO
                var tempLevel = level
                for (i in 0 until effectiveLevels) {
                    totalCost += BigNumber(upgrade.baseCost) * BigNumber(upgrade.costMultiplier).pow(tempLevel)
                    tempLevel++
                }
                if (effectiveLevels <= 0) totalCost = BigNumber(upgrade.baseCost) * BigNumber(upgrade.costMultiplier).pow(level)

                val canAfford = state.money >= totalCost

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
                        val isMaxed = level >= upgrade.maxLevel
                        Column(modifier = Modifier.weight(1f)) {
                            Text(upgrade.name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                            Text(upgrade.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Level: $level / ${if (upgrade.maxLevel >= 999999) "MAX" else upgrade.maxLevel}", fontSize = 12.sp)
                            if (isMaxed) {
                                Text("MAXED OUT", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Text("Cost (${if (state.buyMultiplier == "MAX") "Max" else "${state.buyMultiplier}x"}): ${totalCost.format()}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Button(
                            onClick = { onBuyUpgrade(upgrade.id, totalCost) },
                            enabled = canAfford && !isMaxed
                        ) {
                            Text(if (isMaxed) "MAX" else "Buy")
                        }
                    }
                }
            }
        }
    }
}
