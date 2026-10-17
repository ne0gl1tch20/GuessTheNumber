# Android HTML-first UI: locked direction

The Android player-facing UI is now treated as an HTML/CSS/JS-first product surface.

## Locked rules

- **Numberverse** is the player-facing brand.
- **The engine decides. The interface celebrates.** is the architecture tagline.
- Player-facing Android screens use the shared web bundle as the canonical visual implementation.
- Kotlin/KMP remains authoritative for game rules, progression, economy, rewards, saves, validation and mutations.
- Compose remains only for native/system surfaces and a reversible fallback while a route is being migrated and verified.
- The same web bundle is the target visual layer for Windows.
- No remote UI, analytics, accounts, ads or online dependency is introduced by this migration.

## Migration contract

A Compose screen is retired only after its real interactions, localization, save behavior and host lifecycle behavior have been represented by the shared web route. Placeholder HTML does not count as a migration.

The current Play/Worlds/Rewards/Journey route is the first canonical HTML surface. The next cohesive migration work covers Upgrades, Shop, More and their real Kotlin-backed actions, followed by the remaining progression and player utility surfaces.

## Visual identity

The shared `web-play` bundle uses local assets for branded icons. The sparkle/star mark is represented by `icons/sparkle.svg` rather than a platform emoji so Android and Windows render the same icon shape.
