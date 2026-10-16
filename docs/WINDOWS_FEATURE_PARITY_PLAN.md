# Windows Feature Parity Plan

**Status:** In progress; Stage 2-D candidate evidence recorded, Stage 2 exit gate remains open.
**Reviewed:** 2026-10-16
**Depends on:** [KMP Foundation Status](KMP_FOUNDATION_STATUS.md)

## Goal

**Product wish / locked direction: Windows is an exact port of GTN, built with Kotlin Multiplatform and shared code and assets wherever practical.** Android is the source of truth for the game. Windows must reproduce the same player-facing game, not a smaller desktop edition or a newly organized substitute UI.

Port the Android experience screen by screen: same features, screen hierarchy, information, labels, controls, menus, dialogs, navigation destinations, locked/unlocked states, loading/empty/error states, and interaction outcomes. Adapt only the presentation and input where desktop requires it (resizable window, keyboard, mouse, focus, hover and desktop conventions). Desktop adaptations must preserve the Android screen's content and meaning; any deliberate feature difference needs an explicit reason and documentation.

Prefer Kotlin Multiplatform shared production rules, models, validation, transaction logic, localization catalogs and canonical assets. Reuse Compose UI components where they work across targets, and use thin Android/Windows host adapters for platform-specific lifecycle, storage, audio, notifications, windowing, installers and updates. Share the original assets rather than creating separate Windows copies. Keep Android working throughout the port and preserve existing save compatibility.

This plan is the single source of truth for Windows parity and replaces Windows migration checklists that duplicate its stages.

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
- Do not create extraction-only PRs. Shared-code/UI work is in scope when it directly advances the exact Windows port, prevents duplicated behavior/assets or fixes a demonstrated cross-platform defect.
- Do not claim runtime verification from a successful build alone. Keep unrun checks marked unverified.
- Preserve offline play and existing save/MSI/update-channel compatibility; do not add ads, analytics or accounts.

## Definition of exact port

The port is complete only when every Android player-facing screen and reachable interaction has a Windows counterpart or is explicitly classified as an approved platform-specific difference. Matching only the feature list is not enough.

- [ ] Build a source-backed Android screen and route inventory, including nested destinations, menus, popups, dialogs, sheets, tutorials, feature-flagged entries and debug-only tools.
- [ ] For each screen, compare Android and Windows visible content, hierarchy, typography, spacing, icons, imagery, states, controls, actions and navigation. Record screenshot evidence at comparable window/content sizes where practical.
- [ ] Preserve every player-facing feature and the same underlying game rules, rewards, progression, localization meaning, save semantics and validation. Do not omit a screen because the current Windows shell has no route for it.
- [ ] Reuse shared Kotlin code and canonical assets first. Move a rule/model/resource to shared source sets only when its dependencies and platform contracts support that move; otherwise provide a narrow Windows adapter without duplicating authoritative game logic.
- [ ] Keep Android as the reference implementation. Every Windows port slice must identify the Android source screen/component it matches and check that Android behavior remains unchanged.
- [ ] Adapt mobile-only interaction patterns to desktop only where needed. Desktop navigation, keyboard shortcuts, hover and window resizing may differ in input mechanics, not in available functionality or meaning.
- [ ] Track parity per screen and interaction as **Verified**, **Not yet verified**, **Approved platform-specific difference** or **Blocked**, with evidence. A successful compile or unit-test run does not establish visual/runtime parity.
- [ ] Do not call Windows parity complete until all required rows have evidence and there are no unexplained missing screens, controls, menus or behaviors.
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

- [x] Guess input and Submit/Enter behavior use the production engine; range and whole-number validation are implemented. User-reported focused runtime checks passed for Enter and button submission.
- [x] Correct/incorrect rewards, attempts, streaks and Frenzy flow through the shared production engine.
- [x] Expose the four shared mutators on Windows, recalculate the range/target on toggle, and hide direction hints for Blind.
- [ ] Verify rate limiting and the full set of invalid, boundary, post-solve and mutator runtime cases.
- [x] Add 1/10/100/MAX quick-buy selection with exact combined costs and atomic bulk purchase transactions for Money, Prestige and Ultra upgrades.
- [x] Persist/reload a run through the desktop save adapter; user reported the focused restart/save check passed.
- [ ] Complete the remaining Android economy/catalog effect parity review and confirm current candidate CI/runtime checks.

