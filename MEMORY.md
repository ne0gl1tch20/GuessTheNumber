# Project Memory: Gamified Guess the Number Simulator (v3.0.0 Update)


## Developer Console and Diagnostics Update (2026-10-10)
- Added a registry-backed developer command autocomplete layer in `domain/command/CommandAutocomplete.kt`, including `DevCommand`, `CommandParser`, `CommandRegistry` and `CommandAutocomplete`. Suggestions include command descriptions and argument choices instead of being hard-coded in the screen.
- Added real debug commands: `/autoclicker on|off`, `/setlevel <upgradeId> <level>`, `/newnumber`, `/state`, `/inspect`, `/version`, `/logs clear|pause|resume`, plus `/unlock all` and `/completeguess` aliases. `/help` documents them.
- Dev Settings now has a persisted `verboseLogging` switch, severity filters, matching-entry count, and log entries show event names and correlation IDs. Existing developer routes remain gated to debug builds.
- `GameLogger` defaults verbose traces off, accepts TRACE/DEBUG only when verbose mode is enabled in debug builds, and limits release logging to WARN/ERROR/FATAL. File log storage is debug-only. Guess processing logs begin/rate-limit/result events with a correlation ID.
- Added unit tests for command autocomplete and parsing in `app/src/test/java/com/jarrlyyy/guessthenumber/domain/CommandAutocompleteTest.kt`.
- **Verification status:** changes were inspected through GitHub file fetches, but the Android build and unit tests have not been run. Automatic builds on push are disabled to conserve Actions resources; do not start a manual workflow unless explicitly requested.


## Project Overview

**Current player-facing pitch:** Start with a simple number guess, then build a run around upgrades, Frenzy streaks, random events, achievement tiers, cosmetics, stackable mutators, and custom challenges. v1.12 is intended to feel like a game you can keep poking at because every run can be pushed, optimized, or made weird in a different way. 🎮✨

A fully polished, feature-complete Android incremental game combining classic guess-the-number mechanics with deep idle progression, reset role hierarchy, anti-cheat protection, offline progression, JSON data-driven assets, startup loading splash screen for safe save data synchronization, 10 Save Slot System, developer settings with raw save editor, crash recovery with copyable & shareable crash logs, unified Dev Console logs UI inside an independent scrollable console box, non-crash log storage saving to Android/data files, background music selector/player in More navbar saving audio files to Android/data, crash recovery, interactive tutorial onboarding, and Material 3 Jetpack Compose UI with standard tab navigation. Version `v3.0.0` (versionCode 18).



## v3.0.0 Beyond the Numbers Ultimate Expansion
- Current app metadata in `app/build.gradle.kts`: versionName `3.0.1`, versionCode `19`.
- Added a save-compatible Endless Rift loop, unlocked only after all four world bosses are defeated. Rift tier, best cleared tier and active state are persisted in `GameState` with defaults for old saves.
- Rift runs rematch bosses in a repeating four-world order. Each victory scales the next boss by +2 HP per tier, multiplies that victory's Money and Nebula rewards by the current tier, records the best cleared tier and automatically advances to the next world.
- The World Map displays tier-scaled boss HP and the active Rift tier, and uses the existing guessing combat, phase rules and miss penalties for Rift rematches.
- Endgame Challenges now starts or leaves the Rift and displays current/best tier. Navigation calls the same ViewModel progression action handler used by other progression screens.
- Added the eight Rift UI strings to all 23 locale catalogs (Arabic, German, English UK/US, Spanish, Filipino, French, Hindi, Indonesian, Italian, Japanese, Korean, Dutch, Polish, Portuguese Brazil/Portugal, Russian, Thai, Turkish, Ukrainian, Vietnamese, Simplified Chinese and Traditional Chinese).
- Changelog updated for v3.0.0. The v3.0.1 cleanup removes the redundant More Hub tutorial and export/import entries and their standalone destinations; action-driven tutorial and Save Slots backup/restore remain available. Build/runtime validation must be confirmed from GitHub Actions before describing a commit as build-verified.

## v2.0.0 Beyond the Numbers Expansion
- Current release metadata in `app/build.gradle.kts`: versionName `2.0.0`, versionCode `17`.
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
