package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.jarrlyyy.guessthenumber.ui.theme.UltraPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltraScreen(
    state: GameState,
    onUltra: () -> Unit,
    onBuyUltraUpgrade: (String, BigNumber) -> Unit,
    onBuyUltraShopItem: (String, Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameEngine = GameEngine()
    val ultraReward = gameEngine.calculateUltraReward(state.money)
    val canUltra = state.money >= BigNumber(10_000_000_000)

    val upgrades = remember { JsonConfigRepository(context).loadUltraUpgrades() }
    val shopItems = remember { JsonConfigRepository(context).loadUltraShopItems() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ultra Hub & Upgrades") },
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
                        Text("Ultra Balance: ${state.ultra.format()}", color = UltraPurple, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Requirement: 10,000,000,000 Money")
                        Text("Current Money: ${state.money.format()}")
                        Text("Preview Reward: +${ultraReward.format()} Ultra (Max 2,000)")
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onUltra,
                            enabled = canUltra,
                            colors = ButtonDefaults.buttonColors(containerColor = UltraPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (canUltra) "Perform Ultra Reset" else "Need 10B Money to Ultra")
                        }
                    }
                }
            }

            // Ultra Upgrades Header
            item {
                Text("Ultra Upgrade Tree (JSON Driven)", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(upgrades) { upgrade ->
                val level = state.ultraUpgradeLevels[upgrade.id] ?: 0
                val cost = BigNumber(upgrade.baseCost) * BigNumber(upgrade.costMultiplier).pow(level)
                val canAfford = state.ultra >= cost

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
                            Text("Cost: ${cost.format()} Ultra", fontSize = 14.sp, color = UltraPurple)
                        }
                        Button(
                            onClick = { onBuyUltraUpgrade(upgrade.id, cost) },
                            enabled = canAfford && level < upgrade.maxLevel,
                            colors = ButtonDefaults.buttonColors(containerColor = UltraPurple)
                        ) {
                            Text("Buy")
                        }
                    }
                }
            }

            // Ultra Shop Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ultra Shop (JSON Driven)", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(shopItems) { item ->
                val purchased = state.ultraShopPurchases.contains(item.id)
                val canAfford = state.ultra >= BigNumber(item.cost)

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
                            Text("Cost: ${item.cost} Ultra", color = UltraPurple)
                        }
                        Button(
                            onClick = { onBuyUltraShopItem(item.id, item.cost) },
                            enabled = canAfford && !purchased,
                            colors = ButtonDefaults.buttonColors(containerColor = UltraPurple)
                        ) {
                            Text(if (purchased) "Owned" else "Buy")
                        }
                    }
                }
            }
        }
    }
}
