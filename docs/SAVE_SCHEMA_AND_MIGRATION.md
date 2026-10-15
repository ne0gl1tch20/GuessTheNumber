# Save Schema and Migration Audit

**Audit date:** 2026-10-15  
**Scope:** Current Android save model and `SaveManager` behavior, based on source inspection. This describes observed behavior; it does not introduce a new schema or migration.

## Current serialized model

The primary save payload is a kotlinx.serialization JSON encoding of `GameState`. Its nested serialized types include `GameStatistics`, `GameSettings`, and `BigNumber`. The model currently has no dedicated integer save-schema-version field. `lastSavedVersion` records the app version and should not be treated as a schema version.

The current `GameState` fields fall into these groups:

- **Core run/economy:** difficulty and profile identity, money/prestige/ultra/Nebula, current range and target, attempts, correct guesses, streaks, upgrade-level maps and shop purchases.
- **Progression/rewards:** mutators, achievements, completed challenges, statistics, prestige/ultra reset counts, LiveOps claims, permanent event boosts, daily quests and loot.
- **World/endgame:** unlocked/active worlds, defeated bosses, boss progress, relic inventory/equipment, mastery, secrets, Codex entries, Home Base, boss-battle state and Endless Rift state.
- **Session and migration metadata:** last-save timestamp, app version, time-travel penalty timestamp, tutorial state, auto-clicker state, buy multiplier and last random event.
- **Settings:** audio/haptics/notifications, display and number formatting, locale, reduced motion/flashes, volume, user-selected music paths/queues/playlists, theme customization, More-screen ordering/colors and feature-tour state.

Field names and defaults are defined by `app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt`. `GameSettings` is also stored through global preferences and may be stripped from per-slot saves after global-settings migration is marked complete.

## JSON compatibility behavior

`SaveManager` configures kotlinx.serialization JSON with:

- `ignoreUnknownKeys = true`: fields introduced by a newer app do not by themselves reject an older app's parse.
- `encodeDefaults = true`: writes include default-valued fields.
- Missing fields can use model constructor defaults, provided the model can deserialize the remaining payload.

This is permissive compatibility, not a formal migration registry. There is no explicit per-schema migration dispatch in the inspected `GameState`/`SaveManager` path. A new field with a safe default can be backward-compatible; renames, type changes or semantic changes need an explicit migration and fixture tests.

## Storage layout and recovery

- Preferences DataStore: `game_save_prefs`.
- Active slot key: `active_save_slot`, clamped to slots 1–10.
- Per-slot primary: `game_state_slot_<n>_json`.
- Per-slot recovery backup: `game_state_slot_<n>_backup_json`.
- Legacy single-save keys: `game_state_json` and `game_state_backup_json`.
- Legacy migration validates the old primary, copies it to Slot 1 and copies a non-empty legacy backup if present, then removes the legacy keys in one DataStore edit.
- `saveGame` replaces the backup with the existing primary only when that primary validates; a corrupt primary does not overwrite a valid recovery backup.
- `loadGame` prefers a valid primary, otherwise restores a valid backup to the primary key. If neither is usable, it returns a fresh state.
- `updateGameAtomically` edits primary and backup together after validating the primary and transformed output. It returns `null` for a missing/invalid primary, invalid output or a caught exception. It does not currently initialize a missing slot for an atomic transform.
- Export includes valid slots/backups plus global settings and active slot. Import validates each imported save and writes the accepted slots in a DataStore edit.

The backup/export helper names include “encrypt”, but the inspected implementation is XOR plus Base64 with a fixed salt marker and a plain-Base64 fallback. This is obfuscation, not modern cryptographic confidentiality or integrity protection. Do not claim otherwise or silently change the existing format in a compatibility-only PR.

## Validation and normalization

`validateSave` checks JSON deserialization, known difficulty, non-negative balances and selected counters/maps, valid range ordering, target-in-range and a positive save timestamp. It is not a complete semantic validator for every field in `GameState`; for example, many progression IDs, settings strings and cross-field relationships are not exhaustively checked there.

`sanitizeLoadedState` clamps core balances/counters and repairs the core range/target, difficulty and profile identity. It does not normalize every nested statistic, progression collection or settings value. Any validation hardening must be additive and tested against existing valid saves rather than silently rejecting old payloads.

## Compatibility evidence and known gaps

- PR #152 adds an inline legacy JSON regression: newer fields omitted, an unknown future field present, large balances and progression data loaded, saved and reloaded.
- The regression is representative but is not a historical byte-for-byte fixture from a released build.
- Settings and save-slot metadata need additional round-trip coverage.
- Existing tests cover corrupt-primary rejection without overwriting a valid backup, and atomic challenge updates across reload. They do not inject a genuine DataStore I/O failure.
- The ViewModel can publish challenge-updated in-memory state before its asynchronous save completes. A failed persistence operation can therefore leave memory ahead of disk. Fixing this needs explicit write-result handling and serialized state/persistence coordination; do not solve it by blindly replacing current memory with a possibly stale saved snapshot.
- There is no dedicated save-schema version/migration registry yet. Adding one should be a separate, tested design after current behavior is documented.

## Next steps

1. Add representative fixtures from actual historical save formats where available, plus settings and slot metadata round-trip coverage.
2. Design a test seam for DataStore write failure without changing production behavior just to satisfy a test.
3. Test failure outcomes for both `saveGame` and `updateGameAtomically`, including preservation of primary/backup and ViewModel state consistency.
4. Only then design persistence coordination for reward claims and decide whether an explicit schema-version/migration registry is warranted.
