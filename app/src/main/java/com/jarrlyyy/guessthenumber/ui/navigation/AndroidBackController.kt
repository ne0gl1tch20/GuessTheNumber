package com.jarrlyyy.guessthenumber.ui.navigation

/**
 * Shared bridge between Compose back handling and the HTML WebView surface.
 * The activity owns the actual WebView lookup and JavaScript execution.
 */
object AndroidBackController {
    @Volatile
    var handler: ((onUnhandled: () -> Unit) -> Unit)? = null

    fun handleBack(onUnhandled: () -> Unit) {
        handler?.invoke(onUnhandled) ?: onUnhandled()
    }
}
