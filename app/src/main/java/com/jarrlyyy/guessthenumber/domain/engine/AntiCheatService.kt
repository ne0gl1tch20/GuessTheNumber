package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.rules.GuessRateLimitRules

object AntiCheatService {
    private var lastGuessTimestampNanos: Long? = null

    fun validateGuessRate(): Boolean {
        val now = System.nanoTime()
        if (GuessRateLimitRules.isRateLimited(lastGuessTimestampNanos, now)) {
            GameLogger.log(LogLevel.WARN, LoggerCategory.GAME, "ANTICHEAT_RATE_LIMIT", "Suspicious high-frequency guess attempt blocked.")
            return false
        }
        lastGuessTimestampNanos = now
        return true
    }

    fun validateTimeJump(currentState: GameState): GameState {
        val now = System.currentTimeMillis()
        if (now < currentState.lastSaveTimestamp) {
            GameLogger.log(LogLevel.WARN, LoggerCategory.GAME, "ANTICHEAT_TIME_TRAVEL", "Device clock tampering detected (current time is before last save). Adjusting timestamp.")
            return currentState.copy(lastSaveTimestamp = now)
        }
        return currentState
    }

    fun sanitizeCurrency(state: GameState): GameState {
        val sanitizedMoney = if (state.money.value < java.math.BigDecimal.ZERO) BigNumber.ZERO else state.money
        val sanitizedPrestige = if (state.prestige.value < java.math.BigDecimal.ZERO) BigNumber.ZERO else state.prestige
        val sanitizedUltra = if (state.ultra.value < java.math.BigDecimal.ZERO) BigNumber.ZERO else state.ultra
        val sanitizedNebula = if (state.nebula.value < java.math.BigDecimal.ZERO) BigNumber.ZERO else state.nebula

        return state.copy(
            money = sanitizedMoney,
            prestige = sanitizedPrestige,
            ultra = sanitizedUltra,
            nebula = sanitizedNebula
        )
    }
}
