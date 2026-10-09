package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import kotlinx.coroutines.delay
import com.jarrlyyy.guessthenumber.ui.components.ExpressiveMilestoneBanner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.jarrlyyy.guessthenumber.ui.theme.expressivePressScale
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
    val locale = remember(state.settings.locale) { JsonConfigRepository(context, state.settings.locale).localeManager }
    val gameEngine = GameEngine()
    val requiredMoney = gameEngine.calculateUltraRequirement(state.ultraCount)
    val ultraReward = gameEngine.calculateUltraReward(state.money, state.ultraCount)
    val canUltra = state.money >= requiredMoney && state.prestige >= BigNumber(1_000)
    val ultraInteractionSource = remember { MutableInteractionSource() }
    var showUltraCelebration by remember { mutableStateOf(false) }
    var observedUltraCount by remember { mutableStateOf(state.ultraCount) }

    LaunchedEffect(state.ultraCount) {
        val reachedNewUltra = state.ultraCount > observedUltraCount
        observedUltraCount = state.ultraCount
        if (reachedNewUltra) {
            showUltraCelebration = true
            delay(if (state.settings.reducedMotion) 700L else 2400L)
            showUltraCelebration = false
        }
    }

    val upgrades = remember { JsonConfigRepository(context).loadUltraUpgrades() }
    val shopItems = remember { JsonConfigRepository(context).loadUltraShopItems() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locale.getString("ultra_hub_title", "Ultra Hub & Upgrades")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = locale.getString("back", "Back"))
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
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AnimatedContent(targetState = state.ultra.format(), label = "ultraBalance") { balance ->
                            Text(localizedText("Ultra Balance: $balance"), color = UltraPurple, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(localizedText("Requirements: ${requiredMoney.format()} Money AND 1,000 Prestige (Reset #${state.ultraCount + 1})"))
                        Text(localizedText("Current Money: ${state.money.format()} | Current Prestige: ${state.prestige.format()}"))
                        Text(localizedText("Preview Reward: +${ultraReward.format()} Ultra (Max 2,000)"))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(localizedText("🔄 Reset Info: Resets all regular Money, Prestige balance, Upgrade Levels, and Prestige Upgrade Levels. Each ultra increases the money requirement for the next ultra."), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onUltra,
                            enabled = canUltra,
                            interactionSource = ultraInteractionSource,
                            colors = ButtonDefaults.buttonColors(containerColor = UltraPurple),
                            modifier = Modifier
                                .expressivePressScale(
                                    interactionSource = ultraInteractionSource,
                                    enabled = canUltra,
                                    reducedMotion = state.settings.reducedMotion,
                                    scaleDownFactor = 0.97f
                                )
                                .fillMaxWidth()
                        ) {
                            Text(if (canUltra) "Perform Ultra Reset" else "Need ${requiredMoney.format()} Money & 1K Prestige to Ultra")
                        }
                    }
                }
            }

            item {
                ExpressiveMilestoneBanner(
                    visible = showUltraCelebration,
                    title = "Ultra achieved!",
                    description = "Your cosmic reset is complete. Time to build bigger.",
                    reducedMotion = state.settings.reducedMotion
                )
            }

            // Ultra Upgrades Header
            item {
                Text(localizedText("Ultra Upgrade Tree (JSON Driven)"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                val multipliers = listOf("1", "10", "100", "MAX")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(localizedText("Buy Multiplier:"), style = MaterialTheme.typography.titleMedium)
                    Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                        OutlinedButton(onClick = { expanded = true }, shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(localizedText("${state.buyMultiplier}x"), style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = locale.getString("dropdown", "Dropdown"))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            multipliers.forEach { mult ->
                                DropdownMenuItem(text = { Text(localizedText("$mult x"), style = MaterialTheme.typography.bodyMedium) }, onClick = { onUpdateMultiplier(mult); expanded = false })
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

                Surface(Modifier.fillMaxWidth().then(if (state.settings.reducedMotion) Modifier else Modifier.animateItem(placementSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy))).animateContentSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(upgrade.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(upgrade.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                Text(localizedText("Level: ") + level + " / " + if (upgrade.maxLevel >= 999999) "MAX" else upgrade.maxLevel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            if (!isMaxed) {
                                Spacer(Modifier.height(4.dp))
                                Text(localizedText("Cost (${state.buyMultiplier}x): ${totalCost.format()} Ultra"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Spacer(Modifier.height(4.dp))
                                Text(localizedText("MAXED"), style = MaterialTheme.typography.bodySmall, color = UltraPurple)
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
                Text(localizedText("Ultra Shop (JSON Driven)"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(shopItems, key = { it.id }) { item ->
                val purchased = state.ultraShopPurchases.contains(item.id)
                val canAfford = state.ultra >= BigNumber(item.cost)

                Card(modifier = Modifier.fillMaxWidth().then(if (state.settings.reducedMotion) Modifier else Modifier.animateItem(placementSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy))).animateContentSize()) {
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
                            Text(localizedText("Cost: ${item.cost} Ultra"), color = UltraPurple)
                        }
                        Button(
                            onClick = { onBuyUltraShopItem(item.id, item.cost) },
                            enabled = canAfford && !purchased,
                            colors = ButtonDefaults.buttonColors(containerColor = UltraPurple)
                        ) {
                            AnimatedContent(targetState = purchased, label = "ultraShopPurchase_${item.id}") { isPurchased -> Text(if (isPurchased) "Owned" else "Buy") }
                        }
                    }
                }
            }
        }
    }
}
