# Kotlin Multiplatform port status

## Goal

Make Android and Windows desktop run the same GuessTheNumber game domain while preserving the existing Android app, save JSON format, offline behavior and platform-specific integrations. This is a staged migration; the current desktop app is not yet at full feature parity.

## PR dependency order

Merge in order because each PR is based on the preceding branch:

1. [PR #105 — foundational shared domain rules](https://github.com/ne0gl1tch20/GuessTheNumber/pull/105)
2. [PR #106 — shared JVM BigNumber](https://github.com/ne0gl1tch20/GuessTheNumber/pull/106)
3. [PR #107 — shared GameState models](https://github.com/ne0gl1tch20/GuessTheNumber/pull/107)
4. [PR #108 — shared GameEngine](https://github.com/ne0gl1tch20/GuessTheNumber/pull/108)
5. [PR #109 — desktop gameplay, save adapter and Lua runtime foundation](https://github.com/ne0gl1tch20/GuessTheNumber/pull/109)

These PRs are intentionally stacked and remain unmerged until reviewed/merged in order.

## Stage status

### Step 2 — shared domain foundation

- Difficulty balance/validation and profile-icon rules are in `shared/commonMain`.
- Android keeps compatibility facades to avoid a large UI rewrite.

### Step 3 — shared engine and progression

- The existing BigDecimal-based `BigNumber` is moved to `shared/jvmMain` without changing its string serialization.
- `GameState`, settings, statistics and item-definition models are moved to `shared/jvmMain`.
- The existing `GameEngine` is moved without intentionally changing reward formulas.
- The feedback-driven `GuessingBot` is now in `commonMain`.
- Backward-clock detection is a deterministic shared policy; Android retains its system-clock/logging adapter.
- Random-event rewards and bounded LiveOps claim rules are shared.
- Additional progression services still need auditing and migration, including upgrade purchases, prestige/ultra reset orchestration, boss/world/relic/mastery logic and challenge flows.

### Step 4 — persistence

- A shared JSON codec uses `ignoreUnknownKeys = true` and `encodeDefaults = true`, matching the existing Android save JSON policy; Android `SaveManager` now uses the same `Json` configuration.
- The desktop adapter uses atomic file replacement and a last-known-good backup in `~/.guessthenumber/`.
- Android continues using its DataStore/encryption/recovery implementation through compatibility aliases. The desktop save format is JSON-compatible, but cross-platform save import/export UX and deeper corruption/fault-injection coverage remain to be added.

### Step 5 — Android reconnection

- Existing Android model and engine package names are retained with typealiases/facades, so current callers can migrate without a broad UI rewrite.
- Android-specific storage, notifications, widgets, audio, logging and context-bound services remain platform adapters.

### Step 6 — desktop gameplay

- Desktop now calls the shared `GameEngine` and `GameState`, with difficulty selection, rewards, streaks, counters and local persistence.
- It is an initial real gameplay slice, not yet the entire Android UI or progression set.

### Step 7 — Lua and localization

- The instruction-budgeted LuaJ sandbox and strict activity payload validator are moved to `shared/jvmMain`, and Android keeps compatibility facades.
- The Windows UI now loads the bundled Number Rush Lua activity through the shared sandbox and validates its payload before displaying it.
- LuaJ stays JVM-specific. The complete activity catalog/host API, script lifecycle, Kotlin-owned activity rewards and remote signed LiveOps bundle flow still need integration on desktop.
- Localization catalogs and formatting still need a platform-neutral catalog/formatting layer; Android locale/resource integrations remain Android-specific.

### Step 8 — verification and parity

- Common rules and JVM tests cover difficulty/profile rules, core guessing, random events, LiveOps claims, save codec behavior and Lua payload validation.
- Windows CI runs `:shared:jvmTest`, compiles the desktop app and packages the Windows MSI.
- Android CI runs its unit tests, lint, full Gradle check and APK builds for PRs.
- Final completion requires both workflows to pass after the stacked PRs are merged, plus feature-by-feature parity checks and save compatibility verification. No full-port claim should be made until the remaining progression, localization and Lua host work is complete.

## Platform strategy

The shared module currently has a JVM target. That deliberately lets Android and Windows reuse the existing BigDecimal model and LuaJ implementation without replacing them with lossy arithmetic or an unverified Lua runtime. Platform-neutral rules stay in `commonMain`; JVM-compatible domain logic lives in `jvmMain`; Android system integrations remain in `:app`; Windows storage/window/installer integration remains in `:desktopApp`.

A later native target should only be added if the project needs a non-JVM platform. It is not required for Android + Windows desktop.
