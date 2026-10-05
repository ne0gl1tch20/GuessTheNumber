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
import java.io.File

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val saveManager = SaveManager(application)
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

    private var autoClickerJob: Job? = null
    private var saveJob: Job? = null
    private var timerJob: Job? = null
    private var randomEventJob: Job? = null

    init {
        AppErrorHandler.init(application.filesDir)
        _hasPreviousCrash.value = AppErrorHandler.hasPreviousCrash()
        GameLogger.configureStorage(File(application.filesDir, "game_logs.txt"), _gameState.value.settings.saveLogsToStorage)
        loadGame()
        startAutoSave()
        startPlaytimeTimer()
        startRandomEvents()
        startAutoClicker()
    }

    fun loadGame(slot: Int = -1) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetSlot = if (slot in 1..MAX_SAVE_SLOTS) slot else saveManager.getActiveSlot()
            saveManager.setActiveSlot(targetSlot)
            _activeSlot.value = targetSlot

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
            var finalState = timeCheckedState

            if (!timeTravelDetected && deltaSeconds > 60 && timeCheckedState.autoClickerActive) {
                val maxOfflineSeconds = 28800L
                val effectiveSeconds = minOf(deltaSeconds, maxOfflineSeconds)
                val earningsPerSec = BigNumber(500)
                val totalOfflineEarnings = earningsPerSec * BigNumber(effectiveSeconds.toDouble())
                
                finalState = timeCheckedState.copy(
                    money = timeCheckedState.money + totalOfflineEarnings
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

    fun createSlot(slot: Int, difficultyId: String) {
        if (slot !in 1..MAX_SAVE_SLOTS || !Difficulty.isValid(difficultyId)) return
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = saveManager.getSlotMetadata(slot)
            if (!metadata.isEmpty) return@launch
            saveManager.saveGame(GameState(difficultyId = difficultyId), slot)
            loadGame(slot)
        }
    }

    fun updateSlotProfile(slot: Int, name: String, iconId: String) {
        if (slot !in 1..MAX_SAVE_SLOTS || !SaveProfile.isValidIcon(iconId)) return
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = saveManager.getSlotMetadata(slot)
            if (metadata.isEmpty) return@launch
            val state = saveManager.loadGame(slot)
            val updated = state.copy(
                profileName = name.trim().take(24),
                profileIconId = iconId
            )
            saveManager.saveGame(updated, slot)
            loadGame(_activeSlot.value)
        }
    }

    fun switchSlot(slot: Int) {
        if (slot in 1..MAX_SAVE_SLOTS && slot != _activeSlot.value) {
            viewModelScope.launch(Dispatchers.IO) {
                saveManager.saveGame(_gameState.value.copy(lastSaveTimestamp = System.currentTimeMillis()), _activeSlot.value)
                loadGame(slot)
            }
        }
    }

    fun migrateLegacySave() {
        viewModelScope.launch(Dispatchers.IO) {
            val success = saveManager.migrateLegacySaveToSlot1()
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
            val success = saveManager.duplicateSlot(sourceSlot, targetSlot)
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun restoreBackup(slot: Int, onResult: (Boolean) -> Unit) {
        if (slot !in 1..MAX_SAVE_SLOTS) {
            onResult(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val success = saveManager.restoreBackup(slot)
            if (success && _activeSlot.value == slot) {
                val loaded = saveManager.loadGame(slot)
                _gameState.value = AntiCheatService.sanitizeCurrency(loaded)
            }
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun resetSlot(slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            saveManager.resetSlot(slot)
            if (_activeSlot.value == slot) {
                loadGame(slot)
            }
        }
    }

    fun makeGuess(guess: Long) {
        if (!AntiCheatService.validateGuessRate()) return
        val currentState = _gameState.value
        val result = gameEngine.processGuess(currentState, guess)
        guessingBot.observeGuess(guess.coerceIn(currentState.currentRangeMin, currentState.currentRangeMax), result.feedback)
        if (result.feedback == com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback.CORRECT) {
            guessingBot.reset(result.newState.currentRangeMin, result.newState.currentRangeMax)
        }
        _gameState.value = result.newState
        checkAchievements(result.newState)
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
        val state = _gameState.value
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
            _gameState.value = state.copy(money = newMoney, upgradeLevels = newLevels, statistics = newStats)
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
            _gameState.value = state.copy(nebula = newNebula, shopPurchases = newPurchases, autoClickerActive = autoActive)
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.SHOP, "BUY_SHOP", "Purchased shop item $itemId")
        }
    }

    fun buyPrestigeUpgrade(upgradeId: String, cost: BigNumber) {
        val state = _gameState.value
        if (state.prestige >= cost) {
            val currentLevel = state.prestigeUpgradeLevels[upgradeId] ?: 0
            val newLevels = state.prestigeUpgradeLevels.toMutableMap()
            newLevels[upgradeId] = currentLevel + 1
            val newPrestige = state.prestige - cost
            _gameState.value = state.copy(prestige = newPrestige, prestigeUpgradeLevels = newLevels)
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.UPGRADE, "BUY_PRESTIGE_UPGRADE", "Bought prestige upgrade $upgradeId to level ${currentLevel + 1}")
        }
    }

    fun buyUltraUpgrade(upgradeId: String, cost: BigNumber) {
        val state = _gameState.value
        if (state.ultra >= cost) {
            val currentLevel = state.ultraUpgradeLevels[upgradeId] ?: 0
            val newLevels = state.ultraUpgradeLevels.toMutableMap()
            newLevels[upgradeId] = currentLevel + 1
            val newUltra = state.ultra - cost
            _gameState.value = state.copy(ultra = newUltra, ultraUpgradeLevels = newLevels)
            saveGameAsync()
            GameLogger.log(LogLevel.INFO, LoggerCategory.UPGRADE, "BUY_ULTRA_UPGRADE", "Bought ultra upgrade $upgradeId to level ${currentLevel + 1}")
        }
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
            val success = saveManager.importSave(json, _activeSlot.value)
            if (success) {
                val loaded = saveManager.loadGame(_activeSlot.value)
                val sanitized = AntiCheatService.sanitizeCurrency(loaded)
                withContext(Dispatchers.Main) {
                    _gameState.value = sanitized
                    onResult(true)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun resetData() {
        viewModelScope.launch(Dispatchers.IO) {
            saveManager.resetData()
            loadGame(1)
            withContext(Dispatchers.Main) {
                backgroundMusicManager.stop()
            }
        }
    }

    fun updateSettings(newSettings: GameSettings) {
        val oldEnabled = _gameState.value.settings.notificationsEnabled
        if (newSettings.locale != _gameState.value.settings.locale) {
            updateLocaleRepo(newSettings.locale)
        }
        _gameState.update { it.copy(settings = newSettings) }
        GameLogger.configureStorage(File(getApplication<Application>().filesDir, "game_logs.txt"), newSettings.saveLogsToStorage)
        saveGameAsync()

        val context = getApplication<Application>()
        if (newSettings.notificationsEnabled && !oldEnabled) {
            val workRequest = PeriodicWorkRequestBuilder<GameReminderWorker>(
                24, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                GameReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } else if (!newSettings.notificationsEnabled && oldEnabled) {
            WorkManager.getInstance(context).cancelUniqueWork(GameReminderWorker.WORK_NAME)
        }

        GameLogger.log(LogLevel.INFO, LoggerCategory.UI, "UPDATE_SETTINGS", "Updated game settings.")
    }

    fun updateBackgroundMusicPath(path: String?) {
        _gameState.update { it.copy(settings = it.settings.copy(backgroundMusicPath = path)) }
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
    }

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
        _gameState.update { it.copy(tutorialCompleted = true) }
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
            saveManager.saveGame(_gameState.value.copy(lastSaveTimestamp = System.currentTimeMillis()), _activeSlot.value)
        }
    }

    private fun startAutoSave() {
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(30000)
                saveManager.saveGame(_gameState.value.copy(lastSaveTimestamp = System.currentTimeMillis()), _activeSlot.value)
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
                val currentState = _gameState.value
                val event = randomEventEngine.roll(currentState) ?: continue
                withContext(Dispatchers.Main) {
                    _gameState.value = event.state
                }
                saveGameAsync()
                GameLogger.log(
                    LogLevel.INFO,
                    LoggerCategory.GAME,
                    "RANDOM_EVENT",
                    "Random event triggered: " + event.type.name
                )
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
                val delayMillis = if (state.autoClickerActive && state.autoClickerSpeed > 0.0) {
                    (1000.0 / state.autoClickerSpeed).toLong()
                } else {
                    1000L
                }
                delay(delayMillis)
                val currentState = _gameState.value
                if (currentState.autoClickerActive) {
                    val guess = guessingBot.nextGuess(
                        currentState.currentRangeMin,
                        currentState.currentRangeMax
                    )
                    val result = gameEngine.processGuess(currentState, guess)
                    guessingBot.observeGuess(guess, result.feedback)
                    if (result.feedback == com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback.CORRECT) {
                        guessingBot.reset(result.newState.currentRangeMin, result.newState.currentRangeMax)
                    }
                    withContext(Dispatchers.Main) {
                        _gameState.value = result.newState
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
        viewModelScope.launch(Dispatchers.IO) {
            saveManager.saveGame(_gameState.value.copy(lastSaveTimestamp = System.currentTimeMillis()), _activeSlot.value)
        }
    }
}
