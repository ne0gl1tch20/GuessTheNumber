package com.jarrlyyy.guessthenumber

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarrlyyy.guessthenumber.ui.components.ExpressiveSaveLoadingScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jarrlyyy.guessthenumber.ui.navigation.NavGraph
import com.jarrlyyy.guessthenumber.ui.screens.CrashRecoveryScreen
import com.jarrlyyy.guessthenumber.ui.theme.GuessTheNumberTheme
import com.jarrlyyy.guessthenumber.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()
    private var notificationDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationDestination = savedInstanceState?.getString(STATE_NOTIFICATION_DESTINATION)
            ?: intent.getStringExtra(EXTRA_NOTIFICATION_DESTINATION)
        setContent {
            val state by viewModel.gameState.collectAsState()
            val isLoadingSave by viewModel.isLoadingSave.collectAsState()
            val hasPreviousCrash by viewModel.hasPreviousCrash.collectAsState()
            val offlineGains by viewModel.offlineGains.collectAsState()
            val incomePerSecond by viewModel.incomePerSecond.collectAsState()
            val showChangelogPopup by viewModel.showChangelogPopup.collectAsState()
            val showTimeTravelPopup by viewModel.showTimeTravelPopup.collectAsState()
            val timeTravelSeconds by viewModel.timeTravelSeconds.collectAsState()
            val isPlayingMusic by viewModel.isPlayingMusic.collectAsState()
            val musicCurrentPosition by viewModel.musicCurrentPosition.collectAsState()
            val musicDuration by viewModel.musicDuration.collectAsState()
            val musicAlbumArt by viewModel.musicAlbumArt.collectAsState()
            val hasAnySave by viewModel.hasAnySave.collectAsState()
            val needsSettingsMigration by viewModel.needsSettingsMigration.collectAsState()
            val isMigratingSettings by viewModel.isMigratingSettings.collectAsState()

            GuessTheNumberTheme(
                themeMode = state.settings.themeMode,
                primaryHex = state.settings.customPrimaryColor,
                secondaryHex = state.settings.customSecondaryColor,
                tertiaryHex = state.settings.customTertiaryColor
            ) {
                val locale = viewModel.localeManager
                when {
                    hasPreviousCrash -> {
                        // Crash recovery must be the first visible boot layer.
                        // Keep it independent from NavGraph and CompositionLocals.
                        CrashRecoveryScreen(
                            locale = locale,
                            onDismiss = { viewModel.dismissCrash() }
                        )
                    }

                    isLoadingSave -> {
                        ExpressiveSaveLoadingScreen(
                            title = locale.getString("loading_save_data", "Loading save data..."),
                            reducedMotion = state.settings.reducedMotion
                        )
                    }

                    else -> {
                        NavGraph(
                            state = state,
                            onMakeGuess = { viewModel.makeGuess(it) },
                            incomePerSecond = incomePerSecond,
                            onBuyUpgrade = { id, cost -> viewModel.buyUpgrade(id, cost) },
                            onBuyShopItem = { id, cost -> viewModel.buyShopItem(id, cost) },
                            onEquipCosmetic = { id -> viewModel.equipCosmetic(id) },
                            onBuyPrestigeUpgrade = { id, cost -> viewModel.buyPrestigeUpgrade(id, cost) },
                            onBuyPrestigeShopItem = { id, cost -> viewModel.buyPrestigeShopItem(id, cost) },
                            onBuyUltraUpgrade = { id, cost -> viewModel.buyUltraUpgrade(id, cost) },
                            onBuyUltraShopItem = { id, cost -> viewModel.buyUltraShopItem(id, cost) },
                            onPrestige = { viewModel.prestigeReset() },
                            onUltra = { viewModel.ultraReset() },
                            onExecuteDevCommand = { viewModel.executeDevCommand(it) },
                            onExportSave = { viewModel.exportSave(it) },
                            onImportSave = { json, cb -> viewModel.importSave(json, cb) },
                            onResetData = { viewModel.resetData() },
                            onUpdateSettings = { viewModel.updateSettings(it) },
                            onUpdateBackgroundMusic = { viewModel.updateBackgroundMusicPath(it) },
                            isPlayingMusic = isPlayingMusic,
                            musicCurrentPosition = musicCurrentPosition,
                            musicDuration = musicDuration,
                            musicAlbumArt = musicAlbumArt,
                            onPlayMusic = { viewModel.playBackgroundMusic() },
                            onPauseMusic = { viewModel.pauseBackgroundMusic() },
                            onStopMusic = { viewModel.stopBackgroundMusic() },
                            onSeekMusic = { viewModel.seekBackgroundMusic(it) },
                            onCompleteTutorial = { viewModel.completeTutorial() },
                            hasPreviousCrash = hasPreviousCrash,
                            onDismissCrash = { viewModel.dismissCrash() },
                            offlineGains = offlineGains,
                            onDismissOfflineGains = { viewModel.dismissOfflineGains() },
                            showChangelogPopup = showChangelogPopup,
                            onDismissChangelog = { viewModel.dismissChangelogPopup() },
                            showTimeTravelPopup = showTimeTravelPopup,
                            timeTravelSeconds = timeTravelSeconds,
                            onDismissTimeTravel = { viewModel.dismissTimeTravelPopup() },
                            needsSettingsMigration = needsSettingsMigration,
                            isMigratingSettings = isMigratingSettings,
                            onMigrateSettings = { viewModel.migrateSettingsToGlobal() },
                            hasAnySave = hasAnySave,
                            onEarnMinigameReward = { nebula, money -> viewModel.earnMinigameReward(nebula, money) },
                            onUpdateMultiplier = { viewModel.updateBuyMultiplier(it) },
                            onClaimLiveOpsEventReward = { viewModel.claimLiveOpsEventReward(it) },
                            onActivateChallengeBuilder = { ids -> viewModel.activateChallengeBuilder(ids) },
                            viewModel = viewModel,
                            initialDestination = notificationDestination,
                            onNotificationDestinationHandled = { notificationDestination = null }
                        )
                    }
                }
            }
        }
    }
}