# Guess The Number (Incremental & Arcade Edition)

Guess The Number is an advanced, feature-rich incremental and arcade game built around Kotlin/KMP game logic and the Jarrlyy UI SDK shared HTML/CSS/JavaScript frontend. **Shared UI migration status: complete.** The shared UI foundation is in place; development continues with regular bug fixes, polish and compatibility updates as issues are discovered. Fixes are currently committed directly to `master` while this workflow is in use. Windows runtime verification and platform-specific parity tracking remain separate from the shared UI migration status.

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

- The shared UI migration is considered **complete**. The Jarrlyy UI SDK and shared HTML/CSS/JavaScript frontend are the common UI foundation, with Kotlin/KMP remaining authoritative for game rules and saves.
- Ongoing work is maintenance: fix bugs as they are discovered, refine the UI and keep platform integrations compatible. Windows runtime verification and any remaining platform-specific parity items are tracked separately; migration completion does not itself claim every runtime check has passed.
- Kotlin/KMP remains authoritative for game rules, progression, economy, validation, rewards, save transactions/migrations and recovery; JavaScript is a presentation client, not a second game engine.
- Compose remains for native/system surfaces and platform integration, not duplicate player-facing game screens. Keep a reversible fallback until each migrated slice passes its acceptance checks.
- Preserve offline play, existing saves, localization, accessibility, audio/notifications/widgets where applicable, and no ads, analytics or accounts. See the shared UI migration matrix for the staged conversion and verification gates.

## KMP foundation and Windows parity

The KMP foundation shares deterministic rules for guess transitions, reward multipliers, penalties, progression requirements and challenge-claim decisions, with common tests. The current UI still has separate Compose implementations and full Windows parity is not established. The chosen [Shared Web UI Architecture](docs/SHARED_WEB_UI_ARCHITECTURE.md) records the target of replacing duplicate player-facing screens with one frontend while retaining Kotlin as game authority.

- [Shared web UI architecture and migration gates](docs/SHARED_WEB_UI_ARCHITECTURE.md)
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
