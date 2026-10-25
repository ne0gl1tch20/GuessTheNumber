# Ultimate Widget Visual Redesign

**Status:** Implementation foundation in this pull request  
**Branch:** `feature/ultimate-widget-visual-redesign`

## Goal

Give Guess The Number a consistent, playful, polished arcade identity across the Android WebView and desktop shared HTML frontend. The visual direction combines friendly game feedback, tactile controls and restrained neon accents without copying another game's branding.

## Architecture constraints

- The shared `app/src/main/assets/web-play/` frontend remains the canonical player-facing UI for Android and Windows.
- Compose remains for native/system surfaces and host integration; do not create a second Compose implementation of player-facing screens.
- Kotlin/KMP remains authoritative for game state, rewards, balances, progression, save operations, validation and economy. HTML requests actions through the existing allowlisted bridge.
- Reuse `jarrlyyuisdk` core and advanced components. Extend existing APIs and namespaced tokens rather than adding another UI framework.
- Use local bundled assets only. No remote fonts, CDN, network dependency, analytics or ads.
- Keep localization, existing routes, bridge contracts, save compatibility and gameplay behavior unchanged.

## Visual system

### Typography

Use existing local font assets first:
- **Manrope** for readable body copy and controls.
- **Space Grotesk** for expressive headings and display labels.
- **JetBrains Mono** for currency, counters and number-heavy game data.
- **Inter** as a resilient local fallback.

Fonts must load offline and remain legible at larger text sizes. Never rely on a remote font service.

### Components

Apply consistent treatments to buttons, icon buttons, cards, inputs, dialogs, bottom sheets, drawers, badges, progress UI and notification surfaces:
- Rounded but deliberate corner radii and consistent spacing.
- Clear surface layers, restrained purple/cyan highlights and gold reserved for rewards.
- Tactile pressed states, disabled/loading states and visible focus indicators.
- Local SVG icons wherever an icon is part of product chrome; avoid emoji or text glyphs as a substitute for a real icon.
- Responsive dialog dimensions, wrapping and scrolling to prevent clipped popup layouts.

### Motion and accessibility

- Use short, spring-like press and entrance motion where it improves feedback.
- Respect `prefers-reduced-motion` and keep all state changes understandable without animation.
- Preserve keyboard navigation, visible focus, screen-reader labels, contrast, touch targets and zoom.
- Do not communicate status using color alone.
- Keep responsive layouts usable at narrow phone widths and wider desktop windows.

## Implementation approach

The first implementation layer is a shared CSS token and interaction layer in `web-play/styles.css`. It deliberately builds on existing font registrations, theme variables, SVG sprite, game classes and Jarrlyy UI SDK components. This keeps the Android and desktop presentation in sync because both hosts load the same frontend.

Next refinements within the same design effort should audit existing screens against this system: gameplay/HUD, upgrade and shop cards, progression/rewards, dialogs and confirmations, settings/save utilities, tutorials, changelog and developer-only screens. Prefer fixing shared components once over adding per-screen overrides.

## Verification checklist

- [ ] Android WebView and desktop host use the same packaged HTML/CSS/JS assets.
- [ ] Bundled fonts load with no network access and fallback cleanly.
- [ ] Buttons, fields, cards and overlays have consistent focus/pressed/disabled states.
- [ ] Dialogs and long text wrap and scroll within viewport bounds on narrow devices.
- [ ] Reduced-motion preference removes nonessential animation.
- [ ] Keyboard navigation, large text, accessible labels and contrast remain usable.
- [ ] SVG icons resolve locally and remain crisp at different sizes.
- [ ] Existing localization, Kotlin bridge actions, reward authority, save/load and gameplay flows remain unchanged.
- [ ] Run relevant Gradle tests and shared-web asset validation before merge; visually smoke-test Android and desktop when available.

## Scope note

This is a visual-system foundation, not a claim that every screen has already been individually redesigned or visually verified. The existing shared HTML architecture is intentional and should not be reopened by this work.
