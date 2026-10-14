# Temporary plan: Roadmap #59 — Return-friendly goals, streaks, and widgets

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Plan fair daily/session goals, comeback mechanics that do not punish absence, and useful user-selected home-screen widgets. Prevent duplicate claims and date/timezone exploits; expose only user-approved non-sensitive data; refresh efficiently and work with stale/offline data. Avoid guilt, artificial scarcity, or requiring notifications.

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
