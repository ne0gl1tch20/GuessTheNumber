# Windows Feature Parity Plan

**Status:** In progress; Windows has a desktop shell and shared guessing-screen pilot, not the full Android feature set.
**Reviewed:** 2026-10-15
**Depends on:** [KMP Foundation Status](KMP_FOUNDATION_STATUS.md)

## Goal

Bring Windows to the agreed player-facing feature set in deliberate, testable slices. Reuse shared rules and selected UI where helpful, while keeping Windows windowing, input, storage, update and installer behavior in the desktop host.

This replaces the Windows-related phases buried in the temporary KMP migration roadmap.

## Locked five-stage execution order

**Execution order is locked: complete stages 1 through 5 sequentially.** The W0–W8 sections below are detailed work packages mapped into these five stages, not competing roadmaps. Do not skip ahead to later-stage feature work unless an earlier-stage blocker is documented and the dependency is genuinely independent.

1. **Stage 1 — Desktop readiness and parity inventory** *(W0)*  
   Audit Android features against Windows, update the parity matrix with source evidence, and establish reliable Windows navigation, lifecycle, input/focus, resizing, scaling, theme and localization loading.  
   **Gate:** desktop shell and inventory are ready to host full features.

2. **Stage 2 — Complete core gameplay and economy** *(W1–W2)*  
   Finish guessing/input/feedback, rewards, attempts, streaks, mutators, upgrades/shop, persistence, Prestige, Ultra, Nebula and talent/economy/reset behavior.  
   **Gate:** core play and economy work end-to-end with consistent state, rewards and save/reload behavior.

3. **Stage 3 — Full progression and activities** *(W3–W4)*  
   Add achievements, challenges, statistics, world map, bosses, relics, mastery, Home Base, Codex, Endless Rift, arcade/minigames, events and supported Lua activities.  
   **Gate:** supported progression and activities work offline and preserve Kotlin-owned validation and reward/state boundaries.

4. **Stage 4 — Player utilities and developer tooling** *(W5–W6)*  
   Complete settings, accessibility, locale/theme, save slots, backup/import/export/recovery, tutorials, player-facing errors, supported music controls and debug-only developer tools.  
   **Gate:** players can configure and recover their game; developer tools stay out of production.

5. **Stage 5 — Release hardening and final parity verification** *(W7–W8)*  
   Verify debug/release update-channel separation, MSI clean install and upgrade/recovery behavior, save compatibility, Windows CI, focused shared tests, runtime regression and every row of the final parity matrix.  
   **Gate:** every agreed feature is marked verified with evidence, intentionally platform-specific, not included by agreement, or blocked. Do not declare parity complete while required rows remain unverified or release blockers remain.

### Sequencing rules

- Finish and record each stage's exit gate before treating the next stage as active.
- Each implementation slice should be a focused PR with relevant tests and parity-matrix updates; merge only when its required checks and review are acceptable.
- Do not create more migration-only UI extraction work. Reopen KMP foundation work only for a demonstrated defect or a specific cross-platform feature requirement.
- Do not claim runtime verification from a successful build alone. Keep unrun checks marked unverified.
- Preserve offline play and existing save/MSI/update-channel compatibility; do not add ads, analytics or accounts.

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

- [x] Inventory Android destinations, visible features, settings, save operations and debug-only routes against Windows (see source-backed inventory below).
- [x] Update the parity table below before implementation.
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

## Stage 1 source audit (2026-10-15)

This is a repository-source audit, not a runtime test. Paths below link to the code inspected on `master`.

### Confirmed source findings

