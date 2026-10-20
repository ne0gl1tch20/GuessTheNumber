package com.jarrlyyy.guessthenumber.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.jarrlyyy.guessthenumber.ui.screens.PlayerProfile
import com.jarrlyyy.guessthenumber.ui.screens.PlayerProfileScreen
import com.jarrlyyy.guessthenumber.ui.screens.PlayerProfileStore
import com.jarrlyyy.guessthenumber.ui.screens.SharedWebUiPrototypeScreen
import com.jarrlyyy.guessthenumber.ui.screens.LuaTestingHubScreen
import com.jarrlyyy.guessthenumber.ui.viewmodel.GameViewModel
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp

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
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val profileStore = remember(appContext) { PlayerProfileStore(appContext) }
    val playerProfile by profileStore.profile.collectAsState(initial = null)
    var profileLoaded by remember { mutableStateOf(false) }
    var showPlayerProfile by remember { mutableStateOf(false) }
    LaunchedEffect(profileStore) {
        profileStore.profile.first()
        profileLoaded = true
    }
    var showDevSettings by remember { mutableStateOf(false) }
    var dailyRewardTestRequest by remember { mutableStateOf(0) }
    var webPlayMounted by remember { mutableStateOf(hasAnySave) }
    LaunchedEffect(hasAnySave) { if (hasAnySave) webPlayMounted = true }
    var debugDestination by remember { mutableStateOf<String?>(null) }
    var showSaveSlots by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<String?>(null) }

    if (!profileLoaded) {
        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    if (playerProfile?.setupComplete != true) {
        CompositionLocalProvider(LocalAppLocaleManager provides locale) {
            PlayerProfileScreen(
                profile = playerProfile,
                firstLaunch = true,
                store = profileStore,
                onBack = {}
            )
        }
        return
    }

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
            dailyRewardTestRequest = dailyRewardTestRequest,
            incomePerSecond = incomePerSecond,
            localeManager = locale,
            stateProvider = { viewModel.gameState.value },
            offlineGains = offlineGains,
            onDismissOfflineGains = onDismissOfflineGains,
            onClaimDailyReward = viewModel::claimDailyReward,
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
                    onExecuteCommand = { command ->
                        if (command.trim().equals("/testdailyrewards", ignoreCase = true)) {
                            showDevSettings = false
                            debugDestination = null
                            dailyRewardTestRequest++
                            "Opening the daily reward popup in the HTML game."
                        } else {
                            viewModel.executeDevCommand(command)
                        }
                    },
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

    if (webPlayMounted && !showSaveSlots) {
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(12.dp)
                .clickable { showPlayerProfile = true },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                val avatarEmoji = when (playerProfile?.avatarId) {
                    "cat" -> "🐱"; "fox" -> "🦊"; "dog" -> "🐶"; "robot" -> "🤖"
                    "ghost" -> "👻"; "dragon" -> "🐉"; "alien" -> "👽"; "rocket" -> "🚀"
                    "crown" -> "👑"; "gem" -> "💎"; "controller" -> "🎮"; "moon" -> "🌙"
                    "sun" -> "☀️"; "flower" -> "🌸"; "lightning" -> "⚡"; else -> "⭐"
                }
                val customAvatar = remember(playerProfile?.customAvatarPath) {
                    playerProfile?.customAvatarPath?.let { path ->
                        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
                    }
                }
                if (playerProfile?.avatarId == "custom" && customAvatar != null) {
                    Image(
                        bitmap = customAvatar,
                        contentDescription = "Profile picture",
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                    )
                } else {
                    androidx.compose.material3.Text(avatarEmoji)
                }
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                androidx.compose.material3.Text(
                    playerProfile?.username.orEmpty(),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }

    if (showPlayerProfile) {
        CompositionLocalProvider(LocalAppLocaleManager provides locale) {
            PlayerProfileScreen(
                profile = playerProfile,
                firstLaunch = false,
                store = profileStore,
                onBack = { showPlayerProfile = false }
            )
        }
    }

    }

    if (initialDestination != null) {
        onNotificationDestinationHandled()
    }
}
