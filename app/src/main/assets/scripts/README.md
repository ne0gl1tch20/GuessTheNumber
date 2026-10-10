# Reusable Lua Scripts

This folder contains the app-bundled, offline Lua catalog. `manifest.json` registers the first `number_rush` activity in `minigames/number_rush.lua`. Scripts execute through the shared LuaJ sandbox and Kotlin `LuaEngine` facade.

## Source boundary

This folder is the **offline, app-bundled source**. Remote event scripts belong in the separate planned `GuessTheNumber-LiveOps` content repository and must never be downloaded over or write into this folder. The runtime registry may resolve both sources, but only verified remote bundles may be cached in app-private storage.

The initial offline catalog stays small: one featured sample minigame plus reusable helpers. The GO button launches this activity without network access, giving the app a fallback if LiveOps is unavailable.

## Intended organization

```text
assets/scripts/
  README.md
  common/       # future reusable helpers and small utilities
  minigames/    # future small, temporary minigame entry points
  events/       # future local event scripts
  ui/           # future reusable presentation helpers
  manifest.json # versioned local script manifest
```

Keep scripts small, composable and explicit about entry points. Shared helpers must declare the minimum host API version they need. Scripts must not assume they can access arbitrary files, the network, Android APIs or save data.

Activity copy is returned as a bounded Lua activity table and rendered by Compose; text localization-key integration is a follow-up. Bundled scripts are validated against the catalog and host API, and the Lua runtime applies a source-size and instruction budget.

Offline scripts, verified LiveOps scripts, the one-tap GO flow and the debug-only Dev Settings Lua Console must all use one shared Kotlin Lua engine facade and versioned host API. This folder contains content only: no per-script engine, UI bridge, command executor or runtime dependency belongs here. Developer console commands resolve registered script IDs through the shared registry; they do not run arbitrary paths or fetch source directly.

Future remote LiveOps scripts should be distributed as verified, versioned bundles; they should not overwrite this bundled directory at runtime. Keep bundled content as the offline fallback.


## Signed LiveOps setup

The app-side client and publisher format are documented in [LIVEOPS_BUNDLE_FORMAT.md](../../../../../docs/LIVEOPS_BUNDLE_FORMAT.md). The planned external content repository must publish a signed `manifest.json` plus `scripts/*.lua`, and the app must be built with the matching RSA public key using `-PluaLiveOpsPublicKeyBase64=...`. Without that key, remote refresh deliberately fails closed and the bundled Number Rush activity still works offline.
