# Temporary plan: Roadmap #62 — Material 3 Expressive motion and loading system

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Establish coherent shapes, tonal color, typography, component states, and reusable Compose motion/loading patterns. Use brief interactions and real progress states; never fake progress or leave unexplained endless spinners. Respect reduced motion, TalkBack, font scaling, cancellation, rapid taps, and lower-end device performance.

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
