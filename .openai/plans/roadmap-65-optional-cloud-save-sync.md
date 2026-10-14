# Temporary plan: Roadmap #65 — Optional Google Play Games cloud saves

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Plan optional Google Play Games Services sign-in and versioned cloud save backup/restore only after platform setup. Preserve guest/offline play, validate payloads, sync at sensible checkpoints, expose status/retry, and show a localized conflict comparison requiring explicit choice before overwriting local or cloud progress. Do not claim functionality before real setup/device testing.

## Acceptance checklist
- [ ] Inspect current implementation and related PRs first; avoid duplicate work.
- [ ] Keep implementation focused and preserve offline-first behavior, saves, reward rules, and Kotlin/Lua trust boundaries where relevant.
- [ ] Localize new player-facing strings across all supported locales.
- [ ] Update the changelog in the implementation PR for shipped player-facing changes.
- [ ] Add focused tests and list device-only checks separately.
- [ ] Do not run builds, tests, emulators, device checks, or Actions without explicit authorization.
- [ ] Do not claim completion without implementation and validation evidence.

## Out of scope
No app code, UI, gameplay, settings, assets, tests, version bump, release notes, workflow runs, or feature implementation is included here. Reconcile this plan with existing PRs before implementation.
