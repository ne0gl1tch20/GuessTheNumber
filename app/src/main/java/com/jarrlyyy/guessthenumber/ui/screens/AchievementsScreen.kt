package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.data.repository.JsonConfigRepository
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.ui.theme.MoneyGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    state: GameState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val achievements = remember {
        JsonConfigRepository(context).loadAchievements()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements & Perks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Text("Achievement Progress", style = MaterialTheme.typography.titleMedium)
                        val unlockedCount = achievements.count { state.achievements.contains(it.id) }
                        Text(
                            text = "Unlocked: $unlockedCount / ${achievements.size}",
                            fontSize = 16.sp,
                            color = MoneyGold
                        )
                        Text(
                            text = "Unlocking achievements grants permanent Nebula rewards and passive perks!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(achievements) { achievement ->
                val isUnlocked = state.achievements.contains(achievement.id)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) MoneyGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(achievement.name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(achievement.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reward: +${achievement.rewardNebula} Nebula", fontSize = 12.sp, color = MoneyGold)
                        }
                    }
                }
            }
        }
    }
}
