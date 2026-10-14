# Temporary plan: Roadmap #92 — Debug runtime test bench and SFX mixer

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Plan a Debug-only in-app test bench with bounded deterministic checks and real per-check pass/fail/skip results, isolated from player progression. Add an SFX mixer independent of the music mixer and resilient SFX lookup that reports missing folders/clips without crashing or blocking gameplay. Do not add sound assets in this phase.

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
