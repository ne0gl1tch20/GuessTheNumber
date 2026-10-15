# Windows Feature Parity Plan

**Status:** Planned; Windows has a desktop target and shared guessing-screen pilot, not the full Android feature set.
**Reviewed:** 2026-10-15
**Depends on:** [KMP Foundation Status](KMP_FOUNDATION_STATUS.md)

## Goal

Bring Windows to the agreed player-facing feature set in deliberate, testable slices. Reuse shared rules and selected UI where helpful, while keeping Windows windowing, input, storage, update and installer behavior in the desktop host.

This replaces the Windows-related phases buried in the temporary KMP migration roadmap.

## Scope rules

- Android is the behavior reference unless a platform-specific difference is approved.
- Preserve game rules, rewards, progression, localization meaning, save compatibility and offline play.
- Do not copy Android APIs into desktop code.
- Keep developer consoles, diagnostics and test helpers debug-only.
- Keep debug and release update channels isolated.
- Preserve existing Windows MSI identity and in-place upgrade behavior; inspect WiX/MSI configuration before changing identifiers.
- Do not add ads, analytics or accounts.

## Delivery sequence

### W0. Inventory and desktop shell

- [ ] Inventory Android destinations, visible features, settings, save operations and debug-only routes against Windows.
- [ ] Update the parity table below before implementation.
- [ ] Confirm desktop navigation, window lifecycle, theme, locale loading and error/loading/empty states.
- [ ] Verify keyboard/mouse navigation, focus order, Enter/Escape, resizing, minimum window size, high-DPI scaling and narrow layouts.

**Exit:** The desktop shell can host the planned feature slices without navigation or windowing blockers.

### W1. Core gameplay loop

- [ ] Guess input, submit behavior, target/range presentation and feedback.
- [ ] Correct/incorrect rewards, attempts, streaks, Frenzy and relevant mutators.
- [ ] Rate limiting, validation, boundary handling and blind-feedback behavior.
- [ ] Quick-buy controls, base upgrades and shop.
- [ ] Persist/reload a run and verify reward/counter consistency.

**Exit:** The full core loop works on Windows and matches Android rules for deterministic cases.

### W2. Economy and resets

- [ ] Prestige and Ultra flows, requirements, confirmation and rewards.
- [ ] Nebula currency, permanent upgrades and talent tree.
- [ ] Large-number display and calculations preserve Android semantics.
- [ ] Test costs, rounding, reset preservation and duplicate reward prevention.

**Exit:** Economy and reset flows preserve progression and do not corrupt or duplicate rewards.

### W3. Progression and collection

- [ ] Achievements and claimable rewards.
- [ ] Daily/weekly challenges and claim-state persistence.
- [ ] Statistics and progression summaries.
- [ ] World map, world unlocks, bosses, relic inventory/sets and mastery.
- [ ] Secret discoveries, Home Base, Codex and Endless Rift.
- [ ] Locked/unlocked/completed states and explanatory requirements.

**Exit:** Player-facing progression systems are accessible and state-consistent on Windows.

### W4. Activities and events

- [ ] Arcade and minigames.
- [ ] Random events, challenge/mutator setup and staking where in agreed release scope.
- [ ] Number Rush and bundled LuaJ activities using existing sandbox/validation boundaries.
- [ ] Offline event availability and reward validation.
- [ ] Missing optional content or audio fails gracefully instead of crashing.

**Exit:** Supported activities work offline, and scripted content cannot bypass Kotlin-owned reward/state validation.

### W5. Player utilities and data

- [ ] Settings, theme, locale and desktop-supported accessibility options.
- [ ] Save slots, slot switching, backup/export/import and recovery appropriate to Windows.
- [ ] Tutorial/feature tours and replay entry points.
- [ ] About, player-facing error states and crash recovery surfaces.
- [ ] Music controls where Windows audio support exists; document intentional differences.
- [ ] Localize all visible strings and accessible labels.

**Exit:** Players can configure, save, restore and understand Windows without Android-only screens.

### W6. Developer tooling

- [ ] Console, state inspection, test helpers and verbose logs are debug-only.
- [ ] Developer commands are not exposed unintentionally in production.
- [ ] Add deterministic tests for common rules and desktop adapters.

**Exit:** Debug tools aid diagnosis and are absent or disabled in production packages.

### W7. Updates, packaging and release readiness

- [ ] Keep debug update discovery/install separate from production releases.
- [ ] Validate artifacts and show clear errors for missing, expired or incompatible downloads.
- [ ] Test clean MSI install, in-place upgrade, repeated upgrade, failed-upgrade recovery, downgrade and uninstall.
- [ ] Preserve the verified MSI UpgradeCode and user-data location; do not change identifiers blindly.
- [ ] Verify saves survive restart and supported upgrades.
- [ ] Align version metadata, locale assets and changelog.

**Exit:** Windows packages install/update predictably and preserve user data.

### W8. Final parity gate

- [ ] Run Windows Desktop CI and required Android validation on the final candidate.
- [ ] Run focused common tests for deterministic rules and state transitions.
- [ ] Execute Windows runtime regression for input, resizing, localization, offline play, persistence, update channels and installer behavior.
- [ ] Verify Android save compatibility after shared model/logic changes.
- [ ] Publish final feature matrix with each row marked Verified on Windows, Intentionally platform-specific, Not implemented or Blocked, with evidence.
- [ ] Fix release-blocking regressions before declaring Windows parity complete.

## Initial feature parity matrix

A shared component or green CI is not proof that a complete feature works end-to-end on Windows.

| Feature area | Current assessment | Evidence needed |
|---|---|---|
| Shared deterministic rules | Shared foundation exists | Common tests and feature parity cases |
| Guessing UI | Shared pilot exists | Runtime input, feedback, reward and persistence regression |
| Upgrades and shop | Full Windows integration not established | Buy/cost/state tests |
| Prestige, Ultra and talents | Full Windows integration not established | Requirement, reset and reward tests |
| Achievements and challenges | Full Windows integration not established | Unlock/claim/persistence tests |
| World map, bosses, relics, Home Base, Codex, Rift | Full Windows integration not established | Progression and save round-trip tests |
| Arcade, minigames, Lua activities | Full Windows integration not established | Offline play and sandbox/reward validation |
| Save slots and backup/recovery | Full Windows integration not established | Restart, recovery and compatibility tests |
| Localization | Feature-level audit needed | Verify visible surfaces in supported locales |
| Keyboard/mouse/window behavior | Desktop QA required | Focus, shortcuts, resize and high-DPI results |
| Debug/release update channels | Existing boundary must be retained | Artifact/channel isolation tests |
| MSI install and upgrades | Packaging QA required | Install/upgrade/rollback evidence |

## Definition of done

Windows feature parity is complete only when every agreed player-facing feature is verified on Windows or explicitly approved as platform-specific/not included. Build success alone is not enough. Update this matrix as slices merge, and update the player-facing changelog in the same PR whenever a player-visible feature changes.
