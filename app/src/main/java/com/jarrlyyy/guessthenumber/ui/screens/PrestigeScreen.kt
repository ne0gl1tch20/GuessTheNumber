package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.PrestigeBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrestigeScreen(
    state: GameState,
    onPrestige: () -> Unit,
    onBuyPrestigeUpgrade: (String, BigNumber) -> Unit,
    onBuyPrestigeShopItem: (String, Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameEngine = GameEngine()
    val requiredMoney = gameEngine.calculatePrestigeRequirement(state.prestigeCount)
    val prestigeReward = gameEngine.calculatePrestigeReward(state.money, state.prestigeCount)
    val canPrestige = state.money >= requiredMoney

    val upgrades = remember { JsonConfigRepository(context).loadPrestigeUpgrades() }
    val shopItems = remember { JsonConfigRepository(context).loadPrestigeShopItems() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prestige Hub & Upgrades") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Currency Display
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Prestige Balance: ${state.prestige.format()}", color = PrestigeBlue, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Requirement: ${requiredMoney.format()} Money (Reset #${state.prestigeCount + 1})")
                        Text("Current Money: ${state.money.format()}")
                        Text("Preview Reward: +${prestigeReward.format()} Prestige")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🔄 Reset Info: Resets all regular Money, Upgrade Levels, and resets guessing range back to 1-100. Each prestige increases the money requirement for the next prestige.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onPrestige,
                            enabled = canPrestige,
                            colors = ButtonDefaults.buttonColors(containerColor = PrestigeBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (canPrestige) "Perform Prestige Reset" else "Need ${requiredMoney.format()} Money to Prestige")
                        }
                    }
                }
            }

            // Prestige Upgrades Header
            item {
                Text("Prestige Upgrade Tree (JSON Driven)", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(upgrades) { upgrade ->
                val level = state.prestigeUpgradeLevels[upgrade.id] ?: 0
                val cost = BigNumber(upgrade.baseCost) * BigNumber(upgrade.costMultiplier).pow(level)
                val canAfford = state.prestige >= cost

                Card(modifier = Modifier.fillMaxWidth()) {
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
                            Text("Cost: ${cost.format()} Prestige", fontSize = 14.sp, color = PrestigeBlue)
                        }
                        Button(
                            onClick = { onBuyPrestigeUpgrade(upgrade.id, cost) },
                            enabled = canAfford && level < upgrade.maxLevel,
                            colors = ButtonDefaults.buttonColors(containerColor = PrestigeBlue)
                        ) {
                            Text("Buy")
                        }
                    }
                }
            }

            // Prestige Shop Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Prestige Shop (JSON Driven)", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(shopItems) { item ->
                val purchased = state.prestigeShopPurchases.contains(item.id)
                val canAfford = state.prestige >= BigNumber(item.cost)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontSize = 18.sp)
                            Text(item.description, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cost: ${item.cost} Prestige", color = PrestigeBlue)
                        }
                        Button(
                            onClick = { onBuyPrestigeShopItem(item.id, item.cost) },
                            enabled = canAfford && !purchased,
                            colors = ButtonDefaults.buttonColors(containerColor = PrestigeBlue)
                        ) {
                            Text(if (purchased) "Owned" else "Buy")
                        }
                    }
                }
            }
        }
    }
}
