package com.jarrlyyy.guessthenumber

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.ui.navigation.NavGraph
import com.jarrlyyy.guessthenumber.ui.theme.GuessTheNumberTheme
import com.jarrlyyy.guessthenumber.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                if (isLoadingSave) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = locale.getString("loading_save_data", "Loading save data..."),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                } else {
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
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
