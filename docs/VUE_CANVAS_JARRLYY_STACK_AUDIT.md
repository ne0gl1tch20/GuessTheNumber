# Vue + Canvas + Jarrlyy Unified Stack — Phase 0 Audit

**Audit date:** 2026-10-27  
**Branch reviewed:** `master`  
**Status:** Source inspection completed; runtime/build baseline not yet measured.

## Findings

### Existing web UI and asset pipeline

- The production player UI already lives in `app/src/main/assets/web-play/`; the current `index.html`, `app.js` and `styles.css` are large, established files (approximately 90 KB, 118 KB and 103 KB respectively at audit time).
- The HTML loads the local Jarrlyy UI SDK core, advanced components and host adapter, then the existing trait extension host, `app.js` and `menu-studio.js`.
- The page already uses a restrictive Content Security Policy: scripts/styles are same-origin only, `connect-src 'none'`, and object/embed content is disabled. Any integration must preserve this posture.
- Fonts, icons, locale files and the SDK are already packaged locally. The manifest generator includes `web-play`, `fonts`, `locales` and `libraries`, so a vendored Vue runtime and new local modules can be hash-covered without introducing a runtime CDN.
- Gradle already includes a generated web-play assets directory, but the inspected build configuration does not establish a frontend npm/Vite build pipeline. Adding a mandatory Node/npm step to Android builds would be a compatibility and offline-build risk.

### Jarrlyy UI SDK

- The SDK is intentionally framework-free and host-neutral, using namespaced `jui-` classes, declarative `data-jui-*` attributes and local JS/CSS files.
- Vue should consume/adapt the existing SDK rather than replace it. Existing screens must continue to work during migration.
- Do not move existing global styles or SDK initialization into a Vue-only lifecycle until the impact is tested.

### Native bridge and gameplay authority

- The Android production host already uses `WebViewAssetLoader` and AndroidX WebView messaging APIs. This is a better foundation than introducing a new unrestricted `JavascriptInterface`.
- The separate gameplay bridge contract document is marked proposed, so its documented schema must not be mistaken for proof that every production action already implements it.
- The frontend includes a preview host for standalone/demo behavior. Preview state must remain clearly separated from production host state and must never be mistaken for persisted gameplay.

### Runtime and test constraints

- Source inspection confirms no `package.json` or local Vue runtime through the inspected search results. Vue is therefore **not yet integrated**.
- The audit did not execute Gradle, frontend tests, Android runtime checks or desktop runtime checks. No performance baseline is claimed.
- C++ remains deferred until repeatable profiling demonstrates a bottleneck. This audit found no evidence that adding a native C++ layer is currently necessary.
- Lua sandbox implementation and its current tests still need a focused source/test audit before changing that subsystem.

## Implementation decisions

1. Preserve the current production HTML/JS route and existing SDK consumers while adding the new stack incrementally.
2. Vendor and pin Vue locally; do not use a CDN or require a network connection at runtime.
3. Prefer a reproducible, optional frontend build task that does not make ordinary Android/desktop builds depend on network access or a globally installed Node toolchain.
4. Begin Vue integration with a small, low-risk presentation surface, not a rewrite of the entire 118 KB application script.
5. Mount Canvas only for optional effects. Keep controls and text in semantic HTML and honor reduced-motion settings.
6. Keep all authoritative state changes in Kotlin. Reuse the existing WebView messaging boundary and tighten contracts only after comparing them with actual host handlers.
7. Do not add C++ until profiling, and do not mark Windows/device parity or runtime acceptance complete without executing the relevant checks.

## Gate result

**Phase 0 source-audit gate: passed with follow-up work.** The architecture and current integration points are identified. The baseline measurement, focused Lua audit, and runtime/build checks remain open. Phase 1 must add a real local Vue runtime and a verified integration path before Vue can be called integrated.
