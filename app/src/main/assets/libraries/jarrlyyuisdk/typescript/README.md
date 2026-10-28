# Jarrlyy UI SDK TypeScript source

This directory contains the typed source for the framework-free Jarrlyy UI SDK. It lives inside GuessTheNumber and has no runtime framework dependency.

## Build

From this directory:

```sh
npm install
npm run typecheck
npm run build
```

The build emits the utility ES modules and declaration files into `dist/`, compiles the **core UI runtime** from `runtime/jarrlyy-ui.ts` to the classic browser script `../components/jarrlyy-ui.js`, and compiles the advanced component runtime from `runtime/jarrlyy-ui-advanced.ts` to `../components/jarrlyy-ui-advanced.js`. The classic-script outputs preserve the existing `window.JarrlyyUI`, `window.JarrlyyUIMotion` and `window.JarrlyyUIAdvanced` contracts used by the WebView.

The core migration is staged: the public core and motion API contracts are declared in TypeScript, and the complete existing behavior layer is now authored as TypeScript. Some legacy internal callback and DOM paths still need tighter annotations. Keep changes in the TypeScript source and rebuild the classic scripts instead of editing generated runtime output by hand.

CI type-checks the reusable modules, builds all three outputs, and runs runtime tests against the generated scripts. The game remains the owner of game state, saves, rewards, localization and native bridge behavior.

## Public API

- Core: `init`, `enhance`, `toast`, `dialog`, `tour`, `createKeyButton`.
- Motion: `count`, `progress`, `floatText`, `init`.
- Host integration: `mountHost(options?, window?, document?)`, `setHostTheme(root, "light" | "dark" | "system")`, `isJarrlyyEventName(name)`.
- Advanced UI: `setLoading(button, loading, label?, document?)`, `setProgress(element, value)`.
- DOM helpers: `humanize`, `setTextContent`, `prefersReducedMotion`, `setIconButtonLabel`.

Keep the SDK focused on reusable presentation and accessible interactions. Do not move game rules, save serialization, rewards or unrestricted native bridge access into this library.
