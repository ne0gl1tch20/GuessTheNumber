# Seasonal Events Migration Audit

## Decision

Move seasonal event definitions into a bounded, bundled Lua data script. Keep `game/liveops_manifest.json` unchanged as a validated compatibility fallback. Kotlin remains authoritative for date windows, eligibility, claim limits, reward calculations, save-state mutations, feature flags, navigation, and UI.

This is a content migration, not a rewrite of the Seasonal Events system and not a replacement of seasonal claims with random events.

## What this PR changes

- Adds `scripts/events/seasonal_events.lua` with the same two event definitions as the existing JSON manifest.
- Adds `LuaSeasonalEventCatalog`, which parses a bounded list of typed fields and passes the result through the shared Kotlin `LiveOpsContentValidator`.
- Makes `LiveOpsRepository` prefer the bundled Lua catalog and fall back to the existing JSON manifest if Lua execution or catalog validation fails.
- Leaves the JSON manifest, claim policy, save fields, feature flags, route, and screen behavior in place.
- Adds parity and fallback tests to prevent the Lua catalog from silently drifting from the JSON compatibility copy.

## Responsibilities that stay in Kotlin

- Manifest schema and event content validation.
- Date-window evaluation and the `UPCOMING` / `ACTIVE` / `ENDING` / `ARCHIVED` status calculation.
- Feature-flag checks and screen/menu routing.
- Eligibility checks for correct guesses, Prestige, and Ultra.
- Claim serialization, max-claim enforcement, reward application, statistics updates, and save persistence.
- Permanent event-boost cap and its effects.
- Existing `GameState.liveOpsClaims` and `GameState.permanentEventBoosts` save fields.
- The current Seasonal Events UI and its localization behavior.

Lua returns content only. It receives no game state and has no capability to award currency, change save data, access Android APIs, or access the network.

## Compatibility and tests

- Existing JSON remains the fallback if the Lua catalog fails or returns invalid content.
- The Lua catalog is checked against the unchanged JSON manifest.
- The repository still calculates event status from the device clock after loading the catalog.
- Existing save data and the current claim policy are unchanged.
- Offline startup remains supported.

## Remaining trade-off

The Lua catalog and JSON fallback intentionally duplicate the two event definitions. The parity test guards that duplication. Removing the JSON copy is not part of this PR; it should only be considered after the fallback and content-authoring workflow are settled. A general Lua bridge or Lua-owned reward policy is not justified by the current requirements.
