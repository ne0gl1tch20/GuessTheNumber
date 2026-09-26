package com.jarrlyyy.guessthenumber.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.jarrlyyy.guessthenumber.domain.model.GameState

class GuessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GuessWidget()
}

class GuessWidget : GlanceAppWidget() {
    val moneyKey = stringPreferencesKey("widget_money")
    val rangeKey = stringPreferencesKey("widget_range")
    val streakKey = stringPreferencesKey("widget_streak")

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val saveManager = SaveManager(context)
        val state = saveManager.loadGame()

        provideContent {
            GlanceTheme {
                val money = currentState(moneyKey) ?: state.money.format()
                val range = currentState(rangeKey) ?: "${state.currentRangeMin} - ${state.currentRangeMax}"
                val streak = currentState(streakKey) ?: state.streak.toString()

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = "🎯 Guess The Number",
                        style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Money: $money",
                        style = TextStyle(color = GlanceTheme.colors.primary)
                    )
                    Text(
                        text = "Range: $range | Streak: $streak",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        Button(
                            text = "Quick Guess",
                            onClick = actionRunCallback<QuickGuessActionCallback>()
                        )
                    }
                }
            }
        }
    }
}

class QuickGuessActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val saveManager = SaveManager(context)
        val state = saveManager.loadGame()
        val engine = GameEngine()
        val guess = state.targetNumber // Guaranteed correct guess for instant widget gratification
        val result = engine.processGuess(state, guess)
        saveManager.saveGame(result.newState)

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[stringPreferencesKey("widget_money")] = result.newState.money.format()
            prefs[stringPreferencesKey("widget_range")] = "${result.newState.currentRangeMin} - ${result.newState.currentRangeMax}"
            prefs[stringPreferencesKey("widget_streak")] = result.newState.streak.toString()
        }
        GuessWidget().update(context, glanceId)
    }
}
