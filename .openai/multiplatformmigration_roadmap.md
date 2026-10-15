# Temporary Kotlin Multiplatform Migration Roadmap

**Purpose:** Track the ordered PR plan for the entire Android-to-Kotlin-Multiplatform migration before it reaches the finish line. This is an execution checklist, not a claim that unfinished work is complete.

**Last reviewed:** 2026-10-15  
**Current checkpoint:** PRs #142–#156 are merged. The save compatibility and DataStore write-failure test items through PR #156 are complete. Current KMP task: make the shared production guess transition return its complete next `GuessCoreState` (counters, streak, target and range) as one deterministic result, while Android retains RNG generation, `BigNumber` economy math, statistics and persistence. Do not start unrelated save follow-ups while this migration step is in progress.

## Rules for every PR

1. Use focused, ordered, reversible PRs. Never push directly to `master`.
2. Merge only when the latest required Android Debug/Release and Windows Desktop CI runs pass. Inspect logs and fix failures before merging.
3. Avoid manual/duplicate workflow runs just to poll status.
4. Preserve Android behavior and existing saves, save slots, large-number values, encrypted backup/import, migration and recovery.
5. Keep the game offline-first: no ads, analytics or accounts.
6. Preserve Windows MSI in-place upgrades and its verified UpgradeCode/product identity rules. Inspect existing WiX/MSI configuration before changing identifiers.
7. Follow `.openai/MEMORY.md`, `.openai/temp_plan1.md`, localization requirements and the standing changelog rule. For player-facing changes, update `app/src/main/assets/changelogs.md` in the same PR. Never list planned work as completed.
8. Add common tests for deterministic rules and state transitions. Avoid unnecessary emulator-heavy tests.
9. A shared UI pilot does not count as full platform parity. Verify each feature and record evidence.

## Completed migration PRs

- [x] #123 - Shared guess-entry UI pilot.
- [x] #124 - Shared production guess-transition tests.
- [x] #125 - Shared production mutator rules.
- [x] #126 - Shared Frenzy multiplier rules.
- [x] #127 - Shared Prestige/Ultra requirements.
- [x] #128 - Shared challenge reward validation.
- [x] #129 - Shared Prestige/Ultra reward formulas.
- [x] #130 - Shared critical-chance scaling.
- [x] #131 - Shared guess-reward multiplier rules.
- [x] #132 - Shared wrong-guess tax rules.
- [x] #133 - Shared difficulty balance rules.
- [x] #134 - Shared mutator range rules.
- [x] #135 - Shared talent reward multiplier.
- [x] #136 - Shared base guess reward constants.
- [x] #137 - Windows CI now runs Android locale/JSON/version/Kotlin preflight checks and watches relevant shared/desktop/app/tool paths.
- [x] #138 - Shared challenge-claim eligibility. Android and Windows CI passed; merged as `b08c9fd`.

## Ordered PR plan from here

### Phase 1 - Finish deterministic shared domain rules

- [x] **First completed:** #138 centralized the claim eligibility predicate in shared rules; Nebula/save-state mutation remains in the platform engine.
- [x] Inventory production decisions in Android `GameEngine` and classify them below. The next extraction must avoid duplicating already-shared rules.
- [x] Extracted shared range/target invariants into `GuessStateRules` and applied them in the shared reducer, with ordered-range, invalid-range and inclusive-boundary tests. Production Android state migration remains separate.
- [ ] Extract remaining pure progression/economy/upgrade/reset requirement formulas only after checking they are not already shared. Keep authoritative currency math and rounding unchanged.
- [x] Added a shared deterministic guess-transition contract that accepts explicit critical-roll and next-target inputs, validates the next target, and is used by Android `GameEngine`. Common tests cover threshold behavior, target validation and incorrect guesses without random inputs. Android still owns RNG generation, BigNumber reward/penalty math and full GameState mutation.
- [x] Extend the shared production transition to return the complete next `GuessCoreState` (updated counters/streak plus target/range) and add common tests for correct and incorrect guesses. Android delegates target/range state updates to this result; differential parity coverage remains in `GuessCoreStateParityTest`. Platform RNG, clock, storage, notifications, audio and haptics stay behind adapters.

