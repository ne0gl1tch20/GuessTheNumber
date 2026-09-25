package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold
import com.jarrlyyy.guessthenumber.ui.theme.NebulaPink
import com.jarrlyyy.guessthenumber.ui.theme.PrestigeBlue
import com.jarrlyyy.guessthenumber.ui.theme.UltraPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    state: GameState,
    onMakeGuess: (Long) -> Unit
) {
    var guessInput by remember { mutableStateOf("") }
    var lastFeedback by remember { mutableStateOf("Guess a number between ${state.currentRangeMin} and ${state.currentRangeMax}") }

    val infiniteTransition = rememberInfiniteTransition(label = "moe_pulse")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guess The Number Simulator") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Currencies Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scalePulse),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = "Money", tint = MoneyGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Money: ${state.money.format()}", color = MoneyGold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Prestige", tint = PrestigeBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Prestige: ${state.prestige.format()}", color = PrestigeBlue, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Ultra", tint = UltraPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Ultra: ${state.ultra.format()}", color = UltraPurple, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "Nebula", tint = NebulaPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Nebula: ${state.nebula.format()}", color = NebulaPink, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Play Area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Range: ${state.currentRangeMin} - ${state.currentRangeMax}", fontSize = 20.sp, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Streak: ${state.streak} (Best: ${state.bestStreak})", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = lastFeedback,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = guessInput,
                    onValueChange = { guessInput = it },
                    label = { Text("Enter your guess") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(onDone = {
                        val g = guessInput.toLongOrNull()
                        if (g != null) {
                            onMakeGuess(g)
                            lastFeedback = if (g < state.targetNumber) "📈 Too Low!" else if (g > state.targetNumber) "📉 Too High!" else "✨ Correct! Earned reward!"
                            guessInput = ""
                        }
                    }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val g = guessInput.toLongOrNull()
                        if (g != null) {
                            onMakeGuess(g)
                            lastFeedback = if (g < state.targetNumber) "📈 Too Low!" else if (g > state.targetNumber) "📉 Too High!" else "✨ Correct! Earned reward!"
                            guessInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("GUESS")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
