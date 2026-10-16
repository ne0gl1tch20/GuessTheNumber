## Standing Changelog Rule (2026-10-13)

- Whenever a code, UI, gameplay, localization, configuration, or other player-facing app change is implemented, update `app/src/main/assets/changelogs.md` in the same change/PR so the changelog does not fall behind.
- Describe only changes actually implemented; keep planned work in the Joy Phase roadmap, not in completed/released changelog entries.
- Preserve the current app version unless a version bump is explicitly part of the release work. Do not imply a build or tests passed without verified results.
- The user confirmed PR #36 (undocumented changes changelog update) is merged.

## Windows exact-port requirement (2026-10-16)

- User's locked goal: Windows should be an exact port of GTN using Kotlin Multiplatform, shared code and shared assets wherever practical.
- Android is the reference for screens, content hierarchy, controls, menus, dialogs, navigation, states, feature availability and game behavior. Desktop-only input/windowing adaptations are allowed; missing or redesigned features are not.
- Prefer shared production rules/models, UI components, localization catalogs and canonical assets. Keep OS-specific lifecycle, storage, audio, notification, installer and update behavior in host adapters.
- Audit Android vs Windows screen-by-screen and interaction-by-interaction. Record visual/runtime evidence; compilation and CI do not prove exact UI parity.
- docs/WINDOWS_FEATURE_PARITY_PLAN.md is the single source of truth for execution and acceptance criteria. Do not maintain duplicate Windows migration plans under .openai/.
- Keep Android working and preserve save, reward, localization and offline behavior during the Windows port.
# Project Memory: Gamified Guess the Number Simulator (v3.0.0 Update)


## Developer Console and Diagnostics Update (2026-10-10)
- Added a registry-backed developer command autocomplete layer in `domain/command/CommandAutocomplete.kt`, including `DevCommand`, `CommandParser`, `CommandRegistry` and `CommandAutocomplete`. Suggestions include command descriptions and argument choices instead of being hard-coded in the screen.
- Added real debug commands: `/autoclicker on|off`, `/setlevel <upgradeId> <level>`, `/newnumber`, `/state`, `/inspect`, `/version`, `/logs clear|pause|resume`, plus `/unlock all` and `/completeguess` aliases. `/help` documents them.
- Dev Settings now has a persisted `verboseLogging` switch, severity filters, matching-entry count, and log entries show event names and correlation IDs. Existing developer routes remain gated to debug builds.
- `GameLogger` defaults verbose traces off, accepts TRACE/DEBUG only when verbose mode is enabled in debug builds, and limits release logging to WARN/ERROR/FATAL. File log storage is debug-only. Guess processing logs begin/rate-limit/result events with a correlation ID.
- Added unit tests for command autocomplete and parsing in `app/src/test/java/com/jarrlyyy/guessthenumber/domain/CommandAutocompleteTest.kt`.
- **Verification status:** changes were inspected through GitHub file fetches, but the Android build and unit tests have not been run. Automatic builds on push are disabled to conserve Actions resources; do not start a manual workflow unless explicitly requested.


## Project Overview

**Current player-facing pitch:** Start with a simple number guess, then build a run around upgrades, Frenzy streaks, random events, achievement tiers, cosmetics, stackable mutators, custom challenges, world progression, boss battles, and endgame challenges. The current app metadata is **v3.0.2 (versionCode 20)**. 🎮✨

A feature-rich Android incremental/arcade game built with Kotlin and Jetpack Compose, including classic guessing, idle upgrades, Prestige/Ultra/Nebula progression, anti-cheat/time-travel protections, offline progress, JSON-driven content, 10 save slots, encrypted backup/restore, crash recovery, developer console, player-selected background music, tutorial onboarding, Material 3 UI, world progression, boss battles, relics, Home Base, Codex, and Endless Rift. Current app version: `v3.0.2` (versionCode 20).



## v3.0.0 Beyond the Numbers Ultimate Expansion
- Current app metadata in `app/build.gradle.kts`: versionName `3.0.2`, versionCode `20`. v3.0.2 records the merged PR #24 streak-milestone celebration feature; no build/test result should be inferred from the version bump alone.
- Added a save-compatible Endless Rift loop, unlocked only after all four world bosses are defeated. Rift tier, best cleared tier and active state are persisted in `GameState` with defaults for old saves.
- Rift runs rematch bosses in a repeating four-world order. Each victory scales the next boss by +2 HP per tier, multiplies that victory's Money and Nebula rewards by the current tier, records the best cleared tier and automatically advances to the next world.
- The World Map displays tier-scaled boss HP and the active Rift tier, and uses the existing guessing combat, phase rules and miss penalties for Rift rematches.
- Endgame Challenges now starts or leaves the Rift and displays current/best tier. Navigation calls the same ViewModel progression action handler used by other progression screens.
- Added the eight Rift UI strings to all 23 locale catalogs (Arabic, German, English UK/US, Spanish, Filipino, French, Hindi, Indonesian, Italian, Japanese, Korean, Dutch, Polish, Portuguese Brazil/Portugal, Russian, Thai, Turkish, Ukrainian, Vietnamese, Simplified Chinese and Traditional Chinese).
- Changelog updated for v3.0.0. The v3.0.1 cleanup removes the redundant More Hub tutorial and export/import entries and their standalone destinations; action-driven tutorial and Save Slots backup/restore remain available. Build/runtime validation must be confirmed from GitHub Actions before describing a commit as build-verified.

