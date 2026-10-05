# Project Memory: Gamified Guess the Number Simulator (v1.10 Update)

## Project Overview
A fully polished, feature-complete Android incremental game combining classic guess-the-number mechanics with deep idle progression, reset role hierarchy, anti-cheat protection, offline progression, JSON data-driven assets, startup loading splash screen for safe save data synchronization, 10 Save Slot System, developer settings with raw save editor, crash recovery with copyable & shareable crash logs, unified Dev Console logs UI inside an independent scrollable console box, non-crash log storage saving to Android/data files, background music selector/player in More navbar saving audio files to Android/data, crash recovery, interactive tutorial onboarding, and Material 3 Jetpack Compose UI with standard tab navigation. Version `v1.10`.

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
9. **Combo Streaks & High-Stakes "Lucky Guess" Staking**: High-stakes 50/50 staking mode (`StakingScreen`) with streak multiplier bonuses.
10. **Save Export/Import & Cloud Backup UI**: Dedicated Cloud Backup and encrypted save transfer UI (`CloudBackupScreen`).
11. **GitHub Actions APK Build Pipeline (`.github/workflows/build-apks.yml`)**: Manual APK build workflow running on `ubuntu-latest` through `workflow_dispatch` only that sets up JDK 17, sets up Gradle with caching, executes `./gradlew assembleDebug` and `./gradlew assembleRelease` (producing unsigned release APK), and uploads both APK variants (`GuessTheNumber-debug` and `GuessTheNumber-release`) as GitHub Actions artifacts.

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
