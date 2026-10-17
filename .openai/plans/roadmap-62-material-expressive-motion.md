# Temporary plan: Roadmap #62 — Material 3 Expressive motion and loading system

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Establish coherent shapes, tonal color, typography, component states, and loading/motion behavior across the app. For the chosen shared player-facing HTML/CSS/JavaScript UI, define shared CSS tokens and accessible web motion/loading patterns. Keep reusable Compose motion patterns for retained native/system surfaces and host UI rather than rebuilding shared game screens in Compose. Use brief interactions and real progress states; never fake progress or leave unexplained endless spinners. Respect reduced motion, screen readers/TalkBack, font scaling, cancellation, rapid taps, keyboard focus, and lower-end device performance. This plan remains unimplemented; it does not claim the web UI or its motion system exists.

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
