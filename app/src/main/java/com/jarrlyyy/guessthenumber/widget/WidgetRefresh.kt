package com.jarrlyyy.guessthenumber.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Coalesces bursts of gameplay saves into one widget refresh.
 * Widgets always read the latest persisted save when Glance renders them.
 */
object WidgetRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastRequestAt = AtomicLong(0L)

    fun request(context: Context) {
        val now = System.currentTimeMillis()
        val previous = lastRequestAt.getAndSet(now)
        if (now - previous < 900L) return
        val appContext = context.applicationContext
        scope.launch {
            delay(350L)
            runCatching { GuessWidget().updateAll(appContext) }
        }
    }
}
