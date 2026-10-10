package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.jarrlyyy.guessthenumber.ui.theme.expressivePressScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.NebulaPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    state: GameState,
    onBuyShopItem: (String, Long) -> Unit,
    onEquipCosmetic: (String) -> Unit
) {
    val context = LocalContext.current
    val configRepo = remember(state.settings.locale) { JsonConfigRepository(context, state.settings.locale) }
    val locale = configRepo.localeManager
    val shopItems = remember(state.settings.locale) { configRepo.loadShopItems() }
    var selectedCategory by remember { mutableStateOf("all") }

    val categories = listOf("all", "automation", "boosts", "cosmetics")
    val filteredItems = if (selectedCategory == "all") shopItems else shopItems.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(locale.getString("shop_title", "Nebula Shop"), style = MaterialTheme.typography.titleMedium) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(locale.getString("available_nebula", "Available Nebula"), style = MaterialTheme.typography.bodyMedium)
                            AnimatedContent(targetState = state.nebula.format(), label = "ShopNebulaAnimation") { nebulaStr ->
                                Text(locale.getString("shop_nebula_balance_format", "%s Nebula", nebulaStr), color = NebulaPink, style = MaterialTheme.typography.titleLarge)
                            }
                            state.equippedCosmeticId?.let { equipped ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    locale.getString("equipped_cosmetic_format", "Equipped: %s", equipped.removePrefix("cosmetic_").replace('_', ' ')),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        Icon(Icons.Default.Star, contentDescription = null, tint = NebulaPink, modifier = Modifier.size(32.dp))
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    locale.getString(
                                        "shop_category_${category}",
                                        when (category) {
                                            "automation" -> "Automation"
                                            "boosts" -> "Boosts"
                                            "cosmetics" -> "Cosmetics"
                                            else -> "All"
                                        }
                                    )
                                )
                            }
                        )
                    }
                }
            }

            items(filteredItems, key = { it.id }) { item ->
                val purchased = item.id in state.shopPurchases
                val equipped = state.equippedCosmeticId == item.id
                val canAfford = state.nebula >= BigNumber(item.nebulaCost)
                val itemInteractionSource = remember(item.id) { MutableInteractionSource() }

                val targetCardColor = if (equipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                val cardColor = if (state.settings.reducedMotion) targetCardColor else animateColorAsState(targetValue = targetCardColor, label = "shopCardColor_${item.id}").value

                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .then(if (state.settings.reducedMotion) Modifier else Modifier.animateItem(placementSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)))
                        .animateContentSize(),
                    shape = MaterialTheme.shapes.large,
                    color = cardColor,
                    tonalElevation = if (equipped) 4.dp else 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(locale.getString("cost", "Cost: %s Nebula", item.nebulaCost), style = MaterialTheme.typography.labelMedium, color = NebulaPink)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (item.isCosmetic && purchased) {
                            OutlinedButton(
                                onClick = { onEquipCosmetic(item.id) },
                                enabled = !equipped,
                                interactionSource = itemInteractionSource,
                                modifier = Modifier.expressivePressScale(
                                    interactionSource = itemInteractionSource,
                                    enabled = !equipped,
                                    reducedMotion = state.settings.reducedMotion
                                )
                            ) {
                                AnimatedContent(targetState = equipped, label = "cosmeticEquipState_${item.id}") { isEquipped ->
                                    Text(if (isEquipped) locale.getString("equipped", "Equipped") else locale.getString("equip", "Equip"))
                                }
                            }
                        } else {
                            Button(
                                onClick = { onBuyShopItem(item.id, item.nebulaCost) },
                                enabled = canAfford && !purchased,
                                interactionSource = itemInteractionSource,
                                modifier = Modifier.expressivePressScale(
                                    interactionSource = itemInteractionSource,
                                    enabled = canAfford && !purchased,
                                    reducedMotion = state.settings.reducedMotion
                                )
                            ) {
                                AnimatedContent(targetState = purchased, label = "shopPurchaseState_${item.id}") { isPurchased ->
                                    Text(if (isPurchased) locale.getString("owned", "Owned") else locale.getString("acquire", "Acquire"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
