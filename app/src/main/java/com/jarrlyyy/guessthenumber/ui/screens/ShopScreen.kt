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
        topBar = { TopAppBar(title = { Text("Nebula Shop") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Nebula Currency: ${state.nebula.format()}", color = NebulaPink, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shopItems) { item ->
                    val purchased = state.shopPurchases.contains(item.id)
                    val canAfford = state.nebula >= BigNumber(item.nebulaCost)

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
                                Text("Cost: ${item.nebulaCost} Nebula", color = NebulaPink)
                            }
                            Button(
                                onClick = { onBuyShopItem(item.id, item.nebulaCost) },
                                enabled = canAfford && !purchased
                            ) {
                                Text(if (purchased) "Owned" else "Buy")
                            }
                        }
                    }
                }
            }
        }
    }
}