## v2.0.0 Beyond the Numbers Expansion
- Historical v2.0.0 release metadata: versionName `2.0.0`, versionCode `17`.
- Added backward-compatible per-save fields: `relicInventory`, `equippedRelicIds`, `worldMasteryLevels`, `discoveredSecretIds`, `codexEntries`, `homeBaseLevel`, `activeBossBattleWorldId`, `bossBattleMistakes`, and `codexRewardClaimed`.
- Added `WorldMapScreen.kt`: pinch/gesture zoom, drag panning, clickable world nodes, unlock requirements, secret-area markers, boss health, phase display and mastery upgrades.
- Boss combat is guessing-driven: starting a battle makes correct manual or auto-clicker guesses damage the active boss. Misses push progress back every 4 misses in phase 1, every 3 in phase 2 and every 2 in phase 3. World mastery adds 2% correct-guess Money per level, up to 20%. Boss victories grant their one-time Money/Nebula reward, relic, Codex entries and initial world mastery.
- Replaced the flat Talent screen with a zoomable connected skill tree. Talent purchases now check parent prerequisites and persist through the prestige currency. Speed talents boost auto-clicker speed; precision talents add critical reward chance; Nebula Resonance adds 50% correct-guess Money; Cosmic Oracle adds one Nebula per correct guess.
- Added `ProgressionScreens.kt` with Relics, Home Base, Explorer's Codex and Endgame screens. Up to two relics can be equipped. Relics add correct-guess bonuses; each Home Base level adds 2% Money bonus up to 40%. Codex completion grants 100 Nebula once all 12 entries are discovered.
- Added direct More-menu shortcuts and navigation destinations for World Map, Relics, Home Base, Codex, Endgame, Cloud Backup, Lucky Stake and Tutorial. Debug builds also expose Process Inspector. Lucky Stake now settles currency changes through the ViewModel.
- Integrated a Beyond the Numbers progression card at the top of Play, showing live unlocked-world and defeated-boss counts and direct shortcuts to World Map, Relics, Home Base and Codex. `PlayScreen` routes those actions through `NavGraph` instead of leaving progression discoverability only in More.
- Locale keys for the Play progression hub (`beyond_numbers_title`, `world_progress_summary`) are present in all 23 locale JSON files with localized values; existing progression strings continue to use the locale catalog and fallback system.
- Changelog v2.0.0 is recorded in `app/src/main/assets/changelogs.md`.
- Validation status for the Play hub integration: source and locale updates were committed directly to `master`. GitHub Actions builds were triggered for the two Kotlin integration commits and were still in progress at the latest check; no emulator/runtime verification has been performed in this pass.

## Strict Project Rules: Localization & Open-Source Licenses
- **EVERYTHING USER-VISIBLE MUST BE LOCALIZED**: Zero hardcoded user-visible strings (screen titles, buttons, dialogs, toasts, accessibility descriptions, achievements, upgrades, shop items, settings, dynamic text formatting with `%s`/`%d`) in Kotlin/Compose UI. All strings must reside in `assets/locales/en_us.json` and be fetched via `LocaleManager`.
- **LICENSE ATTRIBUTION**: Open-source license attribution must accurately reflect actual project dependencies.

## Architecture & Package Structure (`com.jarrlyyy.guessthenumber`)
- **`domain.model`**: `BigNumber`, `GameState`, `GameStatistics`, `GameSettings`, `UpgradeDef`, `ShopItemDef`, `AchievementDef`, `LogEntry`.
- **`domain.engine`**: `GameEngine`, `AntiCheatService`, `RngService`.
- **`domain.command`**: `CommandExecutor`, `DeveloperConfig`.
- **`data.store`**: `SaveManager`, `SaveSerializer`, `SaveValidator`, `SaveMigration`.
- **`data.repository`**: `JsonConfigRepository` (`game_config.json`, `upgrades.json`, `shop_items.json`, `prestige_upgrades.json`, `prestige_shop.json`, `ultra_upgrades.json`, `ultra_shop.json`, `achievements.json`, `minigames.json`, `challenges.json`, `locales/en_us.json`).
- **`data.logger`**: `GameLogger`, `LogLevel`, `LoggerCategory` (21 categories) with timestamped format `[HH:mm:ss.SSS][CATEGORY][LEVEL] message`.
- **`data.crash`**: `AppErrorHandler`, `CrashHandler`.
- **`data.notification`**: `NotificationHelper`, `GameReminderWorker`, `BootReceiver`.
- **`widget`**: `GuessWidgetReceiver`, `GuessWidget`, `QuickGuessActionCallback` (Jetpack Glance).
- **`ui.navigation`**: `NavGraph`, `Screen`.
- **`ui.theme`**: Material 3 `Theme`, `Color`, `Typography`.
- **`ui.screens`**: `PlayScreen`, `UpgradeScreen`, `ShopScreen`, `MoreScreen`, `PrestigeScreen`, `UltraScreen`, `ArcadeScreen`, `StatsScreen`, `SettingsScreen`, `AboutScreen`, `DevSettingsScreen`, `CrashRecoveryScreen`, `TalentScreen`, `MutatorsScreen`, `StakingScreen`, `SaveSlotsScreen`.
- **`ui.viewmodel`**: `GameViewModel`.

## Advanced Features Implemented in v1.10
1. **Comprehensive Internationalization & Localization (`v1.9`)**: 100% of user-visible UI text, screen titles, dialogs, buttons, toasts, and data-driven config definitions (`upgrades.json`, `shop_items.json`, `achievements.json`, `prestige_shop.json`, `ultra_shop.json`, `minigames.json`, `mutators.json`, `challenges.json`, `talents.json`) are fully localized and managed via `LocaleManager` and `en_us.json`.
2. **Dynamic Math & Clue Analysis (`v1.9`)**: Guess feedback tips and math clues dynamically inject real-time game state variables (midpoints, parities, deltas, interval bounds, moduli).
3. **3 Distinct Save Slots System (`SaveSlotsScreen`, `SaveManager`)**: Manages Slot 1, Slot 2, and Slot 3 independently with separate preferences keys, metadata summaries, switching, and reset actions.
4. **Legacy Save Migration & Safety Prompt**: Detects legacy single-save data and prompts the user to transfer it into Slot 1. If declined or dismissed without confirmation, the app closes immediately (`System.exit(0)`) to protect user data.
5. **Advanced Idle Automation & AI Guessing Bots**: Autonomous background guessing bots with adjustable speed and auto-clicker optimization.
6. **Prestige Talent Web / Skill Tree**: Interconnected Prestige talent nodes (`TalentScreen`) providing passive velocity, critical, and reward multipliers.
7. **Interactive Android Home Screen Widgets (Jetpack Glance)**: Glance-based app widgets (`GuessWidget`) supporting real-time currency display and quick-guess action triggers.
8. **Gameplay Mutators & Daily Seeded Challenges**: Customizable difficulty mutators (Hardcore, Hyper Speed, Blindfolded) and daily seeded challenge claims (`MutatorsScreen`).
9. **Weekly Challenges**: Weekly challenge sets rotate through varied goal types including correct guesses, streaks, money, guess volume, Prestige, Ultra, Frenzy, and playtime, with week-scoped claim IDs and varied Nebula rewards.
10. **Random Events**: Occasional varied events trigger about once per minute check with a 35% roll, offering Cash Burst, Nebula Rain, Jackpot, Streak Surge, Range Shuffle, Tax Refund, or rare Cosmic Gift effects with localized notices.
11. **Achievement Tier System**: 27 localized achievements across Bronze, Silver, Gold, and Diamond tiers, with milestone-based unlock checks and escalating Nebula rewards.
12. **Combo / Frenzy System**: Consecutive correct guesses retain the existing streak combo and unlock Frenzy tiers every 5 correct guesses, adding a 25% reward multiplier per tier up to 5x.
13. **High-Stakes "Lucky Guess" Staking**: High-stakes 50/50 staking mode (`StakingScreen`) with streak multiplier bonuses.
10. **Save Export/Import & Cloud Backup UI**: Dedicated Cloud Backup and encrypted save transfer UI (`CloudBackupScreen`).
11. **GitHub Actions APK Build Pipeline (`.github/workflows/build-apks.yml`)**: Manual APK build workflow running on `ubuntu-latest` through `workflow_dispatch` only that sets up JDK 17, sets up Gradle with caching, executes `./gradlew assembleDebug` and `./gradlew assembleRelease` (producing unsigned release APK), and uploads both APK variants (`GuessTheNumber-debug` and `GuessTheNumber-release`) as GitHub Actions artifacts.

