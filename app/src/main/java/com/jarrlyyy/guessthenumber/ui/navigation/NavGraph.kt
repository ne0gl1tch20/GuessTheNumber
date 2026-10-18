package com.jarrlyyy.guessthenumber.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jarrlyyy.guessthenumber.BuildConfig
import androidx.compose.runtime.CompositionLocalProvider
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import com.jarrlyyy.guessthenumber.ui.screens.CrashRecoveryScreen
import com.jarrlyyy.guessthenumber.ui.screens.DevSettingsScreen
import com.jarrlyyy.guessthenumber.ui.screens.ProductionWebPlayScreen
import com.jarrlyyy.guessthenumber.ui.screens.SaveSlotsScreen
import com.jarrlyyy.guessthenumber.ui.viewmodel.GameViewModel

/**
 * Player-facing navigation is owned by the shared HTML surface.
 * Kotlin remains the authoritative engine, save system and action host.
 * Native Compose is retained only for boot/recovery/system-only surfaces.
 */
@Composable
fun NavGraph(
    state: GameState,
    onMakeGuess: (Long) -> Boolean,
    incomePerSecond: BigNumber,
    onBuyUpgrade: (String, BigNumber) -> Unit,
    onBuyShopItem: (String, Long) -> Unit,
    onEquipCosmetic: (String) -> Unit,
    onBuyPrestigeUpgrade: (String, BigNumber) -> Unit,
    onBuyPrestigeShopItem: (String, Long) -> Unit,
    onBuyUltraUpgrade: (String, BigNumber) -> Unit,
    onBuyUltraShopItem: (String, Long) -> Unit,
    onPrestige: () -> Unit,
    onUltra: () -> Unit,
    onExecuteDevCommand: (String) -> String,
    onExportSave: ((String) -> Unit) -> Unit,
    onImportSave: (String, (Boolean) -> Unit) -> Unit,
    onResetData: () -> Unit,
    onUpdateSettings: (GameSettings) -> Unit,
    onUpdateBackgroundMusic: (String?) -> Unit,
    isPlayingMusic: Boolean,
    musicCurrentPosition: Int,
    musicDuration: Int,
    musicAlbumArt: android.graphics.Bitmap?,
    onPlayMusic: () -> Unit,
    onPauseMusic: () -> Unit,
    onStopMusic: () -> Unit,
    onSeekMusic: (Int) -> Unit,
    onCompleteTutorial: () -> Unit,
    hasPreviousCrash: Boolean,
    onDismissCrash: () -> Unit,
    offlineGains: BigNumber?,
    onDismissOfflineGains: () -> Unit,
    showChangelogPopup: Boolean,
    onDismissChangelog: () -> Unit,
    showTimeTravelPopup: Boolean,
    timeTravelSeconds: Long,
    onDismissTimeTravel: () -> Unit,
    needsSettingsMigration: Boolean,
    isMigratingSettings: Boolean,
    onMigrateSettings: (Boolean) -> Unit,
    onEarnMinigameReward: (Long, BigNumber) -> Unit,
    onUpdateMultiplier: (String) -> Unit,
    onClaimLiveOpsEventReward: (String) -> Unit,
    onActivateChallengeBuilder: (Set<String>) -> Unit,
    viewModel: GameViewModel,
    hasAnySave: Boolean,
    initialDestination: String? = null,
    onNotificationDestinationHandled: () -> Unit = {}
) {
    val locale = viewModel.localeManager
    var showDevSettings by remember { mutableStateOf(false) }

    if (!hasAnySave) {
        CompositionLocalProvider(LocalAppLocaleManager provides locale) {
            SaveSlotsScreen(viewModel = viewModel, onNavigateBack = {})
        }
        return
    }

    if (hasPreviousCrash) {
        CrashRecoveryScreen(
            locale = locale,
            onDismiss = onDismissCrash,
            reducedMotion = state.settings.reducedMotion
        )
        return
    }

    if (BuildConfig.DEBUG && showDevSettings) {
        CompositionLocalProvider(LocalAppLocaleManager provides locale) {
            DevSettingsScreen(
                state = state,
                onExecuteCommand = viewModel::executeDevCommand,
                onImportSave = viewModel::importSave,
                onUpdateSettings = viewModel::updateSettings,
                onGetAppPreferencesJson = viewModel::getAppPreferencesJson,
                onApplyAppPreferencesJson = viewModel::applyAppPreferencesJson,
                onGetSaveJson = viewModel::getSaveJson,
                onApplySaveJson = viewModel::applySaveJson,
                onNavigate = { destination ->
                    if (BuildConfig.DEBUG && destination == "dev_settings") showDevSettings = true
                },
                onBack = { showDevSettings = false }
            )
        }
        return
    }

    ProductionWebPlayScreen(
        state = state,
        incomePerSecond = incomePerSecond,
        localeManager = locale,
        stateProvider = { viewModel.gameState.value },
        onSubmitGuess = { guess -> viewModel.makeGuessWithResult(guess) },
        onClaimDailyQuest = viewModel::claimDailyQuest,
        onOpenLootChest = viewModel::openLootChest,
        onNavigateProgression = { progression ->
            when {
                progression == "dev_settings" && BuildConfig.DEBUG -> showDevSettings = true
                progression == "home_upgrade" -> viewModel.progressionAction("home_upgrade", "")
                progression == "claim_codex" -> viewModel.progressionAction("claim_codex", "")
                progression.startsWith("equip_relic:") -> viewModel.progressionAction("equip_relic", progression.substringAfter(':'))
            }
        },
        onWorldAction = viewModel::worldProgressAction,
        onBuyUpgrade = { id, cost -> viewModel.buyUpgrade(id, cost) },
        onBuyShopItem = { id, cost -> viewModel.buyShopItem(id, cost) },
        onEquipCosmetic = viewModel::equipCosmetic,
        onUpdateMultiplier = viewModel::updateBuyMultiplier,
        onUpdateSettings = viewModel::updateSettings,
        onPrestigeReset = viewModel::prestigeReset,
        onUltraReset = viewModel::ultraReset,
        onBuyTalent = viewModel::buyTalent,
        onSetMutatorActive = viewModel::setMutatorActive,
        onArcadeReward = viewModel::awardArcadeResult,
        onSettleStake = viewModel::settleRandomStake
    )

    if (initialDestination != null) {
        onNotificationDestinationHandled()
    }
}
