# Shared HTML UI cross-platform migration execution plan

**Owner decision:** Use the same bundled HTML/CSS/JavaScript presentation for player-facing UI on Android and Windows. Kotlin/KMP remains the authority for game state, costs, purchases, progression, rewards, validation and persistence.

**Starting point:** PR #256 merged the Upgrades and Shop route into Android's `app/src/main/assets/web-play/` bundle. Android's `ProductionWebPlayScreen` loads that bundle through Android WebView. The Windows desktop app still renders player screens in Compose Desktop. The Windows host and the full remaining screen migration are not complete.

## Workstream A: Windows shared HTML host

1. Evaluate and select a maintained Windows embedded browser integration. WebView2 is the preferred candidate, but do not claim it is implemented until a buildable host proves it. Document runtime prerequisites, missing-runtime behavior, licensing, MSI distribution and offline operation before changing installer behavior.
2. Build a small isolated host spike that loads the packaged `web-play/index.html` with network access unnecessary, supports resize/keyboard/focus and reliably disposes browser resources.
3. Implement the same strict versioned message contract as Android, with exact local-page/origin checks, bounded payloads, request ID deduplication and an explicit action allow-list. Never expose a generic Kotlin method dispatcher or allow JavaScript to mutate saves directly.
4. Adapt platform-specific input and lifecycle behavior without maintaining a second HTML/CSS/JS copy.
5. Add automated bridge and resource tests, then verify the app on Windows. Do not replace the existing Compose routes until the host is stable and the required bridge actions exist.

## Workstream B: complete the shared player-facing screens

Migrate in cohesive, testable batches, reusing canonical catalogs and existing Kotlin transactions:
1. Prestige, Ultra and talent progression.
2. Home Base, Relics, Codex, Endgame and remaining world/boss details.
3. Settings, statistics, achievements, changelog, tutorial and save-management presentation.
4. Arcade/minigames and player-facing Live Ops surfaces where platform contracts allow sharing.

Keep developer tools and OS-owned/system surfaces native. Save-slot boot and crash recovery remain native until a shared implementation is justified and parity can be proved.

For every route, include state rendering, localization, loading/empty/error/locked states, accessibility semantics, keyboard/touch/mouse behavior and authoritative bridge responses. A clickable mockup without real transactions is not a completed migration.

## Workstream C: parity and regression verification

For each batch:
- Run Android validation/tests and Windows desktop CI against the exact PR head.
- Add focused tests for bridge parsing, unknown actions/fields, malformed messages, duplicate IDs, invalid purchases, affordability, ownership, progression prerequisites and save compatibility as applicable.
- Compare the same scenarios and responsive sizes on both platforms.
- Manually verify IME/keyboard/focus, resize, screen-reader labels, offline launch, save/reload and lifecycle cleanup where CI cannot establish them.
- Update `docs/SHARED_WEB_UI_MIGRATION_MATRIX.md` with source/test/runtime evidence. Keep CI results distinct from manual runtime verification.

## Non-negotiable constraints

- Preserve offline play and existing save compatibility/recovery.
- No ads, analytics, accounts or remote UI code.
- Kotlin/KMP owns rules, costs, rewards, progression and persistence.
- Share the exact web bundle. Platform adapters may differ; player-facing designs should not fork.
- Do not change the existing Windows MSI UpgradeCode or save locations as part of host experimentation.
- Do not merge a route based on compilation alone if it can be checked through the app.
