package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.engine.ArcadeRewardRules
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Shared native Compose Canvas arcade games. This source set is consumed by Android
 * and the Windows desktop app, so gameplay and scoring stay identical on both.
 */
@Composable
fun ArcadeCanvasGame(
    gameId: String,
    onDismiss: () -> Unit,
    isRewarding: Boolean = false,
    onReward: (Long, BigNumber) -> Unit
) {
    val isMath = gameId == "math_blitz"
    var score by remember(gameId) { mutableStateOf(0) }
    var timeLeft by remember(gameId) { mutableStateOf(60) }
    var finished by remember(gameId) { mutableStateOf(false) }
    var first by remember(gameId) { mutableStateOf(Random.nextInt(2, 13)) }
    var second by remember(gameId) { mutableStateOf(Random.nextInt(2, 13)) }
    var operation by remember(gameId) { mutableStateOf(Random.nextInt(0, 3)) }
    val answer = when (operation) { 0 -> first + second; 1 -> first * second; else -> first - second }
    var choices by remember(gameId) { mutableStateOf(mathChoices(answer)) }
    val cards = remember(gameId) { (listOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8)).shuffled() }
    val selected = remember(gameId) { mutableStateListOf<Int>() }
    val matched = remember(gameId) { mutableStateListOf<Int>() }
    var startTime by remember(gameId) { mutableStateOf(System.nanoTime()) }
    var moves by remember(gameId) { mutableStateOf(0) }

    LaunchedEffect(gameId) {
        while (timeLeft > 0 && !finished) {
            delay(1_000)
            timeLeft--
        }
        finished = true
    }
    LaunchedEffect(selected.size, gameId) {
        if (!isMath && selected.size == 2) {
            delay(500)
            val a = selected[0]
            val b = selected[1]
            if (cards[a] == cards[b]) {
                if (a !in matched) matched.add(a)
                if (b !in matched) matched.add(b)
                score += 2
                if (matched.size == cards.size) finished = true
            } else {
                score = (score - 1).coerceAtLeast(0)
            }
            selected.clear()
        }
    }

    val elapsed = ((System.nanoTime() - startTime) / 1_000_000_000L).toInt().coerceAtLeast(1)
    val rewardNebula = ArcadeRewardRules.calculateNebulaReward(elapsed.toLong(), score)
    val rewardMoney = ArcadeRewardRules.calculateMoneyReward(elapsed.toLong(), score)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isMath) "Math Blitz" else "Memory Grid") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Score  $score", style = MaterialTheme.typography.titleMedium)
                    Text(if (finished) "Finished" else "${timeLeft}s", color = MaterialTheme.colorScheme.primary)
                }
                Canvas(Modifier.fillMaxWidth().height(66.dp)) {
                    val progress = (timeLeft / 60f).coerceIn(0f, 1f)
                    drawRoundRect(Color(0xFF25334A), size = Size(size.width, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f))
                    drawRoundRect(Color(0xFF72D9C5), size = Size(size.width * progress, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f))
                    for (index in 0..5) drawCircle(Color.White.copy(alpha = 0.18f), radius = 4f + index, center = Offset(size.width * (index + 1) / 7f, size.height / 2f), style = Stroke(width = 2f))
                }
                if (isMath && !finished) {
                    Text("$first ${listOf("+", "×", "−")[operation]} $second = ?", style = MaterialTheme.typography.headlineMedium)
                    choices.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { option ->
                                Button(onClick = {
                                    if (option == answer) score++
                                    first = Random.nextInt(2, 13)
                                    second = Random.nextInt(2, 13)
                                    operation = Random.nextInt(0, 3)
                                    choices = mathChoices(when (operation) { 0 -> first + second; 1 -> first * second; else -> first - second })
                                }, modifier = Modifier.weight(1f)) { Text(option.toString()) }
                            }
                        }
                    }
                } else if (!isMath && !finished) {
                    Text("Find all 8 matching pairs. Moves: $moves", style = MaterialTheme.typography.bodyMedium)
                    cards.indices.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { index ->
                                val revealed = index in selected || index in matched
                                Button(
                                    onClick = {
                                        if (selected.size < 2 && index !in selected && index !in matched) {
                                            selected.add(index)
                                            if (selected.size == 2) moves++
                                        }
                                    },
                                    enabled = !revealed && selected.size < 2,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) { Text(if (revealed) cards[index].toString() else "?", style = MaterialTheme.typography.titleLarge) }
                            }
                        }
                    }
                    Text("Pairs: ${matched.size / 2} / 8", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(if (isMath) "Time's up!" else "Grid cleared!", style = MaterialTheme.typography.titleLarge)
                    Text("Final score: $score")
                    Text("Play time: ${elapsed}s")
                    Text("+$rewardNebula Nebula  •  +$rewardMoney Money", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            if (finished) Button(
                onClick = { onReward(rewardNebula, BigNumber(rewardMoney)); onDismiss() },
                enabled = !isRewarding
            ) { Text(if (isRewarding) "Saving…" else "Claim reward") }
            else TextButton(onClick = onDismiss) { Text("Quit") }
        }
    )
}

private fun mathChoices(answer: Int): List<Int> =
    (listOf(answer) + List(3) { answer + Random.nextInt(-12, 13) }.filter { it != answer }.distinct().take(3))
        .let { values -> (values + List(4 - values.size) { answer + it + 1 }).distinct().take(4).shuffled() }
