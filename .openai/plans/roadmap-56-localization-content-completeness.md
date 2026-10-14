# Temporary plan: Roadmap #56 — Localization and content completeness

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Audit all supported locales across UI, dialogs, Settings, About/update UI, tutorials, Lua activities/events, accessibility labels, plurals, and formatting. Detect missing keys, leaked placeholders, hardcoded player-facing text, clipping, and incorrect fallback behavior. Treat key parity as necessary but not proof of translation quality.

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
