# Traits & Seasonal System: Implementation Plan

Status: architecture plan only. No runtime implementation is included in this change.

## Product goal

Build a large trait registry for GuessTheNumber with:
- 300+ built-in traits available without network access.
- A target of 150+ additional downloadable LiveOps traits.
- Visible, invisible, and seasonal traits using one versioned definition model.
- Background synchronization that does not block startup or gameplay.
- A Developer Settings inspector for search, state, validation, diagnostics, and timing.
- Compatibility with existing saves, the current Lua Activity Hub, and existing signed LiveOps bundles.

These are catalog targets, not a reason to create hundreds of separate code paths. Traits should mostly be declarative definitions evaluated by a small, testable engine.

## Findings from the repository audit

- Android app uses Kotlin and Jetpack Compose; the Android module already declares NDK version 30.0.16248370.
- The app already uses LuaJ (libs.luaj.jme) through LuaEngine.kt and LuaSandbox.kt. This is a JVM Lua runtime, not a native Lua/C++ runtime.
- Existing remote Lua activities are downloaded by LuaLiveOpsRepository.kt, with HTTPS, a signed manifest, per-script SHA-256 checks, staging, version checks, and recovery behavior.
- The current LiveOps payload is an activity catalog. It should not be overloaded with trait definitions: traits need a separate schema and validation contract.
- Current LiveOps repository config limits the activity bundle to 50 scripts. Trait catalog sizing and delivery should be designed separately.
- No C++ source files, CMake project, or JNI bridge were found in the current repository tree. Adding native code would be a new subsystem.
- WorkManager is already an app dependency, but no dedicated Lua LiveOps worker was found in the source tree during this audit.

## Architecture decisions

### Authority and execution flow

1. Kotlin owns authoritative game state, save data, reward/economy mutations, trait activation policy, and the registry lifecycle.
2. Built-in and downloaded trait definitions are parsed and validated in Kotlin.
3. The existing LuaJ sandbox remains the initial script runtime. Lua scripts may produce only bounded, declarative requests through a narrow host API.
4. Kotlin validates Lua output and converts approved, typed input into an optional C++ evaluation request through JNI.
5. C++ returns a bounded result object; Kotlin validates it again and decides whether an allowed effect may be applied.
6. C++ must not directly mutate saves/rewards or call arbitrary Kotlin methods. Avoid circular callbacks and duplicate sources of truth.

This provides a safe Lua -> Kotlin -> C++ path without introducing a second native Lua runtime. A future native Lua runtime should be considered only if a measured requirement justifies replacing LuaJ.

### Native C++ policy

- Do not add a collection of third-party libraries "just in case."
- First implement and benchmark a small native core using the C++ standard library, with clear JNI boundaries.
- Add Eigen, simdjson, or another runtime dependency only if real workload benchmarks demonstrate a benefit and ABI, size, maintenance, and license checks pass.
- Use Google Benchmark and Catch2 as development/test dependencies only if compatible with the repository's build setup; do not package them in the APK.
- Keep the native engine optional behind a Kotlin interface and provide a deterministic Kotlin fallback until native parity is established.
- Measure APK size, startup, evaluation latency, allocations, and battery impact on supported Android ABIs before making C++ mandatory.

## Trait definition contract

Each trait should have a stable, unique ID and fields equivalent to:
- schemaVersion
- id
- titleKey and descriptionKey
- category
- visibility (visible or hidden)
- source (builtin or liveops)
- definitionVersion
- minHostApiVersion / maxHostApiVersion as appropriate
- activationConditions
- dependencies and conflict declarations
- effects from a closed allowlist
- seasonId / validity metadata when seasonal
- deterministic test fixtures where applicable

Definitions must not contain arbitrary filesystem paths, native function names, unrestricted code execution, or direct save/reward mutations. IDs remain stable across title/localization changes. Duplicate IDs, dependency cycles, unsupported effects, incompatible versions, oversized payloads, and invalid time windows must be rejected.

## Offline-first LiveOps synchronization

Use a separate signed trait manifest and content namespace; do not silently change the existing Lua activity wire format.

