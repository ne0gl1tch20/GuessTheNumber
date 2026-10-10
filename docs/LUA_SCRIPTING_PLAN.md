# Lua Scripting Layer: Implementation Plan

**Status:** Planned only. No Lua runtime or dependency is installed by this plan.  
**Priority:** After the current game is stable and existing tests/builds pass.  
**Owner of game state:** Kotlin engine. Lua is an optional, constrained content layer.

## Goal

Add a small embedded Lua runtime above the existing Kotlin game engine. Use it for reusable scripted behavior, temporary events, and small minigames without moving core progression or save ownership out of Kotlin.

## Non-goals

- Do not replace the Kotlin engine, Compose UI, navigation, save manager, or existing command executor.
- Do not add Lua during the current stability push.
- Do not let scripts directly access Android APIs, files, network, reflection, arbitrary Kotlin objects, or the raw save file.
- Do not download and execute an unverified script from GitHub.
- Do not require internet for the main game or bundled scripts.

## Stability gate

Lua implementation may begin only after:
1. Debug and release builds complete successfully.
2. Existing unit tests pass, including game engine, world configuration, content validation, and LiveOps tests.
3. A save/load, backup recovery, language switching, app startup, and core guessing smoke test passes.
4. The current stability bugs are resolved and the baseline commit is recorded.
5. A small runtime spike demonstrates that the selected Lua runtime works on minSdk 26 and the app's supported ABIs without breaking offline builds.

If any gate fails, fix the baseline first. Keep this plan as documentation only until then.

## Proposed architecture

```text
GitHub event repository (future, signed/versioned bundles)
                 |
       download + verify + cache
                 |
      ScriptBundleRepository
                 |
   schema/API compatibility checks
                 |
       LuaRuntimeCoordinator
                 |
    restricted Kotlin host APIs
       /          |           \
  Game API     UI API      Event API
       \          |           /
            Kotlin engine
                 |
      validated game-state actions
```

Kotlin remains authoritative for currencies, upgrades, Prestige/Ultra progression, save mutations, reward claims, eligibility, and time. Lua requests typed actions; Kotlin validates and applies them. Lua may suggest a UI presentation or minigame step, but must not directly mutate engine state.

## Runtime and isolation requirements

- Select a maintained, Android-compatible Lua runtime only after checking licensing, maintenance, APK size, ABI support, offline dependency availability, and testability.
- Run scripts with a restricted environment: no unrestricted `io`, `os`, `debug`, package loading, reflection, JNI, Android context, or arbitrary Java/Kotlin access.
- Set instruction/time budgets, memory limits, cancellation, recursion limits, and maximum UI/action counts. If the chosen runtime cannot enforce limits, do not execute remotely supplied scripts in-process until an enforceable design exists.
- Expose only explicitly registered host functions. Version every public API.
- Validate every argument, identifier, number range, reward, and action on the Kotlin side.
- Fail closed: script errors stop the event/minigame and return control to the normal game without corrupting the save.
- Do not log secrets, complete saves, or arbitrary script payloads. Log script ID/version, lifecycle events, errors, and correlation IDs.
- Never treat a GitHub URL or a script hash alone as proof of authenticity. Verify a signed release manifest or equivalent trusted signature before enabling remote scripts.

## Suggested milestones

### M0 - Stability baseline
Finish the current game stabilization work, run the existing tests/builds, and record known issues and a baseline commit.

### M1 - Runtime feasibility spike
Compare candidate Lua runtimes. Prototype a local bundled script that returns a value and calls one harmless host API. Measure startup, APK size, execution time, memory, and behavior on supported devices. Do not merge if limits or licensing are unclear.

### M2 - Script bundle and version contract
Define bundle manifest schema, script IDs, entry points, API compatibility range, content hash, signature verification, required assets, locale keys, permissions/capabilities, and rollback behavior.

