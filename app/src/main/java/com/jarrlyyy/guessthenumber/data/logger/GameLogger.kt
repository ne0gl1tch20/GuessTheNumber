package com.jarrlyyy.guessthenumber.data.logger

import android.util.Log
import com.jarrlyyy.guessthenumber.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/** Central logging boundary: verbose traces are opt-in in debug builds. */
object GameLogger {
    private const val TAG = "GuessTheNumberGame"
    private val logs = ConcurrentLinkedQueue<LogEntry>()
    private val mutableLogFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logFlow: StateFlow<List<LogEntry>> = mutableLogFlow.asStateFlow()

    @Volatile private var paused = false
    @Volatile private var verboseEnabled = false
    @Volatile private var logStorageFile: File? = null
    @Volatile private var saveToStorageEnabled = false
    private const val MAX_LOGS = 1000

    fun configureVerbose(enabled: Boolean) {
        verboseEnabled = BuildConfig.DEBUG && enabled
        log(LogLevel.INFO, LoggerCategory.STATE, "VERBOSE_LOGGING_CONFIGURED",
            "Verbose diagnostics ${if (verboseEnabled) "enabled" else "disabled"}.")
    }

    fun configureStorage(file: File, enabled: Boolean) {
        logStorageFile = file
        saveToStorageEnabled = BuildConfig.DEBUG && enabled
    }

    fun formatTimestamp(timestamp: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    fun log(
        level: LogLevel,
        category: LoggerCategory,
        event: String,
        message: String,
        correlationId: String? = null
    ) {
        if (paused) return
        if (!BuildConfig.DEBUG && level !in setOf(LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL)) return
        if (BuildConfig.DEBUG && !verboseEnabled && level in setOf(LogLevel.TRACE, LogLevel.DEBUG)) return

        val now = System.currentTimeMillis()
        logs.add(LogEntry(now, level, category, event, message, correlationId))
        while (logs.size > MAX_LOGS) logs.poll()
        mutableLogFlow.value = logs.toList()

        val line = buildString {
            append("[")
            append(formatTimestamp(now))
            append("][")
            append(category.name)
            append("][")
            append(level.name)
            append("][")
            append(event)
            append("] ")
            append(message)
            correlationId?.let { append(" (CID: ").append(it).append(")") }
        }

        if (saveToStorageEnabled) {
            try {
                logStorageFile?.appendText("$line\n")
            } catch (error: Exception) {
                Log.w(TAG, "Could not persist diagnostic log (${error.javaClass.simpleName}).")
            }
        }

        when (level) {
            LogLevel.TRACE, LogLevel.DEBUG -> Log.d(TAG, line)
            LogLevel.INFO -> Log.i(TAG, line)
            LogLevel.WARN -> Log.w(TAG, line)
            LogLevel.ERROR, LogLevel.FATAL -> Log.e(TAG, line)
        }
    }

    fun pause() { paused = true }
    fun resume() { paused = false }

    fun clear() {
        logs.clear()
        mutableLogFlow.value = emptyList()
    }

    fun exportLogs(): String = logs.joinToString("\n") { entry ->
        buildString {
            append("[")
            append(formatTimestamp(entry.timestamp))
            append("][")
            append(entry.category.name)
            append("][")
            append(entry.level.name)
            append("][")
            append(entry.event)
            append("] ")
            append(entry.message)
            entry.correlationId?.let { append(" (CID: ").append(it).append(")") }
        }
    }
}
