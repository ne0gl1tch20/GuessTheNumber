# Windows Migration Stage 2-D — Parity Verification

**Status:** Candidate CI and focused interactive checks passed; Stage 2 exit gate remains open pending complete W1/W2 parity and full runtime evidence.  
**Reviewed:** 2026-10-16  
**Scope:** Deterministic core guess transitions and Windows save/reload integration. This is not a claim of full gameplay/economy or runtime parity.

## Evidence collected

| Area | Evidence | Result |
|---|---|---|
| Production guess engine | `shared/src/jvmAndAndroidMain/.../domain/engine/GameEngine.kt` is shared by Android and Windows. | Source verified |
| Android core-state parity | `app/src/test/java/com/jarrlyyy/guessthenumber/domain/engine/GuessCoreStateParityTest.kt` checks seeded correct/incorrect transitions against shared core rules. | Test exists; CI must run on the current candidate |
| Desktop save integrity | `DesktopSaveStoreTest.kt` covers round-trip, backup recovery, corrupt primary, legacy defaults and injected write failure. | Test exists; merged PR #196 CI passed |
| Engine + persistence integration | `ProductionGuessSaveParityTest.kt` checks a deterministic wrong guess, reload, correct guess, counters and another reload using the production `GameEngine` and desktop store. | PR #197 merged after all checks passed |
| Android validation after PR #202 candidate | [Run 38840144773](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38840144773) | Passed on candidate commit `38c7421` |
| Windows Desktop CI after PR #202 candidate | [Run 38840144782](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38840144782) | Passed on candidate commit `38c7421` |
| User-reported Windows runtime checks (2026-10-16) | User confirmed Enter submission, Submit button, shop/reward/progression checks and restart/save check passed. | **Passed by user report**; not independently observed in this session |
| Additional Windows runtime coverage | Resizing/high-DPI, localization, update-channel and full installer lifecycle | **Unverified**; do not infer these from CI or the focused gameplay checks |

## Current findings

- Android and Windows use the same production `GameEngine` and `GameState` source boundary, so this slice avoids a second desktop-only implementation of guessing rules.
- Windows persists the production state through its desktop-native adapter. The integration test makes guess transitions and disk round-trips one assertion path.
- The current Windows Play screen still hardcodes save slot 1 and presents only the basic guessing loop and a few counters.
- PR #199 adds the initial Windows economy/progression slice: catalog-backed Money/Prestige/Ultra upgrades, reset actions, supported Nebula shop purchases, talent unlocks and the feedback-driven Auto Clicker. See [PR #199](https://github.com/ne0gl1tch20/GuessTheNumber/pull/199).
- The follow-up reward-parity slice now records daily guess/correct/streak progress, clears stale daily claim state on a date change, applies Android's 1% Epic / 5% Rare / 18% Common chest drop thresholds, and exposes claimable daily rewards plus chest counts in the Windows Play screen. Deterministic tests cover claim amounts, duplicate claims, threshold boundaries and stacked reward bonuses.
- The latest economy slice implements the two defined shop boosts: Nebula Crystal doubles future Nebula rewards from desktop guess bonuses, daily quests and achievement milestones; Golden Guess Pass adds a critical-sized Money bonus on every tenth lifetime guess when correct. Unsupported/unknown shop effects remain disabled.
- Desktop guesses and accepted economy transactions now evaluate Android-matching achievement milestones with one-time rewards. Basic world unlock/select hooks enforce the Android world's boss, correct-guess, upgrade, Prestige and Ultra requirements. Full world map/boss/relic/activity screens remain Stage 3 work.
- Candidate CI is confirmed on commit `38c7421`: [Windows Desktop CI run 38840144782](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38840144782) and [Android Validation run 38840144773](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38840144773) both completed successfully.
- The user reports that focused Windows checks passed, including Enter-to-submit, button submission, shop/reward/progression checks and restart/save persistence. This is recorded as user-reported runtime evidence, not an independently witnessed session.
- Existing deterministic economy tests cover stacked bonus totals, Nebula Crystal doubling daily-quest/achievement/guess rewards, Golden Guess Pass on every tenth lifetime guess, one-time achievement rewards, world requirement enforcement and idempotence.
- The Stage 2 follow-up candidate adds exact-cost 1/10/100/MAX purchases across Money, Prestige and Ultra upgrades, plus mutator toggles and confirmation dialogs for Prestige/Ultra resets. Bulk buys now take the largest affordable prefix for 10/100/MAX, matching Android. The desktop catalog reader now loads from `game/`, with canonical talent, Prestige shop and Ultra shop JSON bundled; deterministic tests cover catalog loading, bulk costs/affordability and mutator range/validation. These additions still need candidate CI and user runtime verification.
- CI and those focused checks do not establish full parity for every W1/W2 row. Window resizing/high-DPI, localization, update-channel behavior and full MSI lifecycle remain unverified.

## Stage 2-D gate

**Not passed yet.** The current PR #202 candidate's Windows Desktop CI and Android Validation both pass, and the user reports focused runtime checks for Enter submission, Submit button, shop/rewards/progression and restart/save passed. The remaining gate is broader W1/W2 parity review and evidence: verify required economy/reward/reset behavior against Android and close any remaining gaps, keeping untested runtime rows explicitly unverified. Full world map/boss/activity screens remain Stage 3 work. Do not call Stage 2 complete until the complete W1/W2 requirements are evidenced.

## Next required checks

1. **Done for the candidate:** Android Validation run 38840144773 and Windows Desktop CI run 38840144782 passed on commit `38c7421`.
2. **Automated fixtures present:** exact stacked reward totals, Nebula Crystal reward doubling, Golden Guess Pass tenth-guess behavior, achievement reward idempotence and world unlock/select requirements are covered by `DesktopEconomyTransactionsTest`.
3. **Focused runtime checks reported passed by user:** Enter/button submission, shop/rewards/progression and restart/save. Broader windowing, localization, update and installer runtime checks remain unverified.
4. **New candidate still pending validation:** [Stage 2 economy parity branch](https://github.com/ne0gl1tch20/GuessTheNumber/tree/feat/windows-stage-2-economy-parity) adds bulk buys, mutator controls and reset confirmations. Do not treat the added tests as passing until CI reports success.
5. Remaining gaps: rate-limiting/edge-case parity, canonical talent/catalog effects, broad large-number and reset checks, and actual Windows runtime checks for mutators and bulk purchases. Review remaining W1/W2 rows against Android before declaring Stage 2 complete.
