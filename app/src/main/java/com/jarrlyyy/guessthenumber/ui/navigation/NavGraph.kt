package com.jarrlyyy.guessthenumber.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.screens.*
import com.jarrlyyy.guessthenumber.ui.components.ExpressiveScreen
import com.jarrlyyy.guessthenumber.ui.components.expressiveSelection
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import androidx.compose.runtime.CompositionLocalProvider
import com.jarrlyyy.guessthenumber.ui.viewmodel.GameViewModel


@Composable
fun NavGraph(
    state: GameState,
    onMakeGuess: (Long) -> Unit,
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
    hasAnySave: Boolean
) {
    val navController = rememberNavController()
    val locale = viewModel.localeManager
    if (!hasAnySave) {
        CompositionLocalProvider(LocalAppLocaleManager provides locale) {
            SaveSlotsScreen(viewModel = viewModel, onNavigateBack = {})
        }
        return
    }
    val items = listOf(Screen.Play, Screen.Upgrade, Screen.Shop, Screen.More)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var tutorialStep by rememberSaveable { mutableIntStateOf(0) }
    val tutorialActive = !state.tutorialCompleted
    LaunchedEffect(currentRoute, tutorialActive, tutorialStep) {
        if (tutorialActive) {
            when {
                tutorialStep == 1 && currentRoute == Screen.Upgrade.route -> tutorialStep = 2
                tutorialStep == 2 && currentRoute == Screen.More.route -> tutorialStep = 3
                tutorialStep == 3 && currentRoute == Screen.Settings.route -> onCompleteTutorial()
            }
        }
    }

    if (needsSettingsMigration) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(locale.getString("settings_migration_title", "Settings Update")) },
            text = {
                if (isMigratingSettings) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(locale.getString("settings_migration_loading", "Migrating your app settings..."))
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                } else {
                    Text(locale.getString("settings_migration_text", "Your existing app settings will be moved from the save data into global App Preferences. Your game progress will stay unchanged."))
                }
            },
            confirmButton = {
                if (!isMigratingSettings) {
                    Button(onClick = { onMigrateSettings(true) }) {
                        Text(locale.getString("ok", "OK"))
                    }
                }
            }
        )
    }

    if (hasPreviousCrash) {
        CrashRecoveryScreen(locale = locale, onDismiss = onDismissCrash, reducedMotion = state.settings.reducedMotion)
        return
    }

    if (showTimeTravelPopup) {
        AlertDialog(
            onDismissRequest = onDismissTimeTravel,
            title = { Text(locale.getString("time_travel_title", "⏳ Time Travel Detected!")) },
            text = { Text(locale.getString("time_travel_text", "It seems like the time has changed since the last time you played (device clock is %d seconds behind last save). As a consequence, auto-clicker and passive generation have been temporarily paused for 24 hours.", timeTravelSeconds)) },
            confirmButton = {
                Button(onClick = onDismissTimeTravel) {
                    Text(locale.getString("understood", "Understood"))
                }
            }
        )
    } else if (showChangelogPopup) {
        AlertDialog(
            onDismissRequest = onDismissChangelog,
            title = { Text(locale.getString("changelog_popup_title", "🚀 What's New in v%s!", BuildConfig.VERSION_NAME)) },
            text = { Text(locale.getString("changelog_popup_text", "Guess The Number v%s is installed! Check out the changelog for the latest features, polish, and fixes.", BuildConfig.VERSION_NAME)) },
            confirmButton = {
                Button(onClick = {
                    onDismissChangelog()
                    navController.navigate(Screen.ChangelogViewer.route)
                }) {
                    Text(locale.getString("view_changelog"))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissChangelog) {
                    Text(locale.getString("dismiss"))
                }
            }
        )
    } else if (offlineGains != null && offlineGains > BigNumber.ZERO) {
        AlertDialog(
            onDismissRequest = onDismissOfflineGains,
            title = { Text(locale.getString("welcome_back_title", "🌙 Welcome Back!")) },
            text = { Text(locale.getString("offline_gains_text", "While you were away, your auto-clicker generated %s money!", offlineGains.format())) },
            confirmButton = {
                Button(onClick = onDismissOfflineGains) {
                    Text(locale.getString("collect", "Collect"))
                }
            }
        )
    }

    CompositionLocalProvider(LocalAppLocaleManager provides locale) {
        Scaffold(
            bottomBar = {
                if (items.any { it.route == currentRoute }) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp
                    ) {
                        items.forEach { screen ->
                            val labelText = when (screen.route) {
                                Screen.Play.route -> locale.getString("nav_play", "Play")
                                Screen.Upgrade.route -> locale.getString("nav_upgrade", "Upgrades")
                                Screen.Shop.route -> locale.getString("nav_shop", "Shop")
                                Screen.More.route -> locale.getString("nav_more", "More")
                                else -> screen.title
                            }
                            NavigationBarItem(
                                icon = { Icon(modifier = Modifier.expressiveSelection(currentRoute == screen.route), imageVector = screen.icon, contentDescription = labelText) },
                                label = { Text(labelText) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Play.route,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        if (state.settings.reducedMotion) EnterTransition.None
                        else fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(
                                initialOffsetX = { it / 12 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) +
                            scaleIn(
                                initialScale = 0.97f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                    },
                    exitTransition = {
                        if (state.settings.reducedMotion) ExitTransition.None
                        else fadeOut(animationSpec = tween(140)) +
                            slideOutHorizontally(targetOffsetX = { -it / 16 }, animationSpec = tween(160)) +
                            scaleOut(targetScale = 0.985f, animationSpec = tween(160))
                    },
                    popEnterTransition = {
                        if (state.settings.reducedMotion) EnterTransition.None
                        else fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(
                                initialOffsetX = { -it / 12 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) +
                            scaleIn(
                                initialScale = 0.97f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                    },
                    popExitTransition = {
                        if (state.settings.reducedMotion) ExitTransition.None
                        else fadeOut(animationSpec = tween(140)) +
                            slideOutHorizontally(targetOffsetX = { it / 16 }, animationSpec = tween(160)) +
                            scaleOut(targetScale = 0.985f, animationSpec = tween(160))
                    }
                ) {
                    composable(Screen.Play.route) {
                        ExpressiveScreen(enabled = !state.settings.reducedMotion) { PlayScreen(state = state, onMakeGuess = { guess ->
                            onMakeGuess(guess)
                            if (tutorialActive && tutorialStep == 0) tutorialStep = 1
                        }, onClaimDailyQuest = viewModel::claimDailyQuest, onOpenLootChest = viewModel::openLootChest, onWorldAction = viewModel::worldProgressAction, onNavigateProgression = { route -> navController.navigate(route) }, incomePerSecond = incomePerSecond) }
                    }
                    composable(Screen.Upgrade.route) {
                        ExpressiveScreen(enabled = !state.settings.reducedMotion) { UpgradeScreen(
                            state = state,
                            onBuyUpgrade = onBuyUpgrade,
                            onUpdateMultiplier = onUpdateMultiplier
                        ) }
                    }
                    composable(Screen.Shop.route) {
                        ExpressiveScreen(enabled = !state.settings.reducedMotion) { ShopScreen(state = state, onBuyShopItem = onBuyShopItem, onEquipCosmetic = onEquipCosmetic) }
                    }
                    composable(Screen.More.route) {
                        ExpressiveScreen(enabled = !state.settings.reducedMotion) { MoreScreen(
                            state = state,
                            onNavigate = { route ->
                                // Keep the legacy More-menu route working for existing saves.
                                navController.navigate(
                                    if (route == "changelog") Screen.ChangelogViewer.route else route
                                )
                            },
                            onUpdateBackgroundMusic = onUpdateBackgroundMusic,
                            isPlayingMusic = isPlayingMusic,
                            musicCurrentPosition = musicCurrentPosition,
                            musicDuration = musicDuration,
                            musicAlbumArt = musicAlbumArt,
                            onPlayMusic = onPlayMusic,
                            onPauseMusic = onPauseMusic,
                            onStopMusic = onStopMusic,
                            onSeekMusic = onSeekMusic,
                            onUpdateSettings = onUpdateSettings
                        ) }
                    }
                    composable(Screen.MusicPlayer.route) {
                        MusicPlayerScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Prestige.route) {
                        PrestigeScreen(
                            state = state,
                            onPrestige = onPrestige,
                            onBuyPrestigeUpgrade = onBuyPrestigeUpgrade,
                            onUpdateMultiplier = onUpdateMultiplier,
                            onBuyPrestigeShopItem = onBuyPrestigeShopItem,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Ultra.route) {
                        UltraScreen(
                            state = state,
                            onUltra = onUltra,
                            onBuyUltraUpgrade = onBuyUltraUpgrade,
                            onUpdateMultiplier = onUpdateMultiplier,
                            onBuyUltraUpgrade = onBuyUltraUpgrade,
                            onUpdateMultiplier = onUpdateMultiplier,
                            onBuyUltraShopItem = onBuyUltraShopItem,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Arcade.route) {
                        ArcadeScreen(
                            onEarnReward = onEarnMinigameReward,
                            onBack = { navController.popBackStack() },
                            reducedMotion = state.settings.reducedMotion
                        )
                    }
                    composable(Screen.Stats.route) {
                        StatsScreen(state = state, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(state = state, onResetData = onResetData, onUpdateSettings = onUpdateSettings, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.About.route) {
                        AboutScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                    if (BuildConfig.DEBUG) {
                        composable(Screen.DevSettings.route) {
                            DevSettingsScreen(
                                state = state,
                                onExecuteCommand = onExecuteDevCommand,
                                onImportSave = onImportSave,
                                onUpdateSettings = onUpdateSettings,
                                onGetAppPreferencesJson = viewModel::getAppPreferencesJson,
                                onApplyAppPreferencesJson = viewModel::applyAppPreferencesJson,
                                onGetSaveJson = viewModel::getSaveJson,
                                onApplySaveJson = viewModel::applySaveJson,
                                onNavigate = { route -> navController.navigate(route) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("process_inspector") {
                            ProcessInspectorScreen(
                                state = state,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable(Screen.SaveSlots.route) {
                        SaveSlotsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("achievements") {
                        AchievementsScreen(
                            state = state,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("live_ops") {
                        LiveOpsScreen(
                            state = state,
                            onClaimReward = onClaimLiveOpsEventReward,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("talent") {
                        TalentScreen(
                            state = state,
                            onBuyTalent = viewModel::buyTalent,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("mutators") {
                        MutatorsScreen(
                            state = state,
                            onSelectMutator = { id, active -> viewModel.setMutatorActive(id, active) },
                            onActivateChallengeBuilder = onActivateChallengeBuilder,
                            onCompleteChallenge = { id, reward -> viewModel.claimChallenge(id, reward) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.WorldMap.route) {
                        WorldMapScreen(state = state, onWorldAction = viewModel::worldProgressAction, onProgressionAction = viewModel::progressionAction, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Relics.route) {
                        RelicsScreen(state = state, onAction = viewModel::progressionAction, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.HomeBase.route) {
                        HomeBaseScreen(state = state, onAction = viewModel::progressionAction, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Codex.route) {
                        CodexScreen(state = state, onAction = viewModel::progressionAction, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Endgame.route) {
                        EndgameScreen(state = state, onNavigate = { navController.navigate(it) }, onAction = viewModel::progressionAction, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Staking.route) {
                        StakingScreen(state = state, onStakeResult = viewModel::settleStake, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.ChangelogViewer.route) {
                        MarkdownViewerScreen(
                            assetFileName = "changelogs.md",
                            title = "Changelog",
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                if (tutorialActive) {
                    Box(Modifier.fillMaxSize(), contentAlignment = if (tutorialStep == 0 || tutorialStep == 3) androidx.compose.ui.Alignment.TopCenter else androidx.compose.ui.Alignment.BottomCenter) {
                        GuidedTutorialOverlay(
                            step = tutorialStep,
                            title = locale.getString("guided_tutorial_title_" + tutorialStep, "Guided Tutorial"),
                            instruction = locale.getString("guided_tutorial_instruction_" + tutorialStep, "Follow the highlighted action to continue."),
                            stepLabel = locale.getString("guided_tutorial_step", "Step %d of 4 • Action required", tutorialStep + 1),
                            reducedMotion = state.settings.reducedMotion
                        )
                    }
                }
            }
        }
    }
}
