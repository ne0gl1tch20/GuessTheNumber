package com.jarrlyyy.guessthenumber.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import com.jarrlyyy.guessthenumber.domain.engine.GameEngine

class GuessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GuessWidget()
}

class GuessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val saveManager = SaveManager(context)
        val state = saveManager.loadGame()
        provideContent {
            GlanceTheme {
                val money = state.money.format()
                val range = "${state.currentRangeMin} - ${state.currentRangeMax}"
                val streak = state.streak.toString()
                val mastery = state.worldMasteryLevels.values.sum().toString()
                val worlds = state.unlockedWorldIds.size.toString()
                val bosses = state.defeatedBossIds.size.toString()
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background).padding(12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text("🎯 Guess The Number", style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground))
                    Spacer(GlanceModifier.height(4.dp))
                    Text("Money: $money", style = TextStyle(color = GlanceTheme.colors.primary))
                    Text("Range: $range", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    Text("Streak: $streak  •  Mastery: $mastery", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    Text("Worlds: $worlds  •  Bosses: $bosses", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    Spacer(GlanceModifier.height(8.dp))
                    Button(text = "Quick Guess", onClick = actionRunCallback<QuickGuessActionCallback>())
                }
            }
        }
    }
}

class QuickGuessActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val saveManager = SaveManager(context)
        val engine = GameEngine()
        val updated = saveManager.updateGameAtomically { state -> engine.processGuess(state, state.targetNumber).newState } ?: return
        GuessWidget().update(context, glanceId)
    }
}
