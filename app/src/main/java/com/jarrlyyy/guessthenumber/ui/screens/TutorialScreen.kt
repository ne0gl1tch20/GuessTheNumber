package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

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
    var step by remember { mutableIntStateOf(0) }
    var pressed by remember { mutableStateOf(false) }

    val titles = listOf(
        "🎮 Welcome! Try the button below",
        "📈 Great! Now learn Upgrades",
        "🧭 Nice! More has your systems",
        "🚀 You're ready to play!"
    )
    val descriptions = listOf(
        "This tutorial is interactive. You must press the highlighted control to continue.",
        "Upgrades improve your guessing run. Press the highlighted Upgrades button.",
        "The More screen contains Save Slots, settings, statistics, and advanced systems.",
        "You completed the guided tour. Your new save is ready!"
    )

    fun advance() {
        if (step < 3) {
            step++
            pressed = false
        } else {
            onComplete()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(8.dp)) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(localizedText("Step ${step + 1} of 4"), style = MaterialTheme.typography.labelLarge)
                    Text(titles[step], fontSize = 22.sp, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(descriptions[step], fontSize = 16.sp)

                    if (step < 3) {
                        Text(localizedText("👇 Tap the highlighted button"), color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = { pressed = true; advance() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when (step) {
                                    0 -> "GUESS NUMBER"
                                    1 -> "UPGRADES"
                                    else -> "MORE"
                                }
                            )
                        }
                        if (!pressed) {
                            Text(localizedText("You need to press it to continue."), style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Button(onClick = { onComplete() }, modifier = Modifier.fillMaxWidth()) {
                            Text(localizedText("Start Playing!"))
                        }
                    }
                }
            }
        }
    }
}
