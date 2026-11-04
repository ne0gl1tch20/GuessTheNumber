# Seasonal Calendar and Asset-Driven Game Themes

**Status:** Planning only. This document expands the existing seasonal-event work in [Joy Phase Roadmap](./JOY_PHASE_ROADMAP.md), especially PRs #67 (Live Events Engine), #69 (Seasonal events and collectibles), #70 (Events Hub and calendar), #72 (Developer event simulator and preview tools), and #74 (Release and event readiness gate). It does not create a second event engine or change the planned PR count.

## Product direction

Seasons should visibly transform the real GuessTheNumber game using GTN's own packaged assets. They should feel like part of the game, not a generic banner or a temporary CSS tint. Reuse existing artwork, icons, backgrounds, decorations, trait assets, and particle effects wherever they fit. Add original seasonal artwork only when the asset audit proves that an appropriate existing asset does not exist.

A season may change the Play screen atmosphere, backgrounds, decorative elements, icons, selected UI accents, and ambient effects. Seasonal visuals must never change guessing rules, difficulty, balances, progression, save data, reward eligibility, or competitive fairness.

## Calendar and automatic activation

The seasonal calendar is date-driven and activates automatically without requiring a player to claim or manually enable an event.

Initial calendar candidates:

| Season / event | Target window or rule | Notes |
|---|---|---|
| New Year | December 26 – January 7 | Crosses the year boundary; test both sides of New Year. |
| Valentine's Day | February 7 – February 15 | Short themed presentation. |
| Lunar New Year | Lunar-calendar date range, supplied by validated calendar data | Do not approximate it with a fixed Gregorian date. |
| Easter | Easter weekend, calculated for the applicable year | Use a tested Gregorian Easter calculation and define the exact start/end timezone behavior. |
| Philippine summer | March – May, configurable | Optional regional theme, not a claim that every region has the same seasons. |
| Philippine Independence Day | June 12 | Optional short national-day theme; keep regional/cultural presentation respectful and configurable. |
| Back to School | June onward by default for the Philippines; configurable by region/calendar | School calendars vary, so allow the configured start/end dates to change without an app release. |
| Halloween | October 15 – November 1 | Includes the lead-in and a short post-event window. |
| Christmas | December 1 – January 6 | Includes the **Let It Snow** Play-screen footer control and snow effect. |
| Winter | Region-dependent | Do not activate a winter/snow theme globally where it does not fit the selected region. |

These are starting product defaults, not immutable dates. Store event windows and regional applicability as versioned, validated data. Support explicit start/end dates for movable events and computed calendar rules where appropriate. Define overlap priority so two simultaneous events do not unpredictably replace each other's assets; allow compatible decorations/effects to layer when safe.

Use the player's configured calendar region and local date for presentation, with a documented fallback when region or timezone information is unavailable. Date calculations must be deterministic and testable around midnight, year boundaries, leap years, daylight-saving transitions, and timezone changes. Do not trust a device clock for reward eligibility or authoritative event claims.

## Asset-first implementation

Before creating seasonal artwork or code, inventory the actual source assets and map reusable candidates to each season. Inspect image dimensions, formats, transparency, animation support, licenses, and current usage. Record which assets are reused, which are unsuitable, and which art gaps remain.

The proposed logical asset organization is:

```text
game_data/
  seasons/
    new_year/
      manifest.json
      backgrounds/
      decorations/
      icons/
      effects/
    valentines/
    lunar_new_year/
    easter/
    philippine_summer/
    independence_day/
    back_to_school/
    halloween/
    christmas/
      backgrounds/
      decorations/
      effects/
        snow/
```

This is a logical layout proposal, not a claim that these directories or files already exist. Integrate it with the current encrypted `game_data` archive/chunk pipeline and its validation/versioning rules. Do not add plaintext duplicate asset trees to the APK or bypass the central archive reader.

Each season manifest should be versioned and data-only. It should identify available asset roles (for example background, decoration, icon, particle/effect configuration), supported display contexts, optional layers, and safe fallbacks. Validate paths, formats, dimensions, sizes, and references before activation. Never execute downloaded scripts or treat event configuration as code.

Asset behavior requirements:

- Reuse suitable assets already in GTN before adding new files.
- Load only assets needed by the active season and current screen.
- Cache validated assets and release unused resources appropriately.
- Preserve the normal game theme as a complete fallback when a season or asset is unavailable, invalid, expired, or unsupported.
- Missing optional decorations/effects must not break the Play screen.
- Do not overwrite or mutate original shared assets.
- Keep seasonal assets compatible with Android and Windows using the same canonical Web Play frontend and asset source.
- Respect reduced-motion and performance settings; provide static or disabled-effect fallbacks.
- Avoid adding large asset packs or dependencies without a demonstrated visual need.

