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

## Music restoration fixes (2026-10-10)

- Startup restoration now filters persisted queue entries and playlist memberships against the currently available imported library, so stale IDs from removed or unavailable audio files are not exposed as playable items.
- A MediaPlayer prepare/start failure now releases the failed player and clears stale current-track, duration, progress and album-art state, including the persisted current-track pointer. This prevents the screen/media notification from continuing to represent a failed track as active.
- The music changes were inspected at source level only; device playback, service recreation and headset/lock-screen controls still require runtime validation.

## Follow-up integration fixes (2026-10-10)

- Save-slot switching now cancels the switch when the current slot cannot be persisted, rather than changing the active slot and risking loss of unsaved progress.
- Save initialization now catches non-cancellation failures and clears the loading indicator instead of leaving the app stuck in a loading state. Coroutine cancellation is rethrown normally.
- Profile edits update the in-memory active state only after the updated save has been persisted successfully.
- Creating or switching save slots requests a widget refresh so slot-aware widgets can reflect the selected state.
- Autosave and asynchronous save paths refresh widgets only after successful persistence and log failed writes.
- Queue removal now adjusts the queue cursor when an earlier item is removed; removing the selected item no longer makes the next normal track skip the item that takes its place. An empty queue stops playback.
- Confirmed `.github/workflows/build-apks.yml` uses `workflow_dispatch` only. No workflow was started and no build/test result is claimed.

- Save-slot creation now refuses to overwrite malformed-primary or backup-only slots, protecting potentially recoverable progress. Slot metadata reads a validated backup when the primary is invalid so the UI can still display the recoverable save instead of presenting it as empty.

## Still requires the remaining integration pass

- All 23 locale catalogs now have matching key coverage and no audited string-format placeholder mismatches. A separate manual review of older hardcoded/dynamically assembled UI text is still needed.
- Continue the numerical RPG balance review across stacked relic, mastery, Home Base, secret and permanent-event bonuses; current formulas are connected, but have not been play-tested for late-game pacing.
- Run the manual GitHub Actions validation/build and resolve compiler/lint/runtime issues before calling the pass build-verified.

Current app version: 3.0.1 (versionCode 19).


## Additional integration pass (2026-10-10)

- Live Ops reward labels now use locale catalog keys with format arguments for currency and claim counts, rather than constructing dynamic English strings that the exact-string translation lookup could never match. The permanent bonus text, claim buttons, and Back accessibility label also use existing localized catalog entries.
- Both home-screen widgets now advance to the next populated save slot instead of stepping through every empty slot. If no valid saves exist, the current selection is retained rather than cycling into an arbitrary blank slot.
- These are source-level changes only. No build, emulator run, or GitHub Actions workflow was started, so compilation and device behavior remain unverified.

- Widget Play buttons now open the slot currently shown by that widget. MainActivity defers the requested switch until initial save loading has completed and crash recovery has been dismissed, avoiding a race with startup initialization; loading state is raised before the asynchronous load begins.
- Boss and Endless Rift payout bonuses now include the permanent Live Ops money boost, and Home Base uses the same 2%-per-level rate (capped at 40%) across correct-guess and progression reward paths. This closes a gap where a save-slot permanent bonus and Home Base level affected guess payouts but not progression payouts consistently.
- Widget refreshes now also follow slot duplication, backup restoration, slot resets, full-data resets and successful save-JSON edits. Applying JSON to the active slot reloads the validated state into gameplay immediately, preventing the editor from leaving the UI on stale state that could later overwrite the edit.
- The bundled Live Ops manifest now describes the currency the claim handler actually awards: Nebula. Event descriptions also state the real per-claim reward and per-save permanent bonus effect instead of advertising unimplemented CyberTokens/NeonCrystals.
- The bundled event claim caps now permit up to 25 total permanent bonus-level increases across the two events (20 + 5), matching the documented level-25 cap instead of making levels 16–25 unreachable from the shipped manifest.
- Queue removal now bypasses Repeat One once when the currently playing track is removed from the queue, so the deleted queue entry does not loop forever after its current playback ends. Manually choosing another track clears that one-shot bypass.
- Achievement milestone payouts now increment `statistics.nebulaEarned` together with the actual Nebula balance, keeping lifetime earnings accurate when achievements unlock during gameplay.
- Weekly/daily challenge claims now add their Nebula payout to `statistics.nebulaEarned` in the domain claim transaction, matching the balance change and preventing the challenge reward path from undercounting lifetime earnings.
- Offline auto-clicker earnings now increment lifetime `moneyEarned` as well as the wallet balance, so the statistics screen includes money granted during startup catch-up.
- World unlock actions are now idempotent: tapping unlock again for an already-unlocked world no longer emits duplicate progression notifications or repeats unlock-side effects.

## Latest stabilization and localization pass (source changes; validation pending)

