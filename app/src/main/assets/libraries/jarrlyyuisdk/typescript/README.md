# Jarrlyy UI SDK TypeScript source

This directory is the typed source layer for the reusable Jarrlyy UI SDK. It lives inside GuessTheNumber and has no framework dependency.

## Build

From this directory:

```sh
npm install
npm run typecheck
npm run build
```

The module build emits ES2020 JavaScript modules and declaration files into `dist/`. The runtime build compiles `runtime/jarrlyy-ui-advanced.ts` into the classic browser script at `../components/jarrlyy-ui-advanced.js`. That generated file keeps the existing `window.JarrlyyUIAdvanced` API so the WebView can continue loading it with a normal script tag. Commit the generated runtime asset together with source changes.

CI builds the modules and classic runtime before running the SDK runtime tests, so those tests exercise the TypeScript-generated component layer.

## Public API

Typed host integration and interactive helpers are also available:

- `mountHost(options?, window?, document?)` initializes available SDK layers and returns theme/event helpers.
- `setHostTheme(root, "light" | "dark" | "system")` applies or clears a theme override.
- `isJarrlyyEventName(name)` validates namespaced `jui:*` events.
- `setLoading(button, loading, label?, document?)` preserves and restores a button's label and disabled state.
- `setProgress(element, value)` clamps progress to 0–100 and updates ARIA progress attributes.

Import module helpers from the generated `dist/index.js`:

- `humanize(value, fallback?)`
- `setTextContent(node, value, fallback?)`
- `prefersReducedMotion()`
- `setIconButtonLabel(button, iconId, label)`

Keep game state, saves, rewards, localization and native bridge logic out of this library. The TypeScript component runtime owns reusable UI behavior only; host applications continue to own routing and game rules.
