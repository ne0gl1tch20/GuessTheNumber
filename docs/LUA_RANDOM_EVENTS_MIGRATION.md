# Lua-Powered Random Events Migration

## Current implementation in this change

- The bundled script `scripts/events/random_event.lua` decides whether an event triggers and selects one of the seven supported event IDs.
- Kotlin validates the returned table and maps the event ID to the closed `RandomEventType` enum.
- Kotlin's `RandomEventEngine.apply` remains the only code that changes currencies, streaks, targets, statistics, or save state.
- Lua selection failures are logged and treated as a missed event. The normal guessing game continues.
- The existing one-minute scheduling, event notification, save behavior, and eight-second event banner remain in Kotlin.

## Deliberately not exposed to Lua

Lua does not receive `GameState`, currency values, save files, DataStore, Android APIs, network access, or arbitrary reward operations. The script can only return a bounded selection descriptor. Unknown event IDs are rejected before any game-state change.

## Follow-up: retire the legacy seasonal-offer flow

The old Seasonal Events screen is a separate limited-time claim/reward system backed by `liveops_manifest.json`; it is not the same feature as the periodic random-event selector. Do not delete it in the same change that moves random-event selection.

After this selector passes CI and device checks, a separate change may sunset the seasonal-offer screen, navigation entry, feature flags, and claim entry point. Keep existing save fields and permanent bonus calculations compatible with existing saves unless a separately tested save migration explicitly changes them.

## Validation

- Unit tests verify all supported event IDs still apply their reward effects in Kotlin.
- Robolectric tests execute the bundled Lua selector repeatedly and assert it returns only supported IDs or no event.
- The normal unit test, lint, full Gradle check, Debug APK, and unsigned Release APK workflow must pass.
- On-device, confirm events still appear periodically, notifications/banner still work, and no event is granted when the Lua selector fails.
