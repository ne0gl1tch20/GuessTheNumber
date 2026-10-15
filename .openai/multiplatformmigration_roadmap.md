# Temporary Kotlin Multiplatform Migration Roadmap

**Purpose:** Track the ordered PR plan for the entire Android-to-Kotlin-Multiplatform migration before it reaches the finish line. This is an execution checklist, not a claim that unfinished work is complete.

**Last reviewed:** 2026-10-15  
**Current checkpoint:** PR #138 merged after both Android and Windows CI passed. PR #139 adds this roadmap; its docs-only Android workflow was queued at last check.

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
- [ ] Extract the next low-risk, pure state validation/invariant into `shared/src/commonMain`; add valid, invalid, boundary and duplicate-action tests in `commonTest`.
- [ ] Extract remaining pure progression/economy/upgrade/reset requirement formulas only after checking they are not already shared. Keep authoritative currency math and rounding unchanged.
- [ ] Define the shared production transition contract and inject deterministic randomness for tests. Platform RNG, clock, storage, notifications, audio and haptics stay behind adapters.

### Phase 1 audit - Android `GameEngine` inventory (2026-10-15)

Inspected `app/src/main/java/com/jarrlyyy/guessthenumber/domain/engine/GameEngine.kt` against the shared rules currently in `shared/src/commonMain`.

| Production decision | Classification | Current owner / next action |
|---|---|---|
| Guess feedback, attempt/correct counters, streak and best-streak update | Already shared | `GuessStateRules`; Android engine delegates to it. Keep reward and platform state mutation separate for now. |
| Guess reward constants and multipliers (streak, Frenzy, critical chance, upgrades, talents, mutators, difficulty) | Already shared | `GuessRewardRules`, `FrenzyRules`, `CriticalChanceRules`, `MutatorRules`, `DifficultyRules`; Android still composes them with `BigNumber`. |
| Wrong-guess penalty and mutator tax rates | Already shared | `GuessPenaltyRules`; Android applies the resulting amount to its `BigNumber` balance. |
| Prestige/Ultra requirements and reward ratios | Already shared | `ProgressionRequirementRules` and `ProgressionRewardRules`; BigNumber conversion and flooring stay in Android. |
| Challenge claim eligibility and challenge reward validation | Already shared | `ChallengeRewardRules`; Android still commits completed IDs, Nebula and statistics. |
| Random critical roll and next-target selection | Platform adapter / transition dependency | Uses Android engine's injected `kotlin.random.Random`; next step is to define deterministic inputs for shared transition tests, not to move platform RNG or alter probabilities. |
| Currency, statistics and full `GameState` updates | Not migrated as shared production transition | Android `GameEngine` currently commits these updates. Needs an explicit shared state/transition contract plus differential tests before moving mutations. |
| Save/persistence, clocks, notifications, audio and haptics | Platform adapter | Keep out of common domain rules; migrate only through explicit adapter contracts if needed. |

**Audit conclusion:** the easy deterministic formulas are already extracted. Do not invent another wrapper around them. The next implementation PR should introduce a small deterministic input/result contract around the production guess transition, with injected critical-roll/next-target values in tests, while leaving Android's existing random generation and `BigNumber` state mutation authoritative until differential tests prove parity.

### Phase 2 - Shared production state and engine

- [ ] Add a shared production state model and explicit mapping to/from existing Android state.
- [ ] Add differential tests comparing old engine behavior with shared behavior for identical states and injected random values.
- [ ] Migrate one production transition per PR: guess submission; reward/penalty application; upgrade purchases; Prestige; Ultra; challenge claims; achievements/rewards; remaining progression actions.
- [ ] Make transitions atomic and duplicate-resistant. Failed validation or persistence must not award currency twice or leave partial state.
- [ ] Do not replace the existing large-number representation until a separately tested cross-platform numeric migration is justified.

### Phase 3 - Save and persistence compatibility

- [ ] Document the actual save schema and migration rules before changing models.
- [ ] Add representative legacy-save fixtures and round-trip tests: load → map → update → save → reload.
- [ ] Cover large currency values, omitted legacy fields, upgrades, reset counts, mutators, challenges, statistics, world progression, settings and save-slot metadata.
- [ ] Keep Android storage adapters, encrypted backup/import, 10 save slots, atomic transactions, checksum/integrity checks, last-valid backup recovery and corruption handling intact.
- [ ] Introduce common serialization only after verifying compatible dependencies and behavior for all intended targets.

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
