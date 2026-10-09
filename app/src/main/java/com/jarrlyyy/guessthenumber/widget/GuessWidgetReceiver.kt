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
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine

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
        provideContent {
            val prefs = currentState<Preferences>()
            val slot = (prefs[widgetSaveSlotKey] ?: selectedSlot).coerceIn(1, MAX_SAVE_SLOTS)
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background).padding(12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Text("🎯 Guess The Number", style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground))
                    Text("Save slot \$slot • ${state.profileName.ifBlank { state.difficultyId }}", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    Spacer(GlanceModifier.height(6.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.Start) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text("Money", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                            Text(state.money.format(), style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary))
                        }
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text("Range", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                            Text("${state.currentRangeMin}–${state.currentRangeMax}", style = TextStyle(color = GlanceTheme.colors.onBackground))
                        }
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.Start) {
                        Text("Streak ${state.streak}", modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                        Text("Mastery ${state.worldMasteryLevels.values.sum()}", modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    }
                    Text("Worlds ${state.unlockedWorldIds.size} • Bosses ${state.defeatedBossIds.size}", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    Spacer(GlanceModifier.height(8.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                        Button(text = "Quick Guess", onClick = actionRunCallback<QuickGuessActionCallback>(), modifier = GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(8.dp))
                        Button(text = "Next Slot", onClick = actionRunCallback<CycleWidgetSaveSlotCallback>(), modifier = GlanceModifier.defaultWeight())
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
            engine.processGuess(state, state.targetNumber).newState
        } ?: return
        GuessWidget().update(context, glanceId)
    }
}

class CycleWidgetSaveSlotCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val saveManager = SaveManager(context)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            val current = (preferences[widgetSaveSlotKey] ?: saveManager.getActiveSlot()).coerceIn(1, MAX_SAVE_SLOTS)
            preferences[widgetSaveSlotKey] = if (current >= MAX_SAVE_SLOTS) 1 else current + 1
        }
        GuessWidget().update(context, glanceId)
    }
}
