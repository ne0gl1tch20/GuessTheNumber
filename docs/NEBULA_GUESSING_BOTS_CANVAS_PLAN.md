# Plan: Nebula Shop Guessing Bots + 2D Canvas Arena

**Status:** Planning only. No gameplay implementation is included in this PR.
**Target repository:** `ne0gl1tch20/GuessTheNumber`
**Target branch for implementation:** `master` via a reviewed pull request
**Current plan branch:** `plan/nebula-guessing-bots-canvas`

## Goal

Let players spend Nebula in the existing Nebula Shop to buy autonomous guessing bots. Every owned bot appears in the Play screen's 2D Canvas arena and visibly types guesses, submits them, reacts to Too Low / Too High feedback, narrows its search range and celebrates correct guesses. Players can buy medium-priced upgrades that improve bot speed and strategy. The arena remains the main view, while less-used controls can live in a pull-up drawer.

This is an app feature, not just a decorative HTML animation. It must use real game state, persist purchases and upgrade levels, and ship through the Android app update process. Keep the existing offline-first behavior.

## Repository findings

- The shared player UI is under `app/src/main/assets/web-play/`; `index.html` loads the bundled Vue runtime, the Jarrlyy adapter, `vue-canvas-effects.js`, then `app.js`.
- The existing Vue/Canvas integration is a small visual-effects slice, not a full Play-screen migration. Extend it incrementally rather than rewriting the screen.
- The Nebula Shop already has catalog-driven categories including **Automation**, **Boosts** and **Cosmetics**.
- `GameState` already persists Nebula, shop purchases, upgrade levels and auto-clicker state. The inspected save path uses permissive JSON defaults but does not have a dedicated schema-version migration registry. New fields must therefore be backward-compatible and covered by old-save fixtures.
- Kotlin/KMP remains authoritative for purchases, currency, bot progression, guessing outcomes and saves. JavaScript/Vue and Canvas are presentation and input layers only.

## Player experience

1. A new **Automation** section in the Nebula Shop lists bot types and bot upgrades, with current price, owned state, effects and affordability.
2. Buying a bot atomically spends Nebula and records ownership. Failed or repeated requests cannot spend currency twice.
3. The bot appears in the Play-screen arena without requiring a manual refresh. A bot has a distinct name, color/silhouette and status.
4. Its visible typing animation displays the exact guess that the game engine receives. The engine resolves the guess and returns the real Too Low / Too High / Correct result; the bot then reacts and tries again according to its strategy.
5. Owned bots and their upgrade levels persist after app restart, save-slot switching, export/import and supported legacy-save loading.
6. A swipeable/tappable pull-up drawer (handle marked `⌃`) contains controls such as bot management, shop shortcuts, upgrades, round history and settings. It must not cover the main arena by default or trap keyboard/screen-reader users.

## Initial catalog proposal

These are balancing candidates, not final prices. Before implementation, compare them with current Nebula income and shop prices in the canonical catalog. Keep prices and effects data-driven; do not hardcode costs into UI code.

| Item | Proposed initial cost | Intended effect |
|---|---:|---|
| Starter Bot | 25 Nebula | Owns one bot with a simple valid guessing strategy |
| Smart Bot | 60 Nebula | Uses interval narrowing / binary-search strategy |
| Speed Upgrade | 35 Nebula | Shortens typing and think delays within a safe minimum |
| Logic Upgrade | 50 Nebula | Improves the bot's strategy or guess efficiency |
| Parallel Bot Upgrade | 90 Nebula | Unlocks an additional active bot slot, subject to performance limits |

Prices, caps, requirements, localization strings and upgrade formulas must be catalog-driven. Avoid promising that a bot generates passive currency unless a separately balanced and tested reward rule is approved. Bots should initially demonstrate real guessing and feedback rather than become an unbounded currency exploit.

## Technical design

### 1. Catalog and economy

- Extend the existing canonical shop catalog using its established model and validation rules; prefer adding item definitions and a narrowly scoped bot definition/catalog over another independent shop implementation.
- Use stable IDs for bot types and upgrades, explicit cost currency (`Nebula`), maximum levels, prerequisites, effects and localization keys.
- Route purchases through the existing Kotlin transaction path: validate request and affordability, calculate cost/effect, apply atomically, record transaction, persist, then emit updated view state.
- The UI must never directly subtract currency or trust a client-supplied balance, price or upgrade level.

### 2. Bot simulation and authoritative guessing

- Define a deterministic bot state model: ID/type, active status, current lower/upper bounds, current guess, attempt count, strategy, typing progress and upgrade levels.
- Kotlin/KMP owns bot strategy decisions that affect gameplay and validates/resolves each guess using the same rules as the game. The web layer requests an action through an allowlisted/versioned bridge and renders the authoritative response.
- Use explicit state transitions such as `idle → typing → submitting → too_low/too_high → thinking → typing`, and `correct → celebration → next_round`.
- Prevent duplicate submissions, stale responses, infinite loops and rewards from duplicate or invalid events. Handle the case where a target changes, the player changes mode, a save slot changes or the screen is closed mid-turn.
- Keep random strategies seedable for tests. Make sure each bot's displayed guess matches the actual guess submitted to the engine.

### 3. Vue + Canvas arena