## v1.12.1 Combined Implementation

- App version is now `versionCode 12` / `versionName 1.12.1`.
- Save Slots is the first-launch destination when no valid save exists; encrypted Import/Export is placed directly on the Save Slots screen.
- Encrypted export now creates one backup bundle containing global App Preferences, all 10 save slots, and slot backups. Import restores the complete bundle.
- App settings are stored in a dedicated global App Preferences DataStore rather than individual save slots.
- Existing per-save settings are detected for migration and shown in a one-time confirmation/loading popup before being moved to global preferences.
- New save creation supports name, icon, and persistent per-save difficulty.
- New saves use an interactive action-driven tutorial.
- Theme settings now include presets, custom RGB color picking, and global custom primary/secondary/tertiary colors.
- More screen shortcut order is globally persisted and can be changed with long-press drag-and-drop.
- Developer JSON editing is split into global App Preferences and Save Slots with a Save 1–10 selector.
- CI remains manual-only; no automatic build was started as part of this implementation.

## v1.14.0 Global Localization & Internationalization
- App version is now `versionCode 14` / `versionName 1.14.0`.
- Added 23 supported locale overlays with English fallback.
- LocaleManager now resolves BCP-47 tags, merges locale overlays over English, detects RTL languages, and performs locale-aware number/date/currency/percent formatting.
- Settings exposes the complete supported locale registry.
- Android per-app language configuration is declared for Android 13+.
- Added JSON asset validation, localization surface auditing, locale contract validation, and LocaleManager unit tests.
- Manual CI now runs localization validation, JSON validation, unit tests, lint, full Gradle check, and report uploads.
- Full repository scanning remains an acceptance criterion: Kotlin, XML, JSON, Markdown/text assets, locale assets, game data, tests, and workflow/configuration files must be inspected during the de-hardcoding pass.
- CI remains manual-only.

## v1.16.0 Expressive Motion & Gameplay Feedback
- Added four persistent worlds: Verdant Grove, Crystal Caverns, Ember Summit, and Nebula Rift. Each has its own boss, requirement gate, and one-time victory reward.
- Boss encounters are multi-hit: 3 hits, 5 hits, 7 hits, and 10 hits by tier. Boss damage persists per save slot; victories reward increasing Money and Nebula.
- World unlock gates require the prior boss plus correct-guess milestones, upgrade levels, Prestige and Ultra milestones. All world/boss UI strings and boss HP labels were added across all 23 locale files.
- Added collectible Treasure Drops: correct guesses have an 18% Common, 5% Rare, and 1% Epic chest drop chance. Chest counts are saved per slot; opening a chest awards either Money or Nebula based on tier.
- Play screen now shows chest inventory, tier-specific reward values, total drops found, opened count, and latest drop tier.
- Added 14 treasure-drop localization keys to all 23 locale JSON files: Arabic, German, English UK/US, Spanish, Filipino, French, Hindi, Indonesian, Italian, Japanese, Korean, Dutch, Polish, Portuguese Brazil/Portugal, Russian, Thai, Turkish, Ukrainian, Vietnamese, Simplified Chinese, and Traditional Chinese.
- Added a persistent Daily Bounty Board to the Play screen with four goals: make 10 guesses, get 5 correct guesses, reach a 5-correct streak, and buy upgrades 3 times.
- Bounties grant claimable Money or Nebula rewards; progress and claims are saved per game slot and reset when the local calendar day changes.
- Added English and Filipino localization for all bounty-board strings. Other locale overlays use the existing English fallback until translated.
- App version is now `versionCode 16` / `versionName 1.16.0`.
- Added an expressive save-loading screen connected to the actual save-switch loading state.
- Expanded spring-based transitions, animated card/content changes, tactile press feedback, and gameplay result animations across Play, Upgrades, Shop, Prestige, Ultra, Achievements, Arcade, Save Slots, guided tutorial, crash recovery, and navigation.
- Added celebration banners for correct guesses, achievement unlocks, Prestige resets, and Ultra resets.
- New motion effects respect the existing reduced-motion preference.
- The changes were merged into `master` through PR #2.
- CI workflows remain manual-only; no build was started as part of the version/changelog update.

## v1.15.0 Expressive More Menu & Persistent Reordering
- App version is now `versionCode 15` / `versionName 1.15.0`.
- More shortcut order is persisted as a stable default and now drives the actual More menu rendering.
- More reorder UI received additional Material 3 Expressive surfaces, shapes, drag elevation, and clearer reorder-state messaging.
- What's New popup copy is fully locale-backed with a `%s` version placeholder and no hardcoded release copy in NavGraph.
- LocaleManager tests cover localized What's New formatting.

## v1.13.0 Material 3 Expressive UI Overhaul
- App version is now `versionCode 13` / `versionName 1.13.0`.
- The approved 15-part Material 3 Expressive UI plan was combined into one implementation pass.
- Added centralized expressive typography, shapes, spring motion primitives, screen entrance/exit transitions, and animated bottom-navigation selection.
- Applied shared expressive screen motion to Play, Upgrades, Shop, and More without changing game logic, save behavior, localization, tutorial state, or offline architecture.
- Connected the existing global Reduced Motion setting to the new screen/navigation animations.
- CI remains manual-only.

