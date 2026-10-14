# Temporary plan: Roadmap #94 — App-wide hardening: remaining audit follow-ups

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Plan a structured, evidence-based audit of remaining screen, settings, feature, persistence, security, and engine-boundary gaps after the merged reward-arithmetic work (#41) and corrupted LiveOps claim-state fix (#44). Keep fixes focused; validate navigation/state restoration, loading/error/retry paths, lifecycle cancellation, input validation, migration, offline behavior, permissions/privacy, update handling, Debug/Release gating, and Kotlin/Lua/C++ boundaries where present. Do not duplicate fixes already merged or turn this into a risky rewrite.

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
