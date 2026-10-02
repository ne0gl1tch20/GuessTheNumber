package com.jarrlyyy.guessthenumber.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.screens.*


@Composable
fun NavGraph(
    state: GameState,
    onMakeGuess: (Long) -> Unit,
    onBuyUpgrade: (String, BigNumber) -> Unit,
    onBuyShopItem: (String, Long) -> Unit,
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
    onPlayMusic: () -> Unit,
    onPauseMusic: () -> Unit,
    onStopMusic: () -> Unit,
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
    onEarnMinigameReward: (Long, BigNumber) -> Unit,
    onUpdateMultiplier: (String) -> Unit
) {
    if (!state.tutorialCompleted) {
        TutorialScreen(onComplete = onCompleteTutorial)
        return
    }

    val navController = rememberNavController()
    val items = listOf(Screen.Play, Screen.Upgrade, Screen.Shop, Screen.More)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (hasPreviousCrash) {
        CrashRecoveryScreen(onDismiss = onDismissCrash)
        return
    }

    if (showTimeTravelPopup) {
        AlertDialog(
            onDismissRequest = onDismissTimeTravel,
            title = { Text("⏳ Time Travel Detected!") },
            text = { Text("It seems like the time has changed since the last time you played (device clock is $timeTravelSeconds seconds behind last save). As a consequence, auto-clicker and passive generation have been temporarily paused for 24 hours.") },
            confirmButton = {
                Button(onClick = onDismissTimeTravel) {
                    Text("Understood")
                }
            }
        )
    } else if (showChangelogPopup) {
        AlertDialog(
            onDismissRequest = onDismissChangelog,
            title = { Text("🚀 What's New in v1.6!") },
            text = { Text("A new version of Guess The Number is installed! Check out the changelog for details on widgets, anti-time-travel, and advanced features.") },
            confirmButton = {
                Button(onClick = {
                    onDismissChangelog()
                    navController.navigate(Screen.ChangelogViewer.route)
                }) {
                    Text("View Changelog")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissChangelog) {
                    Text("Dismiss")
                }
            }
        )
    } else if (offlineGains != null && offlineGains > BigNumber.ZERO) {
        AlertDialog(
            onDismissRequest = onDismissOfflineGains,
            title = { Text("🌙 Welcome Back!") },
            text = { Text("While you were away, your auto-clicker generated ${offlineGains.format()} money!") },
            confirmButton = {
                Button(onClick = onDismissOfflineGains) {
                    Text("Collect")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (items.any { it.route == currentRoute }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
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
        NavHost(
            navController = navController,
            startDestination = Screen.Play.route,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + slideInHorizontally(initialOffsetX = { 100 }) },
            exitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) + slideOutHorizontally(targetOffsetX = { -100 }) },
            popEnterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + slideInHorizontally(initialOffsetX = { -100 }) },
            popExitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) + slideOutHorizontally(targetOffsetX = { 100 }) }
        ) {
            composable(Screen.Play.route) {
                PlayScreen(state = state, onMakeGuess = onMakeGuess)
            }
            composable(Screen.Upgrade.route) {
                UpgradeScreen(
                    state = state,
                    onBuyUpgrade = onBuyUpgrade,
                    onUpdateMultiplier = onUpdateMultiplier
                )
            }
            composable(Screen.Shop.route) {
                ShopScreen(state = state, onBuyShopItem = onBuyShopItem)
            }
            composable(Screen.More.route) {
                MoreScreen(
                    state = state,
                    onNavigate = { route -> navController.navigate(route) },
                    onUpdateBackgroundMusic = onUpdateBackgroundMusic,
                    isPlayingMusic = isPlayingMusic,
                    onPlayMusic = onPlayMusic,
                    onPauseMusic = onPauseMusic,
                    onStopMusic = onStopMusic
                )
            }
            composable(Screen.Prestige.route) {
                PrestigeScreen(
                    state = state,
                    onPrestige = onPrestige,
                    onBuyPrestigeUpgrade = onBuyPrestigeUpgrade,
                    onBuyPrestigeShopItem = onBuyPrestigeShopItem,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Ultra.route) {
                UltraScreen(
                    state = state,
                    onUltra = onUltra,
                    onBuyUltraUpgrade = onBuyUltraUpgrade,
                    onBuyUltraShopItem = onBuyUltraShopItem,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Arcade.route) {
                ArcadeScreen(onEarnReward = onEarnMinigameReward, onBack = { navController.popBackStack() })
            }
            composable(Screen.Stats.route) {
                StatsScreen(state = state, onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(state = state, onExportSave = onExportSave, onImportSave = onImportSave, onResetData = onResetData, onUpdateSettings = onUpdateSettings, onBack = { navController.popBackStack() })
            }
            composable(Screen.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.DevSettings.route) {
                DevSettingsScreen(
                    state = state,
                    onExecuteCommand = onExecuteDevCommand,
                    onImportSave = onImportSave,
                    onUpdateSettings = onUpdateSettings,
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
            composable("achievements") {
                AchievementsScreen(
                    state = state,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("talent") {
                TalentScreen(
                    state = state,
                    onBuyTalent = onBuyPrestigeShopItem,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("mutators") {
                MutatorsScreen(
                    state = state,
                    onSelectMutator = { id -> onBuyShopItem(id, 0L) },
                    onCompleteChallenge = { id, reward -> onEarnMinigameReward(reward, BigNumber.ZERO) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ChangelogViewer.route) {
                MarkdownViewerScreen(
                    assetFileName = "changelogs.md",
                    title = "Changelog",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
