# Implementation Plan - Advanced Game Logic & QoL Features

We will expand **Guess The Number** with deep progression mechanics, strategic gameplay depth, and powerful Quality of Life (QoL) features.

## Proposed Features

1. **Quick-Buy Multipliers & Max-Buy** (`1x`, `10x`, `100x`, `MAX`) in upgrade screens (`UpgradeScreen`, `PrestigeScreen`, `UltraScreen`).
2. **Offline Earnings & Progress Modal** tracking time away, idle currency generated, and milestones reached upon app resume.
3. **Save Export & Import (JSON / Base64)** in `SettingsScreen` and `SaveManager` for safe cross-device backup and restoration.
4. **Prestige Skill Tree / Talent Web** allowing branching passive paths (Precision, Abundance, Fortune).
5. **Daily Seeded Challenges & Mutators** providing daily competitive runs with unique rulesets (Blindfolded, Limited Fuel, Taxes).
6. **Interactive Android Home Screen Widget** displaying current streak, active multipliers, and idle generation rate.

## Proposed Changes

### [Domain & Data Models]
- **[NEW]** [TalentTree.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/TalentTree.kt): Data structures for prestige talent nodes and unlock states.
- **[NEW]** [ChallengeMode.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/ChallengeMode.kt): Seeded daily challenge rules and mutators.
- **[MODIFY]** [GameState.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt): Add fields for offline timestamp, talent unlocks, buy multiplier preference, and challenge progress.

### [Save & Offline Progression]
- **[MODIFY]** [SaveManager.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/data/store/SaveManager.kt): Add export/import string logic and offline earnings calculation.

### [UI & ViewModels]
- **[MODIFY]** [GameViewModel.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/viewmodel/GameViewModel.kt): Handle buy multipliers (`1x`/`10x`/`MAX`), offline earnings summary state, talent tree spending, and challenge mode execution.
- **[MODIFY]** [UpgradeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/UpgradeScreen.kt): Add multiplier toggle buttons.
- **[NEW]** [TalentScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/TalentScreen.kt): Interactive skill tree UI.
- **[NEW]** [ChallengeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ChallengeScreen.kt): Daily seeded challenge and mutators UI.
- **[MODIFY]** [SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt): Add Save Export/Import options.

### [Widgets]
- **[NEW]** [GuessWidget.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/widget/GuessWidget.kt): Glance widget for streak and idle progress.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to verify compilation.
- Unit tests for save export/import, offline earnings calculation, and buy multiplier cost formulas.

### Manual Verification
- Deploy to device/emulator to test buy multipliers, export/import save backup, talent tree purchases, and offline earnings popup.
