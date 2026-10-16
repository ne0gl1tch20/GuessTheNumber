# Windows Migration Stage 2-D — Parity Verification

**Status:** PR #217's Android Validation and Windows Desktop CI passed on its exact head; PR #218's catalog-driven follow-up is pending CI. Stage 2 exit gate remains open pending complete W1/W2 parity and full runtime evidence.  
**Reviewed:** 2026-10-16  
**Scope:** Deterministic core guess transitions and Windows save/reload integration. This is not a claim of full gameplay/economy or runtime parity.

## Current repository candidate (2026-10-16)

Historical evidence below refers to the commits named in each row and is not automatically evidence for the current master. PR #215 is merged. PR #217 is merged, and its exact-head Android Validation and Windows Desktop CI passed. Current follow-up: [PR #218](https://github.com/ne0gl1tch20/GuessTheNumber/pull/218), which replaces duplicated hardcoded talent costs/prerequisites with catalog-driven validation; CI for #218 must be checked separately.

- [Android Validation and Tests, run 38901674190](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38901674190): **passed** for PR #217 head `9162283e57f3c310e59e474b917ff13caa19f0ab`.
- [Windows Desktop CI, run 38901674304](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38901674304): **passed** for that same PR #217 head, including compilation/tests and MSI upgrade verification.
- PR #218 adds catalog-driven talent validation; its checks are separate and pending. No runtime screenshot parity or full Windows runtime verification is claimed.
- **Stage 2-D / Stage 2 exit gate: still open.** Earlier successful workflow runs listed below are historical evidence for their specific commits only.

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

## Stage 2-D follow-up — talent transaction hardening

- The desktop talent transaction now validates talent IDs, exact canonical costs, and prerequisite IDs against the Android talent catalog before changing Prestige or unlock state.
- Added deterministic regression coverage for unknown talents, caller-supplied discounted/negative costs, prerequisite mismatches, missing prerequisites, successful purchase at the canonical cost, and duplicate purchases.
- This closes a transaction-integrity gap in the Stage 2 economy review. It does **not** prove the talent effects themselves are fully applied at runtime, and it does not close the overall Stage 2 gate.
- Windows and Android CI for this new candidate must pass before this change is considered validated. Runtime checks of talent effects and the remaining W1/W2 rows are still required.

## Current findings

- Android and Windows use the same production `GameEngine` and `GameState` source boundary, so this slice avoids a second desktop-only implementation of guessing rules.
- Windows persists the production state through its desktop-native adapter. The integration test makes guess transitions and disk round-trips one assertion path.
- The Windows app now has a separate Save Slots destination with ten local slots and active-slot persistence; import/export and slot reset remain pending in the parity audit. Continue verifying save selection and production gameplay integration rather than assuming the slot UI proves every save path.
- PR #199 adds the initial Windows economy/progression slice: catalog-backed Money/Prestige/Ultra upgrades, reset actions, supported Nebula shop purchases, talent unlocks and the feedback-driven Auto Clicker. See [PR #199](https://github.com/ne0gl1tch20/GuessTheNumber/pull/199).
- The follow-up reward-parity slice now records daily guess/correct/streak progress, clears stale daily claim state on a date change, applies Android's 1% Epic / 5% Rare / 18% Common chest drop thresholds, and exposes claimable daily rewards plus chest counts in the Windows Play screen. Deterministic tests cover claim amounts, duplicate claims, threshold boundaries and stacked reward bonuses.
- The latest economy slice implements the two defined shop boosts: Nebula Crystal doubles future Nebula rewards from desktop guess bonuses, daily quests and achievement milestones; Golden Guess Pass adds a critical-sized Money bonus on every tenth lifetime guess when correct. Unsupported/unknown shop effects remain disabled.
- Desktop guesses and accepted economy transactions now evaluate Android-matching achievement milestones with one-time rewards. Basic world unlock/select hooks enforce the Android world's boss, correct-guess, upgrade, Prestige and Ultra requirements. Full world map/boss/relic/activity parity remains unfinished. PR #215 now contains partial candidate implementations for some of these screens, but they remain unverified and do not close the Stage 2 gate.
- Historical CI passed on commit `38c7421`: [Windows Desktop CI run 38840144782](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38840144782) and [Android Validation run 38840144773](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38840144773). These are historical results, not validation for later candidates.
- The user reports that focused Windows checks passed, including Enter-to-submit, button submission, shop/reward/progression checks and restart/save persistence. This is recorded as user-reported runtime evidence, not an independently witnessed session.
- Existing deterministic economy tests cover stacked bonus totals, Nebula Crystal doubling daily-quest/achievement/guess rewards, Golden Guess Pass on every tenth lifetime guess, one-time achievement rewards, world requirement enforcement and idempotence.
- The Stage 2 follow-up candidate adds exact-cost 1/10/100/MAX purchases across Money, Prestige and Ultra upgrades, plus mutator toggles and confirmation dialogs for Prestige/Ultra resets. Bulk buys now take the largest affordable prefix for 10/100/MAX, matching Android. The desktop catalog reader now loads from `game/`, with canonical talent, Prestige shop and Ultra shop JSON bundled; deterministic tests cover catalog loading, bulk costs/affordability and mutator range/validation. These additions still need candidate CI and user runtime verification.
- CI and those focused checks do not establish full parity for every W1/W2 row. Window resizing/high-DPI, localization, update-channel behavior and full MSI lifecycle remain unverified.

