package com.jarrlyyy.guessthenumber.data.logger

import kotlinx.serialization.Serializable

@Serializable
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val category: LoggerCategory,
    val event: String,
    val message: String,
    val correlationId: String? = null
)

enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL
}

enum class LoggerCategory {
    GAME, ECONOMY, GUESS, UPGRADE, PRESTIGE, ULTRA, NEBULA, SHOP, ARCADE,
    AUTOCLICKER, SAVE, JSON, COMMAND, STATE, NAVIGATION, UI, PERFORMANCE,
    ERROR, CRASH, TEST
}
