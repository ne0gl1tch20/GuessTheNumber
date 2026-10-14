# Windows desktop prototype

This module is the first Compose Multiplatform desktop host for GuessTheNumber.

## Run locally

From the repository root, with JDK 17 installed:

```bash
./gradlew :desktopApp:run
```

On Windows PowerShell, use `.\gradlew.bat :desktopApp:run`.

## Current scope

- A small playable 1–100 guessing loop with mouse/keyboard input.
- Uses `:shared`'s `GuessRules` so feedback logic is not duplicated.
- Runs offline and does not connect to services.

This is a platform proof of concept, not a port of the full Android game. It does not yet load Android saves, use the Lua activity runtime, or provide the full progression systems. Those need separate compatibility work before any save or runtime code is shared.
