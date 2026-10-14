# Feature Integration Plan

**Last reviewed:** 2026-10-14  
**Source of truth:** `master`  
**Rule:** Preserve every existing branch. Integrate focused changes through new branches and pull requests; do not merge an old cumulative branch wholesale just because its name sounds relevant.

## 1. Stranded branch inventory

The branch comparisons below compare each branch with current `master`. The counts are comparison metadata, not a claim that every ahead commit is still unique: some features were already merged by other PRs, so review the actual diff before porting anything.

| Branch | Compared with master | Observed file overlap | Integration decision |
|---|---:|---|---|
| `feat/joy-shop-purchase-feedback` | 5 ahead, 4 behind | ShopScreen, UpgradeScreen, changelog | Port only the purchase feedback still missing on master. Do not merge the old branch wholesale. |
| `feat/joy-upgrade-purchase-feedback` | 7 ahead, 4 behind | AchievementsScreen, ShopScreen, UpgradeScreen, changelog | Check its unique upgrade/achievement changes against the focused feedback PR. Avoid duplicate changes. |
| `feat/joy-achievement-unlock-queue` | 9 ahead, 4 behind | AchievementsScreen, ShopScreen, UpgradeScreen, PlayScreen, changelog | Port the queued achievement announcement behavior as a small, current-master change. |
| `feat/joy-early-goal-celebrations` | 11 ahead, 4 behind | PlayScreen and the shared feedback files, changelog | Review separately after achievement/purchase work; preserve the queue and avoid replaying old edits. |
| `feat/joy-random-event-announcements` | 17 ahead, 4 behind | PlayScreen, milestone banner, achievement/shop/upgrade screens, changelog | Integrate only after a source review of event announcement logic and interactions with other celebrations. |
| `feat/joy-high-low-feedback` | 3 ahead, 4 behind | ShopScreen and changelog in the comparison | High/low gameplay feedback already landed through PR #73. Inspect any remaining shop-only delta separately; do not re-import the merged feedback. |
| `docs/multiplayer-release-gate` | 19 ahead, 4 behind | Shared gameplay files and changelog | Treat as a mixed cumulative branch, not a multiplayer implementation. Extract its genuinely unique feedback work; keep multiplayer planning separate. |
| `docs/multiplayer-diagnostics-checklist` | Not yet compared | Documentation | Review its document against the current multiplayer plan before incorporating it. |

**Preservation:** Do not delete these branches during integration. Keep them as recovery/reference points until each unique change has either landed through a reviewed PR or been explicitly marked superseded.

## 2. Current-master feature audit (scoped, not a whole-repository certification)

Status meanings:
- **Implemented:** the relevant behavior is visible in current-master source or its current-master changelog.
- **Partial:** a related capability exists, but the full roadmap scope is not present or verified.
- **Planned:** no implementation was found in the inspected current-master paths.
- **Needs verification:** source evidence alone is not enough to claim the behavior works on a device.

| Area | Status on current master | Evidence / remaining work |
|---|---|---|
| High/low guess feedback | Implemented | PR #73 and the Unreleased Player Experience section in [changelogs.md](../../app/src/main/assets/changelogs.md) document distinct feedback cards/icons and reduced-motion handling. Device/theme checks remain unverified. |
| Shop purchase feedback | Partial | [ShopScreen.kt](../../app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ShopScreen.kt) shows owned/equipped states, but a clear transient success highlight was absent in the inspected master source. PR #84 proposes this. |
| Upgrade purchase feedback | Partial | [UpgradeScreen.kt](../../app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/UpgradeScreen.kt) animates levels, but a success highlight was absent in the inspected master source. PR #84 proposes this. |
| Achievement celebrations | Partial | [AchievementsScreen.kt](../../app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/AchievementsScreen.kt) already displays a celebration banner, but its master implementation handles a batch as one announcement. PR #84 proposes a queue. |
| Haptics | Partial | [GameState.kt](../../app/src/main/java/com/jarrlyyy/guessthenumber/domain/model/GameState.kt) has a `vibrationEnabled` preference and [SettingsScreen.kt](../../app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt) exposes it. A complete app-wide haptic audit is still needed; PR #84 proposes feedback for purchases and achievement unlocks only. |
| First-run guided tutorial | Partial | Current project memory records an in-app guided overlay that requires real actions and supports English/Filipino. Verify current source and fresh-install/replay behavior before marking the broader onboarding roadmap complete. |
| First-visit popups for More Hub features | Planned / needs source verification | Roadmap item #61 asks for per-feature discovery popups, persisted global flags, skip/next/done and Settings/About exclusions. Do not treat the existing first-run tutorial as proof that this separate feature-discovery flow exists. |
| Player-selected background music | Implemented in part | Current-master source includes [BackgroundMusicManager.kt](../../app/src/main/java/com/jarrlyyy/guessthenumber/data/audio/BackgroundMusicManager.kt) and [MusicPlayerScreen.kt](../../app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/MusicPlayerScreen.kt). Full lifecycle/device behavior still needs verification. |
| Dedicated SFX mixer and resilient missing-clip lookup | Planned | Roadmap item #92 specifies a separate SFX volume path, Debug diagnostics, and a non-crashing not-found result. Code search did not locate a dedicated SFX mixer/API. No sound assets are required for the first implementation. |
| Local Wi-Fi/Bluetooth multiplayer | Planned | No multiplayer game implementation was established by this source audit. Start with a protocol/session design and opt-in local peer discovery; do not treat the branch named `docs/multiplayer-release-gate` as working multiplayer. |
| Build and runtime validation | Needs verification | This review used source and branch metadata only. No builds, unit tests, emulator/device tests, or Actions were run. |

## 3. Safe integration order

1. **Purchase and achievement polish:** review PR #84 first. It is based on current master and intentionally avoids merging stale branches.
2. **Early-goal and random-event announcements:** compare each branch's unique logic against current PlayScreen and the new queue behavior. Port one behavior per PR, with deduplication and reduced-motion handling.
3. **SFX mixer:** add an independent SFX volume preference and a resilient lookup/player API. Missing `assets/sfx/` or a missing clip must be a normal not-found result, never a crash. Do not add audio assets yet and do not couple SFX volume to music volume.
4. **Tutorial discovery popups:** build on the existing guided tutorial and global preferences. Use stable feature IDs, localized text, Skip/Next/Done, replay support, and ensure players can always leave.
5. **Local multiplayer:** first implement a transport-independent session protocol and state machine; then prototype one local transport. Keep core single-player offline, require explicit user initiation, handle disconnect/rejoin, validate every message, and never trust a peer's claimed rewards or save state.
6. **Reconcile roadmap statuses:** mark a feature complete only after its code lands on master and required checks are reviewed. Device-only claims stay “needs verification” until actually tested.

## 4. Rules for every follow-up PR

- Start from current `master`; do not use a diverged feature branch as the base unless the PR is intentionally a branch-recovery operation.
- Keep each PR narrow and describe which stranded work it supersedes or preserves.
- Reuse existing preferences and locale keys where they match actual behavior; add localized keys across all supported locales when new user-facing copy is necessary.
- Preserve save compatibility, economy/reward rules, offline-first play, privacy defaults, and Debug/Release gating.
- Do not start builds, emulator/device tests, or Actions automatically. Record unrun checks as unrun, not passed.
