# Project documentation

This directory contains focused technical notes for the Android game and its incremental Kotlin Multiplatform migration. Status labels in individual documents matter: an audit, source review, or planned step is not the same as a passing test or completed release gate.

## Start here

| Document | What it covers |
|---|---|
| [Game content and feature flags](GAME_CONTENT.md) | Bundled JSON schemas, feature flags, LiveOps event fields, validation and save-data compatibility expectations |
| [Integration audit status](INTEGRATION_AUDIT_STATUS.md) | Recent integrated features, known gaps, and remaining validation/release work |
| [Performance baseline](PERFORMANCE_BASELINE.md) | Repeatable performance measurement plan; device measurements remain pending unless results are explicitly recorded |
| [Seasonal events migration audit](SEASONAL_EVENTS_MIGRATION_AUDIT.md) | Boundaries for event content migration and Kotlin-owned reward/state behavior |
| [LiveOps bundle format](LIVEOPS_BUNDLE_FORMAT.md) | Lua activity bundle format, signing, validation and publishing workflow |

## Related documents outside this directory

- [Root README](../README.md): project overview, setup, CI expectations and current KMP boundaries.
- [KMP migration roadmap](../.openai/multiplatformmigration_roadmap.md): ordered migration tasks and evidence-based checkpoints.
- [LiveOps template](../liveops-template/README.md): copy-ready scaffold for the separate content repository.

## Validation language

- **Implemented** means source changes exist.
- **Tested** means the named test or workflow actually ran and passed for the referenced revision.
- **Pending / not measured** means no passing result or measurement is claimed.
- **Migration in progress** means shared rules exist, not that every Android feature has desktop parity.

When updating these documents, keep claims tied to merged code and actual validation results. Keep planned work separate from completed work.
