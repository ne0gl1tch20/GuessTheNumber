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
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager

private data class WorldMapNode(
    val id: String,
    val nameKey: String,
    val fallback: String,
    val x: Float,
    val y: Float,
    val bossId: String,
    val bossKey: String,
    val bossFallback: String,
    val hp: Int,
    val secretId: String,
    val secretAt: Long
)

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
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val worlds = listOf(
        WorldMapNode("verdant_grove", "world_verdant_name", "Verdant Grove", 0.16f, 0.24f, "verdant_guardian", "boss_verdant_name", "Verdant Guardian", 3, "whispering_hollow", 50),
        WorldMapNode("crystal_caverns", "world_crystal_name", "Crystal Caverns", 0.43f, 0.43f, "crystal_golem", "boss_crystal_name", "Crystal Golem", 5, "shard_archive", 150),
        WorldMapNode("ember_summit", "world_ember_name", "Ember Summit", 0.67f, 0.24f, "ember_dragon", "boss_ember_name", "Ember Dragon", 7, "ashen_vault", 300),
        WorldMapNode("nebula_rift", "world_nebula_name", "Nebula Rift", 0.82f, 0.59f, "nebula_titan", "boss_nebula_name", "Nebula Titan", 10, "lost_observatory", 600)
    )
    val world = worlds.firstOrNull { it.id == selectedWorld } ?: worlds.first()
    val unlocked = world.id in state.unlockedWorldIds
    val totalUpgrades = state.upgradeLevels.values.sum()
    val canUnlock = when (world.id) {
        "verdant_grove" -> true
        "crystal_caverns" -> "verdant_guardian" in state.defeatedBossIds && state.correctGuesses >= 25 && totalUpgrades >= 5
        "ember_summit" -> "crystal_golem" in state.defeatedBossIds && state.correctGuesses >= 100 && totalUpgrades >= 15 && state.prestigeCount >= 1
        else -> "ember_dragon" in state.defeatedBossIds && state.correctGuesses >= 250 && totalUpgrades >= 40 && state.prestigeCount >= 3 && state.ultraCount >= 1
    }
    val canFight = when (world.id) {
        "verdant_grove" -> state.correctGuesses >= 10 && totalUpgrades >= 3
        "crystal_caverns" -> state.correctGuesses >= 50 && totalUpgrades >= 10
        "ember_summit" -> state.correctGuesses >= 150 && totalUpgrades >= 25 && state.prestigeCount >= 1
        else -> state.correctGuesses >= 500 && totalUpgrades >= 75 && state.prestigeCount >= 5 && state.ultraCount >= 1
    }
    val damage = state.bossBattleProgress[world.bossId] ?: 0
    val defeated = world.bossId in state.defeatedBossIds
    val secretFound = world.secretId in state.discoveredSecretIds
    val worldRequirementKey = when (world.id) { "crystal_caverns" -> "world_crystal_requirement"; "ember_summit" -> "world_ember_requirement"; "nebula_rift" -> "world_nebula_requirement"; else -> "world_verdant_requirement" }
    val bossRequirementKey = when (world.id) { "crystal_caverns" -> "boss_crystal_requirement"; "ember_summit" -> "boss_ember_requirement"; "nebula_rift" -> "boss_nebula_requirement"; else -> "boss_verdant_requirement" }
    val bossRewardKey = when (world.id) { "crystal_caverns" -> "boss_crystal_reward"; "ember_summit" -> "boss_ember_reward"; "nebula_rift" -> "boss_nebula_reward"; else -> "boss_verdant_reward" }
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
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(locale.getString("world_map_hint", "Pinch to zoom, drag to explore, and tap a world to inspect it."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val worldPathColor = MaterialTheme.colorScheme.outlineVariant
            val unlockedWorldGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            val lockedWorldGlow = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            val secretPathColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                        for (i in 0 until pts.lastIndex) {
                            drawLine(worldPathColor, pts[i], pts[i + 1], strokeWidth = 7f, cap = StrokeCap.Round)
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
                            onClick = { selectedWorld = node.id; if (isUnlocked) onProgressionAction("select_world", node.id) },
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
                    worlds.filter { it.secretAt <= state.correctGuesses }.forEach { node ->
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
                        Text(locale.getString(bossRequirementKey, "Battle requirements"), style = MaterialTheme.typography.bodySmall)
                        Text(locale.getString(bossRewardKey, "Victory reward"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        Text(locale.getString("boss_hp_progress", "%d / %d HP", (world.hp - damage).coerceAtLeast(0), world.hp))
                        LinearProgressIndicator(progress = { damage.toFloat() / world.hp.toFloat() }, modifier = Modifier.fillMaxWidth())
                        val phase = ((damage * 3) / world.hp + 1).coerceIn(1, 3)
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
                        val masteryCost = BigNumber(10L * (mastery + 1L))
                        Text(locale.getString("world_mastery_label", "World mastery: %d", mastery))
                        Text(locale.getString("world_mastery_bonus", "+%d%% Money per correct guess in this world.", mastery * 2), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        Button(onClick = { onProgressionAction("mastery", world.id) }, enabled = mastery < 10 && state.nebula >= masteryCost, modifier = Modifier.fillMaxWidth()) {
                            Text(locale.getString("world_mastery_upgrade", "Increase mastery • %s Nebula", masteryCost.format()))
                        }
                    }
                    if (world.secretAt <= state.correctGuesses) {
                        Text(if (secretFound) locale.getString("secret_discovered", "Secret area discovered") else locale.getString("secret_available", "A hidden route has appeared! Tap its key on the map."), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
