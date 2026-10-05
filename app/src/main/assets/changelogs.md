# 🚀 Guess The Number Simulator - Changelog

## v1.11 (Random Events, Achievement Tiers & Combo / Frenzy)
- ⚡ **Random Events**: Added seven varied occasional events with cash, Nebula, streak, range, refund, and jackpot effects. Events are checked periodically rather than firing constantly.\n- 🌐 **Random Event Localization**: Added English and Filipino event names and descriptions.\n- 🏆 **Achievement Tiers**: Expanded the system to 27 achievements across Bronze, Silver, Gold, and Diamond tiers, with escalating Nebula rewards.\n- 🌐 **Achievement Localization**: Added localized names and descriptions for the expanded catalog in English and Filipino.\n- 🔥 **Combo Frenzy**: Consecutive correct guesses now unlock Frenzy tiers every 5-streak steps.
- 📈 **Frenzy Rewards**: Each Frenzy tier adds a 25% reward multiplier, capped at 5x, on top of the existing streak multiplier.
- ✨ **Live Frenzy Indicator**: The Play screen shows the active Frenzy multiplier once the streak reaches 5.
- 🌐 **Localized Frenzy UI**: Added English and Filipino Frenzy text.
- 🧪 **Frenzy Tests**: Added engine coverage for tier scaling and reward increases.

## v1.10 (Save Slot Expansion, Profiles, Difficulty & Backup Improvements)
- 💾 **10 Save Slots**: Expanded save management from 3 slots to 10 with dynamic slot handling and metadata support.
- 🎚️ **Per-Slot Difficulty**: Added Classic, Hard, and Extreme difficulty profiles at the save-slot level.
- 👤 **Save Profiles**: Added customizable profile names and icons with sanitized save metadata.
- 📋 **Slot Duplication & Backup Restore**: Added duplication into empty slots and backup restoration with protection against overwriting occupied slots.
- 🛠️ **About Screen License Fix**: Fixed open-source license author and license fields displaying `%s` instead of their actual values.
- 🧪 **Localization Validation**: Improved placeholder validation so literal percent signs in localized text are handled correctly.
- 🎮 **Difficulty Mechanics**: Classic, Hard, and Extreme now have distinct range sizes, reward multipliers, and incorrect-guess money penalties.
- 📈 **Difficulty-Aware Ranges**: Better Range upgrades now scale from each difficulty's own base range.
- 🔄 **Reset Consistency**: Prestige resets preserve the selected difficulty's range.
- 🧪 **Engine Tests**: Added coverage for difficulty penalties, rewards, and range behavior.
- 🤖 **Smarter Guessing Bot**: Reworked the auto-clicker to use feedback-driven binary search instead of reading the hidden target directly.
- 🧪 **Bot Tests**: Added coverage for feedback-based target finding and difficulty-sized ranges.

## v1.9 (Comprehensive Internationalization & Localization Overhaul)
- 🌐 **Full Locale & Internationalization Support**: Localized 100% of user-visible strings across all screens (Play, Prestige, Ultra, Upgrades, Shop, Arcade, More, Mutators, Achievements, Cloud Backup, Crash Recovery, Settings, Dev Settings) into `locales/en_us.json`.
- 💬 **Data-Driven Localized Assets**: Fully wired all JSON config assets (`upgrades.json`, `shop_items.json`, `achievements.json`, `prestige_shop.json`, `prestige_upgrades.json`, `ultra_shop.json`, `ultra_upgrades.json`, `minigames.json`, `mutators.json`, `challenges.json`, `talents.json`) and randomized feedback/hint messages through `LocaleManager`.
- 🧮 **Dynamic Math & Clue Analysis**: Upgraded guess feedback tips and math clues to dynamically inject real-time game state variables (midpoints, parities, deltas, interval bounds, moduli).
- 🛠️ **UI Polish & Architecture**: Unified all unused and secondary screens into the More Hub with polished Material 3 design consistency.

