# Android Runtime QA Checklist (Post-KMP Foundation)

**Status:** Planned, not yet executed. This is post-foundation QA, not an active UI-migration roadmap.
**Date opened:** 2026-10-15
**Scope:** Verify production Android Play behavior after integrating the shared controlled input and feedback UI.

## Rules

- Test on an actual Android device or emulator with the production Play screen.
- Record app version/commit, device/API level, locale and settings for each session.
- Compare behavior with prior production behavior. Do not change expected behavior just to make shared UI pass.
- Keep persistence, random feedback tips, rate limiting, rewards and game-state mutation host-owned unless separately planned and tested.
- Green CI is necessary for code changes but does not count as evidence for manual runtime behavior.
- Mark each row Pass, Fail or Blocked and include an observation. Never mark untested cases as passing.
- These checks do not block unrelated feature development unless a concrete regression is found.

## Test matrix

| ID | Area | Steps | Expected behavior | Status / evidence |
|---|---|---|---|---|
| R1 | Input filtering | Enter digits, letters, punctuation and a negative sign where supported. | Character acceptance matches prior behavior; no crash. | Not run |
| R2 | IME submit | Enter a valid integer and submit using the keyboard action. | Exactly one submission follows the existing path. | Not run |
| R3 | Button submit | Enter a valid integer and tap guess. | Same submission path and result as keyboard submit. | Not run |
| R4 | Empty/malformed input | Clear field or enter text that cannot parse to Long, then submit. | No invalid guess; prior behavior retained. | Not run |
| R5 | Lower boundary | Submit the minimum and a value below it, if allowed. | Range handling and comparison match existing rules. | Not run |
| R6 | Upper boundary | Submit the maximum and a value above it, if allowed. | Range handling and comparison match existing rules. | Not run |
| R7 | Too-low feedback | Submit a valid in-range guess below target. | Localized feedback/tips and low-state styling remain correct. | Not run |
| R8 | Too-high feedback | Submit a valid in-range guess above target. | Localized feedback/tips and high-state styling remain correct. | Not run |
| R9 | Correct feedback | Submit target number. | Success feedback, reward path and celebration remain intact. | Not run |
| R10 | Rate limiting | Submit guesses rapidly enough to trigger limiter. | Message appears; no extra guess/reward mutation. | Not run |
| R11 | Blind mutator | Submit with the relevant mutator active. | Feedback remains hidden as before. | Not run |
| R12 | Rewards/counters | Compare currency, attempt/correct counts and streak. | Existing reward arithmetic and counters remain unchanged. | Not run |
| R13 | Locale | Repeat feedback cases in at least two supported locales. | Host-provided strings remain localized. | Not run |
| R14 | Reduced motion | Repeat correct feedback with reduced motion enabled. | Existing reduced-motion behavior remains consistent. | Not run |
| R15 | Persistence | Guess, leave/reopen app and inspect saved state. | Save format, slots, backup/import and recovery remain compatible. | Not run |
| R16 | Regression | Navigate away/back, then repeat a guess. | Input/feedback state matches expected behavior; no stale status. | Not run |

## Evidence log

For each execution, append commit SHA, device/API level, build variant, locale, test IDs, results, observations and screenshot/log/reproduction notes where useful.

## Outcome

- Record actual outcomes; do not infer runtime behavior from CI.
- Fix demonstrated regressions or open a focused follow-up issue.
- This checklist is QA, not authorization to continue extracting unrelated UI components.
