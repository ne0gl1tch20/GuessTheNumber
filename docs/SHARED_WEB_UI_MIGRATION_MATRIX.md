# Shared Web UI Migration Matrix

**Status (2026-10-17):** `NavGraph` now honors the bundled `shared_web_play` feature flag and selects `ProductionWebPlayScreen` when enabled, so the canonical `web-play/` HTML/CSS/JS bundle is the actual Android route when the flag is on. The flag configuration is enabled. The four tabs cover the current Play, Worlds, Rewards and Journey slice; remaining player-facing destinations are not yet migrated, and no device/runtime parity claim is made.
**Reviewed:** 2026-10-17
**Architecture authority:** [Shared Web UI Architecture](SHARED_WEB_UI_ARCHITECTURE.md)
**Product roadmap:** [Joy Phase Roadmap](../.openai/docs/JOY_PHASE_ROADMAP.md)
**Windows parity authority:** [Windows Feature Parity Plan](WINDOWS_FEATURE_PARITY_PLAN.md)
**Shared visual foundation:** [Shared Web UI Theme Foundation](SHARED_WEB_UI_THEME_FOUNDATION.md)

This matrix tracks real player-facing screen migration. The isolated `SharedWebUiPrototypeScreen` and its temporary counter are excluded from production migration counts.

## Locked migration scope: all Android player-facing screens

The owner has explicitly chosen a complete shared-web UI for Android mobile, not a permanent hybrid where only Play uses HTML. The same HTML/CSS/JavaScript screen bundle is the target for Windows. Kotlin/KMP remains the only authority for state-changing game decisions and persistence. Compose remains only where a native/system surface or platform host integration is genuinely needed.

Migrate in cohesive, testable feature batches rather than isolated cosmetic PRs:

1. **Foundation and Play:** branding, navigation primitives, shared theme, accessible screen shell, secure local WebView host, typed bridge, and the full core Play loop.
2. **Core game destinations:** Upgrades, Shop, Prestige, Ultra, talents and all currency/economy actions, reusing existing Kotlin transaction APIs.
3. **Progression and activities:** worlds/map, bosses, relics, Home Base, Codex, Endless Rift, achievements, daily quests, arcade/minigames and challenge/event screens.
4. **Player utilities:** settings, music player, save slots/import/export, statistics, changelog, tutorial, localization, and all player-facing More destinations.
5. **Host/system boundary:** keep crash recovery, permissions, notifications, widgets, file pickers and platform-specific settings native where required; route their presentation/results back to the shared UI without duplicating game rules.
6. **Windows parity:** host the exact same bundle in the supported Windows web view and implement the same typed bridge contract. Do not claim parity until the host is integrated and runtime-verified.

Do not enable a new HTML route by default until its source, focused tests, offline startup, lifecycle/back navigation, accessibility/IME, save compatibility and feature parity have been reviewed. Keep the old Compose destination as a reversible fallback during each migration batch. A screen counts as migrated only when its actual interactions and localization are covered, not merely because an HTML mockup exists.

## First production slice: Play screen core loop

**Candidate:** the core guessing experience in `app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/PlayScreen.kt`.

The source currently receives `GameState`, `onMakeGuess: (Long) -> Boolean`, progression callbacks and `incomePerSecond`. It owns presentation concerns such as the editable guess text, feedback message/tone, Math Strategy panel state and correct-guess celebration. It also renders many additional sections (economy stats, active boss battle, random event, Beyond the Numbers shortcut, and progression-related panels). Do not treat the entire file as a tiny, low-risk migration.

### First-slice scope proposal

Migrate the primary guessing surface as the first real web-backed gameplay experience:

