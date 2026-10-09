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

## Still requires the remaining integration pass

- Finish notification category settings/schedules and widget live-refresh wiring.
- Finish secret-area unique reward/action chains and deeper mastery/relic/Home Base/endgame cross-system effects.
- Run the manual GitHub Actions validation/build and resolve compiler/lint/localization failures before calling the pass build-verified.

Current app version: 3.0.1 (versionCode 19).