### Phase 1 audit - Android `GameEngine` inventory (2026-10-15)

Inspected `app/src/main/java/com/jarrlyyy/guessthenumber/domain/engine/GameEngine.kt` against the shared rules currently in `shared/src/commonMain`.

| Production decision | Classification | Current owner / next action |
|---|---|---|
| Guess feedback, attempt/correct counters, streak and best-streak update | Already shared | `GuessStateRules`; Android engine delegates to it. Keep reward and platform state mutation separate for now. |
| Guess reward constants and multipliers (streak, Frenzy, critical chance, upgrades, talents, mutators, difficulty) | Already shared | `GuessRewardRules`, `FrenzyRules`, `CriticalChanceRules`, `MutatorRules`, `DifficultyRules`; Android still composes them with `BigNumber`. |
| Wrong-guess penalty and mutator tax rates | Already shared | `GuessPenaltyRules`; Android applies the resulting amount to its `BigNumber` balance. |
| Prestige/Ultra requirements and reward ratios | Already shared | `ProgressionRequirementRules` and `ProgressionRewardRules`; BigNumber conversion and flooring stay in Android. |
| Challenge claim eligibility and challenge reward validation | Already shared | `ChallengeRewardRules`; Android still commits completed IDs, Nebula and statistics. |
| Random critical roll and next-target selection | Shared deterministic contract + platform adapter | Android generates the values using its existing `kotlin.random.Random` and passes them to `GuessTransitionRules`; common tests can inject exact values. The RNG and probabilities remain unchanged. |
| Currency, statistics and full `GameState` updates | Not migrated as shared production transition | Android `GameEngine` currently commits these updates. Needs an explicit shared state/transition contract plus differential tests before moving mutations. |
| Save/persistence, clocks, notifications, audio and haptics | Platform adapter | Keep out of common domain rules; migrate only through explicit adapter contracts if needed. |

**Audit conclusion:** deterministic formulas, the guess-economy decision and the complete next `GuessCoreState` for a submitted guess are shared. Android still owns `BigNumber` currency arithmetic, game statistics and full `GameState` mutation. Keep those boundaries intact until separate parity coverage demonstrates exact legacy behavior.

### Phase 2 - Shared production state and engine

- [x] Add a shared production guessing-state slice and explicit mapping to/from the existing Android `GameState`; do not change save serialization.
- [x] Add differential tests comparing the legacy scalar rule, shared state model and Android `GameEngine` for correct and incorrect guesses with deterministic random inputs.
- [x] Migrate guess submission to the shared state model while keeping currency/statistics mutation on Android.
- [x] #145 - Extract reward multipliers and difficulty/tax penalty rates into a common deterministic decision result; keep `BigNumber` arithmetic and currency mutation on Android.
- [x] #146 - Add combined wrong-guess penalty parity coverage; Android and Windows CI passed before merge.
- [x] #146 - Add Android parity coverage for the combined Hard-difficulty and tax-mutator penalty; preserve current `BigNumber` deductions and guess statistics.
- [x] #147 - Share a deterministic challenge-claim decision result and test accepted, invalid and duplicate claims; keep Nebula arithmetic, statistics mutation and persistence on Android.
- [x] #148 - Audit atomicity across state mutation and persistence before changing transaction behavior. Failed validation or persistence must not award currency twice or leave partial state.
- [ ] Do not replace the existing large-number representation until a separately tested cross-platform numeric migration is justified.



### Phase 2 audit - Challenge claim and persistence atomicity (2026-10-15)

Inspected `GameEngine.claimChallenge`, `GameViewModel.claimChallenge`, `SaveManager.saveGame`, `SaveManager.updateGameAtomically` and the existing `SaveValidationTest` coverage after #147 merged.

