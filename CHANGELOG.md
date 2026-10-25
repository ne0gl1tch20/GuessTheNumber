# GuessTheNumber Changelog

This is the project-level release summary. For the exhaustive PR-by-PR record, including closed-unmerged attempts, see [docs/PR_CHANGELOG.md](docs/PR_CHANGELOG.md). A PR's presence in that ledger does not mean it shipped; status is recorded per PR.

## v3.2.0 — Creative Mode Phase 8–10 hardening

**Android version:** 3.2.0  
**versionCode:** 23  
**Status:** development build. Automated checks are required for merge; physical-device interaction and process-death recovery remain manual QA items.

### Creative Mode gameplay and recovery
- Added an isolated Creative Mode run engine with bounded ranges, too-low/too-high/correct outcomes, attempt limits, bounded hints and completed-run protection.
- Added per-slot start/resume, guess submission, hint use, restart and close flows in Save Slots.
- Added schema v3 active-run snapshots and persist/resume/clear helpers; schema v1/v2 Creative Mode data migrates without inventing a running session.
- Portable encrypted backups carry Creative Mode configuration and active-run snapshots.
- Creative Mode has no normal-game currency, reward, progression or arbitrary-script authority.
- Hardened snapshot validation so a completed run must contain an actual attempt and correct guess; added tests for composed range scaling, additive hints and strictest attempt-limit selection.

### Trait and seasonal architecture
- Centralized deterministic seasonal windows with inclusive starts, exclusive ends and winter across year boundaries.
- Routed trait season lookup through the shared policy and kept signed catalog validation, offline fallback and cross-system isolation.
- Trait effects and Creative Mode rules remain separate rather than stacking implicitly.

### Save security, UI and development history
- Portable encrypted exports use passphrase-derived authenticated encryption; the app does not store the passphrase. Losing it may make a backup unrecoverable.
- Shared HTML/CSS/JavaScript UI and the Jarrlyy UI SDK received extensive migration, accessibility, navigation, font, icon and popup-reliability work.
- Web-play asset auto-sync, update-channel handling, developer diagnostics, offline rewards and bridge-safe arcade claims were expanded.
- Added a PR-by-PR ledger covering PRs #1–#356, including open and closed-unmerged work, to preserve the project history instead of treating only the latest feature as the whole release.
- Bumped the app to 3.2.0 / versionCode 23 and synchronized the web-play compatibility gate.

### Validation boundaries
- Use the checks attached to the exact PR head/merge commit for Android tests, Windows build/tests, HTML/CSS/JS, native/security validation and trait profiling.
- CI does not replace physical-device verification of Save Slots interaction, app restart/process-death recovery or visual layout.

## v3.1.0 — Creative Mode foundation and seasonal policy

- Added the first isolated Creative Mode runtime, schema v3 snapshots, save-slot persistence and seasonal calendar policy.
- Added portable encrypted backups, compatibility tests and the initial exhaustive PR ledger.
- Android version: 3.1.0, versionCode 22.

## v3.0.3 — First-run tours and updater reliability

- Added per-feature first-run tours and saved tour completion.
- Fixed localized reward formatting and improved debug/stable APK discovery, package/signature checks and Android installer handoff.
- Android version: 3.0.3, versionCode 21.

## v3.0.2 and earlier — Development history

The [PR development ledger](docs/PR_CHANGELOG.md) preserves individual PR titles and status snapshots through PR #356 across core gameplay, progression/economy, localization, save systems, Android/Windows parity, shared HTML/CSS/JavaScript UI, Jarrlyy UI SDK, updater reliability, trait catalogs, native optimization, security and Creative Mode work.
