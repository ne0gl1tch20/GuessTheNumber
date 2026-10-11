# Seasonal Events Migration Audit

## Decision

Do not remove the legacy Seasonal Events claim flow as part of the Lua random-event migration. The two systems currently have different player-facing behavior and are not drop-in replacements.

This document records the compatibility requirements for a separate, reviewable migration. It is an audit and plan, not a claim that the seasonal flow has already been migrated.

## Current seasonal-offer behavior

- `LiveOpsRepository` reads the bundled `game/liveops_manifest.json` offline, validates the manifest, and calculates event status from the device clock.
- `LiveOpsScreen` displays event status, eligibility, reward amounts, claim counts, and the save slot's permanent event bonus level.
- Eligibility is based on correct guesses, Prestige count, and Ultra count. Each event also has a date window and a maximum claim count.
- `GameViewModel.claimLiveOpsEventReward` is the claim entry point. Claims are serialized with a mutex.
- `GameState.liveOpsClaims` and `GameState.permanentEventBoosts` are persisted save-state fields. Existing saves may contain them.
- The `live_ops` and `seasonal_rewards` flags gate the screen and its menu entry.
- The current manifest defines `season_1_cyber_guess` and `holiday_neon_rush`; the latter requires 100 correct guesses.

## Why random events are not equivalent

The Lua random-event script selects one of seven bounded event types for an in-game trigger. Kotlin remains responsible for reward effects and state changes. It does not currently reproduce seasonal event calendars, per-event claim limits, eligibility checks, claim history, or permanent event-boost progression.

Deleting the seasonal flow now would remove those behaviors rather than migrate them.

## Required migration sequence

1. **Keep current behavior intact.** Do not delete the seasonal screen, route, feature flags, claim method, manifest, or save fields in this audit change.
2. **Choose the intended replacement.** Decide whether seasonal offers remain a Kotlin-owned feature with Lua used only for authored event content, or whether a future typed and capability-limited Kotlin-to-Lua API will support the seasonal flow. Do not expose `GameState`, save storage, Android APIs, network access, or arbitrary reward operations to Lua.
3. **Define parity tests before changing runtime code.** Cover active/upcoming/archived windows, invalid manifests, eligibility thresholds, max claims, concurrent claims, reward amount, permanent-boost cap and effects, and offline startup.
4. **Preserve save compatibility.** Continue reading and writing `liveOpsClaims` and `permanentEventBoosts`. Any future removal or transformation requires an explicit versioned migration with tests for old saves.
5. **Implement in a separate PR.** Keep the diff focused. Do not combine it with progressive navigation unlocks or unrelated scripting work.
6. **Verify before sunset.** Run the existing LiveOps tests plus new claim-path tests, the project's normal test/lint/build checks, and a device smoke test. Only remove obsolete UI/routes/flags after the replacement is demonstrated to preserve the agreed behavior.

## Files audited

- `app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/LiveOpsScreen.kt`
- `app/src/main/java/com/jarrlyyy/guessthenumber/data/repository/LiveOpsRepository.kt`
- `app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt`
- `app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/RandomEvent.kt`
- `app/src/main/java/com/jarrlyyy/guessthenumber/ui/viewmodel/GameViewModel.kt`
- `app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt`
- `app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/MoreScreen.kt`
- `app/src/main/assets/game/liveops_manifest.json`
- `app/src/test/java/com/jarrlyyy/guessthenumber/data/repository/LiveOpsRepositoryTest.kt`
- `docs/LUA_RANDOM_EVENTS_MIGRATION.md`
- `docs/LUA_ACTIVITY_HUB_PLAN.md`

## Acceptance criteria for the eventual migration

- No duplicate or free reward claims under concurrent taps.
- Existing saves load without losing claim counts or permanent bonus levels.
- Eligibility, claim caps, date windows, and reward calculations remain deterministic and tested.
- Lua failures cannot crash gameplay or mutate progression directly.
- Seasonal behavior remains offline-capable.
- Navigation and feature flags are removed only after replacement behavior is verified.
