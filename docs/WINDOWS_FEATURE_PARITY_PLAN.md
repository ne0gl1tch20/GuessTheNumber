# Windows Feature Parity Plan

**Status:** Planned; Windows has a desktop target and shared guessing-screen pilot, not the full Android feature set.
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

## Stage 1 source audit (2026-10-15)

This is a repository-source audit, not a runtime test. Paths below link to the code inspected on `master`.

### Confirmed source findings

- **Windows entry point and shell:** [`desktopApp/.../desktop/Main.kt`](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt) opens one Compose Desktop `Window`, checks/installs Windows Debug updates, and embeds `SharedGuessGameScreen()`. The shown host does not wire Android's production `GameViewModel`, full `GameState`, production navigation, or save system into Windows.
- **Shared guessing screen:** [`SharedGuessGameScreen.kt`](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/ui/SharedGuessGameScreen.kt) owns temporary in-memory demo state and generates a local target. [`SharedGuessGameContent.kt`](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/ui/SharedGuessGameContent.kt) explicitly leaves game state, target generation, persistence and navigation to the host. This is a pilot UI, not end-to-end Windows gameplay parity.
- **Android feature/navigation reference:** [`Screen.kt`](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/Screen.kt) defines Play, Upgrades, Shop, More, Music Player, Arcade, Activity Hub, Statistics, Settings, About, Dev Settings, Prestige, Ultra, Achievements, Talent Web, Mutators, Process Inspector, Changelog, Save Slots, World Map, Relics, Home Base, Codex, Endgame and Lucky Stake. [`NavGraph.kt`](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt) wires these destinations to Android-owned state/actions and Android-specific services.
- **Windows packaging:** [`desktopApp/build.gradle.kts`](../desktopApp/build.gradle.kts) targets Compose Desktop MSI and preserves the existing MSI UpgradeCode. [.github/workflows/windows-desktop.yml](../.github/workflows/windows-desktop.yml) runs localization/JSON/preflight checks, shared and desktop JVM tests, desktop compilation and MSI packaging; its PR path includes a legacy-to-current MSI upgrade check. These checks do not prove interactive runtime parity.
- **Windows updater:** [`WindowsDebugUpdateClient.kt`](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/update/WindowsDebugUpdateClient.kt) reads the public GitHub Releases API and validates the Windows Debug manifest/channel/checksum format. Keep this behavior isolated from production release discovery.
- **Project inclusion:** [`settings.gradle.kts`](../settings.gradle.kts) includes `:app`, `:shared` and `:desktopApp`.

### Initial feature parity matrix

A shared component or green CI is not proof that a complete feature works end-to-end on Windows. “Source gap” means the inspected Windows entry point does not currently wire the feature; “runtime unverified” means no interactive Windows session was run as part of this audit.

| Feature area | Current source-backed assessment | Evidence / next proof |
|---|---|---|
| Desktop launch and window close | **Implemented in source; runtime unverified.** One Compose Desktop window with close-to-exit behavior. | [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt); run on Windows and verify launch/close. |
| Responsive guessing UI | **Shared pilot present; runtime unverified.** Shared content changes layout at a width threshold, but the desktop host uses standalone demo state. | [SharedGuessGameContent.kt](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/ui/SharedGuessGameContent.kt), [SharedGuessGameScreen.kt](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/ui/SharedGuessGameScreen.kt); test resize, narrow window and high DPI. |
| Production game state, rewards and save persistence | **Source gap in Windows host.** The desktop entry point does not connect the shared pilot to Android's authoritative state/save architecture. | [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt); add a Windows-owned state/persistence adapter and round-trip tests before claiming core-loop parity. |
| Android production destinations | **Source gap in Windows host.** Android defines 25 named destinations; Windows currently displays the guessing pilot and updater card rather than equivalent production navigation. | [Screen.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/Screen.kt), [NavGraph.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt), [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt); map destinations to Windows feature slices. |
| Upgrades, shop, Prestige, Ultra, talents and economy | **Not wired in inspected Windows entry point.** | Android destination and game-state/action wiring in [NavGraph.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt); implement/test end-to-end in Stage 2. |
| Achievements, challenges, statistics, map, bosses, relics, Home Base, Codex, Rift and activities | **Not wired in inspected Windows entry point.** | Android destination list and [NavGraph.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt); inventory individual screens/actions during Stage 1 and implement in Stage 3. |
| Settings, locale, accessibility, save slots and recovery | **Not wired in inspected Windows entry point.** | [NavGraph.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt); define Windows-native equivalents in Stage 4. |
| Localization | **CI validators exist; desktop runtime coverage unverified.** Main window strings inspected are hardcoded English. | [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt), [Windows Desktop CI](../.github/workflows/windows-desktop.yml); identify reusable locale resources and verify actual Windows rendering. |
| Keyboard/mouse/focus, Enter/Escape, minimum size, high DPI and resize | **Runtime unverified.** Source audit does not establish usable focus order, shortcuts or behavior across display settings. | Run a Windows interactive QA pass and record each outcome; add host-specific behavior where missing. |
| Windows Debug update channel | **Source implemented; end-to-end runtime unverified.** Public prerelease discovery validates manifest channel and checksum shape; installer path exists in desktop host. | [WindowsDebugUpdateClient.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/update/WindowsDebugUpdateClient.kt), [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt); verify real update and failure paths without crossing into production releases. |
| MSI packaging and upgrade identity | **Build configuration and CI checks present; full lifecycle not established.** UpgradeCode is explicitly preserved. | [desktopApp/build.gradle.kts](../desktopApp/build.gradle.kts), [Windows Desktop CI](../.github/workflows/windows-desktop.yml); retain UpgradeCode and complete install/upgrade/recovery regression in Stage 5. |
| Shared deterministic rules | **Shared foundation and common tests exist; Windows end-to-end use is not established by the desktop entry point.** | [KMP Foundation Status](KMP_FOUNDATION_STATUS.md), [shared rules](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/rules/GuessRules.kt); connect the authoritative game flow and test integration. |

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