1. Load built-in traits and the last validated local trait bundle immediately.
2. Schedule a constrained background manifest check using WorkManager; use appropriate connectivity and battery constraints and do not promise immediate execution.
3. Compare bundle version and per-file hashes; request only changed content where the hosting format supports it.
4. Require HTTPS, verify a signature over the exact manifest payload, validate hashes, size limits, schema and host compatibility, and reject rollback/replay.
5. Download into a staging directory, validate the entire candidate bundle, then atomically switch the active pointer.
6. Keep the previous verified bundle available for recovery; a failed/interrupted update must not damage the active bundle.
7. Continue to work with built-in traits when offline, the signing key is absent, or remote content is invalid.
8. Log bounded diagnostic events without logging private gameplay data or script contents.

The existing LiveOps implementation is a useful pattern to reuse, but trait synchronization must have its own typed models, limits, tests, and manifest version.

## Seasonal lifecycle

- Model seasons as versioned content with explicit start/end timestamps, time zone rules, eligibility, and fallback behavior.
- Define whether expiry disables only new activation or also hides seasonal UI; never invalidate saves or permanently remove previously earned permanent rewards.
- Make evaluation deterministic for a given game state and active content version.
- Handle device clock changes conservatively. Time-based eligibility is not a source of authority for economy/security decisions.
- Keep event UI/cosmetics separate from core gameplay rules.

## Developer inspector

Build a Compose screen only after the registry API stabilizes. It should support:
- Search by ID/title/category/source/visibility.
- Built-in vs downloaded, active vs inactive, valid vs rejected, and season status.
- Definition and dependency inspection.
- Last validation error and bounded execution trace.
- Evaluation duration and native/fallback path.
- Debug-only simulation and fault-injection actions.
- No arbitrary Lua source execution in production; keep debug commands typed and debug-gated.

## Delivery phases and acceptance criteria

### Phase 0 — Baseline and contract
- Record current Lua/LiveOps tests and app build status.
- Add schema examples and contract tests before runtime integration.
- Decide the trait effect allowlist and compatibility policy.

### Phase 1 — Kotlin registry, built-in sample, tests
- Add registry models, validation, dependency/conflict resolution, and deterministic evaluation.
- Ship a small sample pack first; do not generate the 300+ catalog yet.
- Verify built-in traits work in airplane mode and cannot alter protected state except through approved Kotlin effects.

### Phase 2 — Signed trait LiveOps
- Add a separate trait manifest/payload and background sync worker.
- Test unchanged content, changed hashes, invalid signatures, duplicate IDs, unsupported schemas, interrupted downloads, rollback, cache tampering, and offline launch.
- Verify current Lua Activity Hub bundles continue to work unchanged.

### Phase 3 — C++ evaluator
- Add JNI and CMake only after the Kotlin contract and benchmark fixture are stable.
- Run identical fixture inputs through Kotlin fallback and C++ and compare outputs.
- Add native unit tests, fuzz malformed inputs, and verify bounded memory/time behavior.
- Add third-party runtime libraries only with a documented measured need.

### Phase 4 — Seasons and inspector
- Add season activation/expiry, localized metadata, Compose inspector, and diagnostics.
- Test time-zone changes, clock changes, expired content, and save migration.

### Phase 5 — Catalog scale-up
- Expand to 300+ built-in and 150+ LiveOps traits after the pipeline passes reliability and performance gates.
- Prefer declarative data over unique executable scripts.
- Require CI validation and localization coverage for every catalog update.

## Required test matrix

- Offline fresh install and offline relaunch.
- Online initial download and unchanged refresh.
- Changed content, invalid signature/hash, malformed payload, duplicate IDs and dependency cycles.
- Interrupted download and atomic activation.
- Corrupt cache and fallback to the last verified/built-in catalog.
- Unsupported app/host API and schema versions.
- Lua instruction budget and payload size limits.
- Deterministic Kotlin/C++ parity.
- Existing save import/export and old saves.
- Current Lua activity behavior unchanged.
- Startup time, frame-time impact, memory, APK size, and battery measurements.

## Scope of the first implementation PR

The first implementation PR should contain the Kotlin trait schema/registry and tests, plus a small built-in sample. It should not introduce third-party C++ libraries, native Lua, a 450-trait catalog, or a full inspector before the contract and tests are proven. Follow-up PRs can add signed LiveOps synchronization and then the optional C++ evaluator.
