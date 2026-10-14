package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jarrlyyy.guessthenumber.ui.components.ReusableColorPickerDialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.consumePositionChange
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.navigation.Screen
import androidx.compose.ui.platform.LocalContext
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import com.jarrlyyy.guessthenumber.data.repository.FeatureFlagRepository
import com.jarrlyyy.guessthenumber.BuildConfig


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    state: GameState,
    onNavigate: (String) -> Unit,
    onUpdateSettings: (com.jarrlyyy.guessthenumber.domain.model.GameSettings) -> Unit
) {
    var colorTarget by remember { mutableStateOf<String?>(null) }
    var showRequirementsFor by remember { mutableStateOf<String?>(null) }
    var showFeatureDiscoveryFor by remember { mutableStateOf<String?>(null) }
    var featureDiscoveryStep by remember { mutableIntStateOf(0) }
    val discoveryExcludedRoutes = setOf("settings", "about", "dev_settings", "lua_testing_hub")
    val openFeature: (String) -> Unit = { route ->
        when (route) {
            "ultra", "prestige" -> onNavigate(route)
            "save_slots" -> onNavigate(Screen.SaveSlots.route)
            "changelog" -> onNavigate(Screen.ChangelogViewer.route)
            "music" -> onNavigate(Screen.MusicPlayer.route)
            else -> onNavigate(route)
        }
    }

    val locale = LocalAppLocaleManager.current
    val context = LocalContext.current
    val featureFlags = remember { FeatureFlagRepository(context).load() }
    val defaultOrder = com.jarrlyyy.guessthenumber.domain.model.DEFAULT_MORE_SCREEN_ORDER
    val normalizedOrder = remember(state.settings.moreScreenOrder) {
        (state.settings.moreScreenOrder + defaultOrder).distinct()
            .filter { it in defaultOrder }
            .let { it + defaultOrder.filterNot(it::contains) }
    }
    var order by remember(normalizedOrder) { mutableStateOf(normalizedOrder) }
    val visibleOrder = remember(order, featureFlags) {
        order.filter { route ->
            when (route) {
                "world_map" -> featureFlags["world_map"] == true
                "lua_hub" -> featureFlags["lua_scripting"] == true
                "lua_testing_hub" -> BuildConfig.DEBUG && featureFlags["dev_console"] == true
                "live_ops" -> featureFlags["live_ops"] == true && featureFlags["seasonal_rewards"] == true
                "mutators" -> featureFlags["challenge_builder"] == true
                "dev_settings" -> BuildConfig.DEBUG && featureFlags["dev_console"] == true
                else -> true
            }
        }
    }
    val allBossIds = remember {
        setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan")
    }
    val defeatedBossCount = state.defeatedBossIds.count { it in allBossIds }
    val firstBossDefeated = defeatedBossCount > 0
    val allBossesDefeated = state.defeatedBossIds.containsAll(allBossIds)
    val lockedRoutes = buildSet {
        if (!firstBossDefeated) {
            add("relics")
            add("codex")
        }
        if (!allBossesDefeated) add("endgame")
    }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var isReordering by remember { mutableStateOf(false) }

    LaunchedEffect(state.settings.moreScreenOrder) {
        if (state.settings.moreScreenOrder != normalizedOrder) {
            onUpdateSettings(state.settings.copy(moreScreenOrder = normalizedOrder))
        }
    }

    val labels = mapOf(
        "achievements" to "Achievements & Tiers",
        "live_ops" to "Random & Seasonal Events",
        "mutators" to "Mutators & Challenge Builder",
        "talent" to "Prestige Talent Web",
        "arcade" to "Arcade Minigames",
        "stats" to "Statistics",
        "changelog" to "Changelog",
        "settings" to "Settings",
        "about" to "About & Licenses",
        "dev_settings" to "Dev Settings",
        "ultra" to "Ultra Hub & Upgrades",
        "prestige" to "Prestige Hub & Upgrades",
        "save_slots" to "Save Slots",
        "music" to localizedText("Background Music Player"),
        "world_map" to localizedText("World Map"),
        "lua_hub" to localizedText("Activity Hub"),
        "lua_testing_hub" to localizedText("Lua Testing Hub"),
        "relics" to localizedText("Relic Collection"),
        "home_base" to localizedText("Home Base"),
        "codex" to localizedText("Explorer's Codex"),
        "endgame" to localizedText("Endgame Challenges"),
        "staking" to localizedText("Lucky Stake")
    )
    val icons = mapOf(
        "achievements" to Icons.Default.EmojiEvents,
        "live_ops" to Icons.Default.Event,
        "mutators" to Icons.Default.Tune,
        "talent" to Icons.Default.AccountTree,
        "arcade" to Icons.Default.Favorite,
        "stats" to Icons.Default.Face,
        "changelog" to Icons.Default.Info,
        "settings" to Icons.Default.Settings,
        "about" to Icons.Default.Info,
        "dev_settings" to Icons.Default.Build,
        "ultra" to Icons.Default.Star,
        "prestige" to Icons.Default.Star,
        "save_slots" to Icons.Default.Save,
        "music" to Icons.Default.MusicNote,
        "world_map" to Icons.Default.Map,
        "lua_hub" to Icons.Default.PlayArrow,
        "lua_testing_hub" to Icons.Default.Code,
        "relics" to Icons.Default.Diamond,
        "home_base" to Icons.Default.Home,
        "codex" to Icons.Default.MenuBook,
        "endgame" to Icons.Default.AutoAwesome,
        "staking" to Icons.Default.Casino
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText("More Hub"), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(localizedText("More Menu"), style = MaterialTheme.typography.headlineSmall)
                            Text(
                                localizedText("Your shortcuts, your order."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                if (isReordering) {
                                    dragging = null
                                    dragDistance = 0f
                                    onUpdateSettings(state.settings.copy(moreScreenOrder = order))
                                }
                                isReordering = !isReordering
                            }
                        ) {
                            Icon(Icons.Default.DragHandle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isReordering) localizedText("Done") else localizedText("Reorder"))
                        }
                    }

                    if (isReordering) {
                        Text(
                            localizedText("Hold and drag to rearrange"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(visibleOrder, key = { it }) { route ->
                val selected = dragging == route
                val isLocked = route in lockedRoutes
                val animatedScale by animateFloatAsState(
                    targetValue = if (selected) 1.025f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                    ),
                    label = "moreMenuItemScale"
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .animateItem(
                            placementSpec = androidx.compose.animation.core.spring(
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                            )
                        )
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            shadowElevation = if (selected) 16f else 0f
                        }
                        .then(
                            if (isReordering) {
                                Modifier.pointerInput(isReordering, route) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { dragging = route; dragDistance = 0f },
                                        onDragCancel = { dragging = null; dragDistance = 0f },
                                        onDragEnd = {
                                            dragging = null
                                            dragDistance = 0f
                                            onUpdateSettings(state.settings.copy(moreScreenOrder = order))
                                        },
                                        onDrag = { change, amount ->
                                            change.consumePositionChange()
                                            if (dragging == route) {
                                                dragDistance += amount.y
                                                val index = order.indexOf(route)
                                                if (dragDistance > 48.dp.toPx() && index < order.lastIndex) {
                                                    order = order.toMutableList().also {
                                                        val tmp = it[index]
                                                        it[index] = it[index + 1]
                                                        it[index + 1] = tmp
                                                    }
                                                    dragDistance = 0f
                                                } else if (dragDistance < -48.dp.toPx() && index > 0) {
                                                    order = order.toMutableList().also {
                                                        val tmp = it[index]
                                                        it[index] = it[index - 1]
                                                        it[index - 1] = tmp
                                                    }
                                                    dragDistance = 0f
                                                }
                                            }
                                        }
                                    )
                                }
                            } else {
                                Modifier.clickable {
                                    if (isLocked) {
                                        showRequirementsFor = route
                                    } else if (
                                        route !in discoveryExcludedRoutes &&
                                        route !in state.settings.tutorialSeenFeatures
                                    ) {
                                        showFeatureDiscoveryFor = route
                                        featureDiscoveryStep = 0
                                    } else {
                                        openFeature(route)
                                    }
                                }
                            }
                        ),
                    shape = MaterialTheme.shapes.large,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else state.settings.moreScreenButtonColors[route]?.let { hex ->
                        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
                    } ?: MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val customColor = state.settings.moreScreenButtonColors[route]?.let { hex ->
                            runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
                        }
                        val contentTint = if (selected) MaterialTheme.colorScheme.primary
                        else if (customColor != null) {
                            val luminance = 0.2126f * customColor.red + 0.7152f * customColor.green + 0.0722f * customColor.blue
                            if (luminance > 0.55f) Color.Black else Color.White
                        } else MaterialTheme.colorScheme.onSurfaceVariant
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else icons[route] ?: Icons.Default.Circle,
                            contentDescription = null,
                            tint = if (isLocked) MaterialTheme.colorScheme.outline else contentTint
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                labels[route] ?: route,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else contentTint
                            )
                            if (isLocked) {
                                Text(
                                    locale.getString("more_locked_short", "Locked · tap to view requirements"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isReordering) {
                            Icon(Icons.Default.DragHandle, contentDescription = localizedText("Reorder"), tint = contentTint)
                        } else {
                            IconButton(
                                onClick = { colorTarget = route },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = contentTint)
                            }
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
                                contentDescription = if (isLocked) locale.getString("more_locked_short", "Locked · tap to view requirements") else localizedText("Open"),
                                tint = if (isLocked) MaterialTheme.colorScheme.outline else contentTint
                            )
                        }
                    }
                }
            }

        }
    }

    if (showFeatureDiscoveryFor != null) {
        val route = showFeatureDiscoveryFor!!
        val finishDiscovery: () -> Unit = {
            onUpdateSettings(state.settings.copy(
                tutorialSeenFeatures = state.settings.tutorialSeenFeatures + route
            ))
            showFeatureDiscoveryFor = null
            openFeature(route)
        }
        AlertDialog(
            onDismissRequest = { showFeatureDiscoveryFor = null },
            title = { Text(locale.getString("more_feature_discovery_title", "Quick tour")) },
            text = {
                Text(
                    locale.getString(
                        if (featureDiscoveryStep == 0) "more_feature_discovery_step_1" else "more_feature_discovery_step_2",
                        if (featureDiscoveryStep == 0) "This feature has its own tools and progress. Take a quick look, then choose what to explore." else "You can return to this feature from the More Hub whenever you want."
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (featureDiscoveryStep == 0) featureDiscoveryStep = 1 else finishDiscovery()
                }) {
                    Text(locale.getString(if (featureDiscoveryStep == 0) "next" else "done", if (featureDiscoveryStep == 0) "Next" else "Done"))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = finishDiscovery) {
                        Text(locale.getString("more_feature_discovery_skip", "Skip and open"))
                    }
                    if (featureDiscoveryStep > 0) {
                        TextButton(onClick = { featureDiscoveryStep = 0 }) {
                            Text(locale.getString("back", "Back"))
                        }
                    }
                }
            }
        )
    }

    if (showRequirementsFor != null) {
        val route = showRequirementsFor!!
        val requirementText = when (route) {
            "relics", "codex" -> locale.getString(
                "more_unlock_first_boss",
                "Defeat your first world boss to unlock this feature."
            )
            "endgame" -> locale.getString(
                "more_unlock_all_bosses",
                "Defeat all four world bosses to unlock this feature."
            )
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { showRequirementsFor = null },
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            title = { Text(locale.getString("more_unlock_dialog_title", "Unlock requirements")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(requirementText)
                    Text(
                        locale.getString("more_unlock_boss_progress", "World bosses defeated: %d / 4", defeatedBossCount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (route == "relics" || route == "codex") {
                        Text(
                            locale.getString(
                                "more_unlock_first_boss_progress",
                                "First boss defeated: %s",
                                if (firstBossDefeated) locale.getString("yes", "Yes") else locale.getString("no", "No")
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRequirementsFor = null }) {
                    Text(locale.getString("close", "Close"))
                }
            }
        )
    }

    if (colorTarget != null) {
        val route = colorTarget!!
        val themeDefault = MaterialTheme.colorScheme.surfaceContainerHigh
        val defaultHex = "#%02X%02X%02X".format(
            (themeDefault.red * 255).toInt(),
            (themeDefault.green * 255).toInt(),
            (themeDefault.blue * 255).toInt()
        )
        ReusableColorPickerDialog(
            initialHex = state.settings.moreScreenButtonColors[route] ?: defaultHex,
            title = "🎨 ${labels[route] ?: route}",
            onDismiss = { colorTarget = null },
            onApply = { hex ->
                onUpdateSettings(
                    state.settings.copy(
                        moreScreenButtonColors = state.settings.moreScreenButtonColors + (route to hex)
                    )
                )
                colorTarget = null
            },
            onReset = {
                onUpdateSettings(
                    state.settings.copy(
                        moreScreenButtonColors = state.settings.moreScreenButtonColors - route
                    )
                )
                colorTarget = null
            }
        )
    }


}
