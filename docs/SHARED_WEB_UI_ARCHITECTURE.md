# Shared Web UI and Native Host Architecture

**Decision:** Chosen architecture direction; documentation-only decision. No application runtime has been migrated.
**Owner decision:** Use one HTML/CSS/JavaScript frontend for shared player-facing screens across Android and Windows, retain Kotlin/KMP as the trusted game authority, and keep Compose for native host/system surfaces and desktop-shell integration.
**Implementation status (source review, 2026-10-17):** Android still enters through `MainActivity` + Compose `setContent` and `NavGraph`; Windows still uses a Compose Desktop `Window`. The build files contain no WebView/Cordova integration, and no bundled app HTML/CSS/JS frontend exists yet. The existing shared module contains selected Kotlin rules/contracts and a controlled Compose guessing screen. These are current facts, not a blocker to the chosen direction.
**Reviewed:** 2026-10-17

## Goal

Make GuessTheNumber look and behave like the same game across Android and Windows, with future platforms able to reuse the same screens and interactions. Share the actual frontend bundle rather than separately reimplementing every screen in Compose on each platform.

This chosen direction changes the preferred **UI delivery layer**, not the game's product requirements. Preserve offline play, existing save compatibility, Kotlin-owned progression/rewards, localization, recovery, and the no-ads/no-analytics/no-accounts rule.

## Proposed stack

- **HTML:** semantic screen structure and accessible controls.
- **CSS:** shared design tokens, layout, responsive behavior, typography, colors, animations and reduced-motion support.
- **JavaScript:** screen composition, navigation presentation, input handling, local presentation state and calls to the native/game bridge.
- **Kotlin/KMP:** authoritative game rules, progression, economy, reward validation, save transactions/migrations, and platform-neutral contracts where existing KMP support is appropriate.
- **Compose:** native host-owned UI where it provides value, including platform-specific settings/system surfaces and desktop shell behavior. Do not recreate the shared game screens in Compose merely to say every platform uses Compose.
- **Cordova:** mobile WebView packaging and plugin bridge on supported Cordova platforms, starting with Android if a prototype proves the integration.
- **Windows host:** keep the Kotlin/Compose Desktop application and host the same packaged web frontend through a maintained Windows WebView integration (for example WebView2 or a justified alternative). Do not depend on Cordova's legacy Windows platform, which is no longer actively maintained.
- **Dependencies:** add only what a concrete requirement needs. Prefer a small dependency set, pinned versions, licenses and maintenance checks, and explicit bundle-size/build impact review.

## Platform map

| Platform | Shared player UI | Native host and system integration |
|---|---|---|
| Android | Shared HTML/CSS/JS bundle inside the chosen WebView host | Kotlin game authority and Android adapters; Cordova plugins may provide a documented JS-to-native bridge |
| Windows | The exact same web bundle | Kotlin/Compose Desktop shell and a maintained embedded WebView integration; implement the same bridge contract without pretending Cordova Windows is supported |
| Future iOS | Reuse the same web bundle if the platform is approved | Cordova iOS or another supported WebView host plus a native adapter to Kotlin/KMP where feasible; validate separately before promising parity |
| Browser/Electron, if later approved | Reuse the same web bundle | A dedicated adapter; native-only features must degrade clearly and never silently alter game rules |

## Authority and bridge contract

The web frontend is a presentation client, not a second game engine.

1. Kotlin remains the source of truth for Money, Prestige, Ultra, Nebula, eligibility, achievements, progression, reward amounts, save slots, migration and recovery.
2. JavaScript sends typed, versioned requests such as read-current-view-state, submit-guess, purchase-upgrade, claim-reward, change-setting and request-save-status.
3. Kotlin validates the request and current game state, applies the action once, persists as required, and returns a typed result plus the updated view state.
4. Never accept a reward amount, currency balance, eligibility result, save path, arbitrary class name or native method name from the web UI as trusted authority.
5. Keep the bridge allowlisted, schema-validated, size-bounded, asynchronous where appropriate and explicit about errors/cancellation. No arbitrary JavaScript-to-reflection, filesystem, shell, JNI or unrestricted network access.
6. Native APIs are exposed only through narrow platform adapters. Cordova plugin calls and the Windows WebView bridge implement the same documented frontend-facing contract.
7. Keep existing Kotlin/Lua/LiveOps boundaries intact. The web layer must not weaken the Lua sandbox or allow remotely supplied content to execute privileged native actions.

## Visual consistency rules

- One canonical frontend bundle and one set of design tokens/assets for shared screens.
- One navigation/screen model in JavaScript for the shared game UI; platform hosts own only genuinely native/system surfaces.
- Support touch, mouse, keyboard, focus, window resizing, text scaling and reduced motion without forking game behavior.
- Platform-specific adapters may expose different capabilities, but the shared UI must explain unavailable capabilities rather than silently presenting nonfunctional controls.
- Screenshot and interaction checks must compare the same screen/state across supported hosts. A shared bundle alone does not prove visual parity.

