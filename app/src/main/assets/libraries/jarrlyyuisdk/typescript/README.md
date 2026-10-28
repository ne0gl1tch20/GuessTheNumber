# Jarrlyy UI SDK TypeScript source

This directory is the typed source layer for the reusable Jarrlyy UI SDK. It lives inside GuessTheNumber for now and has no framework dependency.

## Build

From this directory:

```sh
npm install
npm run typecheck
npm run build
```

The build emits ES2020 JavaScript modules and declaration files into `dist/`. Generated output is not committed. CI installs the pinned TypeScript compiler, type-checks the source and builds it.

## Public API

Typed host integration and interactive helpers are also available:

- `mountHost(options?, window?, document?)` initializes available SDK layers and returns theme/event helpers.
- `setHostTheme(root, "light" | "dark" | "system")` applies or clears a theme override.
- `isJarrlyyEventName(name)` validates namespaced `jui:*` events.
- `setLoading(button, loading, label?, document?)` preserves and restores a button's label and disabled state.
- `setProgress(element, value)` clamps progress to 0–100 and updates ARIA progress attributes.

These typed implementations are being brought under tests before replacing their existing browser-global implementations. This keeps current app behavior stable during the migration.

Import from the generated `dist/index.js`:

- `humanize(value, fallback?)`
- `setTextContent(node, value, fallback?)`
- `prefersReducedMotion()`
- `setIconButtonLabel(button, iconId, label)`

The existing WebView script API remains in place during this initial TypeScript foundation phase. Do not delete or replace the current runtime scripts until the compiled module has dedicated integration tests and a compatibility plan. Keep game state, saves, rewards, localization and native bridge logic out of this library.
