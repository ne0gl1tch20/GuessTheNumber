# Shared Web UI Migration Matrix

**Status (2026-10-18):** Android routes the main player-facing surface through the shared `web-play/` HTML/CSS/JS bundle by default. The bundle now includes Play, Worlds, Rewards, Journey, Upgrades and Shop. Windows still renders its player UI with Compose Desktop; the shared HTML bundle is not hosted there yet.

**Reviewed:** 2026-10-18  
**Architecture authority:** [Shared Web UI Architecture](SHARED_WEB_UI_ARCHITECTURE.md)  
**Execution plan:** [Cross-platform migration execution plan](SHARED_WEB_UI_CROSS_PLATFORM_EXECUTION_PLAN.md)  
**Product roadmap:** [Joy Phase Roadmap](../.openai/docs/JOY_PHASE_ROADMAP.md)

## Locked migration scope

All player-facing screens use one shared HTML/CSS/JavaScript presentation bundle on Android and Windows. Kotlin/KMP remains authoritative for game rules, economy, progression, rewards, validation and persistence. Compose remains appropriate for native/system boundaries and the Windows desktop shell, not as a permanent duplicate of player-facing game screens.

A screen counts as migrated only when its real interactions, localization, loading/empty/error/locked states, accessibility/input behavior and Kotlin bridge actions are represented. Static mockups and route names do not count.

## Current implementation

| Surface | Android | Windows | Remaining acceptance work |
|---|---|---|---|
| Play / Worlds / Rewards / Journey | Shared HTML route | Shared HTML host for Play; other shared screens remain in the bundle but Windows navigation/parity is incomplete | Wire all intended HTML routes through the desktop bridge and verify localization, keyboard/IME, accessibility and resize behavior |
| Upgrades | Shared HTML route, wired to Kotlin purchase bridge | Shared HTML route, wired to desktop Kotlin transactions | Verify multipliers, purchase results, max-level/affordability states and save/reload on both |
| Shop | Shared HTML route, wired to Kotlin purchase/equip bridge | Shared HTML route, wired to desktop Kotlin transactions | Verify currency, ownership, purchase/equip behavior, error states and save/reload on both |
| Prestige / Ultra / talents | Migration pending or existing native route | Compose UI | Add real shared screens and authoritative transaction actions |
| World Map / bosses | World presentation and bridge actions in shared bundle | Partial Compose implementation | Add complete shared route coverage and verify battle progression/rewards |
| Home Base / Relics / Codex / Endgame / Endless Rift | Migration pending | Partial Compose implementations | Build shared routes and bridge-backed state/actions; preserve transaction and unlock rules |
| Achievements / Daily quests | HTML presentation for progress, quests and chest actions | Partial/route-specific Compose UI | Unify state and interactions through the shared bundle |
| Settings / Music / Save management / Statistics / Changelog / Tutorial | Native/system or migration pending | Partial Compose UI | Migrate player-facing presentation; retain OS-owned capabilities in native adapters |
| Arcade / minigames / Live Ops player surfaces | Migration pending or existing native feature | Partial Compose UI | Share presentation where supported; keep sandboxing, validation and platform integrations native |
| Developer-only surfaces | Native | Native/partial | Remain native unless a concrete cross-platform requirement justifies migration |
| First-run save selection / crash recovery | Native Compose | Desktop-specific recovery not fully verified | Keep native/system-owned; verify recovery and save compatibility on both |

## Completion gates

- [x] One canonical packaged `web-play/` frontend is loaded by Android and by the Windows host for Play, Upgrades and Shop. No separate Windows copy of the UI.
- [ ] Expand the Windows host to all intended shared HTML routes; document runtime/distribution behavior and add lifecycle/origin tests.
- [ ] All player-facing routes listed above have real state, interactions and localized loading/empty/error/locked states.
- [ ] Host bridges use a strict versioned allow-list. JavaScript cannot directly write saves or decide costs/rewards.
- [ ] Existing saves, offline behavior, recovery and no-ads/no-analytics/no-accounts requirements are preserved.
- [ ] Android and Windows builds/tests pass on the exact candidate commit.
- [ ] Runtime interaction, keyboard/IME, accessibility, resize, persistence and screenshot parity are checked separately from CI.
- [ ] This matrix is updated with evidence rather than treating route coverage or compilation as parity.

## Current milestone

PR #256 added Upgrades and Shop to Android. The Windows host implementation now renders Play, Upgrades and Shop from the same bundle and routes core gameplay, upgrade/shop transactions, multiplier changes, daily quest claims and loot chest rewards through Kotlin. This is a first migration batch, not full UI parity. Remaining Windows routes, other Android screens, CI and runtime verification are tracked in the [cross-platform execution plan](SHARED_WEB_UI_CROSS_PLATFORM_EXECUTION_PLAN.md).