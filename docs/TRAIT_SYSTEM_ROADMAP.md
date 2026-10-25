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

- New `GTNSEC2:` versioned transfer envelopes use AES-256-GCM with authenticated data and an Android Keystore-protected non-exportable key.
- Save Slots export now uses `exportSecureSave()` and import uses `importSecureOrLegacySave()` through the existing view-model actions.
- Secure envelopes are authenticated before their payload reaches `SaveManager`; malformed/tampered envelopes are rejected.
- Existing legacy encrypted/plain import remains accepted through an explicit compatibility path, covered by a regression test.
- The secure export key is device-bound in this phase because the current transfer UI has no passphrase/key-sharing flow. A future portable export format must add explicit user-controlled key material rather than embedding a secret in the APK.
- C++ is not used to implement cryptography from scratch. Native code is used for the gameplay algorithm; platform cryptography provides the save-transfer security boundary.

### Phase 6 acceptance

- Android native build, production-engine integration tests and secure-transfer tests pass in GitHub Actions.
- No save state or reward authority moves into C++.
- Native output stays within supplied bounds and deterministic benchmark inputs produce matching checksums.
- Secure envelope ciphertext/tag tampering and malformed envelopes are rejected without a legacy downgrade; legacy import compatibility is covered by tests.
- The measured native-vs-Kotlin core comparison supports keeping the native path enabled, with JNI overhead explicitly excluded from the performance claim.
- Android validation/tests, Windows build/tests, HTML/CSS/JS checks, native/security validation and the native-vs-Kotlin benchmark all passed on the final save-transfer integration commit.

## Phase 7 — Creative mode save data and effects: planned after Phase 6

- Define a versioned creative-mode save schema with safe defaults, validation and migration.
- Decide which creative settings and custom effects are per-save-slot and which are global.
- Build bounded effect definitions and validation so custom effects cannot directly grant rewards, corrupt normal saves or bypass gameplay rules.
- Cover import/export, legacy compatibility, localization, malformed data, slot isolation and round-trip tests before connecting creative effects to gameplay.

## Completion rule

No phase adds features outside its acceptance criteria. Phase 6 is complete only after its security/native tests and measured comparison are green. If the comparison shows Kotlin is already fast enough, the native path may be reduced or removed and Phase 6 can still be considered successful. Phase 7 then handles creative-mode save data and effects separately.
