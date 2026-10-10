package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.data.repository.WorldConfigRepository
import androidx.compose.ui.platform.LocalContext
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldMapScreen(
    state: GameState,
    onWorldAction: (String, String) -> Unit,
    onProgressionAction: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val locale = LocalAppLocaleManager.current
    var selectedWorld by remember { mutableStateOf(state.activeWorldId) }
    // Keep the map selection aligned with persisted progression, especially when an Endless Rift
    // victory advances to the next world or an active run is restored after process recreation.
    LaunchedEffect(state.activeWorldId) {
        selectedWorld = state.activeWorldId
    }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    val worlds = remember { WorldConfigRepository(context).loadWorlds() }
    val world = worlds.firstOrNull { it.id == selectedWorld } ?: worlds.first()
    val unlocked = world.id in state.unlockedWorldIds
    val totalUpgrades = state.upgradeLevels.values.sum()
    val canUnlock = world.unlockBossId == null || (
        world.unlockBossId in state.defeatedBossIds &&
            state.correctGuesses >= world.unlockCorrectGuesses &&
            totalUpgrades >= world.unlockUpgradeCount &&
            state.prestigeCount >= world.unlockPrestigeCount &&
            state.ultraCount >= world.unlockUltraCount
    )
    val canFight = state.endlessRiftActive || (
        state.correctGuesses >= world.fightCorrectGuesses &&
            totalUpgrades >= world.fightUpgradeCount &&
            state.prestigeCount >= world.fightPrestigeCount &&
            state.ultraCount >= world.fightUltraCount
    )
    val damage = state.bossBattleProgress[world.bossId] ?: 0
    val bossHp = world.hp + if (state.endlessRiftActive) ((state.endlessRiftTier - 1).coerceAtLeast(0) * 2) else 0
    val defeated = world.bossId in state.defeatedBossIds && !state.endlessRiftActive
    val secretFound = world.secretId in state.discoveredSecretIds
    val worldRequirementKey = world.worldRequirementKey
    val bossRequirementKey = world.bossRequirementKey
    val bossRewardKey = world.bossRewardKey
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locale.getString("world_map_title", "World Map")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = locale.getString("back", "Back")) } },
                actions = {
                    IconButton(onClick = { zoom = 0.82f; pan = Offset.Zero }) {
                        Icon(Icons.Default.FitScreen, contentDescription = locale.getString("map_fit_screen", "Fit map to screen"))
                    }
                    IconButton(onClick = { zoom = 1f; pan = Offset.Zero }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = locale.getString("map_reset_view", "Reset map view"))
                    }
                    IconButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(0.7f) }) { Icon(Icons.Default.ZoomOut, contentDescription = locale.getString("zoom_out", "Zoom out")) }
                    IconButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(3f) }) { Icon(Icons.Default.ZoomIn, contentDescription = locale.getString("zoom_in", "Zoom in")) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(locale.getString("world_map_hint", "Pinch to zoom, drag to explore, and tap a world to inspect it."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val worldPathColor = MaterialTheme.colorScheme.outlineVariant
            val unlockedWorldGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            val lockedWorldGlow = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            val secretPathColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                val mapWidth = maxWidth
                val mapHeight = maxHeight
                Box(Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom; scaleY = zoom
                    translationX = pan.x; translationY = pan.y
                }.pointerInput(Unit) {
                    detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.65f, 3f)
                        pan = Offset(
                            (pan.x + gesturePan.x).coerceIn(-size.width * 0.7f, size.width * 0.7f),
                            (pan.y + gesturePan.y).coerceIn(-size.height * 0.7f, size.height * 0.7f)
                        )
                    }
                }) {
                    Canvas(Modifier.fillMaxSize()) {
                        val pts = worlds.map { Offset(size.width * it.x, size.height * it.y) }
                        // The route graph branches from Crystal Caverns toward both later realms.
                        val connections = listOf(0 to 1, 1 to 2, 1 to 3, 2 to 3)
                        connections.forEach { (from, to) ->
                            val routeUnlocked = worlds[from].id in state.unlockedWorldIds &&
                                worlds[to].id in state.unlockedWorldIds
                            drawLine(
                                color = worldPathColor.copy(alpha = if (routeUnlocked) 0.95f else 0.45f),
                                start = pts[from],
                                end = pts[to],
                                strokeWidth = if (routeUnlocked) 7f else 5f,
                                cap = StrokeCap.Round
                            )
                        }
                        worlds.forEachIndexed { i, node ->
                            if (state.correctGuesses >= node.secretAt) {
                                drawLine(secretPathColor, pts[i], Offset(pts[i].x + 38f, pts[i].y + 52f), strokeWidth = 4f, cap = StrokeCap.Round)
                            }
                        }
                        worlds.forEachIndexed { i, node ->
                            val p = pts[i]
                            drawCircle(
                                color = if (node.id in state.unlockedWorldIds) unlockedWorldGlow else lockedWorldGlow,
                                radius = 35f, center = p
                            )
                        }
                    }
                    worlds.forEach { node ->
                        val isUnlocked = node.id in state.unlockedWorldIds
                        val isSelected = node.id == selectedWorld
                        Surface(
                            onClick = { if (!state.endlessRiftActive || node.id == state.activeWorldId) { selectedWorld = node.id; if (isUnlocked) onProgressionAction("select_world", node.id) } },
                            modifier = Modifier.offset(x = mapWidth * node.x - 54.dp, y = mapHeight * node.y - 35.dp).width(108.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                isUnlocked -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            tonalElevation = if (isSelected) 5.dp else 1.dp
                        ) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = when (node.id) {
                                        "verdant_grove" -> Icons.Default.Forest
                                        "crystal_caverns" -> Icons.Default.Diamond
                                        "ember_summit" -> Icons.Default.LocalFireDepartment
                                        else -> Icons.Default.AutoAwesome
                                    },
                                    contentDescription = null,
                                    tint = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(locale.getString(node.nameKey, node.fallback), style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                Text(if (node.bossId in state.defeatedBossIds) "★" else if (isUnlocked) "●" else "🔒", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    worlds.filter { it.secretAt <= state.correctGuesses && it.id in state.unlockedWorldIds }.forEach { node ->
                        Surface(
                            onClick = { onProgressionAction("secret", node.secretId) },
                            modifier = Modifier.offset(x = mapWidth * node.x - 18.dp, y = mapHeight * node.y + 46.dp).size(36.dp),
                            shape = RoundedCornerShape(50),
                            color = if (node.secretId in state.discoveredSecretIds) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Key, contentDescription = locale.getString("secret_area", "Secret area"), modifier = Modifier.size(18.dp)) } }
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(locale.getString(world.nameKey, world.fallback), style = MaterialTheme.typography.titleLarge)
                    Text(locale.getString(world.bossKey, world.bossFallback), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (unlocked) locale.getString("world_status_unlocked", "Unlocked") else locale.getString("world_status_locked", "Locked"))
                    if (!unlocked) {
                        Text(locale.getString(worldRequirementKey, "Requirements not met"), style = MaterialTheme.typography.bodySmall)
                        Text(locale.getString("world_requirements_pending", "Requirements not met"), style = MaterialTheme.typography.labelSmall)
                        Button(onClick = { onWorldAction(world.id, "unlock") }, enabled = canUnlock, modifier = Modifier.fillMaxWidth()) {
                            Text(locale.getString("world_unlock", "Unlock world"))
                        }
                    } else if (!defeated) {
                        if (!state.endlessRiftActive) {
                            Text(locale.getString(bossRequirementKey, "Battle requirements"), style = MaterialTheme.typography.bodySmall)
                            Text(locale.getString(bossRewardKey, "Victory reward"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (state.endlessRiftActive) {
                            Text(locale.getString("rift_tier_label", "Endless Rift • Tier %d", state.endlessRiftTier), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Text(locale.getString("boss_hp_progress", "%d / %d HP", (bossHp - damage).coerceAtLeast(0), bossHp))
                        LinearProgressIndicator(progress = { (damage.toFloat() / bossHp.toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        val phase = ((damage * 3) / bossHp + 1).coerceIn(1, 3)
                        Text(locale.getString("boss_phase_label", "Phase %d / 3 • Correct guesses damage the boss.", phase))
                        Text(locale.getString("boss_battle_tip", "Phase 1: every 4 misses pushes back your progress. Phase 2: every 3 misses. Phase 3: every 2 misses."))
                        if (state.activeBossBattleWorldId == world.id) {
                            Text(locale.getString("boss_battle_active", "Battle active! Return to Play and guess the number. Every three misses restores 1 HP."), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { onWorldAction(world.id, "boss") }, enabled = canFight || state.activeBossBattleWorldId == world.id, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.SportsMma, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(locale.getString(if (state.activeBossBattleWorldId == world.id) "boss_battle_stop" else "boss_battle_start", if (state.activeBossBattleWorldId == world.id) "Leave battle" else "Start boss battle"))
                        }
                    } else {
                        Text(locale.getString("boss_defeated", "Boss defeated"))
                        val mastery = state.worldMasteryLevels[world.id] ?: 1
                        val masteryBaseCost = BigNumber(10L * (mastery + 1L))
                        val secretForMastery = mapOf(
                            "verdant_grove" to "whispering_hollow",
                            "crystal_caverns" to "shard_archive",
                            "ember_summit" to "ashen_vault",
                            "nebula_rift" to "lost_observatory"
                        )[world.id]
                        val secretDiscount = if (secretForMastery != null && secretForMastery in state.discoveredSecretIds) 0.20 else 0.0
                        val sanctuaryDiscount = (state.homeBaseLevel * 0.01).coerceAtMost(0.20)
                        val fullRelicSetDiscount = if (
                            state.equippedRelicIds.containsAll(setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan"))
                        ) 0.10 else 0.0
                        val totalDiscount = (secretDiscount + sanctuaryDiscount + fullRelicSetDiscount).coerceAtMost(0.40)
                        val masteryCost = masteryBaseCost * BigNumber(1.0 - totalDiscount)
                        Text(locale.getString("world_mastery_label", "World mastery: %d", mastery))
                        Text(locale.getString("world_mastery_bonus", "+%d%% Money per correct guess in this world.", mastery * 2), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        Button(onClick = { onProgressionAction("mastery", world.id) }, enabled = mastery < 10 && state.nebula >= masteryCost, modifier = Modifier.fillMaxWidth()) {
                            Text(locale.getString("world_mastery_upgrade", "Increase mastery • %s Nebula", masteryCost.format()))
                        }
                    }
                    if (world.secretAt <= state.correctGuesses && unlocked) {
                        Text(if (secretFound) locale.getString("secret_discovered", "Secret area discovered") else locale.getString("secret_available", "A hidden route has appeared! Tap its key on the map."), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
