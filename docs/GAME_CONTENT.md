# Game Content, Schemas and Feature Flags

This game keeps content bundled and offline. Kotlin remains authoritative for gameplay rules, progression calculations and reward application; JSON describes tunable content and event parameters.

## Schema versioning

Bundled content JSON uses `schemaVersion`. The currently supported schema is **1**.

- Keep the schema version unchanged for backward-compatible additions that have defaults.
- When changing required fields or their meaning, update the model and loader, add an explicit migration or compatibility path, then bump the schema version.
- Unsupported versions must not be interpreted as current data. The repository rejects them and uses its documented fallback.
- `liveops_manifest.json` also has `manifestVersion`, which versions the event manifest independently from the shared JSON schema.

## Content files

- `worlds.json`: world and boss definitions, progression requirements, map positions and base rewards.
- `game_config.json`: starting range and money, critical chance, Ultra cap and auto-clicker income.
- `upgrades.json`, `prestige_upgrades.json`, `ultra_upgrades.json`: upgrade definitions and balance values.
- `shop_items.json`, `prestige_shop.json`, `ultra_shop.json`: shop IDs, costs and descriptions.
- `achievements.json`, `challenges.json`, `minigames.json`, `talents.json`: rewards, multipliers and progression relationships.
- `guess_feedback_messages.json`: localized feedback message counts.
- `liveops_manifest.json`: event schedules, reward amounts, claim caps and eligibility requirements.
- `feature_flags.json`: offline feature switches.

## Feature flags

Edit `app/src/main/assets/game/feature_flags.json`:

- `live_ops`: enable the seasonal events area.
- `seasonal_rewards`: enable event offers and claims.
- `world_map`: enable the World Map destination.
- `challenge_builder`: enable the Mutators and Challenge Builder destination.
- `dev_console`: enable developer tools in debug builds only.

Missing keys use the documented defaults in `DefaultFeatureFlags`. The UI menu and navigation destinations consult the same repository. LiveOps reward claims re-check the relevant flags in the ViewModel.

## LiveOps event fields

Each event includes:

- `startDate` and `endDate`: ISO-8601 UTC timestamps.
- `rewardNebula`: Nebula paid per successful claim.
- `permanentBoostPerClaim`: permanent event boost increment, capped by the game rules.
- `maxClaims`: per-save-slot claim limit.
- `minCorrectGuesses`, `minPrestigeCount`, `minUltraCount`: eligibility requirements.

The UI displays the configured reward and requirements. The ViewModel validates the current schedule, feature flags, eligibility and claim count again before applying a reward. LiveOps is bundled content and does not fetch remote event data.

## Validation and tests

`ContentValidation`, `WorldConfigRepository` and `LiveOpsContentValidator` reject invalid schemas, duplicate/blank IDs, invalid rewards/costs, bad multipliers, missing talent-parent/world-boss references, malformed schedules and invalid eligibility requirements. Loaders then log a warning and use their documented fallback rather than trusting malformed content.

Relevant tests:

- `ContentValidationTest`
- `JsonConfigRepositoryTest`
- `WorldConfigRepositoryTest`
- `LiveOpsRepositoryTest`
- `GameEngineTest`

Run the unit test task before release:

```bash
./gradlew :app:testDebugUnitTest
```

Do not treat source inspection as a passing test run. Confirm the actual Gradle result before release.
