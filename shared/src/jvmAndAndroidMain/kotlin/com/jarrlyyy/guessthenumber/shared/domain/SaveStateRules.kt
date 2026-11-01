package com.jarrlyyy.guessthenumber.shared.domain

import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.GameState
import java.math.BigDecimal

/**
 * Canonical validation for the persisted GameState contract.
 * Android DataStore and desktop file persistence must reject the same invalid game data.
 */
object SaveStateRules {
    fun validate(state: GameState): Boolean =
        Difficulty.isValid(state.difficultyId) &&
            state.money.value >= BigDecimal.ZERO &&
            state.prestige.value >= BigDecimal.ZERO &&
            state.ultra.value >= BigDecimal.ZERO &&
            state.nebula.value >= BigDecimal.ZERO &&
            state.currentRangeMin >= 1L &&
            state.currentRangeMin <= state.currentRangeMax &&
            state.targetNumber in state.currentRangeMin..state.currentRangeMax &&
            state.attempts >= 0L &&
            state.correctGuesses >= 0L &&
            state.streak >= 0 &&
            state.bestStreak >= 0 &&
            state.statistics.totalGuesses >= 0L &&
            state.statistics.correctGuesses >= 0L &&
            state.statistics.failedGuesses >= 0L &&
            state.statistics.moneyEarned.value >= BigDecimal.ZERO &&
            state.statistics.moneySpent.value >= BigDecimal.ZERO &&
            state.statistics.prestigesCount >= 0L &&
            state.statistics.ultrasCount >= 0L &&
            state.statistics.nebulaEarned >= 0L &&
            state.statistics.playtimeSeconds >= 0L &&
            state.statistics.arcadePlayed >= 0L &&
            state.statistics.randomEventsTriggered >= 0L &&
            state.statistics.arcadeBestScores.values.all { it >= 0 } &&
            state.upgradeLevels.values.all { it >= 0 } &&
            state.prestigeUpgradeLevels.values.all { it >= 0 } &&
            state.ultraUpgradeLevels.values.all { it >= 0 } &&
            state.liveOpsClaims.values.all { it >= 0 } &&
            state.permanentEventBoosts >= 0 &&
            state.dailyQuestGuesses >= 0 &&
            state.dailyQuestCorrectGuesses >= 0 &&
            state.dailyQuestBestStreak >= 0 &&
            state.dailyQuestUpgradePurchases >= 0 &&
            state.lootCommonChests >= 0 &&
            state.lootRareChests >= 0 &&
            state.lootEpicChests >= 0 &&
            state.lootDropsFound >= 0L &&
            state.lootChestsOpened >= 0L &&
            state.endlessRiftTier >= 0 &&
            state.endlessRiftBestTier >= 0 &&
            state.lastSaveTimestamp > 0L
}
