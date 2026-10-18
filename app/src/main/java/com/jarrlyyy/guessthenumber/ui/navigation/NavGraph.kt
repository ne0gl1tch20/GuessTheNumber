package com.jarrlyyy.guessthenumber.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.jarrlyyy.guessthenumber.ui.components.ReusableColorPickerDialog
import com.jarrlyyy.guessthenumber.ui.screens.CrashRecoveryScreen
import com.jarrlyyy.guessthenumber.ui.screens.DevSettingsScreen
import com.jarrlyyy.guessthenumber.ui.screens.ProcessInspectorScreen
import com.jarrlyyy.guessthenumber.ui.screens.ProductionWebPlayScreen
import com.jarrlyyy.guessthenumber.ui.screens.SaveSlotsScreen
import com.jarrlyyy.guessthenumber.ui.screens.SharedWebUiPrototypeScreen
import com.jarrlyyy.guessthenumber.ui.screens.LuaTestingHubScreen
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
    var webPlayMounted by remember { mutableStateOf(hasAnySave) }
    LaunchedEffect(hasAnySave) { if (hasAnySave) webPlayMounted = true }
    var debugDestination by remember { mutableStateOf<String?>(null) }
    var showSaveSlots by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<String?>(null) }

    if (!webPlayMounted && !showSaveSlots) {
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

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
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
                    progression == "save_slots" -> showSaveSlots = true
                    progression == "color_picker_primary" -> colorPickerTarget = "Primary"
                    progression == "color_picker_secondary" -> colorPickerTarget = "Secondary"
                    progression == "color_picker_tertiary" -> colorPickerTarget = "Tertiary"
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
                    onNavigate = { destination -> debugDestination = destination },
                    onBack = { showDevSettings = false; debugDestination = null }
                )
            }
        }

        if (BuildConfig.DEBUG) {
            when (debugDestination) {
                "process_inspector" -> CompositionLocalProvider(LocalAppLocaleManager provides locale) {
                    ProcessInspectorScreen(state = state, onBack = { debugDestination = null })
                }
                "web_ui_prototype" -> CompositionLocalProvider(LocalAppLocaleManager provides locale) {
                    SharedWebUiPrototypeScreen(
                        state = state,
                        onSubmitGuess = { guess -> viewModel.makeGuessWithResult(guess) },
                        onBack = { debugDestination = null }
                    )
                }
                "lua_testing_hub" -> CompositionLocalProvider(LocalAppLocaleManager provides locale) {
                    LuaTestingHubScreen(onBack = { debugDestination = null })
                }
            }
        }

        if (showSaveSlots) {
            CompositionLocalProvider(LocalAppLocaleManager provides locale) {
                SaveSlotsScreen(viewModel = viewModel, onNavigateBack = { showSaveSlots = false })
            }
        }

        colorPickerTarget?.let { target ->
            val current = when (target) {
                "Primary" -> state.settings.customPrimaryColor
                "Secondary" -> state.settings.customSecondaryColor
                else -> state.settings.customTertiaryColor
            }
            CompositionLocalProvider(LocalAppLocaleManager provides locale) {
                ReusableColorPickerDialog(
                    initialHex = current,
                    title = locale.getString("settings_custom_theme_color", "Custom Theme Color"),
                    onDismiss = { colorPickerTarget = null },
                    onApply = { hex ->
                        val updated = when (target) {
                            "Primary" -> state.settings.copy(themePreset = "Custom", customPrimaryColor = hex)
                            "Secondary" -> state.settings.copy(themePreset = "Custom", customSecondaryColor = hex)
                            else -> state.settings.copy(themePreset = "Custom", customTertiaryColor = hex)
                        }
                        viewModel.updateSettings(updated)
                        colorPickerTarget = null
                    }
                )
            }
        }
    }

    if (initialDestination != null) {
        onNotificationDestinationHandled()
    }
}
