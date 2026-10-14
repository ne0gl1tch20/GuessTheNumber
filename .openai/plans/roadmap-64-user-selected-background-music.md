# Temporary plan: Roadmap #64 — Player-selected background music lifecycle

**Status:** Planning only. This temporary Draft PR does not implement the feature.

## Scope
Let players select downloaded audio through the system picker. Persist selection/access correctly and play only after a track is selected and playback is enabled. Plan choose/replace/remove controls, volume, audio focus, lifecycle and route handling, and graceful recovery for deleted files, revoked URI access, unsupported formats, and interruptions. Keep music mixer work separate from SFX.

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
