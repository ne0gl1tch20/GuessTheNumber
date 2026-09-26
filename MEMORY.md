# Project Memory: Gamified Guess the Number Simulator

## Project Overview
A fully polished, feature-complete Android incremental game combining classic guess-the-number mechanics with deep idle progression, reset role hierarchy (1. Ultra, 2. Prestige located behind More Hub navigation buttons leading to dedicated Prestige and Ultra screens featuring JSON data-driven upgrade trees and shops), anti-cheat protection, offline progression, JSON data-driven assets (`upgrades.json`, `shop_items.json`, `prestige_upgrades.json`, `prestige_shop.json`, `ultra_upgrades.json`, `ultra_shop.json`), startup loading splash screen for safe save data synchronization, developer settings with raw save editor, crash recovery with copyable & shareable crash logs, unified Dev Console logs UI inside an independent scrollable console box with search/filter/pause/resume/export, non-crash log storage saving to Android/data files, command autocomplete while typing dev commands, background music selector/player in More navbar saving audio files to Android/data, crash recovery, interactive tutorial onboarding, and Material 3 Jetpack Compose UI with standard tab navigation. Version `v1.5`.

## Architecture & Package Structure (`com.jarrlyyy.guessthenumber`)
- **`domain.model`**: `BigNumber`, `GameState`, `GameStatistics`, `GameSettings`, `UpgradeDef`, `ShopItemDef`, `AchievementDef`, `LogEntry`.
- **`domain.engine`**: `GameEngine`, `AntiCheatService`, `RngService`.
- **`domain.command`**: `CommandExecutor`, `DeveloperConfig`.
- **`data.store`**: `SaveManager`, `SaveSerializer`, `SaveValidator`, `SaveMigration`.
- **`data.repository`**: `JsonConfigRepository` (`game_config.json`, `upgrades.json`, `shop_items.json`, `prestige_upgrades.json`, `prestige_shop.json`, `ultra_upgrades.json`, `ultra_shop.json`, `achievements.json`, `minigames.json`, `challenges.json`).
- **`data.logger`**: `GameLogger`, `LogLevel`, `LoggerCategory` (21 categories) with timestamped format `[HH:mm:ss.SSS][CATEGORY][LEVEL] message`.
- **`data.crash`**: `AppErrorHandler`, `CrashHandler`.
- **`data.notification`**: `NotificationHelper`, `GameReminderWorker`, `BootReceiver`.
- **`ui.navigation`**: `NavGraph`, `Screen`.
- **`ui.theme`**: Material 3 `Theme`, `Color`, `Typography`.
- **`ui.screens`**: `PlayScreen`, `UpgradeScreen`, `ShopScreen`, `MoreScreen`, `PrestigeScreen`, `UltraScreen`, `ArcadeScreen`, `StatsScreen`, `SettingsScreen`, `AboutScreen`, `DevSettingsScreen`, `TutorialScreen`, `CrashRecoveryScreen`.
- **`ui.viewmodel`**: `GameViewModel`.

## Bug Fixes & Improvements in v1.5 (QoL and Logic Features)
- Added **Smart Number Formatting** (Scientific, Engineering, Standard Short Scale, Alpha Suffixes).
- Added **Quick-Buy Multipliers & Max-Buy** (`1x`, `10x`, `100x`, `MAX`) in UpgradeShop.
- Added **Offline Progress & Summary Modal** calculating earnings and minigame tickets during absence.
- Added **Save Export/Import Backup** with Base64 encrypted JSON strings in Settings.
- Added **Audio & Haptic Fine-Tuning** (Master volume sliders, SFX, vibration, and reduce flashes mode).
- Added **Markdown Changelog Parser** for changelog popups.
- Added **Developer Process & Logic Inspector** screen in Dev Settings to inspect active variables, background loops, and game state.
- Added **Achievements & Perks Navigation** integrated into the More navigation hub.
- Bumped app version code to `5` and version name to `1.5`.

## Key Features & Systems Implemented
1. **BigNumber System**: Arbitrary precision decimal representation with canonical suffixes and multiple notation support.
2. **Game Engine & Reset Hierarchy**:
   - **Ultra Reset** (Tier 1 Ultimate: requires 10B Money, awards Ultra, max 2,000 cap; accessed via Ultra Hub screen with JSON upgrade tree and shop).
   - **Prestige Reset** (Tier 2 Standard: requires 50M Money, awards Prestige; accessed via Prestige Hub screen with JSON upgrade tree and shop).
   Located behind the More Hub navigation buttons.
3. **Data-Driven JSON Assets**: `game_config.json`, `upgrades.json`, `shop_items.json`, `prestige_upgrades.json`, `prestige_shop.json`, `ultra_upgrades.json`, `ultra_shop.json`, `achievements.json`, `minigames.json`, `challenges.json`, `changelogs.md`.
4. **Crash Recovery & Log Sharing**: Crash handler saves fatal exceptions to Android/data files; recovery screen provides copyable and shareable crash logs.
5. **Unified Dev Console & Log Storage / Essential Dev Commands**: Unified terminal-style live log console with search, pause/resume, clear, and copy, plus toggleable non-crash log storage saving to Android/data. Includes robust essential dev commands (`/timeskip`, `/max_upgrades`, `/unlock_all`, `/win`, `/speed`, `/stats`, `/give`, `/set`, `/reset`) with autocomplete chips.
6. **Command Autocomplete**: Instant suggestion chips while typing developer commands.
7. **Background Music Player**: Accessible from More navbar, allowing users to select an audio file, persist it in Android/data storage, and control playback (`Play`, `Pause`, `Stop`, looping).
8. **Interactive Onboarding Tutorial & Arcade Minigames**.
9. **Notification System & Background Reminders**: WorkManager-based periodic battery-efficient reminders, Android 13+ `POST_NOTIFICATIONS` runtime permission, settings toggle, and `BOOT_COMPLETED` broadcast receiver resilience.
