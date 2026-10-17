# Shared Web UI Migration Matrix

**Status (2026-10-18):** Android player-facing navigation now routes through the shared `web-play/` HTML/CSS/JS surface by default. `NavGraph` keeps Compose only for save-slot boot, crash recovery and other native/system boundaries. Kotlin remains authoritative for state, rules, economy, progression, rewards, persistence and validation.
**Reviewed:** 2026-10-18
**Architecture authority:** [Shared Web UI Architecture](SHARED_WEB_UI_ARCHITECTURE.md)
**Product roadmap:** [Joy Phase Roadmap](../.openai/docs/JOY_PHASE_ROADMAP.md)

## Locked migration scope: all Android player-facing screens

The owner has chosen a complete shared-web UI for Android mobile, with the same bundle as the target visual layer for Windows. Compose is not a permanent second implementation of player-facing screens.

A screen counts as migrated only when its real interactions, localization, loading/empty/error/locked states, accessibility/input behavior and Kotlin bridge actions are represented. Visual mockups do not count.

## Current implementation

| Surface | Status | Evidence / remaining work |
|---|---|---|
| Play / Worlds / Rewards / Journey | **HTML canonical route** | Android source is wired to `ProductionWebPlayScreen`; real guessing, quest, chest and world actions use the Kotlin bridge. Android runtime/IME/accessibility and Windows runtime parity still need verification. |
| Upgrades | **Compose migration pending** | Needs real upgrade data, multiplier selection, purchase actions and authoritative Kotlin transaction results in the shared web bundle. |
| Shop | **Compose migration pending** | Needs real shop inventory, purchase/equip actions and authoritative state. |
| Prestige / Ultra / talents | **Compose migration pending** | Needs real economy transactions and progression state/actions. |
| Worlds / bosses | **HTML canonical route** | Current shared web route renders world state and routes unlock/boss actions to Kotlin. |
| Relics / Home Base / Codex / Endless Rift | **Migration pending** | Requires full player-facing routes and bridge actions. |
| Achievements / Daily quests | **HTML presentation** | Achievement progress and daily quest/chest interactions are already represented in the shared route. |
| Settings / Music / Save management / Statistics / Changelog / Tutorial | **Native/system or migration pending** | Keep OS-owned capabilities native; migrate player-facing presentation and return results through the shared UI. |
| Developer-only surfaces | **Native** | Remain native until a concrete sharing requirement exists. |

## Next cohesive conversion target

The next implementation batch is **Upgrades + Shop + More/player utilities**, built as one shared HTML route with real Kotlin bridge actions. It must reuse the existing authoritative transaction methods rather than recreating economy rules in JavaScript.

The migration does not claim that every destination is already converted. It locks the architecture and records the actual current boundary so Compose does not quietly become a second permanent UI again.
