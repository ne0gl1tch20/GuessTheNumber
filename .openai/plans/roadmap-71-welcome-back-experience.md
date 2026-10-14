# Temporary plan: Roadmap #71 — Welcome-back experience

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Plan a short, skippable recap of relevant progress, newly available events/challenges, and still-valid unclaimed rewards. Avoid guilt, loss threats, forced interruptions, repeated recaps, or rewards for merely opening the app. Preserve offline progress, localize content, and respect notification preferences.

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
