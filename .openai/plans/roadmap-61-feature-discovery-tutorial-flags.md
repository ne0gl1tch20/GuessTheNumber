# Temporary plan: Roadmap #61 — Interactive feature discovery and saved tutorial flags

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Plan friendly localized interactive tutorial steps with Skip, Next, and Done, plus first-use More Hub discovery excluding Settings/About. Persist stable versioned completion flags in global preferences, support replay where appropriate, and ensure back navigation, restart, language switching, and returning-player paths never trap the player.

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
