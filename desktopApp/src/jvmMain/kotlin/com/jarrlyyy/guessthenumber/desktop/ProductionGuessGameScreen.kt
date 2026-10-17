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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveSlotSummary
import com.jarrlyyy.guessthenumber.desktop.persistence.defaultDesktopSaveDirectory
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import kotlinx.coroutines.Job
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
    onOpenStats: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onBackFromSettings: () -> Unit = {},
    navigationPosition: DesktopNavigationPosition = DesktopNavigationPosition.LEFT,
    onNavigationPositionChange: (DesktopNavigationPosition) -> Unit = {},
    onOpenParityDestination: (DesktopDestination) -> Unit = {},
    onThemeSettingsLoaded: (
        themeMode: String,
        primaryHex: String,
        secondaryHex: String,
        tertiaryHex: String,
        reducedMotion: Boolean
    ) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val engine = remember { GameEngine() }
    val autoClicker = remember { DesktopGuessingBot() }
    val saveStore = remember { DesktopSaveStore(defaultDesktopSaveDirectory()) }
    val catalogResult = remember { runCatching { DesktopEconomyCatalog.load() } }
    val catalog = catalogResult.getOrNull()
    val talentCatalogResult = remember { runCatching { DesktopEconomyRepository().talents() } }
    val talentCatalog = talentCatalogResult.getOrNull()
    val minigameCatalogResult = remember { runCatching { DesktopEconomyRepository().minigames() } }
    val minigameCatalog = minigameCatalogResult.getOrNull()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<GameState?>(null) }
    var activeSaveSlot by remember { mutableStateOf(1) }
    var saveSlotSummaries by remember { mutableStateOf<List<DesktopSaveSlotSummary>>(emptyList()) }
    var guessText by remember { mutableStateOf("") }
    var lastGuessTimestampNanos by remember { mutableStateOf(0L) }
    var selectedShopCategory by remember { mutableStateOf("all") }
    var feedback by remember { mutableStateOf("Loading your saved game…") }
    var feedbackStatus by remember { mutableStateOf(GuessGameStatus.READY) }
    var isSaving by remember { mutableStateOf(false) }
    var webLastGuessStatus by remember { mutableStateOf<String?>(null) }
    val webRequestIds = remember { LinkedHashSet<String>() }
    var autoClickerFailurePaused by remember { mutableStateOf(false) }
    var pendingReset by remember { mutableStateOf<String?>(null) }
    var settingsSaveJob by remember { mutableStateOf<Job?>(null) }
    val latestSaving by rememberUpdatedState(isSaving)

    LaunchedEffect(Unit) {
        val selectedSlot = withContext(Dispatchers.IO) { saveStore.activeSlot() }
        activeSaveSlot = selectedSlot
        val loaded = withContext(Dispatchers.IO) { saveStore.load(selectedSlot) }
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
            val created = withContext(Dispatchers.IO) { saveStore.save(activeSaveSlot, fresh) }
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
                    val refreshedSave = withContext(Dispatchers.IO) { saveStore.save(activeSaveSlot, transaction.state) }
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
            val saved = withContext(Dispatchers.IO) { saveStore.save(activeSaveSlot, progressedState) }
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

    fun updateSettings(settings: GameSettings) {
        val updated = (state ?: return).copy(settings = settings)
        state = updated
        settingsSaveJob?.cancel()
        settingsSaveJob = scope.launch {
            delay(200)
            val saved = withContext(Dispatchers.IO) { saveStore.save(activeSaveSlot, updated) }
            if (!saved) {
                feedback = "Settings could not be saved. Your last valid save remains available."
            }
        }
    }

    fun resetAllGameData() {
        settingsSaveJob?.cancel()
        val resetState = GameState(
            targetNumber = Random.nextLong(1L, 101L),
            currentRangeMin = 1L,
            currentRangeMax = 100L
        )
        state = resetState
        autoClicker.reset(resetState.currentRangeMin, resetState.currentRangeMax)
        isSaving = true
        scope.launch {
            val saved = withContext(Dispatchers.IO) { saveStore.reset(activeSaveSlot, resetState) }
            feedback = if (saved) "Game data reset." else "Could not complete the data reset. Check the local save files."
            isSaving = false
        }
    }

    val currentState = state

    LaunchedEffect(currentState?.settings) {
        val settings = currentState?.settings ?: return@LaunchedEffect
        onThemeSettingsLoaded(
            settings.themeMode,
            settings.customPrimaryColor,
            settings.customSecondaryColor,
            settings.customTertiaryColor,
            settings.reducedMotion
        )
    }

    LaunchedEffect(currentState) {
        val snapshot = currentState ?: return@LaunchedEffect
        if (!snapshot.autoClickerActive || autoClickerFailurePaused || "auto_clicker_basic" !in snapshot.shopPurchases) {
            return@LaunchedEffect
        }
        delay(engine.autoClickerDelayMillis(snapshot))
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
        val guessProgressState = DesktopEconomyTransactions.recordGuessProgress(
                before = snapshot,
                after = bonusAdjustedState,
                correct = correct,
                today = LocalDate.now().toString(),
                lootRoll = if (correct) Random.nextInt(1000) else null
            )
        val bossAdjustedState = DesktopEconomyTransactions.recordBossGuess(guessProgressState, correct)
        val finalState = DesktopEconomyTransactions.evaluateAchievements(bossAdjustedState)
        val saved = withContext(Dispatchers.IO) { saveStore.save(activeSaveSlot, finalState) }
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

        if (section == DesktopGameSection.ARCADE) {
            val minigames = minigameCatalog
            if (minigames == null) {
                Card(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Arcade unavailable", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                        Text("The canonical minigame catalog could not be loaded. No Arcade rewards can be claimed.")
                        Button(onClick = { onOpenParityDestination(DesktopDestination.MORE) }) { Text("Back to More") }
                    }
                }
            } else {
                DesktopArcadeScreen(
                    state = currentState,
                    minigames = minigames,
                    isRewarding = isSaving,
                    onEarnReward = { nebulaReward, moneyReward ->
                        commitTransaction { before ->
                            DesktopEconomyTransactions.awardArcadeReward(before, nebulaReward, moneyReward)
                        }
                    },
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) }
                )
            }
            return@Card
        }

        if (section == DesktopGameSection.PLAY ||
            section == DesktopGameSection.UPGRADES ||
            section == DesktopGameSection.SHOP
        ) {
            DesktopSharedWebView(
                onRequest = { raw, reply ->
                    scope.launch {
                        val request = runCatching { org.json.JSONObject(raw) }.getOrNull()
                        val requestId = request?.optString("id", "").orEmpty()
                        fun sendError(code: String) {
                            reply(org.json.JSONObject().put("version", 1).put("id", requestId)
                                .put("type", "error").put("error", code).toString())
                        }
                        if (request == null || raw.length > 512 ||
                            request.optInt("version", -1) != 1 ||
                            !requestId.matches(Regex("[A-Za-z0-9_-]{1,48}")) ||
                            request.opt("type") !is String
                        ) {
                            sendError("invalid_request")
                            return@launch
                        }
                        if (!webRequestIds.add(requestId)) {
                            sendError("duplicate_request")
                            return@launch
                        }
                        if (webRequestIds.size > 128) webRequestIds.remove(webRequestIds.first())
                        val type = request.getString("type")
                        val expectedKeys = when (type) {
                            "read_state" -> 3
                            "submit_guess", "update_multiplier", "buy_upgrade", "buy_shop_item", "equip_cosmetic", "claim_daily_quest", "open_loot_chest" -> 4
                            else -> -1
                        }
                        if (expectedKeys < 0 || request.length() != expectedKeys) {
                            sendError("unknown_action")
                            return@launch
                        }
                        var accepted = true
                        var message = ""
                        var nextState = state
                        var guessStatus = webLastGuessStatus
                        if (type == "submit_guess") {
                            val guess = request.optString("guess", "").takeIf { it.matches(Regex("-?[0-9]{1,19}")) }?.toLongOrNull()
                            val before = state
                            if (guess == null || before == null || guess !in before.currentRangeMin..before.currentRangeMax) {
                                accepted = false
                                message = "Enter a whole number inside the displayed range."
                            } else if (isSaving) {
                                accepted = false
                                message = "Please wait for the current save to finish."
                            } else {
                                isSaving = true
                                val result = engine.processGuess(before, guess)
                                val correct = result.feedback == GuessFeedback.CORRECT
                                val bonusAdjusted = DesktopEconomyTransactions.applyGuessBonuses(before, result.newState, correct)
                                val progressed = DesktopEconomyTransactions.recordGuessProgress(
                                    before = before,
                                    after = bonusAdjusted,
                                    correct = correct,
                                    today = LocalDate.now().toString(),
                                    lootRoll = if (correct) Random.nextInt(1000) else null
                                )
                                val finalState = DesktopEconomyTransactions.evaluateAchievements(
                                    DesktopEconomyTransactions.recordBossGuess(progressed, correct)
                                )
                                val saved = withContext(Dispatchers.IO) { saveStore.save(activeSaveSlot, finalState) }
                                if (saved) {
                                    state = finalState
                                    nextState = finalState
                                    guessStatus = result.feedback.name
                                    webLastGuessStatus = guessStatus
                                    autoClicker.observeGuess(guess, result.feedback)
                                    if (correct) autoClicker.reset(finalState.currentRangeMin, finalState.currentRangeMax)
                                    message = when (result.feedback) {
                                        GuessFeedback.TOO_LOW -> "Too low. Try again!"
                                        GuessFeedback.TOO_HIGH -> "Too high. Try again!"
                                        GuessFeedback.CORRECT -> "Correct! You earned ${finalState.money - before.money} Money."
                                    }
                                } else {
                                    accepted = false
                                    message = "Could not save that guess. Your previous save is intact."
                                }
                                isSaving = false
                            }
                        } else if (type != "read_state") {
                            val before = state
                            if (before == null || isSaving || catalog == null) {
                                accepted = false
                                message = "The game is still loading or saving."
                            } else {
                                val transaction = when (type) {
                                    "update_multiplier" -> DesktopEconomyTransactions.setBuyMultiplier(before, request.optString("multiplier"))
                                    "buy_upgrade" -> {
                                        val id = request.optString("upgradeId")
                                        val upgrade = catalog.upgrades.firstOrNull { it.id == id }
                                        if (upgrade == null) null else DesktopEconomyTransactions.buyUpgrade(before, upgrade)
                                    }
                                    "buy_shop_item" -> {
                                        val id = request.optString("itemId")
                                        val item = catalog.shopItems.firstOrNull { it.id == id }
                                        if (item == null) null else DesktopEconomyTransactions.buyShopItem(before, item)
                                    }
                                    "equip_cosmetic" -> {
                                        val id = request.optString("itemId")
                                        if (catalog.shopItems.none { it.id == id && it.isCosmetic }) null
                                        else DesktopEconomyTransactions.equipCosmetic(before, id)
                                    }
                                    "claim_daily_quest" -> DesktopEconomyTransactions.claimDailyQuest(
                                        before, request.optString("questId"), LocalDate.now().toString()
                                    )
                                    "open_loot_chest" -> {
                                        val tier = request.optString("tier")
                                        val available = when (tier) {
                                            "common" -> before.lootCommonChests
                                            "rare" -> before.lootRareChests
                                            "epic" -> before.lootEpicChests
                                            else -> 0
                                        }
                                        if (available <= 0) null else {
                                            val moneyReward = when (tier) {
                                                "common" -> BigNumber(5_000)
                                                "rare" -> BigNumber(25_000)
                                                else -> BigNumber(100_000)
                                            }
                                            val nebulaReward = when (tier) {
                                                "common" -> 5L
                                                "rare" -> 25L
                                                else -> 100L
                                            }
                                            val giveNebula = Random.nextBoolean()
                                            val consumed = when (tier) {
                                                "common" -> before.copy(lootCommonChests = before.lootCommonChests - 1)
                                                "rare" -> before.copy(lootRareChests = before.lootRareChests - 1)
                                                else -> before.copy(lootEpicChests = before.lootEpicChests - 1)
                                            }
                                            val rewarded = if (giveNebula) consumed.copy(
                                                nebula = consumed.nebula + BigNumber(nebulaReward),
                                                statistics = consumed.statistics.copy(nebulaEarned = consumed.statistics.nebulaEarned + nebulaReward),
                                                lootChestsOpened = consumed.lootChestsOpened + 1
                                            ) else consumed.copy(
                                                money = consumed.money + moneyReward,
                                                statistics = consumed.statistics.copy(moneyEarned = consumed.statistics.moneyEarned + moneyReward),
                                                lootChestsOpened = consumed.lootChestsOpened + 1
                                            )
                                            EconomyTransaction(rewarded, true, "Opened $tier chest.")
                                        }
                                    }
                                    else -> null
                                }
                                if (transaction == null) {
                                    accepted = false
                                    message = "That item or action is not available."
                                } else if (!transaction.accepted) {
                                    accepted = false
                                    message = transaction.message
                                } else {
                                    isSaving = true
                                    val progressed = DesktopEconomyTransactions.evaluateAchievements(transaction.state)
                                    val saved = withContext(Dispatchers.IO) { saveStore.save(activeSaveSlot, progressed) }
                                    if (saved) {
                                        state = progressed
                                        nextState = progressed
                                        message = transaction.message
                                    } else {
                                        accepted = false
                                        message = "Could not save that change. Your previous save is intact."
                                    }
                                    isSaving = false
                                }
                            }
                        }
                        val visibleState = nextState ?: state
                        val payload = if (visibleState != null && catalog != null) {
                            desktopWebPlayState(visibleState, catalog, accepted, guessStatus, message)
                        } else org.json.JSONObject().put("accepted", false).put("message", "Loading game…")
                        reply(org.json.JSONObject().put("version", 1).put("id", requestId)
                            .put("type", "state").put("state", payload).toString())
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
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

            if (section == DesktopGameSection.SAVE_SLOTS) {
                LaunchedEffect(activeSaveSlot, currentState?.lastSaveTimestamp, section) {
                    saveSlotSummaries = withContext(Dispatchers.IO) { saveStore.slotSummaries() }
                }
                DesktopSaveSlotsContent(
                    activeSlot = activeSaveSlot,
                    slots = saveSlotSummaries,
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) },
                    onRefresh = { scope.launch { saveSlotSummaries = withContext(Dispatchers.IO) { saveStore.slotSummaries() } } },
                    onSwitchSlot = { targetSlot ->
                        val snapshot = state
                        if (snapshot != null && !isSaving && targetSlot != activeSaveSlot) {
                            scope.launch {
                                isSaving = true
                                val saved = withContext(Dispatchers.IO) {
                                    saveStore.save(activeSaveSlot, snapshot.copy(lastSaveTimestamp = System.currentTimeMillis())) &&
                                        saveStore.setActiveSlot(targetSlot)
                                }
                                if (saved) {
                                    val loaded = withContext(Dispatchers.IO) { saveStore.load(targetSlot) }
                                    if (loaded != null) {
                                        activeSaveSlot = targetSlot
                                        state = loaded
                                        autoClicker.reset(loaded.currentRangeMin, loaded.currentRangeMax)
                                        feedback = "Switched to save slot $targetSlot."
                                    } else feedback = "That slot has no valid save. Create a new slot or recover its backup first."
                                } else feedback = "Could not save the current slot. Switching was cancelled."
                                isSaving = false
                                saveSlotSummaries = withContext(Dispatchers.IO) { saveStore.slotSummaries() }
                            }
                        }
                    },
                    onCreateSlot = { targetSlot ->
                        val current = state
                        if (current != null) scope.launch {
                            val fresh = GameState(
                                targetNumber = Random.nextLong(1L, 101L),
                                currentRangeMin = 1L,
                                currentRangeMax = 100L,
                                settings = current.settings,
                                tutorialCompleted = current.tutorialCompleted,
                                profileName = "Slot $targetSlot",
                                lastSaveTimestamp = System.currentTimeMillis()
                            )
                            val created = withContext(Dispatchers.IO) {
                                saveStore.createFreshSlot(targetSlot, fresh) && saveStore.setActiveSlot(targetSlot)
                            }
                            if (created) {
                                activeSaveSlot = targetSlot
                                state = fresh
                                autoClicker.reset(fresh.currentRangeMin, fresh.currentRangeMax)
                                feedback = "Created and switched to slot $targetSlot."
                            } else feedback = "Could not create that slot. It may already contain data."
                            saveSlotSummaries = withContext(Dispatchers.IO) { saveStore.slotSummaries() }
                        }
                    },
                    onDuplicateSlot = { source, target ->
                        scope.launch {
                            val duplicated = withContext(Dispatchers.IO) { saveStore.duplicateSlot(source, target) }
                            feedback = if (duplicated) "Duplicated slot $source to slot $target." else "Could not duplicate that slot. The target may not be empty."
                            saveSlotSummaries = withContext(Dispatchers.IO) { saveStore.slotSummaries() }
                        }
                    },
                    onRestoreBackup = { target ->
                        scope.launch {
                            val restored = withContext(Dispatchers.IO) { saveStore.restoreBackup(target) }
                            if (restored && target == activeSaveSlot) {
                                val recovered = withContext(Dispatchers.IO) { saveStore.load(target) }
                                if (recovered != null) state = recovered
                            }
                            feedback = if (restored) "Restored the previous backup for slot $target." else "Could not restore a valid backup for that slot."
                            saveSlotSummaries = withContext(Dispatchers.IO) { saveStore.slotSummaries() }
                        }
                    }
                )
            }

            if (section == DesktopGameSection.MUTATORS) {
                DesktopMutatorsContent(
                    state = currentState,
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) },
                    onMutatorChange = { mutatorId, active ->
                        commitTransaction { before -> DesktopEconomyTransactions.setMutatorActive(before, mutatorId, active, engine) }
                    },
                    onActivateBuilder = { selectedMutators ->
                        commitTransaction { before ->
                            val changes = (before.activeMutators - selectedMutators).map { it to false } +
                                (selectedMutators - before.activeMutators).map { it to true }
                            var transaction = EconomyTransaction(before, true, "Challenge mutators applied.")
                            for ((id, active) in changes) {
                                transaction = DesktopEconomyTransactions.setMutatorActive(transaction.state, id, active, engine)
                                if (!transaction.accepted) break
                            }
                            transaction
                        }
                    },
                    onClaimChallenge = { challengeId, reward ->
                        commitTransaction { before ->
                            if (challengeId in before.completedChallenges) EconomyTransaction(before, false, "Challenge already claimed.")
                            else EconomyTransaction(
                                before.copy(
                                    completedChallenges = before.completedChallenges + challengeId,
                                    nebula = before.nebula + BigNumber(reward),
                                    statistics = before.statistics.copy(nebulaEarned = before.statistics.nebulaEarned + reward)
                                ), true, "Challenge reward claimed: +$reward Nebula."
                            )
                        }
                    }
                )
            }

            if (section == DesktopGameSection.WORLD_MAP) {
                DesktopWorldMapContent(
                    state = currentState,
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) },
                    onWorldAction = { worldId, action ->
                        commitTransaction { before ->
                            when (action) {
                                "unlock" -> DesktopEconomyTransactions.unlockWorld(before, worldId)
                                "select" -> DesktopEconomyTransactions.selectWorld(before, worldId)
                                "boss" -> DesktopEconomyTransactions.toggleBossBattle(before, worldId)
                                else -> EconomyTransaction(before, false, "That world action is not supported yet.")
                            }
                        }
                    }
                )
            }


            if (section == DesktopGameSection.HOME_BASE) {
                DesktopHomeBaseContent(
                    state = currentState,
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) },
                    onUpgrade = {
                        commitTransaction { before -> DesktopEconomyTransactions.upgradeHomeBase(before) }
                    }
                )
            }

            if (section == DesktopGameSection.RELICS) {
                DesktopRelicsContent(
                    state = currentState,
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) },
                    onRelicAction = { relicId, action ->
                        val before = state
                        if (before == null || isSaving) {
                            feedback = "Wait for the current save to finish."
                        } else {
                            val relic = runCatching { DesktopRelicsCatalog.load().firstOrNull { it.id == relicId } }.getOrNull()
                            if (relic == null) {
                                feedback = "That relic is not in the catalog."
                            } else {
                                val transaction = when (action) {
                                    "claim" -> DesktopEconomyTransactions.claimRelic(before, relicId, relic.unlockBossId)
                                    "equip" -> DesktopEconomyTransactions.equipRelic(before, relicId)
                                    "unequip" -> DesktopEconomyTransactions.unequipRelic(before, relicId)
                                    else -> EconomyTransaction(before, false, "That relic action is not available.")
                                }
                                val updated = transaction.state
                                if (!transaction.accepted) {
                                    feedback = transaction.message
                                } else {
                                    isSaving = true
                                    scope.launch {
                                        val persisted = withContext(Dispatchers.IO) {
                                            saveStore.save(activeSaveSlot, updated.copy(lastSaveTimestamp = System.currentTimeMillis()))
                                        }
                                        if (persisted) {
                                            state = updated
                                            feedback = when (action) {
                                                "claim" -> "Relic claimed: " + relic.name + "."
                                                "equip" -> "Equipped " + relic.name + "."
                                                else -> "Unequipped " + relic.name + "."
                                            }
                                        } else {
                                            feedback = "Could not save the relic change. Your previous saved state is still intact."
                                        }
                                        isSaving = false
                                    }
                                }
                            }
                        }
                    }
                )
            }

            if (section == DesktopGameSection.LUCKY_STAKE) {
                DesktopLuckyStakeContent(
                    state = currentState,
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) },
                    onStakeResult = { stake, payout, won ->
                        commitTransaction { DesktopEconomyTransactions.settleStake(it, stake, payout, won) }
                    }
                )
            }

            if (section == DesktopGameSection.ENDGAME) {
                DesktopEndgameContent(
                    state = currentState,
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) },
                    onOpenWorldMap = { onOpenParityDestination(DesktopDestination.WORLD_MAP) }
                )
            }

            if (section == DesktopGameSection.CODEX) {
                DesktopCodexContent(
                    state = currentState,
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) },
                    onClaimCompletionReward = {
                        commitTransaction { DesktopEconomyTransactions.claimCodexCompletionReward(it) }
                    }
                )
            }

            if (section == DesktopGameSection.ACHIEVEMENTS) {
                DesktopAchievementsContent(
                    state = currentState,
                    onBack = { onOpenParityDestination(DesktopDestination.MORE) }
                )
            }

            if (section == DesktopGameSection.PLAY) {
            DesktopEconomyTransactions.activeBossBattleStatus(currentState)?.let { boss ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚔", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Boss Battle Active", style = MaterialTheme.typography.titleMedium)
                                Text(boss.bossName, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("${boss.hp - boss.damage} / ${boss.hp} HP", style = MaterialTheme.typography.labelMedium)
                        }
                        LinearProgressIndicator(
                            progress = { boss.damage.toFloat() / boss.hp.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Correct guesses deal damage. Wrong guesses can restore HP as the boss changes phase.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            onClick = {
                                commitTransaction { DesktopEconomyTransactions.toggleBossBattle(it, boss.worldId) }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Leave battle") }
                    }
                }
            }
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
                    } else if (!isSaving && lastGuessTimestampNanos != 0L && System.nanoTime() - lastGuessTimestampNanos < 50_000_000L) {
                        feedbackStatus = GuessGameStatus.READY
                        feedback = "Please wait a moment before guessing again."
                    } else if (!isSaving) {
                        lastGuessTimestampNanos = System.nanoTime()
                        isSaving = true
                        val result = engine.processGuess(currentState, guess)
                        val correct = result.feedback == GuessFeedback.CORRECT
                        val bonusAdjustedState = DesktopEconomyTransactions.applyGuessBonuses(
                            before = currentState,
                            after = result.newState,
                            correct = correct
                        )
                        val guessProgressState = DesktopEconomyTransactions.recordGuessProgress(
                                before = currentState,
                                after = bonusAdjustedState,
                                correct = correct,
                                today = LocalDate.now().toString(),
                                lootRoll = if (correct) Random.nextInt(1000) else null
                            )
                        val bossAdjustedState = DesktopEconomyTransactions.recordBossGuess(guessProgressState, correct)
                        val finalState = DesktopEconomyTransactions.evaluateAchievements(bossAdjustedState)
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) { saveStore.save(activeSaveSlot, finalState) }
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
                    val catalogError = catalogResult.exceptionOrNull()?.message ?: "unknown error"
                    Text(
                        "Economy catalog could not be loaded: $catalogError",
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
                val prestigeReward = engine.calculatePrestigeReward(currentState)
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
                val ultraReward = engine.calculateUltraReward(currentState)
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Available Nebula", style = MaterialTheme.typography.labelLarge)
                            Text(currentState.nebula.format(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            currentState.equippedCosmeticId?.let { equippedId ->
                                Text("Equipped: " + equippedId.removePrefix("cosmetic_").replace('_', ' '), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("✦", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "All", "automation" to "Automation", "boosts" to "Boosts", "cosmetics" to "Cosmetics").forEach { (categoryId, label) ->
                        FilterChip(selected = selectedShopCategory == categoryId, onClick = { selectedShopCategory = categoryId }, label = { Text(label) })
                    }
                }

                if (catalog == null) {
                    val catalogError = catalogResult.exceptionOrNull()?.message ?: "unknown error"
                    Text("Shop catalog could not be loaded: $catalogError", color = MaterialTheme.colorScheme.error)
                } else {
                    val visibleItems = catalog.shopItems.filter { item -> selectedShopCategory == "all" || item.category == selectedShopCategory }
                    if (visibleItems.isEmpty()) {
                        Text("No items in this category yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    visibleItems.forEach { item ->
                        val purchased = item.id in currentState.shopPurchases
                        val equipped = currentState.equippedCosmeticId == item.id
                        val supported = item.id in setOf("auto_clicker_basic", "nebula_boost", "golden_guess") || (item.isCosmetic && item.id.startsWith("cosmetic_"))
                        val canAfford = currentState.nebula >= BigNumber(item.nebulaCost)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        when (item.id) {
                                            "nebula_boost" -> "Effect: doubles future Nebula rewards"
                                            "golden_guess" -> "Effect: guarantees critical rewards on every tenth lifetime guess"
                                            "auto_clicker_basic" -> "Effect: unlocks the Auto Clicker"
                                            else -> if (item.isCosmetic) "Cosmetic" else "Shop item"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("Cost: ${item.nebulaCost} Nebula", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                when {
                                    item.isCosmetic && purchased -> OutlinedButton(
                                        onClick = { commitTransaction { DesktopEconomyTransactions.equipCosmetic(it, item.id) } },
                                        enabled = !isSaving && !equipped
                                    ) {
                                        AnimatedContent(targetState = equipped, label = "desktopCosmeticEquip_${item.id}") { isEquipped ->
                                            Text(if (isEquipped) "Equipped" else "Equip")
                                        }
                                    }
                                    else -> Button(
                                        onClick = { commitTransaction { DesktopEconomyTransactions.buyShopItem(it, item) } },
                                        enabled = !isSaving && supported && !purchased && canAfford
                                    ) {
                                        AnimatedContent(targetState = purchased, label = "desktopShopPurchase_${item.id}") { isPurchased ->
                                            Text(when { isPurchased -> "Owned"; !supported -> "Unavailable"; else -> "Acquire" })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (section == DesktopGameSection.TALENT) {
                if (talentCatalog == null) {
                    val talentCatalogError = talentCatalogResult.exceptionOrNull()?.message ?: "unknown error"
                    Text("Talent catalog could not be loaded: $talentCatalogError")
                } else {
                    DesktopTalentWebContent(
                        state = currentState,
                        talents = talentCatalog,
                        isSaving = isSaving,
                        onBuyTalent = { talent ->
                            commitTransaction {
                                DesktopEconomyTransactions.buyTalent(
                                    it, talent.id, talent.cost, talent.requiredParentId
                                )
                            }
                        }
                    )
                }
            }

            if (section == DesktopGameSection.STATS) {
                val stats = currentState.statistics
                val totalGuesses = stats.totalGuesses
                val accuracy = if (totalGuesses > 0) {
                    (stats.correctGuesses.toDouble() / totalGuesses.toDouble()) * 100.0
                } else {
                    0.0
                }
                val roundedAccuracy = kotlin.math.round(accuracy * 10.0) / 10.0
                val playtimeHours = stats.playtimeSeconds / 3600
                val playtimeMinutes = (stats.playtimeSeconds % 3600) / 60
                val playtimeSeconds = stats.playtimeSeconds % 60

                SectionTitle("Advanced Statistics")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Guessing Precision", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("$roundedAccuracy%", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "Correct: ${stats.correctGuesses} · Failed: ${stats.failedGuesses} · Total: $totalGuesses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { if (totalGuesses > 0) stats.correctGuesses.toFloat() / totalGuesses.toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                    }
                }

                SectionTitle("Performance & Economy")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        CurrencySummary("Money Earned", stats.moneyEarned.format())
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        CurrencySummary("Money Spent", stats.moneySpent.format())
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        CurrencySummary("Current Streak", currentState.streak.toString())
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        CurrencySummary("Best Streak", currentState.bestStreak.toString())
                    }
                }

                SectionTitle("Resets & Meta Progression")
                Text("Prestige Resets: ${stats.prestigesCount}")
                Text("Ultra Resets: ${stats.ultrasCount}")
                Text("Nebula Earned: ${stats.nebulaEarned}")

                SectionTitle("Session & Arcade")
                Text("Total Playtime: ${playtimeHours} hr ${playtimeMinutes} min ${playtimeSeconds} sec")
                Text("Arcade Minigames Played: ${stats.arcadePlayed}")
                Text("Current Number Range: ${currentState.currentRangeMin} - ${currentState.currentRangeMax}")
            }


            if (section == DesktopGameSection.SETTINGS) {
                if (currentState == null) {
                    CircularProgressIndicator()
                } else {
                    DesktopSettingsScreen(
                        state = currentState,
                        isSaving = isSaving,
                        onUpdateSettings = ::updateSettings,
                        onResetData = ::resetAllGameData,
                        navigationPosition = navigationPosition,
                        onNavigationPositionChange = onNavigationPositionChange,
                        onBack = onBackFromSettings
                    )
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
                        Button(onClick = onOpenStats) { Text("Statistics") }
                        Button(onClick = onOpenSettings) { Text("Settings & Save Management") }
                        Button(onClick = onOpenAbout) { Text("About and updates") }
                        Text("More Windows destinations", style = MaterialTheme.typography.titleMedium)
                        Text("These routes are being brought into parity with Android in stages. A connected route does not mean every Android interaction is complete yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val parityDestinations = listOf(DesktopDestination.WORLD_MAP, DesktopDestination.MUTATORS, DesktopDestination.SAVE_SLOTS, DesktopDestination.RELICS, DesktopDestination.HOME_BASE, DesktopDestination.CODEX, DesktopDestination.ENDGAME, DesktopDestination.LUCKY_STAKE, DesktopDestination.MUSIC_PLAYER, DesktopDestination.ARCADE, DesktopDestination.ACTIVITY_HUB, DesktopDestination.LUA_TESTING_HUB, DesktopDestination.CHANGELOG, DesktopDestination.LIVE_OPS, DesktopDestination.DEVELOPER_SETTINGS, DesktopDestination.DEVELOPER_CONSOLE, DesktopDestination.PROCESS_INSPECTOR)
                        parityDestinations.chunked(2).forEach { destinations ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                destinations.forEach { destination -> Button(onClick = { onOpenParityDestination(destination) }, modifier = Modifier.weight(1f)) { Text(destination.label) } }
                                if (destinations.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
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
