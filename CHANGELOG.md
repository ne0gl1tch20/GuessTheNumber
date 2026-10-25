# GuessTheNumber Changelog

This is the project-level release summary. For the exhaustive PR-by-PR record, see [docs/PR_CHANGELOG.md](docs/PR_CHANGELOG.md), which indexes the repository's 351 returned pull requests and marks merged versus open/closed-unmerged work.

## v3.1.0 — Creative Mode and seasonal systems

**Android version:** 3.1.0  
**versionCode:** 22  
**Status:** development release; device-level Creative Mode interaction and process-death recovery still need manual verification.

### Creative Mode
- Added an isolated Creative Mode run engine with bounded ranges, too-low/too-high/correct outcomes, attempt limits, bounded parity hints and completed-run protection.
- Added per-slot start/resume, guess submission, hint use, restart and close flows in Save Slots.
- Added schema v3 active-run snapshots with strict ID, range, target and counter validation.
- Added persist/resume/clear helpers. Portable encrypted backup bundles carry the Creative Mode configuration and active-run snapshot.
- Migrated schema v1/v2 Creative Mode data forward without inventing a running session.
- Kept Creative Mode runs separate from normal-game money, rewards, progression and scripting.

### Traits and seasonal systems
- Centralized deterministic seasonal windows, including winter across year boundaries, with inclusive start and exclusive end dates.
- Routed the existing trait-season lookup through the shared calendar policy.
- Kept seasonal activation offline-first and compatible with signed/validated trait catalogs.
- Expanded the trait/season lifecycle and cross-system isolation documentation. Creative Mode effects do not silently stack with trait rewards.

### Saves, security and release documentation
- Retained portable encrypted backups using passphrase-derived AES-GCM keys and authenticated format metadata.
- Kept schema migration and per-slot Creative Mode snapshots inside the encrypted backup payload.
- Added focused tests for runtime outcomes, attempt limits, hints, snapshot persistence/resume, schema migration and season boundaries.
- Restored this root changelog and the exhaustive PR ledger under docs/PR_CHANGELOG.md.

### Validation notes
- GitHub Actions results must be checked against the exact merged commit. CI does not replace on-device visual/interaction checks.
- The roadmap records the implementation status and outstanding device checks in [docs/TRAIT_SYSTEM_ROADMAP.md](docs/TRAIT_SYSTEM_ROADMAP.md).

## v3.0.3 — First-run tours and updater reliability

- Added per-feature first-run tours and saved tour completion.
- Fixed localized Nebula reward formatting.
- Improved release/debug APK discovery, package/signature validation and Android installer handoff.
- Android version: 3.0.3, versionCode 21.

## v3.0.2 and earlier — Development history

The full PR ledger preserves the original titles and statuses for the available repository history, including shared HTML/CSS/JavaScript UI work, the Jarrlyy UI SDK, Android/Windows parity, localization, save systems, gameplay/economy features, updater fixes, trait catalogs, native optimization, security and Creative Mode development.

See [the PR development ledger](docs/PR_CHANGELOG.md) for the individual PR links and status snapshot.
