# Implementation Plan - Comprehensive Localization of UI Strings

## Problem
While core navigation and select screens use `LocaleManager`, a large number of UI strings across screens (Arcade, Cloud Backup, Crash Recovery, Dev Settings, Live Ops, More, Mutators, Play, Prestige, Save Slots, Settings, Staking, Stats, Talents, Tutorial, Ultra, Upgrade, etc.) and in-game JSON config entries (shop items, mutators, minigames, challenges, talents) are currently hardcoded in English inside the Compose files and JSON assets.

## Proposed Changes

### 1. Locale Assets (`en_us.json`)
- Add translation keys for all screens:
  - Arcade titles, minigame descriptions, buttons ("Play", "Guess", "Claim Reward", "Done", "Box", "Win/Lose text")
  - Cloud Backup titles, buttons, status messages
  - Crash Recovery warning, logs, actions
  - Dev Settings titles, console, tooltips
  - Live Ops seasonal event titles, descriptions, buttons
  - More Hub titles, items, music player labels
  - Mutators & Challenges titles, buttons ("Activate", "Disable", "Claim", "Completed")
  - Play Screen input prompt, guess button, status labels
  - Prestige, Ultra, Upgrade, Talents, Staking, Stats, Settings titles and buttons
  - All JSON items in `minigames.json`, `mutators.json`, `challenges.json`, `shop_items.json`, `prestige_shop.json`, `ultra_shop.json`, `talents.json` via `JsonConfigRepository` mapping.

### 2. UI Screens & Repositories
- Update `JsonConfigRepository` to dynamically load and fallback names & descriptions for minigames, mutators, challenges, talents, and shop items via `LocaleManager`.
- Update all Composable screen files to use `locale.getString("key", "default")` for all user-facing text.

## Verification Plan
- Build the project with `gradle_build(":app:assembleDebug")` to ensure 0 compilation errors.
- Test app UI interaction and verify locale strings.
