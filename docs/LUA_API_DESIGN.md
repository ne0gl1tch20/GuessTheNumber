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

## Console and diagnostics

Keep the existing developer console as the command entry point. Add a distinct `/lua` command family only in debug builds, for example:
- `/lua status`
- `/lua list`
- `/lua inspect <script-id>`
- `/lua run <bundled-script-id>`
- `/lua stop <instance-id>`
- `/lua logs`

These names are proposals. Do not provide a general `eval` command for arbitrary typed Lua in release builds. A debug REPL, if ever added, must be debug-only, local-only, resource-limited, and clearly separated from production script execution.

The log viewer should merge central app logs and Lua diagnostics using timestamp, level, source, event/script IDs and correlation ID. Preserve severity filtering and log-storage preferences. Bound log volume and redact arbitrary payloads.

## API versioning

Every bundle declares:
- `scriptSchemaVersion`
- `luaRuntimeVersion` or runtime compatibility range
- `hostApiVersion`
- minimum and maximum supported app versions
- required capabilities
- content hash and signature metadata

Reject incompatible bundles before executing code. Add compatibility tests before changing a public host API. Deprecate an API for at least one planned compatibility cycle where practical.