- Expanded `tools/audit_localization_surface.py` so it now checks literal `localizedText` phrases against the canonical English catalog, flags dynamic interpolation passed into phrase-based localization, and identifies literal `getString` keys that are missing or use English sentences instead of stable key IDs. This is a source audit, not a compiler; run it in CI and triage remaining findings before declaring the audit complete.
- Arcade minigame names and descriptions now resolve from the active app locale instead of creating a separate locale manager that could drift after language changes. Dynamic prompts, scores, timers, reaction times, box results, memory results, reward labels and Number Rush prompts use format-aware catalog lookups. Memory-match rewards now depend on an explicit correctness flag rather than checking translated display text.
- Settings and Staking dynamic values now use locale-aware formats. The English catalog has entries for the newly audited settings, staking, Arcade, Ultra/Prestige and crash-recovery messages. Existing locale overlays inherit these entries from the English catalog until translated overlays are completed; full translation parity still needs a fresh validation report.
- `LocaleManager` now attempts to resolve phrase-based fallbacks against the English catalog when a call does not provide a separate default. This improves legacy `localizedText` call coverage; dynamic phrases have been moved to keyed format calls in the reviewed screens.
- Removed the old, duplicate More-screen audio import/dialog path and its unused navigation arguments. The More hub now links to the reusable full-screen offline music player instead of exposing a second, partially separate player implementation.
- Music state restoration now removes missing track IDs from persisted queue/playlists and clears a stale persisted current-track ID. Empty queue playback now stops cleanly, and removing a non-current duplicate of the playing track no longer incorrectly consumes the Repeat One bypass.
- Notification delivery now has an awaitable worker-facing path, so `GameReminderWorker` waits for notification handling instead of completing while its fire-and-forget coroutine may still be pending. Category cancellation is available, and disabling global/category notification settings clears already-posted notifications. App-preference JSON imports now resynchronize reminder work and widgets.
- Music service stop now removes the foreground notification and scheduled notification updater before stopping, preventing the service loop from reposting a stale playback notification.

### Remaining release gates

- Run localization/JSON/version checks, the expanded localization-surface audit, Kotlin preflight, Gradle unit tests, lint and debug/release APK builds on the latest `master` revision.
- Run the Android emulator workflow and inspect test results/logs for startup, crash recovery, save-slot switching, notification navigation, widget launch routing and music playback lifecycle.
- Review and translate any newly added English catalog entries across all 23 locale overlays; confirm placeholder parity and audit findings after running the repository scripts.
- Finish balancing review with gameplay tests for relic set thresholds, mastery costs/milestones, secret-area gates, Home Base level caps, boss/Rift payout scaling and one-time endgame claims. Source review alone does not establish stable progression balance.

**Status: not yet stable.** These are committed source changes. Build, lint, unit-test and emulator results have not been obtained for this revision.

## Follow-up audit additions (2026-10-10)

- The World Map mastery panel now computes the same discounts as the progression action: 20% for the world's secret, up to 20% from Home Base, and 10% for the complete equipped relic set, capped at 40%. The affordability check and displayed cost now match the amount charged by the ViewModel.
- Music-library track removal now adjusts the queue cursor when the removed track appears before or at the selected queue entry. This avoids skipping tracks or leaving the cursor outside the filtered queue after deleting a library item.
- The localization surface pass added canonical English entries for music-player, crash recovery, achievement tiers, dynamic money/income, save-slot creation, upgrade multipliers and legacy screen headings. Dynamic money/queue labels now use format strings instead of phrase-based lookups. The audit also recognizes catalog key families such as `loot_tier_common` and `guided_tutorial_title_0` rather than reporting their prefixes as missing literal keys.
- Build and emulator workflows now run on pushes to `master` and pull requests, with same-branch concurrency cancellation so superseded checks do not keep consuming runner time.

### Latest validation status

- Localization validation, version metadata, JSON asset validation, the localization-surface audit, hardcoded-string audit and Kotlin/build preflight all passed on earlier runs in this change series.
- Gradle unit-test tasks repeatedly failed during dependency resolution before test execution. The runner reported that it could not find `org.jetbrains.kotlinx:coroutines-core:1.8.1`, `org.jetbrains.kotlinx:coroutines-android:1.8.1` and `androidx.glance:appwidget:1.2.0` in the configured Google Maven/Maven Central repositories. These versions are already present in a previously successful revision, so this is not yet evidence of a source compilation failure; the final run on the latest commit must confirm whether resolution recovers.
- Emulator runtime tests did not start because their JVM unit-test step hit the same dependency-resolution failure.
- **Not stable yet:** a fresh build and emulator run for the latest workflow configuration is pending. Lint, Gradle check, APK output and runtime behavior remain unverified.


## Build blocker correction (2026-10-10)

- Fixed three invalid Maven artifact names in `gradle/libs.versions.toml`: coroutines dependencies now use `kotlinx-coroutines-core` and `kotlinx-coroutines-android`, and Glance uses `glance-appwidget`. The previous coordinates omitted the artifact-name prefixes, matching the dependency-resolution failures in recent Actions runs.
- The Android emulator workflow is now manual-only. Pushes and pull requests no longer start emulator jobs; do not resume emulator tests until explicitly requested.
- A new build workflow is expected from the dependency-coordinate commit. APK compilation and unit-test status remain pending until that run completes.