## v1.8 (Lucky Number Overhaul, 3 Save Slot System & Legacy Migration, Clipboard Encryption & Data-Driven Feedback)
- 🎲 **True Lucky Number Box Minigame**: Overhauled the Lucky Number Box arcade minigame so that exactly one random box is the winning jackpot, while other boxes reveal a miss, adding genuine risk and reward.
- 🔒 **Encrypted Clipboard Transfer**: Enhanced save export and import in Settings and Cloud Backup with robust XOR cipher combined with Base64 obfuscation and salt validation.
- 📊 **Variable & Process Inspector Expansion**: Updated the Process & Variable Inspector screen to display real-time target guess numbers, range bounds, and complete runtime state variables.
- 💬 **Data-Driven Randomized Guess Feedback**: Implemented data-driven JSON configuration (`guess_feedback_messages.json`) featuring 30 varied too-low and 30 varied too-high feedback messages.
- ✨ **Hidden Background Money Generation (`/s`)**: Polished passive income calculation and live rate indicator on the Play screen.
- ✨ **3 Distinct Save Slots System**: Implemented a comprehensive 3 save slot system (`Slot 1`, `Slot 2`, `Slot 3`) allowing players to manage separate game saves with independent progression, money, prestige, ultra currency, attempts, and timestamps.
- 🔄 **Legacy Save Migration & Safety Prompt**: Added automatic detection for legacy single-saves from prior versions. When opening the Save Slots screen, players are prompted to securely transfer their legacy progress into Slot 1. If declined or dismissed without confirmation, the app closes immediately (`System.exit(0)`) to preserve data integrity.
- 🛠️ **Save Slot Management UI**: Created a dedicated `SaveSlotsScreen` accessible from the More Hub, featuring slot summary cards, active slot highlighting, new game creation, individual slot resetting, and direct resume actions.

## v1.7 (Security & Integrity Hardening Pass)
- ✨ **Expressive Material 3 Detailed Statistics**: Redesigned the Stastistics Screen with Material Expressive design principles, featuring a prominent hero accuracy gauge card, financial metrics cards, active and best streak counters, reset metadata, session playtime duration, and icon-badged detail breakdown rows.
- 🛡️ **Authoritative Game Engine & Save Integrity**: Overhauled `GameEngine`, `AntiCheatService`, and `SaveManager` to strictly validate and sanitize all game state operations, preventing negative currency exploits, invalid upgrade levels, and corrupted BigNumbers.
- 🔒 **Robust Save Validation & Migration**: Implemented rigorous JSON validation rejecting tampered saves, impossible ranges, and corrupted timestamps.
- ⏰ **Precise Anti-Time-Travel & Rate Limiting**: Added backward-clock tampering detection with a 24-hour penalty while supporting normal forward time synchronization and preventing rapid input abuse without disrupting automation.
- 🧪 **Comprehensive Unit Test Suite**: Added comprehensive local unit tests covering guess resolution, reward calculations, criticals/streaks, upgrades, prestige/ultra, talents, mutators, challenge duplicate claims, save validation, and anti-time-travel.
- 🛠️ **Performance Fixes**: Fixed app freeze and ANR when selecting the **MAX** buy multiplier with uncapped upgrades (such as Reward Multiplier) by optimizing affordable level and cost calculations.
- ✨ **Material 3 Expressive & Organic Animations**: Overhauled the entire app UI with Google's Material Expressive design system, featuring organic spring-physics interactions, scale-down press effects, and smooth micro-animations.
- ✨ **Uncramped Currency & Stats Dashboard**: Redesigned the Play screen header with spacious, dedicated surface bubble cards for Prestige, Ultra, and Nebula metrics.
- ✨ **Settings Dropdowns & AMOLED Mode**: Converted theme appearance, number notation, and reminder frequency selector buttons into clean Material 3 dropdown menus, and added pure black AMOLED theme mode.
- ✨ **Money Generation Rate Display**: Added live money generation rate (`/s`) indicator right below the money balance on the Play screen.
- ✨ **Advanced Idle Automation & AI Guessing Bots**: Implemented autonomous background guessing bots with adjustable speed and auto-clicker optimization.
- ✨ **Prestige Talent Web & Mutators Polish**: Fixed UI rendering bugs (latex symbols fixed to clean multiplier icons like `×2`, `×3`, `×5`), added robust data-driven talent nodes (`talents.json`) with higher prestige costs, and fully integrated daily seeded challenges with Nebula rewards.
- ✨ **Adaptive Max-Buy Upgrades**: Enhanced upgrade scaling to support fluid `1x`, `10x`, `100x`, and `MAX` calculations based on current currency and max level caps.
- ✨ **Interactive Android Home Screen Widgets (Jetpack Glance)**: Implemented Glance-based app widgets (`GuessWidget`) supporting real-time currency display and quick-guess action triggers.
- ✨ **Combo Streaks & High-Stakes "Lucky Guess" Staking**: Added high-stakes 50/50 staking mode (`StakingScreen`) with streak multiplier bonuses.
- ✨ **Save Export/Import & Cloud Backup UI**: Created a dedicated Cloud Backup and encrypted save transfer UI (`CloudBackupScreen`).

