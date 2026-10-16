package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.desktop.DesktopGuessingBot
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyCatalog
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyRepository
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyTransactions
import com.jarrlyyy.guessthenumber.desktop.economy.EconomyTransaction
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveStore
import com.jarrlyyy.guessthenumber.desktop.persistence.defaultDesktopSaveDirectory
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameStatus
import com.jarrlyyy.guessthenumber.shared.ui.SharedGuessFeedbackCard
import com.jarrlyyy.guessthenumber.shared.ui.SharedGuessFeedbackIcon
import com.jarrlyyy.guessthenumber.shared.ui.toSharedFeedbackIcon

/** Windows presentation backed by the production engine, economy rules and desktop save adapter. */
@Composable
fun ProductionGuessGameScreen(
    section: DesktopGameSection = DesktopGameSection.PLAY,
    onOpenAbout: () -> Unit = {},
    onOpenPrestige: () -> Unit = {},
    onOpenUltra: () -> Unit = {},
    onOpenTalent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val engine = remember { GameEngine() }
    val autoClicker = remember { DesktopGuessingBot() }
    val saveStore = remember { DesktopSaveStore(defaultDesktopSaveDirectory()) }
    val catalogResult = remember { runCatching { DesktopEconomyCatalog.load() } }
    val catalog = catalogResult.getOrNull()
    val talentCatalogResult = remember { runCatching { DesktopEconomyRepository().talents() } }
    val talentCatalog = talentCatalogResult.getOrNull()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<GameState?>(null) }
    var guessText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("Loading your saved game…") }
    var feedbackStatus by remember { mutableStateOf(GuessGameStatus.READY) }
    var isSaving by remember { mutableStateOf(false) }
    var autoClickerFailurePaused by remember { mutableStateOf(false) }
    var pendingReset by remember { mutableStateOf<String?>(null) }
    val latestSaving by rememberUpdatedState(isSaving)

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { saveStore.load(1) }
        if (loaded != null) {
            autoClicker.reset(loaded.currentRangeMin, loaded.currentRangeMax)
            state = loaded
            feedback = "Saved game loaded."
        } else {
            val fresh = GameState(
                targetNumber = Random.nextLong(1L, 101L),
                currentRangeMin = 1L,
                currentRangeMax = 100L
            )
            val created = withContext(Dispatchers.IO) { saveStore.save(1, fresh) }
            if (created) {
                autoClicker.reset(fresh.currentRangeMin, fresh.currentRangeMax)
                state = fresh
                feedback = "New game created."
            } else {
                feedback = "Could not create the desktop save. Try again."
            }
        }
    }

    fun commitTransaction(transform: (GameState) -> EconomyTransaction) {
        val before = state ?: return
        if (isSaving) return
        isSaving = true
        scope.launch {
            val transaction = transform(before)
            if (!transaction.accepted) {
                if (transaction.state != before) {
                    val refreshedSave = withContext(Dispatchers.IO) { saveStore.save(1, transaction.state) }
                    if (refreshedSave) {
                        state = transaction.state
                        feedback = transaction.message
                    } else {
                        feedback = "Could not save the refreshed state. Your previous saved state is still intact."
                    }
                } else {
                    feedback = transaction.message
                }
                isSaving = false
                return@launch
            }
            val progressedState = DesktopEconomyTransactions.evaluateAchievements(transaction.state)
            val saved = withContext(Dispatchers.IO) { saveStore.save(1, progressedState) }
            if (saved) {
                state = progressedState
                if (
                    before.activeMutators != progressedState.activeMutators ||
                    before.currentRangeMin != progressedState.currentRangeMin ||
                    before.currentRangeMax != progressedState.currentRangeMax
                ) {
                    autoClicker.reset(progressedState.currentRangeMin, progressedState.currentRangeMax)
                }
                if (before.autoClickerActive != progressedState.autoClickerActive) {
                    autoClickerFailurePaused = false
                }
                val newAchievements = progressedState.achievements - before.achievements
                feedback = if (newAchievements.isNotEmpty()) {
                    "${transaction.message} Achievement unlocked: ${newAchievements.sorted().joinToString()}."
                } else transaction.message
            } else {
                feedback = "Could not save that change. Your previous saved state is still intact."
            }
            isSaving = false
        }
    }

    val currentState = state

    LaunchedEffect(currentState) {
        val snapshot = currentState ?: return@LaunchedEffect
        if (!snapshot.autoClickerActive || autoClickerFailurePaused || "auto_clicker_basic" !in snapshot.shopPurchases) {
            return@LaunchedEffect
        }
        delay(1_000L)
        while (latestSaving) delay(100L)
        if (state != snapshot) return@LaunchedEffect

        val guess = autoClicker.nextGuess(snapshot.currentRangeMin, snapshot.currentRangeMax)
        isSaving = true
        val result = engine.processGuess(snapshot, guess)
        val correct = result.feedback == GuessFeedback.CORRECT
        val bonusAdjustedState = DesktopEconomyTransactions.applyGuessBonuses(
            before = snapshot,
            after = result.newState,
            correct = correct
        )
        val finalState = DesktopEconomyTransactions.evaluateAchievements(
            DesktopEconomyTransactions.recordGuessProgress(
                before = snapshot,
                after = bonusAdjustedState,
                correct = correct,
                today = LocalDate.now().toString(),
                lootRoll = if (correct) Random.nextInt(1000) else null
            )
        )
        val saved = withContext(Dispatchers.IO) { saveStore.save(1, finalState) }
        if (saved) {
            autoClicker.observeGuess(guess, result.feedback)
            if (result.feedback == GuessFeedback.CORRECT) {
                autoClicker.reset(finalState.currentRangeMin, finalState.currentRangeMax)
            }
            state = finalState
            feedbackStatus = desktopFeedbackStatus(result.feedback, "mut_blind" in finalState.activeMutators)
            feedback = when (result.feedback) {
                GuessFeedback.TOO_LOW -> "Auto Clicker: too low."
                GuessFeedback.TOO_HIGH -> "Auto Clicker: too high."
                GuessFeedback.CORRECT -> "Auto Clicker solved the round and earned ${finalState.money - snapshot.money} Money." +
                    if (finalState.lootEventId > snapshot.lootEventId) " Treasure drop: ${finalState.lastLootDropTier} chest!" else ""
            }
        } else {
            feedbackStatus = GuessGameStatus.READY
            feedback = "Auto Clicker paused because the save failed. Your previous saved state is still intact."
            autoClickerFailurePaused = true
        }
        isSaving = false
    }

    Card(modifier = modifier.fillMaxSize()) {
        if (currentState == null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(feedback)
            }
            return@Card
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Guess The Number", style = MaterialTheme.typography.headlineMedium)
            Text("Find the hidden number between ${currentState.currentRangeMin} and ${currentState.currentRangeMax}.")
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                CurrencySummary("Money", currentState.money.toString())
                CurrencySummary("Prestige", currentState.prestige.toString())
                CurrencySummary("Ultra", currentState.ultra.toString())
                CurrencySummary("Nebula", currentState.nebula.toString())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                CurrencySummary("Attempts", currentState.attempts.toString())
                CurrencySummary("Correct", currentState.correctGuesses.toString())
                CurrencySummary("Streak", currentState.streak.toString())
            }
            if (section == DesktopGameSection.PLAY) {
            Text(
                "Daily progress: guesses ${currentState.dailyQuestGuesses}/10 · correct ${currentState.dailyQuestCorrectGuesses}/5 · best streak ${currentState.dailyQuestBestStreak}/5 · upgrades ${currentState.dailyQuestUpgradePurchases}/3",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Treasure chests: Common ${currentState.lootCommonChests} · Rare ${currentState.lootRareChests} · Epic ${currentState.lootEpicChests}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val submitGuess = {
                    val guess = guessText.toLongOrNull()
                    if (guess == null) {
                        feedbackStatus = GuessGameStatus.INVALID_INPUT
                        feedback = "Enter a whole number first."
                    } else if (guess !in currentState.currentRangeMin..currentState.currentRangeMax) {
                        feedbackStatus = GuessGameStatus.INVALID_INPUT
                        feedback = "Choose a number within the displayed range."
                    } else if (!isSaving) {
                        isSaving = true
                        val result = engine.processGuess(currentState, guess)
                        val correct = result.feedback == GuessFeedback.CORRECT
                        val bonusAdjustedState = DesktopEconomyTransactions.applyGuessBonuses(
                            before = currentState,
                            after = result.newState,
                            correct = correct
                        )
                        val finalState = DesktopEconomyTransactions.evaluateAchievements(
                            DesktopEconomyTransactions.recordGuessProgress(
                                before = currentState,
                                after = bonusAdjustedState,
                                correct = correct,
                                today = LocalDate.now().toString(),
                                lootRoll = if (correct) Random.nextInt(1000) else null
                            )
                        )
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) { saveStore.save(1, finalState) }
                            if (saved) {
                                autoClicker.observeGuess(guess, result.feedback)
                                if (result.feedback == GuessFeedback.CORRECT) {
                                    autoClicker.reset(finalState.currentRangeMin, finalState.currentRangeMax)
                                }
                                autoClickerFailurePaused = false
                                state = finalState
                                feedbackStatus = desktopFeedbackStatus(result.feedback, "mut_blind" in finalState.activeMutators)
                                feedback = when (result.feedback) {
                                    GuessFeedback.TOO_LOW -> if ("mut_blind" in finalState.activeMutators) {
                                        "Guess recorded. Blind mutator hides direction hints."
                                    } else "Too low. Try again!"
                                    GuessFeedback.TOO_HIGH -> if ("mut_blind" in finalState.activeMutators) {
                                        "Guess recorded. Blind mutator hides direction hints."
                                    } else "Too high. Try again!"
                                    GuessFeedback.CORRECT -> "Correct! You earned ${finalState.money - currentState.money} Money." +
                                        if (finalState.lootEventId > currentState.lootEventId) " Treasure drop: ${finalState.lastLootDropTier} chest!" else ""
                                }
                                guessText = ""
                            } else {
                                feedbackStatus = GuessGameStatus.READY
                                feedback = "Could not save that guess. Your previous saved state is still intact."
                            }
                            isSaving = false
                        }
                    }
            }
            com.jarrlyyy.guessthenumber.shared.ui.SharedGuessInputField(
                value = guessText,
                onValueChange = { input -> guessText = input.filter(Char::isDigit).take(18) },
                onSubmit = { submitGuess() },
                minNumber = currentState.currentRangeMin,
                maxNumber = currentState.currentRangeMax,
                modifier = Modifier.fillMaxWidth(),
                label = "Your guess",
                enabled = !isSaving,
                submitOnEnter = true
            )
            Button(
                onClick = { submitGuess() },
                enabled = guessText.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "Saving…" else "Submit guess")
            }
            SharedGuessFeedbackCard(
                status = feedbackStatus,
                message = feedback,
                leadingContent = { contentColor ->
                    val icon = when (feedbackStatus.toSharedFeedbackIcon()) {
                        SharedGuessFeedbackIcon.TIP -> "💡"
                        SharedGuessFeedbackIcon.TOO_LOW -> "📉"
                        SharedGuessFeedbackIcon.TOO_HIGH -> "📈"
                        SharedGuessFeedbackIcon.CORRECT -> "✨"
                    }
                    Text(icon, color = contentColor, style = MaterialTheme.typography.titleLarge)
                },
                reducedMotion = currentState.settings.reducedMotion
            )

            SectionTitle("Mutators")
            val mutators = listOf(
                Triple("mut_hardcore", "Hardcore", "Doubles the guessing range and rewards."),
                Triple("mut_speed", "Speed", "Triples correct-guess rewards."),
                Triple("mut_blind", "Blind", "Quintuples rewards and hides too-low/too-high hints."),
                Triple("mut_tax", "Tax", "Multiplies rewards and applies the shared incorrect-guess tax.")
            )
            mutators.forEach { (id, name, description) ->
                val active = id in currentState.activeMutators
                EconomyActionCard(
                    title = name,
                    description = description,
                    detail = if (active) "Active · Reward multiplier is applied by the shared game engine." else "Inactive",
                    action = if (active) "Disable" else "Enable",
                    enabled = !isSaving
                ) {
                    commitTransaction { DesktopEconomyTransactions.setMutatorActive(it, id, !active, engine) }
                }
            }

            SectionTitle("Daily quests")
            val dailyQuests = listOf(
                Triple("daily_guesses_10", "Make 10 guesses", currentState.dailyQuestGuesses >= 10),
                Triple("daily_correct_5", "Get 5 correct guesses", currentState.dailyQuestCorrectGuesses >= 5),
                Triple("daily_streak_5", "Reach a streak of 5", currentState.dailyQuestBestStreak >= 5),
                Triple("daily_upgrades_3", "Buy 3 upgrades", currentState.dailyQuestUpgradePurchases >= 3)
            )
            dailyQuests.forEach { (id, title, complete) ->
                val claimed = id in currentState.claimedDailyQuests
                val progress = when (id) {
                    "daily_guesses_10" -> "${currentState.dailyQuestGuesses.coerceAtMost(10)} / 10 guesses"
                    "daily_correct_5" -> "${currentState.dailyQuestCorrectGuesses.coerceAtMost(5)} / 5 correct guesses"
                    "daily_streak_5" -> "${currentState.dailyQuestBestStreak.coerceAtMost(5)} / 5 best streak"
                    else -> "${currentState.dailyQuestUpgradePurchases.coerceAtMost(3)} / 3 upgrades"
                }
                val reward = when (id) {
                    "daily_guesses_10" -> "Reward: 10,000 Money"
                    "daily_correct_5" -> "Reward: 25 Nebula"
                    "daily_streak_5" -> "Reward: 50 Nebula"
                    else -> "Reward: 35 Nebula"
                }
                EconomyActionCard(
                    title = title,
                    description = progress,
                    detail = reward,
                    action = if (claimed) "Claimed" else if (complete) "Claim" else "In progress",
                    enabled = !isSaving && complete && !claimed
                ) {
                    commitTransaction { DesktopEconomyTransactions.claimDailyQuest(it, id, LocalDate.now().toString()) }
                }
            }

            SectionTitle("Achievements")
            Text(
                "${currentState.achievements.size} / 27 unlocked · milestone rewards are credited automatically once",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (currentState.achievements.isNotEmpty()) {
                Text(
                    currentState.achievements.sorted().joinToString(" · ") { it.replace('_', ' ') },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            SectionTitle("World progression")
            DesktopEconomyTransactions.worldProgress(currentState).forEach { world ->
                EconomyActionCard(
                    title = world.name,
                    description = world.requirement,
                    detail = when {
                        world.selected -> "Currently selected"
                        world.unlocked -> "Unlocked"
                        else -> "Locked until requirements are met"
                    },
                    action = when {
                        world.selected -> "Selected"
                        world.unlocked -> "Select"
                        world.canUnlock -> "Unlock"
                        else -> "Locked"
                    },
                    enabled = !isSaving && !world.selected && (world.unlocked || world.canUnlock)
                ) {
                    commitTransaction {
                        if (world.unlocked) DesktopEconomyTransactions.selectWorld(it, world.id)
                        else DesktopEconomyTransactions.unlockWorld(it, world.id)
                    }
                }
            }

            if ("auto_clicker_basic" in currentState.shopPurchases) {
                Button(
                    onClick = { commitTransaction { DesktopEconomyTransactions.setAutoClickerActive(it, !it.autoClickerActive) } },
                    enabled = !isSaving
                ) { Text(if (currentState.autoClickerActive) "Pause auto-clicker" else "Resume auto-clicker") }
            }

            }

            if (section == DesktopGameSection.UPGRADES) {
                SectionTitle("Upgrades")

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Available Money", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            AnimatedContent(targetState = currentState.money.format(), label = "UpgradeMoneyAnimation") { moneyText ->
                                Text("$moneyText Money", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text("🛒", style = MaterialTheme.typography.headlineMedium)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Buy Multiplier:", style = MaterialTheme.typography.titleMedium)
                    var multiplierMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        OutlinedButton(
                            onClick = { multiplierMenuExpanded = true },
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            enabled = !isSaving
                        ) {
                            Text("${currentState.buyMultiplier}x", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("▾", style = MaterialTheme.typography.titleMedium)
                        }
                        DropdownMenu(
                            expanded = multiplierMenuExpanded,
                            onDismissRequest = { multiplierMenuExpanded = false }
                        ) {
                            listOf("1", "10", "100", "MAX").forEach { multiplier ->
                                DropdownMenuItem(
                                    text = { Text("$multiplier x", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        commitTransaction { DesktopEconomyTransactions.setBuyMultiplier(it, multiplier) }
                                        multiplierMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider()

                if (catalog == null) {
                    Text(
                        "Economy catalog could not be loaded: ${catalogResult.exceptionOrNull()?.message ?: "unknown error"}",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    catalog.upgrades.forEach { upgrade ->
                        val level = currentState.upgradeLevels[upgrade.id] ?: 0
                        val cost = desktopUpgradeDisplayCost(
                            upgrade = upgrade,
                            level = level,
                            multiplier = currentState.buyMultiplier,
                            money = currentState.money
                        )
                        DesktopUpgradeParityCard(
                            upgrade = upgrade,
                            level = level,
                            cost = cost,
                            canAfford = currentState.money >= cost,
                            isSaving = isSaving,
                            reducedMotion = currentState.settings.reducedMotion,
                            multiplier = currentState.buyMultiplier
                        ) {
                            commitTransaction { DesktopEconomyTransactions.buyUpgrade(it, upgrade) }
                        }
                    }
                }
            }

            if (section == DesktopGameSection.PRESTIGE) {
            SectionTitle("Prestige")
                val prestigeRequirement = engine.calculatePrestigeRequirement(currentState.prestigeCount)
                val prestigeReward = engine.calculatePrestigeReward(currentState.money, currentState.prestigeCount)
                Text("Requirement: $prestigeRequirement Money · Estimated reward: $prestigeReward Prestige")
                Button(
                    onClick = { pendingReset = "prestige" },
                    enabled = !isSaving && currentState.money >= prestigeRequirement && prestigeReward > BigNumber.ZERO
                ) { Text("Prestige reset") }

                catalog?.prestigeUpgrades?.forEach { upgrade ->
                    val level = currentState.prestigeUpgradeLevels[upgrade.id] ?: 0
                    val cost = DesktopEconomyTransactions.upgradeBatchCost(upgrade, level, currentState.buyMultiplier, currentState.prestige)
                    EconomyActionCard(
                        title = upgrade.name,
                        description = upgrade.description,
                        detail = "Level $level / ${upgrade.maxLevel} · Batch cost: $cost Prestige",
                        action = if (level >= upgrade.maxLevel) "MAX" else "Buy ×${currentState.buyMultiplier}",
                        enabled = !isSaving && level < upgrade.maxLevel && cost > BigNumber.ZERO && currentState.prestige >= cost
                    ) {
                        commitTransaction { DesktopEconomyTransactions.buyPrestigeUpgrade(it, upgrade) }
                    }
                }

                }

                if (section == DesktopGameSection.ULTRA) {
                SectionTitle("Ultra")
                val ultraRequirement = engine.calculateUltraRequirement(currentState.ultraCount)
                val ultraReward = engine.calculateUltraReward(currentState.money, currentState.ultraCount)
                Text("Requirement: $ultraRequirement Money and 1,000 Prestige · Estimated reward: $ultraReward Ultra")
                Button(
                    onClick = { pendingReset = "ultra" },
                    enabled = !isSaving && currentState.money >= ultraRequirement &&
                        currentState.prestige >= BigNumber(1_000) && ultraReward > BigNumber.ZERO
                ) { Text("Ultra reset") }

                catalog?.ultraUpgrades?.forEach { upgrade ->
                    val level = currentState.ultraUpgradeLevels[upgrade.id] ?: 0
                    val cost = DesktopEconomyTransactions.upgradeBatchCost(upgrade, level, currentState.buyMultiplier, currentState.ultra)
                    EconomyActionCard(
                        title = upgrade.name,
                        description = upgrade.description,
                        detail = "Level $level / ${upgrade.maxLevel} · Batch cost: $cost Ultra",
                        action = if (level >= upgrade.maxLevel) "MAX" else "Buy ×${currentState.buyMultiplier}",
                        enabled = !isSaving && level < upgrade.maxLevel && cost > BigNumber.ZERO && currentState.ultra >= cost
                    ) {
                        commitTransaction { DesktopEconomyTransactions.buyUltraUpgrade(it, upgrade) }
                    }
                }

                }

            if (section == DesktopGameSection.SHOP) {
            SectionTitle("Nebula shop")
                catalog?.shopItems?.forEach { item ->
                    val purchased = item.id in currentState.shopPurchases
                    val supported = item.id in setOf("auto_clicker_basic", "nebula_boost", "golden_guess") ||
                        (item.isCosmetic && item.id.startsWith("cosmetic_"))
                    EconomyActionCard(
                        title = item.name,
                        description = item.description,
                        detail = when (item.id) {
                            "nebula_boost" -> "Effect: doubles future Nebula rewards · Cost: ${item.nebulaCost} Nebula"
                            "golden_guess" -> "Effect: guarantees critical rewards on every tenth lifetime guess · Cost: ${item.nebulaCost} Nebula"
                            else -> "Cost: ${item.nebulaCost} Nebula"
                        },
                        action = if (purchased) "Owned" else if (supported) "Buy" else "Unavailable",
                        enabled = !isSaving && supported && !purchased && currentState.nebula >= BigNumber(item.nebulaCost)
                    ) {
                        commitTransaction { DesktopEconomyTransactions.buyShopItem(it, item) }
                    }
                }

                }

            if (section == DesktopGameSection.TALENT) {
            SectionTitle("Talents")
                if (talentCatalog == null) {
                    Text("Talent catalog could not be loaded: ${talentCatalogResult.exceptionOrNull()?.message ?: "unknown error"}")
                } else {
                    talentCatalog.forEach { talent ->
                        val owned = talent.id in currentState.prestigeShopPurchases
                        val parentOwned = talent.requiredParentId == null ||
                            talent.requiredParentId in currentState.prestigeShopPurchases
                        EconomyActionCard(
                            title = talent.name,
                            description = talent.description,
                            detail = when {
                                owned -> "Unlocked"
                                !parentOwned -> "Cost: ${talent.cost} Prestige · Requires ${talent.requiredParentId}"
                                else -> "Cost: ${talent.cost} Prestige"
                            },
                            action = if (owned) "Owned" else if (!parentOwned) "Locked" else "Unlock",
                            enabled = !isSaving && !owned && parentOwned &&
                                currentState.prestige >= BigNumber(talent.cost)
                        ) {
                            commitTransaction {
                                DesktopEconomyTransactions.buyTalent(
                                    it, talent.id, talent.cost, talent.requiredParentId
                                )
                            }
                        }
                    }
                }
            }

            }

            if (section == DesktopGameSection.MORE) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("More", style = MaterialTheme.typography.titleLarge)
                        Text("Choose a destination.")
                        Button(onClick = onOpenPrestige) { Text("Prestige") }
                        Button(onClick = onOpenUltra) { Text("Ultra") }
                        Button(onClick = onOpenTalent) { Text("Talent Web") }
                        Button(onClick = onOpenAbout) { Text("About and updates") }
                    }
                }
            }

            Text(
                "Your Windows run is saved locally after each accepted guess or economy transaction. A backup is kept for recovery if the primary save is damaged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val resetKind = pendingReset
    if (resetKind != null) {
        val isPrestige = resetKind == "prestige"
        AlertDialog(
            onDismissRequest = { if (!isSaving) pendingReset = null },
            title = { Text(if (isPrestige) "Confirm Prestige reset?" else "Confirm Ultra reset?") },
            text = {
                Text(
                    if (isPrestige) {
                        "This resets Money and regular upgrade levels, then grants Prestige. Your Prestige upgrades and other preserved progression stay."
                    } else {
                        "This resets Money, Prestige and regular/Prestige upgrade levels, then grants Ultra. Your Ultra upgrades and other preserved progression stay."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingReset = null
                        if (isPrestige) {
                            commitTransaction { DesktopEconomyTransactions.prestigeReset(it, engine) }
                        } else {
                            commitTransaction { DesktopEconomyTransactions.ultraReset(it, engine) }
                        }
                    },
                    enabled = !isSaving
                ) { Text(if (isPrestige) "Reset for Prestige" else "Reset for Ultra") }
            },
            dismissButton = {
                TextButton(onClick = { pendingReset = null }, enabled = !isSaving) { Text("Cancel") }
            }
        )
    }
}


@Composable
private fun DesktopUpgradeParityCard(
    upgrade: UpgradeDef,
    level: Int,
    cost: BigNumber,
    canAfford: Boolean,
    isSaving: Boolean,
    reducedMotion: Boolean,
    multiplier: String,
    onBuy: () -> Unit
) {
    var previousLevel by remember(upgrade.id) { mutableStateOf(level) }
    var purchaseHighlight by remember(upgrade.id) { mutableStateOf(false) }
    LaunchedEffect(level) {
        val oldLevel = previousLevel
        previousLevel = level
        if (level > oldLevel) {
            purchaseHighlight = true
            delay(if (reducedMotion) 350L else 650L)
            purchaseHighlight = false
        }
    }

    val isMaxed = level >= upgrade.maxLevel
    val targetColor = if (purchaseHighlight) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val cardColor = if (reducedMotion) targetColor else animateColorAsState(
        targetValue = targetColor,
        label = "upgradePurchaseColor_${upgrade.id}"
    ).value

    Surface(
        modifier = Modifier.fillMaxWidth().then(
            if (reducedMotion) Modifier else Modifier.animateContentSize()
        ),
        shape = MaterialTheme.shapes.large,
        color = cardColor,
        tonalElevation = if (purchaseHighlight && !reducedMotion) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(upgrade.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(upgrade.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    val maxLevelLabel = if (upgrade.maxLevel >= 999999) "MAX" else upgrade.maxLevel.toString()
                    AnimatedContent(targetState = level, label = "upgradeLevel_${upgrade.id}") { animatedLevel ->
                        Text(
                            text = "Level: $animatedLevel / $maxLevelLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!isMaxed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Cost (${multiplier}x): ${cost.format()} Money",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onBuy,
                enabled = !isSaving && !isMaxed && canAfford,
                shape = MaterialTheme.shapes.medium,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isMaxed) "MAX" else "Buy")
            }
        }
    }
}

private fun desktopUpgradeDisplayCost(
    upgrade: UpgradeDef,
    level: Int,
    multiplier: String,
    money: BigNumber
): BigNumber {
    val base = BigNumber(upgrade.baseCost)
    val costMultiplier = BigNumber(upgrade.costMultiplier)
    val effectiveLevels = when (multiplier) {
        "10" -> minOf(10, (upgrade.maxLevel - level).coerceAtLeast(0))
        "100" -> minOf(100, (upgrade.maxLevel - level).coerceAtLeast(0))
        "MAX" -> {
            var count = 0
            var total = BigNumber.ZERO
            var currentLevel = level
            while (currentLevel < upgrade.maxLevel) {
                val nextCost = base * costMultiplier.pow(currentLevel)
                if (money >= total + nextCost) {
                    total += nextCost
                    count++
                    currentLevel++
                } else {
                    break
                }
            }
            if (count == 0 && level < upgrade.maxLevel && money >= base * costMultiplier.pow(level)) 1 else count
        }
        else -> minOf(1, (upgrade.maxLevel - level).coerceAtLeast(0))
    }
    var totalCost = BigNumber.ZERO
    for (offset in 0 until maxOf(1, effectiveLevels)) {
        totalCost += base * costMultiplier.pow(level + offset)
    }
    return totalCost
}

@Composable
private fun CurrencySummary(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
}

@Composable
private fun EconomyActionCard(
    title: String,
    description: String,
    detail: String,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClick, enabled = enabled) { Text(action) }
        }
    }
}


internal fun desktopFeedbackStatus(feedback: GuessFeedback, blindMutatorActive: Boolean): GuessGameStatus = when {
    blindMutatorActive && feedback != GuessFeedback.CORRECT -> GuessGameStatus.READY
    feedback == GuessFeedback.TOO_LOW -> GuessGameStatus.TOO_LOW
    feedback == GuessFeedback.TOO_HIGH -> GuessGameStatus.TOO_HIGH
    feedback == GuessFeedback.CORRECT -> GuessGameStatus.CORRECT
    else -> GuessGameStatus.READY
}
