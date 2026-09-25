package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(state: GameState, onBack: () -> Unit) {
    val stats = state.statistics
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Game Statistics", fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
            Text("Total Guesses: ${stats.totalGuesses}")
            Text("Correct Guesses: ${stats.correctGuesses}")
            Text("Failed Guesses: ${stats.failedGuesses}")
            Text("Money Earned: ${stats.moneyEarned.format()}")
            Text("Money Spent: ${stats.moneySpent.format()}")
            Text("Prestiges: ${stats.prestigesCount}")
            Text("Ultras: ${stats.ultrasCount}")
            Text("Playtime: ${stats.playtimeSeconds} seconds")
        }
    }
}
