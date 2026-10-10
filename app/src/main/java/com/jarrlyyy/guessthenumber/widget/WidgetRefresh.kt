package com.jarrlyyy.guessthenumber.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.jarrlyyy.guessthenumber.data.store.MAX_SAVE_SLOTS
import com.jarrlyyy.guessthenumber.data.store.SaveManager

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


/** Advances to the next populated save slot, skipping empty slots. */
suspend fun nextOccupiedSaveSlot(context: Context, currentSlot: Int): Int {
    val saveManager = SaveManager(context)
    val current = currentSlot.coerceIn(1, MAX_SAVE_SLOTS)
    for (offset in 1..MAX_SAVE_SLOTS) {
        val candidate = ((current - 1 + offset) % MAX_SAVE_SLOTS) + 1
        if (!saveManager.getSlotMetadata(candidate).isEmpty) return candidate
    }
    return current
}
