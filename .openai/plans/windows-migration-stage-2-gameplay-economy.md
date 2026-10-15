# Windows Migration Stage 2 — Core Gameplay and Economy

**Status:** Implementation in progress; Stage 2-D parity audit is open and the Stage 2 exit gate has not passed.  
**Source of truth:** `docs/WINDOWS_FEATURE_PARITY_PLAN.md`, Stage 2 (W1–W2).  
**Opened:** 2026-10-15

## Current implementation finding

Stage 2 already exists in the Windows feature-parity plan. This file tracks its implementation breakdown and does not replace the parity plan.

PRs #193–#196 have moved the production `GameState`, `BigNumber`, `GameEngine` and `Difficulty` boundary into shared JVM/Android code, connected the Windows Play screen to the production guess engine, and added desktop-native persistence with backup recovery and failure-path tests. The Windows screen currently exposes only the basic guessing loop and a few counters. PR #198 (Stage 2-B initial slice) now adds desktop economy transactions, catalog loading, purchase/reset UI and deterministic tests. Full parity of effects tied to Android-only services and actual Windows runtime verification remain outstanding. See [`docs/WINDOWS_STAGE_2D_PARITY_VERIFICATION.md`](../../docs/WINDOWS_STAGE_2D_PARITY_VERIFICATION.md) for evidence and the current gate.

## Goal

Make Windows use the same deterministic gameplay rules, reward/economy decisions, and progression state semantics as Android, with desktop-owned input, lifecycle and persistence adapters. Keep offline play and existing save compatibility.

## Work packages

### S2-A — Production gameplay boundary and core loop

- [x] Audit the production `GameEngine`, `GameState`, `BigNumber`, shared rules, desktop shell and existing tests before moving the production boundary.
- [ ] Define a platform-neutral command/result boundary for submit guess, start next round, and invalid/duplicate/post-solve submissions.
- [x] Keep target generation injectable/deterministic in tests; do not duplicate the guessing rules in desktop UI code.
- [x] Replace the standalone Windows pilot state with a production-engine-backed guessing screen for input, range, attempts and feedback. Full economy UI remains in S2-B.
- [ ] Preserve Android behavior and player-facing strings; update the changelog in the same PR for any player-visible change.

**Exit gate:** Windows runs the real guessing loop through the shared production boundary, with deterministic common tests and no second source of truth for guess counters.

### S2-B — Rewards, upgrades, shop and resets

- [ ] Share or adapt the production reward decision path: critical hits, streak/Frenzy, difficulty, mutators and upgrade/talent bonuses.
- [ ] Decide and document a multiplatform representation for `BigNumber` before porting currency math. Do not replace decimal arithmetic with `Double` or change multiplication/rounding order merely to simplify the port.
- [x] Integrate Money upgrades, Nebula shop purchase records, Prestige/Ultra resets and upgrades, talent unlock prerequisites, and reset eligibility/preservation rules in the desktop transaction layer. Runtime verification and effect parity for services/background systems remain open.
- [x] Reject duplicate shop/talent purchases and invalid resets; persist transaction results before publishing the new desktop UI state.
- [x] Add deterministic tests for upgrade cost/affordability, Prestige/Ultra thresholds and reset preservation, duplicate shop purchases, talent prerequisites and desktop catalog loading. [ ] Expand exact reward fixtures and verify all catalog effects against Android.

**Exit gate:** PARTIAL. Core guess rewards use the shared production engine. The first economy transaction/UI slice is implemented and tested in PR #198, but full effect parity, Android service-owned progression and actual Windows runtime checks are still required.

### S2-C — Persistence and recovery

- [x] Define a desktop-native storage adapter; do not import Android DataStore APIs into desktop code.
- [x] Preserve the shared serialized save contract and safe defaults for legacy fields.
- [x] Cover primary/backup recovery, corrupt saves, and save/reload round trips. The screen currently uses slot 1; player-facing slot selection remains unimplemented.
- [x] Keep writes off the UI thread and publish accepted guess state only after its save succeeds.
- [x] Add legacy-default compatibility fixtures and injected write-failure tests.

