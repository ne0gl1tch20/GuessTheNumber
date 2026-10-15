# Guess The Number (Incremental & Arcade Edition)

An advanced, feature-rich incremental and arcade game built with Kotlin and Jetpack Compose. Android is the primary feature-rich platform. The Kotlin Multiplatform foundation shares deterministic game rules and selected guessing UI contracts with the Windows desktop target; this does not mean the full game has feature parity on Windows. See the plans below.

## Lua Scripting Layer (Implementation in Progress)

The embedded LuaJ runtime and shared Kotlin engine facade support the bundled offline Number Rush activity, and Dev Settings includes typed /lua commands. Kotlin remains authoritative for save data, progression, eligibility and rewards.

- [Lua implementation roadmap](docs/LUA_SCRIPTING_PLAN.md)
- [Draft Lua host API contract](docs/LUA_API_DESIGN.md)
- [GitHub LiveOps bundle delivery plan](docs/LIVEOPS_SCRIPT_BUNDLES_PLAN.md)
- [Signed LiveOps bundle format](docs/LIVEOPS_BUNDLE_FORMAT.md)
- [Copy-ready LiveOps repository template](liveops-template/README.md)
- [Reserved reusable scripts directory](app/src/main/assets/scripts/README.md)

The runtime uses an instruction-budgeted Lua sandbox without Android/Java bridge, filesystem, OS, network, package loader or coroutine access. The signed LiveOps client remains fail-closed until its content repository and trusted public-key configuration are set up.

## Key features

- Number guessing, incremental engine, quick-buy multipliers and background guessing bots.
- Standard, Prestige and Ultra upgrades, talents and scaling progression.
- Weekly challenges, offline random events, achievements, Frenzy and mutators.
- Arcade/minigames, world progression, bosses, relics, Home Base, Codex and Endless Rift.
- Multiple save slots, backup/restore and recovery.
- Android audio, notifications, widgets and developer diagnostics.

## Architecture

- Android UI: Jetpack Compose and Material 3 Expressive.
- Shared KMP foundation: deterministic rules and selected guessing UI components in shared/.
- Android retains its production navigation, authoritative GameState mutation, BigNumber currency math, persistence, RNG integration and Android-only services.
- Windows retains desktop window/input behavior, updates and MSI packaging.
- The app remains offline-first with no ads, analytics or accounts.

## KMP foundation and Windows parity

The KMP foundation shares deterministic rules for guess transitions, reward multipliers, penalties, progression requirements and challenge-claim decisions, with common tests. Selected controlled guessing UI is also shared. Windows has a desktop target, but end-to-end parity for all player-facing features is not yet established.

- [KMP foundation status and exit criteria](docs/KMP_FOUNDATION_STATUS.md)
- [Windows feature parity plan](docs/WINDOWS_FEATURE_PARITY_PLAN.md)
- [Android runtime QA checklist](.openai/kmp_runtime_parity_checklist.md)
- [Documentation index](docs/README.md)
- [Save schema and migration notes](docs/SAVE_SCHEMA_AND_MIGRATION.md)

## Validation and CI

PRs use Android validation/tests and Windows Desktop CI. Routine validation is checks/tests only and is not intended to build Android APKs. The stable debug APK workflow is separate. Use the latest run for the exact commit and inspect failures before claiming a check passed.

Local Android unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

For shared common tests, confirm the task name from Gradle rather than guessing it. Device/emulator coverage is separate.

## Getting started

1. Open the repository in a recent Android Studio version with Kotlin support and the configured Android SDK.
2. Sync Gradle and run the app module on an emulator or physical Android device. Android currently uses minSdk 26 and targetSdk 35.
