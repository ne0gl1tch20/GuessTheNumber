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

## Phase 5 — Measure and optimize runtime: in progress

Acceptance criteria:
- Keep a repeatable warmed JVM benchmark for the activation hot path and expanded catalog.
- Report median and p95 evaluation latency plus throughput without flaky machine-dependent pass/fail thresholds.
- Preserve deterministic results and existing activation behavior.
- Remove avoidable per-guess allocations only where profiling supports the change.
- Run Android, Windows and HTML checks after optimization and compare benchmark output from before/after commits.

## Phase 6 — Native/C++ evaluation: conditional, not automatically required

Do **not** port the trait engine to C++ merely to complete a numbered phase. Kotlin/JVM is the default implementation.

Only open a native implementation experiment if Phase 5 demonstrates a material runtime bottleneck after Kotlin-side optimization and profiling identifies work that would benefit from native code. Any experiment must:
- show a repeatable improvement against the same benchmark and representative workloads;
- include interop, packaging, memory, debugging and maintenance costs in the comparison;
- preserve Kotlin as the authority for gameplay state, eligibility, saves and rewards;
- be removable without changing save formats or gameplay behavior.

If Kotlin meets the project's performance needs, record the measurements and mark Phase 6 **not needed**. That is a successful outcome, not unfinished work.

## Completion rule

No phase adds features outside its acceptance criteria. Once Phase 5 meets its criteria and Phase 6 is either justified by evidence or explicitly marked not needed, the trait system is complete. New trait content and unrelated features should be handled separately.
