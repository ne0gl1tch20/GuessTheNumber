# Project Memory: Gamified Guess the Number Simulator (v2.0.0 Update)

## Project Overview

**Current player-facing pitch:** Start with a simple number guess, then build a run around upgrades, Frenzy streaks, random events, achievement tiers, cosmetics, stackable mutators, and custom challenges. v1.12 is intended to feel like a game you can keep poking at because every run can be pushed, optimized, or made weird in a different way. 🎮✨

A fully polished, feature-complete Android incremental game combining classic guess-the-number mechanics with deep idle progression, reset role hierarchy, anti-cheat protection, offline progression, JSON data-driven assets, startup loading splash screen for safe save data synchronization, 10 Save Slot System, developer settings with raw save editor, crash recovery with copyable & shareable crash logs, unified Dev Console logs UI inside an independent scrollable console box, non-crash log storage saving to Android/data files, background music selector/player in More navbar saving audio files to Android/data, crash recovery, interactive tutorial onboarding, and Material 3 Jetpack Compose UI with standard tab navigation. Version `v2.0.0` (versionCode 17).



## v2.0.0 Beyond the Numbers Expansion
- Current release metadata in `app/build.gradle.kts`: versionName `2.0.0`, versionCode `17`.
- Added backward-compatible per-save fields: `relicInventory`, `equippedRelicIds`, `worldMasteryLevels`, `discoveredSecretIds`, `codexEntries`, `homeBaseLevel`, `activeBossBattleWorldId`, `bossBattleMistakes`, and `codexRewardClaimed`.
- Added `WorldMapScreen.kt`: pinch/gesture zoom, drag panning, clickable world nodes, unlock requirements, secret-area markers, boss health, phase display and mastery upgrades.
- Boss combat is guessing-driven: starting a battle makes correct manual or auto-clicker guesses damage the active boss. Misses push progress back every 4 misses in phase 1, every 3 in phase 2 and every 2 in phase 3. World mastery adds 2% correct-guess Money per level, up to 20%. Boss victories grant their one-time Money/Nebula reward, relic, Codex entries and initial world mastery.
- Replaced the flat Talent screen with a zoomable connected skill tree. Talent purchases now check parent prerequisites and persist through the prestige currency. Speed talents boost auto-clicker speed; precision talents add critical reward chance; Nebula Resonance adds 50% correct-guess Money; Cosmic Oracle adds one Nebula per correct guess.
- Added `ProgressionScreens.kt` with Relics, Home Base, Explorer's Codex and Endgame screens. Up to two relics can be equipped. Relics add correct-guess bonuses; each Home Base level adds 2% Money bonus up to 40%. Codex completion grants 100 Nebula once all 12 entries are discovered.
- Added direct More-menu shortcuts and navigation destinations for World Map, Relics, Home Base, Codex, Endgame, Cloud Backup, Lucky Stake and Tutorial. Debug builds also expose Process Inspector. Lucky Stake now settles currency changes through the ViewModel.
- Locale keys for new progression UI have been added across all 23 locale JSON files. Filipino additions are translated; the newly added English fallback strings in other locales need native-language translation review before claiming complete localization quality.
- Changelog v2.0.0 is recorded in `app/src/main/assets/changelogs.md`.
- Validation status: GitHub source edits were committed directly to `master`. An Android build and emulator/runtime verification have not been run in this pass; verify compile and navigation before release.

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
- **`ui.screens`**: `PlayScreen`, `UpgradeScreen`, `ShopScreen`, `MoreScreen`, `PrestigeScreen`, `UltraScreen`, `ArcadeScreen`, `StatsScreen`, `SettingsScreen`, `AboutScreen`, `DevSettingsScreen`, `TutorialScreen`, `CrashRecoveryScreen`, `TalentScreen`, `MutatorsScreen`, `StakingScreen`, `CloudBackupScreen`, `SaveSlotsScreen`.
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
