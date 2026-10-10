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
- Added a compact widget layout alongside the expanded layout. Both widget layouts refresh after persisted gameplay saves through a debounced refresh coordinator that publishes the latest saved state.
- Removed the widget's duplicated gameplay engine/reward implementation. Widget actions no longer grant a free correct guess or duplicate boss/relic reward logic.

## Completed in the latest RPG integration pass

- Expanded relic equipment capacity to four boss relics and added explicit two-, three- and four-relic set bonuses. Updated the Relic Collection UI and all 23 locale catalogs to describe the new capacity and bonus tiers.
- Full four-relic set adds a stronger currency bonus and extra Nebula generation; discovering all four secret areas adds another Nebula bonus on correct guesses.
- Secret discoveries now grant one-time money/Nebula rewards, Codex entries, a world-specific mastery discount and an ongoing reward bonus in that world.
- Mastery level 5 and 10 milestones grant Nebula and Codex entries; the secret-area discount reduces mastery upgrade costs.
- Home Base upgrades now account for money spent, grant milestone Nebula at levels 5/10/15/20 and add Sanctuary Codex entries.
- Boss and Endless Rift rewards now receive connected bonuses from relic set size, world mastery, Home Base and discovered secret areas.
- Mastery upgrades now discount costs from the matching discovered secret, Home Base level and the complete four-relic set, capped at a 40% combined discount.
- Mastery level 5 and 10 milestones add Codex entries and Nebula rewards. The endgame now includes a claimable goal for reaching mastery level 5 in every world.
- Endgame goals expose reward-claim buttons when their requirements are met, and the Codex completion reward requires all 12 core world/boss/secret entries instead of being affected by bonus Codex entries.

## Completed in the follow-up audit pass
- Live Ops is now offline-only: the event manifest is read from the bundled asset instead of attempting a remote GitHub fetch. Event availability automatically follows the bundled start/end dates, so scheduled events can activate and expire without a network refresh.
- Live Ops claims now validate event status/date and per-event claim limits, grant the displayed 10 Nebula, increment the persisted claim count and permanent boost level, and update lifetime Nebula earnings. The boost level now contributes +2% correct-guess money per level (capped at 25 levels) and +1 Nebula per correct guess for every 5 levels.
- Arcade reward payouts now update lifetime money/Nebula earned statistics too.

- Completed the missing daily-bounty labels across the remaining locale catalogs. A key/placeholder audit now reports 824 matching English keys in every locale catalog; Arabic also retains its existing `rtl` metadata key. No missing keys or `%s/%d/%f/%i` placeholder mismatches were found in the audited catalogs.
- Daily bounty claims now update `statistics.moneyEarned` or `statistics.nebulaEarned` alongside the actual payout, keeping lifetime earned totals aligned with rewards.


- Notification taps now reach the requested in-app destination on both cold launch and when the app is already running; the destination request is cleared after navigation so later taps are handled too.
- Media playback notification controls now use the active locale catalog instead of hardcoded English labels. Added translated Previous/Pause/Play/Next/Stop labels to all 23 locale catalogs.
- Sticky media-service restarts attempt to resume the last saved local track when Android recreates the service after process death.
- Repeated category notifications reuse their existing notification IDs without repeatedly alerting for the same visible notification.
- Aligned endgame goal display checks for bosses, secrets and relics with the stricter reward-claim checks, avoiding goals that appear complete but cannot be claimed.
- Correct-guess Nebula bonuses from relics, secret discoveries and talents are now evaluated even if that guess produced no positive money payout.

## Stability and save-integrity fixes (2026-10-10)

- Backup recovery now writes a validated backup back to the primary save key, so a recoverable corrupted/missing primary save does not repeatedly fall back on every launch. The validated backup is retained until a later successful save replaces it.
- Single-save imports now return the actual `saveGame` result. A storage write failure is no longer reported to the caller as a successful import.
- Fresh-slot creation now checks slot occupancy and writes the new save inside one DataStore edit transaction, preventing concurrent create requests from overwriting a slot that another request just populated.
- These changes were reviewed against the existing `SaveManager` implementation and preserve the current JSON/save schema. No build or GitHub Actions workflow was run, as requested.

## Still requires the remaining integration pass

- All 23 locale catalogs now have matching key coverage and no audited string-format placeholder mismatches. A separate manual review of older hardcoded/dynamically assembled UI text is still needed.
- Continue the numerical RPG balance review across stacked relic, mastery, Home Base, secret and permanent-event bonuses; current formulas are connected, but have not been play-tested for late-game pacing.
- Run the manual GitHub Actions validation/build and resolve compiler/lint/runtime issues before calling the pass build-verified.

Current app version: 3.0.1 (versionCode 19).