## v1.5
- ✨ **Smart Number Formatting**: Added support for Scientific, Engineering, Standard Short Scale, and Alpha Suffixes number notations in Settings.
- ✨ **Quick-Buy Multipliers & Max-Buy**: Added `1x`, `10x`, `100x`, and `MAX` buy multipliers in UpgradeShop.
- ✨ **Offline Progress & Summary Modal**: Enhanced offline earnings calculation and summary modal on app startup.
- ✨ **Export/Import Save Backups**: Added Base64 encrypted save string export and import in Settings.
- ✨ **Audio & Haptic Fine-Tuning**: Added master volume sliders, sound effects, vibration, and reduce flashes toggles.
- ✨ **Markdown Changelog Parser**: Changelog dialog now parses and formats markdown headers and list items.
- ✨ **Developer Process & Logic Inspector**: Added dedicated developer process and variable inspection screen in Dev Settings.
- ✨ **Achievements & Perks Navigation**: Integrated achievements and active perks into the More navigation screen.

## v1.4
- ✨ **Scaling Reset Requirements**: Prestige and Ultra money requirements now scale up progressively with each reset (Prestige scales by $2.5\times$ and Ultra by $5.0\times$ per reset).
- ✨ **UI Bug Fixes & QoL**: Fixed cramped buy multiplier buttons in the upgrades screen and added current money balance display.
- ✨ **Prestige & Ultra Requirements & Reset Info**: Updated Ultra requirements to require both Money and 1,000 Prestige. Added explicit reset info text on Prestige and Ultra screens detailing reset costs, scaling, and reset contents.
- ✨ **Quick-Buy Multipliers (`1x`, `10x`, `100x`, `MAX`)**: Added interactive buy multiplier filter chips and multi-level cost scaling to the Upgrade screen.

## v1.3
- ✨ Added a welcoming startup loading screen with circular progress animation to safely catch up and validate save state on app launch.
- 🛠️ Fixed Dev Settings Save Editor error message ("Failed to apply invalid save json") when submitting invalid save JSON strings.
- 🎨 Converted dev settings action buttons (Run, Load Current Save, Apply Save JSON) into space-saving icon buttons.
- 📤 Added dedicated share crash logs icon button alongside copy support in the crash recovery screen.
- 📜 Enclosed the Dev Console live logs terminal viewer in a fully independent, scrollable console box with color-coded syntax.

## v1.2
- 🛠️ **Essential Developer Commands**: Added robust new dev commands (`/timeskip`, `/max_upgrades`, `/unlock_all`, `/win`, `/speed`, `/stats`) along with autocomplete chips for rapid testing and debugging.
- 🔔 Added notification system with Android 13+ `POST_NOTIFICATIONS` runtime permission, WorkManager background reminders, and `BOOT_COMPLETED` reboot resilience.
- 🔄 Relocated Prestige and Ultra resets behind the More Hub with explicit Reset Role Hierarchy (1. Ultra, 2. Prestige).
- 🎓 Added interactive onboarding tutorial screen for first-time players.
- 💰 Guaranteed minimum +10 money reward on initial correct guesses.
- ⚙️ Reimplemented Developer Console as Developer Settings with a raw JSON save editor and clean log stream (`[13:56:30.000][GUESS][INFO] Example output.`).
- ⚙️ Enhanced Settings with master volume slider, sound/vibration toggles, reduce flashes mode, and checkmark confirmation for data reset.
- ⇄ Added smooth horizontal swiping between navigation tabs (Play, Upgrade, Shop, More).
- 🔒 Encrypted and obfuscated save export/import strings.
- 🎮 Fixed and expanded Arcade minigames to reward exactly 1 Nebula per win.
- ⬆️ Added more rich upgrades to `upgrades.json`.

## v1.0
- ✨ Implemented Material 3 UI across all screens, replacing emojis with polished Material Icons.
- 🎮 Added fully playable Arcade minigames with Nebula & Money rewards.
- ⬆️ Expanded Upgrade tree with rich data-driven upgrades.
- 🌸 Added Moe animations and spring-based bouncy visual feedback throughout gameplay.
- 🛡️ Integrated anti-cheat protection, save export/import, offline progression, and developer tools.