## Christmas: “Let It Snow”

Christmas should have a real snow presentation using GTN's existing particle/effect system where suitable, with a dedicated seasonal effect configuration and a graceful static fallback.

Add a **Let It Snow** control in the Play screen footer. It should:
- Be visible during the Christmas season.
- Toggle the snow effect on/off immediately without navigating away from the game.
- Persist the player's preference across launches, while remaining scoped to the seasonal effect.
- Respect reduced-motion/performance preferences and avoid covering inputs, clues, or important text.
- Default according to the agreed product behavior and provide a clear accessible/localized label and state.
- Never change gameplay, rewards, or save-slot progression.

The footer control should not be permanently exposed outside the Christmas season unless a separate explicit preview/debug mode is active.

## Shared developer preview and simulation

Extend the existing shared Developer Settings experience so Android and Windows can preview the same seasonal presentation. Build on the developer dashboard and event simulator planned in #66 and #72 rather than creating a separate platform-specific simulator.

Debug-only controls should allow developers to:
- Select automatic date-based mode or force a specific season for preview.
- Preview each configured season, including events not currently in season.
- Preview the normal/default theme and missing/invalid-asset fallback.
- Toggle seasonal layers/effects independently where useful.
- Inspect the selected season ID, date window, region, asset-manifest version, and fallback/validation diagnostics.
- Reset the simulator to automatic mode.

Simulation must be isolated from real player state. It must not grant rewards, unlock collectibles, alter event claims, change save slots, or persist a forced season as the player's real calendar state. All simulator UI, routes, commands, and force overrides must be absent or inaccessible in Release builds. Keep the shared preview behavior consistent on Android and Windows.

## Architecture and boundaries

- Reuse the Live Events Engine from #67 for event lifecycle, configuration validation, active/upcoming/ended state, expiry, and offline fallback.
- Keep calendar resolution and authoritative date/event decisions in shared Kotlin/KMP where appropriate; Web Play consumes a validated presentation model and renders it.
- Keep HTML/CSS/JavaScript responsible for presentation and effects, not authoritative event eligibility, progression, save transactions, or reward settlement.
- Use the existing localization system for all event names, descriptions, footer labels, accessibility text, and developer diagnostics.
- Treat event/season configuration as validated data only. No downloaded executable code.
- Bundle a safe default calendar/configuration so offline play and first launch work without a network request.
- Preserve existing saves and reward rules. Collectibles/rewards remain part of #69 and must use the existing idempotent Kotlin-owned reward path.
- Do not make seasonal visuals depend on ads, accounts, analytics, or network availability.

## Suggested delivery sequence

1. **Source asset audit:** inventory current images, icons, traits, effects, and licenses; produce an asset-to-season mapping and identify genuine gaps. No asset generation or code changes before this audit.
2. **Calendar and event integration:** implement validated season definitions and deterministic date resolution by extending #67; add boundary and overlap tests.
3. **Asset presentation layer:** load the active season's validated asset manifest through the existing archive reader and apply the presentation to the shared Web Play surface on Android and Windows.
4. **First themed seasons:** implement a small set using confirmed existing assets first (New Year, Easter, Halloween, and Christmas are the initial candidates); expand the catalog as suitable assets are mapped or created.
5. **Christmas snow:** add the seasonal snow effect and Play footer **Let It Snow** preference/control.
6. **Developer preview:** extend #72 with shared Android/Windows season simulation, isolated preview state, fallback diagnostics, and Release exclusion.
7. **More seasons and regional configuration:** add Valentine's Day, Lunar New Year, Philippine summer/Independence Day/Back to School, and other suitable regional or global events through data/configuration rather than one-off engine code.
8. **Readiness and regression:** extend #74 with asset integrity, localization, date-boundary, overlap, offline fallback, reduced-motion, performance, save/reward isolation, Android/Windows parity, and Release-gating checks.

## Acceptance criteria

- Active seasons are selected correctly from validated calendar data and the configured region/date.
- Movable dates and cross-year windows are covered by deterministic tests.
- The active season changes the actual game presentation using mapped GTN assets, not just an event label or generic overlay.
- Missing, invalid, or unavailable seasonal assets always fall back cleanly to the normal theme.
- Android and Windows render the same canonical seasonal presentation.
- Christmas shows a localized Play-footer **Let It Snow** control and a working, preference-aware snow effect.
- Developer simulation previews all seasons without changing real saves, progression, or rewards and is unavailable in Release.
- All player-facing strings are localized and accessible.
- Offline play, save compatibility, gameplay rules, reward integrity, privacy, and performance remain intact.
- Evidence distinguishes source implementation, automated tests, Android runtime validation, and Windows runtime validation. Do not mark a platform verified without testing it.
- Do not start builds, emulator/device tests, or GitHub Actions unless separately authorized.
