# Temporary plan: Roadmap #58 — Privacy, security, and player trust

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Audit permissions, storage, exported components, update/LiveOps traffic, notifications, widgets, music, logs, and Kotlin/Lua/C++ boundaries. Preserve offline local play and no analytics/identifiers by default. Explain local versus transmitted data in localized plain language; make optional network-facing features genuinely optional and validate untrusted input.

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
