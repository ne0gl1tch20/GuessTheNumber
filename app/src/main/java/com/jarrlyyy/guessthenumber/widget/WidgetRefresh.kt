package com.jarrlyyy.guessthenumber.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounces gameplay save bursts so both widget layouts refresh using the newest persisted state.
 */
object WidgetRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var refreshJob: Job? = null

    fun request(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            refreshJob?.cancel()
            refreshJob = scope.launch {
                delay(350L)
                runCatching {
                    GuessWidget().updateAll(appContext)
                    CompactGuessWidget().updateAll(appContext)
                }
            }
        }
    }
}
