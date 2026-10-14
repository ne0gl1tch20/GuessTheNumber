# Cross-Core Hardening Audit

Run the read-only baseline from the repository root:

```sh
python3 tools/core_hardening_audit.py
```

The script is deliberately manual-only. It does not build the Android app, run tests, launch emulators, access the network, or change any player data.

## What the baseline checks

- **Localization:** JSON syntax, duplicate keys, missing fallback keys, settings-description presence, and printf-style placeholder parity against `en_us.json`.
- **Save integrity:** confirms the existing save-validation test and manager are present. This is inventory, not proof that interrupted writes or every migration path is correct.
- **Economy/rewards:** confirms the engine test surface exists. Reward idempotency, overflow behavior, and every claim path still require focused behavioral tests.
- **Startup/lifecycle:** inventories the ViewModel entry point. Cold/warm startup, widget/notification routing, cancellation, media recreation, and stuck-loading behavior require runtime or targeted lifecycle tests.
- **Lua boundary:** inventories bundled scripts. Static inspection cannot prove instruction budgets, cancellation, or sandbox isolation.
- **Audio:** reports whether optional SFX assets exist. An absent `assets/sfx/` folder is allowed; a missing clip must never crash or block gameplay. Music and SFX volume must remain independent.
- **Performance:** this script intentionally does not claim to measure startup, frame time, memory, or battery. Record repeatable device/profiler baselines before making optimization claims.
- **Accessibility/settings:** verifies that the main settings and localization surfaces exist. TalkBack, font scaling, touch targets, keyboard navigation, contrast, reduced motion, and live setting effects still need focused UI/device review.

## How to interpret results

- **ERROR** means a deterministic static contract failed, such as malformed JSON, duplicate keys, or a placeholder mismatch.
- **WARN** means a review item was found. Missing locale keys can be valid fallback behavior, so a warning is not automatically a user-visible defect.
- **INFO** records optional or not-yet-measured surfaces without treating them as failures.

This is a baseline guardrail, not a replacement for Kotlin unit tests, stress tests, or device verification. Do not mark any runtime area as verified just because this script passes. Keep all verification manual until a separate change explicitly authorizes a workflow or device run.
