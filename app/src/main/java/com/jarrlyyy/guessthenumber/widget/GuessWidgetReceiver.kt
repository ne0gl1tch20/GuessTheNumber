package com.jarrlyyy.guessthenumber.widget

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jarrlyyy.guessthenumber.data.store.MAX_SAVE_SLOTS
import com.jarrlyyy.guessthenumber.data.repository.LocaleManager
import com.jarrlyyy.guessthenumber.MainActivity
import com.jarrlyyy.guessthenumber.R
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import androidx.glance.unit.ColorProvider

private val widgetSaveSlotKey = intPreferencesKey("widget_selected_save_slot")

class GuessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GuessWidget()
}

/** Polished launcher widget. Values come from the selected persisted save. */
class GuessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val saveManager = SaveManager(context)
        val widgetPreferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val selectedSlot = (widgetPreferences[widgetSaveSlotKey] ?: saveManager.getActiveSlot()).coerceIn(1, MAX_SAVE_SLOTS)
        val state = saveManager.loadGame(selectedSlot)
        val locale = LocaleManager(context)
        locale.loadLocaleForTag(state.settings.locale)
        val title = locale.getString("widget_title", "Guess The Number")
        val titleBitmap = renderWidgetFontText(context, title, "fonts/Manrope-VariableFont_wght.ttf", 150, 18, 14f, widgetTextColor(context, 0xFF202536.toInt(), 0xFFF4F5FA.toInt()), true)
        val balanceBitmap = renderWidgetFontText(context, state.money.format(), "fonts/JetBrainsMono-VariableFont_wght.ttf", 110, 28, 22f, widgetTextColor(context, 0xFF6750A4.toInt(), 0xFFD0BCFF.toInt()), true)
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background)
                        .cornerRadius(22.dp).padding(12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.gtn_app_logo),
                            contentDescription = locale.getString("widget_title", "Guess The Number"),
                            modifier = GlanceModifier.size(30.dp)
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Image(provider = ImageProvider(titleBitmap), contentDescription = title, modifier = GlanceModifier.width(150.dp).height(18.dp))
                            Text(
                                locale.getString("widget_slot", "Save slot %d • %s", selectedSlot, state.profileName.ifBlank { state.difficultyId }),
                                style = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.SansSerif, color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }
                    }
                    Spacer(GlanceModifier.height(7.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Column(
                            modifier = GlanceModifier.defaultWeight().background(GlanceTheme.colors.surface)
                                .cornerRadius(15.dp).padding(horizontal = 11.dp, vertical = 8.dp)
                        ) {
                            Text(locale.getString("widget_money", "Balance"), style = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.SansSerif, color = GlanceTheme.colors.onSurfaceVariant))
                            Image(provider = ImageProvider(balanceBitmap), contentDescription = state.money.format(), modifier = GlanceModifier.width(110.dp).height(28.dp))
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Column(
                            modifier = GlanceModifier.defaultWeight().background(GlanceTheme.colors.surface)
                                .cornerRadius(15.dp).padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(locale.getString("widget_streak", "Streak %d", state.streak), style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, color = GlanceTheme.colors.onSurface))
                            Spacer(GlanceModifier.height(3.dp))
                            Text(locale.getString("widget_worlds_bosses", "Worlds %d • Bosses %d", state.unlockedWorldIds.size, state.defeatedBossIds.size), style = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.SansSerif, color = GlanceTheme.colors.onSurfaceVariant))
                        }
                    }
                    Spacer(GlanceModifier.height(7.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Button(text = locale.getString("play", "Play"), onClick = actionRunCallback<OpenWidgetSaveSlotCallback>(), modifier = GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(8.dp))
                        Button(text = locale.getString("widget_next_slot", "Switch save"), onClick = actionRunCallback<CycleWidgetSaveSlotCallback>(), modifier = GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }
}


class OpenWidgetSaveSlotCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val saveManager = SaveManager(context)
        val widgetState = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val slot = (widgetState[widgetSaveSlotKey] ?: saveManager.getActiveSlot()).coerceIn(1, MAX_SAVE_SLOTS)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_WIDGET_SAVE_SLOT, slot)
        }
        context.startActivity(launchIntent)
    }
}

class CycleWidgetSaveSlotCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val saveManager = SaveManager(context)
        val activeSlot = saveManager.getActiveSlot()
        val widgetState = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val current = (widgetState[widgetSaveSlotKey] ?: activeSlot).coerceIn(1, MAX_SAVE_SLOTS)
        val nextSlot = nextOccupiedSaveSlot(context, current)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences[widgetSaveSlotKey] = nextSlot
        }
        GuessWidget().update(context, glanceId)
    }
}
