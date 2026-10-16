package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopTalentDef
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState

private data class DesktopTalentNodePosition(
    val id: String,
    val title: String,
    val description: String,
    val x: Float,
    val y: Float,
    val parentId: String? = null
)

private val desktopTalentNodePositions = listOf(
    DesktopTalentNodePosition("talent_speed_1", "Velocity I", "Increase automation speed.", .5f, .12f),
    DesktopTalentNodePosition("talent_crit_1", "Precision I", "Improve the rewards of accurate play.", .2f, .32f),
    DesktopTalentNodePosition("talent_speed_2", "Velocity II", "Push automation further.", .8f, .32f, "talent_speed_1"),
    DesktopTalentNodePosition("talent_crit_2", "Precision II", "Build on your precision training.", .2f, .55f, "talent_crit_1"),
    DesktopTalentNodePosition("talent_reward_1", "Nebula Resonance", "Increase earnings across your run.", .5f, .72f, "talent_crit_2"),
    DesktopTalentNodePosition("talent_master_1", "Cosmic Oracle", "Unlock the final branch of the talent web.", .8f, .88f, "talent_reward_1")
)

/** Windows counterpart of Android's connected, selectable Prestige Talent Web. */
@Composable
internal fun DesktopTalentWebContent(
    state: GameState,
    talents: List<DesktopTalentDef>,
    isSaving: Boolean,
    onBuyTalent: (DesktopTalentDef) -> Unit
) {
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selectedId by remember { mutableStateOf("talent_speed_1") }
    val byId = remember(talents) { talents.associateBy { it.id } }
    val positions = remember(talents) { desktopTalentNodePositions.filter { it.id in byId } }
    val selectedPosition = positions.firstOrNull { it.id == selectedId } ?: positions.firstOrNull()
    val selected = selectedPosition?.let { byId[it.id] }
    val selectedOwned = selected?.id?.let { it in state.prestigeShopPurchases } ?: false
    val parentOwned = selectedPosition?.parentId?.let { it in state.prestigeShopPurchases } ?: true
    val affordable = selected != null && state.prestige >= BigNumber(selected.cost)

    val unlockedBranchColor = MaterialTheme.colorScheme.primary
    val lockedBranchColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Prestige Talent Web", style = MaterialTheme.typography.headlineSmall)
                Text("Prestige balance: ${state.prestige.format()}", style = MaterialTheme.typography.titleMedium)
                Text("Select a connected node to inspect and unlock its skill.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = { zoom = .82f; pan = Offset.Zero }) { Icon(Icons.Default.FitScreen, contentDescription = "Fit tree") }
            OutlinedButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(.55f) }) { Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out") }
            OutlinedButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(2.8f) }) { Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in") }
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().height(400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight
            Box(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                }.pointerInput(Unit) {
                    detectTransformGestures { _, delta, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(.55f, 2.8f)
                        pan = Offset(
                            (pan.x + delta.x).coerceIn(-size.width * .65f, size.width * .65f),
                            (pan.y + delta.y).coerceIn(-size.height * .65f, size.height * .65f)
                        )
                    }
                }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    positions.filter { it.parentId != null }.forEach { node ->
                        val parent = positions.firstOrNull { it.id == node.parentId } ?: return@forEach
                        val bothOwned = node.id in state.prestigeShopPurchases &&
                            parent.id in state.prestigeShopPurchases
                        drawLine(
                            color = if (bothOwned) unlockedBranchColor else lockedBranchColor,
                            start = androidx.compose.ui.geometry.Offset(size.width * parent.x, size.height * parent.y),
                            end = androidx.compose.ui.geometry.Offset(size.width * node.x, size.height * node.y),
                            strokeWidth = 7f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                positions.forEach { node ->
                    val talent = byId[node.id] ?: return@forEach
                    val owned = node.id in state.prestigeShopPurchases
                    val parentReady = node.parentId == null || node.parentId in state.prestigeShopPurchases
                    Surface(
                        onClick = { selectedId = node.id },
                        modifier = Modifier.offset(
                            x = canvasWidth * node.x - 60.dp,
                            y = canvasHeight * node.y - 35.dp
                        ).width(120.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = when {
                            node.id == selectedId -> MaterialTheme.colorScheme.primaryContainer
                            owned -> MaterialTheme.colorScheme.tertiaryContainer
                            parentReady -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (node.id == selectedId) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        tonalElevation = if (node.id == selectedId) 4.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (owned) Icons.Default.CheckCircle else if (parentReady) Icons.Default.Star else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (owned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                            Text(node.title, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                            Text("${talent.cost} Prestige", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (selected != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(selectedPosition?.title ?: selected.name, style = MaterialTheme.typography.titleLarge)
                    Text(selectedPosition?.description ?: selected.description, style = MaterialTheme.typography.bodyMedium)
                    if (!parentOwned) {
                        Text("Unlock the connected parent node first.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { onBuyTalent(selected) },
                        enabled = !isSaving && !selectedOwned && parentOwned && affordable,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedOwned) "Unlocked" else "Unlock skill")
                    }
                }
            }
        }
    }
}
