package com.jarrlyyy.guessthenumber.shared.domain

/** Platform-independent save-profile icon validation and display mapping. */
object SaveProfileRules {
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
