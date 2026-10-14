# Temporary plan: Roadmap #93 — Compose previews and accessibility: remaining follow-ups

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Plan only gaps remaining after the merged Compose preview/accessibility work in PR #40 and the progression-node semantics work in PR #43. Audit actual preview coverage for loading/empty/error/theme/localized states, useful hover guidance with touch/keyboard equivalents, TalkBack announcements, focus order, font scaling, contrast, touch targets, and reduced motion. Avoid duplicating existing implementations and do not claim visual/device coverage without evidence.

## Acceptance checklist
- [ ] Inspect current code, merged PRs, and open PRs before proposing edits.
- [ ] Record concrete findings and split implementation into focused PRs where needed.
- [ ] Preserve offline-first behavior, saves, reward rules, privacy, accessibility, and Kotlin/Lua trust boundaries.
- [ ] Localize all new player-facing strings across supported locales.
- [ ] Update the changelog in the implementation PR for shipped player-facing changes.
- [ ] Add focused tests and clearly separate device-only checks.
- [ ] Do not run builds, tests, emulators, device checks, or Actions without explicit authorization.
- [ ] Do not claim completion without implementation and validation evidence.

## Out of scope
This PR is documentation-only. No app code, UI, gameplay, settings, assets, tests, version bump, release notes, or feature implementation is included. Do not merge this plan as implementation.