- Add a Vue-managed Play-screen component incrementally; do not migrate the entire UI as part of the first slice.
- Use Canvas 2D for bot avatars, movement, typing indicators, speech bubbles and small effects. Keep readable guesses, feedback, buttons and status accessible in semantic HTML, with a non-Canvas fallback.
- Reuse Jarrlyy UI SDK tokens and components for the Nebula Shop cards, drawer, dialogs and controls.
- Scale for device pixel ratio, resize/orientation changes and low-end phones. Cap active bots and frame work; pause/reduce decorative animation when hidden or reduced motion is enabled. No CDN dependencies or network requirement.
- The typing effect is presentation timing only: it must never change a guess or delay authoritative validation indefinitely.

### 4. Persistence and compatibility

- Add bot ownership/upgrade fields with safe defaults compatible with existing serialized saves, unless an existing field/catalog structure can represent the data cleanly without ambiguous semantics.
- Inspect current `GameState`, save validation, export/import and slot-switch paths before choosing the exact representation.
- Add fixtures for legacy saves with no bot fields and round-trip tests for owned bots and upgrades. Preserve existing balances, purchases, backup recovery and slot data.
- Do not introduce a broad save-schema migration framework as incidental scope. If inspection reveals that a semantic migration is required, stop and document it separately before changing save format.

### 5. Localization and accessibility

- Add localized names, descriptions, price/effect labels, bot status and feedback strings to the canonical English catalog and all supported locale files, following the repository's existing localization workflow.
- Keep keyboard navigation, screen-reader descriptions, visible status text, focus behavior, reduced motion and touch targets functional with Canvas disabled.

## Delivery phases and gates

### Phase 0 — Baseline audit (before code)
- Inspect canonical shop/catalog models and JSON, purchase transaction APIs, bot/auto-clicker logic, Kotlin↔web bridge, save validation/migration, localization tooling and CI workflows.
- Record actual current Nebula prices/income and select costs that are affordable but not trivial.
- Run existing HTML/CSS/JS, Android validation and Windows Desktop CI to establish baseline.

**Gate:** implementation design identifies the exact authoritative files and bridge messages; no parallel economy or duplicate shop system.

### Phase 1 — Purchase + persistence vertical slice
- Add one starter bot to the canonical Automation catalog.
- Implement atomic Nebula purchase, ownership persistence, localization and legacy-save round-trip tests.
- Add unit tests for insufficient balance, duplicate requests, save/reload, export/import and slot switching.

**Gate:** buying one bot is real and persistent before Canvas work begins.

### Phase 2 — Real bot guessing
- Implement one bot strategy with bounded retries, deterministic tests and explicit handling of target changes and invalid states.
- Connect it through the allowlisted bridge to Kotlin-authoritative guess resolution.
- Verify displayed guess, submitted guess, feedback and reward are consistent; test duplicate/stale events.

**Gate:** bot can complete real rounds without desynchronizing the player's game or bypassing economy rules.

### Phase 3 — Canvas arena and drawer
- Add Vue-managed Canvas rendering for owned bots and visible typing/feedback/celebration.
- Add accessible HTML status text and fallback, responsive drawer behavior and reduced-motion/lifecycle handling.
- Test multiple bots, orientation changes, screen navigation and background/foreground transitions.

**Gate:** visuals faithfully represent authoritative state, no clipped popups/drawer, no runaway animation loops and no noticeable input blocking.

### Phase 4 — More bots and upgrades
- Add Smart Bot, Speed, Logic and Parallel Bot upgrades after the first bot passes the gates.
- Validate price curves, caps, prerequisites and performance. Tune prices against actual progression rates.

**Gate:** all purchase and upgrade effects are testable, balanced and persisted.

### Phase 5 — Release validation
- Run all relevant CI: HTML/CSS/JS tests, Android unit/validation and debug APK build, Windows Desktop CI, save compatibility tests, catalog/localization validation and static/security checks.
- Add regression tests for Canvas lifecycle, bot strategy, bridge validation, atomic purchases and no double rewards.
- Inspect generated APK/assets to confirm Vue/Canvas resources are bundled locally.
- Manually validate on Android when a physical device is available; if not, report device testing as pending rather than claiming it passed.

**Release gate:** PR checks pass, no unexplained failures, save compatibility is proven, and the owner reviews the implementation PR. Do not merge automatically.

## Out of scope for the first release

- Online bot competitions, accounts, analytics or cloud requirements.
- Real-money purchases, ads or paid APIs.
- Lua/C++ bot logic unless a measured need justifies it.
- Full-screen Vue migration or unrelated UI redesign.
- Bots secretly generating unbounded passive rewards.
- Merging implementation before the owner has reviewed the PR and checks.

## Acceptance checklist

- [ ] Nebula Shop shows data-driven bot and upgrade cards with correct affordability.
- [ ] Purchase atomically deducts Nebula once and survives restart/save-slot operations.
- [ ] A purchased bot appears in the arena immediately.
- [ ] The bot visibly types the exact guess it submits.
- [ ] Too Low / Too High / Correct feedback matches the authoritative game engine.
- [ ] Bot strategy is bounded, deterministic in tests and cannot duplicate rewards.
- [ ] Upgrade effects change actual bot behavior, not only labels or animations.
- [ ] Old saves load unchanged; bot state survives save/reload and export/import.
- [ ] Offline operation, all supported locales, reduced motion and accessible fallback work.
- [ ] CI passes for HTML/CSS/JS, Android and Windows.
- [ ] Manual Android validation is recorded honestly before release.
