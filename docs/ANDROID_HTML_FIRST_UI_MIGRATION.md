# Android HTML-first UI migration

## Decision

The Android player-facing UI is migrating to the shared HTML/CSS/JS surface. Kotlin remains authoritative for game rules, progression, economy, saves, validation, and side effects. Compose remains for native/system surfaces and as a reversible fallback while individual HTML routes reach parity.

## Locked direction

- **Numberverse** is the player-facing brand.
- **The engine decides. The interface celebrates.** is the UI architecture tagline.
- Shared HTML/CSS/JS is the canonical visual layer for player-facing game screens that need Android/Windows parity.
- Android WebView hosts the same web bundle rather than maintaining a second visual implementation in Compose.
- The Kotlin bridge is the only authority for mutations and persisted state.
- No online dependency, analytics, accounts, ads, or remote UI code is introduced by this migration.

## Current milestone

The existing shared four-screen Play surface is now enabled by default on Android through `shared_web_play`. It covers the core player loop and progression/reward presentation while retaining the existing Compose navigation and fallback around it.

## Migration order

1. Core Play surface — enabled.
2. Upgrade, Shop, and More shell surfaces — migrate to the same HTML host and bridge contracts.
3. Player-facing progression screens — World Map, Prestige, Ultra, Home Base, Relics, Codex, Endgame, and related reward surfaces.
4. Utility/player surfaces — Stats, Arcade, Music, Settings, Save Slots, Changelog, Live Ops, Achievements, and other non-developer screens.
5. Developer-only screens remain native until there is a concrete reason to share them.

Each phase must preserve the existing Kotlin behavior and save compatibility before the corresponding Compose route is retired.

## Visual rule

HTML is not a simplified fallback UI. It should reproduce the Android player experience as a first-class game interface: typography, spacing, icons, motion, responsive layout, touch targets, reduced-motion behavior, and feedback should be deliberately matched.
