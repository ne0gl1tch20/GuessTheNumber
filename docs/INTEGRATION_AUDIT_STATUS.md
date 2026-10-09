# Beyond the Numbers integration status

## Completed in this pass

- Expanded the offline music library/player backend with persistent local library, metadata, queue, shuffle, repeat, playlists, playlist membership editing, queue reordering and missing-file cleanup.
- Added a reusable full-screen `MusicPlayerScreen` component using the shared offline music manager.
- Routed the reusable player through `NavGraph` and the More Hub.
- Unified `BackgroundMusicManager` behind an application-scoped shared instance so `GameViewModel`, navigation and the player use one playback owner.
- Added an Android foreground media playback service with `MediaSessionCompat`, lock-screen/headset transport callbacks and a persistent playback notification.
- Added the required media playback foreground-service permissions and AndroidX media dependency.
- Kept the shared music manager alive across screen navigation; explicit stop controls the playback/service lifecycle.
- Extended save-compatible `GameSettings` with music library/queue/playlist preference fields for future migration into the main save/preferences layer.

## Completed in the latest notification/widget pass

- Added independent reminder, progression-milestone and game-event switches in Settings. Notification dispatch checks both the global switch and the matching category switch.
- Connected progression notifications to new achievements, world unlocks, boss victories, secret discoveries, Codex claims, Home Base milestones and Endless Rift milestones.
- Connected event notifications to actual random-event engine rolls, rather than state comparisons that can miss event timing.
- Kept reminder, progression and event notifications on separate channels and preserved destination extras for navigation.
- Reminder scheduling and reboot restoration honor the reminder category switch and the selected 12/24/48-hour interval.
- Expanded the home-screen widget with money, range, streak, mastery, world and boss metrics; added per-widget save-slot cycling and an Open Game action.
- Added a compact widget layout alongside the expanded layout. Both widget layouts refresh after persisted gameplay saves through a throttled refresh coordinator.
- Removed the widget's duplicated gameplay engine/reward implementation. Widget actions no longer grant a free correct guess or duplicate boss/relic reward logic.

## Completed in the latest RPG integration pass

- Expanded relic equipment capacity to four boss relics and added explicit two-, three- and four-relic set bonuses.
- Full four-relic set adds a stronger currency bonus and extra Nebula generation; discovering all four secret areas adds another Nebula bonus on correct guesses.
- Secret discoveries now grant one-time money/Nebula rewards, Codex entries, a world-specific mastery discount and an ongoing reward bonus in that world.
- Mastery level 5 and 10 milestones grant Nebula and Codex entries; the secret-area discount reduces mastery upgrade costs.
- Home Base upgrades now account for money spent, grant milestone Nebula at levels 5/10/15/20 and add Sanctuary Codex entries.
- Boss and Endless Rift rewards now receive connected bonuses from relic set size, world mastery, Home Base and discovered secret areas.
- Mastery level 5 and 10 milestones add Codex entries and Nebula rewards. The endgame now includes a claimable goal for reaching mastery level 5 in every world.
- Endgame goals expose reward-claim buttons when their requirements are met, and the Codex completion reward requires all 12 core world/boss/secret entries instead of being affected by bonus Codex entries.

## Still requires the remaining integration pass

- Run a localization audit to catch missing keys, placeholder mismatches and untranslated legacy UI strings.
- Review RPG reward balance, set-specific effects, boss/Rift reward consistency and whether Codex completion thresholds should include the new mastery/Sanctuary entries.
- Run the manual GitHub Actions validation/build and resolve compiler/lint/runtime issues before calling the pass build-verified.

Current app version: 3.0.1 (versionCode 19).
