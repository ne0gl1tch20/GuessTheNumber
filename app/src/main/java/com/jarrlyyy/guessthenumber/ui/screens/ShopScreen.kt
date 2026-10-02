package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    onBuyShopItem: (String, Long) -> Unit
) {
    val context = LocalContext.current
    val shopItems = remember {
        JsonConfigRepository(context).loadShopItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nebula Shop", style = MaterialTheme.typography.titleMedium) },
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
            // Material 3 Expressive Balance Header Card (Matching UpgradeScreen style)
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
                            Text("Available Nebula", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(2.dp))
                            AnimatedContent(targetState = state.nebula.format(), label = "ShopNebulaAnimation") { nebulaStr ->
                                Text("$nebulaStr Nebula", color = NebulaPink, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = NebulaPink, modifier = Modifier.size(32.dp))
                    }
                }
            }

            items(shopItems, key = { it.id }) { item ->
                val purchased = state.shopPurchases.contains(item.id)
                val canAfford = state.nebula >= BigNumber(item.nebulaCost)

                // Material 3 Expressive Shop Item Card (Matching UpgradeScreen style)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Cost: ${item.nebulaCost} Nebula",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NebulaPink,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onBuyShopItem(item.id, item.nebulaCost) },
                            enabled = canAfford && !purchased,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (purchased) "Owned" else "Acquire", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
