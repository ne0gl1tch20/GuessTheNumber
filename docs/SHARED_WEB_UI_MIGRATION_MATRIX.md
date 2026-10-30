# Shared Web UI Migration Matrix

**Status (2026-10-30):** Android and Windows both load the canonical `web-play/` bundle. Windows routes 17 player-facing sections through `DesktopSharedWebView`; Compose remains for the desktop shell and native/system surfaces. This source-level route coverage does not mean every screen has passed runtime or visual parity checks.

**Reviewed:** 2026-10-30  
**Architecture authority:** [Shared Web UI Architecture](SHARED_WEB_UI_ARCHITECTURE.md)  
**Execution plan:** [Cross-platform migration execution plan](SHARED_WEB_UI_CROSS_PLATFORM_EXECUTION_PLAN.md)  
**Product roadmap:** [Joy Phase Roadmap](../.openai/docs/JOY_PHASE_ROADMAP.md)

## Locked migration scope

All player-facing screens use one shared HTML/CSS/JavaScript presentation bundle on Android and Windows. Kotlin/KMP remains authoritative for game rules, economy, progression, rewards, validation and persistence. Compose remains appropriate for native/system boundaries and the Windows desktop shell, not as a permanent duplicate of player-facing game screens.

A screen counts as migrated only when its real interactions, localization, loading/empty/error/locked states, accessibility/input behavior and Kotlin bridge actions are represented. Static mockups and route names do not count.

## Current implementation

| Surface | Android | Windows | Remaining acceptance work |
|---|---|---|---|
| Play / Upgrades / Shop | Canonical shared HTML routes with Kotlin bridge actions | Same routes via `DesktopSharedWebView`; Kotlin desktop transactions remain authoritative | Verify input validation, purchase affordability/max-level/ownership states, rate limiting and save/reload on a current Windows build |
| Prestige / Ultra / Talent | Shared HTML routes and Android production callbacks | Same shared routes; desktop bridge calls shared engine/desktop transactions | Verify reset preservation, six permanent effects, talent prerequisites/effects and large-number cases |
| Mutators / Challenges | Shared HTML route and Kotlin-owned state/actions | Same shared route and desktop mutator/challenge transactions | Exercise every mutator combination, builder gating and daily/weekly claim persistence |
| Worlds / Rewards / Journey / Relics / Home Base / Codex / Statistics / Arcade / Lucky Stake | Shared HTML bundle and Android bridge | Shared HTML routes for `worlds`, `rewards`, `journey`, `relics`, `homebase`, `codex`, `stats`, `arcade` and `stake` | Verify each route's locked/loading/error states, actions, rewards, localization and save/reload; full world/boss/activity scope follows Stage 3 sequencing |
| Settings | Shared HTML route with host-owned settings persistence | Shared HTML route with desktop-owned settings persistence and theme callback | Verify every setting meaning, persistence, theme, locale and desktop-only input adaptations |
| More / Save Slots / Music / Changelog / About / update UI | Native or route-specific Android surfaces | Compose/native desktop surfaces and desktop save adapter | Complete feature-specific parity; Music requires a desktop audio implementation; save import/export/reset and recovery need explicit tests |
| Developer-only surfaces | Native, debug gated | Native/partial, must remain debug gated | Verify production builds cannot expose developer routes or commands |
| First-run save selection / crash recovery | Native Compose + DataStore | Desktop-native save recovery | Preserve compatibility and verify failure/recovery on both platforms |

The 17 shared Windows player routes are locked by `DesktopGameSectionRouteTest`; the desktop host bridge is implemented in `ProductionGuessGameScreen.kt`. A route mapping assertion proves only that a section selects a shared route. It does not prove that every bridge action, state, localization string or runtime interaction is complete.

## Completion gates

- [x] One canonical packaged `web-play/` frontend is loaded by Android and Windows; Windows source maps 17 player-facing routes to the same bundle. No separate Windows copy of the UI.
- [ ] Complete route-by-route Windows bridge, lifecycle/origin, runtime and distribution verification; do not count route mapping alone as completion.
- [ ] All player-facing routes listed above have real state, interactions and localized loading/empty/error/locked states.
- [ ] Host bridges use a strict versioned allow-list. JavaScript cannot directly write saves or decide costs/rewards.
- [ ] Existing saves, offline behavior, recovery and no-ads/no-analytics/no-accounts requirements are preserved.
- [ ] Android and Windows builds/tests pass on the exact candidate commit.
- [ ] Runtime interaction, keyboard/IME, accessibility, resize, persistence and screenshot parity are checked separately from CI.
- [ ] This matrix is updated with evidence rather than treating route coverage or compilation as parity.

## Current milestone

Windows now loads 17 player-facing sections from the same bundle as Android, with Kotlin bridge handlers for gameplay, economy, progression, rewards, settings and supported activities. The Windows route mapping and shared guess rate-limit policy have focused tests. This is not full UI parity: bridge-action coverage, Windows runtime, localization, accessibility, responsive behavior, save recovery and later parity stages remain open. Follow the [Windows Feature Parity Plan](WINDOWS_FEATURE_PARITY_PLAN.md) for stage order and acceptance.