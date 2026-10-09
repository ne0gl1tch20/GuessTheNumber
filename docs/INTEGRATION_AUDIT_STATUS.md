# Beyond the Numbers integration status

## Completed in this pass

- Expanded the offline music library/player backend with persistent local library, metadata, queue, shuffle, repeat, playlists, playlist membership editing, queue reordering and missing-file cleanup.
- Added a reusable full-screen `MusicPlayerScreen` component using the shared offline music manager.
- Extended save-compatible `GameSettings` with music library/queue/playlist preference fields for future migration into the main save/preferences layer.

## Still requires the main navigation/view-model integration pass

- Replace the legacy More Hub music popup with the reusable `MusicPlayerScreen` through `NavGraph`.
- Unify the reusable music manager with the existing `GameViewModel` background music instance so there is one playback owner across the app.
- Add Android media-session/background-service controls for playback outside the foreground activity.
- Finish notification category settings/schedules and widget live-refresh wiring.
- Finish secret-area unique reward/action chains and deeper mastery/relic/Home Base/endgame cross-system effects.
- Run the manual GitHub Actions validation/build and resolve compiler/lint/localization failures before calling the pass build-verified.

Current app version: 3.0.1 (versionCode 19).
