# Shared HTML Mobile UI Migration

## Locked decision

The player-facing Android mobile UI is migrating to the shared HTML/CSS/JS surface.

### Ownership

- **HTML/CSS/JS:** player-facing presentation, navigation visuals, interaction feedback, responsive layout, animation and shared Android/Windows presentation.
- **Kotlin/KMP:** authoritative game state, rules, economy, progression, rewards, persistence, validation, recovery and action results.
- **Compose:** native/system/dev-only surfaces and a temporary fallback while an individual player-facing screen is being migrated.

Once a player-facing screen is migrated, its Compose version is no longer the production presentation path.

## Screen migration order

1. Core game loop: Play, guessing, feedback and run state.
2. Progression: Upgrades, Shop, Prestige, Ultra and progression/world surfaces.
3. Player utility: More, Settings, Music and player-facing information.
4. Content surfaces: Achievements, Stats, LiveOps, Arcade and related player features.
5. Edge/recovery surfaces that are suitable for shared presentation.
6. Remove migrated production Compose routes after host and fallback behavior are verified.

Dev-only tools such as Lua testing and process inspection can remain native unless there is a concrete reason to share them.

## Bridge rule

The web UI never becomes the authority for gameplay. It sends typed requests to the Kotlin host and renders the returned authoritative state. Navigation requests may select a web route, but they do not directly mutate game state.

## Platform goal

Android WebView and Windows WebView2 should consume the same player-facing assets. Platform-specific code should be limited to hosting, lifecycle, system integrations and the authoritative bridge.

## Compatibility constraints

- Offline-first behavior remains unchanged.
- No ads, analytics or accounts are introduced by this migration.
- Existing save data and recovery behavior remain authoritative.
- Reduced-motion preferences must be respected by the shared UI.
- Localization remains data-driven and must not be hardcoded into bridge logic.
