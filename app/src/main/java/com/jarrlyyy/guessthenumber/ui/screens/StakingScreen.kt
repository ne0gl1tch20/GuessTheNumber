package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StakingScreen(
    state: GameState,
    onStakeResult: (BigNumber, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var stakeInput by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Combo Streaks & Lucky Stake") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Streak: ${state.streak} (Best: ${state.bestStreak})", fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Money: ${state.money.format()}", color = MoneyGold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Stake a portion of your money on a 50/50 'Lucky Guess' coin flip. Win to double your stake (+streak multiplier bonus), lose to forfeit it!", fontSize = 13.sp)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = stakeInput,
                    onValueChange = { stakeInput = it },
                    label = { Text("Stake Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = {
                        val amt = stakeInput.toLongOrNull()
                        if (amt != null && amt > 0) {
                            val stakeAmount = BigNumber(amt)
                            if (state.money >= stakeAmount) {
                                val won = Random.nextBoolean()
                                val multiplier = 2.0 + (state.streak * 0.1)
                                if (won) {
                                    val payout = stakeAmount * BigNumber(multiplier)
                                    onStakeResult(payout, true)
                                    resultMessage = "🎉 Lucky Guess Won! Earned ${payout.format()}!"
                                } else {
                                    onStakeResult(stakeAmount, false)
                                    resultMessage = "💥 Lucky Guess Lost! Forfeited ${stakeAmount.format()}."
                                }
                                stakeInput = ""
                            } else {
                                resultMessage = "Insufficient funds for this stake!"
                            }
                        } else {
                            resultMessage = "Please enter a valid stake amount."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyGold)
                ) {
                    Text("Stake & Flip (50/50)", color = MaterialTheme.colorScheme.onBackground)
                }
            }

            if (resultMessage != null) {
                item {
                    Text(
                        text = resultMessage!!,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}
