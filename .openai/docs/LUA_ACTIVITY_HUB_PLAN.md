# Lua Testing Hub and Activity Hub plan

## Goals

- Present the player-facing Lua experience as **Activity Hub**, without exposing implementation terminology.
- Provide a separate **Lua Testing Hub** in the More menu for debug builds only.
- Keep Kotlin authoritative for progression, rewards, saves, and all permanent game-state changes.
- Sunset the legacy seasonal-event UI/runtime only after needed behavior has migrated to Lua-powered random events.

## Delivery sequence

1. Audit the current navigation, Lua engine, feature flags, random-event engine, and legacy seasonal-event flow.
2. Add the debug-only Lua Testing Hub. It should report runtime status, list registered scripts, run a registered script through the existing restricted engine, display errors, and stop active scripts. Do not add a raw Lua REPL.
3. Rename the user-facing Lua Activity Hub to Activity Hub. Keep the existing `lua_scripting` feature flag controlling player access.
4. Design and implement a typed Kotlin-to-Lua game API bridge with explicit capabilities and Kotlin-side validation.
5. Migrate legacy seasonal-event behavior to Lua-powered random events in a separate change. Preserve current event behavior until the replacement passes tests.
6. Remove obsolete seasonal-event navigation/code only after the migration is verified.

## Access rules

- Activity Hub: available in debug or release when `lua_scripting` is enabled.
- Lua Testing Hub: only registered in debug builds and shown in More only when `dev_console` is enabled.
- Raw Lua source evaluation remains disabled. Testing uses registered bundled scripts or verified cached LiveOps scripts through the same sandbox and instruction budget.
- Release builds must not expose the testing route, regardless of direct navigation attempts.

## Acceptance checks

- Debug build: Activity Hub follows `lua_scripting`; Lua Testing Hub appears only with `dev_console`.
- Release build: Activity Hub follows `lua_scripting`; Lua Testing Hub route and More entry are absent.
- Unknown scripts, invalid script output, and runtime failures show errors without crashing the app or modifying permanent save data.
- Starting and stopping scripts correctly cleans up the active activity.
- Existing signed LiveOps verification, anti-rollback behavior, and offline cache fallback remain unchanged.
- Legacy seasonal events are not removed until the Lua replacement has equivalent behavior and regression coverage.
