# Implementation Plan: New Advanced Features for Guess The Number

This implementation plan covers the addition of six major feature sets to **Guess The Number**, with strict adherence to polished Material 3 UI/UX principles (proper padding, scrolling columns, responsive cards, and clean typography to prevent clipping and cramping).

---

## Proposed Changes

### 1. Advanced Idle Automation & AI Guessing Bots
- **[MODIFY]** `app/src/main/assets/game/upgrades.json`: Add bot tiers and automation upgrade definitions.
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt`: Add AI bot level state and bot parameters.
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/ui/viewmodel/GameViewModel.kt`: Implement automated binary search guessing loop when bot is active.

### 2. Prestige Talent Web / Skill Tree (Polished 2D Canvas/Grid UI)
- **[NEW]** `app/src/main/assets/game/talent_tree.json`: Define talent nodes (Path of Precision, Path of Abundance, Path of Fortune) and prerequisites.
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt`: Add unlocked talents map/set.
- **[NEW]** `app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/TalentTreeScreen.kt`: Build an interactive, scrollable, zoomable or well-spaced grid skill tree screen with clear active/locked visual states and padding.
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/Screen.kt` & `NavGraph.kt`: Register talent tree screen route.

### 3. Interactive Android Home Screen Widgets
- **[NEW]** `app/src/main/java/com/jarrlyyy/guessthenumber/widget/GameWidget.kt`: Implement Jetpack Glance AppWidget provider and UI layout.
- **[MODIFY]** `app/src/main/AndroidManifest.xml`: Register AppWidget receiver.

### 4. Gameplay Mutators & Daily Seeded Challenges
- **[MODIFY]** `app/src/main/assets/game/challenges.json`: Add mutator definitions (Blindfolded, Limited Fuel, Daily Seeded Run).
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt`: Add challenge seed and active mutator state.
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/domain/engine/GameEngine.kt` & `GameViewModel.kt`: Handle seeded PRNG and mutator rule checks (warmer/colder, guess limits).

### 5. Combo Streaks & High-Stakes "Lucky Guess" Staking
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt`: Add staking toggle and multiplier streak tracking.
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/PlayScreen.kt` & `GameViewModel.kt`: Add staking UI toggles and multiplier feedback logic with clean spacing.

### 6. Save Export/Import & Cloud Backup UI
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/data/store/SaveManager.kt`: Add export to Base64 JSON string and import validation methods.
- **[MODIFY]** `app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt`: Add backup export/import dialogs and clipboard actions.

---

## Verification Plan

### Automated Tests
- Run unit tests (`gradlew test`) to verify game logic, save serialization, and binary search bot correctness.

### Manual Verification
- Deploy the app to check UI responsiveness, test talent tree node unlocks, verify widget updates, and test save export/import strings.