## v1.12.1 UI/UX Polish Pass
- Theme Presets were removed from More and consolidated into Settings as a polished dropdown with a Custom option.
- Custom theme colors now use an HSV-style hue/saturation/brightness workflow plus live preview and hex entry for Primary, Secondary, and Tertiary colors.
- More menu reordering remains gated behind Reorder/Done and now has lifted scale/elevation and content-size motion while dragging.
- The old full-screen generic tutorial flow was replaced with an in-app guided tutorial overlay that requires real actions in Play, Upgrades, More, and Settings.
- Guided tutorial copy is localized in English and Filipino and tutorial completion remains global so new save slots do not restart onboarding.
- CI remains manual-only.

## v1.12 Phase 13–14
- **Phase 13 — Shop Categories & Cosmetics**: Added category and cosmetic metadata to ShopItemDef, categorized the Nebula Shop into All / Automation / Boosts / Cosmetics, and added three localized cosmetics (cosmetic_nebula_ring, cosmetic_starfield, cosmetic_pixel_glow).
- Cosmetic purchases persist in shopPurchases and the currently equipped cosmetic is stored in equippedCosmeticId.
- **Phase 14 — Mutator Combinations & Challenge Builder**: Added persistent activeMutators state and stackable mutator multipliers. Hardcore doubles the configured range, Hyper Speed triples effective auto-clicker speed, Blindfolded Oracle hides directional feedback, and Heavy Taxation applies its existing 5% wrong-guess penalty.
- Added a Challenge Builder to MutatorsScreen with selectable mutator combinations, Correct / Streak / Guesses / Money goal types, editable targets, calculated Nebula rewards, activation, and one-time challenge claims.
- Added English and Filipino localization for all new shop/cosmetic/challenge-builder UI text.
- Added unit tests for combined mutator multipliers and Hardcore range scaling.
- App version bumped to versionCode 11 / versionName 1.12.
- Phase 15 adds full validation and migration coverage for the v1.12 state shape, including mutator sanitization, cosmetic ownership/equip state, challenge claim persistence, localization validation, and manual CI build verification.
- Phase 15 implementation is complete. CI remains manual-only via GitHub Actions `workflow_dispatch`; the next build should be used to verify the current v1.12 code after the latest UI and documentation polish.

## v1.10 Maintenance Notes
## Phase 6 Gameplay Mechanics
- Added active difficulty tuning to the guessing engine.
- **Classic**: range 1–100, no difficulty penalty, 1× reward multiplier.
- **Hard**: range 1–150, 2% money loss on incorrect guesses, 1.5× reward multiplier.
- **Extreme**: range 1–250, 5% money loss on incorrect guesses, 2.5× reward multiplier.
- Difficulty range scaling respects the existing **Better Range** upgrade.
- Save sanitization and Prestige reset now preserve the selected difficulty's range.
- Added unit coverage for difficulty penalties, rewards, and range behavior.
- Phase 6 changes were committed without starting a new APK build to conserve CI usage.
- Phase 7 implemented a feedback-driven `GuessingBot` using binary search. The auto-clicker now guesses without reading the hidden target and updates its search bounds from TOO_LOW / TOO_HIGH / CORRECT feedback.
- Added `GuessingBotTest` coverage and localized the auto-clicker description in English and Filipino.

- Fixed `AboutScreen` open-source license attribution formatting so author and license placeholders render their actual values.
- Added Phase 5 slot duplication and backup restoration workflow.
- Updated app version metadata to `versionCode 9` / `versionName 1.10`.
- Changelog entries continue using the existing Markdown heading and emoji-bullet format.

## Beyond the Numbers: Roadmap to 1.0 Completion (2026-10-10)

**The finish line is Phase 6 below. Do not treat small commits, plans, or a single successful build as project completion.** Work through phases in order, bundle related changes into meaningful commits, and verify claims against code and GitHub Actions.

### Phase 1 — Establish the real baseline
- Inspect current `master`, the latest workflow runs, integration audit, and relevant source/tests.
- Record concrete remaining blockers tied to actual files and behavior.
- Fix any build or test failures before expanding features.
- No emulator tests unless the user explicitly authorizes them.

### Phase 2 — Finish RPG integration
Verify the full progression loop end to end: World Map, world unlocks and portals, bosses and combat, secret-area discovery/interactions, relic inventory/equipment/set bonuses, world mastery and milestone rewards, Home Base upgrades/discounts, Codex completion, Endless Rift, Prestige, Ultra, Nebula, and upgrade progression.
- Confirm unlock requirements, reward ownership, persistence, save compatibility, and localization.
- Prevent duplicate reward application, inconsistent costs, progression dead ends, and disconnected systems.

### Phase 3 — Close the localization audit
- Audit every user-visible surface: Compose screens, dialogs, toasts, errors, accessibility labels, notifications, widgets, dynamic strings and formatting placeholders.
- Add required keys to the canonical English catalog and all 23 locale catalogs; use real translations where available and preserve placeholder contracts.
- Fix language-switch updates and review hardcoded-text audit findings.
- Exit only when locale validation passes, all catalogs have the required keys, and remaining audit findings are reviewed rather than ignored.

### Phase 4 — Complete shared app systems
- Music: local offline library, metadata, reusable full-screen player, saved playlists, playlist editing, queue/reordering, shuffle, repeat, playback controls, service/media-session/notification integration and failure recovery.
- Notifications: real progression/game events, preference switches, channels, scheduling/reboot restoration and correct cold/warm navigation.
- Widgets: accurate persisted game metrics, save-slot selection/cycling, useful layouts and refresh after relevant state changes.
- Save slots, encrypted import/export, settings and restoration must work together without breaking old saves.

### Phase 5 — Finish UI and gameplay polish
- Complete navigation, animations/transitions, touch feedback, responsive layouts, loading/empty/error states, accessibility labels and theme/language behavior.
- Balance progression costs/rewards and remove known gameplay inconsistencies.
- Do not expand scope with unrelated features while earlier-phase blockers remain; defer new ideas to post-1.0.

### Phase 6 — Release verification (THE FINISH LINE)
- On the final source commit, run repository validation, localization checks, unit tests, lint, full Gradle checks, and Debug and Release APK builds.
- Fix failures and rerun relevant checks; inspect workflow conclusions and generated artifacts.
- Confirm the integration audit has no unresolved release blockers.
- Declare 1.0 completion only when all required checks pass on the final commit and both APKs build successfully. A build pass alone does not prove all features are integrated.

