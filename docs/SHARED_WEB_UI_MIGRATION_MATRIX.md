# Shared Web UI Migration Matrix

**Status:** Planning and source audit only; no production player-facing screen has migrated.
**Reviewed:** 2026-10-17
**Architecture authority:** [Shared Web UI Architecture](SHARED_WEB_UI_ARCHITECTURE.md)
**Product roadmap:** [Joy Phase Roadmap](../.openai/docs/JOY_PHASE_ROADMAP.md)
**Windows parity authority:** [Windows Feature Parity Plan](WINDOWS_FEATURE_PARITY_PLAN.md)
**Shared visual foundation:** [Shared Web UI Theme Foundation](SHARED_WEB_UI_THEME_FOUNDATION.md)

This matrix tracks real player-facing screen migration. The isolated `SharedWebUiPrototypeScreen` and its temporary counter are excluded from production migration counts.

## First production slice: Play screen core loop

**Candidate:** the core guessing experience in `app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/PlayScreen.kt`.

The source currently receives `GameState`, `onMakeGuess: (Long) -> Boolean`, progression callbacks and `incomePerSecond`. It owns presentation concerns such as the editable guess text, feedback message/tone, Math Strategy panel state and correct-guess celebration. It also renders many additional sections (economy stats, active boss battle, random event, Beyond the Numbers shortcut, and progression-related panels). Do not treat the entire file as a tiny, low-risk migration.

### First-slice scope proposal

Migrate the primary guessing surface as the first real web-backed gameplay experience:

- Show the current allowed range using the latest Kotlin-owned game state.
- Accept a numeric guess with accessible touch, keyboard and focus behavior.
- Send a typed, versioned `submit_guess` request; never send or accept a target number, reward amount, or client-computed result.
- Let Kotlin run the existing `GameViewModel.makeGuess(guess: Long)` path, including rate limiting, `GameEngine.processGuess`, talent bonuses, daily quest/loot handling, boss interactions, achievements and asynchronous persistence.
- Return the authoritative updated view state and a controlled result/status to render high/low/correct feedback, attempts and streak. Do not duplicate the guess rules or compute rewards in JavaScript.
- Respect locale, reduced-motion settings, validation errors and busy/double-submit state.
- Keep the surrounding Play screen sections on Compose for this first slice if that makes the migration reversible. Before embedding the web surface inside the existing scroll layout, explicitly verify nested scrolling, height, keyboard resize, focus and accessibility. If those checks make a hybrid panel unreliable, migrate the complete Play route as a cohesive web screen rather than shipping a fragile nested WebView.

### Existing Kotlin authority (source reviewed)

- `GameViewModel.makeGuess(guess: Long)` checks `AntiCheatService.validateGuessRate()`, runs `gameEngine.processGuess`, observes/reset the guessing bot, applies talent bonuses, records daily-guess and loot-drop effects, updates game state, handles active boss battle hits/misses, evaluates achievements, triggers progression notification where applicable and calls `saveGameAsync()`.
- `PlayScreen.submitGuess()` currently validates the text as a `Long`, invokes `onMakeGuess`, handles rate-limit feedback, and chooses presentation feedback. It also builds player-facing clue text from localized/configured feedback messages. The target web implementation must not silently discard these existing behaviors; agree on a typed result/view-state contract before replacing this code.
- `ProductionGuessPresentationAdapter` and shared guessing presentation components already exist. Audit their behavior and the current game engine result before deciding which feedback fields should be returned through the web bridge; do not assume that all Compose-only local feedback can be recreated identically without a contract change.
- The prototype bridge currently supports only `read_state` and `increment_counter`. Neither action is suitable for production gameplay, and neither should be repurposed without defining and testing a production contract.

### Acceptance checklist

- [ ] Use the shared `assets/web-ui/theme.css` token and component foundation; extend it for real screen needs without forking a parallel design system.
- [ ] Agree on a versioned, typed production bridge schema for read-view-state and submit-guess, including request IDs, duplicate/in-flight behavior, validation failures, cancellation and stale responses.
- [ ] Derive view state only from Kotlin; do not expose the secret target number or trust JS-calculated feedback, reward, eligibility, balances or save mutations.
- [ ] Preserve the existing guess/rate-limit/bonus/quest/loot/boss/achievement/save behavior.
- [ ] Match locale and supported formatting behavior; avoid adding hardcoded player-facing strings to HTML.
- [ ] Match reduced-motion and accessible focus/input behavior; keyboard submit and Android IME behavior must be explicit.
- [ ] Verify the layout and scrolling approach on Android; no keyboard overlap, clipped content or broken Compose scrolling.
- [ ] Use the same HTML/CSS/JS assets and bridge semantics on the Windows host; do not claim Windows support before the host exists and is runtime-tested.
- [ ] Add focused contract tests for valid guesses, invalid input, rate limiting, repeated request IDs, stale responses, failed persistence and malformed bridge messages.
- [ ] Compare screenshots and action outcomes against the existing Play screen for equivalent states.
- [ ] Keep a reversible route to the Compose implementation until Android and Windows behavior, save compatibility and performance are verified.

## Screen inventory

The complete screen inventory is not yet source-audited. Add each screen only after identifying its current route/component, state inputs, user actions, localization source, persistence effects, native dependencies and verification evidence.

| Screen / surface | Current status | Next evidence required |
|---|---|---|
| Shared web bridge prototype | Debug-only Android experiment; not production UI; loads the shared CSS foundation | Run focused tests and APK build, exercise lifecycle/offline/bridge behavior on device, complete Windows host spike |
| Shared web theme foundation | Reusable CSS tokens and basic game UI primitives; used by the prototype only | Validate contrast, scaling, accessibility, safe areas and host behavior; adopt in the first production screen |
| Play: core guessing surface | Compose; first production migration candidate | Finalize bridge contract and hybrid-vs-whole-route layout decision |
| Play: economy/progression/boss/event sections | Compose | Inventory individual sections and callbacks before deciding whether they remain Compose temporarily or move with the route |
| Upgrade, Shop, More and progression destinations | Compose | Source-backed inventory, then migrate by coherent feature slices |
| Native/system surfaces | Compose or platform integrations | Keep native where justified; document the boundary rather than forcing them into HTML |

## Evidence rules

Track source implementation, focused tests, CI, Android runtime behavior, Windows runtime behavior, screenshot comparison, save compatibility and installer validation separately. A source audit or successful build alone does not mark a migration row complete.
