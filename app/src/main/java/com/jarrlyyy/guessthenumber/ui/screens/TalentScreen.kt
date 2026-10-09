package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.localization.LocalAppLocaleManager
import com.jarrlyyy.guessthenumber.ui.theme.PrestigeBlue

private data class TalentTreeNode(
    val id: String,
    val titleKey: String,
    val title: String,
    val descriptionKey: String,
    val description: String,
    val cost: Long,
    val x: Float,
    val y: Float,
    val parent: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalentScreen(state: GameState, onBuyTalent: (String, Long) -> Unit, onBack: () -> Unit) {
    val locale = LocalAppLocaleManager.current
    val talents = listOf(
        TalentTreeNode("talent_speed_1", "talent_speed_1_name", "Velocity I", "talent_speed_1_desc", "Increase automation speed.", 100, .5f, .12f),
        TalentTreeNode("talent_crit_1", "talent_crit_1_name", "Precision I", "talent_crit_1_desc", "Improve the rewards of accurate play.", 250, .2f, .32f),
        TalentTreeNode("talent_speed_2", "talent_speed_2_name", "Velocity II", "talent_speed_2_desc", "Push automation further.", 500, .8f, .32f, "talent_speed_1"),
        TalentTreeNode("talent_crit_2", "talent_crit_2_name", "Precision II", "talent_crit_2_desc", "Build on your precision training.", 1000, .2f, .55f, "talent_crit_1"),
        TalentTreeNode("talent_reward_1", "talent_reward_1_name", "Nebula Resonance", "talent_reward_1_desc", "Increase earnings across your run.", 2500, .5f, .72f, "talent_crit_2"),
        TalentTreeNode("talent_master_1", "talent_master_1_name", "Cosmic Oracle", "talent_master_1_desc", "Unlock the final branch of the talent web.", 10000, .8f, .88f, "talent_reward_1")
    )
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selectedId by remember { mutableStateOf("talent_speed_1") }
    val selected = talents.first { it.id == selectedId }
    val selectedUnlocked = selected.id in state.prestigeShopPurchases
    val selectedParentUnlocked = selected.parent == null || selected.parent in state.prestigeShopPurchases
    val selectedAffordable = state.prestige >= BigNumber(selected.cost)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(locale.getString("talent_tree_title", "Prestige Skill Tree")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = locale.getString("back", "Back")) } },
            actions = {
                IconButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(.55f) }) { Icon(Icons.Default.ZoomOut, contentDescription = locale.getString("zoom_out", "Zoom out")) }
                IconButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(2.8f) }) { Icon(Icons.Default.ZoomIn, contentDescription = locale.getString("zoom_in", "Zoom in")) }
                IconButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Icon(Icons.Default.CenterFocusStrong, contentDescription = locale.getString("map_reset_view", "Reset view")) }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(locale.getString("talent_balance", "Prestige balance: %s", state.prestige.format()), style = MaterialTheme.typography.titleMedium)
                        Text(locale.getString("talent_zoom_hint", "Pinch to zoom and drag to explore the connected branches."), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                val w = maxWidth
                val h = maxHeight
                Box(Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y }.pointerInput(Unit) {
                    detectTransformGestures { _, delta, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(.55f, 2.8f)
                        pan = Offset((pan.x + delta.x).coerceIn(-size.width * .65f, size.width * .65f), (pan.y + delta.y).coerceIn(-size.height * .65f, size.height * .65f))
                    }
                }) {
                    Canvas(Modifier.fillMaxSize()) {
                        val byId = talents.associateBy { it.id }
                        talents.filter { it.parent != null }.forEach { node ->
                            val parent = byId[node.parent] ?: return@forEach
                            drawLine(
                                color = if (node.id in state.prestigeShopPurchases && parent.id in state.prestigeShopPurchases) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                start = Offset(size.width * parent.x, size.height * parent.y),
                                end = Offset(size.width * node.x, size.height * node.y),
                                strokeWidth = 7f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    talents.forEach { node ->
                        val bought = node.id in state.prestigeShopPurchases
                        val parentReady = node.parent == null || node.parent in state.prestigeShopPurchases
                        Surface(
                            onClick = { selectedId = node.id },
                            modifier = Modifier.offset(x = w * node.x - 60.dp, y = h * node.y - 35.dp).width(120.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = when { node.id == selectedId -> MaterialTheme.colorScheme.primaryContainer; bought -> MaterialTheme.colorScheme.tertiaryContainer; parentReady -> MaterialTheme.colorScheme.surface; else -> MaterialTheme.colorScheme.surfaceVariant },
                            border = if (node.id == selectedId) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            tonalElevation = if (node.id == selectedId) 4.dp else 1.dp
                        ) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(if (bought) Icons.Default.CheckCircle else if (parentReady) Icons.Default.Star else Icons.Default.Lock, contentDescription = null, tint = if (bought) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                                Text(locale.getString(node.titleKey, node.title), style = MaterialTheme.typography.labelMedium, maxLines = 2)
                                Text(locale.getString("talent_node_cost", "%d Prestige", node.cost), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(locale.getString(selected.titleKey, selected.title), style = MaterialTheme.typography.titleLarge)
                    Text(locale.getString(selected.descriptionKey, selected.description), style = MaterialTheme.typography.bodyMedium)
                    if (!selectedParentUnlocked) Text(locale.getString("talent_parent_locked", "Unlock the connected parent node first."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onBuyTalent(selected.id, selected.cost) }, enabled = !selectedUnlocked && selectedParentUnlocked && selectedAffordable, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrestigeBlue)) {
                        Text(if (selectedUnlocked) locale.getString("unlocked", "Unlocked") else locale.getString("talent_unlock", "Unlock skill"))
                    }
                }
            }
        }
    }
}