**Exit gate:** Windows can save, close/reopen and reload a run without losing or duplicating currency, counters or progression; legacy Android saves are either supported explicitly or the limitation is documented and agreed before implementation.

### S2-D — Parity gate and documentation

- [x] Add an engine-plus-desktop-save integration test for deterministic incorrect/correct guesses, counters and reloads; confirm CI on this PR before treating it as passing.
- [ ] Confirm current-candidate Android validation and Windows Desktop CI.
- [ ] Add representative economy/reward/reset parity scenarios after S2-B is implemented.
- [ ] Perform actual Windows runtime checks for guesses, rewards, upgrades, resets and persistence. CI alone is not runtime evidence.
- [ ] Update `docs/WINDOWS_FEATURE_PARITY_PLAN.md` with evidence-backed status and leave untested rows unverified.
- [x] Keep Stage 3 progression/activities, Stage 4 utilities/tooling, and Stage 5 release hardening out of this stage.

**Exit gate:** NOT MET. PR #198 adds economy purchases and reset flows with automated coverage, but full effect parity and actual Windows runtime verification remain outstanding.

## Recommended PR sequence

1. **S2-A:** shared production boundary, real Windows guess loop, deterministic tests.
2. **S2-B:** economy/rewards/upgrades/shop/Prestige/Ultra/Nebula/talents, with BigNumber compatibility decided first.
3. **S2-C:** desktop persistence adapter, compatibility/recovery/failure tests.
4. **S2-D:** parity matrix, cross-platform scenario coverage and Windows runtime QA evidence.

Keep each PR cohesive and allow multiple related commits per PR. Do not make tiny one-feature migration PRs when the work belongs to the same boundary.

## Guardrails

- **Maintain Android while adding Windows.** Every shared-code, gameplay, economy, save, localization, icon or Gradle change must account for Android compatibility in the same PR; Windows parity must never come at the cost of Android regressions.
- Android remains the behavior reference unless a platform-specific difference is explicitly approved.
- Preserve offline-first behavior; do not add ads, analytics, accounts or network dependencies.
- Run/check the relevant Android and Windows validation for changes that affect shared code or build wiring. Keep the Android changelog current for actual player-facing changes.
- Use root GTN_APP_LOGO.png as the source artwork for both Android mobile launcher icons and Windows app/installer icons; verify packaged output on both platforms before claiming completion.
- Preserve existing saves and MSI/update-channel identity.
- Keep platform services in host adapters; shared production rules stay deterministic and UI-neutral.
- Keep debug tools debug-only.
- Do not claim parity from compilation alone.
- Do not merge this planning PR or any implementation PR automatically.

## First implementation action

Start with S2-A's source audit and architecture boundary. The main risk to resolve before moving production code is the Android-only `GameState`/Java `BigDecimal` `BigNumber` dependency. Keep the current shared rule APIs as the starting point, but do not promote the demo `GuessGameState` into the production save model by renaming it or copying fields without an explicit mapping and compatibility tests.


## Stage 2-B economy implementation slice

- PR #198 adds the Android economy JSON catalogs as Windows JVM resources, immutable desktop economy transactions and Windows UI for Money/Prestige/Ultra currencies, all catalog upgrades, Prestige/Ultra resets, Nebula shop purchase records and talent unlocks.
- Every economy action saves before the visible state is updated; a failed save leaves the previous UI state intact.
- Automated tests cover purchase cost and affordability, max-level rejection, Prestige/Ultra thresholds and reset preservation, duplicate shop purchases, talent prerequisites and catalog parsing.
- Still open: verify actual runtime behavior on Windows, confirm all item effects (especially auto-clicker/background behavior and shop boosts) against Android, expand expected-reward fixtures, and complete the broader Stage 2 economy/feature matrix.
