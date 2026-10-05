package com.jarrlyyy.guessthenumber.domain.model

object SaveProfile {
    const val DEFAULT_ICON = "default"

    val iconIds: List<String> = listOf(
        DEFAULT_ICON,
        "star",
        "bolt",
        "crown",
        "planet"
    )

    fun isValidIcon(id: String): Boolean = id in iconIds

    fun iconSymbol(id: String): String = when (id) {
        "star" -> "⭐"
        "bolt" -> "⚡"
        "crown" -> "👑"
        "planet" -> "🪐"
        else -> "🎮"
    }
}
