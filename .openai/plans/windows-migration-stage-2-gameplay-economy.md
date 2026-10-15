# Windows Migration Stage 2 — Core Gameplay and Economy

**Status:** Planning PR; no production code changed yet.  
**Source of truth:** `docs/WINDOWS_FEATURE_PARITY_PLAN.md`, Stage 2 (W1–W2).  
**Opened:** 2026-10-15

## Finding

Stage 2 already exists in the Windows feature-parity plan. This file adds the implementation breakdown under `.openai/plans/`; it does not replace the parity plan or mark any migration work complete.

The Windows Play destination still hosts the standalone shared guessing pilot. The production Android `GameEngine` uses shared deterministic rules, but its authoritative `GameState`, `BigNumber`, reward calculation, and persistence remain in the Android app module. The pilot reducer is not production gameplay parity.

## Goal

Make Windows use the same deterministic gameplay rules, reward/economy decisions, and progression state semantics as Android, with desktop-owned input, lifecycle and persistence adapters. Keep offline play and existing save compatibility.

## Work packages

### S2-A — Production gameplay boundary and core loop

- [ ] Audit the current Android `GameEngine`, `GameViewModel`, `GameState`, `BigNumber`, shared rules, desktop shell, and existing tests before moving code.
- [ ] Define a platform-neutral command/result boundary for submit guess, start next round, and invalid/duplicate/post-solve submissions.
- [ ] Keep target generation injectable/deterministic in tests; do not duplicate the guessing rules in desktop UI code.
- [ ] Replace the standalone Windows pilot state with a production-connected controller for input, range, attempts, feedback, correct guesses, streaks and Frenzy.
- [ ] Preserve Android behavior and player-facing strings; update the changelog in the same PR for any player-visible change.

**Exit gate:** Windows runs the real guessing loop through the shared production boundary, with deterministic common tests and no second source of truth for guess counters.

### S2-B — Rewards, upgrades, shop and resets

- [ ] Share or adapt the production reward decision path: critical hits, streak/Frenzy, difficulty, mutators and upgrade/talent bonuses.
- [ ] Decide and document a multiplatform representation for `BigNumber` before porting currency math. Do not replace decimal arithmetic with `Double` or change multiplication/rounding order merely to simplify the port.
- [ ] Integrate Money, upgrades, shop purchases, Prestige, Ultra, Nebula, talent reset/preservation rules and eligibility checks.
- [ ] Prevent duplicate rewards and keep reset operations atomic from the player's perspective.
- [ ] Add parity tests for representative state/reward examples from Android.

**Exit gate:** The same input and starting state produce matching feedback, reward, counters and progression decisions on Android and Windows.

### S2-C — Persistence and recovery

- [ ] Define a desktop-native storage adapter; do not import Android DataStore APIs into desktop code.
- [ ] Preserve the existing serialized save contract and safe defaults for legacy fields.
- [ ] Cover primary/backup recovery, corrupt saves, slot selection where supported, and save/reload round trips.
- [ ] Serialize state mutations and writes so failed writes cannot silently leave the visible state ahead of disk.
- [ ] Add compatibility fixtures and failure-path tests before changing save schema or encryption/obfuscation behavior.

**Exit gate:** Windows can save, close/reopen and reload a run without losing or duplicating currency, counters or progression; legacy Android saves are either supported explicitly or the limitation is documented and agreed before implementation.

### S2-D — Parity gate and documentation

- [ ] Run relevant shared/JVM tests and the existing Windows Desktop CI checks; do not manually dispatch workflows unless explicitly authorized.
- [ ] Compare deterministic gameplay/economy scenarios across Android and Windows.
- [ ] Perform actual Windows runtime checks for guesses, rewards, upgrades, resets and persistence. CI alone is not runtime evidence.
- [ ] Update `docs/WINDOWS_FEATURE_PARITY_PLAN.md` with evidence-backed status and leave untested rows unverified.
- [ ] Keep Stage 3 progression/activities, Stage 4 utilities/tooling, and Stage 5 release hardening out of this stage.

**Exit gate:** Stage 2's W1 and W2 checklists are complete with code/test/runtime evidence, and the full core loop and economy preserve Android semantics.

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
