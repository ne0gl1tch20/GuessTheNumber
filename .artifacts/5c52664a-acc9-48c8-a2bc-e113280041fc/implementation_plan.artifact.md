# Implementation Plan - Enhancements and Bug Fixes

## User Review Required

> [!IMPORTANT]
> - **Number Rush Buttons**: We will use a wrap-around layout (like `FlowRow` or a flexible grid) with compact padding for Number Rush buttons so they never feel cramped.
> - **Live Console Icons**: In Dev Settings / Live Console Logs, we will replace text buttons for Pause/Resume, Clear, and Copy with standard Material 3 Icon buttons (Pause/Play, Delete/Clear, ContentCopy) with proper tooltips/content descriptions.
> - **Settings UI Cramped Text**: We will expand text wrapping, adjust typography/padding, and add clean arrangement so no setting texts are cramped.
> - **Themes in Settings**: We will add a Theme selector (System, Dark, Light) directly in SettingsScreen with instant theme application.
> - **Notifications Logic Extension**: We will extend notification logic to support reminder frequency intervals, custom notification titles/messages, and reliable WorkManager scheduling.
> - **Offline Reward Popup**: We will fix the bug where offline reward popup is not showing when app opens by ensuring `offlineGains` state flow is correctly evaluated and observed without premature dismissal.

## Proposed Changes

### UI & Gameplay Enhancements

#### [MODIFY] [ArcadeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ArcadeScreen.kt)
- Fix Number Rush button spacing and layout (use compact sizing, wrap-around / scrollable / flexible arrangement) so buttons are never cramped.

#### [MODIFY] [DevSettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/DevSettingsScreen.kt)
- Turn Live Console control buttons (Pause/Resume, Clear, Copy) into clean Material 3 icon buttons with icons (`Icons.Default.Pause`/`PlayArrow`, `Icons.Default.Delete`, `Icons.Default.ContentCopy`).

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt)
- Fix cramped UI texts by improving spacing and weights.
- Add Theme selector (System / Dark / Light).
- Add extended notification settings (reminder frequency / notification test trigger).

#### [MODIFY] [Theme.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/theme/Theme.kt)
- Accept `themeMode` ("System", "Dark", "Light") to apply correct color schemes.

#### [MODIFY] [GameState.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt)
- Add notification frequency settings to `GameSettings`.

#### [MODIFY] [NavGraph.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt)
- Pass `themeMode` to theme and ensure offline reward popup is correctly displayed and persists until collected.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/MainActivity.kt)
- Support theme mode selection.

## Verification Plan

### Automated Tests
- Gradle build check (`app:assembleDebug`).

### Manual Verification
- Test Number Rush minigame dialog for un-cramped button layout.
- Verify Live Console icons in Dev Settings.
- Verify Settings UI texts and Theme selection.
- Verify extended notification settings and WorkManager scheduling.
- Verify offline reward popup displays correctly on app launch when offline gains > 0.
