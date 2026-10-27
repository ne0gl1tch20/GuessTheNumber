# Unified Frontend and Runtime Plan: Vue + Canvas + Jarrlyy UI SDK + HTML + C++ + Kotlin + Lua + JavaScript

**Status (2026-10-27):** Phase 0 source audit completed and recorded in [the audit report](VUE_CANVAS_JARRLYY_STACK_AUDIT.md). Vue/Canvas integration and runtime verification are still pending; this plan remains the execution authority.
**Target repository:** `ne0gl1tch20/GuessTheNumber`
**Branch policy for this task:** Commit the plan directly to `master`, as requested.
**Architecture rule:** Unite the technologies through explicit boundaries, not by making every language implement the same responsibilities.

## Goal

Evolve GuessTheNumber into a cohesive, maintainable game where Vue manages reactive UI composition, the Jarrlyy UI SDK supplies reusable design-system components, HTML/CSS provide semantic structure and styling, Canvas renders optional custom visuals, JavaScript coordinates presentation, Kotlin/KMP remains authoritative for game state and persistence, Lua supplies sandboxed activity scripting, and C++ is used only for demonstrated native/performance needs.

The existing shared HTML/CSS/JavaScript player interface and Kotlin authority remain the foundation. Vue and Canvas are proposed additions that must be justified by an audit and introduced incrementally. This is not permission for a big-bang rewrite.

## Responsibilities: one owner per job

| Technology | Intended responsibility | Must not become |
|---|---|---|
| HTML | Semantic screen structure, forms, accessible controls and content | A second gameplay engine |
| CSS | Theme tokens, responsive layout, typography, transitions and reduced-motion support | Duplicated per-screen styling systems |
| Vue 3 | Reactive components, screen composition, UI state, component lifecycle and presentation routing | The authority for currency, rewards, saves or game rules |
| Jarrlyy UI SDK | Shared visual language, reusable controls, dialogs, cards, feedback, tokens and UI utilities | A second framework that competes with Vue; adapt or wrap existing SDK APIs instead |
| Canvas 2D | Optional particles, backgrounds, visualizations and effects that benefit from custom drawing | The default renderer for ordinary buttons, text, menus or accessible controls |
| JavaScript | UI orchestration, typed bridge client, localization presentation and Canvas coordination | Trusted save/reward logic or unrestricted native access |
| Kotlin/KMP | Authoritative gameplay transitions, validation, progression, economy, rewards, save transactions/migrations/recovery and shared contracts | A duplicate renderer for all player-facing screens |
| Lua | Bundled, validated, instruction-budgeted activity/event scripts using narrow host APIs | Direct filesystem, OS, network, reflection, arbitrary native calls or authority over rewards/saves |
| C++ | Optional measured hot paths or platform-native algorithms through a narrow JNI/NDK boundary, only when profiling proves a need | A mandatory layer, a second game engine or a reason to move ordinary UI logic out of Kotlin/JS |

## Target architecture

```text
HTML + CSS
    └── Vue 3 screen/component tree
          ├── Jarrlyy UI SDK adapters and reusable components
          ├── Canvas renderer for optional visual effects
          ├── JavaScript localization and presentation state
          └── Versioned, allowlisted host bridge
                    └── Kotlin/KMP game authority
                          ├── State, rules, economy and rewards
                          ├── Save/recovery and platform adapters
                          ├── Sandboxed Lua runtime + narrow activity API
                          └── Optional C++ module via JNI only if justified
```

Canvas is a visual layer mounted inside a Vue component. Vue controls its lifecycle and supplies presentation inputs; Canvas does not own the game state. HTML remains responsible for interactive controls and accessible text. Kotlin validates all state-changing actions. Lua and C++ never bypass that authority.

## Implementation phases

### Phase 0 — Audit and baseline

- Inspect the current `app/src/main/assets/web-play/` bundle, Jarrlyy UI SDK APIs, Gradle configuration, Kotlin/KMP bridge, LuaJ integration, existing native libraries and test workflows.
- Record the current frontend entry point, asset loading, localization, navigation, dialogs, animation patterns and dependency/build constraints.
- Determine whether Vue already exists in the repository. Do not add a second build pipeline or replace existing code before understanding it.
- Establish baseline startup time, packaged bundle size, memory, input responsiveness and test results where measurements can actually be collected.
- Inventory existing C/C++ or JNI code and confirm its purpose. If no measured native bottleneck exists, keep C++ optional and defer it.

