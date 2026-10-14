package com.jarrlyyy.guessthenumber.domain.model

/** Android compatibility aliases for shared event and LiveOps reward rules. */
typealias RandomEventType = com.jarrlyyy.guessthenumber.shared.domain.RandomEventType
typealias RandomEventResult = com.jarrlyyy.guessthenumber.shared.domain.RandomEventResult
typealias RandomEventEngine = com.jarrlyyy.guessthenumber.shared.domain.RandomEventEngine
typealias LiveOpsClaimDefinition = com.jarrlyyy.guessthenumber.shared.domain.LiveOpsClaimDefinition

object LiveOpsClaimPolicy {
    fun apply(state: GameState, event: LiveOpsClaimDefinition): GameState? =
        com.jarrlyyy.guessthenumber.shared.domain.LiveOpsClaimPolicy.apply(state, event)
}
