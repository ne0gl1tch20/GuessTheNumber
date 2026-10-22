# GuessTheNumber HTML/WebView UI Kit + Android Back Navigation Plan

## Scope

Build a reusable, host-neutral HTML/CSS/JavaScript UI kit at `app/src/main/assets/libraries/jarrlyyuisdk/`. The shared web-play bundle consumes it using local relative paths. GuessTheNumber remains the reference theme, while the components are designed so another local WebView app can reuse them with a different theme.

The player-facing web bundle remains the canonical visual layer for Android and Windows. Kotlin/KMP remains authoritative for game state, progression, economy, rewards, saves, validation and side effects.

## Selected component inventory

### Navigation and structure
- [ ] Context menus
- [ ] Keyboard shortcut hints
- [ ] Breadcrumbs
- [ ] Pagination
- [ ] Toolbars
- [ ] Side navigation and drawers
- [ ] Navigation bars and tabs
- [ ] Button groups
- [ ] Split buttons
- [ ] Segmented controls
- [ ] Responsive grids
- [ ] Section headers and dividers
- [ ] Accordions and expandable panels

### Inputs and controls
- [ ] Compose-style toggle switches
- [ ] Switch groups
- [ ] Checkboxes and radio groups
- [ ] Single-value sliders
- [ ] Range sliders
- [ ] Volume sliders with numeric values
- [ ] Text, number and password fields
- [ ] Search fields
- [ ] Dropdowns and selectable lists
- [ ] Color pickers
- [ ] Date and time pickers
- [ ] Drag-and-drop zones

### Actions
- [ ] Primary and secondary buttons
- [ ] Outlined and text buttons
- [ ] Icon buttons
- [ ] Floating action buttons
- [ ] Destructive actions
- [ ] Loading and disabled states

### Feedback and overlays
- [ ] Tooltips
- [ ] Snackbars and toasts
- [ ] Error and success banners
- [ ] Alert dialogs
- [ ] Confirmation dialogs
- [ ] Bottom sheets
- [ ] Update prompts
- [ ] Feature-tour popups
- [ ] Empty states
- [ ] Skeleton loading

### Game UI
- [ ] Game HUD elements
- [ ] Timers and countdowns
- [ ] Level and rank indicators
- [ ] Save-slot cards
- [ ] Progress and XP meters
- [ ] Circular progress indicators
- [ ] Progress bars
- [ ] Achievement cards
- [ ] Currency and reward displays
- [ ] Stat cards
- [ ] Badges and status indicators
- [ ] Charts and graph containers
- [ ] Data tables
- [ ] Carousels

## Design rules

1. Reuse the existing GuessTheNumber theme tokens: dark surfaces, purple/cyan accents, rounded cards, local fonts and local SVG icons.
2. Keep components host-neutral. Do not couple UI-kit code to Android APIs, Compose, Kotlin state or game rules.
3. Use local assets only. No CDN or runtime network dependency.
4. Make touch targets comfortable for Android WebView while preserving desktop mouse and keyboard use.
5. Respect `prefers-reduced-motion` and the game's reduced-motion setting.
6. Keep destructive operations behind explicit confirmation.
7. Keep accessibility state in the component itself: labels, focus, keyboard behavior, selected/checked/disabled states and live status where appropriate.
8. Expose small data attributes and JS helpers rather than requiring a framework.
9. Keep the kit modular so another app can load the CSS/JS without importing the whole GuessTheNumber page.
10. Use the existing SVG icon system rather than Unicode icons for reusable controls.

## Proposed files

```text
app/src/main/assets/libraries/jarrlyyuisdk/
  README.md
  styles/
    jarrlyy-ui.css
    demo.css
  components/
    jarrlyy-ui.js
    demo.js
  icons/
    jarrlyy-icons.svg
  demo.html
```

The first implementation should contain a component gallery in `demo.html` so every control can be exercised independently before it is migrated into gameplay screens.

## Compose-style controls

The first interactive controls to standardize are:

- toggle switch with a sliding thumb and checked/unchecked state
- switch group
- volume slider with percentage/value readout
- generic single-value slider
- range slider
- checkbox/radio group
- segmented control
- dropdown/selectable list

These should share the same focus ring, disabled state, motion curve, sizing tokens and accessibility conventions.

## Android back navigation bug

Current architecture uses a single Compose `NavGraph` with the player-facing HTML bundle mounted in an Android `WebView`. The web UI currently changes screens with `activateScreen()` by toggling `[data-screen]` visibility, so those screen changes are not browser history entries. That explains why Android's system Back can fall through to Activity exit instead of returning to the previous HTML screen.

The fix should have three layers:

1. HTML/WebView history: every player-facing screen transition should create a browser history entry, with `popstate` restoring the previous screen.
2. Android bridge: Android Back should ask the HTML surface to consume the back action first. Only when the web surface is already at its root should the Activity finish.
3. Compose overlays: Save Slots, Player Profile, color picker, Developer Settings and debug/system surfaces must close themselves before the underlying HTML surface receives Back.

Acceptance flow:

- Play -> More -> Settings -> Android Back => More
- Settings -> another HTML page -> Android Back => previous HTML page
- HTML page -> Save Slots -> Android Back => return to HTML page
- Player Profile -> Android Back => close profile
- Color picker -> Android Back => dismiss picker
- Developer Settings/debug page -> Android Back => return to its parent
- Root Play screen -> Android Back => normal Activity exit behavior
- Repeated Back presses must not duplicate history entries or trap the user in a loop

## Validation focus

For this workstream, prioritize HTML/CSS/JS validation and static bridge checks. Android/Windows runtime tests are secondary for UI-kit-only changes, but the Android back-navigation change needs an Android test covering system Back behavior once the native bridge is wired.
