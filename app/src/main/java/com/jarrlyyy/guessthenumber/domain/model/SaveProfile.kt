package com.jarrlyyy.guessthenumber.domain.model

import com.jarrlyyy.guessthenumber.shared.domain.SaveProfileRules

object SaveProfile {
    const val DEFAULT_ICON = SaveProfileRules.DEFAULT_ICON

    val iconIds: List<String> = SaveProfileRules.iconIds

    fun isValidIcon(id: String): Boolean = SaveProfileRules.isValidIcon(id)

    fun iconSymbol(id: String): String = SaveProfileRules.iconSymbol(id)
}
