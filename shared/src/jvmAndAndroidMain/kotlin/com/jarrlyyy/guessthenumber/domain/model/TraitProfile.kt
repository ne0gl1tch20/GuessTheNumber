package com.jarrlyyy.guessthenumber.domain.model

import kotlinx.serialization.Serializable

/**
 * Persistent but invisible memory used by the adaptive trait system.
 *
 * This is deliberately separate from the normal progression fields. Legacy saves can omit
 * the object entirely and receive these defaults through kotlinx.serialization.
 */
@Serializable
data class TraitProfile(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val totalGuessesObserved: Long = 0L,
    val correctGuessesObserved: Long = 0L,
    val failedGuessesObserved: Long = 0L,
    val closeMissesObserved: Long = 0L,
    val longStreaksObserved: Long = 0L,
    val sessionsObserved: Long = 0L,
    val prestigesObserved: Long = 0L,
    val ultrasObserved: Long = 0L,
    val upgradesPurchasedObserved: Long = 0L,
    val moneyEarnedObserved: BigNumber = BigNumber.ZERO,
    val moneySpentObserved: BigNumber = BigNumber.ZERO,
    val consistencyScore: Double = 0.0,
    val accuracyScore: Double = 0.0,
    val riskScore: Double = 0.0,
    val persistenceScore: Double = 0.0,
    val progressionScore: Double = 0.0,
    val helperUses: Map<String, Long> = emptyMap(),
    val helperCooldowns: Map<String, Long> = emptyMap(),
    val awardedBonuses: Set<String> = emptySet()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