**Exit:** The full core loop works on Windows and matches Android rules for deterministic cases.

### W2. Economy and resets

- [x] Prestige and Ultra flows enforce shared requirements/rewards and now require confirmation before reset.
- [x] Load talent names, descriptions, costs and prerequisites from the canonical Android talent catalog.
- [ ] Finish parity for remaining permanent Prestige/Ultra shop effects and confirm their Android behavior before enabling purchases.
- [ ] Verify large-number display and calculations across representative Android-matching cases.
- [x] Add deterministic exact-cost bulk-purchase tests and retain reset-preservation/duplicate-reward tests.
- [ ] Verify all reset preservation and boundary cases on the current candidate.

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

## Stage 2-D candidate evidence (2026-10-16)

The PR #202 candidate commit `38c7421c38b476c71e9baaca5cb7d4dc8c8cfea0` passed both required CI workflows:
- [Windows Desktop CI run 38840144782](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38840144782) — passed.
- [Android Validation and Tests run 38840144773](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38840144773) — passed.

The user reports passing focused Windows runtime checks for Enter-to-submit, button submission, shop/reward/progression behavior and restart/save persistence. These are recorded as user-reported checks, not independently observed runtime evidence. Broader W0/W1/W2 items (including resizing/high-DPI, localization, full economy parity, update channels and MSI lifecycle) are not thereby verified.

The detailed evidence log is [WINDOWS_STAGE_2D_PARITY_VERIFICATION.md](WINDOWS_STAGE_2D_PARITY_VERIFICATION.md). **Stage 2 remains open** until remaining W1/W2 parity gaps are reviewed and every required gate has evidence. Do not start Stage 3 implementation ahead of that gate.

### Stage 2 economy parity implementation candidate (2026-10-16)

