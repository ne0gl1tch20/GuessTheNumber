# GTN Flutter Migration

**Status:** Phase 0 audit and isolated Flutter workspace scaffold  
**Source of truth today:** the existing Kotlin/KMP implementation on `master`  
**Target:** one Flutter/Dart application for Android and Windows, with native integrations only where required.

## Why this is staged

GTN is not a small UI conversion. The repository currently contains a Kotlin/KMP domain engine, Android platform services, a Windows Compose application, a JS/CSS Web Play client, native C++ code, Lua scripts, JSON game content, 23 locale files, trait catalogs, save recovery/backup logic and multiple CI workflows. Replacing all of this in one unverified change would put player saves and game behavior at risk.

This migration is deliberately additive. The existing Android and Windows applications remain buildable until the Flutter replacement passes the gates below. No existing implementation or save format is removed as part of the initial scaffold.

## Audited source areas

| Area | Current source | Migration target |
|---|---|---|
| Deterministic guess rules | `shared/src/commonMain/.../shared/rules` and `shared/domain` | Pure Dart domain package |
| Economy/progression | `shared/src/jvmAndAndroidMain/.../domain/engine` and app repositories | Dart domain services with parity tests |
| BigNumber | `shared/.../domain/model/BigNumber.kt` | Exact decimal/scientific Dart implementation |
| Save slots/recovery | `app/.../data/store/SaveManager.kt` | Versioned Dart persistence + legacy importer |
| Settings | `GlobalPreferencesManager.kt` and `GameSettings` | Dart settings repository |
| Game content | `app/src/main/assets/game`, `game_data` | Shared validated content bundle |
| Localization | `app/src/main/assets/locales` (23 JSON files) | Flutter localization loader using the same source files |
| Traits/catalogs | `app/src/main/assets/traits` | Data-driven Dart trait registry; preserve IDs and semantics |
| Lua | `app/.../scripting`, bundled Lua files | Sandboxed runtime adapter with explicit capability allowlist |
| Native code | `app/src/main/cpp` | FFI only for measured, required native capabilities |
| Android services | notifications, widgets, audio, update/install, backup | Flutter plugins or narrow platform-channel adapters |
| Windows | `desktopApp` | Flutter Windows runner and equivalent platform adapters |
| Web Play | `web-play` | Flutter presentation; preserve web deployment only if still required |

## Reliability rules (non-negotiable)

1. **No big-bang replacement.** Each subsystem is migrated independently behind an adapter.
2. **Kotlin remains the reference oracle** until the corresponding Dart subsystem passes parity tests.
3. **No save loss.** Import the existing 10-slot save format, global preferences, backups and legacy single-save keys. Never overwrite source data during import.
4. **Transactional writes.** A save is committed only after serialization and validation; retain a known-good backup and recover safely from interrupted writes.
5. **Versioned migrations.** Add an explicit schema version and sequential, idempotent migrations. Keep unknown fields when practical and reject destructive/ambiguous conversions.
6. **Deterministic core.** Inject clock, RNG and persistence interfaces. No hidden global state or UI-owned gameplay rules.
7. **Content validation.** Validate schemas, IDs, references, numeric bounds and locale completeness before activating content.
8. **Fail closed for extensions.** Lua and downloaded content cannot access filesystem, network, platform APIs, save internals or rewards without an explicit capability.
9. **No silent fallback for core data.** Invalid game data must produce a visible diagnostic and safe recovery path, not silently start a different game configuration.
10. **CI is a release gate.** Formatting, static analysis, unit tests, migration fixtures, integration tests and platform builds must pass before promotion.

## Migration phases and exit gates

### Phase 0 — audit and scaffold (this PR)
- Record source ownership and migration order.
- Add an isolated Flutter workspace and CI checks.
- Keep all current platforms untouched.
- Exit: Flutter scaffold formats, analyzes, tests and builds independently.

### Phase 1 — compatibility contracts
- Capture real legacy save fixtures (redacted only if they contain personal data).
- Define JSON contracts for GameState, GameSettings, profiles, slots and backups.
- Build Dart import/export/round-trip tests and golden compatibility tests.
- Exit: every available fixture imports without loss; re-export preserves all supported fields.