- `GameEngine.claimChallenge` is a pure state transformation: invalid/duplicate claims return the original state; accepted claims add the challenge ID, Nebula and the Nebula-earned statistic together in the returned `GameState`.
- The current ViewModel claim path publishes the updated state to `_gameState` before scheduling `saveGameAsync()`. The caller does not receive a durable-save result, so a persistence failure can leave the in-memory state ahead of the saved state until a later save/reload. The shared decision prevents ordinary duplicate claims against the current state, but it does not by itself make the state publication and persistence one transaction.
- `SaveManager.updateGameAtomically` uses a single DataStore edit to validate the current primary, transform a decoded state, validate the serialized result and update primary plus backup together. Existing tests cover successful updates and rejecting an invalid transformed state without replacing the primary.
- The atomic update helper returns `null` when the primary save is absent or invalid. A future claim integration must deliberately handle fresh slots and backup-recovery behavior, must not overwrite newer in-memory gameplay state with a stale persisted snapshot, and must surface/handle persistence failure before claiming success.
- No transaction behavior was changed in the audit. PR #149 adds coverage for an accepted `weekly_correct` claim through `updateGameAtomically`, reloads it through a fresh `SaveManager` instance, and verifies a second claim does not increase Nebula or `nebulaEarned`. PR #150 adds a regression test that a rejected atomic update caused by a corrupt primary returns `null` without modifying a valid recovery backup. These tests verify helper-level duplicate and rejection behavior, not actual DataStore I/O failure or the ViewModel's asynchronous publication path. Persistence-failure injection and save/gameplay serialization remain open; do not wire production challenge claims through durable storage until those are designed and tested.

### Phase 3 - Save and persistence compatibility

- [x] #153 documents the actual save schema, defaults, storage keys, validation, recovery and current migration behavior before changing models.
- [x] #152 adds an inline legacy-save fixture and load → save → reload test for omitted newer fields, unknown-field tolerance, large balances and progression preservation.
- [x] PR #152 covers large currency values, omitted legacy fields, upgrades, reset counts, mutators, challenges, statistics and world progression.
- [ ] Cover settings and save-slot metadata round trips; current candidate adds focused global-settings JSON and slot primary/backup metadata tests.
- [ ] Keep Android storage adapters, encrypted backup/import, 10 save slots, atomic transactions, checksum/integrity checks, last-valid backup recovery and corruption handling intact.
- [ ] Introduce common serialization only after verifying compatible dependencies and behavior for all intended targets.

### Phase 3 audit - Save schema and persistence failure design (2026-10-15)

- `GameState` is serialized as JSON with `ignoreUnknownKeys = true` and `encodeDefaults = true`. Missing fields rely on constructor defaults; there is no dedicated integer save-schema version or explicit migration registry in the inspected path. `lastSavedVersion` is app-version metadata, not a schema version.
- The save model and storage layout are documented in `docs/SAVE_SCHEMA_AND_MIGRATION.md`. The current inline compatibility fixture is useful regression coverage but is not a historical save captured from an actual released build.
- `validateSave` checks key balances/counters/range invariants, not every field or cross-field relationship. `sanitizeLoadedState` repairs core fields only. Any hardening must be additive and fixture-tested.
- Backup/export currently uses XOR plus Base64 obfuscation and a plain-Base64 fallback; do not describe it as modern cryptographic protection or change its format in a compatibility-only task.
- Remaining gaps: settings and slot-metadata round trips, genuine DataStore I/O failure injection, and ViewModel consistency when a save fails after an in-memory reward claim. The next step is to design a narrow test seam and test failure outcomes before changing production transaction behavior.

### Phase 3 follow-up - Settings and slot metadata compatibility

PR #154 adds regression tests for global settings JSON round trips across manager instances and for slot metadata after a second save creates a recovery backup. The tests assert current slot values, large balances, profile/difficulty fields, attempts/correct guesses, save timestamp and empty-slot behavior. The next unresolved persistence task remains genuine DataStore I/O failure injection and consistent ViewModel state on failed writes.

### Phase 4 - Full shared UI, navigation and player feature parity

