# Implementation Plan - Features & QoL Enhancements

This plan inventories already implemented features and maps out the remaining items from `features_to_implement.md`.

## Already Implemented Features
1. **Quick-Buy Multipliers (`1x`, `10x`, `100x`, `MAX`)**: Implemented in [UpgradeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/UpgradeScreen.kt) and [GameState.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt).
2. **Local Save Export/Import**: Implemented in [SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt) and [SaveManager.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/data/store/SaveManager.kt).
3. **Notification System & Reminders**: Implemented via [GameReminderWorker.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/data/notification/GameReminderWorker.kt) and toggles in [SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt).

---

## Remaining Features to Implement

### 1. Offline Progress Summary Modal
#### [MODIFY] [GameViewModel.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/viewmodel/GameViewModel.kt)
- Expose offline earnings details (`offlineDurationSeconds`, `offlineMoneyEarned`) to UI state.
#### [MODIFY] [PlayScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/PlayScreen.kt)
- Display an AlertDialog upon startup when returning from an absence detailing time away and idle currency earned.

### 2. Smart Number Formatting & Notation Toggles
#### [MODIFY] [GameState.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt)
- Add `numberNotation: String` (`"Scientific"`, `"Engineering"`, `"Standard"`, `"Alpha"`).
#### [MODIFY] [BigNumber.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/BigNumber.kt)
- Support formatting according to the selected notation style.
#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt)
- Add number format toggle preference.

### 3. Haptic Feedback Integration
#### [MODIFY] [GameViewModel.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/viewmodel/GameViewModel.kt) & UI Screens
- Hook vibration / haptic ticks into guess actions, correct answers, and prestige resets when `vibrationEnabled` is true.

---

## Verification Plan
- Build project successfully using `gradle_build`.
- Verify offline progress dialog appears upon startup when offline gains exist.
- Verify number formatting toggles update currency displays.
