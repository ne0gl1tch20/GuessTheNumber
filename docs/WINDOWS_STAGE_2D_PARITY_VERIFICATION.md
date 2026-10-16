# Windows Migration Stage 2-D — Parity Verification

**Status:** Partial verification implemented; Stage 2 exit gate remains open.  
**Reviewed:** 2026-10-16  
**Scope:** Deterministic core guess transitions and Windows save/reload integration. This is not a claim of full gameplay/economy or runtime parity.

## Evidence collected

| Area | Evidence | Result |
|---|---|---|
| Production guess engine | `shared/src/jvmAndAndroidMain/.../domain/engine/GameEngine.kt` is shared by Android and Windows. | Source verified |
| Android core-state parity | `app/src/test/java/com/jarrlyyy/guessthenumber/domain/engine/GuessCoreStateParityTest.kt` checks seeded correct/incorrect transitions against shared core rules. | Test exists; CI must run on the current candidate |
| Desktop save integrity | `DesktopSaveStoreTest.kt` covers round-trip, backup recovery, corrupt primary, legacy defaults and injected write failure. | Test exists; merged PR #196 CI passed |
| Engine + persistence integration | `ProductionGuessSaveParityTest.kt` checks a deterministic wrong guess, reload, correct guess, counters and another reload using the production `GameEngine` and desktop store. | PR #197 merged after all checks passed |
| Android validation after PR #196 | [Run 38819660294](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38819660294) | Passed on PR #196 head |
| Windows Desktop CI after PR #196 | [Run 38819660339](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38819660339) | Passed on PR #196 head |
| Interactive Windows runtime | Requires launching the actual Windows build and checking behavior. | **Unverified**; no local laptop test was performed for this audit |

## Current findings

- Android and Windows use the same production `GameEngine` and `GameState` source boundary, so this slice avoids a second desktop-only implementation of guessing rules.
- Windows persists the production state through its desktop-native adapter. The integration test makes guess transitions and disk round-trips one assertion path.
- The current Windows Play screen still hardcodes save slot 1 and presents only the basic guessing loop and a few counters.
- PR #199 adds the initial Windows economy/progression slice: catalog-backed Money/Prestige/Ultra upgrades, reset actions, supported Nebula shop purchases, talent unlocks and the feedback-driven Auto Clicker. See [PR #199](https://github.com/ne0gl1tch20/GuessTheNumber/pull/199).
- The follow-up reward-parity slice now records daily guess/correct/streak progress, clears stale daily claim state on a date change, applies Android's 1% Epic / 5% Rare / 18% Common chest drop thresholds, and exposes claimable daily rewards plus chest counts in the Windows Play screen. Deterministic tests cover claim amounts, duplicate claims, threshold boundaries and stacked reward bonuses.
- The latest economy slice implements the two defined shop boosts: Nebula Crystal doubles future Nebula rewards from desktop guess bonuses, daily quests and achievement milestones; Golden Guess Pass adds a critical-sized Money bonus on every tenth lifetime guess when correct. Unsupported/unknown shop effects remain disabled.
- Desktop guesses and accepted economy transactions now evaluate Android-matching achievement milestones with one-time rewards. Basic world unlock/select hooks enforce the Android world's boss, correct-guess, upgrade, Prestige and Ultra requirements. Full world map/boss/relic/activity screens remain Stage 3 work.
- These source changes need current Windows and Android CI results. Actual Windows runtime behavior remains unverified until tested.
- A successful build or unit-test run does not verify mouse/keyboard interaction, window resizing, high-DPI rendering, close/reopen behavior or visual parity on an actual Windows session.

## Stage 2-D gate

**Not passed yet.** The economy slice now includes daily quests/loot, supported shop boosts and basic achievement/world unlock hooks, with deterministic tests added. This branch still needs current Windows Desktop CI and Android validation to pass, followed by actual Windows runtime verification of guessing, shop effects, progression rewards and save/reload. The full world map/boss/activity UI remains Stage 3 work. Do not call Stage 2 complete until its W1/W2 requirements and runtime evidence are recorded.

## Next required checks

1. Confirm Android validation and Windows Desktop CI pass for this PR's head commit.
2. Finish the remaining Stage 2-B economy/reward/reset parity gaps and add deterministic expected-value parity fixtures.
3. Run an actual Windows session for accepted/invalid guesses, correct-answer transitions, restart/reload, save recovery, keyboard/mouse behavior and window resizing.
4. Update the parity matrix with the exact run links and runtime evidence; leave all untested behavior marked unverified.
