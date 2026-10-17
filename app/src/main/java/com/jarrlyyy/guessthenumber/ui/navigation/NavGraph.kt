package com.jarrlyyy.guessthenumber.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import com.jarrlyyy.guessthenumber.ui.screens.CrashRecoveryScreen
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

    ProductionWebPlayScreen(
        state = state,
        incomePerSecond = incomePerSecond,
        stateProvider = { viewModel.gameState.value },
        onSubmitGuess = { guess -> viewModel.makeGuessWithResult(guess) },
        onClaimDailyQuest = viewModel::claimDailyQuest,
        onOpenLootChest = viewModel::openLootChest,
        onNavigateProgression = {},
        onWorldAction = viewModel::worldProgressAction
    )

    if (initialDestination != null) {
        onNotificationDestinationHandled()
    }
}
