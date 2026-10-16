# Android-to-Windows exact-port audit

**Goal:** Windows is the same GuessTheNumber app as Android, implemented with desktop platform APIs. Android source, content, shared rules and assets are authoritative. A screen is not complete based on compilation alone.

## Current baseline

PRs #214 and #215 are merged. PR #215 adds partial implementations for several progression screens, but route coverage does not prove complete screen behavior or runtime parity. PR #217's talent transaction hardening is also merged, and its Android Validation and Windows Desktop CI passed on commit `9162283e57f3c310e59e474b917ff13caa19f0ab`. Current follow-up PR #218 validates talent purchases against the bundled canonical catalog; its CI is pending. The overall port remains incomplete.

## Stage 1: Shell and visual foundation
- [x] Four primary navigation destinations (Play, Upgrades, Shop and More).
- [x] Saved theme mode, custom theme colors and reduced-motion-aware transitions.
- [x] Windows navigation can be positioned left, right, top or bottom and persists locally.
- [ ] Compare Android and Windows screenshots at matching sizes and theme settings.
- [ ] Verify focus, hover, resizing, keyboard navigation and selected/unselected states on a Windows runtime.

## Stage 2: Screen coverage

| Android screen | Windows status |
| --- | --- |
| Play, Upgrades, Shop | Partial implementations; exact visual and interaction parity unverified |
| More | Links to the existing screens and the remaining parity routes |
| Prestige, Ultra, Talent Web, Statistics | Prestige/Ultra/Statistics remain partial; Talent Web now has a connected, selectable six-node tree with zoom/pan and parent-gated purchases. Exact visual/runtime parity remains unverified |
| Settings & Save Management | First pass exists; localization and OS notification delivery remain incomplete |
| About | Windows update page exists; Android content parity incomplete |
| Achievements | First functional port: canonical Android catalog, saved unlock state, progress count, rewards/tier display and All/Unlocked/Locked filters. CI and Windows runtime parity verification pending |
| World Map | Canonical world catalog, saved unlock state, active selection, unlock actions, boss progress, secret thresholds, shared-engine start/leave, guess-driven damage/miss regression and victory rewards; interactive map canvas and runtime parity pending |
| Mutators | Shared-engine toggles, challenge builder and custom/weekly challenge reward claims; localization/runtime parity pending |\n| Save Slots | Ten local slots, persisted active slot, create, switch, duplicate and backup restore using atomic JSON files; import/export and slot reset pending |\n| Relics | Catalog-backed collection and set-bonus preview; claiming requires the matching boss victory, equip/unequip/ownership/four-slot limits now use shared transactions with focused tests; runtime parity pending |
| Home Base | Functional level/building screen; shared upgrade transaction, saved level, Android cost progression, building milestones and correct-guess reward bonus connected; localization/runtime parity unverified |
| Codex | Functional 12-entry Explorer's Codex using Android's exact world/boss/secret IDs, saved discovery state, completion progress and one-time 100 Nebula reward; runtime/localization parity pending |
| Endgame | Functional six-goal tracker for bosses, secrets, relics, sanctuary level, Endless Rift tier and mastery across all worlds; World Map shortcut added; runtime/localization parity pending |
| Lucky Stake | Production screen with Android's 50/50 flip, positive/affordable stake validation, streak-based payout multiplier and shared saved settlement; localization and runtime parity pending |
| Developer Settings, Developer Console, Process Inspector | Navigation routes added; diagnostics/command behavior is still missing |
| Music Player, Arcade, Activity Hub, Lua Testing Hub, Live Ops | Navigation routes added; feature-specific behavior is still missing |
| Changelog | Ported to a Windows scrollable Markdown viewer using the canonical Android `changelogs.md` asset; automated resource test added |
| Crash Recovery | Windows boot/save recovery parity is not verified |
| Guided Tutorial / Feature Tour | Android-equivalent overlays are not ported |

## Stage 3: Behavior and shared rules
- [x] Changelog viewer reuses the canonical Android changelog asset instead of maintaining a duplicate Windows copy.
- [ ] Reuse shared domain rules, repositories, catalog data, localization resources and assets wherever possible.
- [ ] Match Android actions, purchases, rewards, unlocks, resets, settings, confirmations and validation.
- [ ] Preserve offline behavior, save compatibility and recovery behavior, with no ads, analytics or accounts.
- [ ] Gate development and Live Ops features using equivalent flags and sandbox validation.
- [ ] Test persisted state and actions, not just route rendering.