Branch: [`feat/windows-stage-2-economy-parity`](https://github.com/ne0gl1tch20/GuessTheNumber/tree/feat/windows-stage-2-economy-parity).

This candidate closes several concrete gaps found during Stage 2-D review:
- exact-cost 1/10/100/MAX upgrade purchases for Money, Prestige and Ultra;
- a shared buy-multiplier state and atomic rejection when a fixed batch is unaffordable;
- Windows mutator controls using shared engine validation/range rules, including Blind feedback behavior;
- explicit confirmation before Prestige/Ultra resets;
- deterministic tests for bulk cost, MAX affordability and mutator validation/range behavior.

Follow-up source review replaced the hardcoded Windows talent list with the canonical Android talent catalog, including costs, descriptions and prerequisite IDs. It also fixed the desktop resource lookup to use the bundled `game/` directory and added the missing canonical talent, Prestige shop and Ultra shop JSON resources. Bulk purchase calculations now use Android's largest-affordable-prefix behavior for 10/100/MAX. These source changes are not yet validated by CI or new Windows runtime checks. The full Stage 2 gate remains open. Remaining review items include rate-limiting/edge-case parity, remaining Prestige/Ultra shop effect parity, broader large-number and reset checks, and actual Windows runtime evidence for mutators and bulk buys.

## Stage 1 source audit (2026-10-15)

This is a repository-source audit, not a runtime test. Paths below link to the code inspected on `master`.

### Confirmed source findings

- **Windows entry point and shell:** [\`desktopApp/.../desktop/Main.kt\`](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt) opens a resizable Compose Desktop \`Window\` with an initial 1100×760 dp size, a Windows-owned Play/About navigation shell, the shared guessing pilot and Windows Debug update controls. [\`DesktopNavigation.kt\`](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopNavigation.kt) defines the testable shell state/controller. The host still does not wire Android's production \`GameViewModel\`, full \`GameState\`, production navigation or save system into Windows.
- **Legacy shared pilot:** [`SharedGuessGameScreen.kt`](../shared/src/commonMain/kotlin/com/jarrlyyy/guessthenumber/shared/ui/SharedGuessGameScreen.kt) remains a demo surface. The Windows Play route has moved to the production `ProductionGuessGameScreen`, which calls the shared production `GameEngine` and persists `GameState` through the desktop adapter. The Play screen is still only a basic guess loop, not full gameplay/economy parity.
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
| Production game state, basic guess loop and save persistence | **Partially implemented; full parity not reached.** Windows Play uses the shared production `GameEngine`/`GameState` and desktop-native `DesktopSaveStore`; automated tests cover deterministic core transitions, round trips, backup recovery and failed writes. The UI currently uses save slot 1; the first economy slice, daily quest claims and treasure inventory are wired, while broader progression remains incomplete. | [ProductionGuessGameScreen.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/ProductionGuessGameScreen.kt), [DesktopSaveStore.kt](../shared/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/persistence/DesktopSaveStore.kt), [Stage 2-D verification](WINDOWS_STAGE_2D_PARITY_VERIFICATION.md); complete S2-B and run actual Windows runtime checks. |
| Desktop navigation shell | **Initial source implementation; runtime unverified.** Windows has Play and About destinations with a testable state controller. The production Android destinations are not yet implemented on Windows. | [DesktopNavigation.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopNavigation.kt), [DesktopNavigationTest.kt](../desktopApp/src/jvmTest/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopNavigationTest.kt), [Screen.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/Screen.kt); validate keyboard focus/navigation and expand destinations only as their features are implemented. |
| Upgrades, shop, Prestige, Ultra, talents and economy | **Core transaction slice implemented; runtime unverified.** The desktop UI loads Android JSON catalogs and saves accepted changes before publishing state. Auto Clicker, daily quests, treasure drops and once-per-day claims are wired. Nebula Crystal doubles future Nebula rewards from guess bonuses, daily quests and achievement milestones; Golden Guess Pass forces a critical reward through the shared engine on every tenth lifetime guess. Unsupported shop effects remain rejected. | [ProductionGuessGameScreen.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/ProductionGuessGameScreen.kt), [DesktopEconomyTransactions.kt](../shared/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/economy/DesktopEconomyTransactions.kt), [Stage 2 plan](../.openai/plans/windows-migration-stage-2-gameplay-economy.md). |
| Achievements, challenges, statistics, map, bosses, relics, Home Base, Codex, Rift and activities | **Achievement reward hooks and basic world unlock/select hooks implemented; full progression UI not wired.** Windows evaluates the existing 27 achievement milestones after guesses and accepted economy actions, pays each milestone once and checks Android world unlock requirements. Full world map, boss combat, relics, Home Base, Codex, Rift and activities remain Stage 3 work. | [ProductionGuessGameScreen.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/ProductionGuessGameScreen.kt), [DesktopEconomyTransactions.kt](../shared/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/economy/DesktopEconomyTransactions.kt), [NavGraph.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt). |
| Settings, locale, accessibility, save slots and recovery | **Not wired in inspected Windows entry point.** | [NavGraph.kt](../app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt); define Windows-native equivalents in Stage 4. |
| Localization | **CI validators exist; desktop runtime coverage unverified.** Main window strings inspected are hardcoded English. | [desktop Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt), [Windows Desktop CI](../.github/workflows/windows-desktop.yml); identify reusable locale resources and verify actual Windows rendering. |
| System theme and colors | **Interactive QA failed.** The user reported that Windows light/dark changes did not sync. The desktop implementation now polls the Windows personalization registry and keeps separate palettes; this code fix still needs interactive verification. | [DesktopTheme.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopTheme.kt), [DesktopThemePaletteTest.kt](../desktopApp/src/jvmTest/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopThemePaletteTest.kt); test switching while open, restart behavior and contrast on Windows. |
| Responsive shell layout and minimum window size | **Source configured; runtime unverified.** Initial size and 800×600 dp minimum are centralized in `DesktopWindowMetrics`; navigation switches to a compact header below the tested width breakpoint. Shared game content has its own responsive behavior. | [DesktopShellUiPolicy.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopShellUiPolicy.kt), [Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt), [DesktopShellUiPolicyTest.kt](../desktopApp/src/jvmTest/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopShellUiPolicyTest.kt); verify minimum sizing, resize transitions and high-DPI layout on Windows. |
| Focus indicators and update feedback | **Interactive QA found a contrast failure.** Focus outlines and Narrator status announcements were reported working, while the selected Play button's white text blended into its background. Navigation now uses explicit selected/unselected colors and selected semantics; re-test both palettes. | [Main.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt), [DesktopTheme.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopTheme.kt), [DesktopThemePaletteTest.kt](../desktopApp/src/jvmTest/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopThemePaletteTest.kt); verify visible contrast and assistive-technology announcements on Windows. |
| Keyboard navigation and shortcuts | **Interactive QA found an Escape failure.** Ctrl+1/Ctrl+2 and general focus behavior passed; Escape did not return to Play as expected. The key handler/focus ordering was adjusted and Escape now requests shell focus after handling; re-test Escape with and without a message, plus Enter activation. | [DesktopNavigation.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopNavigation.kt), [DesktopShellUiPolicy.kt](../desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopShellUiPolicy.kt), [DesktopNavigationTest.kt](../desktopApp/src/jvmTest/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopNavigationTest.kt), [DesktopShellUiPolicyTest.kt](../desktopApp/src/jvmTest/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopShellUiPolicyTest.kt); verify keyboard focus and actual key behavior on Windows. |
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

### Interactive Windows QA feedback (2026-10-15)

The user performed an interactive check on Windows 10 at 100% display scaling using build artifact SHA-256 `524158d9222d8aac9b9fa922fedec58969d54a0e80f4df6e036ccd9abe1abacf`. Reported 24/32 checklist items complete.

**Passed by user report:** launch/close, minimum sizing, resize/maximize, available scaling checks, initial keyboard focus, Tab movement, visible focus outline, Ctrl+1/Ctrl+2, mouse navigation, accessible names, Narrator updater status, compact navigation, updater loading/latest/error/available/recovery states, gameplay pilot input, About readability, regression pass.

**Failures requiring code fixes and re-test:**
- System light/dark theme did not sync as expected.
- Escape did not return from About to Play.
- Selected Play navigation button was unreadable because its white text blended into a white selected state.

**Still unverified:** explicit Enter activation and Escape behavior in each documented state; interactive theme switching and contrast after the fix. The supplied SHA-256 identifies the tested artifact, not a Git commit. These observations are runtime evidence and should not be marked fixed until the new build is tested.

### Stage 1 implementation progress

- [x] Add a Windows-owned destination/state contract for the initial Play and About shell.
- [x] Add unit tests for default destination, navigation/message clearing and blank-message handling.
- [x] Give the desktop window a resizable 1100×760 dp initial size.
- [ ] Run the desktop unit tests and compilation on Windows CI.
- [x] Configure system-following light/dark palettes with contrast regression checks.
- [x] Centralize initial/minimum window metrics and test the responsive navigation breakpoint.
- [x] Add shell Escape handling, preserve Enter for focused controls, and test shortcut/state mappings.
- [x] Add visible focus outlines and accessible live-region semantics for updater status.
- [x] Classify updater loading, error, update-available and up-to-date/empty states with unit coverage.
- [ ] Verify actual window launch/close, keyboard and mouse focus, minimum sizing, high-DPI and narrow layouts on Windows.
- [ ] Verify system theme switching, focus outlines and live-region announcements in an interactive Windows session.
- [ ] Audit locale loading and loading/error/empty states beyond the updater status.
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

## Stage 2-D follow-up review (2026-10-16)

This is a source and CI review, not a claim of interactive Windows runtime verification.

- **Basic production guessing and desktop persistence:** implemented in source. The Windows Play screen uses the shared production `GameEngine`/`GameState` and the JVM desktop save adapter; the current UI uses slot 1.
- **Automated parity evidence:** Android's existing `GuessCoreStateParityTest` checks deterministic core state transitions. Desktop persistence tests cover round-trip, corrupt-primary backup recovery, legacy JSON defaults and injected write failure. The Stage 2-D branch adds an engine-plus-save/reload integration test.
- **Current-candidate checks:** follow the CI links in the Stage 2-D PR and report. Successful compilation alone is not runtime evidence.
- **Stage 2-B update:** PR #198 adds catalog-backed upgrade purchases, Prestige/Ultra resets and upgrades, Nebula shop purchase records and talent unlocks with transaction tests. Remaining gaps include confirming all item effects against Android (notably auto-clicker/background behavior and shop boosts), expanded reward fixtures and actual Windows runtime testing.
- **Still outstanding:** actual Windows input/window/restart testing and complete UI/localization parity. Do not mark Stage 2 complete or start Stage 3 implementation until the full Stage 2 exit gate is met.