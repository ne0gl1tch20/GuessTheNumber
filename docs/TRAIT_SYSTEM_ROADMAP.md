# Trait System Roadmap

This roadmap is the source of truth for the trait-system phases. The phases are gates, not a promise to add complexity forever. A phase is complete when its acceptance criteria are met and its required checks pass.

## Phase 1 — Trait foundations: complete

- Stable trait IDs and validated declarative definitions.
- Immutable registry snapshots with duplicate, dependency and conflict validation.
- Save-compatible discovery and selection state.

## Phase 2 — Authoritative gameplay integration: complete

- Kotlin owns activation, effect validation, save state and rewards.
- Traits change real gameplay behavior through bounded effect intents.
- Invalid or locked arm selections are rejected.
- Gameplay tests cover payout changes, miss penalties, range hints and streak behavior.

## Phase 3 — Catalogs and extension modules: complete

- Expanded offline trait catalog with package files, localization and icon validation.
- Lua, JSON and JavaScript extension formats have distinct boundaries and capability checks.
- Extension code cannot directly mutate saves or award currency.
- Locale, registry, runtime and extension-host tests are part of CI.

## Phase 4 — Signed online catalogs: complete

- Versioned manifest signatures and SHA-256 payload checks.
- Schema, compatibility, dependency/conflict, size and built-in-ID validation.
- Verified staging, rollback rejection and offline fallback.
- Remote content stays declarative and cannot execute arbitrary code.
- Android, Windows, HTML/CSS/JS, LiveOps reliability and asset-manifest checks passed on the merged PR.

## Phase 5 — Measure and optimize runtime: complete

Acceptance criteria:
- Keep a repeatable warmed JVM benchmark for the activation hot path and expanded catalog.
- Report median and p95 evaluation latency plus throughput without flaky machine-dependent pass/fail thresholds.
- Preserve deterministic results and existing activation behavior.
- Remove avoidable per-guess allocations only where profiling supports the change.
- Run Android, Windows and HTML checks after optimization.
- Record the optimized Kotlin/JVM hot-path measurements as the reference baseline for any Phase 6 native comparison.

Latest CI profile (OpenJDK 21, 320 traits, 1,000 warmups, 21 samples × 200 evaluations): median 32,297 ns/evaluation, p95 40,661 ns/evaluation, median throughput 30,962 evaluations/second. Results are diagnostic, not a machine-speed pass/fail threshold. The report is uploaded by the `Trait Runtime Profiling` workflow.

## Phase 6 — Native C++ engine and save-security work: complete

Phase 6 is deliberately narrow: use native code for personalized compute work, while keeping Kotlin authoritative. Do not port the whole game engine just because C++ exists.

### Native gameplay

- Android now builds a C++20 shared library through CMake/NDK and exposes one narrow JNI function.
- `NativeGameplayOptimizer` is wired into production `GameEngine.processGuess()` for next-target generation after a correct guess. It accepts only bounded range, difficulty ID, streak, attempts and a seeded random input, returns a clamped candidate target, and falls back to Kotlin when native loading or invocation fails.
- The native algorithm receives the **difficulty stored in the active save state** plus that save's streak and attempts. It never reads or mutates save files, rewards or game state; Kotlin validates the transition and remains authoritative.
- A repeatable native-vs-Kotlin core benchmark runs in GitHub Actions and uploads its report. For 128 deterministic cases, 2,000 warmups and 21 × 5,000 iterations, Kotlin median/p95 were 20.671/35.392 ns with 48.38M evaluations/s; native median/p95 were 5.999/6.306 ns with 166.68M evaluations/s. Checksum parity passed and both outputs stayed bounded. The microbenchmark excludes Android JNI-call overhead, so it does not claim whole-game speedup.

### Save transfer security

- New Save Slots exports use the portable `GTNPORT1:` envelope: PBKDF2-HMAC-SHA256 (310,000 iterations), random 128-bit salt, AES-256-GCM, random 96-bit IV and authenticated format metadata. The key is derived from a user passphrase, not Android Keystore, so a backup can be restored on another device with the same passphrase.
- Export asks users to confirm a 12+ character passphrase; import accepts the passphrase without persisting it. The backup format remains compatible across devices using the same format and passphrase.
- Wrong passphrases, altered ciphertext, malformed envelopes and unknown `GTNPORT` versions are rejected. Portable-format failures never downgrade to the legacy parser.
- Existing device-bound `GTNSEC2:` envelopes and supported legacy encrypted/plain imports remain importable through explicit compatibility paths.
- C++ is not used to implement cryptography from scratch. Native code is used for gameplay compute; standard platform/JVM cryptography provides the save-transfer security boundary.

### Phase 6 acceptance

