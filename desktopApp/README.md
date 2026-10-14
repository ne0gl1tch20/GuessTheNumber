# GuessTheNumber Windows desktop app

This is the Windows Compose Multiplatform host, using the shared JVM game domain so Android and desktop run the same game rules and progression calculations.

## Run locally

With JDK 17 installed, from the repository root:

```bash
./gradlew :desktopApp:run
```

On Windows PowerShell:

```powershell
.\gradlew.bat :desktopApp:run
```

## Current desktop features

- Guessing gameplay backed by the shared `:shared` `GameEngine` and `GameState`.
- Classic, Hard and Extreme difficulty selection.
- Shared reward calculations, streaks, guess statistics and difficulty penalties.
- Offline local saves under `~/.guessthenumber/`, with an atomic save file and last-known-good backup.
- Shared save JSON codec with unknown-field tolerance for migration compatibility.
- No accounts, ads, analytics or network requirement.

## Still being ported

The desktop app is not yet at full Android feature parity. Upgrade/shop UI, Prestige and Ultra screens, world/boss/relic systems, the complete Lua activity catalog and host flow, localization, audio, desktop settings and additional save validation/recovery UX remain follow-up work. Android-only features such as widgets, WorkManager notifications and Android system integrations will remain Android-specific.

See [KMP port status](../docs/KMP_PORTING_STATUS.md) for the staged migration and dependency order.
