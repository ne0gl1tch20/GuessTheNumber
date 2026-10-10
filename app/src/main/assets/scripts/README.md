# Reusable Lua Scripts

**Planning directory only.** This folder is reserved for future bundled scripts after the Lua stability gate is passed. No Lua runtime or script execution is enabled by this README.

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

Future remote LiveOps scripts should be distributed as verified, versioned bundles; they should not overwrite this bundled directory at runtime. Keep bundled content as the offline fallback.