### Execution and reporting rules
1. Continue implementation without repeatedly asking the user to approve already-authorized work.
2. Prefer GitHub repository tools and conserve Composio quota.
3. No emulator tests until explicitly approved.
4. Do not claim a workflow passed until its actual conclusion is `success`; distinguish pending, cancelled, failed and successful runs.
5. Keep progress reports short and evidence-based, with commit and Actions links for repository changes.
6. For repository changes, include: “You can build it using GitHub Actions” with https://github.com/ne0gl1tch20/GuessTheNumber/actions/workflows/build-apks.yml.
7. The roadmap is an ordered completion gate, not permission to stop after documenting it. Continue making integrated code changes toward Phase 6.



## Versioned Content, Feature Flags, LiveOps and Validation (2026-10-10)
- Bundled game JSON assets now carry `schemaVersion: 1` (worlds already had it; LiveOps has both `schemaVersion: 1` and independent `manifestVersion: 2`). Config loaders reject unsupported schema versions and fall back to safe defaults/empty lists, with `CONFIG_VALIDATION_FALLBACK` warning logs.
- Added `data/repository/ContentValidation.kt` for unique/nonblank IDs and validation of upgrade costs/multipliers, shop prices, achievement tiers/rewards, challenge rewards, minigame multipliers, and talent parent references. `JsonConfigRepository` applies these checks before exposing content. Game config validates range, critical chance, caps and numeric reward/income strings.
- Fixed feedback-message config compatibility: `guess_feedback_messages.json` stores counts (30 low, 30 high, 55 tips), so the loader now expands the configured counts into localized message-key lookups rather than rejecting the asset.
- Added `feature_flags.json` and `FeatureFlagRepository`. Current flags: `live_ops`, `seasonal_rewards`, `world_map`, `challenge_builder`, `dev_console`. Flags filter the More menu and are enforced at NavGraph destinations; LiveOps claims also check the flags.
- LiveOps event data now declares `rewardNebula`, `permanentBoostPerClaim`, `minCorrectGuesses`, `minPrestigeCount`, and `minUltraCount`. The repository validates schema/manifest versions, unique IDs, status, timestamps, reward values and eligibility thresholds. UI displays requirements and rewards; the ViewModel rechecks eligibility and uses configured values at claim time. Holiday Neon Rush requires 100 correct guesses.
- Added tests: `ContentValidationTest`, `JsonConfigRepositoryTest`, `LiveOpsRepositoryTest`; existing `WorldConfigRepositoryTest` and `GameEngineTest` cover world progression and game rules.
- **Verification:** repository source and bundled JSON were inspected, including IDs/schema versions/reward values. Android build and unit tests have not been executed. Automatic push builds remain disabled to conserve GitHub Actions minutes; do not launch a workflow unless the user asks.


## Joy Phase Roadmap and LiveOps Temporary Locale Packs (2026-10-12)

- The joy-focused roadmap now covers PRs #24–#80. PR #24 is merged; #25 is the active roadmap PR, and the remaining entries are planned work subject to implementation and validation.
- PR #24: merged winning-streak milestone celebrations at streaks of 5, 10, 25, and 50. The correct-guess banner reuses existing localized achievement-title keys. Merge status is confirmed; build/runtime test status must be checked separately.
- PR #25: improve high/low guess feedback.
- PR #26: make correct guesses feel more satisfying.
- PR #27: improve Money and upgrade purchase feedback.
- PR #28: celebrate Prestige, Ultra, and Nebula milestones.
- PR #29: improve achievement unlock celebrations.
- PR #30: make random events feel more exciting.
- PR #31: improve early-game goals and progression.
- PR #32: add math strategy and number mastery. Build on existing high/low clues and dynamic math analysis so players can narrow ranges efficiently using interval bounds, midpoints, parity, deltas, and other relevant number patterns. Add optional guided examples/challenges that encourage fewer guesses and clear reasoning without exposing the hidden target, forcing school-style tests, or changing core fairness/reward rules. Localize all explanations and test calculations against the real guessing rules.
- PR #33: add requirement-based unlocks to the More Hub. Prefer showing ordinary progression-locked entries with a lock indicator; tapping one opens a localized panel showing all unmet requirements and current progress. Hide only debug-only or explicitly disabled/unavailable entries. Keep progression requirements separate from feature flags, recheck unlocks when navigating/performing actions, preserve saved shortcut order, and never expose debug-only routes in release.
- PR #34: localize Lua minigame/activity payload text. Inspect the current Lua engine, payload validator, and locale resolver before choosing the key-based payload schema.
- PR #35: localize Lua random events and related messages.
- PR #36: test Lua localization across supported locales, language switching, missing keys, and fallback behavior.
- PR #37: define and integrate LiveOps-delivered temporary locale packs so translations for limited-time activities can update without an APK release.
- PR #38: validate, cache, expire, replace, and recover temporary locale packs, including offline fallback and malformed-pack rejection.
- PR #39: final joy-phase gameplay, localization, LiveOps, save-compatibility, and regression review.
- PRs #40–#45: staged C++/NDK integration: #40 architecture audit and baseline; #41 NDK/CMake and native test scaffold; #42 narrow JNI bridge; #43 port one bounded deterministic engine computation; #44 Kotlin/native parity and failure tests; #45 native integration, ABI, regression, and measured-performance review.
- Temporary locale packs are translation data only, never executable Lua or remotely downloaded code. Kotlin remains authoritative for locale resolution, schema validation, lifecycle, persistence, rewards, and eligibility; Lua stays sandboxed and returns approved localization keys/arguments.
- Locale packs should declare locale, pack ID/version, expiry, and translated key/value strings. Validate schema, allowed keys, payload size, and expiry before activation. Cache accepted packs for offline use; when missing, invalid, or expired, fall back to the built-in locale and existing locale fallback rules. Keep built-in translations untouched.
- The installed app must first contain the downloader, validator, cache/expiry handling, and locale resolver; LiveOps cannot add those capabilities to an older installed APK by itself.
- Existing feature_flags.json/FeatureFlagRepository gates are not the same as player progression unlocks. The More Hub unlock work must model and display requirement progress separately rather than treating a hidden/disabled feature flag as a locked progression feature.
- Preserve offline operation and existing saves/gameplay. Avoid unrelated dependencies or scope expansion. CI/build verification must be checked from GitHub Actions before claiming success; workflows are manual-only unless the user explicitly asks to start one.


## Math Strategy and Number Mastery (2026-10-12)

- The user wants GuessTheNumber to be more than repeatedly guessing: gameplay should help players use numbers efficiently and apply mathematical reasoning.
- Planned PR #32 builds on existing high/low feedback and dynamic math clues (interval bounds, midpoint, parity, deltas, modulus where relevant) to teach efficient narrowing of the search range through optional examples and challenges.
- Reward efficient reasoning/fewer guesses without revealing the hidden target, forcing school-style tests, or changing core fairness/reward rules. Localize every player-facing explanation and validate calculations against the actual engine rules. Inspect existing mechanics first to avoid duplicating features that already exist.

