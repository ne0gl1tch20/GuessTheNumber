# Temporary plan: Roadmap #57 — Release-candidate readiness gate

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Define a repeatable candidate-specific checklist for Debug/Release builds, version/signing, offline play, save compatibility, localization, privacy/permissions, update-channel separation, Lua boundaries, native fallback where applicable, core-flow stability, and tiered test evidence. Report pass/fail/skip honestly and list release-blocking known issues.

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
