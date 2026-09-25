package com.jarrlyyy.guessthenumber.data.crash

import java.io.File
import java.util.UUID
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel

object AppErrorHandler {
    private var crashMarkerFile: File? = null
    private var crashLogFile: File? = null

    fun init(filesDir: File) {
        crashMarkerFile = File(filesDir, "crash_marker.json")
        crashLogFile = File(filesDir, "crash_log.txt")

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleFatalCrash(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun handleFatalCrash(throwable: Throwable) {
        val correlationId = UUID.randomUUID().toString()
        val crashDetails = buildString {
            append("Timestamp: ${System.currentTimeMillis()}\n")
            append("Correlation ID: $correlationId\n")
            append("Message: ${throwable.localizedMessage}\n")
            append("Stacktrace:\n${throwable.stackTraceToString()}\n")
        }
        GameLogger.log(
            LogLevel.FATAL,
            LoggerCategory.CRASH,
            "UNCAUGHT_EXCEPTION",
            "Fatal crash: ${throwable.message}\n${throwable.stackTraceToString()}",
            correlationId
        )
        try {
            crashMarkerFile?.writeText(
                "{\"timestamp\":${System.currentTimeMillis()},\"error\":\"${throwable.localizedMessage}\",\"cid\":\"$correlationId\"}"
            )
            crashLogFile?.writeText(crashDetails)
        } catch (_: Exception) {}
    }

    fun hasPreviousCrash(): Boolean {
        return crashMarkerFile?.exists() == true
    }

    fun getCrashLog(): String {
        return try {
            crashLogFile?.takeIf { it.exists() }?.readText() ?: "No crash log recorded."
        } catch (e: Exception) {
            "Failed to read crash log: ${e.message}"
        }
    }

    fun clearCrashMarker() {
        try {
            crashMarkerFile?.delete()
            crashLogFile?.delete()
        } catch (_: Exception) {}
    }
}
