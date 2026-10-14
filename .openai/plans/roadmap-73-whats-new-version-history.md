# Temporary plan: Roadmap #73 — What's New and version history

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Plan a localized What's New view for meaningful changes after an update, with a persistent version history in About. Show at most once per installed version, allow dismissal, never block gameplay, validate/version notes, and align with the existing build-channel-aware updater.

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
