# Shared Web Android Screen Contract

Status: locked architecture contract

## Player-facing screen rule

All player-facing Android game screens are rendered by the shared HTML/CSS/JS UI in `app/src/main/assets/web-play/`.

The Android host must not maintain a second player-facing Compose implementation of the same game screen. This prevents Android and Windows from drifting visually or behaviorally.

## Responsibilities

### Shared HTML/CSS/JS
- Owns player-facing layout, typography, visual styling, animations, transitions, responsive behavior, accessibility, and interaction presentation.
- Runs unchanged in Android WebView and the Windows web host.
- Presents the same screen structure and interaction model across supported desktop and mobile sizes.

### Kotlin/KMP game engine
- Is authoritative for game rules, state, economy, progression, rewards, persistence, save compatibility/recovery, validation, and gameplay outcomes.
- Exposes state and actions through the existing bridge contract.
- Never delegates game truth to JavaScript.

### Compose / native Android
- Remains for native/system surfaces, host integration, permissions, settings that are intentionally native, diagnostics/developer tooling, and the fallback surface where the shared web UI cannot be hosted.
- Must not become a parallel implementation of the main player-facing game screens.

## Screen parity contract

A player-facing screen is considered the same screen when its:

1. structure and destinations come from the shared web UI,
2. state is supplied by the authoritative engine,
3. actions are sent through the shared bridge contract,
4. visual behavior is implemented in the shared web assets, and
5. responsive layout adapts to the available viewport rather than switching to a separate Android-only design.

## Migration rule

Existing Compose player screens may remain temporarily while their shared-web replacements are integrated. New player-facing gameplay screens must be added to the shared web surface rather than creating another Compose-only screen.

Migration is complete when the corresponding Android player-facing Compose screen is no longer required for normal gameplay navigation.

## Branding contract

The shared player UI uses **Numberverse** as the product/world identity and the tagline **“The engine decides. The interface celebrates.”**

Decorative star/spark elements should use semantic icon markup (for example inline SVG) where practical rather than relying on emoji glyphs, so the visual result is stable across Android WebView and Windows.

## Non-goals

This contract does not move game logic into JavaScript, remove the Kotlin engine, or require Android system UI to become HTML. It applies to player-facing game screens only.
