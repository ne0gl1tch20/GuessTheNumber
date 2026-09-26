# Project Memory: Gamified Guess the Number Simulator

## Project Overview
A fully polished, feature-complete Android incremental game combining classic guess-the-number mechanics with deep idle progression, reset role hierarchy, anti-cheat protection, offline progression, JSON data-driven assets, startup loading splash screen for safe save data synchronization, developer settings with raw save editor, crash recovery with copyable & shareable crash logs, unified Dev Console logs UI inside an independent scrollable console box, non-crash log storage saving to Android/data files, background music selector/player in More navbar saving audio files to Android/data, crash recovery, interactive tutorial onboarding, and Material 3 Jetpack Compose UI with standard tab navigation. Version `v1.6`.

## Architecture & Package Structure (`com.jarrlyyy.guessthenumber`)
- **`domain.model`**: `BigNumber`, `GameState`, `GameStatistics`, `GameSettings`, `UpgradeDef`, `ShopItemDef`, `AchievementDef`, `LogEntry`.
- **`domain.engine`**: `GameEngine`, `AntiCheatService`, `RngService`.
- **`domain.command`**: `CommandExecutor`, `DeveloperConfig`.
- **`data.store`**: `SaveManager`, `SaveSerializer`, `SaveValidator`, `SaveMigration`.
- **`data.repository`**: `JsonConfigRepository` (`game_config.json`, `upgrades.json`, `shop_items.json`, `prestige_upgrades.json`, `prestige_shop.json`, `ultra_upgrades.json`, `ultra_shop.json`, `achievements.json`, `minigames.json`, `challenges.json`).
- **`data.logger`**: `GameLogger`, `LogLevel`, `LoggerCategory` (21 categories) with timestamped format `[HH:mm:ss.SSS][CATEGORY][LEVEL] message`.
- **`data.crash`**: `AppErrorHandler`, `CrashHandler`.
- **`data.notification`**: `NotificationHelper`, `GameReminderWorker`, `BootReceiver`.
- **`widget`**: `GuessWidgetReceiver`, `GuessWidget`, `QuickGuessActionCallback` (Jetpack Glance).
- **`ui.navigation`**: `NavGraph`, `Screen`.
- **`ui.theme`**: Material 3 `Theme`, `Color`, `Typography`.
- **`ui.screens`**: `PlayScreen`, `UpgradeScreen`, `ShopScreen`, `MoreScreen`, `PrestigeScreen`, `UltraScreen`, `ArcadeScreen`, `StatsScreen`, `SettingsScreen`, `AboutScreen`, `DevSettingsScreen`, `TutorialScreen`, `CrashRecoveryScreen`, `TalentScreen`, `MutatorsScreen`, `StakingScreen`, `CloudBackupScreen`.
- **`ui.viewmodel`**: `GameViewModel`.

## Advanced Features Implemented in v1.6 (Tasks 1 through 6)
1. **Advanced Idle Automation & AI Guessing Bots**: Autonomous background guessing bots with adjustable speed and auto-clicker optimization.
2. **Prestige Talent Web / Skill Tree**: Interconnected Prestige talent nodes (`TalentScreen`) providing passive velocity, critical, and reward multipliers.
3. **Interactive Android Home Screen Widgets (Jetpack Glance)**: Glance-based app widgets (`GuessWidget`) supporting real-time currency display and quick-guess action triggers.
4. **Gameplay Mutators & Daily Seeded Challenges**: Customizable difficulty mutators (Hardcore, Hyper Speed, Blindfolded) and daily seeded challenge claims (`MutatorsScreen`).
5. **Combo Streaks & High-Stakes "Lucky Guess" Staking**: High-stakes 50/50 staking mode (`StakingScreen`) with streak multiplier bonuses.
6. **Save Export/Import & Cloud Backup UI**: Dedicated Cloud Backup and encrypted save transfer UI (`CloudBackupScreen`).
7. **Settings Dropdowns & AMOLED Theme Mode**: Converted theme appearance, number notation, and reminder frequency options into Material 3 `ExposedDropdownMenuBox` dropdowns, adding pure black AMOLED theme mode.
8. **Money Generation Rate Indicator**: Added live money generation rate (`/s`) indicator on the Play screen right below the money balance.

## Key Features & Systems Implemented
1. **BigNumber System**: Arbitrary precision decimal representation with canonical suffixes and multiple notation support.
2. **Game Engine & Reset Hierarchy**:
   - **Ultra Reset** (Tier 1 Ultimate: requires 10B Money, awards Ultra, max 2,000 cap).
   - **Prestige Reset** (Tier 2 Standard: requires 50M Money, awards Prestige).
3. **Data-Driven JSON Assets**: `game_config.json`, `upgrades.json`, `shop_items.json`, `prestige_upgrades.json`, `prestige_shop.json`, `ultra_upgrades.json`, `ultra_shop.json`, `achievements.json`, `minigames.json`, `challenges.json`, `changelogs.md`.
4. **Crash Recovery & Log Sharing**: Crash handler saves fatal exceptions to Android/data files; recovery screen provides copyable and shareable crash logs.
5. **Unified Dev Console & Log Storage / Essential Dev Commands**: Unified terminal-style live log console with search, pause/resume, clear, and copy, plus toggleable non-crash log storage saving to Android/data.
6. **Background Music Player**: Accessible from More navbar, allowing users to select an audio file, persist it in Android/data storage, and control playback.
7. **Interactive Onboarding Tutorial & Arcade Minigames**.
8. **Notification System & Background Reminders**: WorkManager-based periodic battery-efficient reminders, Android 13+ `POST_NOTIFICATIONS` runtime permission, settings toggle, and `BOOT_COMPLETED` broadcast receiver resilience.
