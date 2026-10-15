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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyRepository
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyRules
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveStore
import com.jarrlyyy.guessthenumber.desktop.persistence.defaultDesktopSaveDirectory
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Windows presentation backed by production gameplay, shared game configuration and desktop saves. */
@Composable
fun ProductionGuessGameScreen(modifier: Modifier = Modifier) {
    val engine = remember { GameEngine() }
    val saveStore = remember { DesktopSaveStore(defaultDesktopSaveDirectory()) }
    val economy = remember { DesktopEconomyRepository() }
    val upgrades = remember { economy.upgrades() }
    val prestigeUpgrades = remember { economy.prestigeUpgrades() }
    val ultraUpgrades = remember { economy.ultraUpgrades() }
    val shopItems = remember { economy.shopItems() }
    val prestigeShopItems = remember { economy.prestigeShopItems() }
    val ultraShopItems = remember { economy.ultraShopItems() }
    val talents = remember { economy.talents() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<GameState?>(null) }
    var guessText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("Loading your saved game…") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { saveStore.load(1) }
        if (loaded != null) {
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
                state = fresh
                feedback = "New game created."
            } else {
                feedback = "Could not create the desktop save. Try again."
            }
        }
    }

    fun persist(next: GameState, successMessage: String) {
        if (isSaving) return
        isSaving = true
        scope.launch {
            val saved = withContext(Dispatchers.IO) { saveStore.save(1, next) }
            if (saved) {
                state = next
                feedback = successMessage
            } else {
                feedback = "Could not save that change. Your previous saved state is still intact."
            }
            isSaving = false
        }
    }

    val currentState = state
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
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Guess The Number", style = MaterialTheme.typography.headlineMedium)
            Text("Find the hidden number between ${currentState.currentRangeMin} and ${currentState.currentRangeMax}.")
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatValue("Money", currentState.money.toString())
                StatValue("Prestige", currentState.prestige.toString())
                StatValue("Ultra", currentState.ultra.toString())
                StatValue("Nebula", currentState.nebula.toString())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatValue("Attempts", currentState.attempts.toString())
                StatValue("Correct", currentState.correctGuesses.toString())
                StatValue("Streak", currentState.streak.toString())
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
                    } else {
                        val result = engine.processGuess(currentState, guess)
                        persist(
                            result.newState,
                            when (result.feedback) {
                                GuessFeedback.TOO_LOW -> "Too low. Try again!"
                                GuessFeedback.TOO_HIGH -> "Too high. Try again!"
                                GuessFeedback.CORRECT -> "Correct! You earned ${result.reward} Money."
                            }
                        )
                        guessText = ""
                    }
                },
                enabled = guessText.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "Saving…" else "Submit guess")
            }
            Text(feedback, style = MaterialTheme.typography.bodyLarge)

            Text("Money upgrades", style = MaterialTheme.typography.headlineSmall)
            Text("Increase the power of your guessing run. Costs and descriptions load from the same game configuration as Android.")
            upgrades.forEach { upgrade ->
                val level = currentState.upgradeLevels[upgrade.id] ?: 0
                val cost = DesktopEconomyRules.nextUpgradeCost(upgrade, level)
                EconomyEntry(
                    title = upgrade.name,
                    description = upgrade.description,
                    detail = "Level $level / ${upgrade.maxLevel} · Next cost: ${cost}",
                    buttonText = if (level >= upgrade.maxLevel) "Maxed" else "Buy upgrade",
                    enabled = !isSaving && level < upgrade.maxLevel && currentState.money >= cost
                ) {
                    val next = DesktopEconomyRules.buyMoneyUpgrade(currentState, upgrade)
                    if (next == null) feedback = "Not enough Money for that upgrade."
                    else persist(next, "${upgrade.name} upgraded.")
                }
            }

            Text("Prestige", style = MaterialTheme.typography.headlineSmall)
            Text("Requirement: ${engine.calculatePrestigeRequirement(currentState.prestigeCount)} Money · Estimated reward: ${engine.calculatePrestigeReward(currentState.money, currentState.prestigeCount)} Prestige")
            Button(
                enabled = !isSaving && currentState.money >= engine.calculatePrestigeRequirement(currentState.prestigeCount) &&
                    engine.calculatePrestigeReward(currentState.money, currentState.prestigeCount) > BigNumber.ZERO,
                onClick = {
                    val next = DesktopEconomyRules.prestigeReset(currentState, engine)
                    if (next == null) feedback = "You do not meet the Prestige requirement yet."
                    else persist(next, "Prestige reset complete. Your Prestige currency was preserved.")
                }
            ) { Text("Prestige reset") }
            prestigeUpgrades.forEach { upgrade ->
                val level = currentState.prestigeUpgradeLevels[upgrade.id] ?: 0
                val cost = DesktopEconomyRules.nextUpgradeCost(upgrade, level)
                EconomyEntry(
                    title = upgrade.name,
                    description = upgrade.description,
                    detail = "Level $level / ${upgrade.maxLevel} · Next cost: ${cost} Prestige",
                    buttonText = if (level >= upgrade.maxLevel) "Maxed" else "Buy Prestige upgrade",
                    enabled = !isSaving && level < upgrade.maxLevel && currentState.prestige >= cost
                ) {
                    val next = DesktopEconomyRules.buyPrestigeUpgrade(currentState, upgrade)
                    if (next == null) feedback = "Not enough Prestige for that upgrade."
                    else persist(next, "${upgrade.name} upgraded.")
                }
            }

            Text("Ultra", style = MaterialTheme.typography.headlineSmall)
            Text("Requires ${engine.calculateUltraRequirement(currentState.ultraCount)} Money and at least 1,000 Prestige. Estimated reward: ${engine.calculateUltraReward(currentState.money, currentState.ultraCount)} Ultra.")
            Button(
                enabled = !isSaving && currentState.money >= engine.calculateUltraRequirement(currentState.ultraCount) &&
                    currentState.prestige >= BigNumber(1_000) &&
                    engine.calculateUltraReward(currentState.money, currentState.ultraCount) > BigNumber.ZERO,
                onClick = {
                    val next = DesktopEconomyRules.ultraReset(currentState, engine)
                    if (next == null) feedback = "You do not meet the Ultra requirements yet."
                    else persist(next, "Ultra reset complete.")
                }
            ) { Text("Ultra reset") }
            ultraUpgrades.forEach { upgrade ->
                val level = currentState.ultraUpgradeLevels[upgrade.id] ?: 0
                val cost = DesktopEconomyRules.nextUpgradeCost(upgrade, level)
                EconomyEntry(
                    title = upgrade.name,
                    description = upgrade.description,
                    detail = "Level $level / ${upgrade.maxLevel} · Next cost: ${cost} Ultra",
                    buttonText = if (level >= upgrade.maxLevel) "Maxed" else "Buy Ultra upgrade",
                    enabled = !isSaving && level < upgrade.maxLevel && currentState.ultra >= cost
                ) {
                    val next = DesktopEconomyRules.buyUltraUpgrade(currentState, upgrade)
                    if (next == null) feedback = "Not enough Ultra for that upgrade."
                    else persist(next, "${upgrade.name} upgraded.")
                }
            }

            Text("Nebula shop", style = MaterialTheme.typography.headlineSmall)
            shopItems.forEach { item ->
                val owned = item.id in currentState.shopPurchases
                EconomyEntry(
                    title = item.name,
                    description = item.description,
                    detail = "${item.nebulaCost} Nebula · ${item.category}",
                    buttonText = if (owned) "Owned" else "Purchase",
                    enabled = !isSaving && !owned && currentState.nebula >= BigNumber(item.nebulaCost)
                ) {
                    val next = DesktopEconomyRules.buyNebulaShopItem(currentState, item)
                    if (next == null) feedback = "Not enough Nebula or this item is already owned."
                    else persist(next, "${item.name} purchased.")
                }
            }

            Text("Prestige shop", style = MaterialTheme.typography.headlineSmall)
            prestigeShopItems.forEach { item ->
                val owned = item.id in currentState.prestigeShopPurchases
                EconomyEntry(
                    title = item.name,
                    description = item.description,
                    detail = "${item.cost} Prestige",
                    buttonText = if (owned) "Owned" else "Purchase",
                    enabled = !isSaving && !owned && currentState.prestige >= BigNumber(item.cost)
                ) {
                    val next = DesktopEconomyRules.buyPrestigeShopItem(currentState, item)
                    if (next == null) feedback = "Not enough Prestige or this item is already owned."
                    else persist(next, "${item.name} purchased.")
                }
            }

            Text("Ultra shop", style = MaterialTheme.typography.headlineSmall)
            ultraShopItems.forEach { item ->
                val owned = item.id in currentState.ultraShopPurchases
                EconomyEntry(
                    title = item.name,
                    description = item.description,
                    detail = "${item.cost} Ultra",
                    buttonText = if (owned) "Owned" else "Purchase",
                    enabled = !isSaving && !owned && currentState.ultra >= BigNumber(item.cost)
                ) {
                    val next = DesktopEconomyRules.buyUltraShopItem(currentState, item)
                    if (next == null) feedback = "Not enough Ultra or this item is already owned."
                    else persist(next, "${item.name} purchased.")
                }
            }

            Text("Talents", style = MaterialTheme.typography.headlineSmall)
            talents.forEach { talent ->
                val owned = talent.id in currentState.prestigeShopPurchases
                EconomyEntry(
                    title = talent.name,
                    description = talent.description,
                    detail = "${talent.cost} Prestige" + (talent.requiredParentId?.let { " · Requires $it" } ?: ""),
                    buttonText = if (owned) "Unlocked" else "Unlock talent",
                    enabled = !isSaving && !owned && currentState.prestige >= BigNumber(talent.cost)
                ) {
                    val next = DesktopEconomyRules.buyTalent(currentState, talent)
                    if (next == null) feedback = "Not enough Prestige or a required parent talent is locked."
                    else persist(next, "${talent.name} unlocked.")
                }
            }

            Text(
                "Your Windows run is saved locally after each accepted action. A backup is kept for recovery if the primary save is damaged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun EconomyEntry(
    title: String,
    description: String,
    detail: String,
    buttonText: String,
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
            Button(onClick = onClick, enabled = enabled) { Text(buttonText) }
        }
    }
}