### Phase 2 — deterministic game core
- Port BigNumber and core guess transitions first.
- Port economy, multipliers, criticals, penalties, prestige/ultra, upgrades, talents, mutators, rewards, challenges and offline earnings.
- Differential-test Dart against Kotlin with identical inputs, seeded randomness and clock.
- Exit: broad generated/property tests and fixed regression vectors match exactly.

### Phase 3 — content and extension runtime
- Bring over all JSON, 23 locales, trait definitions/catalogs, assets and audio.
- Validate content and references at startup/build time.
- Port the Lua sandbox contract; do not widen permissions.
- Exit: content counts, IDs, hashes and behavior match the source release.

### Phase 4 — persistence and platform services
- Implement 10 slots, atomic writes, backups, import/export, global settings, recovery and crash-safe migration.
- Port Android audio, notifications, widgets, lifecycle, update UX and platform integrations.
- Exit: fault-injection tests cover failed writes, interrupted imports, corrupted primary/backup and process restarts.

### Phase 5 — Flutter UI and feature parity
- Rebuild onboarding, save slots, gameplay, shops, upgrades, prestige, ultra, talents, arcade, worlds, achievements, profile, settings, developer tools and remaining screens.
- Keep UI thin: commands go through domain services; widgets never mutate saves directly.
- Exit: screen-by-screen acceptance checklist and Android emulator/device tests.

### Phase 6 — Windows and release hardening
- Add Flutter Windows runner and parity for save paths, startup, dialogs, audio, developer tools and updates.
- Add CI build, artifact validation and crash/recovery tests.
- Exit: Android and Windows pass the same domain contract suite; release candidate passes full regression.

### Phase 7 — cutover
- Ship Flutter as opt-in/beta first.
- Keep a one-way, tested import from Kotlin saves and retain export/backup.
- Only after real-world verification, switch the default release channel.
- Remove Kotlin/Compose/JS production paths in a separate reviewed change after no rollback dependency remains.

## Required test layers

- Unit: deterministic domain rules and serialization.
- Property-based: arithmetic, progression bounds, state invariants and migration idempotency.
- Differential: Kotlin reference output vs Dart output for the same input corpus.
- Fixture: historical saves, settings, profiles, catalogs and locale files.
- Fault injection: disk full, permission failure, interrupted write, corrupt primary, corrupt backup and interrupted migration.
- Integration: Android lifecycle, app restart, notification/widget actions, backup/restore and native adapters.
- UI: onboarding, slot selection, gameplay, navigation, accessibility and state restoration.
- Release: APK/Windows artifact integrity, package IDs, signing configuration and reproducible version metadata.

## Explicit non-goals for early phases

- Do not delete Kotlin code or disable current release workflows.
- Do not rewrite the save schema before fixtures and import tests exist.
- Do not port the old WebView/HTML bridge as a second game engine.
- Do not replace arbitrary existing libraries with new dependencies without need.
- Do not claim 100% parity based only on a successful build.

## Current audit findings

- Default branch is `master`; this migration branch is additive.
- Android application code contains the production save manager and many platform services.
- KMP common tests already cover deterministic rules and are valuable parity references.
- Save compatibility documentation records 10 slots, primary/backup recovery, legacy keys and the current JSON behavior.
- BigNumber uses arbitrary-precision decimal arithmetic; a Dart port must not use `double` for balances.
- The repository has 3,162 tracked tree entries at audit time, including a large trait/content corpus. Migration completeness must be inventory-driven rather than estimated from screen count.

## Progress tracking

- [x] Repository inventory and architecture audit
- [x] Save-system behavior reviewed
- [x] BigNumber and deterministic-rule entry points reviewed
- [x] Isolated Flutter workspace scaffold
- [ ] Legacy save fixtures and schema contracts
- [ ] BigNumber parity implementation
- [ ] Full deterministic game-core parity
- [ ] Content/locale/trait parity
- [ ] Save repository and fault-injection suite
- [ ] Android UI and service parity
- [ ] Windows parity
- [ ] Release cutover
