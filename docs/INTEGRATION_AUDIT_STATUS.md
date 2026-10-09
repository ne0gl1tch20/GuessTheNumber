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

- Added localized reminder/progression/event notification titles and bodies to all 23 supported locale catalogs; notifications load the saved global app language.
- Restored reminder scheduling after device reboot from saved notification preferences, including the selected 12/24/48-hour interval and disabled state.
- Kept reminder, progression and event notifications on separate channels with destination extras for navigation.
- Routed supported notification destinations directly from MainActivity into the matching NavGraph start destination.
- Changed the home-screen widget to render metrics from the latest saved game state instead of stale Glance preference snapshots.
- Added a throttled widget refresh after game saves to avoid hammering widget updates during rapid gameplay.
- Updated reminder scheduling to honor the selected 12/24/48-hour interval and reschedule when the interval changes.

## Still requires the remaining integration pass

- Add explicit per-category notification switches and wire real progression/event triggers to the corresponding channels.
- Expand widget layouts with more selectable metrics, quick actions and selected-save-slot support.
- Finish secret-area unique reward/action chains and deeper mastery/relic/Home Base/endgame cross-system effects.
- Run the manual GitHub Actions validation/build and resolve compiler/lint/localization failures before calling the pass build-verified.

Current app version: 3.0.1 (versionCode 19).
