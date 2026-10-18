# Guess The Number Web UI bridge

The player-facing web UI uses a strict one-way authority model:

```text
HTML/CSS/JavaScript
        │
        │ WebMessage: request
        ▼
Kotlin WebView host / bridge
        │
        ├── GameEngine / persisted GameState
        │
        └── Lua scripting layer (when a scripted activity needs it)
        │
        ▼
Kotlin validates and applies the result
        │
        │ WebMessage: state response
        ▼
HTML/CSS/JavaScript
```

## Rules

- JavaScript owns presentation and user interaction only.
- Kotlin remains the authoritative owner of game state, progression, economy, validation, and saves.
- Lua is a constrained content/scripting layer behind Kotlin. Lua does not directly mutate persisted game state and does not communicate with JavaScript directly.
- JavaScript sends versioned bridge requests such as `read_state` and `submit_guess` through `gtnHost.postMessage(...)`.
- Kotlin validates the request with `WebUiBridgeProtocol`, executes the requested game action, then replies with a versioned state response through `JavaScriptReplyProxy`.
- The production web UI installs `gtnHost.onmessage` before sending its initial `read_state` request so Kotlin responses can update the UI.
- The Android host registers the WebMessage listener before loading `https://appassets.androidplatform.net/assets/web-play/index.html` and restricts the listener to that local WebView origin.

This boundary is intentionally kept narrow so the web layer cannot bypass Kotlin game rules or persistence.