- **Windows entry point and shell:** [\`desktopApp/.../desktop/Main.kt\`](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt) opens a resizable Compose Desktop \`Window\` with an initial 1100×760 dp size, a Windows-owned Play/About navigation shell, the shared guessing pilot and Windows Debug update controls. [\`DesktopNavigation.kt\`](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopNavigation.kt) defines the testable shell state/controller. The host still does not wire Android's production \`GameViewModel\`, full \`GameState\`, production navigation or save system into Windows.
- **Shared guessing screen:** [`SharedGuessGameScreen.kt`](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/ui/SharedGuessGameScreen.kt) owns temporary in-memory demo state and generates a local target. [`SharedGuessGameContent.kt`](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/ui/SharedGuessGameContent.kt) explicitly leaves game state, target generation, persistence and navigation to the host. This is a pilot UI, not end-to-end Windows gameplay parity.
- **Android feature/navigation reference:** [`Screen.kt`](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/Screen.kt) declares 26 destinations, including debug-only Lua Testing Hub and Process Inspector. [`NavGraph.kt`](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt) also defines `live_ops` as a separate route and wires destinations to Android-owned state/actions and Android-specific services. The source-backed inventory and planned Windows stage are recorded below.
- **Windows packaging:** [`desktopApp/build.gradle.kts`](../desktopApp/build.gradle.kts) targets Compose Desktop MSI and preserves the existing MSI UpgradeCode. [.github/workflows/windows-desktop.yml](../.github/workflows/windows-desktop.yml) runs localization/JSON/preflight checks, shared and desktop JVM tests, desktop compilation and MSI packaging; its PR path includes a legacy-to-current MSI upgrade check. These checks do not prove interactive runtime parity.
- **Windows updater:** [`WindowsDebugUpdateClient.kt`](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/update/WindowsDebugUpdateClient.kt) reads the public GitHub Releases API and validates the Windows Debug manifest/channel/checksum format. Keep this behavior isolated from production release discovery.
- **Project inclusion:** [`settings.gradle.kts`](../settings.gradle.kts) includes `:app`, `:shared` and `:desktopApp`.

### Initial feature parity matrix

A shared component or green CI is not proof that a complete feature works end-to-end on Windows. “Source gap” means the inspected Windows entry point does not currently wire the feature; “runtime unverified” means no interactive Windows session was run as part of this audit.

| Feature area | Current source-backed assessment | Evidence / next proof |
|---|---|---|
| Desktop launch and window close | **Implemented in source; runtime unverified.** One resizable Compose Desktop window with an initial 1100×760 dp size and close-to-exit behavior. Minimum size and high-DPI behavior remain unverified. | [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt); run on Windows and verify launch/close, minimum size and scaling. |
| Responsive guessing UI | **Shared pilot present; runtime unverified.** Shared content changes layout at a width threshold, but the desktop host uses standalone demo state. | [SharedGuessGameContent.kt](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/ui/SharedGuessGameContent.kt), [SharedGuessGameScreen.kt](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/ui/SharedGuessGameScreen.kt); test resize, narrow window and high DPI. |
| Production game state, rewards and save persistence | **Source gap in Windows host.** The desktop entry point does not connect the shared pilot to Android's authoritative state/save architecture. | [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt); add a Windows-owned state/persistence adapter and round-trip tests before claiming core-loop parity. |
| Desktop navigation shell | **Initial source implementation; runtime unverified.** Windows has Play and About destinations with a testable state controller. The production Android destinations are not yet implemented on Windows. | [DesktopNavigation.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopNavigation.kt), [DesktopNavigationTest.kt](../desktopApp/src/jvmTest/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopNavigationTest.kt), [Screen.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/Screen.kt); validate keyboard focus/navigation and expand destinations only as their features are implemented. |
| Upgrades, shop, Prestige, Ultra, talents and economy | **Not wired in inspected Windows entry point.** | Android destination and game-state/action wiring in [NavGraph.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt); implement/test end-to-end in Stage 2. |
| Achievements, challenges, statistics, map, bosses, relics, Home Base, Codex, Rift and activities | **Not wired in inspected Windows entry point.** | Android destination list and [NavGraph.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt); inventory individual screens/actions during Stage 1 and implement in Stage 3. |
| Settings, locale, accessibility, save slots and recovery | **Not wired in inspected Windows entry point.** | [NavGraph.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt); define Windows-native equivalents in Stage 4. |
| Localization | **CI validators exist; desktop runtime coverage unverified.** Main window strings inspected are hardcoded English. | [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt), [Windows Desktop CI](../.github/workflows/windows-desktop.yml); identify reusable locale resources and verify actual Windows rendering. |
| Keyboard/mouse/focus, Enter/Escape, minimum size, high DPI and resize | **Source configured; runtime unverified.** The desktop host applies an 800×600 dp minimum and handles Ctrl+1 (Play) / Ctrl+2 (About) at the shell level. Focus order, mouse behavior, Enter/Escape, high-DPI behavior and actual size enforcement still require interactive Windows QA. | [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt), [DesktopNavigation.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopNavigation.kt); verify shortcuts and resize limits on an actual Windows display. |
| Windows Debug update channel | **Source implemented; end-to-end runtime unverified.** Public prerelease discovery validates manifest channel and checksum shape; installer path exists in desktop host. | [WindowsDebugUpdateClient.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/update/WindowsDebugUpdateClient.kt), [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt); verify real update and failure paths without crossing into production releases. |
| MSI packaging and upgrade identity | **Build configuration and CI checks present; full lifecycle not established.** UpgradeCode is explicitly preserved. | [desktopApp/build.gradle.kts](../desktopApp/build.gradle.kts), [Windows Desktop CI](../.github/workflows/windows-desktop.yml); retain UpgradeCode and complete install/upgrade/recovery regression in Stage 5. |
| Shared deterministic rules | **Shared foundation and common tests exist; Windows end-to-end use is not established by the desktop entry point.** | [KMP Foundation Status](KMP_FOUNDATION_STATUS.md), [shared rules](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/rules/GuessRules.kt); connect the authoritative game flow and test integration. |

### Source-backed Android destination and data-operation inventory

This inventory was checked against `Screen.kt`, `NavGraph.kt`, `MainActivity.kt`, `GameViewModel.kt`, `SaveManager.kt`, `LocaleManager.kt` and `SAVE_SCHEMA_AND_MIGRATION.md` on 2026-10-15. The stage labels below are sequencing targets, not a claim that the Windows implementation exists. On Windows, only the initial Play/About shell is present; Play still embeds the demo pilot and About is informational, so neither counts as production feature parity.

| Android destination or route | Planned stage | Windows status / notes |
|---|---|---|
| Play | Stage 2 | Shell destination exists; production game state, rules integration, rewards and persistence are not wired. |
| Upgrades | Stage 2 | Not implemented as a Windows production feature. |
| Shop | Stage 2 | Not implemented as a Windows production feature. |
| More | Stage 1 then Stages 2–4 | No production hub/menu yet; it must expose features as their Windows slices land. |
| Prestige | Stage 2 | Not implemented as a Windows production feature. |
| Ultra | Stage 2 | Not implemented as a Windows production feature. |
| Talent Web | Stage 2 | Economy/progression dependencies must be shared consistently; not implemented on Windows. |
| Mutators | Stage 2 | Includes challenge-builder gating and state/reward integration; not implemented on Windows. |
| Statistics | Stage 3 | Not implemented on Windows. |
| Achievements | Stage 3 | Not implemented on Windows. |
| World Map | Stage 3 | Not implemented on Windows; Android route is feature-flag gated. |
| Relics | Stage 3 | Not implemented on Windows; Android locks it until a world boss is defeated. |
| Home Base | Stage 3 | Not implemented on Windows. |
| Codex | Stage 3 | Not implemented on Windows; Android locks it until a world boss is defeated. |
| Endgame | Stage 3 | Not implemented on Windows; Android requires all four world bosses defeated. |
| Arcade | Stage 3 | Not implemented on Windows; rewards must go through validated game-state actions. |
| Activity Hub (`lua_hub`) | Stage 3 | Not implemented on Windows; Android gates it with the Lua scripting feature flag. |
| Lucky Stake | Stage 3 | Not implemented on Windows; settlement must preserve authoritative economy rules. |
| Live Ops (`live_ops` route) | Stage 3 | Separate NavGraph route, not a `Screen.kt` object; Android gates it with Live Ops and seasonal-reward flags. Not implemented on Windows. |
| Settings | Stage 4 | Not implemented on Windows; desktop-native equivalents are required rather than copying Android APIs. |
| Save Slots | Stage 4 | Not implemented on Windows. Android's source defines 10 slots, active-slot selection, slot metadata, backup/recovery and legacy-save migration. |
| Music Player | Stage 4 | Not implemented on Windows; confirm desktop audio support and document any approved platform difference. |
| About | Stage 1 shell / Stage 4 completion | Basic Windows informational screen exists; full Android About behavior/content is not yet matched. |
| Changelog | Stage 4 | Not implemented as a Windows player-facing feature. |
| Lua Testing Hub | Stage 4, debug-only | Android route exists only in debug builds and is feature-flag gated. Must never leak into production. |
| Dev Settings | Stage 4, debug-only | Android route exists only in debug builds and is feature-flag gated. Must never leak into production. |
| Process Inspector | Stage 4, debug-only | Android route exists only in debug builds. Must never leak into production. |

#### Cross-cutting Android behavior that Windows must account for

- **State and save data:** Android's `GameViewModel` owns authoritative `GameState` mutation and coordinates `SaveManager`. Save payloads serialize `GameState` as JSON with kotlinx.serialization; nested data includes settings, statistics and large-number values. The current schema has no dedicated integer schema-version field, and `lastSavedVersion` is app-version metadata, not a schema version. Windows must not blindly import Android `DataStore` APIs or invent a conflicting format. Define a desktop-native storage adapter and explicit compatibility/round-trip tests before Stage 2 gameplay is considered complete.
- **Save operations:** Account for up to 10 slots, active-slot selection, slot metadata, primary/backup recovery, validated legacy migration, save export/import, reset, and app preferences. Existing behavior and save compatibility must be preserved; test failure and recovery paths rather than treating a successful write as enough.
- **Settings:** The saved settings model includes locale, theme customization, number formatting, reduced motion/flashes, audio/haptics/notifications, user-selected music paths/queues/playlists, More-screen ordering/colors and feature-tour state. Some Android controls/services are platform-specific; each needs an explicit Windows equivalent or a documented intentional difference.
- **Localization:** Android `LocaleManager` loads a selected locale JSON overlay on top of the English catalog and falls back to English. The inspected catalog lists 23 locales and locale-aware number/date/time formatting. The Windows shell currently uses hardcoded English strings, so locale resource discovery/loading, fallback behavior and all visible/accessibility strings remain open Stage 1 work.
- **Startup and transient states:** Android has first-save/empty-save-slot handling, crash recovery, settings migration, offline gains, time-travel detection and changelog dialogs, plus notification deep-link routing. Windows needs a documented equivalent, or an explicit platform-specific decision, for every applicable state; the Play/About shell alone does not cover them.
- **Input and windowing:** Keyboard/mouse focus order, Enter/Escape behavior, minimum size, high-DPI scaling, narrow layouts, window close/reopen behavior and theme handling still require Windows runtime verification. CI is not a substitute for this pass.

The inventory is source-backed and completes the **documentation/inventory** part of Stage 1. It does not pass the Stage 1 exit gate: Windows runtime QA, desktop locale/theme/loading/error-state checks and the remaining shell behavior are still unverified.

### Stage 1 implementation progress

- [x] Add a Windows-owned destination/state contract for the initial Play and About shell.
- [x] Add unit tests for default destination, navigation/message clearing and blank-message handling.
- [x] Give the desktop window a resizable 1100×760 dp initial size.
- [ ] Run the desktop unit tests and compilation on Windows CI.
- [ ] Verify actual window launch/close, keyboard and mouse focus, minimum sizing, high-DPI and narrow layouts on Windows.
- [ ] Audit theme/locale loading and loading/error/empty states beyond the updater status.
- [x] Map Android destinations and save/settings operations to planned Windows stages (see inventory below).

The shell is a first implementation slice, not the Stage 1 exit gate. Windows runtime checks remain unverified, and the production game state/save system is intentionally deferred to Stage 2.

### Stage 1 audit conclusion and first implementation slice

The primary blocker is not a missing cosmetic tweak: Windows currently hosts the standalone guessing pilot, not the production game architecture. Therefore, the first implementation slice should establish a **Windows desktop application shell contract** before porting individual features:

1. Introduce a Windows-owned application state/controller boundary that can own the real game state and persistence without importing Android APIs.
2. Establish explicit desktop navigation and a destination registry, initially covering the shell plus the core gameplay destination.
3. Define window behavior centrally: sensible minimum size, resize-aware content, keyboard/mouse focus expectations and a visible loading/error state.
4. Add focused desktop tests for navigation/state transitions and document which checks still require an actual Windows session.
5. Keep updater behavior intact and separate; do not change MSI identity or start Stage 2 feature work until the Stage 1 exit gate is met.

This is a proposed implementation boundary derived from the inspected source, not a claim that these tasks are already implemented. No Windows interactive runtime session was run during this audit.

## Definition of done

Windows feature parity is complete only when every agreed player-facing feature is verified on Windows or explicitly approved as platform-specific/not included. Build success alone is not enough. Update this matrix as slices merge, and update the player-facing changelog in the same PR whenever a player-visible feature changes.
