# Lua Host API: Draft Contract

**Status:** Design draft only. API names are illustrative and must not be treated as implemented.

## API design rules

1. **Kotlin owns truth.** Lua can request actions; it cannot write save state directly.
2. **Typed, narrow, versioned.** Public functions have explicit parameters, return values, and API versions.
3. **Capability-limited.** A script receives only the capabilities granted to its bundle/event.
4. **Deterministic where practical.** Inject event time and seeded random values through the host API rather than exposing system time or unrestricted randomness.
5. **Validate on both sides.** Lua-side checks improve errors; Kotlin-side checks enforce rules.
6. **Stable UI vocabulary.** Scripts describe supported UI components instead of constructing arbitrary Compose trees.

## Candidate namespaces

| Namespace | Purpose | Access |
| --- | --- | --- |
| `game` | Read a limited game snapshot and request approved gameplay actions | Read-only snapshot plus validated action requests |
| `ui` | Show/hide supported panels, text, buttons, progress bars, cards and simple lists | Declarative UI only |
| `event` | Event lifecycle, timer state, completion and cleanup requests | Event-scoped |
| `minigame` | Start rounds, submit a result and request a restart | Per-minigame sandbox |
| `assets` | Read declared, bundled script assets by logical ID | Read-only allowlist |
| `locale` | Resolve a registered localization key with arguments | Read-only |
| `log` | Write structured diagnostics | Bounded, redacted |
| `random` | Seeded random number generation for fair/reproducible event behavior | Bounded |
| `storage` | Small namespaced temporary event state | Event-scoped, size-limited, not save-file access |

No namespace should expose arbitrary filesystem paths, URLs, Android Context, raw DataStore, raw save JSON, reflection, Java classes, or unrestricted coroutine/thread creation.

## Example shape (illustrative pseudocode, not implemented)

```lua
function event.on_start(api)
    api.ui.show_card({
        id = "welcome",
        title_key = "event.neon.title",
        body_key = "event.neon.description"
    })
    api.log.info("event_started", { event_id = api.event.id })
end

function minigame.on_round_start(api)
    api.ui.show_button({
        id = "guess_higher",
        label_key = "minigame.guess_higher",
        action = "submit_guess",
        enabled = true
    })
end

function minigame.on_action(api, action_id, payload)
    if action_id == "submit_guess" then
        return api.minigame.submit_result({
            score = api.game.validate_minigame_score(payload.score)
        })
    end
end
```

The actual bridge must not accept arbitrary callbacks, function source strings, or raw objects from the UI. UI actions should be registered IDs mapped to known Kotlin handlers.

## Candidate Kotlin bridge interfaces

These are conceptual boundaries, not code commitments:

- `LuaRuntimeCoordinator`: owns runtime lifecycle, cancellation, resource budgets and script errors.
- `LuaScriptRegistry`: maps trusted script IDs to validated local bundle entries.
- `LuaHostApiV1`: exposes only approved typed host calls.
- `ScriptBundleRepository`: resolves bundled/cached script bundles and manifests.
- `ScriptActionValidator`: validates every script-originated action against event capabilities and current game state.
- `LuaDiagnosticsAdapter`: maps script logs and errors into the existing central logger.

Prefer interfaces that are easy to fake in unit tests. Keep runtime-specific classes out of the game engine and Compose screens.

## Game API boundaries

Initially permit only:
- Read a minimal snapshot: current mode, permitted progress counters, event ID and remaining event time.
- Request approved navigation or presentation actions.
- Request a game-engine operation by a stable action ID after explicit allowlisting.

Do not initially permit Lua to:
- Grant currencies or permanent upgrades directly.
- Change Prestige/Ultra counts or write arbitrary progression fields.
- Claim achievements or LiveOps rewards.
- Edit difficulty/configuration globally.
- Mutate save data, settings, or feature flags.
- Trigger arbitrary developer commands.

If future events need rewards, the signed event definition and Kotlin reward validator must authorize the exact reward type, maximum amount, claim limit and eligibility. Lua must not choose an arbitrary reward.

## UI API and reusable components

Map the API to a small catalog of existing reusable app components, for example:
- `text`, `icon`, `button`, `card`, `progress`, `timer`, `list`, `dialog`, `image_asset`.
- Layout containers with bounded nesting, child counts, and supported spacing/alignment values.
- Stable action IDs, localized string keys, asset IDs and accessibility labels.