### M3 - Kotlin host API v1
Implement a narrow, typed API with tests. Begin with read-only game snapshots and structured log output. Add gameplay actions only after the authorization and validation layer is tested.

### M4 - Reusable UI and minigame primitives
Expose a constrained UI description/action model that maps to existing Compose components. Build one local-only minigame using reusable scripts and components. Keep layout creation declarative; no arbitrary UI code execution.

### M5 - Developer console integration
Keep the existing console and command executor. Add a script namespace/command group and a shared diagnostic stream so the console can show Kotlin app logs and Lua logs with source, level, script ID, event ID, and correlation ID. Lua commands must call the same validated host API, not bypass it.

### M6 - Offline event lifecycle
Support bundled scripts and event definitions with deterministic start, tick/update, pause/resume, completion, cancellation, and cleanup. Add lifecycle tests and verify that ending an event removes temporary state and UI.

### M7 - GitHub-hosted LiveOps (later)
Build a content publishing pipeline, not a direct GitHub-to-runtime shortcut. Fetch a signed manifest, verify signatures and compatibility, download to a staging location, validate all files, atomically activate a known-good bundle, retain the previous bundle for rollback, and record version/activation status. Existing game remains playable offline using bundled content.

### M8 - Controlled rollout
Use feature flags and a debug-only opt-in first. Release a single non-economy-changing event before permitting any scripted rewards. Expand only after crash, performance, rollback, and save-integrity checks pass.

## One-tap GO flow and manageable Lua hub

The first user-facing Lua entry point should feel like a normal game feature, not a scripting tool. The planned screen is a small **Lua / Events hub** with one prominent **GO** button.

### Player flow
1. The hub selects the current featured event or minigame automatically.
2. One tap on **GO** starts it immediately when its bundle is valid and ready. Do not make the player choose a script ID, type a command, or configure runtime settings.
3. If an event is already running, the same button resumes it. If nothing is featured, show a clear empty state and keep the normal game playable.
4. Show concise states: **Ready**, **Running**, **Paused**, **Offline content**, or **Unavailable**. If launch fails, explain briefly and return to the hub without affecting the save.
5. Offer secondary actions for Details, Change activity, and Exit. Keep developer controls in Dev Settings, not in the player's main flow.

### Keep the feature manageable
- Ship one featured activity and a small, curated list first. Avoid building a full script marketplace or visual editor.
- Bundle a sample minigame locally so GO works offline and can be tested without GitHub.
- Reuse existing Compose components, navigation, localization, feature flags, and app logging.
- Use a single lifecycle owner for start/resume/pause/stop/cleanup. Prevent duplicate launches from repeated taps.
- Display activity name, short description, estimated session length, and a simple progress/status indicator.
- Make Back/Exit reliably remove temporary UI, timers, and event state. Returning to the core guessing game must always work.
- If Lua is disabled or broken, hide/disable the GO entry point and leave the rest of the game usable.

### GO button acceptance checks
- One tap starts a ready bundled activity; a second rapid tap does not create a duplicate instance.
- Resume returns to the same active session when supported.
- No internet connection does not block bundled activities.
- Missing, incompatible, invalid, or crashing scripts show a friendly recoverable state.
- Cancel/Exit cleans up all temporary resources and preserves the save.
- All labels and errors are localized; app and Lua logs share a correlation ID for one launch attempt.

**Scope rule:** design the GO flow now, but implement the real button only after the stability gate and runtime feasibility spike pass. Do not ship a decorative button that pretends Lua is working before a runtime exists.

## Definition of done

- Core Kotlin game works when Lua is disabled, unavailable, or a script fails.
- No script can directly access files, network, Android APIs, or arbitrary host objects.
- All state changes are typed, validated, tested Kotlin engine actions.
- Bundled scripts work offline.
- Remote bundles require signature, schema, API-version, and content validation.
- Event lifecycle cleanup and rollback are tested.
- Dev console displays app and Lua diagnostics without exposing sensitive data.
- Builds and tests are green on the supported configuration.
