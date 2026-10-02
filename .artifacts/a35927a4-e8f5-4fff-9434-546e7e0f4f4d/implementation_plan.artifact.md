# Implementation Plan - GuessTheNumber Security, Integrity & Hardening Pass

Comprehensive integrity, security hardening, and test coverage pass on the "ne0gl1tch20/GuessTheNumber" Android project.

## User Review Required

> [!IMPORTANT]
> - All game state operations are centralized through authoritative domain logic (`GameEngine` and `SaveManager`).
> - Save validation and migration are strictly enforced to reject negative currencies, invalid IDs, impossible upgrade levels, corrupted BigNumbers, and corrupted timestamps.
> - Backward clock tampering detection is enforced with a 24-hour penalty without punishing normal forward time synchronization or regular time drift.
> - Comprehensive unit tests are added to verify game engine logic, guess resolution, reward calculations, criticals/streaks, upgrades, prestige/ultra, talents, mutators, challenge duplicate claims, offline earnings, save validation, corrupted/imported saves, anti-time-travel, rate limiting, and concurrent state updates.
> - Version bumped to `1.7` across `build.gradle.kts`, `README.md`, `changelogs.md`, and `MEMORY.md`.

## Open Questions

- None. All requirements align with production-grade incremental game integrity standards.

## Proposed Changes

### Domain & Engine Integrity
- **[MODIFY] [GameEngine.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/engine/GameEngine.kt)**: Centralize and validate authoritative calculations for rewards, critical chances, streaks, prestige/ultra requirements and rewards, talents, and mutators.
- **[MODIFY] [AntiCheatService.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/engine/AntiCheatService.kt)**: Enforce robust rate limiting and currency sanitization against negative balances or injected states.
- **[MODIFY] [AntiTimeTravelService.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/domain/engine/AntiTimeTravelService.kt)**: Validate backward clock tampering while allowing ordinary forward clock synchronization.

### Save Validation & Security
- **[MODIFY] [SaveManager.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/data/store/SaveManager.kt)**: Strengthen save validation to reject negative currencies, unknown/invalid IDs, impossible upgrade levels, malformed BigNumbers, and corrupted timestamps. Replace legacy weak repeating XOR with robust validation and atomic serialization.

### Challenges, Mutators & Talents Integration
- **[MODIFY] [GameViewModel.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/viewmodel/GameViewModel.kt)**: Wire up authoritative validation for challenge ID completion (preventing duplicate/post-restart claims), active mutators, talent multipliers, and offline earnings consistency.

### Comprehensive Unit Testing
- **[NEW] [GameEngineTest.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/test/java/com/jarrlyyy/guessthenumber/domain/GameEngineTest.kt)**: Add real unit tests covering correct/wrong guesses, reward calculations, criticals/streaks, upgrades, prestige/ultra, talents, mutators, and challenge duplicate claims.
- **[NEW] [SaveValidationTest.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/test/java/com/jarrlyyy/guessthenumber/data/SaveValidationTest.kt)**: Add unit tests for save validation, corrupted/imported saves, anti-time-travel, rate limiting, and concurrent state updates.

### Documentation & Version Bump
- **[MODIFY] [build.gradle.kts](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/build.gradle.kts)**: Bump `versionName` to `"1.7"` and `versionCode` to `6`.
- **[MODIFY] [README.md](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/README.md)**: Update version references to v1.7 and hardening details.
- **[MODIFY] [changelogs.md](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/assets/changelogs.md)**: Add entry for v1.7.
- **[MODIFY] [MEMORY.md](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/MEMORY.md)**: Update project memory for v1.7 hardening pass.

## Verification Plan

### Automated Tests
- Run Gradle unit tests (`./gradlew testDebugUnitTest`).
- Run Gradle build (`./gradlew assembleDebug`).

### Manual Verification
- None required (fully covered by comprehensive automated unit test suite and gradle build verification).
