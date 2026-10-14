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
    onUpdateMultiplier: (String) -> Unit = {},
    onBuyPrestigeShopItem: (String, Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locale = com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager.current
    val gameEngine = GameEngine()
    val requiredMoney = gameEngine.calculatePrestigeRequirement(state.prestigeCount)
    val prestigeReward = gameEngine.calculatePrestigeReward(state.money, state.prestigeCount)
    val canPrestige = state.money >= requiredMoney
    val prestigeInteractionSource = remember { MutableInteractionSource() }
    var showPrestigeCelebration by remember { mutableStateOf(false) }
    var observedPrestigeCount by remember { mutableStateOf(state.prestigeCount) }

    LaunchedEffect(state.prestigeCount) {
        val reachedNewPrestige = state.prestigeCount > observedPrestigeCount
        observedPrestigeCount = state.prestigeCount
        if (reachedNewPrestige) {
            showPrestigeCelebration = true
            delay(if (state.settings.reducedMotion) 700L else 2200L)
            showPrestigeCelebration = false
        }
    }

    val upgrades = remember { JsonConfigRepository(context).loadPrestigeUpgrades() }
    val shopItems = remember { JsonConfigRepository(context).loadPrestigeShopItems() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText("Prestige Hub & Upgrades")) },
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
                        AnimatedContent(targetState = state.prestige.format(), label = "prestigeBalance") { balance ->
                            Text(locale.getString("prestige_balance_format", "Prestige Balance: %s", balance), color = PrestigeBlue, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(locale.getString("prestige_requirements_format", "Requirement: %s Money (Reset #%d)", requiredMoney.format(), state.prestigeCount + 1))
                        Text(locale.getString("prestige_current_money_format", "Current Money: %s", state.money.format()))
                        Text(locale.getString("prestige_preview_reward_format", "Preview Reward: +%s Prestige", prestigeReward.format()))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(localizedText("🔄 Reset Info: Resets all regular Money, Upgrade Levels, and resets guessing range back to 1-100. Each prestige increases the money requirement for the next prestige."), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onPrestige,
                            enabled = canPrestige,
                            interactionSource = prestigeInteractionSource,
                            colors = ButtonDefaults.buttonColors(containerColor = PrestigeBlue),
                            modifier = Modifier
                                .expressivePressScale(
                                    interactionSource = prestigeInteractionSource,
                                    enabled = canPrestige,
                                    reducedMotion = state.settings.reducedMotion,
                                    scaleDownFactor = 0.97f
                                )
                                .fillMaxWidth()
                        ) {
                            Text(if (canPrestige) "Perform Prestige Reset" else "Need ${requiredMoney.format()} Money to Prestige")
                        }
                    }
                }
            }

            item {
                ExpressiveMilestoneBanner(
                    visible = showPrestigeCelebration,
                    title = locale.getString("prestige_achieved_title", "Prestige achieved!"),
                    description = "A new reset is complete. Your next run starts now.",
                    reducedMotion = state.settings.reducedMotion
                )
            }

            // Prestige Upgrades Header
            item {
                Text(localizedText("Prestige Upgrade Tree (JSON Driven)"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                val multipliers = listOf("1", "10", "100", "MAX")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(localizedText("Buy Multiplier:"), style = MaterialTheme.typography.titleMedium)
                    Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                        OutlinedButton(onClick = { expanded = true }, shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(locale.getString("multiplier_format", "%sx", state.buyMultiplier), style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = locale.getString("dropdown", "Dropdown"))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            multipliers.forEach { mult ->
                                DropdownMenuItem(text = { Text(locale.getString("multiplier_option_format", "%s x", mult), style = MaterialTheme.typography.bodyMedium) }, onClick = { onUpdateMultiplier(mult); expanded = false })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
            }
            items(upgrades, key = { it.id }) { upgrade ->
                val level = state.prestigeUpgradeLevels[upgrade.id] ?: 0
                val isMaxed = level >= upgrade.maxLevel
                val totalCost = remember(level, state.buyMultiplier, state.prestige) {
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
                            if (state.prestige < total + next) break
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
                val canAfford = !isMaxed && state.prestige >= totalCost

                Surface(Modifier.fillMaxWidth().then(if (state.settings.reducedMotion) Modifier else Modifier.animateItem(placementSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy))).animateContentSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(upgrade.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(upgrade.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                Text(locale.getString("upgrade_level_format", "Level: %d / %s", level, if (upgrade.maxLevel >= 999999) locale.getString("maxed_label", "MAX") else upgrade.maxLevel.toString()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            if (!isMaxed) {
                                Spacer(Modifier.height(4.dp))
                                Text(locale.getString("prestige_upgrade_cost_format", "Cost (%sx): %s Prestige", state.buyMultiplier, totalCost.format()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Spacer(Modifier.height(4.dp))
                                Text(localizedText("MAXED"), style = MaterialTheme.typography.bodySmall, color = PrestigeBlue)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = { onBuyPrestigeUpgrade(upgrade.id, totalCost) }, enabled = canAfford, shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = PrestigeBlue)) {
                            Text(if (isMaxed) "MAX" else "Buy", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Prestige Shop Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(localizedText("Prestige Shop (JSON Driven)"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(shopItems, key = { it.id }) { item ->
                val purchased = state.prestigeShopPurchases.contains(item.id)
                val canAfford = state.prestige >= BigNumber(item.cost)

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
                            Text(locale.getString("prestige_shop_cost_format", "Cost: %s Prestige", item.cost), color = PrestigeBlue)
                        }
                        Button(
                            onClick = { onBuyPrestigeShopItem(item.id, item.cost) },
                            enabled = canAfford && !purchased,
                            colors = ButtonDefaults.buttonColors(containerColor = PrestigeBlue)
                        ) {
                            AnimatedContent(targetState = purchased, label = "prestigeShopPurchase_${item.id}") { isPurchased -> Text(if (isPurchased) "Owned" else "Buy") }
                        }
                    }
                }
            }
        }
    }
}