## Planned C++/NDK Engine Integration (2026-10-12)

- The user wants C++/NDK engine integration tracked in the GuessTheNumber roadmap as a planned workstream.
- Roadmap PRs #40–#45 follow the joy, math strategy, More Hub unlock, Lua localization, and LiveOps locale-pack phases (#24–#39): #40 architecture audit and baseline; #41 NDK/CMake and native test scaffold; #42 narrow JNI bridge; #43 port one bounded deterministic engine computation; #44 Kotlin/native parity and failure tests; #45 native integration, ABI, regression, and measured-performance review.
- This is a staged integration, not a full rewrite. Kotlin remains authoritative for app lifecycle, game state, persistence, UI, localization, LiveOps networking, eligibility, and rewards. Native code must not become a competing source of truth.
- Before choosing what to port, inspect the current engine architecture and capture baseline correctness/performance measurements. Port only a bounded component with a clear reason to be native; do not claim speed gains without measured evidence.
- JNI must validate inputs, handle errors explicitly, and allow a Kotlin fallback. Test supported ABIs, Debug/Release builds, parity, edge cases, and native failure recovery.
- Preserve the Lua sandbox. Lua must not receive direct JNI/native access or unrestricted save/reward mutation capabilities.
- Confirm actual Android NDK/CMake and ABI configuration during implementation rather than assuming a target ABI. Avoid unrelated dependencies and preserve existing saves, offline operation, and gameplay.

## Current Release and Work State (2026-10-12)

- Current app version is **3.0.2**, versionCode **20**, in `app/build.gradle.kts`. This patch version records the already-merged PR #24 winning-streak celebration behavior, which was missing from the previous changelog. Release notes are in `app/src/main/assets/changelogs.md`; do not claim build, unit-test, or device-test success unless GitHub Actions/results confirm it.
- PR #24 is merged. At streaks of 5, 10, 25, or 50, the correct-guess celebration changes to an achievement-style banner and uses existing localized achievement title keys.
- The active roadmap document is `docs/JOY_PHASE_ROADMAP.md` on branch `docs/build-channel-updater-roadmap`, associated with PR #25: https://github.com/ne0gl1tch20/GuessTheNumber/pull/25. The roadmap covers PRs #24–#80. PR #25 is the high/low feedback work; the rest are plans until implemented and verified.
- The current branch now contains roadmap, memory, changelog, app version metadata, and integration-audit version-reference updates. No build, CI workflow, emulator, or physical-device test has been started in this pass.
- Planned PR details (all remain plans unless implemented and verified):
  - **#25–#33 gameplay:** #25 clearer high/low feedback; #26 correct-guess feel; #27 Money/upgrade feedback; #28 Prestige/Ultra/Nebula milestone celebrations; #29 achievement unlock presentation; #30 random-event presentation; #31 early-game goals; #32 optional math strategy/number mastery; #33 requirement-based More Hub unlocks with localized unmet-requirement panels.
  - **#34–#39 Lua/localization/LiveOps:** #34 localize Lua activity/minigame payloads; #35 localize Lua random events; #36 locale/fallback regression coverage; #37 temporary LiveOps locale packs; #38 pack validation/cache/expiry/recovery; #39 joy/localization/LiveOps/save regression review.
  - **#40–#45 C++/NDK:** #40 architecture audit/baseline; #41 NDK/CMake scaffold; #42 narrow validated JNI bridge; #43 port one bounded deterministic component only if justified; #44 Kotlin/native parity/failure tests; #45 ABI/integration/regression/performance review. Kotlin remains authoritative and has a fallback.
  - **#46–#48 delivery and verification:** #46 separate Release GitHub Releases from Debug Actions-artifact updates, validate ZIP contents and APK compatibility/signature before install, and show optional update availability; #47 fast deterministic checks plus bounded opt-in/manual stress suites; #48 harden Kotlin/C++/Lua contracts and prevent Lua from getting direct JNI, network, filesystem, or unrestricted save/reward access.
  - **#49–#57 polish and readiness:** #49 signature game-feel polish; #50 navigation/interaction consistency; #51 skippable onboarding/help; #52 save integrity/recovery; #53 measured performance/memory/battery improvements; #54 accessibility/adaptive-device checks; #55 global opt-out haptics; #56 localization/content completeness; #57 release-candidate readiness gate.
  - **#58–#66 trust, return features, settings:** #58 privacy/security/player trust; #59 fair return-friendly goals/streaks/widgets; #60 opt-in reminders with quiet hours/frequency controls; #61 interactive first-use feature discovery and stable global tutorial flags; #62 Material 3 Expressive motion/loading system; #63 organized Settings/customization hub; #64 player-selected downloaded background music only, with no automatic playback until selected/enabled; #65 optional Google Play Games/cloud saves only after platform setup, with explicit conflict resolution; #66 Debug-only developer test dashboard with real outcomes and guarded destructive actions.
  - **#67–#74 events and release notes:** #67 reusable validated Live Events Engine; #68 fair daily/weekly challenges; #69 seasonal events/collectibles retained after expiry; #70 Events Hub/calendar; #71 skippable, non-guilt-based Welcome Back recap; #72 Debug-only isolated event simulator; #73 localized What's New/version history shown at most once per installed version; #74 event/release readiness checks with explicit pass/fail/skip.
  - **#75–#80 multiplayer:** #75 architecture and Wi-Fi/Bluetooth transport-fallback design; #76 Wi-Fi-first local P2P lobby with Bluetooth fallback; #77 separate multiplayer guessing mode; #78 reusable multiplayer minigame framework plus one initial minigame; #79 Wi-Fi/Bluetooth diagnostics and two-device regression checklist; #80 privacy/reliability/release gate. Initial multiplayer is opt-in local play, not internet matchmaking.
  - The roadmap file `docs/JOY_PHASE_ROADMAP.md` is the detailed source of truth for each PR's acceptance criteria and ordering.
