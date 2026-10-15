package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import kotlin.random.Random

/** Windows presentation backed by the production engine, economy rules and desktop save adapter. */
@Composable
fun ProductionGuessGameScreen(modifier: Modifier = Modifier) {
    val engine = remember { GameEngine() }
    val autoClicker = remember { DesktopGuessingBot() }
    val saveStore = remember { DesktopSaveStore(defaultDesktopSaveDirectory()) }
    val catalogResult = remember { runCatching { DesktopEconomyCatalog.load() } }
    val catalog = catalogResult.getOrNull()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<GameState?>(null) }
    var guessText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("Loading your saved game…") }
    var isSaving by remember { mutableStateOf(false) }
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
                feedback = transaction.message
                isSaving = false
                return@launch
            }
            val saved = withContext(Dispatchers.IO) { saveStore.save(1, transaction.state) }
            if (saved) {
                state = transaction.state
                feedback = transaction.message
            } else {
                feedback = "Could not save that change. Your previous saved state is still intact."
            }
            isSaving = false
        }
    }

    val currentState = state

    LaunchedEffect(currentState) {
        val snapshot = currentState ?: return@LaunchedEffect
        if (!snapshot.autoClickerActive || "auto_clicker_basic" !in snapshot.shopPurchases) {
            return@LaunchedEffect
        }
        delay(1_000L)
        while (latestSaving) delay(100L)
        if (state != snapshot) return@LaunchedEffect

        val guess = autoClicker.nextGuess(snapshot.currentRangeMin, snapshot.currentRangeMax)
        isSaving = true
        val result = engine.processGuess(snapshot, guess)
        val finalState = DesktopEconomyTransactions.applyGuessBonuses(
            before = snapshot,
            after = result.newState,
            correct = result.feedback == GuessFeedback.CORRECT
        )
        val saved = withContext(Dispatchers.IO) { saveStore.save(1, finalState) }
        if (saved) {
            autoClicker.observeGuess(guess, result.feedback)
            if (result.feedback == GuessFeedback.CORRECT) {
                autoClicker.reset(finalState.currentRangeMin, finalState.currentRangeMax)
            }
            state = finalState
            feedback = when (result.feedback) {
                GuessFeedback.TOO_LOW -> "Auto Clicker: too low."
                GuessFeedback.TOO_HIGH -> "Auto Clicker: too high."
                GuessFeedback.CORRECT -> "Auto Clicker solved the round and earned ${finalState.money - snapshot.money} Money."
            }
        } else {
            feedback = "Auto Clicker paused because the save failed. Your previous saved state is still intact."
            state = snapshot.copy(autoClickerActive = false)
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
            OutlinedTextField(
                value = guessText,
                onValueChange = { input -> guessText = input.filter(Char::isDigit).take(18) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your guess") },
                singleLine = true,
                enabled = !isSaving
            )
            Button(
                onClick = {
                    val guess = guessText.toLongOrNull()
                    if (guess == null) {
                        feedback = "Enter a whole number first."
                    } else if (guess !in currentState.currentRangeMin..currentState.currentRangeMax) {
                        feedback = "Choose a number within the displayed range."
                    } else if (!isSaving) {
                        isSaving = true
                        val result = engine.processGuess(currentState, guess)
                        autoClicker.observeGuess(guess, result.feedback)
                        if (result.feedback == GuessFeedback.CORRECT) {
                            autoClicker.reset(result.newState.currentRangeMin, result.newState.currentRangeMax)
                        }
                        val finalState = DesktopEconomyTransactions.applyGuessBonuses(
                            before = currentState,
                            after = result.newState,
                            correct = result.feedback == GuessFeedback.CORRECT
                        )
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) { saveStore.save(1, finalState) }
                            if (saved) {
                                state = finalState
                                feedback = when (result.feedback) {
                                    GuessFeedback.TOO_LOW -> "Too low. Try again!"
                                    GuessFeedback.TOO_HIGH -> "Too high. Try again!"
                                    GuessFeedback.CORRECT -> "Correct! You earned ${finalState.money - currentState.money} Money."
                                }
                                guessText = ""
                            } else {
                                feedback = "Could not save that guess. Your previous saved state is still intact."
                            }
                            isSaving = false
                        }
                    }
                },
                enabled = guessText.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "Saving…" else "Submit guess")
            }
            Text(feedback, style = MaterialTheme.typography.bodyLarge)
            if ("auto_clicker_basic" in currentState.shopPurchases) {
                Button(
                    onClick = { commitTransaction { DesktopEconomyTransactions.setAutoClickerActive(it, !it.autoClickerActive) } },
                    enabled = !isSaving
                ) { Text(if (currentState.autoClickerActive) "Pause auto-clicker" else "Resume auto-clicker") }
            }

            SectionTitle("Money upgrades")
            if (catalog == null) {
                Text("Economy catalog could not be loaded: ${catalogResult.exceptionOrNull()?.message ?: "unknown error"}")
            } else {
                catalog.upgrades.forEach { upgrade ->
                    val level = currentState.upgradeLevels[upgrade.id] ?: 0
                    val cost = DesktopEconomyTransactions.upgradeCost(upgrade, level)
                    EconomyActionCard(
                        title = upgrade.name,
                        description = upgrade.description,
                        detail = "Level $level / ${upgrade.maxLevel} · Cost: $cost Money",
                        action = if (level >= upgrade.maxLevel) "MAX" else "Buy",
                        enabled = !isSaving && level < upgrade.maxLevel && currentState.money >= cost
                    ) {
                        commitTransaction { DesktopEconomyTransactions.buyUpgrade(it, upgrade) }
                    }
                }

                SectionTitle("Prestige")
                val prestigeRequirement = engine.calculatePrestigeRequirement(currentState.prestigeCount)
                val prestigeReward = engine.calculatePrestigeReward(currentState.money, currentState.prestigeCount)
                Text("Requirement: $prestigeRequirement Money · Estimated reward: $prestigeReward Prestige")
                Button(
                    onClick = { commitTransaction { DesktopEconomyTransactions.prestigeReset(it, engine) } },
                    enabled = !isSaving && currentState.money >= prestigeRequirement && prestigeReward > BigNumber.ZERO
                ) { Text("Prestige reset") }

                catalog.prestigeUpgrades.forEach { upgrade ->
                    val level = currentState.prestigeUpgradeLevels[upgrade.id] ?: 0
                    val cost = DesktopEconomyTransactions.upgradeCost(upgrade, level)
                    EconomyActionCard(
                        title = upgrade.name,
                        description = upgrade.description,
                        detail = "Level $level / ${upgrade.maxLevel} · Cost: $cost Prestige",
                        action = if (level >= upgrade.maxLevel) "MAX" else "Buy",
                        enabled = !isSaving && level < upgrade.maxLevel && currentState.prestige >= cost
                    ) {
                        commitTransaction { DesktopEconomyTransactions.buyPrestigeUpgrade(it, upgrade) }
                    }
                }

                SectionTitle("Ultra")
                val ultraRequirement = engine.calculateUltraRequirement(currentState.ultraCount)
                val ultraReward = engine.calculateUltraReward(currentState.money, currentState.ultraCount)
                Text("Requirement: $ultraRequirement Money and 1,000 Prestige · Estimated reward: $ultraReward Ultra")
                Button(
                    onClick = { commitTransaction { DesktopEconomyTransactions.ultraReset(it, engine) } },
                    enabled = !isSaving && currentState.money >= ultraRequirement &&
                        currentState.prestige >= BigNumber(1_000) && ultraReward > BigNumber.ZERO
                ) { Text("Ultra reset") }

                catalog.ultraUpgrades.forEach { upgrade ->
                    val level = currentState.ultraUpgradeLevels[upgrade.id] ?: 0
                    val cost = DesktopEconomyTransactions.upgradeCost(upgrade, level)
                    EconomyActionCard(
                        title = upgrade.name,
                        description = upgrade.description,
                        detail = "Level $level / ${upgrade.maxLevel} · Cost: $cost Ultra",
                        action = if (level >= upgrade.maxLevel) "MAX" else "Buy",
                        enabled = !isSaving && level < upgrade.maxLevel && currentState.ultra >= cost
                    ) {
                        commitTransaction { DesktopEconomyTransactions.buyUltraUpgrade(it, upgrade) }
                    }
                }

                SectionTitle("Nebula shop")
                catalog.shopItems.forEach { item ->
                    val purchased = item.id in currentState.shopPurchases
                    EconomyActionCard(
                        title = item.name,
                        description = item.description,
                        detail = if (!item.isCosmetic && item.id != "auto_clicker_basic") "Windows effect not implemented yet · Cost: ${item.nebulaCost} Nebula" else "Cost: ${item.nebulaCost} Nebula",
                        action = if (purchased) "Owned" else if (item.isCosmetic || item.id == "auto_clicker_basic") "Buy" else "Unavailable",
                        enabled = !isSaving && (item.isCosmetic || item.id == "auto_clicker_basic") && !purchased && currentState.nebula >= BigNumber(item.nebulaCost)
                    ) {
                        commitTransaction { DesktopEconomyTransactions.buyShopItem(it, item) }
                    }
                }

                SectionTitle("Talents")
                val talents = listOf(
                    Triple("talent_speed_1", "Quick Hands", 10L),
                    Triple("talent_crit_1", "Critical Focus", 25L),
                    Triple("talent_crit_2", "Critical Mastery", 50L),
                    Triple("talent_reward_1", "Reward Resonance", 100L),
                    Triple("talent_speed_2", "Rapid Hands", 75L),
                    Triple("talent_master_1", "Mastery", 250L)
                )
                talents.forEach { (id, name, cost) ->
                    val owned = id in currentState.prestigeShopPurchases
                    EconomyActionCard(
                        title = name,
                        description = id.replace('_', ' '),
                        detail = if (owned) "Unlocked" else "Cost: $cost Prestige",
                        action = if (owned) "Owned" else "Unlock",
                        enabled = !isSaving && !owned && currentState.prestige >= BigNumber(cost)
                    ) {
                        commitTransaction { DesktopEconomyTransactions.buyTalent(it, id, cost) }
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
