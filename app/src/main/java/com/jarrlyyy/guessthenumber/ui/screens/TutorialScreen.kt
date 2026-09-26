package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TutorialScreen(
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (step) {
                            1 -> "🎮 Welcome to Guess The Number!"
                            2 -> "📈 Earn Money & Upgrades"
                            else -> "🚀 Prestige & Beyond"
                        },
                        fontSize = 22.sp,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (step) {
                            1 -> "Guess the secret target number correctly to earn Money. Every initial correct guess awards some money!"
                            2 -> "Spend your hard-earned money on Upgrades in the Upgrades tab to multiply rewards and narrow down guessing ranges."
                            else -> "Reach high milestones to perform Prestige and Ultra resets, unlocking Nebula currency and Arcade minigames!"
                        },
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                onComplete()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (step < 3) "Next" else "Start Playing!")
                    }
                }
            }
        }
    }
}