## Stage 4: Polish and verification
- [ ] Match screen motion and reduced-motion behavior, audio controls, feedback and visual effects where supported.
- [ ] Adapt only platform-specific behavior such as mouse/keyboard input, file pickers and native window chrome.
- [ ] Compare screenshots screen by screen and keep a mismatch checklist.
- [ ] Run focused JVM tests and CI; perform Windows runtime checks before claiming runtime parity.
- [ ] Mark a feature verified only when appearance and behavior both have evidence.

## This batch

Adds explicit navigation to the remaining Android screen families and per-screen parity notes so gaps are visible rather than hidden behind the More menu. The Changelog route is now a functional first screen port: it loads the canonical Android Markdown asset, renders headings and list items in a scrollable desktop layout, and has a resource smoke test. The Achievements route loads the canonical Android achievement catalog, reflects the saved unlock set, displays progress, tier and Nebula rewards, and supports All/Unlocked/Locked filters. The World Map now loads the canonical Android world catalog and displays saved world unlocks, active selection, unlock requirements/actions, boss progress and secret thresholds. World unlock, selection and boss battle start/leave reuse shared Windows transactions with Android progression requirements. Guess-driven boss damage, phase-based miss regression and victory rewards now run through shared Kotlin rules; the interactive map canvas and runtime parity remain unfinished. The Mutators screen now has gameplay switches backed by the shared engine transaction, a builder that applies its selected mutator set, and custom/weekly challenge progress and reward claims saved into game state. The Save Slots screen now lists ten local slots, persists the active slot, creates and duplicates saves, switches slots after saving the current state, and restores valid backups. Slot import/export and explicit slot reset remain pending. Home Base now uses the active save state and shared `DesktopEconomyTransactions.upgradeHomeBase` transaction, applies the Android cost curve, and has been visually tightened toward Android's hierarchy with a primary-container Home card, back icon and restored/locked building icons; the existing shared GameEngine continues to process guesses and DesktopEconomyTransactions applies the saved Home Base reward bonus. Focused transaction tests cover affordability, spending, level persistence and the level cap. The candidate also adds a 12-entry Explorer's Codex with saved discovery progress and a one-time 100 Nebula completion reward, a six-goal Endgame tracker, shared relic claim/equip/unequip transactions, boss battle actions and a playable Lucky Stake screen with stake validation and streak-based payout. These are implementation candidates, not verified Android-equivalent ports; localization and Windows runtime parity remain open. Historical note: on an earlier PR #215 head `c2138c0ff8ce2aba8813dd3104a560b489c82575`, Android Validation passed (run 38890096421) while Windows Desktop CI failed (run 38890096399). PR #215 was subsequently merged at a later head; those earlier results must not be presented as validation of its final merged commit. PR #217's later candidate checks passed (runs 38901674190 and 38901674304). PR #218 is the current talent-catalog follow-up, with CI pending. Other routes listed as partial or tracking shells remain unfinished; Windows runtime/screenshot parity is still unverified.


## Latest repository status (2026-10-16)

- [x] PR #215 merged: expanded progression-screen route coverage. Partial implementations are not treated as parity completion.
- [x] PR #217 merged: rejects unknown, discounted, negative-cost and prerequisite-mismatched talent transactions. Android Validation and Windows Desktop CI passed on that PR's exact head.
- [ ] PR #218 open: replaces the duplicated talent-rule map with catalog-driven validation and a test that iterates the bundled talent definitions. Check results must be confirmed for PR #218's exact head before merge.
- [ ] Manual Windows runtime verification remains open for talent effects, screenshot comparisons, localization, responsive/high-DPI behavior and the remaining Stage 2 flows.


### Talent Web source-of-truth follow-up (2026-10-16)

PR #218 makes the Windows Talent Web's prerequisite/parent gating and transaction validation read the bundled talent catalog instead of maintaining separate prerequisite maps. The shared JVM resource was copied from the Android asset, the redundant desktop resource copy was removed, and the JSON validation workflow now enforces Android/shared-resource parity. The desktop tree still uses fallback English display copy aligned with the Android screen's fallback strings; locale-aware Windows rendering and actual Windows interaction/effect checks remain unverified. PR #218 CI must pass on its latest head before merge.


### Latest core-loop parity follow-up (2026-10-16)

- PR #218 merged after Android Validation and Windows Desktop CI passed (runs [38903226288](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38903226288) and [38903226499](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38903226499)). Talent transaction validation and Talent Web parent gating now read the shared JVM catalog; CI enforces equality with Android's asset.
- Current follow-up branch `fix/windows-auto-clicker-speed-parity` moves the Auto Clicker interval formula into shared `GameEngine.autoClickerDelayMillis`, used by both Android and Windows. Tests cover both speed talents, stacked talent speed plus `mut_speed`, inactive state and non-positive speed.
- Windows runtime speed/feedback verification remains open until a current candidate passes CI and is manually exercised.
