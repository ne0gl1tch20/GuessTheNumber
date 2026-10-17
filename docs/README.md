# Project documentation

This directory contains technical notes for the Android game, KMP architecture and Windows feature parity. An audit or plan is not the same as a passing test or completed release gate.

## Start here

| Document | What it covers |
|---|---|
| [Game content and feature flags](GAME_CONTENT.md) | Bundled JSON schemas, feature flags, LiveOps fields, validation and save compatibility |
| [Integration audit status](INTEGRATION_AUDIT_STATUS.md) | Integrated features, known gaps and remaining validation/release work |
| [KMP foundation status](KMP_FOUNDATION_STATUS.md) | Shared-vs-host architecture boundary and foundation exit criteria |
| [Shared Web UI architecture](SHARED_WEB_UI_ARCHITECTURE.md) | Chosen HTML/CSS/JavaScript UI direction, Kotlin authority, native hosts and migration gates |
| [Shared Web UI migration matrix](SHARED_WEB_UI_MIGRATION_MATRIX.md) | Source-audited first production gameplay slice, Kotlin-owned action path, migration scope and acceptance evidence |
| [Shared Web UI gameplay bridge contract](SHARED_WEB_UI_GAMEPLAY_BRIDGE_CONTRACT.md) | Proposed versioned request/response contract and acceptance checks for the first real guessing interaction |
| [Windows WebView host evaluation](WINDOWS_WEBVIEW_HOST_EVALUATION.md) | Source-based Windows host options, MSI/runtime constraints and the next isolated proof of concept |
| [Windows feature parity plan](WINDOWS_FEATURE_PARITY_PLAN.md) | Ordered plan for the intended player-facing feature set on Windows |
| [Windows Android UI parity audit](WINDOWS_ANDROID_UI_PARITY_AUDIT.md) | Screen-by-screen implementation status and remaining visual/behavior gaps |
| [Windows Stage 2-D parity verification](WINDOWS_STAGE_2D_PARITY_VERIFICATION.md) | Candidate-specific CI/runtime evidence and the still-open Stage 2 gate |
| [Performance baseline](PERFORMANCE_BASELINE.md) | Repeatable performance measurement plan; device measurements remain pending unless recorded |
| [Seasonal events migration audit](SEASONAL_EVENTS_MIGRATION_AUDIT.md) | Boundaries for event-content migration and Kotlin-owned reward/state behavior |
| [LiveOps bundle format](LIVEOPS_BUNDLE_FORMAT.md) | Lua activity bundle format, signing, validation and publishing workflow |

## Related documents

- [Root README](../README.md): overview, setup, CI expectations and KMP boundaries.
- [Android runtime QA checklist](../.openai/kmp_runtime_parity_checklist.md): post-foundation runtime checks; unrun cases remain unverified.
- [Save schema and migration](SAVE_SCHEMA_AND_MIGRATION.md): save format, defaults, recovery and compatibility expectations.

## Validation language

- **Implemented** means source changes exist.
- **Tested** means the named test or workflow actually ran and passed for the referenced revision.
- **Pending / not measured** means no passing result or measurement is claimed.
- **KMP foundation complete** means the agreed shared architecture and selected UI scope is in place; it does not mean Windows has full gameplay parity.
- **Windows parity complete** means the agreed player-facing features are implemented and verified on Windows, or explicitly approved as platform-specific/not included.

Keep plans separate from completed work and tie claims to evidence.
