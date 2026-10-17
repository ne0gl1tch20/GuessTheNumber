# Windows desktop application

This module is the Compose Multiplatform desktop host for GuessTheNumber. The long-term product direction is to render the same packaged HTML/CSS/JavaScript player UI on Android and Windows, with Kotlin/KMP remaining authoritative for game state, economy, progression and persistence.

## Run locally

From the repository root, with JDK 17 installed:

```bash
./gradlew :desktopApp:run
```

On Windows PowerShell, use `.gradlew.bat :desktopApp:run`.

## Current scope

- Play, Upgrades and Shop now render the canonical shared HTML/CSS/JavaScript bundle inside an embedded JavaFX WebView hosted by Compose Desktop.
- The desktop host unpacks only the bundled local web assets, blocks navigation away from the local entry page and exposes a small Kotlin bridge. Game guesses, upgrade purchases, shop purchases/equips, multiplier changes, daily quest claims and loot chest rewards are processed by the existing desktop engine/transaction layer and saved through the existing save store.
- This is the first working host integration, not full parity: other player-facing screens still use Compose Desktop, and Windows runtime/installer behavior plus actual interaction parity still need CI and runtime verification.
- The desktop app runs offline and does not connect to services during ordinary gameplay.

The Android HTML migration has started in `app/src/main/assets/web-play/`; it is not complete across all player-facing screens. See [the shared UI migration matrix](../docs/SHARED_WEB_UI_MIGRATION_MATRIX.md), [the execution plan](../docs/SHARED_WEB_UI_CROSS_PLATFORM_EXECUTION_PLAN.md) and [the Windows host evaluation](../docs/WINDOWS_WEBVIEW_HOST_EVALUATION.md). Native Compose remains appropriate for the desktop shell and OS/system surfaces, not as a permanent duplicate of player-facing game screens.

## Windows Debug update channel

- Successful Windows Desktop CI builds on `master` publish a separate `windows-debug-<run>-<attempt>` prerelease with `GuessTheNumber-Windows-Desktop.zip` and `windows-debug-manifest.json`.
- The desktop app checks this public prerelease channel without embedding a GitHub token. Production releases are not used for Windows Debug discovery.
- The updater downloads to a unique temporary folder, verifies SHA-256, rejects unsafe ZIP paths and unexpected files, extracts only `GuessTheNumber-Windows-Desktop.msi`, and launches Windows Installer through a cleanup helper.
- CI sets the MSI and app-reported version to `1.0.<run number>`; local builds fall back to `1.0.0`.
- The updater does not modify save data. The Windows CI upgrade smoke test verifies the old-to-new MSI path before in-place upgrade behavior is considered proven.
