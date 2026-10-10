# Reusable Lua Scripts

**Planning directory only.** This folder is reserved for future bundled scripts after the Lua stability gate is passed. No Lua runtime or script execution is enabled by this README.

## Source boundary

This folder is the **offline, app-bundled source**. Remote event scripts belong in the separate planned `GuessTheNumber-LiveOps` content repository and must never be downloaded over or write into this folder. The runtime registry may resolve both sources, but only verified remote bundles may be cached in app-private storage.

The initial offline catalog should stay small: one featured sample minigame plus reusable helpers. This lets the planned GO button work without network access and gives the app a fallback if LiveOps is unavailable.

## Intended organization

```text
assets/scripts/
  README.md
  common/       # future reusable helpers and small utilities
  minigames/    # future small, temporary minigame entry points
  events/       # future local event scripts
  ui/           # future reusable presentation helpers
  manifest.json # future versioned local script manifest
```

Keep scripts small, composable and explicit about entry points. Shared helpers must declare the minimum host API version they need. Scripts must not assume they can access arbitrary files, the network, Android APIs or save data.

All text uses localization keys. Assets are referenced by logical IDs, not file paths. Every script is checked for syntax, schema, API compatibility and allowed capabilities before it can run.

Offline scripts, verified LiveOps scripts, the one-tap GO flow and the debug-only Dev Settings Lua Console must all use one shared Kotlin Lua engine facade and versioned host API. This folder contains content only: no per-script engine, UI bridge, command executor or runtime dependency belongs here. Developer console commands resolve registered script IDs through the shared registry; they do not run arbitrary paths or fetch source directly.

Future remote LiveOps scripts should be distributed as verified, versioned bundles; they should not overwrite this bundled directory at runtime. Keep bundled content as the offline fallback.
