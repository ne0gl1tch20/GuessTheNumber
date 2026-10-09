package com.jarrlyyy.guessthenumber.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.data.audio.BackgroundMusicManager
import com.jarrlyyy.guessthenumber.data.crash.AppErrorHandler
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.data.notification.GameReminderWorker
import com.jarrlyyy.guessthenumber.data.notification.NotificationHelper
import com.jarrlyyy.guessthenumber.widget.WidgetRefresh
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import com.jarrlyyy.guessthenumber.data.store.MAX_SAVE_SLOTS
import com.jarrlyyy.guessthenumber.data.store.SaveSlotMetadata
import com.jarrlyyy.guessthenumber.domain.command.CommandExecutor
import com.jarrlyyy.guessthenumber.domain.engine.AntiCheatService
import com.jarrlyyy.guessthenumber.domain.engine.AntiTimeTravelService
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessingBot
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.SaveProfile
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.RandomEventEngine
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository
import com.jarrlyyy.guessthenumber.data.repository.LocaleManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val saveManager = SaveManager(application)
    private val slotOperationMutex = Mutex()
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var configRepository = JsonConfigRepository(application, _gameState.value.settings.locale)
    val localeManager: LocaleManager
        get() = configRepository.localeManager

    private fun updateLocaleRepo(localeTag: String) {
        configRepository = JsonConfigRepository(getApplication(), localeTag)
    }

    private val gameEngine = GameEngine()
    private val guessingBot = GuessingBot()
    private val randomEventEngine = RandomEventEngine()
    private val commandExecutor = CommandExecutor()
    private val backgroundMusicManager = BackgroundMusicManager(application)

    val isPlayingMusic: StateFlow<Boolean> = backgroundMusicManager.isPlaying
    val musicCurrentPosition: StateFlow<Int> = backgroundMusicManager.currentPosition
    val musicDuration: StateFlow<Int> = backgroundMusicManager.duration
    val musicAlbumArt: StateFlow<android.graphics.Bitmap?> = backgroundMusicManager.albumArt

    private val _isLoadingSave = MutableStateFlow(true)
    val isLoadingSave: StateFlow<Boolean> = _isLoadingSave.asStateFlow()

    private val _hasPreviousCrash = MutableStateFlow(false)
    val hasPreviousCrash: StateFlow<Boolean> = _hasPreviousCrash.asStateFlow()

    private val _offlineGains = MutableStateFlow<BigNumber?>(null)
    private val _incomePerSecond = MutableStateFlow(BigNumber.ZERO)
    val incomePerSecond: StateFlow<BigNumber> = _incomePerSecond.asStateFlow()
    private var lastIncomeMeasurement = BigNumber.ZERO
    val offlineGains: StateFlow<BigNumber?> = _offlineGains.asStateFlow()

    private val _showChangelogPopup = MutableStateFlow(false)
    val showChangelogPopup: StateFlow<Boolean> = _showChangelogPopup.asStateFlow()

    private val _showTimeTravelPopup = MutableStateFlow(false)
    val showTimeTravelPopup: StateFlow<Boolean> = _showTimeTravelPopup.asStateFlow()
    private val _timeTravelSeconds = MutableStateFlow(0L)
    val timeTravelSeconds: StateFlow<Long> = _timeTravelSeconds.asStateFlow()

    private val _activeSlot = MutableStateFlow(1)
    val activeSlot: StateFlow<Int> = _activeSlot.asStateFlow()

    private val _hasLegacySave = MutableStateFlow(false)
    val hasLegacySave: StateFlow<Boolean> = _hasLegacySave.asStateFlow()

    private val _hasAnySave = MutableStateFlow(false)
    val hasAnySave: StateFlow<Boolean> = _hasAnySave.asStateFlow()

    private val _needsSettingsMigration = MutableStateFlow(false)
    val needsSettingsMigration: StateFlow<Boolean> = _needsSettingsMigration.asStateFlow()
    private val _isMigratingSettings = MutableStateFlow(false)
    val isMigratingSettings: StateFlow<Boolean> = _isMigratingSettings.asStateFlow()

    private var autoClickerJob: Job? = null
    private var saveJob: Job? = null
    private var timerJob: Job? = null
    private var randomEventJob: Job? = null

    init {
        AppErrorHandler.init(application.filesDir)
        _hasPreviousCrash.value = AppErrorHandler.hasPreviousCrash()
        GameLogger.configureStorage(File(application.filesDir, "game_logs.txt"), _gameState.value.settings.saveLogsToStorage)

        // Crash recovery is the first boot layer. Do not touch save data or start game
        // background jobs until the user has acknowledged the previous crash.
        if (!_hasPreviousCrash.value) {
            loadGame()
            startAutoSave()
            startPlaytimeTimer()
            startRandomEvents()
            startAutoClicker()
        }
    }

    fun loadGame(slot: Int = -1) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetSlot = if (slot in 1..MAX_SAVE_SLOTS) slot else saveManager.getActiveSlot()
            saveManager.setActiveSlot(targetSlot)
            _activeSlot.value = targetSlot

            val anySaveCheck = saveManager.hasAnyValidSave()
            withContext(Dispatchers.Main) { _hasAnySave.value = anySaveCheck }

            saveManager.migrateTutorialCompletionToGlobal()

            val settingsMigrationCheck = saveManager.needsSettingsMigration()
            withContext(Dispatchers.Main) { _needsSettingsMigration.value = settingsMigrationCheck }

            val legacyCheck = saveManager.hasLegacySave()
            if (legacyCheck) {
                withContext(Dispatchers.Main) {
                    _hasLegacySave.value = true
                }
            }

            val loaded = saveManager.loadGame(targetSlot)
            val sanitized = AntiCheatService.sanitizeCurrency(loaded)

            // Auto-detect system locale on fresh boot if default en-US is set
            val effectiveSanitized = if (sanitized.settings.locale == "en-US") {
                val systemTag = java.util.Locale.getDefault().toLanguageTag()
                if (systemTag.startsWith("fil", ignoreCase = true) || systemTag.startsWith("tl", ignoreCase = true)) {
                    sanitized.copy(settings = sanitized.settings.copy(locale = "fil-PH"))
                } else {
                    sanitized
                }
            } else {
                sanitized
            }
            updateLocaleRepo(effectiveSanitized.settings.locale)
            
            val currentVersion = BuildConfig.VERSION_NAME
            val isNewVersion = effectiveSanitized.lastSavedVersion != currentVersion

            val timeTravelCheck = AntiTimeTravelService.checkTimeTravel(effectiveSanitized)
            val timeTravelDetected = timeTravelCheck.timeTravelDetected
            val timeDiffSeconds = timeTravelCheck.timeDifferenceSeconds

            var timeCheckedState = timeTravelCheck.updatedState.copy(
                lastSavedVersion = currentVersion
            )

            val deltaSeconds = if (!timeTravelDetected) (System.currentTimeMillis() - effectiveSanitized.lastSaveTimestamp) / 1000L else 0L
            var finalState = refreshDailyQuestDay(timeCheckedState)

            if (!timeTravelDetected && deltaSeconds > 60 && timeCheckedState.autoClickerActive) {
                val maxOfflineSeconds = 28800L
                val effectiveSeconds = minOf(deltaSeconds, maxOfflineSeconds)
                val earningsPerSec = BigNumber(500)
                val totalOfflineEarnings = earningsPerSec * BigNumber(effectiveSeconds.toDouble())
                
                finalState = finalState.copy(
                    money = finalState.money + totalOfflineEarnings
                )
                withContext(Dispatchers.Main) {
                    _offlineGains.value = totalOfflineEarnings
                }
                GameLogger.log(LogLevel.INFO, LoggerCategory.AUTOCLICKER, "OFFLINE_GAINS", "Earned $totalOfflineEarnings while offline for $effectiveSeconds seconds in slot $targetSlot.")
            }

            guessingBot.reset(finalState.currentRangeMin, finalState.currentRangeMax)
            lastIncomeMeasurement = finalState.money
            _incomePerSecond.value = BigNumber.ZERO

            withContext(Dispatchers.Main) {
                _gameState.value = finalState
                if (effectiveSanitized.dailyQuestDate != finalState.dailyQuestDate) saveGameAsync()
                _isLoadingSave.value = false
                if (timeTravelDetected) {
                    _showTimeTravelPopup.value = true
                    _timeTravelSeconds.value = timeDiffSeconds
                }
                if (isNewVersion && !timeTravelDetected) {
                    _showChangelogPopup.value = true
                }
            }
        }
    }

    suspend fun getSlotMetadata(slot: Int): SaveSlotMetadata {
        return withContext(Dispatchers.IO) {
            saveManager.getSlotMetadata(slot)
        }
    }

    fun createSlot(slot: Int, difficultyId: String, profileName: String = "", profileIconId: String = SaveProfile.DEFAULT_ICON, onResult: (Boolean) -> Unit = {}) {
        if (slot !in 1..MAX_SAVE_SLOTS || !Difficulty.isValid(difficultyId)) {
            onResult(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val success = slotOperationMutex.withLock {
                val created = saveManager.createFreshSlot(slot, difficultyId, profileName, profileIconId)
                if (!created) {
                    false
                } else {
                    val globalSettings = saveManager.loadGame(slot).settings
                    val freshState = GameState(
                        difficultyId = difficultyId,
                        profileName = profileName.trim().take(24),
                        profileIconId = if (SaveProfile.isValidIcon(profileIconId)) profileIconId else SaveProfile.DEFAULT_ICON,
                        currentRangeMax = Difficulty.rangeMax(difficultyId, 0),
                        targetNumber = (Difficulty.rangeMax(difficultyId, 0) / 2L).coerceAtLeast(1L),
                        lastSaveTimestamp = System.currentTimeMillis(),
                        settings = globalSettings,
                        tutorialCompleted = globalSettings.tutorialCompleted
                    )
                    _activeSlot.value = slot
                    _hasAnySave.value = true
                    _gameState.value = freshState
                    guessingBot.reset(freshState.currentRangeMin, freshState.currentRangeMax)
                    lastIncomeMeasurement = freshState.money
                    _incomePerSecond.value = BigNumber.ZERO
                    true
                }
            }
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun updateSlotProfile(slot: Int, name: String, iconId: String) {
        if (slot !in 1..MAX_SAVE_SLOTS || !SaveProfile.isValidIcon(iconId)) return
        viewModelScope.launch(Dispatchers.IO) {
            slotOperationMutex.withLock {
                val metadata = saveManager.getSlotMetadata(slot)
                if (metadata.isEmpty) return@withLock
                val state = saveManager.loadGame(slot)
                val updated = state.copy(
                    profileName = name.trim().take(24),
                    profileIconId = iconId
                )
                saveManager.saveGame(updated, slot)
                if (_activeSlot.value == slot) {
                    _gameState.value = AntiCheatService.sanitizeCurrency(updated)
                }
            }
        }
    }

    fun switchSlot(slot: Int) {
        if (slot in 1..MAX_SAVE_SLOTS && slot != _activeSlot.value) {
            viewModelScope.launch(Dispatchers.IO) {
                slotOperationMutex.withLock {
                    withContext(Dispatchers.Main) {
                        _isLoadingSave.value = true
                    }
                    try {
                        val currentSlot = _activeSlot.value
                        val currentState = _gameState.value.copy(lastSaveTimestamp = System.currentTimeMillis())
                        saveManager.saveGame(currentState, currentSlot)
                        saveManager.setActiveSlot(slot)
                        val loaded = saveManager.loadGame(slot)
                        val sanitized = AntiCheatService.sanitizeCurrency(loaded)
                        withContext(Dispatchers.Main) {
                            _activeSlot.value = slot
                            _gameState.value = sanitized
                            guessingBot.reset(sanitized.currentRangeMin, sanitized.currentRangeMax)
                            lastIncomeMeasurement = sanitized.money
                            _incomePerSecond.value = BigNumber.ZERO
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            _isLoadingSave.value = false
                        }
                    }
                }
            }
        }
    }

    fun migrateSettingsToGlobal(onResult: (Boolean) -> Unit = {}) {
        if (_isMigratingSettings.value) return
        _isMigratingSettings.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val success = slotOperationMutex.withLock { saveManager.migrateSettingsToGlobal() }
            if (success) {
                val active = saveManager.getActiveSlot()
                val loaded = saveManager.loadGame(active)
                val sanitized = AntiCheatService.sanitizeCurrency(loaded)
                withContext(Dispatchers.Main) {
                    _gameState.value = sanitized
                    updateLocaleRepo(sanitized.settings.locale)
                    _needsSettingsMigration.value = false
                    _isMigratingSettings.value = false
                    onResult(true)
                }
            } else {
                withContext(Dispatchers.Main) {
                    _isMigratingSettings.value = false
                    onResult(false)
                }
            }
        }
    }

    fun migrateLegacySave() {
        viewModelScope.launch(Dispatchers.IO) {
            val success = slotOperationMutex.withLock {
                saveManager.migrateLegacySaveToSlot1()
            }
            if (success) {
                withContext(Dispatchers.Main) {
                    _hasLegacySave.value = false
                }
                loadGame(1)
            }
        }
    }

    fun duplicateSlot(sourceSlot: Int, targetSlot: Int, onResult: (Boolean) -> Unit) {
        if (sourceSlot !in 1..MAX_SAVE_SLOTS || targetSlot !in 1..MAX_SAVE_SLOTS) {
            onResult(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val success = slotOperationMutex.withLock {
                saveManager.duplicateSlot(sourceSlot, targetSlot)
            }
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun restoreBackup(slot: Int, onResult: (Boolean) -> Unit) {
        if (slot !in 1..MAX_SAVE_SLOTS) {
            onResult(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val success = slotOperationMutex.withLock {
                val restored = saveManager.restoreBackup(slot)
                if (restored && _activeSlot.value == slot) {
                    val loaded = saveManager.loadGame(slot)
                    val sanitized = AntiCheatService.sanitizeCurrency(loaded)
                    _gameState.value = sanitized
                    guessingBot.reset(sanitized.currentRangeMin, sanitized.currentRangeMax)
                    lastIncomeMeasurement = sanitized.money
                    _incomePerSecond.value = BigNumber.ZERO
                }
                restored
            }
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun resetSlot(slot: Int) {
        if (slot !in 1..MAX_SAVE_SLOTS) return
        viewModelScope.launch(Dispatchers.IO) {
            slotOperationMutex.withLock {
                saveManager.resetSlot(slot)
                if (_activeSlot.value == slot) {
                    saveManager.setActiveSlot(slot)
                    val globalSettings = saveManager.loadGame(slot).settings
                    val fresh = GameState(settings = globalSettings, tutorialCompleted = globalSettings.tutorialCompleted, lastSaveTimestamp = System.currentTimeMillis())
                    saveManager.saveGame(fresh, slot)
                    _gameState.value = fresh
                    guessingBot.reset(fresh.currentRangeMin, fresh.currentRangeMax)
                    lastIncomeMeasurement = fresh.money
                    _incomePerSecond.value = BigNumber.ZERO
                }
            }
        }
    }

    fun makeGuess(guess: Long) {
        if (!AntiCheatService.validateGuessRate()) return
        val currentState = refreshDailyQuestDay(_gameState.value)
        val result = gameEngine.processGuess(currentState, guess)
        guessingBot.observeGuess(guess.coerceIn(currentState.currentRangeMin, currentState.currentRangeMax), result.feedback)
        if (result.feedback == com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback.CORRECT) {
            guessingBot.reset(result.newState.currentRangeMin, result.newState.currentRangeMax)
        }
        val isCorrect = result.feedback == com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback.CORRECT
        val talentAdjustedState = applyTalentBonuses(currentState, result.newState, isCorrect)
        val progressedState = recordLootDrop(
            recordDailyGuess(currentState, talentAdjustedState, isCorrect),
            isCorrect
        )
        _gameState.value = progressedState
        val activeBossWorld = progressedState.activeBossBattleWorldId
        if (activeBossWorld != null) {
            worldProgressAction(activeBossWorld, if (isCorrect) "boss_hit" else "boss_miss")
        }
        checkAchievements(_gameState.value)
        val finalState = _gameState.value
        if (finalState.achievements.size > currentState.achievements.size) {
            NotificationHelper.showProgressionNotification(getApplication<Application>())
        }
        saveGameAsync()
        GameLogger.log(LogLevel.INFO, LoggerCategory.GUESS, "MAKE_GUESS", "Guess $guess resulted in ${result.feedback}, reward: ${result.reward}")
    }

    private fun checkAchievements(state: GameState) {
        val newAchievements = state.achievements.toMutableSet()
        var nebulaReward = 0L

        val milestones = listOf(
            "correct_10" to (state.correctGuesses >= 10),
            "correct_50" to (state.correctGuesses >= 50),
            "correct_100" to (state.correctGuesses >= 100),
            "correct_500" to (state.correctGuesses >= 500),
            "streak_5" to (state.bestStreak >= 5),
            "streak_10" to (state.bestStreak >= 10),
            "streak_25" to (state.bestStreak >= 25),
            "streak_50" to (state.bestStreak >= 50),
            "millionaire" to (state.money >= BigNumber(1_000_000)),
            "ten_millionaire" to (state.money >= BigNumber(10_000_000)),
            "hundred_millionaire" to (state.money >= BigNumber(100_000_000)),
            "billionaire" to (state.money >= BigNumber(1_000_000_000)),
            "prestige_1" to (state.prestigeCount >= 1),
            "prestige_5" to (state.prestigeCount >= 5),
            "prestige_10" to (state.prestigeCount >= 10),
            "prestige_25" to (state.prestigeCount >= 25),
            "ultra_1" to (state.ultraCount >= 1),
            "ultra_3" to (state.ultraCount >= 3),
            "ultra_10" to (state.ultraCount >= 10),
            "ultra_25" to (state.ultraCount >= 25),
            "playtime_1h" to (state.statistics.playtimeSeconds >= 3_600),
            "playtime_10h" to (state.statistics.playtimeSeconds >= 36_000),
            "playtime_50h" to (state.statistics.playtimeSeconds >= 180_000),
            "frenzy_5" to (state.bestStreak >= 5),
            "frenzy_10" to (state.bestStreak >= 10),
            "frenzy_25" to (state.bestStreak >= 25),
            "frenzy_50" to (state.bestStreak >= 50)
        )

        val achievementRewards = mapOf(
            "correct_10" to 10L, "correct_50" to 20L, "correct_100" to 40L, "correct_500" to 100L,
            "streak_5" to 25L, "streak_10" to 50L, "streak_25" to 100L, "streak_50" to 250L,
            "millionaire" to 50L, "ten_millionaire" to 100L, "hundred_millionaire" to 200L, "billionaire" to 500L,
            "prestige_1" to 25L, "prestige_5" to 75L, "prestige_10" to 150L, "prestige_25" to 300L,
            "ultra_1" to 100L, "ultra_3" to 250L, "ultra_10" to 500L, "ultra_25" to 1000L,
            "playtime_1h" to 25L, "playtime_10h" to 100L, "playtime_50h" to 300L,
            "frenzy_5" to 30L, "frenzy_10" to 75L, "frenzy_25" to 150L, "frenzy_50" to 350L
        )

        for ((id, unlocked) in milestones) {
            if (unlocked && newAchievements.add(id)) {
                nebulaReward += achievementRewards[id] ?: 0L
            }
        }

        if (newAchievements.size > state.achievements.size) {
            _gameState.value = state.copy(
                achievements = newAchievements,
                nebula = state.nebula + BigNumber(nebulaReward)
            )
        }
    }

    fun updateBuyMultiplier(multiplier: String) {
        _gameState.update { it.copy(buyMultiplier = multiplier) }
        saveGameAsync()
    }

    fun buyUpgrade(upgradeId: String, cost: BigNumber) {
        val state = refreshDailyQuestDay(_gameState.value)
        if (state.money >= cost) {
            val currentLevel = state.upgradeLevels[upgradeId] ?: 0
            val upgradeDef = JsonConfigRepository(getApplication()).loadUpgrades().find { it.id == upgradeId }
            val maxLevel = upgradeDef?.maxLevel ?: 999999
            val multCount = when (state.buyMultiplier) {
                "10" -> minOf(10, maxLevel - currentLevel)
                "100" -> minOf(100, maxLevel - currentLevel)
                "MAX" -> {
                    val base = BigNumber(upgradeDef?.baseCost ?: "100")
                    val mult = BigNumber(upgradeDef?.costMultiplier ?: 1.5)
                    var count = 0
                    var totalCost = BigNumber.ZERO
                    var currLevel = currentLevel
                    while (currLevel < maxLevel) {
                        val c = base * mult.pow(currLevel)
                        if (state.money >= totalCost + c) {
                            totalCost += c
                            count++
                            currLevel++
                        } else {
                            break
                        }
                    }
                    maxOf(1, count)
                }
                else -> minOf(1, maxLevel - currentLevel)
            }
            val newLevel = minOf(maxLevel, currentLevel + multCount)
            val newLevels = state.upgradeLevels.toMutableMap()
            newLevels[upgradeId] = newLevel
            val newMoney = state.money - cost
            val newStats = state.statistics.copy(moneySpent = state.statistics.moneySpent + cost)
            _gameState.value = state.copy(money = newMoney, upgradeLevels = newLevels, statistics = newStats, dailyQuestUpgradePurchases = state.dailyQuestUpgradePurchases + 1)
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.UPGRADE, "BUY_UPGRADE", "Bought upgrade $upgradeId to level $newLevel")
        }
    }

    fun prestigeReset() {
        val state = _gameState.value
        val reward = gameEngine.calculatePrestigeReward(state.money, state.prestigeCount)
        val requiredMoney = gameEngine.calculatePrestigeRequirement(state.prestigeCount)
        if (state.money >= requiredMoney && reward > BigNumber.ZERO) {
            val newPrestige = state.prestige + reward
            val newStats = state.statistics.copy(prestigesCount = state.statistics.prestigesCount + 1)
            _gameState.value = state.copy(
                money = BigNumber.ZERO,
                prestige = newPrestige,
                upgradeLevels = emptyMap(),
                currentRangeMin = 1,
                currentRangeMax = Difficulty.rangeMax(state.difficultyId, 0),
                statistics = newStats,
                prestigeCount = state.prestigeCount + 1
            )
            guessingBot.reset(_gameState.value.currentRangeMin, _gameState.value.currentRangeMax)
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.PRESTIGE, "PRESTIGE_RESET", "Prestige reset performed (#${state.prestigeCount + 1}), awarded $reward Prestige.")
        }
    }

    fun ultraReset() {
        val state = _gameState.value
        val reward = gameEngine.calculateUltraReward(state.money, state.ultraCount)
        val requiredMoney = gameEngine.calculateUltraRequirement(state.ultraCount)
        val canUltra = state.money >= requiredMoney && state.prestige >= BigNumber(1_000)
        if (canUltra && reward > BigNumber.ZERO) {
            val newUltra = state.ultra + reward
            val newStats = state.statistics.copy(ultrasCount = state.statistics.ultrasCount + 1)
            _gameState.value = state.copy(
                money = BigNumber.ZERO,
                prestige = BigNumber.ZERO,
                ultra = newUltra,
                upgradeLevels = emptyMap(),
                prestigeUpgradeLevels = emptyMap(),
                statistics = newStats,
                ultraCount = state.ultraCount + 1
            )
            guessingBot.reset(_gameState.value.currentRangeMin, _gameState.value.currentRangeMax)
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.ULTRA, "ULTRA_RESET", "Ultra reset performed (#${state.ultraCount + 1}), awarded $reward Ultra.")
        }
    }

    fun buyShopItem(itemId: String, costNebula: Long) {
        val state = _gameState.value
        val cost = BigNumber(costNebula)
        if (state.nebula >= cost && !state.shopPurchases.contains(itemId)) {
            val newPurchases = state.shopPurchases + itemId
            val newNebula = state.nebula - cost
            val autoActive = if (itemId == "auto_clicker_basic") true else state.autoClickerActive
            val cosmetic = if (itemId.startsWith("cosmetic_")) itemId else state.equippedCosmeticId
            _gameState.value = state.copy(
                nebula = newNebula,
                shopPurchases = newPurchases,
                equippedCosmeticId = cosmetic,
                autoClickerActive = autoActive
            )
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.SHOP, "BUY_SHOP", "Purchased shop item $itemId")
        }
    }

    fun equipCosmetic(itemId: String) {
        val state = _gameState.value
        if (!itemId.startsWith("cosmetic_") || itemId !in state.shopPurchases) return
        _gameState.value = state.copy(equippedCosmeticId = itemId)
        saveGameAsync()
    }

    fun setMutatorActive(mutatorId: String, active: Boolean) {
        val state = _gameState.value
        val valid = setOf("mut_hardcore", "mut_speed", "mut_blind", "mut_tax")
        if (mutatorId !in valid) return
        val next = state.activeMutators.toMutableSet().apply {
            if (active) add(mutatorId) else remove(mutatorId)
        }.toSet()
        val rangeMax = gameEngine.getRangeMax(state.copy(activeMutators = next))
        val target = if (rangeMax >= 1L) kotlin.random.Random.nextLong(1L, rangeMax + 1L) else 1L
        val updated = state.copy(
            activeMutators = next,
            currentRangeMax = rangeMax,
            targetNumber = target
        )
        _gameState.value = updated
        saveGameAsync()
        GameLogger.log(LogLevel.INFO, LoggerCategory.SHOP, "MUTATOR_TOGGLE", "Mutator $mutatorId active=$active")
    }

    fun activateChallengeBuilder(mutators: Set<String>) {
        val state = _gameState.value
        val valid = setOf("mut_hardcore", "mut_speed", "mut_blind", "mut_tax")
        val selected = mutators.intersect(valid)
        val next = state.copy(activeMutators = selected)
        val rangeMax = gameEngine.getRangeMax(next)
        _gameState.value = next.copy(
            currentRangeMax = rangeMax,
            targetNumber = kotlin.random.Random.nextLong(1L, rangeMax + 1L)
        )
        saveGameAsync()
    }

    private fun bulkUpgradeCount(upgrade: com.jarrlyyy.guessthenumber.domain.model.UpgradeDef, currentLevel: Int, currency: BigNumber): Int {
        if (currentLevel >= upgrade.maxLevel) return 0
        return when (_gameState.value.buyMultiplier) {
            "10" -> minOf(10, upgrade.maxLevel - currentLevel)
            "100" -> minOf(100, upgrade.maxLevel - currentLevel)
            "MAX" -> {
                var count = 0
                var totalCost = BigNumber.ZERO
                var level = currentLevel
                val base = BigNumber(upgrade.baseCost)
                val mult = BigNumber(upgrade.costMultiplier)
                while (level < upgrade.maxLevel) {
                    val nextCost = base * mult.pow(level)
                    if (currency < totalCost + nextCost) break
                    totalCost += nextCost
                    count++
                    level++
                }
                count
            }
            else -> 1
        }
    }

    fun buyPrestigeUpgrade(upgradeId: String, cost: BigNumber) {
        val state = _gameState.value
        val upgrade = JsonConfigRepository(getApplication()).loadPrestigeUpgrades().find { it.id == upgradeId } ?: return
        val currentLevel = state.prestigeUpgradeLevels[upgradeId] ?: 0
        val count = bulkUpgradeCount(upgrade, currentLevel, state.prestige)
        if (count <= 0 || state.prestige < cost) return

        val newLevel = minOf(upgrade.maxLevel, currentLevel + count)
        val newLevels = state.prestigeUpgradeLevels.toMutableMap()
        newLevels[upgradeId] = newLevel
        val newPrestige = state.prestige - cost
        _gameState.value = state.copy(prestige = newPrestige, prestigeUpgradeLevels = newLevels)
        saveGameAsync()
        GameLogger.log(LogLevel.INFO, LoggerCategory.UPGRADE, "BUY_PRESTIGE_UPGRADE", "Bought prestige upgrade $upgradeId to level $newLevel")
    }

    fun buyUltraUpgrade(upgradeId: String, cost: BigNumber) {
        val state = _gameState.value
        val upgrade = JsonConfigRepository(getApplication()).loadUltraUpgrades().find { it.id == upgradeId } ?: return
        val currentLevel = state.ultraUpgradeLevels[upgradeId] ?: 0
        val count = bulkUpgradeCount(upgrade, currentLevel, state.ultra)
        if (count <= 0 || state.ultra < cost) return

        val newLevel = minOf(upgrade.maxLevel, currentLevel + count)
        val newLevels = state.ultraUpgradeLevels.toMutableMap()
        newLevels[upgradeId] = newLevel
        val newUltra = state.ultra - cost
        _gameState.value = state.copy(ultra = newUltra, ultraUpgradeLevels = newLevels)
        saveGameAsync()
        GameLogger.log(LogLevel.INFO, LoggerCategory.UPGRADE, "BUY_ULTRA_UPGRADE", "Bought ultra upgrade $upgradeId to level $newLevel")
    }

    fun buyTalent(talentId: String, cost: Long) {
        val state = _gameState.value
        val parents = mapOf("talent_speed_2" to "talent_speed_1", "talent_crit_2" to "talent_crit_1", "talent_reward_1" to "talent_crit_2", "talent_master_1" to "talent_reward_1")
        if (talentId in state.prestigeShopPurchases || state.prestige < BigNumber(cost)) return
        val parent = parents[talentId]
        if (parent != null && parent !in state.prestigeShopPurchases) return
        _gameState.value = state.copy(prestige = state.prestige - BigNumber(cost), prestigeShopPurchases = state.prestigeShopPurchases + talentId)
        saveGameAsync()
        GameLogger.log(LogLevel.INFO, LoggerCategory.UPGRADE, "BUY_TALENT", "Unlocked talent $talentId")
    }

    private fun applyTalentBonuses(before: GameState, after: GameState, correct: Boolean): GameState {
        if (!correct) return after
        val earned = after.money - before.money
        if (earned <= BigNumber.ZERO) return after
        var bonusMoney = BigNumber.ZERO
        if ("talent_reward_1" in before.prestigeShopPurchases) bonusMoney += earned * BigNumber(0.5)
        val masteryLevel = (before.worldMasteryLevels[before.activeWorldId] ?: 0).coerceIn(0, 10)
        if (masteryLevel > 0) bonusMoney += earned * BigNumber(masteryLevel * 0.02)
        val criticalChance = when { "talent_crit_2" in before.prestigeShopPurchases -> 0.15; "talent_crit_1" in before.prestigeShopPurchases -> 0.05; else -> 0.0 }
        if (criticalChance > 0.0 && Random.nextDouble() < criticalChance) bonusMoney += earned
        if ("verdant_guardian" in before.equippedRelicIds) bonusMoney += earned * BigNumber(0.10)
        if ("crystal_golem" in before.equippedRelicIds && Random.nextDouble() < 0.10) bonusMoney += earned * BigNumber(0.5)
        if ("ember_dragon" in before.equippedRelicIds) bonusMoney += earned * BigNumber(0.25)
        if ("nebula_titan" in before.equippedRelicIds) bonusMoney += earned * BigNumber(0.25)

        // Relic set bonuses now scale across the full four-world relic collection.
        val equippedBossRelics = before.equippedRelicIds.intersect(
            setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")
        )
        val relicSetBonus = when (equippedBossRelics.size) {
            2 -> 0.10
            3 -> 0.20
            4 -> 0.35
            else -> 0.0
        }
        if (relicSetBonus > 0.0) bonusMoney += earned * BigNumber(relicSetBonus)
        if (before.homeBaseLevel > 0) bonusMoney += earned * BigNumber((before.homeBaseLevel * 0.02).coerceAtMost(0.40))
        val secretForActiveWorld = mapOf(
            "verdant_grove" to "whispering_hollow",
            "crystal_caverns" to "shard_archive",
            "ember_summit" to "ashen_vault",
            "nebula_rift" to "lost_observatory"
        )[before.activeWorldId]
        if (secretForActiveWorld != null && secretForActiveWorld in before.discoveredSecretIds) bonusMoney += earned * BigNumber(0.05)
        val bonusNebula = (if ("talent_master_1" in before.prestigeShopPurchases) 1L else 0L) +
            (if ("ember_dragon" in before.equippedRelicIds) 1L else 0L) +
            (if (equippedBossRelics.size == 4) 1L else 0L) +
            (if (before.discoveredSecretIds.size >= 4) 1L else 0L) +
            (if (before.activeWorldId == "ember_summit" && "ashen_vault" in before.discoveredSecretIds) 1L else 0L)
        return after.copy(money = after.money + bonusMoney, nebula = after.nebula + BigNumber(bonusNebula), statistics = after.statistics.copy(moneyEarned = after.statistics.moneyEarned + bonusMoney, nebulaEarned = after.statistics.nebulaEarned + bonusNebula))
    }

    fun buyPrestigeShopItem(itemId: String, cost: Long) {
        val state = _gameState.value
        val costBig = BigNumber(cost)
        if (state.prestige >= costBig && !state.prestigeShopPurchases.contains(itemId)) {
            val newPurchases = state.prestigeShopPurchases + itemId
            val newPrestige = state.prestige - costBig
            _gameState.value = state.copy(prestige = newPrestige, prestigeShopPurchases = newPurchases)
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.SHOP, "BUY_PRESTIGE_SHOP", "Purchased prestige shop item $itemId")
        }
    }

    fun buyUltraShopItem(itemId: String, cost: Long) {
        val state = _gameState.value
        val costBig = BigNumber(cost)
        if (state.ultra >= costBig && !state.ultraShopPurchases.contains(itemId)) {
            val newPurchases = state.ultraShopPurchases + itemId
            val newUltra = state.ultra - costBig
            _gameState.value = state.copy(ultra = newUltra, ultraShopPurchases = newPurchases)
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.SHOP, "BUY_ULTRA_SHOP", "Purchased ultra shop item $itemId")
        }
    }

    fun exportSave(onExported: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = saveManager.exportSave(_activeSlot.value)
            withContext(Dispatchers.Main) {
                onExported(json)
            }
        }
    }

    fun importSave(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = slotOperationMutex.withLock {
                saveManager.importSave(json, _activeSlot.value)
            }
            if (success) {
                val loaded = saveManager.loadGame(_activeSlot.value)
                val sanitized = AntiCheatService.sanitizeCurrency(loaded)
                withContext(Dispatchers.Main) {
                    _gameState.value = sanitized
                    _hasAnySave.value = true
                    guessingBot.reset(sanitized.currentRangeMin, sanitized.currentRangeMax)
                    lastIncomeMeasurement = sanitized.money
                    _incomePerSecond.value = BigNumber.ZERO
                    onResult(true)
                }
            } else {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun resetData() {
        viewModelScope.launch(Dispatchers.IO) {
            slotOperationMutex.withLock {
                saveManager.resetData()
                val fresh = GameState()
                saveManager.setActiveSlot(1)
                saveManager.saveGame(fresh, 1)
                _activeSlot.value = 1
                _gameState.value = fresh
                guessingBot.reset(fresh.currentRangeMin, fresh.currentRangeMax)
                lastIncomeMeasurement = fresh.money
                _incomePerSecond.value = BigNumber.ZERO
            }
            withContext(Dispatchers.Main) { backgroundMusicManager.stop() }
        }
    }

    fun updateSettings(newSettings: GameSettings) {
        val oldSettings = _gameState.value.settings
        if (newSettings.locale != oldSettings.locale) {
            updateLocaleRepo(newSettings.locale)
        }
        _gameState.update { it.copy(settings = newSettings) }
        viewModelScope.launch(Dispatchers.IO) { saveManager.saveAppPreferences(newSettings) }
        GameLogger.configureStorage(File(getApplication<Application>().filesDir, "game_logs.txt"), newSettings.saveLogsToStorage)
        saveGameAsync()

        val context = getApplication<Application>()
        val workManager = WorkManager.getInstance(context)
        if (!newSettings.notificationsEnabled || !newSettings.notificationRemindersEnabled) {
            workManager.cancelUniqueWork(GameReminderWorker.WORK_NAME)
        } else if (
            !oldSettings.notificationsEnabled ||
            !oldSettings.notificationRemindersEnabled ||
            newSettings.notificationIntervalHours != oldSettings.notificationIntervalHours
        ) {
            val intervalHours = newSettings.notificationIntervalHours.coerceIn(12L, 48L)
            val workRequest = PeriodicWorkRequestBuilder<GameReminderWorker>(
                intervalHours, TimeUnit.HOURS
            ).build()
            workManager.enqueueUniquePeriodicWork(
                GameReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        GameLogger.log(LogLevel.INFO, LoggerCategory.UI, "UPDATE_SETTINGS", "Updated game settings.")
    }

    fun getAppPreferencesJson(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val raw = saveManager.getAppPreferencesJson()
            withContext(Dispatchers.Main) { onResult(raw) }
        }
    }

    fun applyAppPreferencesJson(raw: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = slotOperationMutex.withLock { saveManager.applyAppPreferencesJson(raw) }
            if (success) {
                val loaded = saveManager.loadGame(_activeSlot.value)
                withContext(Dispatchers.Main) {
                    _gameState.value = AntiCheatService.sanitizeCurrency(loaded)
                    updateLocaleRepo(_gameState.value.settings.locale)
                    onResult(true)
                }
            } else withContext(Dispatchers.Main) { onResult(false) }
        }
    }

    fun getSaveJson(slot: Int, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val raw = saveManager.getSaveJson(slot)
            withContext(Dispatchers.Main) { onResult(raw) }
        }
    }

    fun applySaveJson(slot: Int, raw: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = slotOperationMutex.withLock { saveManager.applySaveJson(slot, raw) }
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun updateBackgroundMusicPath(path: String?) {
        val updatedSettings = _gameState.value.settings.copy(backgroundMusicPath = path)
        _gameState.update { it.copy(settings = updatedSettings) }
        viewModelScope.launch(Dispatchers.IO) { saveManager.saveAppPreferences(updatedSettings) }
        saveGameAsync()
        backgroundMusicManager.setSourceAndPlay(path)
        GameLogger.log(LogLevel.INFO, LoggerCategory.UI, "BG_MUSIC", "Updated background music path: $path")
    }

    fun playBackgroundMusic() {
        backgroundMusicManager.play()
    }

    fun pauseBackgroundMusic() {
        backgroundMusicManager.pause()
    }

    fun stopBackgroundMusic() {
        backgroundMusicManager.stop()
    }

    fun seekBackgroundMusic(position: Int) {
        backgroundMusicManager.seekTo(position)
    }

    fun executeDevCommand(command: String): String {
        if (!BuildConfig.DEBUG) return "Developer commands are disabled in release builds."
        val current = _gameState.value
        val result = commandExecutor.execute(command, current) { updated ->
            _gameState.value = updated
            saveGameAsync()
        }
        return result
    }

    fun dismissCrash() {
        AppErrorHandler.clearCrashMarker()
        _hasPreviousCrash.value = false

        // Only begin save loading and background game work after crash recovery is dismissed.
        loadGame()
        startAutoSave()
        startPlaytimeTimer()
        startRandomEvents()
        startAutoClicker()
    }

    fun claimChallenge(challengeId: String, rewardNebula: Long) {
        val (updated, claimed) = gameEngine.claimChallenge(_gameState.value, challengeId, rewardNebula)
        if (claimed) {
            _gameState.value = updated
            saveGameAsync()
        }
    }

    private fun recordLootDrop(state: GameState, correct: Boolean): GameState {
        if (!correct) return state
        val roll = Random.nextInt(1000)
        val tier = when {
            roll < 10 -> "epic"
            roll < 60 -> "rare"
            roll < 240 -> "common"
            else -> null
        } ?: return state
        return when (tier) {
            "epic" -> state.copy(lootEpicChests = state.lootEpicChests + 1, lootDropsFound = state.lootDropsFound + 1, lastLootDropTier = tier, lootEventId = state.lootEventId + 1)
            "rare" -> state.copy(lootRareChests = state.lootRareChests + 1, lootDropsFound = state.lootDropsFound + 1, lastLootDropTier = tier, lootEventId = state.lootEventId + 1)
            else -> state.copy(lootCommonChests = state.lootCommonChests + 1, lootDropsFound = state.lootDropsFound + 1, lastLootDropTier = tier, lootEventId = state.lootEventId + 1)
        }
    }

    private fun progressionRewardBonus(state: GameState, baseMoney: BigNumber): Pair<BigNumber, Long> {
        val relicCount = state.equippedRelicIds.intersect(
            setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")
        ).size
        val setRate = when (relicCount) {
            2 -> 0.10
            3 -> 0.20
            4 -> 0.35
            else -> 0.0
        }
        val masteryRate = ((state.worldMasteryLevels[state.activeWorldId] ?: 0).coerceIn(0, 10) * 0.02)
        val sanctuaryRate = (state.homeBaseLevel * 0.01).coerceAtMost(0.20)
        val secretId = mapOf(
            "verdant_grove" to "whispering_hollow",
            "crystal_caverns" to "shard_archive",
            "ember_summit" to "ashen_vault",
            "nebula_rift" to "lost_observatory"
        )[state.activeWorldId]
        val secretRate = if (secretId != null && secretId in state.discoveredSecretIds) 0.05 else 0.0
        val bonusMoney = baseMoney * BigNumber(setRate + masteryRate + sanctuaryRate + secretRate)
        val bonusNebula = (if (relicCount == 4) 10L else 0L) +
            (if ((state.worldMasteryLevels[state.activeWorldId] ?: 0) >= 5) 5L else 0L) +
            (if (secretId != null && secretId in state.discoveredSecretIds) 5L else 0L)
        return bonusMoney to bonusNebula
    }

    fun worldProgressAction(worldId: String, action: String) {
        val state = _gameState.value
        val totalUpgradeLevels = state.upgradeLevels.values.sum()
        if (worldId !in listOf("verdant_grove", "crystal_caverns", "ember_summit", "nebula_rift")) return
        val bossId = when (worldId) { "verdant_grove" -> "verdant_guardian"; "crystal_caverns" -> "crystal_golem"; "ember_summit" -> "ember_dragon"; else -> "nebula_titan" }
        if (action == "unlock") {
            val canUnlock = when (worldId) {
                "crystal_caverns" -> "verdant_guardian" in state.defeatedBossIds && state.correctGuesses >= 25 && totalUpgradeLevels >= 5
                "ember_summit" -> "crystal_golem" in state.defeatedBossIds && state.correctGuesses >= 100 && totalUpgradeLevels >= 15 && state.prestigeCount >= 1
                "nebula_rift" -> "ember_dragon" in state.defeatedBossIds && state.correctGuesses >= 250 && totalUpgradeLevels >= 40 && state.prestigeCount >= 3 && state.ultraCount >= 1
                else -> worldId == "verdant_grove"
            }
            if (canUnlock) {
                _gameState.value = state.copy(unlockedWorldIds = state.unlockedWorldIds + worldId, activeWorldId = worldId)
                saveGameAsync()
                NotificationHelper.showProgressionNotification(getApplication<Application>())
                GameLogger.log(LogLevel.INFO, LoggerCategory.GAME, "WORLD_UNLOCKED", "Unlocked world $worldId")
            }
            return
        }
        if (action == "boss") {
            if (worldId !in state.unlockedWorldIds || (bossId in state.defeatedBossIds && !state.endlessRiftActive)) return
            if (state.activeBossBattleWorldId == worldId) { _gameState.value = state.copy(activeBossBattleWorldId = null); saveGameAsync(); return }
            val canFight = state.endlessRiftActive || when (worldId) {
                "verdant_grove" -> state.correctGuesses >= 10 && totalUpgradeLevels >= 3
                "crystal_caverns" -> state.correctGuesses >= 50 && totalUpgradeLevels >= 10
                "ember_summit" -> state.correctGuesses >= 150 && totalUpgradeLevels >= 25 && state.prestigeCount >= 1
                else -> state.correctGuesses >= 500 && totalUpgradeLevels >= 75 && state.prestigeCount >= 5 && state.ultraCount >= 1
            }
            if (canFight) { _gameState.value = state.copy(activeWorldId = worldId, activeBossBattleWorldId = worldId); saveGameAsync(); GameLogger.log(LogLevel.INFO, LoggerCategory.GAME, "BOSS_BATTLE_STARTED", "Started boss battle $bossId") }
            return
        }
        if (state.activeBossBattleWorldId != worldId || worldId !in state.unlockedWorldIds || (bossId in state.defeatedBossIds && !state.endlessRiftActive)) return
        val baseBossHp = when (worldId) { "verdant_grove" -> 3; "crystal_caverns" -> 5; "ember_summit" -> 7; else -> 10 }
        val bossHp = baseBossHp + if (state.endlessRiftActive) ((state.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0
        val damage = state.bossBattleProgress[bossId] ?: 0
        if (action == "boss_miss") {
            val misses = (state.bossBattleMistakes[bossId] ?: 0) + 1
            val phase = ((damage * 3) / bossHp + 1).coerceIn(1, 3)
            val missInterval = when (phase) { 1 -> 4; 2 -> 3; else -> 2 }
            val regressedDamage = if (misses % missInterval == 0) (damage - 1).coerceAtLeast(0) else damage
            _gameState.value = state.copy(bossBattleMistakes = state.bossBattleMistakes + (bossId to misses), bossBattleProgress = state.bossBattleProgress + (bossId to regressedDamage))
            saveGameAsync()
            return
        }
        if (action != "boss_hit") return
        val nextDamage = (state.bossBattleProgress[bossId] ?: 0) + 1
        if (nextDamage < bossHp) {
            val oldPhase = ((damage * 3) / bossHp + 1).coerceIn(1, 3)
            val newPhase = ((nextDamage * 3) / bossHp + 1).coerceIn(1, 3)
            val mistakes = if (newPhase != oldPhase) 0 else (state.bossBattleMistakes[bossId] ?: 0)
            _gameState.value = state.copy(bossBattleProgress = state.bossBattleProgress + (bossId to nextDamage), bossBattleMistakes = state.bossBattleMistakes + (bossId to mistakes))
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.GAME, "WORLD_BOSS_HIT", "Hit boss $bossId: $nextDamage/$bossHp")
            return
        }
        val baseMoneyReward = when (worldId) { "verdant_grove" -> BigNumber(25_000); "crystal_caverns" -> BigNumber(100_000); "ember_summit" -> BigNumber(500_000); else -> BigNumber(5_000_000) }
        val baseNebulaReward = when (worldId) { "verdant_grove" -> 10L; "crystal_caverns" -> 25L; "ember_summit" -> 75L; else -> 250L }
        if (state.endlessRiftActive) {
            val tierMultiplier = state.endlessRiftTier.coerceAtLeast(1)
            val (baseBonusMoney, baseBonusNebula) = progressionRewardBonus(state, baseMoneyReward)
            val moneyReward = (baseMoneyReward + baseBonusMoney) * BigNumber(tierMultiplier)
            val nebulaReward = (baseNebulaReward + baseBonusNebula) * tierMultiplier.toLong()
            val order = listOf("verdant_grove", "crystal_caverns", "ember_summit", "nebula_rift")
            val nextWorld = order[(order.indexOf(worldId).coerceAtLeast(0) + 1) % order.size]
            val nextBoss = when (nextWorld) { "verdant_grove" -> "verdant_guardian"; "crystal_caverns" -> "crystal_golem"; "ember_summit" -> "ember_dragon"; else -> "nebula_titan" }
            val nextTier = state.endlessRiftTier + 1
            _gameState.value = state.copy(
                money = state.money + moneyReward,
                nebula = state.nebula + BigNumber(nebulaReward),
                worldBossVictories = state.worldBossVictories + 1,
                bossBattleProgress = state.bossBattleProgress + (bossId to bossHp) + (nextBoss to 0),
                bossBattleMistakes = state.bossBattleMistakes + (nextBoss to 0),
                activeWorldId = nextWorld,
                activeBossBattleWorldId = nextWorld,
                endlessRiftTier = nextTier,
                endlessRiftBestTier = maxOf(state.endlessRiftBestTier, state.endlessRiftTier),
                statistics = state.statistics.copy(moneyEarned = state.statistics.moneyEarned + moneyReward, nebulaEarned = state.statistics.nebulaEarned + nebulaReward)
            )
            saveGameAsync()
            if (state.endlessRiftTier % 5 == 0) {
                NotificationHelper.showProgressionNotification(getApplication<Application>())
            }
            GameLogger.log(LogLevel.INFO, LoggerCategory.GAME, "ENDLESS_RIFT_CLEARED", "Cleared Rift tier ${state.endlessRiftTier}")
            return
        }
        val (bonusMoney, bonusNebula) = progressionRewardBonus(state, baseMoneyReward)
        val finalMoneyReward = baseMoneyReward + bonusMoney
        val finalNebulaReward = baseNebulaReward + bonusNebula
        _gameState.value = state.copy(
            money = state.money + finalMoneyReward,
            nebula = state.nebula + BigNumber(finalNebulaReward),
            defeatedBossIds = state.defeatedBossIds + bossId,
            worldBossVictories = state.worldBossVictories + 1,
            bossBattleProgress = state.bossBattleProgress + (bossId to bossHp),
            activeBossBattleWorldId = null,
            relicInventory = state.relicInventory + bossId,
            codexEntries = state.codexEntries + setOf("world:$worldId", "boss:$bossId"),
            worldMasteryLevels = state.worldMasteryLevels + (worldId to ((state.worldMasteryLevels[worldId] ?: 0) + 1)),
            statistics = state.statistics.copy(
                moneyEarned = state.statistics.moneyEarned + finalMoneyReward,
                nebulaEarned = state.statistics.nebulaEarned + finalNebulaReward
            )
        )
        saveGameAsync()
        NotificationHelper.showProgressionNotification(getApplication<Application>())
        GameLogger.log(LogLevel.INFO, LoggerCategory.GAME, "WORLD_BOSS_DEFEATED", "Defeated boss $bossId")
    }

    /** Handles persistent v2 progression actions. Defaults in GameState preserve older saves. */
    fun progressionAction(action: String, id: String) {
        val state = _gameState.value
        val updated = when (action) {
            "rift_start" -> {
                val requiredBosses = setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")
                if (state.endlessRiftActive || !state.defeatedBossIds.containsAll(requiredBosses)) return
                val tier = maxOf(1, state.endlessRiftBestTier + 1)
                state.copy(
                    endlessRiftActive = true,
                    endlessRiftTier = tier,
                    activeWorldId = "verdant_grove",
                    activeBossBattleWorldId = "verdant_grove",
                    bossBattleProgress = state.bossBattleProgress + ("verdant_guardian" to 0),
                    bossBattleMistakes = state.bossBattleMistakes + ("verdant_guardian" to 0)
                )
            }
            "rift_stop" -> {
                if (!state.endlessRiftActive) return
                state.copy(endlessRiftActive = false, activeBossBattleWorldId = null)
            }
            "select_world" -> {
                if (id !in state.unlockedWorldIds) return
                if (state.endlessRiftActive && id != state.activeWorldId) return
                state.copy(activeWorldId = id, activeBossBattleWorldId = if (state.activeBossBattleWorldId == id) id else null)
            }
            "secret" -> {
                val secretWorld = mapOf("whispering_hollow" to "verdant_grove", "shard_archive" to "crystal_caverns", "ashen_vault" to "ember_summit", "lost_observatory" to "nebula_rift")[id] ?: return
                val threshold = mapOf("whispering_hollow" to 50L, "shard_archive" to 150L, "ashen_vault" to 300L, "lost_observatory" to 600L)[id] ?: return
                if (id in state.discoveredSecretIds || secretWorld !in state.unlockedWorldIds || state.correctGuesses < threshold) return
                val moneyReward = mapOf(
                    "whispering_hollow" to BigNumber(10_000),
                    "shard_archive" to BigNumber(50_000),
                    "ashen_vault" to BigNumber(250_000),
                    "lost_observatory" to BigNumber(1_000_000)
                )[id] ?: BigNumber.ZERO
                val nebulaReward = mapOf("whispering_hollow" to 5L, "shard_archive" to 15L, "ashen_vault" to 50L, "lost_observatory" to 150L)[id] ?: 0L
                state.copy(
                    money = state.money + moneyReward,
                    nebula = state.nebula + BigNumber(nebulaReward),
                    discoveredSecretIds = state.discoveredSecretIds + id,
                    codexEntries = state.codexEntries + "secret:$id",
                    statistics = state.statistics.copy(
                        moneyEarned = state.statistics.moneyEarned + moneyReward,
                        nebulaEarned = state.statistics.nebulaEarned + nebulaReward
                    )
                )
            }
            "equip_relic" -> {
                if (id !in state.relicInventory) return
                val equipped = if (id in state.equippedRelicIds) state.equippedRelicIds - id else (state.equippedRelicIds + id).toList().takeLast(4).toSet()
                state.copy(equippedRelicIds = equipped)
            }
            "home_upgrade" -> {
                val cost = BigNumber(50_000L * (state.homeBaseLevel + 1L))
                if (state.money < cost || state.homeBaseLevel >= 20) return
                val nextLevel = state.homeBaseLevel + 1
                val milestoneNebula = if (nextLevel % 5 == 0) (nextLevel / 5L) * 25L else 0L
                val milestoneEntries = if (nextLevel in setOf(5, 10, 15, 20)) state.codexEntries + "sanctuary:$nextLevel" else state.codexEntries
                state.copy(
                    money = state.money - cost,
                    nebula = state.nebula + BigNumber(milestoneNebula),
                    homeBaseLevel = nextLevel,
                    codexEntries = milestoneEntries,
                    statistics = state.statistics.copy(
                        moneySpent = state.statistics.moneySpent + cost,
                        nebulaEarned = state.statistics.nebulaEarned + milestoneNebula
                    )
                )
            }
            "claim_codex" -> {
                val requiredEntries = setOf(
                    "world:verdant_grove", "world:crystal_caverns", "world:ember_summit", "world:nebula_rift",
                    "boss:verdant_guardian", "boss:crystal_golem", "boss:ember_dragon", "boss:nebula_titan",
                    "secret:whispering_hollow", "secret:shard_archive", "secret:ashen_vault", "secret:lost_observatory"
                )
                if (state.codexRewardClaimed || !state.codexEntries.containsAll(requiredEntries)) return
                state.copy(nebula = state.nebula + BigNumber(100), codexRewardClaimed = true, statistics = state.statistics.copy(nebulaEarned = state.statistics.nebulaEarned + 100))
            }
            "claim_endgame_reward" -> {
                val allBosses = setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")
                val reward = when (id) {
                    "all_bosses" -> if (state.defeatedBossIds.containsAll(allBosses)) 250L to BigNumber(2_000_000) else return
                    "all_secrets" -> if (state.discoveredSecretIds.containsAll(setOf("whispering_hollow", "shard_archive", "ashen_vault", "lost_observatory"))) 200L to BigNumber(1_000_000) else return
                    "all_relics" -> if (state.relicInventory.containsAll(allBosses)) 300L to BigNumber(1_000_000) else return
                    "sanctuary_10" -> if (state.homeBaseLevel >= 10) 500L to BigNumber(5_000_000) else return
                    "rift_tier_10" -> if (state.endlessRiftBestTier >= 10) 1_000L to BigNumber(10_000_000) else return
                    "mastery_all_5" -> if (listOf("verdant_grove", "crystal_caverns", "ember_summit", "nebula_rift").all { (state.worldMasteryLevels[it] ?: 0) >= 5 }) 400L to BigNumber(5_000_000) else return
                    else -> return
                }
                if (id in state.endgameRewardsClaimed) return
                val nebulaReward = reward.first
                val moneyReward = reward.second
                state.copy(
                    money = state.money + moneyReward,
                    nebula = state.nebula + BigNumber(nebulaReward),
                    endgameRewardsClaimed = state.endgameRewardsClaimed + id,
                    statistics = state.statistics.copy(
                        moneyEarned = state.statistics.moneyEarned + moneyReward,
                        nebulaEarned = state.statistics.nebulaEarned + nebulaReward
                    )
                )
            }
            "mastery" -> {
                val bossForMastery = mapOf("verdant_grove" to "verdant_guardian", "crystal_caverns" to "crystal_golem", "ember_summit" to "ember_dragon", "nebula_rift" to "nebula_titan")[id] ?: return
                if (id !in state.unlockedWorldIds || bossForMastery !in state.defeatedBossIds) return
                val current = state.worldMasteryLevels[id] ?: 1
                val secretForWorld = mapOf(
                    "verdant_grove" to "whispering_hollow",
                    "crystal_caverns" to "shard_archive",
                    "ember_summit" to "ashen_vault",
                    "nebula_rift" to "lost_observatory"
                )[id]
                val baseCost = BigNumber(10L * (current + 1L))
                val secretDiscount = if (secretForWorld != null && secretForWorld in state.discoveredSecretIds) 0.20 else 0.0
                val sanctuaryDiscount = (state.homeBaseLevel * 0.01).coerceAtMost(0.20)
                val fullRelicSetDiscount = if (
                    state.equippedRelicIds.containsAll(setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan"))
                ) 0.10 else 0.0
                val totalDiscount = (secretDiscount + sanctuaryDiscount + fullRelicSetDiscount).coerceAtMost(0.40)
                val cost = baseCost * BigNumber(1.0 - totalDiscount)
                if (current >= 10 || state.nebula < cost) return
                val nextLevel = current + 1
                val milestoneNebula = when (nextLevel) {
                    5 -> 25L
                    10 -> 100L
                    else -> 0L
                }
                val milestoneEntries = if (nextLevel == 5 || nextLevel == 10) {
                    state.codexEntries + "mastery:$id:$nextLevel"
                } else state.codexEntries
                state.copy(
                    nebula = state.nebula - cost + BigNumber(milestoneNebula),
                    worldMasteryLevels = state.worldMasteryLevels + (id to nextLevel),
                    codexEntries = milestoneEntries,
                    statistics = state.statistics.copy(nebulaEarned = state.statistics.nebulaEarned + milestoneNebula)
                )
            }
            else -> return
        }
        if (updated != state) {
            _gameState.value = updated
            saveGameAsync()
            val isHomeBaseMilestone = action == "home_upgrade" && updated.homeBaseLevel in setOf(1, 5, 10, 15, 20)
            if (action == "secret" || action == "claim_codex" || isHomeBaseMilestone ||
                (action == "rift_start" && updated.endlessRiftTier > state.endlessRiftBestTier)
            ) {
                NotificationHelper.showProgressionNotification(getApplication<Application>())
            }
            GameLogger.log(LogLevel.INFO, LoggerCategory.GAME, "PROGRESSION_ACTION", "$action:$id")
        }
    }

    fun settleStake(stakeAmount: BigNumber, payout: BigNumber, won: Boolean) {
        val state = _gameState.value
        if (stakeAmount <= BigNumber.ZERO || state.money < stakeAmount) return
        val updatedMoney = if (won) state.money - stakeAmount + payout else state.money - stakeAmount
        _gameState.value = state.copy(money = updatedMoney, statistics = state.statistics.copy(moneyEarned = if (won) state.statistics.moneyEarned + payout else state.statistics.moneyEarned, moneySpent = state.statistics.moneySpent + stakeAmount))
        saveGameAsync()
    }

    fun openLootChest(tier: String) {
        val state = _gameState.value
        val available = when (tier) {
            "common" -> state.lootCommonChests
            "rare" -> state.lootRareChests
            "epic" -> state.lootEpicChests
            else -> 0
        }
        if (available <= 0) return
        val moneyReward = when (tier) {
            "common" -> BigNumber(5_000)
            "rare" -> BigNumber(25_000)
            "epic" -> BigNumber(100_000)
            else -> BigNumber.ZERO
        }
        val nebulaReward = when (tier) {
            "common" -> 5L
            "rare" -> 25L
            "epic" -> 100L
            else -> 0L
        }
        val giveNebula = Random.nextBoolean()
        val updated = when (tier) {
            "common" -> state.copy(lootCommonChests = state.lootCommonChests - 1)
            "rare" -> state.copy(lootRareChests = state.lootRareChests - 1)
            "epic" -> state.copy(lootEpicChests = state.lootEpicChests - 1)
            else -> state
        }
        _gameState.value = if (giveNebula) {
            updated.copy(nebula = updated.nebula + BigNumber(nebulaReward), statistics = updated.statistics.copy(nebulaEarned = updated.statistics.nebulaEarned + nebulaReward), lootChestsOpened = updated.lootChestsOpened + 1)
        } else {
            updated.copy(money = updated.money + moneyReward, statistics = updated.statistics.copy(moneyEarned = updated.statistics.moneyEarned + moneyReward), lootChestsOpened = updated.lootChestsOpened + 1)
        }
        GameLogger.log(LogLevel.INFO, LoggerCategory.GAME, "LOOT_CHEST_OPENED", "Opened $tier loot chest.")
        saveGameAsync()
    }

    fun claimDailyQuest(questId: String) {
        val state = refreshDailyQuestDay(_gameState.value)
        val requirement = when (questId) {
            "daily_guesses_10" -> state.dailyQuestGuesses >= 10
            "daily_correct_5" -> state.dailyQuestCorrectGuesses >= 5
            "daily_streak_5" -> state.dailyQuestBestStreak >= 5
            "daily_upgrades_3" -> state.dailyQuestUpgradePurchases >= 3
            else -> false
        }
        if (!requirement || questId in state.claimedDailyQuests) {
            _gameState.value = state
            return
        }
        val rewardState = when (questId) {
            "daily_guesses_10" -> state.copy(money = state.money + BigNumber(10_000))
            "daily_correct_5" -> state.copy(nebula = state.nebula + BigNumber(25))
            "daily_streak_5" -> state.copy(nebula = state.nebula + BigNumber(50))
            "daily_upgrades_3" -> state.copy(nebula = state.nebula + BigNumber(35))
            else -> state
        }
        _gameState.value = rewardState.copy(claimedDailyQuests = rewardState.claimedDailyQuests + questId)
        saveGameAsync()
        GameLogger.log(LogLevel.INFO, LoggerCategory.GAME, "DAILY_QUEST_CLAIM", "Claimed daily quest $questId")
    }

    private fun refreshDailyQuestDay(state: GameState): GameState {
        val today = LocalDate.now().toString()
        if (state.dailyQuestDate == today) return state
        return state.copy(
            dailyQuestDate = today,
            dailyQuestGuesses = 0,
            dailyQuestCorrectGuesses = 0,
            dailyQuestBestStreak = 0,
            dailyQuestUpgradePurchases = 0,
            claimedDailyQuests = emptySet()
        )
    }

    private fun recordDailyGuess(previous: GameState, next: GameState, correct: Boolean): GameState =
        next.copy(
            dailyQuestDate = previous.dailyQuestDate,
            dailyQuestGuesses = previous.dailyQuestGuesses + 1,
            dailyQuestCorrectGuesses = previous.dailyQuestCorrectGuesses + if (correct) 1 else 0,
            dailyQuestBestStreak = maxOf(previous.dailyQuestBestStreak, next.streak),
            dailyQuestUpgradePurchases = previous.dailyQuestUpgradePurchases,
            claimedDailyQuests = previous.claimedDailyQuests
        )


    fun earnMinigameReward(nebulaReward: Long, moneyReward: BigNumber) {
        val state = _gameState.value
        _gameState.value = state.copy(
            nebula = state.nebula + BigNumber(nebulaReward),
            money = state.money + moneyReward
        )
        saveGameAsync()
        GameLogger.log(LogLevel.INFO, LoggerCategory.ARCADE, "MINIGAME_REWARD", "Earned $nebulaReward Nebula and $moneyReward Money from minigame.")
    }

    fun completeTutorial() {
        _gameState.update { it.copy(tutorialCompleted = true, settings = it.settings.copy(tutorialCompleted = true)) }
        viewModelScope.launch(Dispatchers.IO) { saveManager.saveAppPreferences(_gameState.value.settings) }
        saveGameAsync()
        GameLogger.log(LogLevel.INFO, LoggerCategory.UI, "TUTORIAL_COMPLETE", "Tutorial completed by user.")
    }

    fun dismissOfflineGains() {
        _offlineGains.value = null
    }

    fun dismissChangelogPopup() {
        _showChangelogPopup.value = false
    }

    fun dismissTimeTravelPopup() {
        _showTimeTravelPopup.value = false
    }

    fun claimLiveOpsEventReward(eventId: String) {
        // Handled or logged
        GameLogger.log(LogLevel.INFO, LoggerCategory.UI, "LIVEOPS_CLAIM", "Claimed event reward: $eventId")
    }

    private fun saveGameAsync() {
        viewModelScope.launch(Dispatchers.IO) {
            slotOperationMutex.withLock {
                val slot = _activeSlot.value
                val state = _gameState.value.copy(lastSaveTimestamp = System.currentTimeMillis())
                saveManager.saveGame(state, slot)
                WidgetRefresh.request(getApplication<Application>())
            }
        }
    }

    private fun startAutoSave() {
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(30000)
                slotOperationMutex.withLock {
                    val slot = _activeSlot.value
                    val state = _gameState.value.copy(lastSaveTimestamp = System.currentTimeMillis())
                    saveManager.saveGame(state, slot)
                    WidgetRefresh.request(getApplication<Application>())
                }
            }
        }
    }

    private fun startPlaytimeTimer() {
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000)
                val currentMoney = _gameState.value.money
                _incomePerSecond.value = if (_gameState.value.autoClickerActive) {
                    currentMoney - lastIncomeMeasurement
                } else {
                    BigNumber.ZERO
                }
                lastIncomeMeasurement = currentMoney
                _gameState.update {
                    it.copy(statistics = it.statistics.copy(playtimeSeconds = it.statistics.playtimeSeconds + 1))
                }
            }
        }
    }

    private fun startRandomEvents() {
        randomEventJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(60_000L)
                val event = withContext(Dispatchers.Main) {
                    val currentState = _gameState.value
                    randomEventEngine.roll(currentState)?.also { _gameState.value = it.state }
                } ?: continue
                saveGameAsync()
                GameLogger.log(
                    LogLevel.INFO,
                    LoggerCategory.GAME,
                    "RANDOM_EVENT",
                    "Random event triggered: " + event.type.name
                )
                NotificationHelper.showEventNotification(getApplication<Application>())
                delay(8_000L)
                if (_gameState.value.lastRandomEventId == event.type.name.lowercase()) {
                    _gameState.update { it.copy(lastRandomEventId = null) }
                }
            }
        }
    }

    private fun startAutoClicker() {
        autoClickerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val state = _gameState.value
                val talentSpeed = (if ("talent_speed_1" in state.prestigeShopPurchases) 1.25 else 1.0) * (if ("talent_speed_2" in state.prestigeShopPurchases) 1.5 else 1.0)
                val effectiveSpeed = state.autoClickerSpeed * talentSpeed * if ("mut_speed" in state.activeMutators) 3.0 else 1.0
                val delayMillis = if (state.autoClickerActive && effectiveSpeed > 0.0) {
                    (1000.0 / effectiveSpeed).toLong()
                } else {
                    1000L
                }
                delay(delayMillis)
                withContext(Dispatchers.Main) {
                    val currentState = refreshDailyQuestDay(_gameState.value)
                    if (currentState.autoClickerActive) {
                        val guess = guessingBot.nextGuess(
                            currentState.currentRangeMin,
                            currentState.currentRangeMax
                        )
                        val result = gameEngine.processGuess(currentState, guess)
                        guessingBot.observeGuess(guess, result.feedback)
                        val isCorrect = result.feedback == com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback.CORRECT
                        if (isCorrect) {
                            guessingBot.reset(result.newState.currentRangeMin, result.newState.currentRangeMax)
                        }
                        val talentAdjustedState = applyTalentBonuses(currentState, result.newState, isCorrect)
                        val progressedState = recordLootDrop(
                            recordDailyGuess(currentState, talentAdjustedState, isCorrect),
                            isCorrect
                        )
                        _gameState.value = progressedState
                        val activeBossWorld = progressedState.activeBossBattleWorldId
                        if (activeBossWorld != null) {
                            worldProgressAction(activeBossWorld, if (isCorrect) "boss_hit" else "boss_miss")
                        }
                        saveGameAsync()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoClickerJob?.cancel()
        saveJob?.cancel()
        timerJob?.cancel()
        randomEventJob?.cancel()
        backgroundMusicManager.release()
        runBlocking(Dispatchers.IO) {
            saveManager.saveGame(
                _gameState.value.copy(lastSaveTimestamp = System.currentTimeMillis()),
                _activeSlot.value
            )
        }
    }
}
