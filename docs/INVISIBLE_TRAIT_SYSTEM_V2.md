# Invisible Trait System v2

## Goal

Provide positive, quiet gameplay assistance and progression recognition without exposing a visible trait-management burden to the player.

## Architecture

1. **GameState** remains the authoritative save envelope.
2. **TraitProfile** is a versioned nested save object. Missing fields in older saves use defaults, so existing saves remain readable.
3. **InvisibleTraitEngine** observes gameplay and produces bounded helper/reward intents.
4. **TraitGameplayRules** applies those intents through the existing gameplay result pipeline for Android and desktop parity.
5. **JSON** declares the approved helper/bonus vocabulary and hard limits.
6. **Lua** provides the adaptive decision policy. It receives a sanitized snapshot and can only request known IDs.
7. **Kotlin** validates and applies every actual gameplay effect. Lua never writes save data or chooses currency amounts.

## Legacy migration

No destructive migration is required. `GameState.traitProfile` defaults to `TraitProfile()` when an older JSON save does not contain the field. Existing `prestigeCount`, `ultraCount`, money, unlocks, statistics and trait discovery fields remain authoritative.

## Adaptive helpers

- `mistake_insurance`: reduces an existing miss penalty after repeated failures.
- `range_scout`: narrows the remaining range after repeated misses.
- `streak_guard`: preserves a streak on a miss when the existing streak-protection contract allows it.
- `comeback_reward`: gives a small bounded reward multiplier after a difficult run.
- `momentum_reward`: gives a small bounded reward multiplier during strong streaks.

The helpers are intentionally capped. They should make the game feel helpful, not fake a win.

## Progression bonuses

The system recognizes:

- first Prestige
- first Ultra
- veteran progression at ten Prestiges

Rewards are recorded in `TraitProfile.awardedBonuses`, making them idempotent across save reloads.

## Lua contract

The Lua policy may read only a sanitized snapshot such as:

- current streak
- correct/failed guess counters
- best streak
- Prestige count
- Ultra count
- one-time bonus ledger state

It returns helper and bonus IDs. The host validates those IDs against `trait_profile.json`, applies the configured bounds and owns all persistence.

Lua must not receive raw save JSON, filesystem access, network access, arbitrary Java/Kotlin objects, or unrestricted reward amounts.

## Test requirements

The trait test suite covers:

- legacy saves with no profile
- repeated-failure helper activation
- one-time progression bonus idempotency
- profile persistence through the gameplay transition
- bounded helper count
- deterministic behavior

Future CI should additionally exercise Android/desktop save reload parity and malformed Lua output rejection.
