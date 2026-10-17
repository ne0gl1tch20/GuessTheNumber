# Shared Web UI Gameplay Bridge Contract

**Status:** Proposed contract for the first production gameplay slice. Not implemented.
**Reviewed:** 2026-10-17
**Scope:** The first HTML-hosted guessing interaction. This is a contract proposal, not a claim that a production screen or bridge action exists.

## Boundary

HTML/CSS/JavaScript owns presentation: layout, input editing, visual feedback, transitions and responsive behavior. Kotlin owns the game state and every decision that can affect the save: guess validation, target comparison, attempt/streak changes, rewards, bonuses, quests, bosses, achievements, progression and persistence.

The browser must never receive the hidden target number. It must not calculate or submit rewards, currency deltas, progression unlocks or a purported guess result.

## Version 1 request envelope

Every request is a JSON object with exactly these fields:

```json
{
  "version": 1,
  "id": "guess-0001",
  "type": "submit_guess",
  "payload": {
    "guess": "42"
  }
}
```

- `version` is the integer `1`.
- `id` is a caller-generated unique request identifier matching `[A-Za-z0-9_-]{1,48}`. A host should bound retained IDs and reject/replay a duplicate without applying a gameplay action twice.
- `type` is an allow-listed action name.
- `payload` is an object with an action-specific exact schema. Unknown fields and wrong JSON types are rejected.
- Apply a strict message-size limit before parsing. Do not treat client validation as authoritative.

For the first slice, `submit_guess` accepts a decimal integer string so the bridge can reject overflow and malformed input explicitly before conversion to Kotlin's `Long`. The Kotlin adapter must still invoke the existing authoritative guess path rather than implement a second copy of the game rules. Now `GameViewModel.makeGuessWithResult(guess: Long)` exposes a typed, secret-free outcome while `makeGuess(guess: Long): Boolean` remains as a compatibility wrapper for existing callers. The new result carries acceptance, the engine's public feedback token and safe range/counter fields, never `targetNumber` or reward internals. The result is derived from the same `GameEngine.processGuess` result used by the production pipeline; JavaScript must not recalculate it. This change is source-reviewed only and still needs compilation and focused tests.

## Version 1 response envelope

Success and rejection use a versioned response with the matching request ID:

```json
{
  "version": 1,
  "id": "guess-0001",
  "type": "guess_result",
  "stateRevision": 12,
  "result": {
    "status": "too_low",
    "messageKey": "guess.too_low"
  },
  "viewState": {
    "rangeMin": 1,
    "rangeMax": 100,
    "attempts": 3,
    "streak": 2,
    "busy": false
  }
}
```

This is an illustrative shape only. Field names and status values must be reconciled with the real Kotlin model and localization system before implementation. It intentionally omits the target number and any client-authored reward values.

Use stable machine-readable status codes and localization keys rather than requiring the host to send presentation-ready English strings. Return the authoritative view state after processing so the web UI renders the result rather than guessing what happened. Keep the first slice balanced: HTML/CSS owns layout, input affordances, visual hierarchy and motion; Kotlin owns range/state values, guess outcome, rate limiting, game rules, reward/progression effects and persistence. The web layer must not become a second game engine.

## Required behavior

1. Reuse the existing Kotlin guess/game-engine path and its anti-cheat/rate checks. Do not duplicate the game engine in JavaScript.
2. Validate message origin, main-frame status, exact local page, message length, protocol version, action allow-list, request ID, payload shape, numeric bounds and conversion overflow.
3. Track in-flight and recently completed request IDs. A duplicate must not apply the same action twice. Define a bounded retention strategy and a clear response for a repeated ID.
4. Associate responses with request IDs and state revisions. The UI must ignore stale replies that would roll back a newer rendered state.
5. Disable or mark the submit control busy while a request is pending, while still treating Kotlin as the final authority for rapid or repeated submissions.
6. Send a response for validation failures and processing failures. Align save status with the real persistence API before promising durability: the current `makeGuess` path calls `saveGameAsync()` and returns before that asynchronous save has reported success. Do not claim a save has completed merely because the guess action returned. If the bridge requires durable-commit confirmation, introduce an explicit result from the persistence path instead of inferring it.
7. Preserve existing save compatibility and transaction/recovery behavior. Do not add a parallel web-owned save.
8. Use existing localization resources, accessible status announcements, keyboard/IME behavior, focus handling and reduced-motion preferences.
9. Keep a reversible Compose route until parity and acceptance checks pass on Android and the chosen Windows host.

## First-slice scope

Include only the core guessing surface: range display, numeric input, submit action, authoritative outcome/status, attempts/streak and loading/error states. Keep the rest of the current Play screen in Compose until the team decides whether to embed a small web panel or migrate the whole Play route after measuring scrolling, keyboard, focus and accessibility behavior.

## Acceptance checks before production use

- Valid, malformed, negative, overflow and out-of-range guesses.
- Correct and incorrect guess results produced only by Kotlin.
- Duplicate request IDs, rapid repeated submissions, stale responses and processing/persistence failures.
- No target number or client-controlled reward/progression values in web state or bridge responses.
- Unknown fields/actions, wrong types, oversized messages, wrong origins/pages and non-main-frame calls rejected.
- Existing localization, screen-reader announcements, keyboard/IME, focus and reduced-motion behavior.
- Offline startup and reload; Android lifecycle recreation and back navigation.
- Same bundle and equivalent behavior on the selected Windows host.
- Save compatibility/recovery checks and a reversible Compose fallback.

## Balanced first-slice boundary

Move only the primary guessing interaction first. Keep economy panels, boss/event sections, navigation and the other large PlayScreen sections in Compose until the hybrid-versus-whole-route choice is measured. Preserve the existing Kotlin action path and use a typed secret-free result from that path; do not duplicate target comparison or hint logic in JavaScript. The typed guess-result seam is now implemented in `GameViewModel`, with the Boolean API retained. The save call remains asynchronous, so the UI must not claim durable save completion based only on the returned result.

## Current implementation status

The existing debug prototype supports only `read_state` and `increment_counter` against a temporary Kotlin counter. It does not implement this contract, submit guesses, access the game save or demonstrate Windows parity. This document defines the next integration target; it does not mark Phase 0 complete.
