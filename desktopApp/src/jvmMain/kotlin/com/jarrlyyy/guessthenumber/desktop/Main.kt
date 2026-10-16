package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.awt.Dimension
import com.jarrlyyy.guessthenumber.desktop.update.WindowsBuildVersion
import com.jarrlyyy.guessthenumber.desktop.update.WindowsDebugInstaller
import com.jarrlyyy.guessthenumber.desktop.update.WindowsDebugUpdate
import com.jarrlyyy.guessthenumber.desktop.update.WindowsDebugUpdateClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun DesktopNavigationBar(
    currentDestination: DesktopDestination,
    position: DesktopNavigationPosition,
    onNavigate: (DesktopDestination) -> Unit
) {
    val destinations = DesktopDestination.MAIN_NAVIGATION_ORDER
    val vertical = position == DesktopNavigationPosition.LEFT ||
        position == DesktopNavigationPosition.RIGHT

    if (vertical) {
        NavigationRail(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 152.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                destinations.forEach { destination ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val selected = currentDestination == destination
                        NavigationRailItem(
                            selected = selected,
                            onClick = { if (!selected) onNavigate(destination) },
                            icon = {
                                Icon(
                                    imageVector = when (destination) {
                                        DesktopDestination.PLAY -> Icons.Default.PlayArrow
                                        DesktopDestination.UPGRADES,
                                        DesktopDestination.PRESTIGE,
                                        DesktopDestination.ULTRA -> Icons.Default.Star
                                        DesktopDestination.SHOP -> Icons.Default.ShoppingCart
                                        else -> Icons.Default.Menu
                                    },
                                    contentDescription = desktopNavigationLabel(destination)
                                )
                            },
                            label = { Text(desktopNavigationLabel(destination), maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { this.selected = selected }
                        )
                    }
                }
            }
        }
    } else {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp
        ) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                destinations.forEach { destination ->
                    val selected = currentDestination == destination
                    NavigationBarItem(
                        selected = selected,
                        onClick = { if (!selected) onNavigate(destination) },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    DesktopDestination.PLAY -> Icons.Default.PlayArrow
                                    DesktopDestination.UPGRADES,
                                    DesktopDestination.PRESTIGE,
                                    DesktopDestination.ULTRA -> Icons.Default.Star
                                    DesktopDestination.SHOP -> Icons.Default.ShoppingCart
                                    else -> Icons.Default.Menu
                                },
                                contentDescription = desktopNavigationLabel(destination)
                            )
                        },
                        label = { Text(desktopNavigationLabel(destination), maxLines = 1) },
                        modifier = Modifier.widthIn(min = 76.dp).semantics { this.selected = selected },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}

