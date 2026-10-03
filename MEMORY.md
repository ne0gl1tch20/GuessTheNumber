# Project Memory: Gamified Guess the Number Simulator (v1.8 Update)

## Project Overview
A fully polished, feature-complete Android incremental game combining classic guess-the-number mechanics with deep idle progression, reset role hierarchy, anti-cheat protection, offline progression, JSON data-driven assets, startup loading splash screen for safe save data synchronization, 3 Save Slot System (`v1.8`), developer settings with raw save editor, crash recovery with copyable & shareable crash logs, unified Dev Console logs UI inside an independent scrollable console box, non-crash log storage saving to Android/data files, background music selector/player in More navbar saving audio files to Android/data, crash recovery, interactive tutorial onboarding, and Material 3 Jetpack Compose UI with standard tab navigation. Version `v1.8`.

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
- **`ui.screens`**: `PlayScreen`, `UpgradeScreen`, `ShopScreen`, `MoreScreen`, `PrestigeScreen`, `UltraScreen`, `ArcadeScreen`, `StatsScreen`, `SettingsScreen`, `AboutScreen`, `DevSettingsScreen`, `TutorialScreen`, `CrashRecoveryScreen`, `TalentScreen`, `MutatorsScreen`, `StakingScreen`, `CloudBackupScreen`, `SaveSlotsScreen`.
- **`ui.viewmodel`**: `GameViewModel`.

## Advanced Features Implemented in v1.8
1. **3 Distinct Save Slots System (`SaveSlotsScreen`, `SaveManager`)**: Manages Slot 1, Slot 2, and Slot 3 independently with separate preferences keys, metadata summaries, switching, and reset actions.
2. **Legacy Save Migration & Safety Prompt**: Detects legacy single-save data and prompts the user to transfer it into Slot 1. If declined or dismissed without confirmation, the app closes immediately (`System.exit(0)`) to protect user data.
3. **Advanced Idle Automation & AI Guessing Bots**: Autonomous background guessing bots with adjustable speed and auto-clicker optimization.
4. **Prestige Talent Web / Skill Tree**: Interconnected Prestige talent nodes (`TalentScreen`) providing passive velocity, critical, and reward multipliers.
5. **Interactive Android Home Screen Widgets (Jetpack Glance)**: Glance-based app widgets (`GuessWidget`) supporting real-time currency display and quick-guess action triggers.
6. **Gameplay Mutators & Daily Seeded Challenges**: Customizable difficulty mutators (Hardcore, Hyper Speed, Blindfolded) and daily seeded challenge claims (`MutatorsScreen`).
7. **Combo Streaks & High-Stakes "Lucky Guess" Staking**: High-stakes 50/50 staking mode (`StakingScreen`) with streak multiplier bonuses.
8. **Save Export/Import & Cloud Backup UI**: Dedicated Cloud Backup and encrypted save transfer UI (`CloudBackupScreen`).
9. **Settings Dropdowns & AMOLED Theme Mode**: Converted theme appearance, number notation, and reminder frequency options into Material 3 `ExposedDropdownMenuBox` dropdowns, adding pure black AMOLED theme mode.
10. **Money Generation Rate Indicator**: Added live money generation rate (`/s`) indicator on the Play screen right below the money balance.
