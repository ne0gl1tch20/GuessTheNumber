# Windows desktop application

This module is the Compose Multiplatform desktop host for GuessTheNumber. Android remains the behavioral reference. The player-facing game interface and Settings UI are shared HTML/CSS/JavaScript, with JarrlyyUI as the shared component system. Kotlin/KMP remains authoritative for game state, economy, progression, save validation and platform-neutral rules.

## Run locally

Use JDK 21.

```bash
./gradlew :desktopApp:run
```

On Windows PowerShell:

```powershell
.\gradlew.bat :desktopApp:run
```

## Windows installer

Install Inno Setup 6, then build the Windows setup executable:

```powershell
.\gradlew.bat :desktopApp:packageWindowsInnoSetup
```

The task builds the jpackage app-image and wraps it with `desktopApp/packageResources/windows/GuessTheNumber.iss`. Set `INNO_SETUP_COMPILER` if `ISCC.exe` is installed outside the default Inno Setup 6 directory.

The installer uses a stable Inno Setup AppId so subsequent versions upgrade the existing installation. It removes a detected legacy MSI installation during the one-time migration, then lets the stable Inno Setup AppId handle future upgrades. Player saves remain under `%APPDATA%\GuessTheNumber\saves` and are not removed by installer uninstall.

## Shared web assets

The desktop host copies the bundled HTML, CSS, JavaScript, JarrlyyUI SDK, fonts and locale assets into a per-session directory before starting the WebView. It applies only a previously validated cached asset version. A background updater checks the repository manifest, verifies safe relative paths and SHA-256 hashes, stages all changed files and atomically activates a complete version for the next launch. The running WebView's files are never replaced in place. If the network is unavailable or an update is invalid, the current bundled or cached version remains available. Startup validation can retry once with the previous validated cache.

## Debug update channel

- Successful Windows Desktop CI builds on `master` publish a separate `windows-debug-<run>-<attempt>` prerelease with `GuessTheNumber-Windows-Desktop.zip` and `windows-debug-manifest.json`.
- The desktop app checks the public `debug-latest` channel without embedding a GitHub token.
- The updater verifies the manifest and archive SHA-256, rejects unsafe ZIP paths and unexpected files, extracts only `GuessTheNumber-Windows-Desktop-Setup.exe` and launches it with Inno Setup's silent update switches.
- CI versions the installer as `1.0.<run number>`; local builds fall back to `1.0.0`.
- CI verifies fresh installation, in-place Inno Setup upgrade and uninstall while checking that player data under AppData remains intact.

## Architecture boundaries

- HTML + JarrlyyUI owns the shared player-facing Settings interface.
- Shared Kotlin/KMP owns platform-neutral state, validation, save models and game rules.
- Android and Windows adapters own storage, lifecycle, notifications, audio, windowing, startup registration and update installation.
- Native Compose remains appropriate for the desktop shell and OS-specific surfaces, not as a separate replacement for the shared HTML Settings experience.
