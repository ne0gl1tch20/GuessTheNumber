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

## Phase 6 — Native C++ engine and save-security work: next

Phase 6 investigates native code for two real jobs, rather than porting Kotlin wholesale:

- **Personalized gameplay algorithms:** profile and implement only compute-heavy algorithms that benefit from native execution. Difficulty must read the difficulty selected for the active save slot when that slot is created; it must not silently use a global difficulty or leak settings across slots.
- **Save import/export security:** design a versioned, authenticated encryption format using a vetted cryptographic library. Keep accepting supported legacy-encrypted imports through an explicit compatibility path. Validate and authenticate data before applying it to live state, and use platform key protection where appropriate. C++ by itself is not a security guarantee.
- **Architecture:** Kotlin remains authoritative for gameplay state, save validation, rewards and migration. Native interfaces must be narrow, bounded, testable and removable.
- **Proof:** compare representative workloads against the Phase 5 Kotlin/JVM reference profile, including JNI overhead, memory use and maintenance costs. Keep native code only where it provides a measurable benefit or a clearly justified security integration.

## Phase 7 — Creative mode save data and effects: planned after Phase 6

- Define a versioned creative-mode save schema with safe defaults, validation and migration.
- Decide which creative settings and custom effects are per-save-slot and which are global.
- Build bounded effect definitions and validation so custom effects cannot directly grant rewards, corrupt normal saves or bypass gameplay rules.
- Cover import/export, legacy compatibility, localization, malformed data, slot isolation and round-trip tests before connecting creative effects to gameplay.

## Completion rule

No phase adds features outside its acceptance criteria. Once Phase 5 meets its criteria and Phase 6 is either justified by evidence or explicitly marked not needed, the trait system is complete. New trait content and unrelated features should be handled separately.
