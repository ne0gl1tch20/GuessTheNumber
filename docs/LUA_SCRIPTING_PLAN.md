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

### M5a - Dev Settings Lua Console
Add a dedicated **Lua Console** section inside the existing debug-only Dev Settings screen, using the current console's input, output, clear/copy behavior, and central logger where practical. It must be hidden from release builds and obey the existing `dev_console` feature flag.

Planned commands:
- `/lua status` — runtime availability, API version, and active instance state.
- `/lua list` — list validated bundled scripts and verified cached LiveOps scripts with source/version labels.
- `/lua inspect <script-id>` — show manifest metadata and allowed capabilities.
- `/lua run <script-id>` — start a registered script through the normal coordinator.
- `/lua stop <instance-id>` — cancel an instance and run cleanup.
- `/lua logs` — show bounded, redacted Lua diagnostics.
- `/lua eval <expression>` — **debug-only future REPL**, disabled by default until the runtime sandbox, execution/time limits, memory limits, and cancellation behavior have been demonstrated. It must never be available in release builds or accept network-fetched source.

Typed commands are preferred over arbitrary evaluation. When the REPL is enabled for local development, it must use the same restricted host API and isolated runtime as packaged scripts, run locally only, enforce hard resource budgets, and show an explicit warning that arbitrary input is developer code. It must not provide unrestricted Java/Kotlin access, filesystem/network access, save editing, or a way to bypass existing dev-command authorization. Do not evaluate input by passing it into the existing game command executor.

Acceptance checks: Dev Settings exposes the Lua console only in debug builds with the feature flag enabled; valid commands return structured output; malformed/unknown commands fail without crashing; stopping a script cleans up temporary UI/state; and the release artifact contains no Lua REPL entry point.

### M5b - Shared Lua API engine and adapters
Create one reusable scripting module/layer (package/module name to be decided during implementation) that owns the runtime coordinator, script registry, host API versions, bundle repository, action validator, UI renderer adapter, and diagnostics adapter. Compose screens, the Dev Settings console, bundled scripts, and LiveOps scripts must call this shared layer rather than implementing separate Lua bridges.

Design boundaries:
- **Runtime adapter:** wraps the selected Lua library and owns sandbox/resource limits; runtime-specific types do not leak into screens or game-engine code.
- **Host API registry:** registers versioned, typed functions once and enforces per-script capabilities.
- **Script registry + bundle repository:** resolve stable IDs across app assets and verified cached LiveOps bundles; source type is metadata, not a different execution path.
- **UI adapter:** turns validated declarative UI descriptions into existing reusable Compose components and routes stable action IDs to known Kotlin handlers.
- **Game adapter:** exposes read-only snapshots and approved action requests; the Kotlin engine remains the authority for state changes and rewards.
- **Console adapter:** sends typed commands through the same coordinator and displays structured results/logs; console-only inspection privileges are not granted to scripts.
- **Lifecycle/diagnostics adapter:** coordinates cancellation, cleanup, correlation IDs, bounded logs, and crash recovery.

Expose a small stable facade to app code, for example `LuaEngine` / `LuaEngineFacade`, with operations such as `status()`, `listScripts()`, `inspectScript(id)`, `start(id)`, `stop(instanceId)`, and `observeDiagnostics()`. These are design names only. Keep construction through dependency injection, provide fake implementations for tests, and ensure app startup/core gameplay do not depend on Lua initialization succeeding.

Reusable means one implementation and one versioned contract shared by offline scripts, verified LiveOps scripts, the GO activity flow, and developer tooling. It does not mean scripts can call arbitrary app APIs. Do not add a second scripting runtime or separate per-screen bridges.

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

## Two script sources: bundled offline + remote LiveOps

Use two clearly separated sources behind one registry and one versioned Kotlin host API.

### 1. Bundled offline scripts

Location in this Android repository:

```text
app/src/main/assets/scripts/
  README.md
  manifest.json
  common/
  minigames/
  events/
  ui/
```

- Ship a small curated set with the APK so the GO button can launch an activity without internet.
- Treat bundled scripts as the known-good fallback and pin them to the app release.
- Keep script IDs stable and declare each script's entry point, schema version, host API range, capabilities, locale keys, and required assets in the manifest.
- Do not overwrite these files with downloaded content at runtime.

### 2. Remote LiveOps scripts

Keep authoring and release artifacts in a **separate content-only GitHub repository**, provisionally named `GuessTheNumber-LiveOps` (name can be finalized before creation). This keeps event content/release history separate from Android engine changes.

Suggested content repository:

```text
GuessTheNumber-LiveOps/
  README.md
  schemas/
    liveops-manifest.schema.json
  manifests/
    stable.json
    staging.json
  bundles/
    v1/
      events/
      minigames/
      ui/
      locales/
  release-notes/
```

- Publish immutable, versioned bundles; do not point production clients at a mutable branch's raw Lua files.
- CI validates Lua syntax, manifest schema, script IDs, API compatibility, declared capabilities, file sizes, locale keys, and bundle hashes before release.
- Sign release manifests/bundles with a trusted signing key kept in GitHub Actions secrets. The app contains only the verification public key; never put a publishing token or signing private key in the APK.
- The app downloads metadata, verifies signature/hash/schema/API compatibility, stages the bundle, then atomically activates it. Keep the last known-good bundle for rollback.
- Online scripts are cached for offline reuse only after verification. If no valid cached bundle exists, use bundled content.
- GitHub is the content host, not the trust boundary: HTTPS or a hash alone does not prove who published a bundle.

### 3. Shared runtime and GO behavior

The runtime resolves an activity by trusted ID through a single registry, regardless of whether its source is bundled or a verified cached LiveOps bundle. The GO button never accepts a URL or arbitrary source code.

Suggested resolution order:
1. Valid, eligible featured LiveOps activity when online and its signed bundle is verified.
2. Previously verified cached LiveOps activity if still within its compatibility/expiry rules.
3. Bundled offline featured activity.

The app must explain when it falls back to offline content, prevent duplicate activity instances, and preserve the current game if an event cannot launch. Remote event rewards still require Kotlin-side eligibility and reward validation.

**Repository status:** this describes the intended layout and pipeline only. It does not create the separate repository, install Lua, download scripts, or enable execution.

## Definition of done

- Dev Settings contains a debug-only Lua console behind the existing developer feature flag.
- Typed console commands and any optional local REPL use the shared Lua engine facade and restricted host API.
- Bundled scripts, verified LiveOps scripts, the GO flow, and Dev Settings share one runtime coordinator and one versioned API registry.
- UI, game, diagnostics, bundle lookup, and lifecycle adapters are reusable and independently testable.
- Core Kotlin game works when Lua is disabled, unavailable, or a script fails.
- No script can directly access files, network, Android APIs, or arbitrary host objects.
- All state changes are typed, validated, tested Kotlin engine actions.
- Bundled scripts work offline.
- Remote bundles require signature, schema, API-version, and content validation.
- Event lifecycle cleanup and rollback are tested.
- Dev console displays app and Lua diagnostics without exposing sensitive data.
- Builds and tests are green on the supported configuration.