**Gate:** Publish the audit findings and a minimal dependency/build proposal before migrating screens.

### Phase 1 — Vue and Jarrlyy UI SDK foundation

- Add Vue 3 using a pinned, reproducible local build dependency; bundle all required runtime assets locally so offline play never depends on a CDN.
- Select a build approach compatible with the repository's current Android asset pipeline and CI. Avoid introducing Vite or another tool until its fit is confirmed.
- Create a thin adapter layer so Vue components can reuse the existing Jarrlyy UI SDK without duplicating tokens, components or styles.
- Preserve the SDK's current consumers while wrappers/adapters are introduced; do not break existing HTML/JS screens in one sweep.
- Define typed component props/events, shared theme tokens, localization behavior, keyboard/focus support, touch targets, reduced-motion support and consistent error/loading/empty states.
- Add lint/build checks and component/bridge tests using the repository's supported tooling.

**Gate:** A small local-only Vue screen renders inside the existing WebView, uses SDK styling, works offline and passes accessibility/input smoke checks.

### Phase 2 — Canvas effects module

- Add a reusable Vue-managed Canvas component for selected effects such as particles, confetti or animated backgrounds.
- Keep gameplay controls, text, menus, dialogs and settings as semantic HTML styled by the SDK.
- Scale the drawing buffer correctly for device pixel ratio and resizing; cap particle counts and frame work on low-end devices.
- Pause or reduce animation when hidden, respect reduced-motion settings, handle context/lifecycle cleanup and avoid leaking animation loops.
- Provide a graceful CSS/no-canvas fallback. Effects must never block gameplay or alter authoritative game outcomes.
- Add deterministic visual tests where practical and manual performance checks on available Android hardware.

**Gate:** Canvas effects can be enabled/disabled independently, clean up correctly and show no meaningful input or startup regression.

### Phase 3 — Production bridge and Kotlin authority

- Keep the existing Kotlin-authoritative architecture and evolve the current bridge contract only where Vue requires it.
- Use versioned, allowlisted request/response messages with strict schema validation, bounded payload sizes, explicit errors and duplicate/stale-request handling.
- Kotlin validates and applies guesses, purchases, progression, reward claims, settings with persistence effects and save operations. Return updated view state rather than trusting values sent by JavaScript.
- Never expose arbitrary reflection, class names, filesystem access, shell commands, JNI entry points or unrestricted network access to the web layer.
- Preserve save compatibility, offline startup, recovery, localization, audio and platform integrations.
- Add contract tests for malformed messages, unknown actions, invalid fields, duplicate requests, failed persistence and unauthorized state mutations.

**Gate:** Existing saves and gameplay remain correct; invalid bridge calls cannot change balances, progression, rewards or save data.

### Phase 4 — Lua integration

- Keep the current LuaJ sandbox and existing script/API contracts as the baseline.
- Expose only narrow, typed activity operations through Kotlin-owned host APIs; scripts request actions, while Kotlin validates eligibility and computes/persists outcomes.
- Preserve instruction budgets and deny Android/Java bridge access, filesystem, OS, network, package loading, unrestricted coroutines and arbitrary native calls.
- Validate script manifests and compatibility before loading; keep offline bundled scripts working without remote services.
- Test malformed scripts, budget exhaustion, missing optional assets/SFX, invalid requests and attempted privilege escalation.
- Do not make Vue or Canvas a privileged Lua execution environment.

**Gate:** Existing Lua activities continue to work and sandbox tests prove scripts cannot bypass Kotlin authority.

### Phase 5 — C++ decision gate

- First profile real gameplay on supported hardware and identify a specific CPU-heavy algorithm that Kotlin/JS cannot meet its performance target for.
- If no evidence-backed bottleneck exists, do not add C++ just for architectural symmetry.
- If justified, isolate the native module behind a small Kotlin/JNI API with explicit input validation, bounded memory use and deterministic results where applicable.
- Keep a Kotlin fallback, handle native-library loading failures gracefully and avoid passing raw save structures or privileged objects across JNI.
- Build and test supported ABI variants in CI, inspect package-size impact and document native dependency/license obligations.

**Gate:** Demonstrate a repeatable performance improvement that outweighs startup, maintenance, packaging and debugging costs. Otherwise record C++ as deferred.

### Phase 6 — Incremental screen migration

