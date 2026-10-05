package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
    onUpdateMultiplier: (String) -> Unit = {},
    onBuyUltraShopItem: (String, Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameEngine = GameEngine()
    val requiredMoney = gameEngine.calculateUltraRequirement(state.ultraCount)
    val ultraReward = gameEngine.calculateUltraReward(state.money, state.ultraCount)
    val canUltra = state.money >= requiredMoney && state.prestige >= BigNumber(1_000)

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
                        Text("Requirements: ${requiredMoney.format()} Money AND 1,000 Prestige (Reset #${state.ultraCount + 1})")
                        Text("Current Money: ${state.money.format()} | Current Prestige: ${state.prestige.format()}")
                        Text("Preview Reward: +${ultraReward.format()} Ultra (Max 2,000)")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🔄 Reset Info: Resets all regular Money, Prestige balance, Upgrade Levels, and Prestige Upgrade Levels. Each ultra increases the money requirement for the next ultra.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onUltra,
                            enabled = canUltra,
                            colors = ButtonDefaults.buttonColors(containerColor = UltraPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (canUltra) "Perform Ultra Reset" else "Need ${requiredMoney.format()} Money & 1K Prestige to Ultra")
                        }
                    }
                }
            }

            // Ultra Upgrades Header
            item {
                Text("Ultra Upgrade Tree (JSON Driven)", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                val multipliers = listOf("1", "10", "100", "MAX")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Buy Multiplier:", style = MaterialTheme.typography.titleMedium)
                    Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                        OutlinedButton(onClick = { expanded = true }, shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                            Text("${state.buyMultiplier}x", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            multipliers.forEach { mult ->
                                DropdownMenuItem(text = { Text("$mult x", style = MaterialTheme.typography.bodyMedium) }, onClick = { onUpdateMultiplier(mult); expanded = false })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
            }
            items(upgrades, key = { it.id }) { upgrade ->
                val level = state.ultraUpgradeLevels[upgrade.id] ?: 0
                val isMaxed = level >= upgrade.maxLevel
                val totalCost = remember(level, state.buyMultiplier, state.ultra) {
                    val base = BigNumber(upgrade.baseCost)
                    val mult = BigNumber(upgrade.costMultiplier)
                    var count = when (state.buyMultiplier) {
                        "10" -> minOf(10, upgrade.maxLevel - level)
                        "100" -> minOf(100, upgrade.maxLevel - level)
                        "MAX" -> 0
                        else -> minOf(1, upgrade.maxLevel - level)
                    }
                    if (state.buyMultiplier == "MAX") {
                        var current = level
                        var total = BigNumber.ZERO
                        while (current < upgrade.maxLevel) {
                            val next = base * mult.pow(current)
                            if (state.ultra < total + next) break
                            total += next
                            current++
                            count++
                        }
                    }
                    if (count <= 0) {
                        if (level < upgrade.maxLevel) base * mult.pow(level) else BigNumber.ZERO
                    } else {
                        (0 until count).fold(BigNumber.ZERO) { total, index ->
                            total + base * mult.pow(level + index)
                        }
                    }
                }
                val canAfford = !isMaxed && state.ultra >= totalCost

                Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(upgrade.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(upgrade.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                Text("Level: " + level + " / " + if (upgrade.maxLevel >= 999999) "MAX" else upgrade.maxLevel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            if (!isMaxed) {
                                Spacer(Modifier.height(4.dp))
                                Text("Cost (${state.buyMultiplier}x): ${totalCost.format()} Ultra", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Spacer(Modifier.height(4.dp))
                                Text("MAXED", style = MaterialTheme.typography.bodySmall, color = UltraPurple)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = { onBuyUltraUpgrade(upgrade.id, totalCost) }, enabled = canAfford, shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = UltraPurple)) {
                            Text(if (isMaxed) "MAX" else "Buy", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Ultra Shop Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ultra Shop (JSON Driven)", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(shopItems, key = { it.id }) { item ->
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
