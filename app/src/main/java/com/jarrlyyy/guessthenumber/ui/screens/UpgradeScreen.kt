package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.jarrlyyy.guessthenumber.ui.theme.expressivePressScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    
    // Remember upgrade costs and affordability for all upgrade levels in state to avoid heavy recalculations during UI composition
    val computedUpgradeCosts = remember(state.upgradeLevels, state.buyMultiplier, state.money) {
        upgrades.associate { upgrade ->
            val level = state.upgradeLevels[upgrade.id] ?: 0
            val maxLevel = upgrade.maxLevel
            val base = BigNumber(upgrade.baseCost)
            val mult = BigNumber(upgrade.costMultiplier)
            
            val effectiveLevels = when (state.buyMultiplier) {
                "10" -> minOf(10, maxLevel - level)
                "100" -> minOf(100, maxLevel - level)
                "MAX" -> {
                    // Calculate how many levels we can afford with current money without freezing
                    var count = 0
                    var totalCost = BigNumber.ZERO
                    var currentCurLevel = level
                    while (currentCurLevel < maxLevel) {
                        val cost = base * mult.pow(currentCurLevel)
                        if (state.money >= totalCost + cost) {
                            totalCost += cost
                            count++
                            currentCurLevel++
                        } else {
                            break
                        }
                    }
                    if (count == 0 && level < maxLevel && state.money >= base * mult.pow(level)) {
                        1
                    } else {
                        count
                    }
                }
                else -> minOf(1, maxLevel - level)
            }
            
            var totalCost = BigNumber.ZERO
            val levelsToBuy = maxOf(1, effectiveLevels)
            for (i in 0 until levelsToBuy) {
                totalCost += base * mult.pow(level + i)
            }
            upgrade.id to totalCost
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText("Upgrades"), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            // Material 3 Expressive Balance Header Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(localizedText("Available Money"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            AnimatedContent(targetState = state.money.format(), label = "UpgradeMoneyAnimation") { moneyStr ->
                                Text(localizedText("$moneyStr Money"), color = MoneyGold, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = MoneyGold, modifier = Modifier.size(32.dp))
                    }
                }
            }

            // Multiplier Selector Row (Dropdown)
            item {
                var expanded by remember { mutableStateOf(false) }
                val multipliers = listOf("1", "10", "100", "MAX")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(localizedText("Buy Multiplier:"), style = MaterialTheme.typography.titleMedium)
                    
                    Box(
                        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                    ) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(localizedText("${state.buyMultiplier}x"), style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            multipliers.forEach { mult ->
                                DropdownMenuItem(
                                    text = { Text(localizedText("$mult x"), style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onUpdateMultiplier(mult)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
            }

            items(upgrades, key = { it.id }) { upgrade ->
                val level = state.upgradeLevels[upgrade.id] ?: 0
                val totalCost = computedUpgradeCosts[upgrade.id] ?: BigNumber.ZERO
                val canAfford = state.money >= totalCost
                val buyInteractionSource = remember(upgrade.id) { MutableInteractionSource() }

                // Material 3 Expressive Upgrade Card
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .then(if (state.settings.reducedMotion) Modifier else Modifier.animateItem(placementSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)))
                        .animateContentSize(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
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
                            Text(upgrade.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(upgrade.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                AnimatedContent(targetState = level, label = "upgradeLevel_${upgrade.id}") { animatedLevel ->
                                    Text(text = "Level: $animatedLevel / ${if (upgrade.maxLevel >= 999999) "MAX" else upgrade.maxLevel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!isMaxed) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(localizedText("Cost (${state.buyMultiplier}x): ${totalCost.format()}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            interactionSource = buyInteractionSource,
                            onClick = { onBuyUpgrade(upgrade.id, totalCost) },
                            enabled = canAfford && !isMaxed,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.expressivePressScale(
                                interactionSource = buyInteractionSource,
                                enabled = canAfford && !isMaxed,
                                reducedMotion = state.settings.reducedMotion
                            ),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isMaxed) "MAX" else "Buy", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
