# Temporary plan: Roadmap #66 — Developer Settings test dashboard

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Organize Debug-only test controls by subsystem, separate read-only diagnostics from state-changing tests, show real pass/fail/skip outcomes, prevent duplicate taps, and confirm destructive actions. Reuse actual test hooks, keep long stress tests bounded and opt-in, localize supported labels, and ensure developer entry points/actions are absent from Release.

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