All user-facing text should use locale keys. No script should inject arbitrary HTML, Compose code, or a new navigation route. Unsupported components should fail validation before the UI is rendered.

## Dev Settings Lua Console and diagnostics

Add a dedicated **Lua Console** section to the existing debug-only Dev Settings screen. Reuse its current console interaction patterns where possible, including command input, output history, clear/copy controls, and central logging. Gate it behind both `BuildConfig.DEBUG` and the existing `dev_console` feature flag; it must not appear in release builds.

Proposed typed commands:
- `/lua status`
- `/lua list`
- `/lua inspect <script-id>`
- `/lua run <script-id>`
- `/lua stop <instance-id>`
- `/lua logs`
- `/lua help`

Unknown commands and invalid arguments return a localized or structured error, never crash the app. `/lua run` accepts only a registered stable script ID and uses the common coordinator; it must not accept a raw URL or source file path.

An optional `/lua eval <expression>` REPL is a later, debug-only opt-in, disabled by default. It may be enabled only after the selected runtime can enforce instruction/time, memory, recursion, output, and cancellation limits. It must run locally in the same restricted sandbox as normal scripts and must never be included in release builds. Do not route Lua input through the existing Kotlin game-command parser, and do not expose unrestricted Java/Kotlin access, filesystem/network APIs, raw save data, or hidden bypasses for game commands.

### Shared engine facade

All callers use one app-level facade, with illustrative operations such as:

```kotlin
interface LuaEngineFacade {
    fun status(): LuaEngineStatus
    fun listScripts(): List<LuaScriptDescriptor>
    fun inspectScript(scriptId: String): LuaScriptInspection?
    suspend fun start(scriptId: String): LuaStartResult
    suspend fun stop(instanceId: String): LuaStopResult
    fun observeDiagnostics(): Flow<LuaDiagnostic>
}
```

This is a design sketch, not committed implementation code. Exact signatures and coroutine types depend on the runtime spike. Compose screens, the GO flow, Dev Settings, bundled content, and verified LiveOps content all share the same registry, runtime coordinator, host API registry, action validator, UI adapter, and lifecycle manager. Avoid screen-specific bridges or duplicate runtimes.

The facade delegates to adapters for runtime execution, trusted bundle resolution, typed host calls, declarative UI rendering, game-engine action validation, lifecycle cleanup, and structured diagnostics. Keep runtime-specific objects out of UI and game-domain APIs; inject the facade so unit tests can use a fake engine. Lua initialization failure must not prevent normal app startup or core gameplay.

The log viewer should merge central app logs and Lua diagnostics using timestamp, level, source, event/script IDs and correlation ID. Preserve severity filtering and log-storage preferences. Bound log volume and redact arbitrary payloads.

## One-tap GO entry point

The player-facing entry point should be a normal localized Compose screen/card, not a raw console command.

Proposed host/UI contract (illustrative, not implemented):
- `activity.get_featured()` returns a validated featured activity descriptor or an empty result.
- `activity.get_status()` returns a small state enum such as `READY`, `RUNNING`, `PAUSED`, `OFFLINE_READY`, or `UNAVAILABLE`.
- The Compose **GO** button calls a typed Kotlin coordinator action like `startOrResumeFeaturedActivity()`. The coordinator resolves the trusted script ID itself; UI input never supplies arbitrary script source.
- Kotlin guards against duplicate taps, verifies the bundle/API/capabilities, creates at most one active instance, and returns a typed result for the UI.
- Lua cannot draw or control the GO button directly. Lua supplies validated activity metadata and content; Compose owns the button, accessibility, loading/disabled state, and navigation.
- Secondary controls may show details, select another approved bundled activity, or exit. Keep advanced inspection and script control in the debug-only console.

Use an explicit result type such as `Started(instanceId)`, `Resumed(instanceId)`, `AlreadyRunning(instanceId)`, or `Rejected(reasonCode)`. Localize user-facing messages from reason codes. The UI must never report success before Kotlin confirms the result.

## API versioning

Every bundle declares:
- `scriptSchemaVersion`
- `luaRuntimeVersion` or runtime compatibility range
- `hostApiVersion`
- minimum and maximum supported app versions
- required capabilities
- content hash and signature metadata

Reject incompatible bundles before executing code. Add compatibility tests before changing a public host API. Deprecate an API for at least one planned compatibility cycle where practical.
