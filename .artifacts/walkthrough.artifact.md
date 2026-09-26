# Walkthrough - Features & QoL Enhancements

## Summary of Accomplished Features
1. **Offline Progress Summary Modal**: Automatically detects extended idle duration upon startup and prompts the player with earned idle currency.
2. **Smart Number Formatting & Notation Toggles**: Added support for **Standard**, **Scientific**, and **Engineering** number notations in [BigNumber.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/BigNumber.kt) and [SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt).
3. **Quick-Buy Multipliers (`1x`, `10x`, `100x`, `MAX`)**: Fully interactive upgrade purchasing multiplier selector.
4. **Push Notifications & Reminders**: Configurable push reminders with Android 13+ runtime permissions.

## Verification
- Built successfully via Gradle (`app:assembleDebug`).
