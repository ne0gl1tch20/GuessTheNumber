package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.ui.localization.localizedText

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.data.repository.LiveOpsEvent
import com.jarrlyyy.guessthenumber.data.repository.LiveOpsRepository
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveOpsScreen(
    state: GameState,
    onClaimReward: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<LiveOpsEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            val manifest = LiveOpsRepository(context).loadLiveOpsEvents()
            events = manifest.events
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText("Seasonal Events")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(localizedText("Event Perks & Permanent Bonuses"), style = MaterialTheme.typography.titleMedium)
                            Text(text = localizedText("Permanent Bonus Level: +${state.permanentEventBoosts} (Grants permanent money & currency multipliers across saves!)"),
                                fontSize = 16.sp,
                                color = MoneyGold
                            )
                            Text(text = localizedText("Participate in limited-time live ops events to claim rewards and permanently enhance your progression."),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(events) { event ->
                    val claims = state.liveOpsClaims[event.id] ?: 0
                    val canClaim = event.status == "ACTIVE" && claims < event.maxClaims

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(event.title, fontSize = 18.sp, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = when (event.status) {
                                        "ACTIVE" -> MaterialTheme.colorScheme.primaryContainer
                                        "UPCOMING" -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.errorContainer
                                    }
                                ) {
                                    Text(
                                        text = event.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(event.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(localizedText("Currency: ${event.currencyName}"), fontSize = 12.sp, style = MaterialTheme.typography.labelMedium)
                                    Text(localizedText("Claims: $claims / ${event.maxClaims}"), fontSize = 12.sp, color = MoneyGold)
                                }
                                Button(
                                    onClick = { onClaimReward(event.id) },
                                    enabled = canClaim,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(if (claims >= event.maxClaims) "Max Claimed" else "Claim Reward (+10 Nebula)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
