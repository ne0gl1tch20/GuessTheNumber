# Temporary plan: Roadmap #63 — Expanded Settings and customization hub: remaining scope

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Plan only the remaining Settings customization work after the existing localized descriptions (#45), section organization (#46), and live theme preview (#47). First audit those changes and current controls. Identify only real missing supported options, global-versus-per-save scope clarity, useful per-section reset behavior, persistence/migration gaps, and accessibility/localization gaps. Do not duplicate or rewrite the work already covered by #45–#47; never add fake controls or imply unsupported capabilities.

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
