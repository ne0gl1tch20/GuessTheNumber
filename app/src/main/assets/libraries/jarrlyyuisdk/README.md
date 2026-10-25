# Jarrlyy UI SDK

A local-first, framework-free HTML/CSS/JavaScript UI engine for games and everyday apps. It powers GuessTheNumber's shared Android WebView and Windows desktop UI, and is structured so other hosts can reuse the same core components with their own theme and behavior.

## Load it

From \`app/src/main/assets/web-play/index.html\`:

\`\`\`html
<link rel="stylesheet" href="../libraries/jarrlyyuisdk/styles/jarrlyy-ui.css">
<link rel="stylesheet" href="../libraries/jarrlyyuisdk/styles/jarrlyy-ui-advanced.css">
<script src="../libraries/jarrlyyuisdk/components/jarrlyy-ui.js"></script>
<script src="../libraries/jarrlyyuisdk/components/jarrlyy-ui-advanced.js"></script>
\`\`\`

Keep these local assets together. There is no CDN, npm, runtime network request, Android dependency or game-state dependency. CSS classes use the \`jui-\` namespace and declarative controls use \`data-jui-*\` attributes.

## Reuse across game and everyday apps

Treat the SDK as a host-neutral UI engine, not as a GuessTheNumber-only skin. Core buttons, forms, layout, overlays, navigation, feedback and theme tokens should work in games, dashboards, study tools and utilities. The host owns its data, routing, localization and business rules. Game-specific presentation belongs in optional components or host adapters rather than being required by core controls. Keep selectors namespaced, prefer CSS custom properties for theming, and preserve existing class names and APIs when extending components.

The SDK root (`.jui-root`, or `.jui-advanced-root` for an advanced-only host) opts a page into SDK-wide touch behavior. The default WebView tap highlight is disabled inside that root, while visible `:focus-visible` keyboard indicators remain enabled.

## Curated component inventory

- **Actions:** primary, secondary, quiet and destructive buttons, icon buttons, button groups, segmented controls, floating action button patterns, loading and disabled states.
- **Structure and navigation:** cards, section headers, toolbars, breadcrumbs, tabs, pagination, accordions, context menus, keyboard shortcut hints, responsive grids and dividers.
- **Forms:** text, number, password, search, date/time and color fields; switches, checkboxes, native select controls, single-value and dual-thumb range sliders.
- **Feedback and overlays:** tooltips, toasts, status banners, empty states, skeletons, accessible dialogs, confirmation dialogs, native dialog-based drawers and bottom sheets, update prompt previews and guided tours.
- **Game UI:** HUD rows, currency/reward labels, stat cards, rank/level indicators, progress/XP meters, circular progress, timers/countdowns, save-slot and achievement cards, charts, data tables and carousels.
- **Utilities:** local SVG icon sprite, theme tokens, reduced-motion handling, and helpers for loading states and progress values.

The gallery at \`demo.html\` demonstrates the components with local, non-persistent sample data. It is a component lab, not a production game screen.

## APIs

Core API:
- \`JarrlyyUI.toast(message, duration?)\`
- \`JarrlyyUI.dialog(title, message, options?)\`
- \`JarrlyyUI.tour(steps, options?)\`
- \`JarrlyyUI.init()\`

Advanced API:
- \`JarrlyyUIAdvanced.init()\` (safe to call again; initialized controls are not bound twice)
- \`JarrlyyUIAdvanced.setLoading(button, loading, label?)\`
- \`JarrlyyUIAdvanced.setProgress(element, value)\`

Advanced events bubble from their control:
- \`jui:change\` for segmented controls (\`detail.value\`)
- \`jui:tab-change\` for tab changes (\`detail.value\`)
- \`jui:range-change\` for range selection (\`detail.min\`, \`detail.max\`)
- \`jui:dismiss\` before a banner is removed

All user-facing text remains owned by the host app and must be localized there. The SDK does not own GuessTheNumber's translations, authoritative gameplay, rewards, saves or economy rules.

## Design and accessibility

- Dark theme by default; add \`data-jui-theme="light"\` to the root for light tokens.
- Visible keyboard focus and ARIA selected, checked, disabled, busy and progress states.
- Arrow/Home/End keyboard navigation for tabs and segmented controls.
- Native HTML controls and dialog semantics where they provide more reliable accessibility than custom replacements.
- Comfortable touch targets, responsive overlay widths and safe-area-aware floating actions.
- Reduced-motion support for overlays, loading indicators, skeletons and interactive controls.

## Build integration

Android packages this directory under \`assets/libraries/jarrlyyuisdk/\`. The Windows shared-web host ships the same local web bundle. Keep the core and advanced CSS/JS files versioned together.


## Popup and prompt sizing standard

Use the SDK's `JarrlyyUI.dialog(title, message, options)` for standard prompts where possible. SDK dialogs use a viewport-aware maximum height and width, scroll internally when content is long, wrap long/localized text, and let action buttons wrap on narrow screens. The game host applies the same containment rules to Nimmy, reset/reward prompts and the Arcade popup so host-specific dialogs do not collapse into clipped strips. Avoid fixed heights and conflicting transforms on modal content; let the overlay center the dialog and let the dialog itself handle overflow. Short landscape screens and safe-area insets are covered by the shared CSS rules.


## Motion engine (Phase 2)

The optional motion API is exposed as `JarrlyyUI.motion` and has no dependency on a game loop or native host:

- `JarrlyyUI.motion.count(element, target, { from?, duration?, format? })` animates a numeric label.
- `JarrlyyUI.motion.progress(element, value, { min?, max? })` updates a meter and its accessible progress values.
- `JarrlyyUI.motion.floatText(anchor, message, { className?, container? })` shows short-lived floating feedback.
- `JarrlyyUI.motion.init(root?)` activates one-time `data-jui-reveal` entrances as content scrolls into view.

Motion uses requestAnimationFrame only for active counters, IntersectionObserver for on-demand reveal, transform/opacity for smooth compositing, and automatic cleanup for floating text. It respects `prefers-reduced-motion`, avoids external libraries and runtime requests, and degrades to immediate values when animation APIs are unavailable. These primitives are browser, Android WebView and Windows WebView friendly; host code remains responsible for authoritative state.
