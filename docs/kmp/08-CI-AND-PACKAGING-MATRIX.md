# Android + Windows CI and packaging matrix

## Goal

Add explicit validation for each target without making routine development slower than necessary. Keep the workflows manual or explicitly triggered until the repository's existing build configuration is ready for both targets.

## Proposed validation matrix

| Job | Runner | Purpose |
|---|---|---|
| Android debug compile | Ubuntu/compatible hosted runner | Confirm the current APK target still compiles |
| Shared unit tests | JVM runner | Verify pure game rules, models, validators, and protocol behavior |
| Desktop compile | Windows runner or supported JVM runner | Verify the desktop target compiles against its real platform dependencies |
| Desktop smoke/package | Windows runner | Launch/package the app and verify required resources are present |
| Lua conformance | Android + desktop JVM | Run the same safe bundled scripts and host API validation cases |
| Save compatibility | JVM tests plus Android-specific tests where required | Verify legacy fixtures, round-trip, corruption recovery, and atomic-write behavior |
| Localization/resource audit | JVM runner | Confirm expected locale/resource files are packaged for both targets |

## Build and packaging rules

- Keep Android and desktop build tasks separate so one target's dependencies do not leak into the other.
- Do not use an Android NDK library for Windows.
- Pin compatible Gradle, Kotlin, Android Gradle Plugin, and Compose Multiplatform versions together.
- Cache Gradle only where it is reliable; a cache miss must not affect correctness.
- Preserve the repository's preference for manual workflows unless the project owner explicitly asks for automatic CI.
- Use separate artifact names and version labels for APK and Windows packages.
- Never label an artifact as release-ready merely because compilation passed.

## Suggested workflow stages

1. Manual Android debug build.
2. Manual desktop compile.
3. Manual tests for shared logic.
4. Manual Windows package build.
5. Optional combined matrix after both targets are stable.

## Release artifact expectations

- Android: keep current debug/release channel behavior and signing rules.
- Windows: decide installer format, signing needs, runtime bundling, update channel, and save location before public distribution.
- Both: include version/commit metadata and avoid adding ads, accounts, analytics, or mandatory online services.

## Exit criteria

- [ ] Each target has an explicit Gradle task and runner.
- [ ] CI logs show the actual commands and result for each target.
- [ ] Shared tests run without Android-only APIs.
- [ ] Windows artifact can be installed or launched on a clean Windows environment.
- [ ] Existing Android artifact remains available.
- [ ] Workflows are not silently made automatic as part of the migration.
