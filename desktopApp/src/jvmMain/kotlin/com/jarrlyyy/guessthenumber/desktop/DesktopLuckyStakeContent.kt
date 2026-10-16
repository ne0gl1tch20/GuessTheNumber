package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlin.random.Random

@Composable
internal fun DesktopLuckyStakeContent(
    state: GameState,
    onBack: () -> Unit,
    onStakeResult: (BigNumber, BigNumber, Boolean) -> Unit,
    rng: Random = Random.Default
) {
    var stakeInput by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Casino, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Combo Streaks & Lucky Stake", style = MaterialTheme.typography.headlineSmall)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Current Streak: ${state.streak} (Best: ${state.bestStreak})", style = MaterialTheme.typography.titleMedium)
                Text("Money: ${state.money.format()}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("Stake money on a 50/50 lucky coin flip. Win to receive your stake back plus a streak-based payout; lose and the stake is forfeited.")
                Text("Win payout multiplier: ${"%.1f".format(2.0 + state.streak * 0.1)}×", style = MaterialTheme.typography.labelLarge)
            }
        }
        OutlinedTextField(
            value = stakeInput,
            onValueChange = { input -> stakeInput = input.filter { it.isDigit() } },
            label = { Text("Stake amount") },
            supportingText = { Text("Whole numbers only. Available: ${state.money.format()}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val amount = stakeInput.toLongOrNull()
                when {
                    amount == null || amount <= 0L -> resultMessage = "Please enter a valid stake amount."
                    state.money < BigNumber(amount) -> resultMessage = "Insufficient funds for this stake."
                    else -> {
                        val stake = BigNumber(amount)
                        val won = rng.nextBoolean()
                        val payout = if (won) stake * BigNumber(2.0 + state.streak * 0.1) else BigNumber.ZERO
                        onStakeResult(stake, payout, won)
                        resultMessage = if (won) {
                            "🎉 Lucky Guess Won! Payout: ${payout.format()}."
                        } else {
                            "💥 Lucky Guess Lost! Forfeited ${stake.format()}."
                        }
                        stakeInput = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Stake & Flip (50/50)") }
        resultMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