## Stage 2-D gate

**Not passed yet.** PR #217's Windows Desktop CI and Android Validation passed, and the user reports focused runtime checks for Enter submission, Submit button, shop/rewards/progression and restart/save passed. PR #218's catalog-driven talent validation still needs its own CI results. The remaining gate is broader W1/W2 parity review and evidence: verify required economy/reward/reset behavior against Android and close any remaining gaps, keeping untested runtime rows explicitly unverified. Full world map/boss/activity screens remain Stage 3 work. Do not call Stage 2 complete until the complete W1/W2 requirements are evidenced.

## Next required checks

1. **Done for the candidate:** Android Validation run 38840144773 and Windows Desktop CI run 38840144782 passed on commit `38c7421`.
2. **Automated fixtures present:** exact stacked reward totals, Nebula Crystal reward doubling, Golden Guess Pass tenth-guess behavior, achievement reward idempotence and world unlock/select requirements are covered by `DesktopEconomyTransactionsTest`.
3. **Focused runtime checks reported passed by user:** Enter/button submission, shop/rewards/progression and restart/save. Broader windowing, localization, update and installer runtime checks remain unverified.
4. **Merged earlier:** [PR #204](https://github.com/ne0gl1tch20/GuessTheNumber/pull/204) completed the economy-parity slice for bulk buys, mutator controls and reset confirmations. Continue checking remaining runtime and Android-parity gaps; merge/CI history does not substitute for hands-on Windows verification.
5. Remaining gaps: rate-limiting/edge-case parity, canonical talent/catalog effects, broad large-number and reset checks, and actual Windows runtime checks for mutators and bulk purchases. Review remaining W1/W2 rows against Android before declaring Stage 2 complete.


## Cross-platform parity target — one game across every platform

**Product goal:** Android and Windows must be the same game, not separate implementations that merely offer similar features. Android is the reference implementation for intended gameplay and behavior unless a requirement explicitly defines a platform-specific adaptation.

### Parity contract

- **Screen parity:** Every supported screen and flow must have an equivalent on each platform, including content, navigation, controls, dialogs, loading/empty/error states, locked states, and progression feedback. Layout may adapt to phone and desktop form factors, but functionality and meaning must not silently disappear.
- **Logic parity:** The same inputs and starting state must produce the same gameplay outcomes across platforms: guesses, rewards, economy calculations, upgrade costs, purchase validation, talents and their effects, achievements, daily rewards, resets, unlock requirements, bosses, relics, save/reload behavior, and other supported mechanics.
- **Shared authoritative rules:** Prefer one shared Kotlin implementation for game rules, state transitions, catalogs, validation, and calculations. Android and Windows should call that shared implementation rather than independently reimplementing the same rules. Keep platform-specific adapters for storage, lifecycle, input, notifications, audio, and operating-system integration where needed.
- **Shared UI where practical:** Reuse common screen/components when the multiplatform architecture supports it. Where native or platform-specific UI is necessary, treat Android as the behavioral and content reference and verify the Windows counterpart against it.
- **Intentional platform adaptations only:** Touch versus mouse/keyboard input, window resizing, responsive layout, and OS-specific integrations may differ when needed. Document each intentional difference and confirm it does not change the underlying game rules or remove a supported capability.
- **No parity by assumption:** Compilation, shared source files, and unit tests are useful evidence but do not alone prove screen or runtime parity. Track automated results and hands-on runtime evidence separately.

### Definition of done for cross-platform parity

1. Maintain a screen-by-screen inventory mapping each Android screen/flow to its Windows equivalent, with matching states, actions, navigation, and visible information.
2. Map each gameplay/economy mechanic to its authoritative shared implementation and tests. Remove duplicate platform-specific rule implementations where practical.
3. Use deterministic fixtures or parity tests to run the same starting states and actions on each platform and compare resulting state, rewards, balances, unlocks, and persisted data.
4. Verify the actual Windows app at runtime for the relevant flows; CI compilation alone is not enough. Record the platform/build, steps, result, and any screenshots or logs available.
5. Keep a list of intentional platform differences with reasons and verification. An unexplained mismatch remains a parity gap.
6. Keep the Stage 2 exit gate open until the scoped W1/W2 requirements have evidence. Do not describe the whole game as cross-platform complete while screens, mechanics, effects, or runtime paths remain unfinished or unverified.


### Platform-specific feature exceptions — owner-approved by design

**The project owner decides which platform-specific features are allowed.** The parity contract is the default, not a restriction on the owner's product decisions. The owner may propose or approve platform-specific QoL, UI, operating-system integration, or gameplay features.

- **QoL and presentation:** Examples include Windows keyboard shortcuts and mouse interactions, Android touch gestures and haptics, responsive desktop layouts, accessibility adaptations, platform-native notifications, widgets, and file-picker integration.
- **Gameplay exceptions:** A platform may also receive a unique gameplay feature or different behavior when the owner explicitly approves it. Do not introduce gameplay or balance differences silently or infer approval from technical convenience.
- **Document each exception:** Record the target platform, behavior, purpose, owner approval, affected shared systems, and how it will be verified. Link the decision or issue/PR when available.
- **Protect shared systems:** Unless the owner explicitly approves a change, platform-specific QoL must not alter core balance, reward calculations, progression, reset semantics, or save compatibility. Approved gameplay exceptions should still preserve save integrity and clearly define any intended cross-platform consequences.
- **Verify both sides:** Test the platform-specific behavior on its target platform and regression-check shared behavior on the other platform. A documented exception is intentional, not automatically verified.

Examples: a Windows-only keyboard shortcut that opens the reset confirmation is an ordinary QoL exception. A Windows-only feature that changes Prestige rewards is a gameplay exception and requires the owner's explicit approval before implementation.

**Decision rule:** If a proposed feature creates a platform difference and owner approval is unclear, ask the owner. Do not reject the idea just because it is platform-specific, and do not treat the parity goal as permission to make product decisions on the owner's behalf.

**Current status:** This is the target architecture and acceptance contract, not a claim that parity has already been achieved. The existing Stage 2-D gate remains open; remaining gaps listed in this document must be resolved and verified against Android.


## Latest follow-up — canonical talent catalog (2026-10-16)

- PR #217 merged the initial validation guard and both CI workflows passed for its exact head.
- PR #218 changes the validation source from a hardcoded Kotlin map to the bundled `game/talents.json` resource, adds a data-driven test over every catalog entry, and makes Talent Web parent gating use the catalog's `requiredParentId`. The Android asset and desktop resource were confirmed byte-for-byte identical when this candidate was prepared.
- **PR #218 CI:** pending at the time of this documentation update. Update this status with the exact run links/results before merging.
- **Manual verification still required:** open the Windows app, inspect the Talent Web, buy one affordable talent, verify its effect in gameplay, restart/reload, and confirm the purchase/effect persists. Also verify an unaffordable talent and a locked prerequisite cannot be purchased.

- The Talent Web's visible English labels/descriptions still follow Android's fallback copy rather than the JSON display fields; Windows locale-aware rendering remains a separate parity gap and must not be marked verified.