- Multiplayer is planned only, not implemented. Start with opt-in local peer-to-peer play over Wi-Fi/local network, with Bluetooth as a fallback when Wi-Fi cannot connect and both devices support the selected method. Use a transport-independent protocol and host-authoritative match state; keep multiplayer separate from single-player saves, currencies, achievements, and rewards. Initial local-network play should not require accounts, Play Console setup, a cloud server, or internet access.
- Multiplayer sequence: #75 architecture/transport fallback design; #76 Wi-Fi-first lobby with Bluetooth fallback; #77 guessing mode; #78 reusable minigame framework and a first minigame; #79 Wi-Fi/Bluetooth diagnostics and two-device regression checklist; #80 privacy, reliability, and release gate.
- Two physical Android devices are required for real transport testing. Plan manual tests for same Wi-Fi, phone hotspot, Bluetooth-only fallback, permission denial, disabled Bluetooth, incompatible versions, app background/foreground, host exit, disconnect/reconnect, repeated messages, and cleanup. Simulated tests do not replace physical-device tests.
- Cross-network internet multiplayer is explicitly out of initial scope; it may require signaling and relay/network infrastructure. Do not add accounts, public matchmaking, relay services, or online requirements unless separately approved.
- Preserve the user's constraints: offline single-player remains functional; Kotlin stays authoritative for gameplay state, saves, localization, eligibility, and rewards; Lua remains sandboxed and never receives unrestricted save/reward/JNI/network access; C++ is limited to a measured, bounded component with Kotlin fallback; do not add features or dependencies outside the focused PR; workflows/builds/device tests are manual-only unless explicitly requested.
- App version policy for this pass: bump to 3.0.2 / versionCode 20 because a merged gameplay change needed release documentation. Do not bump again for roadmap-only edits. Planned PRs belong in the roadmap/MEMORY.md, never as completed release notes.

## Repo Rating and Stability Follow-up Roadmap (2026-10-12)

- Planning-based repo rating: **8/10 for ambition, 8/10 for architecture direction, and approximately 6.5/10 for verified release readiness until the remaining checks are run.** This is not a fresh line-by-line source audit or an objective quality metric. Strengths include offline-first gameplay, Kotlin-owned game state and saves, a constrained Lua/native integration plan, localization and encrypted backup/recovery work, and an explicit test roadmap. The main gap is evidence: planned tests and source inspection are not equivalent to passing builds, tests, or real-device validation.
- The detailed roadmap in `docs/JOY_PHASE_ROADMAP.md` now covers PRs **#24–#90** (67 numbered PRs, including merged #24). PRs #81–#90 are additional stability-focused plans; they are not implemented features and must not be added to the player-facing changelog until actually delivered.
- **#81 Startup and crash-recovery state machine:** ensure startup resolves to ready, recoverable error, or retry/exit rather than an indefinite spinner; test first launch, valid/corrupt saves and backups, changelog failure, and process recreation.
- **#82 Save transaction fault-injection tests:** test interrupted writes, storage failures, duplicate operations, migration, reset, import/export, slot creation/switching, and backup recovery without losing a valid save.
- **#83 Localization runtime and release gate:** validate keys, format placeholders, plurals, RTL, dynamic UI/Lua strings, accessibility labels, language switching, and release notes; key parity alone does not prove translation quality.
- **#84 Widget, notification, and music lifecycle integration tests:** verify launch routing, asynchronous scheduling, process recreation, audio focus, URI revocation, queue edits, and cleanup; hardware-dependent checks remain a separately identified device checklist.
- **#85 Lua sandbox adversarial/resource-limit tests:** enforce actual engine limits for input/output size, execution budget, allowed libraries, cancellation, and payload depth; test runaway scripts, malformed data, and blocked filesystem/network/JNI/save/reward access.
- **#86 Economy and reward invariants:** deterministic/property-style tests for currency/statistics parity, one-time claims, duplicate reward prevention, failed persistence, invalid numeric values, caps, and reward consistency across gameplay systems.
- **#87 CI dependency and artifact reproducibility:** audit Gradle/dependency configuration, diagnostic clarity, exact-commit/task reporting, version/channel metadata, and safe artifact contents. GitHub Actions remains manual-only unless the user explicitly authorizes a run.
- **#88 Android permission and external-file recovery:** test denied/revoked/unavailable permissions and file-picker URI access without crashes or repeated prompts; optional permission denial must not break offline core play.
- **#89 Startup/main-thread/resource baseline:** measure startup, first interactive frame, transitions, save load, memory and battery on representative devices; inspect blocking main-thread work, Compose recomposition cost, polling, resource leaks, and service lifecycle before optimizing.
- **#90 Final stability evidence and known-issues gate:** create a candidate-specific pass/fail/skip report and severity-based known-issues list; block releases on data loss, unrecoverable startup, duplicate rewards, sandbox escape, or broken offline core play. Skipped/unrun checks must never be reported as passing.
- PRs #81–#90 are **planning only**. Do not bump app version or write release notes for roadmap-only additions. Preserve offline play, backward-compatible saves, localization requirements, Kotlin authority, Lua sandboxing, and the measured-only C++ rule.
- Changes made in this turn updated `docs/JOY_PHASE_ROADMAP.md` and this memory file on branch `docs/build-channel-updater-roadmap`, associated with PR #25: https://github.com/ne0gl1tch20/GuessTheNumber/pull/25. They are not merged into `master`.
- No build, test, emulator run, physical-device test, or GitHub Actions workflow was started. Do not imply these new roadmap items are implemented or validated.


## Latest consolidated decisions (2026-10-13)

This section supersedes older notes where they conflict with the user's latest explicit decisions. Historical entries above are retained as project history.

### Official release version
- The user explicitly decided that **the official release-day app version will be `1.0.0`**.
- Treat the older version history as the Alpha-to-Beta development era. The official public release starts at 1.0.0.
- The development baseline previously recorded as `3.0.2` / versionCode `20` is not to be changed merely because of this plan. Apply the release version when the user authorizes official release work, and reconcile version metadata/release notes then.

