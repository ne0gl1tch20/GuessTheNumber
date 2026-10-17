# Shared Web UI Theme Foundation

**Status:** Initial reusable CSS foundation implemented; production gameplay UI has not migrated.
**Reviewed:** 2026-10-17
**Source:** `app/src/main/assets/web-ui/theme.css`

## Purpose

Create one host-neutral design foundation for the HTML/CSS/JavaScript game UI. Android WebView and a future Windows host should load the same frontend assets, not maintain separate visual token sets.

The first version is intentionally small. It establishes reusable tokens and baseline controls without pretending to be the final GuessTheNumber visual design.

## Current tokens and primitives

- Colors: background, surface, raised surface, primary text, muted text, accent, accent text, border, focus and error.
- Spacing scale: 4, 8, 12, 16, 20, 24 and 32 CSS pixels.
- Radii: small, medium, large and pill.
- Shared sizing: minimum control height and content maximum width.
- Motion: fast and standard duration tokens, focus ring dimensions and reduced-motion handling.
- Utility classes: `.gtn-screen`, `.gtn-panel`, `.gtn-muted`, `.gtn-eyebrow`, `.gtn-number`, `.gtn-status` and `.gtn-error`.
- Game UI primitives: stacked/inline layouts, secondary buttons, numeric inputs, stat tiles, badges, semantic feedback states and progress bars.
- Baseline controls: inherited form typography, visible keyboard focus, touch-friendly buttons and disabled states.
- Responsive/device behavior: safe-area padding, tabular numbers, color-scheme adaptation and reduced-motion preference support.

## Use it

Load the shared stylesheet before screen-specific styles:

```html
<link rel="stylesheet" href="../web-ui/theme.css">
<link rel="stylesheet" href="styles.css">
```

Screen styles should prefer `--gtn-*` tokens and shared `.gtn-*` primitives instead of inventing parallel color, spacing, radius or focus systems. Screen-specific styles can extend the foundation where the interaction genuinely needs it.

## Current integration

The isolated debug prototype loads the shared stylesheet and uses its tokens for layout, surfaces, text and controls. This is a first integration check, not a production screen migration. The prototype remains a counter-only bridge proof.

## Before production adoption

- Agree on the final game visual direction and validate contrast in light and dark themes.
- Add reusable input, stat, feedback, navigation and modal patterns when actual screen work requires them.
- Verify typography scaling, Android safe areas, IME behavior, keyboard/focus behavior and screen-reader output.
- Confirm CSP and asset paths in Android WebView; validate the same stylesheet bundle with the chosen Windows host.
- Avoid design-token drift: shared components should use the foundation unless a documented reason requires a local token.
- Add automated checks or visual fixtures where they fit the existing build; do not call platform parity complete until runtime evidence exists.

## Honest status

The CSS file exists and the debug prototype references it. Shared input, stat, badge, feedback, progress and layout primitives are now available for future screens. No build, browser runtime, device, accessibility or Windows-host validation has been performed for this increment. The final production theme and reusable production components are still to be designed and verified.
