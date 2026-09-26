# Implementation Plan - Quality of Life (QoL) and Logic Features

This plan outlines the complete implementation of the requested Quality of Life (QoL) enhancements and deep logic features for **Guess The Number**.

## User Review Required

> [!IMPORTANT]
> This plan covers all proposed QoL improvements and advanced game logic features (Smart Number Formatting, Quick-Buy Multipliers, Offline Progress Summary Modal, Save Export/Import backups, Haptic Feedback & Audio controls, Run History Analytics, Daily Seeded Challenge & Mutators, Prestige Talent Tree/Web, Active Achievement Perks & Themes, AI Automated Guessing Bots, Combo Streaks & Hot Streak Multipliers, Markdown parsing for changelog popup, Developer Process & Logic Inspection Screen, and Full Achievements Screen & Logic integrated into More navigation).

## Proposed Changes

### Data & Domain Layer
- **[BigNumber.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/BigNumber.kt)**: Add support for different formatting notations (Scientific, Engineering, Standard Short Scale, Alpha Suffixes).
- **[GameState.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt)**: Extend state to store notation preference, buy multipliers, prestige talent tree unlocks, streak multipliers, offline earnings summary data, audio/haptic settings, and active achievement perk state/unlocked themes.
- **[SaveManager.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/data/store/SaveManager.kt)**: Add Base64-encoded JSON export/import save string support.

### ViewModel & Game Engine
- **[GameViewModel.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/viewmodel/GameViewModel.kt)**:
  - Implement Quick-Buy multiplier logic (`1x`, `10x`, `100x`, `MAX`, percentage of currency).
  - Implement Offline Progress calculation and Summary Modal trigger on app startup.
  - Implement Combo Streaks & Hot Streak Multipliers.
  - Implement AI Guess Bot auto-solving logic loop.
  - Implement Prestige Talent Tree calculation and bonuses.
  - Implement Daily Seeded Challenge mode with mutators (Blindfolded, Limited Fuel, Taxes).
  - Implement active achievement milestone perks and theme unlocks.

### UI Screens & Components
- **[SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt)**: Add notation toggles, save export/import UI, audio volume sliders, and haptic feedback toggles.
- **[UpgradeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/UpgradeScreen.kt)**: Add Quick-Buy multiplier selector (`1x`, `10x`, `100x`, `MAX`, `%`) and Max-Buy All available.
- **[PrestigeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/PrestigeScreen.kt)**: Introduce the 2D Prestige Talent Tree / Web.
- **[StatsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/StatsScreen.kt)**: Enhance stats dashboard with run history analytics, average guesses, fastest solve time, and earnings per minute.
- **[MoreScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/MoreScreen.kt)** & **NavGraph**: Add Achievement screen navigation and integration so players can view active achievements, tiers, and claim/view perks and themes.
- **Changelog Popup**: Update changelog dialog/popup to parse and display markdown formatted release notes.
- **Developer Process & Variable Inspector Screen**: Add a dedicated screen accessible from [DevSettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/DevSettingsScreen.kt) to inspect active variables, background game loops, worker jobs, and game logic states.

## Verification Plan

### Automated Tests
- Run unit tests (`gradle_build("app:testDebugUnitTest")`) to verify BigNumber formatting, save export/import, buy calculations, and game logic.

### Manual Verification
- Deploy app to emulator/device (`deploy`) and test:
  1. Switching number notation formats in Settings.
  2. Using Quick-Buy multipliers in UpgradeScreen.
  3. Exporting and importing save strings.
  4. Testing Prestige Talent Tree and Daily Seeded Challenges.
  5. Verifying haptic feedback and audio sliders.
  6. Viewing the Markdown-rendered changelog popup.
  7. Opening the new Developer Process & Variable Inspector screen from DevSettings.
  8. Navigating to Achievements from More screen and verifying achievement perks and theme unlocks.
