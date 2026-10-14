# Temporary plan: Roadmap #49 — Signature game-feel polish

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Give guessing, near-misses, correct answers, streaks, purchases, unlocks, and progression milestones a cohesive rhythm. Tune brief animation timing, transitions, emphasis, and feedback hierarchy while preserving game rules, rewards, input responsiveness, reduced-motion preferences, and performance. Use original visual language; do not copy Nintendo-owned assets, sounds, characters, or distinctive UI.

## Acceptance checklist
- [ ] Inspect the current implementation and existing roadmap/PRs before changing code; avoid duplicate or conflicting work.
- [ ] Split implementation into focused changes if the audit shows this scope is too broad.
- [ ] Preserve offline-first behavior, existing saves and reward rules, and the Kotlin authority / Lua sandbox boundaries where relevant.
- [ ] Localize every new player-facing string consistently across all supported locale catalogs.
- [ ] Update `app/src/main/assets/changelogs.md` in the same implementation PR for any shipped player-facing behavior, UI, or configuration change.
- [ ] Add focused tests for changed behavior and document device-only checks separately.
- [ ] Do not run builds, tests, emulators, device checks, or GitHub Actions unless explicitly authorized.
- [ ] Do not claim completion until implementation and validation evidence exist.

## Explicitly out of scope
- No production code, gameplay behavior, settings behavior, or assets are changed by this planning PR.
- No app version bump, release note, dependency, workflow run, or merge is included.
- This plan must be reconciled with related existing PRs before implementation.
