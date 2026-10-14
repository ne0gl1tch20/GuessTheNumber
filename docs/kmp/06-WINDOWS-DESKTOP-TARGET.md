# Windows desktop target plan

## Goal

Ship a native-feeling Windows desktop app for GuessTheNumber using Kotlin/JVM and Compose Multiplatform Desktop, while retaining the Android app.

Compose Multiplatform documents desktop support for Windows, macOS, and Linux. The Windows target is a separate JVM desktop application; it is not a conversion of an APK into an EXE.

Reference: https://github.com/JetBrains/compose-multiplatform

## Minimum first desktop milestone

- Launch to a playable basic game screen.
- Support mouse input and keyboard controls.
- Use a resizable window with sensible minimum dimensions.
- Show clear focus states and keyboard navigation.
- Load bundled localization and game configuration from packaged resources.
- Keep gameplay offline.
- Store desktop saves in a dedicated application-data directory.
- Fail gracefully if optional music, sound effects, or Lua content is missing.

Do not attempt to port every Android screen before proving the basic game loop and save path.

## UI migration strategy

1. Keep the existing Android entry point and screens working.
2. Establish a Compose Multiplatform desktop entry point separately.
3. Reuse UI only after its imports and dependencies are compatible with desktop Compose.
4. Keep Android-specific navigation, widgets, activity APIs, permissions, and lifecycle code out of shared UI.
5. Make layout responsive to window size and keyboard/mouse interaction; don't stretch a phone-only layout across a desktop window without review.

## Desktop-specific behavior

- Window lifecycle and close handling.
- Keyboard shortcuts and focus management.
- File pickers and file paths for optional user-selected music.
- Desktop audio implementation.
- App data and save backup paths.
- Windows packaging and app icon.
- Update channel and version display appropriate to desktop builds.

## Packaging direction

Evaluate Compose Desktop packaging for a Windows installer and/or distributable app image. Decide between MSI, EXE installer, or ZIP distribution after checking signing, update strategy, and user installation expectations. A package should bundle a compatible runtime or document a supported JDK requirement.

## Acceptance criteria

- A Windows build launches without Android SDK/runtime assumptions.
- The basic game loop is playable with mouse and keyboard.
- Resource loading works from a packaged artifact, not only from the IDE.
- A desktop save can be created and reloaded.
- Android build remains unchanged by desktop-only setup.
- Package output is produced by a manual or explicit CI workflow before claiming release readiness.
