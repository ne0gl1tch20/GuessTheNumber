package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveStore
import com.jarrlyyy.guessthenumber.desktop.persistence.defaultDesktopSaveDirectory
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.LiveOpsClaimDefinition
import com.jarrlyyy.guessthenumber.domain.model.LiveOpsClaimPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@Composable
internal fun DesktopLiveOpsScreen(onBack: () -> Unit) {
    val repository = remember { DesktopLiveOpsRepository() }
    val saveStore = remember { DesktopSaveStore(defaultDesktopSaveDirectory()) }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(repository.load()) }
    var state by remember { mutableStateOf<GameState?>(null) }
    var activeSlot by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var busyEventId by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            repository.refreshRemoteEvents()
            val latestSnapshot = repository.load()
            val slot = saveStore.activeSlot()
            Triple(slot, saveStore.load(slot), latestSnapshot)
        }
        activeSlot = loaded.first
        state = loaded.second
        snapshot = loaded.third
        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to More") }
            Text("Seasonal LiveOps", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Events use the canonical Android manifest and feature flags. Rewards are validated by the shared Kotlin claim policy and saved to your active slot.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    snapshot = withContext(Dispatchers.IO) {
                        repository.refreshRemoteEvents()
                        repository.load()
                    }
                    feedback = "LiveOps event catalog refreshed."
                }
            }) { Text("Refresh events") }
            Text("Save slot $activeSlot", style = MaterialTheme.typography.labelLarge)
        }
        feedback?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        if (isLoading) {
            CircularProgressIndicator()
        } else if (state == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No active save slot", style = MaterialTheme.typography.titleMedium)
                    Text("Create or select a save before claiming seasonal rewards.")
                }
            }
        } else if (!snapshot.enabled) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Seasonal events are disabled", style = MaterialTheme.typography.titleMedium)
                    Text(snapshot.error ?: "Enable both live_ops and seasonal_rewards in game/feature_flags.json.")
                }
            }
        } else {
            val current = state!!
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Event progression", style = MaterialTheme.typography.titleMedium)
                    Text("Correct guesses: ${current.correctGuesses} · Prestige: ${current.prestigeCount} · Ultra: ${current.ultraCount}")
                    Text("Permanent event bonus: +${current.permanentEventBoosts} levels · Claimed event rewards: ${current.liveOpsClaims.values.sum()}")
                }
            }
            if (snapshot.events.isEmpty()) {
                Text("No seasonal events are currently configured.")
            }
            snapshot.events.forEach { event ->
                val claims = current.liveOpsClaims[event.id] ?: 0
                val eligible = current.correctGuesses >= event.minCorrectGuesses &&
                    current.prestigeCount >= event.minPrestigeCount &&
                    current.ultraCount >= event.minUltraCount
                val canClaim = event.status == "ACTIVE" && claims < event.maxClaims &&
                    eligible && busyEventId == null
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text(event.status.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium)
                        Text(event.description)
                        Text("Reward: +${event.rewardNebula} Nebula · +${event.permanentBoostPerClaim} permanent bonus level(s) per claim")
                        Text("Requirements: ${event.minCorrectGuesses} correct guesses · ${event.minPrestigeCount} Prestige · ${event.minUltraCount} Ultra")
                        Text("Claims: $claims / ${event.maxClaims}")
                        if (!eligible) Text("You do not meet this event's progression requirements yet.", color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = {
                                val now = Instant.now()
                                val latest = repository.load()
                                val latestEvent = latest.events.firstOrNull { it.id == event.id }
                                if (!latest.enabled || latestEvent?.status != "ACTIVE" ||
                                    now.isBefore(Instant.parse(event.startDate)) ||
                                    !now.isBefore(Instant.parse(event.endDate))
                                ) {
                                    snapshot = latest
                                    feedback = "This event is no longer available to claim."
                                } else {
                                    busyEventId = event.id
                                    scope.launch {
                                        val oldState = state
                                        val updated = oldState?.let {
                                            LiveOpsClaimPolicy.apply(
                                                it,
                                                LiveOpsClaimDefinition(
                                                    id = event.id,
                                                    maxClaims = event.maxClaims,
                                                    rewardNebula = event.rewardNebula,
                                                    permanentBoostPerClaim = event.permanentBoostPerClaim,
                                                    minCorrectGuesses = event.minCorrectGuesses,
                                                    minPrestigeCount = event.minPrestigeCount,
                                                    minUltraCount = event.minUltraCount
                                                )
                                            )
                                        }
                                        val saved = updated != null && withContext(Dispatchers.IO) {
                                            saveStore.save(activeSlot, updated.copy(lastSaveTimestamp = System.currentTimeMillis()))
                                        }
                                        if (saved) {
                                            state = updated
                                            feedback = "Claimed +${event.rewardNebula} Nebula and the permanent event bonus."
                                        } else {
                                            feedback = "Claim rejected or save failed. No reward was granted."
                                        }
                                        snapshot = repository.load()
                                        busyEventId = null
                                    }
                                }
                            },
                            enabled = canClaim
                        ) {
                            Text(
                                when {
                                    claims >= event.maxClaims -> "Max claimed"
                                    !eligible -> "Requirements not met"
                                    event.status != "ACTIVE" -> event.status.lowercase().replaceFirstChar { it.uppercase() }
                                    busyEventId == event.id -> "Saving…"
                                    else -> "Claim reward"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