- Android native build, production-engine integration tests and secure-transfer tests pass in GitHub Actions.
- No save state or reward authority moves into C++.
- Native output stays within supplied bounds and deterministic benchmark inputs produce matching checksums.
- Secure envelope ciphertext/tag tampering and malformed envelopes are rejected without a legacy downgrade; legacy import compatibility is covered by tests.
- The measured native-vs-Kotlin core comparison supports keeping the native path enabled, with JNI overhead explicitly excluded from the performance claim.
- Android validation/tests, Windows build/tests, HTML/CSS/JS checks, native/security validation and the native-vs-Kotlin benchmark all passed on the final save-transfer integration commit.

## Phase 7 — Creative mode save data and effects: complete

- Added a versioned Creative Mode schema (v2) with safe defaults, strict bounds and migration from v1. Unsupported schema versions and malformed definitions are rejected.
- Creative Mode configuration is stored per save slot, exposed in the Save Slots UI, copied with slot duplication and removed when that slot is reset.
- The full encrypted backup bundle is now version 3 and includes Creative Mode configurations. Import remains compatible with version 2 bundles and migrates older Creative Mode schemas safely.
- The effect allowlist supports bounded range scaling, extra hints and attempt limits only. Effects cannot execute arbitrary code, award currency or mutate progression; Kotlin validation remains authoritative.
- Added tests for schema migration, unsupported/malformed saves, effect bounds, disabled effects, full-backup serialization and per-slot isolation/reset.
- Portable backup tests cover authenticated round-trip, wrong passphrase, tampering, malformed/unknown versions and legacy import routing.
- Android validation, Phase 6 native/security validation, Windows build/tests and HTML/CSS/JS checks passed on the final implementation commit.


## Phase 8 — Creative Mode runtime core: in progress

- Added a deterministic, immutable Creative Mode run model with validated range, guess outcomes, attempt-limit enforcement and bounded hint usage.
- Rejected/out-of-range guesses do not consume attempts; completed runs reject further guesses.
- The run runtime has no currency, reward, progression, filesystem or script capability.
- The Save Slots screen now launches/resumes a run from the selected slot, submits guesses, consumes bounded parity hints, supports restart and persists run snapshots through the existing slot save API.
- CI and a device-level interaction check are still required before this phase can be marked complete.

## Phase 9 — Creative Mode session persistence and recovery: in progress

- Added schema v3 run snapshots with bounded IDs, range/target validation, attempt/hint counters and completed-state validation.
- Added explicit runtime helpers to persist a run into its per-slot Creative Mode config, resume it after decoding, and clear a saved run.
- Existing encrypted backup bundles already carry the Creative Mode config payload; schema v1/v2 imports migrate forward and never invent an active run.
- Still required: call these helpers from the actual player-facing Creative Mode session lifecycle and verify crash/relaunch recovery, reset/duplicate behavior and atomic writes.

## Phase 10 — Creative effect composition and authoring: in progress

- The Save Slots editor exposes range scaling, extra hints and attempt limits; the shared policy composes them into bounded runtime rules.
- Effects remain declarative and allowlisted. Creative Mode runs have no currency, reward, progression or script capability.
- Runtime checks reject guesses outside the run range, over-limit guesses, stale snapshots and runs whose rules no longer match the saved configuration.
- Still required: add a clear effect preview/conflict explanation and broaden end-to-end tests for combinations before marking this phase complete.

## Phase 11 — Trait/season lifecycle policy: in progress

- Centralized the offline calendar in TraitSeasonPolicy; season windows use inclusive start/exclusive end boundaries and correctly cross the winter year boundary.
- Season selection is deterministic from a supplied date for tests and replayable behavior.
- Still required: connect the lifecycle policy to catalog activation/refresh and document what happens to currently active seasonal traits when a season changes.

## Phase 12 — Trait/effect interoperability and catalog hardening: planned

- Define the narrow bridge between Creative Mode modifiers and trait effect intents; reject unsupported overlaps instead of stacking them accidentally.
- Add tests for duplicate IDs, conflicts, seasonal transitions, remote-catalog rollback, offline fallback and save migrations.
- Expand trait/season content only when its effect has an authoritative Kotlin implementation and localization/test coverage.
- Keep signed remote catalogs fail-closed and built-in traits available offline.

## Phase 13 — Final integration and release verification: planned

- Verify Android, Windows, HTML/CSS/JS, native/security and trait profiling workflows against the exact final commit.
- Audit save compatibility from legacy imports through the current portable backup bundle.
- Update the player changelog and the PR ledger for every shipped change.
- Bump app version only alongside a release-ready set of changes and record the exact versionCode.
- Keep on-device visual/bridge verification separate from CI claims.

## Completion rule

No phase adds features outside its acceptance criteria. A phase is complete only when the implementation is connected to its real production flow, tests cover the acceptance criteria, and required CI checks pass. Documentation and a pure runtime model alone do not count as full gameplay integration. Native performance claims must continue to exclude JNI overhead unless that overhead is measured. Creative effects remain bounded data-driven policy, not arbitrary scripts or reward authority.