### Current roadmap additions
- The current detailed roadmap is `.openai/docs/JOY_PHASE_ROADMAP.md`, with **71 planned PRs (#24–#94)** as last recorded. Verify current branch content before relying on that count.
- PR #93 is planned for Compose previews, hover guidance/tooltips, touch and keyboard alternatives, and extended accessibility including TalkBack, scalable text, contrast, touch targets, reduced motion, and previews for loading/empty/error/theme/localized states.
- PR #94 is planned for app-wide hardening across all screens, features, gameplay logic, persistence, security, and engine boundaries. It includes navigation/back behavior, state restoration, loading/empty/error/retry handling, lifecycle cancellation, input validation, migrations, reward idempotency, offline behavior, permissions/privacy, update flows, and Debug/Release feature gating.
- **Settings localization acceptance rule:** every setting, including toggles, selectors, sliders, dropdowns, and advanced options, must have a localized title and localized plain-language description explaining what it controls and its practical effect. Examples: “Enable haptics” explains when vibration happens; “Enable background music” explains optional playback behavior. Descriptions must reflect actual behavior, refresh on language changes, use correct localized placeholders, cover supported locales, and clarify global versus per-save scope. Remove or implement dead/misleading controls.
- Audit Kotlin/Lua/C++ boundaries where present. Kotlin remains authoritative for gameplay state, persistence, reward eligibility, and claims. Lua must be resource-bounded and unable to reach restricted filesystem/network/JNI or directly mutate saves/rewards. C++/NDK remains conditional on a narrow justified use case and measured benefit; keep Kotlin fallback and parity/failure coverage. Do not force a native port if it is not beneficial.
- Implement findings in focused PRs, not one risky rewrite. Preserve offline-first play, saves, balance, accessibility, privacy, and reward rules. Planned items are not implemented until code is changed and verified.

### Repository workflow reminders
- PR #39 (https://github.com/ne0gl1tch20/GuessTheNumber/pull/39) was last recorded open on branch `build/pr87-reproducible-artifacts`; re-check current status before relying on it.
- User does not need actual device performance traces for PR #38's protocol. Do not initiate device profiling based on that task.
- Never start builds, tests, emulators, physical-device tests, or GitHub Actions unless explicitly authorized. Do not claim checks passed without observing results.
- Whenever app code/UI/gameplay/localization/config/player-facing behavior changes, update `app/src/main/assets/changelogs.md` in the same PR. Changelog entries must describe implemented changes only; planned work belongs in the roadmap.
- For repository-change responses, include https://github.com/ne0gl1tch20/GuessTheNumber/actions and the exact sentence: **“You can build it using GitHub Actions.”**


## Project pause and future engine-expansion direction (2026-10-13)

- **Project status: PAUSED by the user.** Do not continue repository work, edit files, open/update PRs, inspect new tasks, or run builds/tests/Actions unless the user explicitly resumes it. The user's rule is: **any new message from them means the project is unpaused**. When they return with a message, acknowledge that the project is unpaused before continuing with the requested task. Until then, leave the repository untouched.
- The user wants to explore taking GuessTheNumber to the next level with **C++/NDK plus the existing Lua and LiveOps architecture**, where that combination is technically justified and maintainable. This is an idea, not authorization to implement or add dependencies now.
- Future exploration should consider a deliberate, versioned API/capability surface for Lua activities/events and developer commands, with typed arguments/results, explicit allowlists, permission boundaries, validation, bounded execution/resources, cancellation, clear errors, compatibility/version negotiation, and focused tests. Keep debug-only commands gated to Debug; never expose privileged developer capabilities to player-facing LiveOps scripts.
- Audit whether any C++/NDK or Lua/LiveOps dependency is genuinely needed before adding it. Pin and document justified dependencies, check license/maintenance/ABI compatibility, and retain Kotlin fallback where possible. Avoid duplicating existing command registries, payload validators, sandbox limits, JNI boundary plans, or security audits; extend the existing architecture and roadmap items instead of creating parallel systems.
- Kotlin remains authoritative for gameplay state, persistence, localization resolution, eligibility, and reward settlement. Lua must not gain unrestricted filesystem, network, JNI/native, save, or reward access. C++ must use a narrow validated JNI/API boundary and must not bypass Kotlin validation or become a second source of truth. LiveOps should deliver validated data/configuration rather than remotely executable native code; any script capability must remain explicitly bounded and trusted by the current architecture.
- This is planning only. No implementation, build, test, emulator run, or workflow is implied by this note.


## Advanced audio processing idea (2026-10-13, planning only)

- The user wants to explore advanced audio processing for GuessTheNumber: potentially a custom low-latency audio mixer, sample processing, and DSP effects. Native C++/NDK may be appropriate **when profiling and requirements justify it**, especially for latency-sensitive real-time processing, intensive sample/DSP calculations, or a required well-maintained native audio library.
- This is separate from ordinary music/SFX volume controls and routing. Coordinate with planned PR #92's music/SFX mixers so the work does not duplicate mixer UI or basic playback controls.
- First establish measurable latency/CPU/memory/audio-quality goals and inspect Android's existing audio APIs and current playback architecture. Prefer the simplest reliable platform/Kotlin/JVM solution when it meets requirements; use native code only for a demonstrated benefit. Check dependency licenses, maintenance, ABI support, packaging size, and fallback behavior.
- Keep audio processing bounded and lifecycle-aware: handle audio focus, route/device changes, interruptions, cancellation, resource cleanup, and failures without blocking the UI or breaking offline gameplay. Do not begin implementation while the project is paused.

## Cross-platform maintenance rule (2026-10-15)

- **Windows migration must not regress or neglect Android.** Android is the production behavior reference while Windows support is added. For every shared-domain, gameplay, economy, save-model, localization, asset, or build-system change, assess Android impact as part of the same PR and keep Android compiling and behaving as before.
- Prefer shared production logic where the platform boundary supports it, while keeping Android-only lifecycle, storage, resources, notifications and UI in the Android host. Do not solve Windows parity by replacing Android behavior or weakening Android validation.
- Keep CI/verification coverage for both Android and Windows when a change touches shared code or Gradle configuration. Verify save-format and BigNumber compatibility, and update app/src/main/assets/changelogs.md in the same PR for actual player-facing changes.
- GTN_APP_LOGO.png at repository root is the canonical artwork for both Android mobile launcher icons and Windows app/installer icons. Android launcher density/adaptive/round resources must be generated from it and checked in packaged builds; do not treat the icon task as complete until verified.
- This is a standing project rule, not a one-time migration checklist. Include it in future plans and reviews.


## Windows exact-port continuation (2026-10-16)

- Next focused UI parity increment is branch `feat/windows-shared-guess-feedback`: Windows Play now uses the common `SharedGuessFeedbackCard` also used by Android, with shared status colors, semantic feedback icon tokens, Blind mutator hint suppression and reduced-motion preference.
- PR should remain open/unmerged until checks pass and the user reviews it. CI can establish compilation/tests; a Windows runtime visual comparison against Android is still needed before marking this UI row verified.
- Product wish remains locked: Windows should be the same GTN experience the user plays on Android, adapted for desktop input/windowing rather than a smaller or reorganized substitute. Continue one concrete screen/interaction parity gap at a time and keep the parity plan evidence-based.
