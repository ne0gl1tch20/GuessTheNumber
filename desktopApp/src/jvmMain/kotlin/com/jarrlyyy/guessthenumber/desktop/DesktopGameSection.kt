package com.jarrlyyy.guessthenumber.desktop

/** Selects the Android-parity destination rendered inside the Windows desktop shell. */
enum class DesktopGameSection {
    PLAY,
    UPGRADES,
    SHOP,
    MORE,
    PRESTIGE,
    ULTRA,
    TALENT,
    STATS,
    SETTINGS,
    ACHIEVEMENTS,
    WORLD_MAP,
    MUTATORS,
    SAVE_SLOTS,
    RELICS,
    HOME_BASE,
    CODEX,
    ENDGAME,
    LUCKY_STAKE,
    ARCADE
}


/** Player-facing destinations already implemented by the canonical shared HTML surface. */
internal val DesktopGameSection.sharedHtmlScreen: String?
    get() = when (this) {
        DesktopGameSection.PLAY -> "play"
        DesktopGameSection.UPGRADES -> "upgrades"
        DesktopGameSection.SHOP -> "shop"
        DesktopGameSection.PRESTIGE -> "prestige"
        DesktopGameSection.ULTRA -> "ultra"
        DesktopGameSection.TALENT -> "talent"
        DesktopGameSection.SETTINGS -> "settings"
        DesktopGameSection.ACHIEVEMENTS -> "rewards"
        DesktopGameSection.WORLD_MAP -> "worlds"
        DesktopGameSection.MUTATORS -> "mutators"
        DesktopGameSection.RELICS -> "relics"
        DesktopGameSection.HOME_BASE -> "homebase"
        DesktopGameSection.CODEX -> "codex"
        DesktopGameSection.ENDGAME -> "journey"
        DesktopGameSection.STATS -> "stats"
        DesktopGameSection.ARCADE -> "arcade"
        DesktopGameSection.LUCKY_STAKE -> "stake"
        else -> null
    }
