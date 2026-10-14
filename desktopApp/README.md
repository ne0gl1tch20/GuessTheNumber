# Windows desktop application

This module is the Compose Multiplatform desktop host for GuessTheNumber.

## Run locally

From the repository root, with JDK 17 installed:

```bash
./gradlew :desktopApp:run
```

On Windows PowerShell, use `.\gradlew.bat :desktopApp:run`.

## Current scope

- The desktop entry point now hosts `:shared`'s reusable Compose gameplay screen.
- The shared screen uses `:shared`'s `GuessRules` for feedback, supports mouse interaction, submit-on-Enter and a responsive max-width layout.
- Runs offline and does not connect to services.

This is the first shared-UI slice, not the full Android UI migration. Android's production Play screen still contains Android-specific repositories, localization access and game-state callbacks, so it has intentionally not been replaced by the smaller pilot screen. Next work should introduce platform adapters and move production gameplay state behind shared contracts before migrating the full Play screen and navigation. Save persistence, LuaJ, LiveOps, audio and installer/update paths remain platform-specific until individually audited.
