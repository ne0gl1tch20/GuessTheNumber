package com.jarrlyyy.guessthenumber.data.logger

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object GameLogger {
    private const val TAG = "GuessTheNumberGame"
    private val _logs = ConcurrentLinkedQueue<LogEntry>()
    private val _logFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logFlow: StateFlow<List<LogEntry>> = _logFlow.asStateFlow()

    private var isPaused = false
    private const val MAX_LOGS = 1000

    private var logStorageFile: File? = null
    private var saveToStorageEnabled = false

    fun configureStorage(file: File, enabled: Boolean) {
        logStorageFile = file
        saveToStorageEnabled = enabled
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun log(level: LogLevel, category: LoggerCategory, event: String, message: String, correlationId: String? = null) {
        if (isPaused) return
        val now = System.currentTimeMillis()
        val entry = LogEntry(
            timestamp = now,
            level = level,
            category = category,
            event = event,
            message = message,
            correlationId = correlationId
        )
        _logs.add(entry)
        while (_logs.size > MAX_LOGS) {
            _logs.poll()
        }
        _logFlow.value = _logs.toList()

        val timeStr = formatTimestamp(now)
        val logString = "[$timeStr][$category][$level] $message ${correlationId?.let { "(CID: $it)" } ?: ""}"
        
        if (saveToStorageEnabled) {
            try {
                logStorageFile?.appendText("$logString\n")
            } catch (_: Exception) {}
        }

        when (level) {
            LogLevel.TRACE, LogLevel.DEBUG -> Log.d(TAG, logString)
            LogLevel.INFO -> Log.i(TAG, logString)
            LogLevel.WARN -> Log.w(TAG, logString)
            LogLevel.ERROR, LogLevel.FATAL -> Log.e(TAG, logString)
        }
    }

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }
    fun clear() {
        _logs.clear()
        _logFlow.value = emptyList()
    }

    fun exportLogs(): String {
        return _logs.joinToString("\n") {
            "[${formatTimestamp(it.timestamp)}][${it.category}][${it.level}] ${it.message}"
        }
    }
}