- Migrate one existing screen or feature slice at a time, starting with a low-risk surface after the Vue foundation passes.
- Preserve existing routes and a rollback path until each migrated slice passes acceptance checks.
- Reuse SDK components and existing Kotlin actions; do not rewrite game rules in Vue, Canvas, Lua or C++.
- Verify loading, error, locked/unlocked, empty, tutorial, localization-change, navigation and recovery states.
- Keep Compose/native UI for system-owned surfaces and platform integration.
- Update the migration matrix and changelog with actual evidence; do not mark platform parity complete based on source changes alone.

### Phase 7 — Release hardening

- Run unit, common, bridge, Lua sandbox, frontend build and available Android/Windows CI checks.
- Verify offline launch, local asset loading, existing-save compatibility, app lifecycle, accessibility, input methods and native bridge restrictions.
- Compare startup, memory, frame responsiveness and packaged size with the Phase 0 baseline.
- Inspect the final dependency tree, licenses, security advisories and bundle output.
- Keep unverified device/Windows checks explicitly pending; do not claim runtime verification without a real run.

## Acceptance checklist

- [ ] Vue is locally bundled and offline-safe.
- [ ] Jarrlyy UI SDK remains the shared design system, with no duplicate competing component library.
- [ ] Canvas is limited to suitable custom visuals and has cleanup, resize, reduced-motion and fallback behavior.
- [ ] HTML controls remain semantic, keyboard-accessible and screen-reader-friendly.
- [ ] JavaScript/Vue is presentation-only; Kotlin/KMP remains authoritative for gameplay, economy, rewards and saves.
- [ ] Lua stays sandboxed and cannot invoke privileged APIs directly.
- [ ] C++ is added only if profiling proves value; Kotlin fallback and ABI coverage exist if added.
- [ ] Existing saves, offline play, localization, recovery and current platform integrations remain compatible.
- [ ] Automated checks and actual runtime checks are reported separately.
- [ ] Performance and package-size changes are measured against a baseline.
- [ ] Each migration slice has a rollback path and documented acceptance evidence.

## Non-goals

- Rewriting the entire game in Vue or JavaScript.
- Rendering every control on Canvas.
- Replacing Kotlin save/reward authority with Lua, C++ or frontend state.
- Adding a mandatory C++ layer without a measured need.
- Introducing remote runtime dependencies, ads, analytics or accounts.
- Claiming Windows or device parity before the relevant checks run.

## Current execution status

- [x] Phase 0 source audit written to `VUE_CANVAS_JARRLYY_STACK_AUDIT.md`.
- [x] Initial Phase 1 foundation: Vue 3.5.22 production runtime is bundled locally at `app/src/main/assets/libraries/vue/`; HTML loads it before the isolated effects module.
- [x] First isolated Canvas slice: correct-guess celebrations use a Vue-managed Canvas with DPR capped at 2, reduced-motion support, hidden-page cleanup and a legacy fallback.
- [x] Initial Vue/Jarrlyy adapter added with SDK toast/dialog reuse, an SDK-styled Vue button component and namespaced host events.
- [x] Automated lifecycle/adapter checks added to the shared frontend CI workflow.
- [ ] Phase 1 acceptance gate: confirm CI passes, verify no regression, and complete keyboard/focus/accessibility checks before migrating ordinary UI controls.
- [ ] Phase 0 runtime/build/performance baseline and focused Lua sandbox audit.
- [ ] Phases 3–7: implement incrementally and record real test evidence. No Android/Windows runtime acceptance is claimed yet.

## References within this repository

- [Unified stack source audit](VUE_CANVAS_JARRLYY_STACK_AUDIT.md)
- [Shared Web UI architecture](SHARED_WEB_UI_ARCHITECTURE.md)
- [Shared Web UI migration matrix](SHARED_WEB_UI_MIGRATION_MATRIX.md)
- [Gameplay bridge contract](SHARED_WEB_UI_GAMEPLAY_BRIDGE_CONTRACT.md)
- [Lua scripting plan](LUA_SCRIPTING_PLAN.md)
- [Lua API design](LUA_API_DESIGN.md)
- [Save schema and migration notes](SAVE_SCHEMA_AND_MIGRATION.md)

**Decision summary:** Unite the stack through clear ownership and small, testable integration steps. Vue composes the UI, Jarrlyy UI SDK defines the visual system, HTML/CSS keep controls accessible, Canvas handles optional drawing, JavaScript coordinates presentation, Kotlin/KMP owns truth, Lua runs sandboxed activities, and C++ stays optional until evidence says otherwise.
