# HTML UI Conversion Lock

**Decision:** Locked on 2026-10-17.

GuessTheNumber's player-facing game UI is now committed to a **single shared HTML/CSS/JavaScript frontend** for both Android and Windows.

## Product rule

- Android mobile player-facing screens use the shared web UI as the canonical presentation layer.
- Windows desktop player-facing screens use the **exact same** HTML/CSS/JavaScript bundle and responsive layout.
- Compose is not a second implementation of the same player-facing game screens.
- Compose/Kotlin remains for native/system surfaces and platform host integration where HTML is not the right layer.
- Kotlin/KMP remains authoritative for game state, rules, economy, progression, rewards, persistence, save recovery and validation.

## Visual identity

- Product UI name: **Numberverse**.
- Core line: **The engine decides. The interface celebrates.**
- Icons are real shared UI assets/SVGs rather than platform-specific text-symbol approximations where an icon is part of the product chrome.
- The frontend is offline-first: no remote fonts, CDN assets, analytics, ads or account dependency.

## Host rule

Android and Windows are hosts for the same frontend, not separate UI projects. Each host supplies only the narrow bridge and lifecycle/system capabilities required by its platform.

The bridge must be versioned, typed, allowlisted, size-bounded and authoritative on the Kotlin side. JavaScript may request actions and render returned view state; it may not decide rewards, balances, eligibility, save mutations or hidden game results.

## Conversion scope

The locked migration covers the player-facing Compose destination set, including:

- Play and the complete core run loop
- Upgrades
- Shop
- Prestige
- Ultra
- Worlds / world map / bosses
- Relics
- Home Base
- Codex
- Endless Rift
- Achievements
- Daily quests
- Arcade/minigames and challenge/event surfaces
- Settings and player-facing utilities
- Music player
- Save slots/import/export
- Statistics
- Changelog
- Tutorial
- Localization-driven player UI

Native/system boundaries remain native when required for permissions, notifications, widgets, file pickers, crash recovery or other OS-owned behavior. Those surfaces must return results to the shared web UI without duplicating game rules.

## Parity rule

A screen is considered converted only when the real interactions, localization, loading/empty/error/locked states, accessibility/input behavior and Kotlin bridge actions are represented in the shared frontend. A visual mockup does not count as migration.

The old Compose implementation may remain temporarily as a rollback path during verification. Once a screen passes its Android and Windows parity gates, its duplicate Compose player UI can be retired.

## Non-goals

This lock does **not** claim that every screen is already converted or that Windows runtime parity is already verified. It locks the destination architecture so future work implements the conversion instead of reopening the Compose-vs-HTML decision.
