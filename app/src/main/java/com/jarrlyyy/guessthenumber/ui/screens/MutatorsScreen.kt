package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold
import java.time.LocalDate
import java.time.temporal.WeekFields

data class MutatorDef(val id: String, val rewardMultiplier: Double)
data class WeeklyChallengeDef(val id: String, val kind: String, val target: Long, val rewardNebula: Long)

private fun weeklyChallenges(week: Int): List<WeeklyChallengeDef> {
    val pool = listOf(
        WeeklyChallengeDef("correct", "correct", 75, 75),
        WeeklyChallengeDef("streak", "streak", 15, 90),
        WeeklyChallengeDef("money", "money", 10_000_000, 100),
        WeeklyChallengeDef("guesses", "guesses", 250, 80),
        WeeklyChallengeDef("prestige", "prestige", 2, 125),
        WeeklyChallengeDef("ultra", "ultra", 1, 175),
        WeeklyChallengeDef("frenzy", "frenzy", 25, 110),
        WeeklyChallengeDef("playtime", "playtime", 3_600, 95)
    )
    val offset = (week - 1) % pool.size
    return List(3) { pool[(offset + it * 2) % pool.size] }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutatorsScreen(
    state: GameState,
    onSelectMutator: (String, Boolean) -> Unit,
    onActivateChallengeBuilder: (Set<String>) -> Unit,
    onCompleteChallenge: (String, Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configRepository = remember(state.settings.locale) { JsonConfigRepository(context, state.settings.locale) }
    val locale = configRepository.localeManager

    val mutators = listOf(
        MutatorDef("mut_hardcore", 2.0),
        MutatorDef("mut_speed", 1.5),
        MutatorDef("mut_blind", 5.0),
        MutatorDef("mut_tax", 10.0)
    )
    var builderSelection by remember(state.activeMutators) { mutableStateOf(state.activeMutators) }
    var challengeKind by remember { mutableStateOf("correct") }
    var targetText by remember { mutableStateOf("25") }

    val today = LocalDate.now()
    val week = today.get(WeekFields.ISO.weekOfWeekBasedYear())
    val weekYear = today.get(WeekFields.ISO.weekBasedYear())
    val challenges = weeklyChallenges(week).map {
        it.copy(id = "weekly_" + weekYear + "_" + week + "_" + it.kind)
    }

    val builderTarget = targetText.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
    val builderId = "builder_${challengeKind}_${builderTarget}_${builderSelection.sorted().joinToString("_")}"
    val builderReward = 50L + (builderSelection.size * 50L)
    val builderProgress = when (challengeKind) {
        "correct" -> state.correctGuesses
        "streak" -> state.bestStreak.toLong()
        "guesses" -> state.statistics.totalGuesses
        else -> state.money.value.toLong()
    }
    val builderEligible = builderProgress >= builderTarget
    val builderCompleted = builderId in state.completedChallenges

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locale.getString("mutators_title", "Mutators & Weekly Challenges")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = locale.getString("back", "Back"))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(locale.getString("mutators_gameplay", "Gameplay Mutators"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(mutators) { mutator ->
                val active = mutator.id in state.activeMutators
                val name = locale.getString("mutator_${mutator.id}_name", mutator.id)
                val description = locale.getString("mutator_${mutator.id}_desc", "")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                            Text(description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(locale.getString("mutator_reward_format", "Reward Multiplier: %sx", mutator.rewardMultiplier), color = MoneyGold, fontSize = 14.sp)
                        }
                        Switch(checked = active, onCheckedChange = { onSelectMutator(mutator.id, it) })
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(locale.getString("challenge_builder_title", "Challenge Builder"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                Text(locale.getString("challenge_builder_desc", "Combine mutators and build a custom goal."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                Text(locale.getString("challenge_builder_mutators", "Builder Mutators"), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mutators.forEach { mutator ->
                        FilterChip(
                            selected = mutator.id in builderSelection,
                            onClick = {
                                builderSelection = builderSelection.toMutableSet().apply {
                                    if (!add(mutator.id)) remove(mutator.id)
                                }
                            },
                            label = { Text(locale.getString("mutator_${mutator.id}_name", mutator.id)) }
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("correct", "streak", "guesses", "money")) { kind ->
                        FilterChip(
                            selected = challengeKind == kind,
                            onClick = {
                                challengeKind = kind
                                targetText = if (kind == "correct") "25" else if (kind == "streak") "10" else if (kind == "guesses") "100" else "1000000"
                            },
                            label = { Text(locale.getString("builder_kind_${kind}", kind)) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter(Char::isDigit).take(12) },
                    label = { Text(locale.getString("challenge_builder_target", "Target")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(locale.getString("challenge_builder_preview", "Custom Challenge"), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(locale.getString("challenge_builder_progress", "Progress: %s / %s", builderProgress, builderTarget), style = MaterialTheme.typography.bodyMedium)
                        Text(locale.getString("challenge_builder_reward", "Reward: +%s Nebula", builderReward), color = MoneyGold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onActivateChallengeBuilder(builderSelection) }) {
                                Text(locale.getString("challenge_builder_activate", "Activate Build"))
                            }
                            Button(onClick = { onCompleteChallenge(builderId, builderReward) }, enabled = builderEligible && !builderCompleted) {
                                Text(if (builderCompleted) locale.getString("completed", "Completed") else if (builderEligible) locale.getString("claim", "Claim") else locale.getString("in_progress", "In Progress"))
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(locale.getString("weekly_challenges_title", "Weekly Challenges"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(challenges) { challenge ->
                val completed = challenge.id in state.completedChallenges
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
                val eligible = progress >= challenge.target
                val name = locale.getString("weekly_${challenge.kind}_name", challenge.kind)
                val description = if (challenge.kind == "money") locale.getString("weekly_money_desc", "").format(BigNumber(challenge.target).format())
                else locale.getString("weekly_${challenge.kind}_desc", "").format(challenge.target)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                        Text(description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(locale.getString("weekly_progress_format", "Progress: %s / %s", progress, challenge.target), fontSize = 13.sp)
                        Text(locale.getString("weekly_reward_format", "Reward: +%s Nebula", challenge.rewardNebula), color = MoneyGold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onCompleteChallenge(challenge.id, challenge.rewardNebula) }, enabled = eligible && !completed) {
                            Text(if (completed) locale.getString("completed", "Completed") else if (eligible) locale.getString("claim", "Claim") else locale.getString("in_progress", "In Progress"))
                        }
                    }
                }
            }
        }
    }
}
