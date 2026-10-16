package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.GameState
import java.time.LocalDate
import java.time.temporal.WeekFields

internal data class DesktopChallengeDefinition(val id: String, val kind: String, val target: Long, val rewardNebula: Long)

private val desktopMutators = listOf(
    Triple("mut_hardcore", "Hardcore", 2.0),
    Triple("mut_speed", "Speed", 1.5),
    Triple("mut_blind", "Blind", 5.0),
    Triple("mut_tax", "Tax", 10.0)
)

private fun desktopWeeklyChallenges(): List<DesktopChallengeDefinition> {
    val today = LocalDate.now()
    val week = today.get(WeekFields.ISO.weekOfWeekBasedYear())
    val year = today.get(WeekFields.ISO.weekBasedYear())
    val pool = listOf(
        DesktopChallengeDefinition("correct", "correct", 75, 75),
        DesktopChallengeDefinition("streak", "streak", 15, 90),
        DesktopChallengeDefinition("money", "money", 10_000_000, 100),
        DesktopChallengeDefinition("guesses", "guesses", 250, 80),
        DesktopChallengeDefinition("prestige", "prestige", 2, 125),
        DesktopChallengeDefinition("ultra", "ultra", 1, 175),
        DesktopChallengeDefinition("frenzy", "frenzy", 25, 110),
        DesktopChallengeDefinition("playtime", "playtime", 3_600, 95)
    )
    val offset = (week - 1) % pool.size
    return List(3) { pool[(offset + it * 2) % pool.size].let { c -> c.copy(id = "weekly_${year}_${week}_${c.kind}") } }
}

@Composable
internal fun DesktopMutatorsContent(
    state: GameState,
    onBack: () -> Unit,
    onMutatorChange: (String, Boolean) -> Unit,
    onActivateBuilder: (Set<String>) -> Unit,
    onClaimChallenge: (String, Long) -> Unit
) {
    var builderSelection by remember(state.activeMutators) { mutableStateOf(state.activeMutators) }
    var challengeKind by remember { mutableStateOf("correct") }
    var targetText by remember { mutableStateOf("25") }
    val target = targetText.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
    val builderId = "builder_${challengeKind}_${target}_${builderSelection.sorted().joinToString("_")}"
    val builderProgress = when (challengeKind) {
        "correct" -> state.correctGuesses
        "streak" -> state.bestStreak.toLong()
        "guesses" -> state.statistics.totalGuesses
        else -> state.money.value.toLong()
    }
    val builderReward = 50L + builderSelection.size * 50L
    val weekly = remember { desktopWeeklyChallenges() }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Mutators & Weekly Challenges", style = MaterialTheme.typography.headlineSmall)
        }
        Text("Gameplay Mutators", style = MaterialTheme.typography.titleLarge)
        desktopMutators.forEach { (id, name, multiplier) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(when (id) { "mut_hardcore" -> "Hardcore rules"; "mut_speed" -> "Faster-paced challenge"; "mut_blind" -> "Reduced visual hints"; else -> "Tax challenge rules" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Reward multiplier: ${multiplier}x", color = MaterialTheme.colorScheme.primary)
                    }
                    Switch(checked = id in state.activeMutators, onCheckedChange = { onMutatorChange(id, it) })
                }
            }
        }
        Text("Challenge Builder", style = MaterialTheme.typography.titleLarge)
        Text("Choose mutators and set a goal. Activating applies the selected mutator set to your current run.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            desktopMutators.forEach { (id, name, _) ->
                FilterChip(selected = id in builderSelection, onClick = { builderSelection = builderSelection.toMutableSet().apply { if (!add(id)) remove(id) } }, label = { Text(name) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("correct", "streak", "guesses", "money").forEach { kind ->
                FilterChip(selected = challengeKind == kind, onClick = { challengeKind = kind; targetText = when (kind) { "correct" -> "25"; "streak" -> "10"; "guesses" -> "100"; else -> "1000000" } }, label = { Text(kind.replaceFirstChar { it.uppercase() }) })
            }
        }
        OutlinedTextField(value = targetText, onValueChange = { targetText = it.filter(Char::isDigit).take(12) }, label = { Text("Target") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Custom Challenge", style = MaterialTheme.typography.titleMedium)
                Text("Progress: ${builderProgress.coerceAtLeast(0)} / $target")
                Text("Reward: +$builderReward Nebula")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onActivateBuilder(builderSelection) }) { Text("Activate Build") }
                    Button(onClick = { onClaimChallenge(builderId, builderReward) }, enabled = builderProgress >= target && builderId !in state.completedChallenges) { Text(if (builderId in state.completedChallenges) "Completed" else if (builderProgress >= target) "Claim" else "In Progress") }
                }
            }
        }
        Text("Weekly Challenges", style = MaterialTheme.typography.titleLarge)
        weekly.forEach { challenge ->
            val progress = when (challenge.kind) {
                "correct" -> state.correctGuesses
                "streak", "frenzy" -> state.bestStreak.toLong()
                "money" -> state.money.value.toLong()
                "guesses" -> state.statistics.totalGuesses
                "prestige" -> state.prestigeCount
                "ultra" -> state.ultraCount
                "playtime" -> state.statistics.playtimeSeconds
                else -> 0L
            }
            val completed = challenge.id in state.completedChallenges
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(challenge.kind.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                    Text("Progress: ${progress.coerceAtLeast(0)} / ${challenge.target}")
                    Text("Reward: +${challenge.rewardNebula} Nebula", color = MaterialTheme.colorScheme.primary)
                    Button(onClick = { onClaimChallenge(challenge.id, challenge.rewardNebula) }, enabled = progress >= challenge.target && !completed) { Text(if (completed) "Completed" else if (progress >= challenge.target) "Claim" else "In Progress") }
                }
            }
        }
    }
}
