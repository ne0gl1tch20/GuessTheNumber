# Jarrlyy UI SDK

A local-first, framework-free HTML/CSS/JavaScript component kit for GuessTheNumber's shared WebView UI and future host apps.

## Location

\`app/src/main/assets/libraries/jarrlyyuisdk/\`

## Load it

From \`app/src/main/assets/web-play/index.html\`:

\`\`\`html
<link rel="stylesheet" href="../libraries/jarrlyyuisdk/styles/jarrlyy-ui.css">
<script src="../libraries/jarrlyyuisdk/components/jarrlyy-ui.js"></script>
\`\`\`

The kit is deliberately namespaced with \`jui-\` classes and \`data-jui-*\` attributes so it can coexist with the existing game UI. It has no CDN, npm, network, Android or game-state dependency.

## Included component patterns

- Buttons, icon buttons, badges, cards and tooltips
- Text/number/password/search fields, native date/time inputs, switches, checkboxes and sliders
- Context menus and keyboard-shortcut hints
- Breadcrumbs, carousels, pagination and sortable data tables
- Progress/XP meters, level/rank indicators, stat cards and game HUDs
- Save-slot and achievement cards
- SVG-backed charts, timers/countdowns and drag/drop file zones
- Toasts, accessible dialogs, update prompts and feature-tour steps

## JavaScript API

- \`JarrlyyUI.toast(message, duration?)\`
- \`JarrlyyUI.dialog(title, message, options?)\`
- \`JarrlyyUI.init()\` (safe to call again for newly inserted markup; existing behavior should not be initialized twice)

Declarative controls are initialized automatically. See \`demo.html\` for working examples.

## Theme and accessibility

Override the \`--jui-*\` custom properties to theme the kit. Components use visible keyboard focus, accessible state, semantic native controls and reduced-motion support. All text supplied by applications should be localized by the host app; the kit does not own GuessTheNumber's translations or authoritative game rules.

## Build integration

Android APK builds package this directory at \`assets/libraries/jarrlyyuisdk/\`. The Windows shared-web host copies the same local SDK files alongside the shared web bundle. No remote asset requests are needed.