private fun desktopNavigationLabel(destination: DesktopDestination): String = when (destination) {
    DesktopDestination.SETTINGS -> "Settings"
    DesktopDestination.MUSIC_PLAYER -> "Music"
    DesktopDestination.SAVE_SLOTS -> "Saves"
    DesktopDestination.STATS -> "Stats"
    DesktopDestination.WORLD_MAP -> "World"
    DesktopDestination.CHANGELOG -> "Changelog"
    else -> destination.label
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Guess The Number",
        icon = painterResource("GTN_APP_LOGO.png"),
        state = remember { WindowState(size = DpSize(DesktopWindowMetrics.INITIAL_WIDTH, DesktopWindowMetrics.INITIAL_HEIGHT)) },
        resizable = true
    ) {
        val density = LocalDensity.current
        LaunchedEffect(density) {
            window.minimumSize = Dimension(
                with(density) { DesktopWindowMetrics.MIN_WIDTH.roundToPx() },
                with(density) { DesktopWindowMetrics.MIN_HEIGHT.roundToPx() }
            )
        }
        var themeMode by remember { mutableStateOf("System") }
        var primaryHex by remember { mutableStateOf("#7C4DFF") }
        var secondaryHex by remember { mutableStateOf("#00BCD4") }
        var tertiaryHex by remember { mutableStateOf("#00C853") }
        var reducedMotion by remember { mutableStateOf(false) }
        var navigationPosition by remember { mutableStateOf(DesktopNavigationPosition.load()) }

        DesktopTheme(
            window = window,
            themeMode = themeMode,
            primaryHex = primaryHex,
            secondaryHex = secondaryHex,
            tertiaryHex = tertiaryHex
        ) {
            val scope = rememberCoroutineScope()
            val shellFocusRequester = remember { FocusRequester() }
            val shell = remember { DesktopShellController() }
            val shellState = shell.state
            val updateClient = remember { WindowsDebugUpdateClient() }
            val installer = remember { WindowsDebugInstaller() }
            var availableUpdate by remember { mutableStateOf<WindowsDebugUpdate?>(null) }
            var status by remember { mutableStateOf("Checking for Windows Debug updates…") }
            var isBusy by remember { mutableStateOf(true) }

            suspend fun refreshUpdates() {
                isBusy = true
                status = "Checking for Windows Debug updates…"
                try {
                    availableUpdate = withContext(Dispatchers.IO) {
                        updateClient.findLatestUpdate(WindowsBuildVersion.current)
                    }
                    status = if (availableUpdate == null) {
                        "You're using the latest available Windows Debug CI artifact."
                    } else {
                        "Version ${availableUpdate!!.version} is ready to install."
                    }
                } catch (error: Exception) {
                    status = "Update check failed: ${error.message ?: "network error"}"
                } finally {
                    isBusy = false
                }
            }

            LaunchedEffect(Unit) {
                refreshUpdates()
            }
            LaunchedEffect(shellFocusRequester) {
                shellFocusRequester.requestFocus()
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            val handled = shell.handleKey(
                                key = event.key,
                                ctrlPressed = event.isCtrlPressed
                            )
                            if (handled && event.key == Key.Escape) {
                                shellFocusRequester.requestFocus()
                            }
                            handled
                        }
                        .focusRequester(shellFocusRequester)
                        .focusable(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (shellState.message != null) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                shellState.message.orEmpty(),
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (navigationPosition == DesktopNavigationPosition.TOP) {
                        DesktopNavigationBar(shellState.destination, navigationPosition, shell::navigate)
                    }

                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (navigationPosition == DesktopNavigationPosition.LEFT) {
                            DesktopNavigationBar(shellState.destination, navigationPosition, shell::navigate)
                        }

                    AnimatedContent(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        targetState = shellState.destination,
                        transitionSpec = {
                            if (reducedMotion) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                (
                                    fadeIn(tween(220)) +
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
                                ) togetherWith (
                                    fadeOut(tween(140)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 16 },
                                            animationSpec = tween(160)
                                        ) +
                                        scaleOut(targetScale = 0.985f, animationSpec = tween(160))
                                )
                            }
                        },
                        label = "androidStyleDestinationTransition"
                    ) { destination ->
                    when (destination) {
                        DesktopDestination.PLAY,
                        DesktopDestination.UPGRADES,
                        DesktopDestination.SHOP,
                        DesktopDestination.MORE,
                        DesktopDestination.PRESTIGE,
                        DesktopDestination.ULTRA,
                        DesktopDestination.TALENT,
                        DesktopDestination.STATS,
                        DesktopDestination.SETTINGS,
                        DesktopDestination.ACHIEVEMENTS,
                        DesktopDestination.WORLD_MAP,
                        DesktopDestination.MUTATORS,
                        DesktopDestination.SAVE_SLOTS,
                        DesktopDestination.RELICS,
                        DesktopDestination.HOME_BASE,
                        DesktopDestination.CODEX,
                        DesktopDestination.ENDGAME,
                        DesktopDestination.LUCKY_STAKE,
                        DesktopDestination.ARCADE -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                ProductionGuessGameScreen(
                                    section = when (destination) {
                                        DesktopDestination.UPGRADES -> DesktopGameSection.UPGRADES
                                        DesktopDestination.SHOP -> DesktopGameSection.SHOP
                                        DesktopDestination.MORE -> DesktopGameSection.MORE
                                        DesktopDestination.PRESTIGE -> DesktopGameSection.PRESTIGE
                                        DesktopDestination.ULTRA -> DesktopGameSection.ULTRA
                                        DesktopDestination.TALENT -> DesktopGameSection.TALENT
                                        DesktopDestination.STATS -> DesktopGameSection.STATS
                                        DesktopDestination.SETTINGS -> DesktopGameSection.SETTINGS
                                        DesktopDestination.ACHIEVEMENTS -> DesktopGameSection.ACHIEVEMENTS
                                        DesktopDestination.WORLD_MAP -> DesktopGameSection.WORLD_MAP
                                        DesktopDestination.MUTATORS -> DesktopGameSection.MUTATORS
                                        DesktopDestination.SAVE_SLOTS -> DesktopGameSection.SAVE_SLOTS
                                        DesktopDestination.RELICS -> DesktopGameSection.RELICS
                                        DesktopDestination.HOME_BASE -> DesktopGameSection.HOME_BASE
                                        DesktopDestination.CODEX -> DesktopGameSection.CODEX
                                        DesktopDestination.ENDGAME -> DesktopGameSection.ENDGAME
                                        DesktopDestination.LUCKY_STAKE -> DesktopGameSection.LUCKY_STAKE
                                        DesktopDestination.ARCADE -> DesktopGameSection.ARCADE
                                        else -> DesktopGameSection.PLAY
                                    },
                                    onOpenAbout = { shell.navigate(DesktopDestination.ABOUT) },
                                    onOpenPrestige = { shell.navigate(DesktopDestination.PRESTIGE) },
                                    onOpenUltra = { shell.navigate(DesktopDestination.ULTRA) },
                                    onOpenTalent = { shell.navigate(DesktopDestination.TALENT) },
                                    onOpenStats = { shell.navigate(DesktopDestination.STATS) },
                                    onOpenSettings = { shell.navigate(DesktopDestination.SETTINGS) },
                                    navigationPosition = navigationPosition,
                                    onNavigationPositionChange = { selected ->
                                        navigationPosition = selected
                                        DesktopNavigationPosition.save(selected)
                                    },
                                    onOpenParityDestination = { shell.navigate(it) },
                                    onBackFromSettings = { shell.navigate(DesktopDestination.MORE) },
                                    onThemeSettingsLoaded = { mode, primary, secondary, tertiary, reduced ->
                                        themeMode = mode
                                        primaryHex = primary
                                        secondaryHex = secondary
                                        tertiaryHex = tertiary
                                        reducedMotion = reduced
                                    }
                                )
                            }
                        }

                        DesktopDestination.CHANGELOG -> DesktopChangelogScreen(onBack = { shell.navigate(DesktopDestination.MORE) })

                        DesktopDestination.SAVE_SLOTS,
                        DesktopDestination.RELICS,
                        DesktopDestination.DEVELOPER_SETTINGS,
                        DesktopDestination.DEVELOPER_CONSOLE,
                        DesktopDestination.PROCESS_INSPECTOR,
                        DesktopDestination.MUSIC_PLAYER,
                        DesktopDestination.ACTIVITY_HUB,
                        DesktopDestination.LUA_TESTING_HUB,
                        DesktopDestination.LIVE_OPS -> DesktopParityScreen(destination = destination, onBack = { shell.navigate(DesktopDestination.MORE) }, onNavigate = { shell.navigate(it) })

                        DesktopDestination.ABOUT -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Windows Debug updates", style = MaterialTheme.typography.titleMedium)
                                    Text("Installed version: ${WindowsBuildVersion.current}")
                                    Text(
                                        status,
                                        color = if (
                                            classifyDesktopUpdateStatus(
                                                status = status,
                                                isBusy = isBusy,
                                                hasUpdate = availableUpdate != null
                                            ) == DesktopUpdateStatusKind.ERROR
                                        ) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.semantics {
                                            liveRegion = LiveRegionMode.Polite
                                        }
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { scope.launch { refreshUpdates() } },
                                            enabled = !isBusy
                                        ) {
                                            Text("Check again")
                                        }
                                        val update = availableUpdate
                                        if (update != null) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        isBusy = true
                                                        status = "Downloading and verifying version ${update.version}…"
                                                        try {
                                                            withContext(Dispatchers.IO) {
                                                                installer.downloadVerifyAndLaunch(update)
                                                            }
                                                            status = "Windows Installer is opening. GuessTheNumber will close for the update."
                                                            exitApplication()
                                                        } catch (error: Exception) {
                                                            status = "Update failed: ${error.message ?: "installer error"}"
                                                            isBusy = false
                                                        }
                                                    }
                                                },
                                                enabled = !isBusy
                                            ) {
                                                Text("Install update")
                                            }
                                        }
                                        if (isBusy) {
                                            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = status })
                                        }
                                    }
                                    Text(
                                        "Debug builds are separate from production releases. Windows may ask for permission to install the MSI.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("About Guess The Number", style = MaterialTheme.typography.headlineSmall)
                                    Text("Windows desktop edition")
                                    Text("Designed for offline play. No ads, analytics or accounts.")
                                    Text("This Windows build is under active development. Some Android features are not available on Windows yet.")
                                    Text("Installed version: ${WindowsBuildVersion.current}")
                                }
                            }
                        }
                    }
                    }
                        if (navigationPosition == DesktopNavigationPosition.RIGHT) {
                            DesktopNavigationBar(shellState.destination, navigationPosition, shell::navigate)
                        }
                    }

                    if (navigationPosition == DesktopNavigationPosition.BOTTOM) {
                        DesktopNavigationBar(shellState.destination, navigationPosition, shell::navigate)
                    }
                }
            }
        }
    }
}
