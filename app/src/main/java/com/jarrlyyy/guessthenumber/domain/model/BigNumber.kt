package com.jarrlyyy.guessthenumber.domain.model

/**
 * Compatibility alias while Android UI/domain callers migrate to the shared model.
 * The implementation stays in shared jvmMain because it deliberately uses BigDecimal.
 */
typealias BigNumber = com.jarrlyyy.guessthenumber.shared.domain.BigNumber
