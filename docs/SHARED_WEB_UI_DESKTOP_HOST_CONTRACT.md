# Shared Web Play UI: Desktop and Multi-Host Contract

**Status:** Frontend responsiveness implemented on the idle-game Play bundle branch. Windows/other native hosts are not implemented by this document.
**Reviewed:** 2026-10-17
**Related:** [Shared Web UI Architecture](SHARED_WEB_UI_ARCHITECTURE.md), [Migration Matrix](SHARED_WEB_UI_MIGRATION_MATRIX.md), [Gameplay Bridge Contract](SHARED_WEB_UI_GAMEPLAY_BRIDGE_CONTRACT.md), [Windows Feature Parity Plan](WINDOWS_FEATURE_PARITY_PLAN.md)

## Purpose

The player-facing HTML/CSS/JavaScript bundle is the canonical presentation layer, not an Android-only screen mock. It must adapt to narrow phones, tablets, desktop windows and future approved WebView hosts without forking the visual design or game rules.

## Frontend requirements

- Keep the same canonical HTML/CSS/JS assets across supported hosts. Do not copy the screen into a separate Windows-only implementation.
- Use responsive CSS rather than user-agent sniffing. The bundle supports a narrow single-column layout, a two-column layout from 720 CSS pixels, and a wider desktop treatment from 1100 CSS pixels, with a compact fallback at 360 CSS pixels.
- Use semantic HTML, labeled inputs, keyboard form submission, visible `:focus-visible` states and pointer-capability-aware hover styling.
- Never disable browser zoom. Respect reduced-motion settings and avoid animation-only feedback.
- Use local/system font fallbacks or explicitly bundled licensed font assets. Never fetch fonts, scripts, images or data from a remote CDN for core offline gameplay.
- Keep safe-area padding for mobile hosts; allow scrolling and resizing in desktop hosts.
- Treat CSS breakpoints as layout hints, not host detection. Platform-specific actions must come from the bridge contract.

## Native host contract

Each host adapter must:
1. Load only the packaged local frontend from an exact allow-listed origin and page. Reject external navigation and untrusted messages.
2. Implement the same versioned request/response schema and action allow-list. Validate protocol version, request ID, message size, exact field types and numeric conversion at the host boundary.
3. Deduplicate bounded request IDs and avoid applying a gameplay action more than once for a repeated request.
4. Return correlated authoritative responses. JavaScript ignores mismatched/stale replies and never owns game rules, rewards, progression or save writes.
5. Keep platform APIs out of the page unless exposed by an explicit, minimal bridge method. No generic JavaScript-to-native method dispatcher.
6. Support keyboard/IME, mouse and touch input as available, focus recovery, window resize, lifecycle disposal, back/escape behavior, and error/offline states.
7. Preserve offline use, current save compatibility, localization and no ads/no analytics/no accounts.

## Platform status

| Platform | Host direction | Status |
|---|---|---|
| Android | Android WebView using a packaged asset loader and origin-restricted message listener | Source adapter exists on the production idle-UI branch; build/device verification still required |
| Windows | Existing Kotlin/Compose Desktop shell plus a maintained embedded WebView integration (WebView2 is a candidate pending dependency/runtime/MSI assessment) | Not implemented; do not claim feature parity |
| Future iOS or other approved platforms | Supported platform WebView plus a narrow adapter to the same bridge contract | Not implemented; platform-specific review required |

## Desktop acceptance checklist

- [ ] Window resizing at 800×600, 1280×720 and ultrawide sizes without clipping or horizontal page overflow.
- [ ] Keyboard-only use: tab order, visible focus, Enter submit, focus after errors and Escape/back behavior where relevant.
- [ ] Mouse hover affordances do not hide information or replace keyboard/touch affordances.
- [ ] Windows WebView runtime distribution, startup/offline behavior and installer/upgrade compatibility are proven.
- [ ] The exact same packaged frontend files are loaded on Android and Windows.
- [ ] Shared bridge contract tests cover valid requests, malformed messages, unknown fields/actions, wrong origin, duplicate IDs, stale responses, overflow and processing errors.
- [ ] Equivalent game outcomes, localization, accessibility and save/reload behavior are checked against Android.
- [ ] Record source implementation, CI, automated tests, runtime verification and screenshot comparison separately.

A responsive CSS layout is necessary but not sufficient for desktop compatibility. Windows remains unsupported for this surface until its host adapter and runtime checks exist.
