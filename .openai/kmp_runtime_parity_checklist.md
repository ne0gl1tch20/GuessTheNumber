# KMP Production Guessing Runtime Parity Matrix

**Status:** Planned, not yet executed  
**Date opened:** 2026-10-15  
**Scope:** Android production Play screen while the controlled shared input and shared feedback card are integrated. This checklist is evidence collection, not a claim that parity has passed.

## Rules

- Test on an actual Android device or emulator with the production Play screen.
- Record app version/commit, device/API level, locale and settings for each test session.
- Do not change expected behavior just to make the shared UI pass. Compare with the last known production behavior.
- Keep platform-specific persistence, random feedback tips, rate limiting, rewards and game-state mutation host-owned unless a separate migration is planned and tested.
- A green CI run is necessary for code changes, but does not count as evidence for manual runtime behavior.
- Mark each row **Pass**, **Fail** or **Blocked** and include an observation. Never mark untested cases as passing.

## Test matrix

| ID | Area | Steps | Expected behavior to verify | Status / evidence |
|---|---|---|---|---|
| R1 | Input filtering | Enter digits, letters, punctuation and a negative sign where supported by the prior input behavior. | Input acceptance/rejection matches the prior production field; no unexpected characters or crashes. | Not run |
| R2 | IME submit | Enter a valid integer and submit using the keyboard action. | Exactly one submission occurs and follows the existing production path. | Not run |
| R3 | Button submit | Enter a valid integer and tap the guess button. | Same submission path and result as keyboard submission. | Not run |
| R4 | Empty/malformed input | Clear the field or enter text that cannot parse to a Long, then submit. | No invalid guess is applied; behavior matches the previous screen. | Not run |
| R5 | Lower boundary | Submit the current minimum and a value below it, if input allows. | Range handling and target comparison match the existing production rules; record any coercion behavior. | Not run |
| R6 | Upper boundary | Submit the current maximum and a value above it, if input allows. | Range handling and target comparison match the existing production rules; record any coercion behavior. | Not run |
| R7 | Too-low feedback | Submit a valid in-range guess below the target. | Existing localized feedback/tips appear; shared feedback status uses the low state and the correct styling. | Not run |
| R8 | Too-high feedback | Submit a valid in-range guess above the target. | Existing localized feedback/tips appear; shared feedback status uses the high state and the correct styling. | Not run |
| R9 | Correct feedback | Submit the target number. | Existing success feedback, reward path and celebration behavior remain intact. | Not run |
| R10 | Rate limiting | Submit guesses rapidly enough to trigger the existing limiter. | Rate-limit message appears and no extra guess/reward mutation is applied. | Not run |
| R11 | Blind mutator | With the relevant mutator active, submit a guess. | Feedback remains hidden as before; the shared status integration does not reveal the target result. | Not run |
| R12 | Rewards and counters | Compare currency, attempt/correct counts and streak after known correct/incorrect guesses. | Existing reward arithmetic and counters are unchanged. | Not run |
| R13 | Locale | Repeat feedback cases in at least two supported locales. | Host-provided strings remain localized; shared UI does not hardcode production feedback wording. | Not run |
| R14 | Reduced motion | Repeat correct feedback with reduced motion enabled. | Existing reduced-motion timing and visual behavior remain consistent. | Not run |
| R15 | Persistence | Make guesses, leave/reopen the app and inspect saved state. | Save format, save slots, encrypted backup/import and recovery behavior remain compatible. | Not run |
| R16 | Regression | Navigate away and back, then repeat a guess. | Input and feedback state reset/persist exactly as the existing production flow expects; no stale status leaks. | Not run |

## Evidence log

For each execution, append a dated record with:

- Commit SHA / PR
- Device and Android API level
- App build variant and locale
- Test IDs executed
- Pass/fail/blocked result per ID
- Exact observed result for failures
- Screenshot, log or reproduction notes where useful
- Follow-up issue/PR link

## Exit criteria

- Every row is executed or explicitly marked blocked with a reason.
- Every failure is fixed or documented as a known blocker with a linked follow-up.
- Android and Windows CI pass for the implementation under review.
- Save compatibility and host-owned game behavior remain intact.
- Only then choose the next production UI extraction. Do not treat the shared pilot or a green CI run as full runtime parity.