- Show the current allowed range using the latest Kotlin-owned game state.
- Accept a numeric guess with accessible touch, keyboard and focus behavior.
- Send a typed, versioned `submit_guess` request; never send or accept a target number, reward amount, or client-computed result.
- Let Kotlin run the existing `GameViewModel.makeGuess(guess: Long)` path, including rate limiting, `GameEngine.processGuess`, talent bonuses, daily quest/loot handling, boss interactions, achievements and asynchronous persistence.
- Return the authoritative updated view state and a controlled result/status to render high/low/correct feedback, attempts and streak. Do not duplicate the guess rules or compute rewards in JavaScript. The current Android callback returns only Boolean and Compose computes feedback using its state, including the target; first introduce a typed secret-free result from the same engine result while preserving existing Boolean callers.
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
- [ ] Add a typed secret-free Kotlin guess outcome without breaking existing Boolean callers; define save-state semantics separately because `saveGameAsync()` is asynchronous.
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
| Shared web bridge prototype | Debug-only Android experiment; separate from the production web app | Run focused tests and APK build, exercise lifecycle/offline/bridge behavior on device, complete Windows host spike |
| Shared web theme foundation | Reusable CSS token foundation exists; the production Play bundle currently has its own more advanced styling | Unify the production bundle with shared tokens in a cohesive visual-system pass; validate contrast, scaling, accessibility, safe areas and host behavior |
| Play / Worlds / Rewards / Journey | Shared HTML/CSS/JS route wired behind `shared_web_play`; bundled flag enabled; source integration only | Android runtime/IME/back/lifecycle/accessibility checks, plus parity for every action and state |
| Play: economy/progression/boss/event sections | Compose | Inventory individual sections and callbacks before deciding whether they remain Compose temporarily or move with the route |
| Upgrade, Shop, More, settings, save management, minigames and remaining progression destinations | Migration gap remains; the current web bundle does not yet replace these full destinations | Migrate as coherent HTML screens with authoritative Kotlin bridge actions; preserve Compose fallback until verified |
| Native/system surfaces | Compose or platform integrations | Keep native where justified; document the boundary rather than forcing them into HTML |

## Evidence rules

Track source implementation, focused tests, CI, Android runtime behavior, Windows runtime behavior, screenshot comparison, save compatibility and installer validation separately. A source audit or successful build alone does not mark a migration row complete.


## Shared idle-game Play bundle: responsive and host portability

The current implementation slice adds the canonical bundle under `app/src/main/assets/web-play/` (`index.html`, `styles.css`, `app.js`) and the Android host adapter in `ProductionWebPlayScreen.kt`. It is a visual/host foundation; the existing production Compose Play route is not switched over yet.

### Portability implemented in the shared frontend

- Mobile-first single-column layout remains the default for narrow touch screens.
- At 720 CSS pixels and above, the same semantic content reflows into a two-column layout for tablets, desktop windows and wider embedded views; the layout expands again at 1100 pixels.
- Extra-narrow screens receive a compact stat row and a full-width submit control rather than horizontal overflow.
- Keyboard focus is visible, mouse hover is only enabled for hover-capable fine pointers, and buttons use a touch-friendly interaction hint.
- The viewport no longer disables user zoom. Safe-area padding and reduced-motion preferences remain supported.
- The frontend uses local/system font fallbacks and bundled CSS/JS only; no remote font/CDN dependency or network connection is introduced.

### Host compatibility status (do not overclaim)

| Host | Shared bundle fit | Host adapter status | Verification |
|---|---|---|---|
| Android WebView | Same canonical bundle | Source implementation in `ProductionWebPlayScreen.kt` | Build/device, IME, lifecycle and bridge runtime remain unverified |
| Windows desktop | Same bundle is designed to reflow in a resizable window | A maintained WebView2/Kotlin bridge adapter is still required | Not implemented or runtime-verified |
| Other WebView-capable platforms | Frontend can be reused subject to engine feature support | Each platform needs a documented, allow-listed host bridge and lifecycle adapter | Not implemented or verified |

The HTML bundle alone does not make Windows or other platforms supported. Platform hosts must implement the same versioned bridge semantics, exact local-origin/page checks, bounded message validation, duplicate-request handling and authoritative Kotlin actions. Do not enable player-facing production routing until the Windows host choice, bridge parity, accessibility/input behavior, save compatibility and runtime evidence are complete.