- [ ] Define shared navigation/action contracts and reusable presentation state.
- [ ] Migrate feature slices with UI, state/actions, localized strings, persistence behavior and tests together:
  1. Core loop: Play, guess input/feedback, rewards, upgrades and shop.
  2. Economy/resets: Prestige, Ultra, Nebula, permanent upgrades and talents.
  3. Progression: achievements, challenges, mutators, statistics, relics, world map/bosses, Home Base, Codex and Endless Rift.
  4. Activities: Arcade/minigames, Number Rush, LuaJ temporary activities and LiveOps.
  5. Player utilities: save slots, backup/restore, tutorial/feature tours, settings, About and crash recovery.
  6. Developer tools: command console, diagnostics, test helpers and logging remain debug-only.
- [ ] Keep all user-visible text localized. Validate every supported locale, placeholder contract and localization-surface scan for Android, shared and desktop sources.
- [ ] Keep UI patterns platform-appropriate when necessary instead of forcing identical layouts.

### Phase 5 - Windows production readiness

- [ ] Expand Windows beyond the simplified shared guessing-screen pilot until the agreed game features are implemented and parity-reviewed.
- [ ] Verify keyboard/mouse navigation, focus, resizing, minimum window size, high-DPI behavior, narrow layouts, close/reopen, offline play and recovery.
- [ ] Verify Windows Debug update discovery and installation stays separate from the production/release channel.
- [ ] Test the existing MSI upgrade path: clean install, in-place upgrade, repeated update, failed-upgrade recovery, downgrade behavior and uninstall. Preserve the verified UpgradeCode and user data.
- [ ] Keep intended shared content, locale assets, version metadata and changelog behavior aligned across Android and Windows.

### Phase 6 - Cross-platform verification and release gate

- [ ] Keep Android and Windows preflight coverage aligned for locales, JSON assets, version metadata and Kotlin source structure.
- [ ] Add focused shared tests and targeted stress tests for large-number math, long sessions, duplicate rewards, concurrent saves/recovery and Lua activity validation.
- [ ] Confirm release builds exclude developer-only tools and update channels cannot cross-install the wrong build.
- [ ] Publish a feature parity matrix: **verified on both**, **intentionally platform-specific**, or **not implemented**.
- [ ] Run final Android Debug/Release and Windows Desktop CI and inspect save compatibility and MSI upgrade evidence.

## Finish-line acceptance criteria

Do not call the migration complete until all are true:

- [ ] Android and Windows use the shared production engine for intended common rules and state transitions; platform adapters handle only platform-specific services.
- [ ] The full agreed player-facing feature set works on Windows, not only the shared guessing pilot.
- [ ] Existing Android saves and encrypted backup/import remain compatible and recoverable.
- [ ] Windows MSI upgrades existing installations in place without duplicate installs or save loss.
- [ ] All user-visible strings are localized and locale/asset checks pass.
- [ ] Android Debug/Release and Windows Desktop CI pass on the final candidate.
- [ ] Offline-first behavior remains intact, with no ads, analytics or accounts accidentally added.
- [ ] Documentation, changelog and parity matrix describe only verified work.

## Maintenance

Update this file whenever a PR merges, fails, is replaced or changes the next action. Keep the current checkpoint accurate. This is temporary planning; after migration, consolidate durable architecture decisions into normal project docs and remove/archive this file in a separate cleanup PR.

This roadmap is a proposed order based on current repository context, not a claim that every remaining file has already been audited.


### Phase 3 follow-up - Save-write failure test design

- [x] #154 covers global settings JSON and save-slot primary/backup metadata round trips; Android and Windows CI passed before merge.
- [x] Drafted `docs/SAVE_WRITE_FAILURE_TEST_DESIGN.md` to define a narrow DataStore write-failure seam and explicit assertions for primary/backup preservation.
- [x] Added an injectable DataStore seam and deterministic tests for failed `saveGame` and `updateGameAtomically` writes, including exact primary/backup preservation assertions.
- [ ] Add ViewModel failed-persistence consistency tests after the storage contract is verified. The current PR deliberately does not alter claim publication or reward semantics.