## Migration approach

Do not rewrite the whole app in one PR. Keep the existing app working while validating the new architecture.

### Phase 0 — Audit and prototype

- Inventory the current Compose destinations, Kotlin action/state APIs, save/recovery behavior, localization, audio, notifications, widgets, update channels and desktop integrations.
- Record which screens are candidates for the shared web UI and which are native/system-only.
- Build a small throwaway or isolated vertical-slice prototype containing one read-only state screen and one harmless state-changing action.
- Prove the Android WebView/Cordova lifecycle and plugin bridge, Windows embedded WebView lifecycle, keyboard/focus/resizing, offline asset loading, accessibility, and Kotlin-authoritative action round trip.
- Measure startup time, memory, bundle size, input latency and packaged app size on available devices.
- Decide the maintained Windows WebView dependency only after verifying licensing, maintenance, supported OS versions and distribution implications.

### Phase 1 — Define contracts and design tokens

- Write versioned request/response schemas and bridge error semantics.
- Define shared design tokens, localization key handling and accessibility expectations.
- Add contract tests for invalid payloads, duplicate requests, stale state and failed persistence.
- Do not move authoritative rules into JavaScript for convenience.

### Phase 2 — First production vertical slice

- Migrate one bounded, low-risk screen to the shared web frontend.
- Keep Kotlin responsible for all actions and persistence.
- Compare Android and Windows screenshots and action outcomes.
- Keep a reversible route back to the existing Compose screen until parity and save compatibility are demonstrated.

### Phase 3 — Migrate by feature slices

- Migrate related screens in small PRs with the existing app changelog updated for each player-facing change.
- Keep native-only surfaces in Kotlin/Compose and use a shared bridge for game actions.
- Maintain a parity matrix for each migrated screen and its loading, empty, error, locked/unlocked and recovery states.
- Avoid maintaining two active implementations of the same screen after the web version has passed its exit criteria.

### Phase 4 — Release and retire duplicate UI

- Verify save compatibility, migration/recovery, offline behavior, app lifecycle, update channels, Windows installer upgrades and Android release packaging.
- Run tests and runtime checks on actual supported hosts. CI alone is not enough to prove WebView rendering, focus, performance or native bridge behavior.
- Remove old duplicate screen code only after the replacement is verified and rollback is no longer needed.
- Keep platform-specific native code when required; do not force all UI or system integrations into HTML.

## Exit criteria before expanding beyond a prototype

- Same web assets render on Android and Windows with matching screen content and visual identity.
- The native bridge is versioned, allowlisted and tested; invalid calls cannot mutate saves or award rewards.
- Kotlin remains authoritative for game state, economy and persistence.
- Existing saves load and recover without format regressions.
- Offline startup and gameplay work without remote resources.
- Touch, keyboard, mouse, resizing, accessibility and lifecycle behavior are verified for each host.
- Performance and memory are measured against the existing app, with regressions documented and addressed.
- CI/build support, dependency licenses and maintenance owners are documented.
- The owner explicitly approves moving from prototype to production migration.

## Risks and trade-offs

- WebView rendering differs slightly across Android WebView and Windows WebView implementations; shared CSS reduces drift but does not erase platform differences.
- A native bridge creates a security and correctness boundary. It must be much narrower than unrestricted JS access to Kotlin.
- A large web rewrite can regress input, accessibility, lifecycle, performance, save behavior or offline startup. Use a vertical slice and preserve rollback.
- Cordova's Windows platform is not actively maintained. The Windows host needs its own maintained WebView integration.
- Compose remains useful for native/system surfaces and host integration, but forcing Compose to render the same game screens would duplicate the frontend and undermine the goal.
- Sharing UI does not automatically share game rules. Kotlin remains authoritative and bridge contract tests must prove that.

## Official references

- [Cordova platform support](https://cordova.apache.org/docs/en/latest/guide/support/)
- [Cordova Android plugin bridge](https://cordova.apache.org/docs/en/latest/guide/platforms/android/plugin.html)
- [Cordova plugin architecture](https://cordova.apache.org/docs/en/latest/guide/hybrid/plugins/)

## Decision status

The HTML/CSS/JavaScript shared frontend with Kotlin authority and Compose-supported native hosts is the **chosen architecture direction**. This decision locks the target, not an implementation schedule or permission to skip migration gates. It does **not** mean Cordova has been added, a WebView host has been integrated, Compose screens have been removed, the app has been converted to HTML, or any runtime parity has been tested. Prototype and production slices must still pass the documented bridge, save-compatibility, offline, accessibility, performance and cross-platform verification gates.
