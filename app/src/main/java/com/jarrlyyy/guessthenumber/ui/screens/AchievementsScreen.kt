package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.jarrlyyy.guessthenumber.ui.components.ExpressiveMilestoneBanner
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
    val configRepo = remember(state.settings.locale) { JsonConfigRepository(context, state.settings.locale) }
    val locale = configRepo.localeManager
    val achievements = remember(state.settings.locale) {
        configRepo.loadAchievements()
    }
    var observedAchievements by remember { mutableStateOf(state.achievements) }
    var showUnlockCelebration by remember { mutableStateOf(false) }
    var unlockCelebrationText by remember { mutableStateOf("") }

    LaunchedEffect(state.achievements) {
        val newlyUnlocked = state.achievements - observedAchievements
        observedAchievements = state.achievements
        if (newlyUnlocked.isNotEmpty()) {
            val achievementName = achievements.firstOrNull { it.id in newlyUnlocked }?.name
            unlockCelebrationText = if (achievementName != null) {
                locale.getString("achievement_unlocked_banner", "Unlocked: %s", achievementName)
            } else {
                locale.getString("achievement_unlocked_fallback", "A new achievement is unlocked!")
            }
            showUnlockCelebration = true
            delay(if (state.settings.reducedMotion) 700L else 2200L)
            showUnlockCelebration = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locale.getString("achievements_title", "Achievements & Perks")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = locale.getString("back", "Back"))
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
                ExpressiveMilestoneBanner(
                    visible = showUnlockCelebration,
                    title = locale.getString("achievement_unlocked_title", "Achievement unlocked!"),
                    description = unlockCelebrationText,
                    reducedMotion = state.settings.reducedMotion
                )
            }

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
                        Text(locale.getString("achievement_progress_title", "Achievement Progress"), style = MaterialTheme.typography.titleMedium)
                        val unlockedCount = achievements.count { state.achievements.contains(it.id) }
                        Text(
                            text = locale.getString("achievement_unlocked_format", "Unlocked: %d / %d", unlockedCount, achievements.size),
                            fontSize = 16.sp,
                            color = MoneyGold
                        )
                        val targetProgress = if (achievements.isEmpty()) 0f else unlockedCount.toFloat() / achievements.size
                        val animatedProgress by animateFloatAsState(
                            targetValue = targetProgress,
                            animationSpec = if (state.settings.reducedMotion) snap() else tween(550),
                            label = "achievementProgress"
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Text(
                            text = locale.getString("achievement_progress_desc", "Unlocking achievements grants permanent Nebula rewards and passive perks!"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(achievements, key = { it.id }) { achievement ->
                val isUnlocked = state.achievements.contains(achievement.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (state.settings.reducedMotion) Modifier
                            else Modifier.animateItem(
                                placementSpec = spring(
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                                )
                            )
                        )
                        .animateContentSize(),
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
                            Text(locale.getString("reward_format", "Reward: +%s Nebula", achievement.rewardNebula), fontSize = 12.sp, color = MoneyGold)
                            Text(
                                text = locale.getString(
                                    "achievement_tier_format",
                                    "Tier: %s",
                                    locale.getString("achievement_tier_" + achievement.tier, achievement.tier.replaceFirstChar { it.uppercase() })
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
