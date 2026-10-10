package com.jarrlyyy.guessthenumber.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.toMutablePreferences
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jarrlyyy.guessthenumber.MainActivity
import com.jarrlyyy.guessthenumber.data.repository.LocaleManager
import com.jarrlyyy.guessthenumber.data.store.MAX_SAVE_SLOTS
import com.jarrlyyy.guessthenumber.data.store.SaveManager

private val compactWidgetSaveSlotKey = intPreferencesKey("widget_selected_save_slot")

class CompactGuessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactGuessWidget()
}

class CompactGuessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val saveManager = SaveManager(context)
        val preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val slot = (preferences[compactWidgetSaveSlotKey] ?: saveManager.getActiveSlot()).coerceIn(1, MAX_SAVE_SLOTS)
        val state = saveManager.loadGame(slot)
        val locale = LocaleManager(context)
        locale.loadLocaleForTag(state.settings.locale)
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background).padding(12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Text(locale.getString("widget_title", "🎯 Guess The Number"), style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground))
                    Text(locale.getString("widget_slot", "Save slot %d • %s", slot, state.profileName.ifBlank { state.difficultyId }), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    Spacer(GlanceModifier.height(4.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Column(modifier = GlanceModifier) {
                            Text(locale.getString("widget_money", "Money"), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                            Text(state.money.format(), style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary))
                        }
                        Column(modifier = GlanceModifier) {
                            Text(locale.getString("widget_streak", "Streak %d", state.streak), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                            Text(locale.getString("widget_worlds_bosses", "Worlds %d • Bosses %d", state.unlockedWorldIds.size, state.defeatedBossIds.size), style = TextStyle(color = GlanceTheme.colors.onBackground))
                        }
                    }
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Button(text = locale.getString("play", "Play"), onClick = actionRunCallback<OpenCompactWidgetSaveSlotCallback>(), modifier = GlanceModifier)
                        Spacer(GlanceModifier.width(8.dp))
                        Button(text = locale.getString("widget_next_slot", "Next Slot"), onClick = actionRunCallback<CycleCompactWidgetSaveSlotCallback>(), modifier = GlanceModifier)
                    }
                }
            }
        }
    }
}

class OpenCompactWidgetSaveSlotCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val saveManager = SaveManager(context)
        val widgetState = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val slot = (widgetState[compactWidgetSaveSlotKey] ?: saveManager.getActiveSlot()).coerceIn(1, MAX_SAVE_SLOTS)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_WIDGET_SAVE_SLOT, slot)
        }
        context.startActivity(launchIntent)
    }
}

class CycleCompactWidgetSaveSlotCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val saveManager = SaveManager(context)
        val activeSlot = saveManager.getActiveSlot()
        val widgetState = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val current = (widgetState[compactWidgetSaveSlotKey] ?: activeSlot).coerceIn(1, MAX_SAVE_SLOTS)
        val nextSlot = nextOccupiedSaveSlot(context, current)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[compactWidgetSaveSlotKey] = nextSlot }
        }
        CompactGuessWidget().update(context, glanceId)
    }
}
