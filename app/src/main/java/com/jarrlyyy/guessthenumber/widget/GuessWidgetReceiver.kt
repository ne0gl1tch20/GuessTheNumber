package com.jarrlyyy.guessthenumber.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.*
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import com.jarrlyyy.guessthenumber.data.store.MAX_SAVE_SLOTS
import com.jarrlyyy.guessthenumber.data.repository.LocaleManager
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import kotlin.random.Random

private val widgetSaveSlotKey = intPreferencesKey("widget_selected_save_slot")

class GuessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GuessWidget()
}

class GuessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val saveManager = SaveManager(context)
        val widgetPreferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val selectedSlot = (widgetPreferences[widgetSaveSlotKey] ?: saveManager.getActiveSlot()).coerceIn(1, MAX_SAVE_SLOTS)
        val state = saveManager.loadGame(selectedSlot)
        val locale = LocaleManager(context)
        locale.loadLocaleForTag(state.settings.locale)
        provideContent {
            val prefs = currentState<Preferences>()
            val slot = (prefs[widgetSaveSlotKey] ?: selectedSlot).coerceIn(1, MAX_SAVE_SLOTS)
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background).padding(12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Text(locale.getString("widget_title", "🎯 Guess The Number"), style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground))
                    Text(locale.getString("widget_slot", "Save slot %d • %s", slot, state.profileName.ifBlank { state.difficultyId }), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    Spacer(GlanceModifier.height(6.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.Start) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(locale.getString("widget_money", "Money"), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                            Text(state.money.format(), style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary))
                        }
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(locale.getString("widget_range", "Range"), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                            Text("${state.currentRangeMin}–${state.currentRangeMax}", style = TextStyle(color = GlanceTheme.colors.onBackground))
                        }
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.Start) {
                        Text(locale.getString("widget_streak", "Streak %d", state.streak), modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                        Text(locale.getString("widget_mastery", "Mastery %d", state.worldMasteryLevels.values.sum()), modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    }
                    Text(locale.getString("widget_worlds_bosses", "Worlds %d • Bosses %d", state.unlockedWorldIds.size, state.defeatedBossIds.size), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    Spacer(GlanceModifier.height(8.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                        Button(text = locale.getString("widget_quick_guess", "Quick Guess"), onClick = actionRunCallback<QuickGuessActionCallback>(), modifier = GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(8.dp))
                        Button(text = locale.getString("widget_next_slot", "Next Slot"), onClick = actionRunCallback<CycleWidgetSaveSlotCallback>(), modifier = GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }
}

class QuickGuessActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val saveManager = SaveManager(context)
        val preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val slot = (preferences[widgetSaveSlotKey] ?: saveManager.getActiveSlot()).coerceIn(1, MAX_SAVE_SLOTS)
        val engine = GameEngine()
        saveManager.updateGameAtomically(slot) { state ->
            val after = engine.processGuess(state, state.targetNumber).newState
            val earned = after.money - state.money
            if (earned <= BigNumber.ZERO) return@updateGameAtomically after
            var bonusMoney = BigNumber.ZERO
            if ("talent_reward_1" in state.prestigeShopPurchases) bonusMoney += earned * BigNumber(0.5)
            val mastery = (state.worldMasteryLevels[state.activeWorldId] ?: 0).coerceIn(0, 10)
            if (mastery > 0) bonusMoney += earned * BigNumber(mastery * 0.02)
            val criticalChance = when {
                "talent_crit_2" in state.prestigeShopPurchases -> 0.15
                "talent_crit_1" in state.prestigeShopPurchases -> 0.05
                else -> 0.0
            }
            if (criticalChance > 0.0 && Random.nextDouble() < criticalChance) bonusMoney += earned
            if ("verdant_guardian" in state.equippedRelicIds) bonusMoney += earned * BigNumber(0.10)
            if ("crystal_golem" in state.equippedRelicIds && Random.nextDouble() < 0.10) bonusMoney += earned * BigNumber(0.5)
            if ("ember_dragon" in state.equippedRelicIds) bonusMoney += earned * BigNumber(0.25)
            if ("nebula_titan" in state.equippedRelicIds) bonusMoney += earned * BigNumber(0.25)
            if (state.equippedRelicIds.size >= 2) bonusMoney += earned * BigNumber(0.10)
            if (state.homeBaseLevel > 0) bonusMoney += earned * BigNumber((state.homeBaseLevel * 0.02).coerceAtMost(0.40))
            val secretForWorld = mapOf(
                "verdant_grove" to "whispering_hollow",
                "crystal_caverns" to "shard_archive",
                "ember_summit" to "ashen_vault",
                "nebula_rift" to "lost_observatory"
            )[state.activeWorldId]
            if (secretForWorld != null && secretForWorld in state.discoveredSecretIds) bonusMoney += earned * BigNumber(0.05)
            val bonusNebula = (if ("talent_master_1" in state.prestigeShopPurchases) 1L else 0L) +
                (if ("ember_dragon" in state.equippedRelicIds) 1L else 0L)
            after.copy(
                money = after.money + bonusMoney,
                nebula = after.nebula + BigNumber(bonusNebula),
                statistics = after.statistics.copy(
                    moneyEarned = after.statistics.moneyEarned + bonusMoney,
                    nebulaEarned = after.statistics.nebulaEarned + bonusNebula
                )
            )
        } ?: return
        GuessWidget().update(context, glanceId)
    }
}

class CycleWidgetSaveSlotCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val saveManager = SaveManager(context)
        val activeSlot = saveManager.getActiveSlot()
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            val current = (preferences[widgetSaveSlotKey] ?: activeSlot).coerceIn(1, MAX_SAVE_SLOTS)
            preferences[widgetSaveSlotKey] = if (current >= MAX_SAVE_SLOTS) 1 else current + 1
        }
        GuessWidget().update(context, glanceId)
    }
}
