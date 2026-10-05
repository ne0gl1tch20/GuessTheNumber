package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.WeekFields
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold

data class MutatorDef(
    val id: String,
    val name: String,
    val description: String,
    val rewardMultiplier: Double
)

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
    onSelectMutator: (String) -> Unit,
    onCompleteChallenge: (String, Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configRepository = remember(state.settings.locale) { JsonConfigRepository(context, state.settings.locale) }
    val locale = configRepository.localeManager

    val mutators = listOf(
        MutatorDef("mut_hardcore", "Hardcore Range", "Guessing range is doubled. Rewards ×2.", 2.0),
        MutatorDef("mut_speed", "Hyper Speed", "Auto-clicker speed ×3, but earnings halved.", 1.5),
        MutatorDef("mut_blind", "Blindfolded Oracle", "No lower/higher feedback indicators. Rewards ×5.", 5.0)
    )

    val today = LocalDate.now()
    val week = today.get(WeekFields.ISO.weekOfWeekBasedYear())
    val weekYear = today.get(WeekFields.ISO.weekBasedYear())
    val challenges = weeklyChallenges(week).map {
        it.copy(id = "weekly_" + weekYear + "_" + week + "_" + it.kind)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locale.getString("mutators_title", "Mutators & Weekly Challenges")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = locale.getString("back", "Back"))
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(locale.getString("mutators_gameplay", "Gameplay Mutators"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
            }

            items(mutators) { mutator ->
                val active = state.shopPurchases.contains(mutator.id)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mutator.name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                            Text(mutator.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(locale.getString("mutator_reward_format", "Reward Multiplier: %sx", mutator.rewardMultiplier), color = MoneyGold, fontSize = 14.sp)
                        }
                        Button(
                            onClick = { onSelectMutator(mutator.id) }
                        ) {
                            Text(if (active) locale.getString("disable", "Disable") else locale.getString("activate", "Activate"))
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
                val completed = state.completedChallenges.contains(challenge.id)
                val progress = when (challenge.kind) {
                    "correct" -> state.correctGuesses.toLong()
                    "streak", "frenzy" -> state.bestStreak.toLong()
                    "money" -> state.money.value.toLong()
                    "guesses" -> state.statistics.totalGuesses
                    "prestige" -> state.prestigeCount
                    "ultra" -> state.ultraCount
                    "playtime" -> state.statistics.playtimeSeconds
                    else -> 0L
                }
                val eligible = progress >= challenge.target
                val name = when (challenge.kind) {
                    "correct" -> "Weekly Accuracy"
                    "streak" -> "Streak Climber"
                    "money" -> "Millionaire Sprint"
                    "guesses" -> "Guess Marathon"
                    "prestige" -> "Prestige Push"
                    "ultra" -> "Ultra Expedition"
                    "frenzy" -> "Frenzy Hunt"
                    else -> "Playtime Grinder"
                }
                val description = when (challenge.kind) {
                    "correct" -> "Make %d correct guesses this week."
                    "streak" -> "Reach a best streak of %d this week."
                    "money" -> "Reach %s money."
                    "guesses" -> "Make %d total guesses this week."
                    "prestige" -> "Perform %d Prestige resets."
                    "ultra" -> "Perform %d Ultra reset."
                    "frenzy" -> "Reach a best streak of %d for Frenzy."
                    else -> "Play for %d seconds."
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(locale.getString("weekly_" + challenge.kind + "_name", name), fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (challenge.kind == "money") description.format(BigNumber(challenge.target).format())
                            else description.format(challenge.target),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(locale.getString("weekly_progress_format", "Progress: %s / %s", progress, challenge.target), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(locale.getString("weekly_reward_format", "Reward: +%s Nebula", challenge.rewardNebula), color = MoneyGold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onCompleteChallenge(challenge.id, challenge.rewardNebula) },
                            enabled = eligible && !completed
                        ) {
                            Text(if (completed) locale.getString("completed", "Completed") else if (eligible) locale.getString("claim", "Claim") else locale.getString("in_progress", "In Progress"))
                        }
                    }
                }
            }
