# Jarrlyy UI SDK → Compose parity mapping

## Goal

Make the native Android and Windows Compose UI feel like the Jarrlyy UI SDK demo without replacing the app's existing layout, routes, game rules, save behavior, or localized copy. The web SDK remains HTML/CSS/JS; native screens use Compose-native equivalents that share its design tokens and interaction contracts.

## Component mapping

| Existing app element | Jarrlyy UI SDK component | Compose parity primitive | Existing usage / migration target |
| --- | --- | --- | --- |
| Primary action buttons | `.jui-button.jui-button-primary` | `JarrlyyButton` | Shared guess panel first; then primary actions in `PlayScreen.kt`, `ArcadeScreen.kt`, progression screens and save dialogs |
| Secondary / outlined buttons | `.jui-button`, `.jui-button-secondary` | `JarrlyySecondaryButton` | Shared guess panel first; then cancel/back/secondary actions across screens |
| Cards and grouped panels | `.jui-card` | `JarrlyyCard` | `SharedGuessFeedbackCard.kt` first; then cards in `PlayScreen.kt`, `StatsScreen.kt`, `LiveOpsScreen.kt`, `ShopScreen.kt` and progression screens |
| Text and numeric inputs | `.jui-field`, `.jui-field-label` | `JarrlyyTextField` | `SharedGuessInputField.kt` first; then forms in `SettingsScreen.kt`, `StakingScreen.kt`, `SaveSlotsScreen.kt`, `PlayerProfileScreen.kt` and developer tools |
| Switches | `.jui-switch`, `.jui-switch-group` | `JarrlyySwitchRow` | Settings toggles, developer logging switches and mutator selection. Track is fixed at 44 × 26 dp; label takes remaining width. |
| Dialogs / overlays | `dialog.jui-overlay`, `.jui-bottom-sheet`, `.jui-drawer` | `JarrlyyDialog` for confirmation dialogs; keep Compose-native navigation/sheets for platform behavior | Existing dialogs in `ArcadeScreen.kt`, `SaveSlotsScreen.kt`, `FeatureTourOverlay.kt`, `MoreScreen.kt` and `NavGraph.kt` |
| Linear progress / XP | `.jui-meter`, `.jui-xp` | `JarrlyyProgressBar` | Progress in `PlayScreen.kt`, `StatsScreen.kt`, `AchievementsScreen.kt`, `WorldMapScreen.kt` and desktop codex/achievement/relic screens |
| Status chips | `.jui-badge`, `.jui-badge-success`, `.jui-badge-cyan` | Material 3 Surface/AssistChip with SDK token colors | Ranks, status, locked/unlocked and event labels |
| Feedback panels | `.jui-banner`, `.jui-banner-success`, `.jui-banner-danger` | `JarrlyyCard` + status-specific color tokens | Shared guess feedback and future success/error notices |
| Segmented controls / tabs | `.jui-segmented`, `.jui-tabs` | Compose `SingleChoiceSegmentedButtonRow` / `TabRow` with matching token styling | Use only where the app already has a segmented/tab interaction; do not introduce new navigation. |

## Design tokens

- Control height: 44 dp minimum.
- Button radius: 12 dp.
- Input radius: 11 dp.
- Card radius: 18 dp with a subtle outline.
- Switch track: 44 × 26 dp with a compact moving thumb.
- Progress track: 10 dp high, rounded, purple-to-cyan fill.
- Primary accent: theme primary → secondary gradient.
- Use the existing app theme and reduced-motion preference; do not add new theme settings or change localized labels.

## Behavior-preservation rules

1. Keep each screen's current placement, spacing, navigation, click callback, enabled/disabled condition and state ownership.
2. Do not move gameplay, saves, rewards or validation into UI components.
3. Keep localized text supplied by the calling screen; shared primitives must not hard-code user-facing copy.
4. Keep WebView SDK demo isolated from Kotlin game state. Native parity components are Compose equivalents, not a JS bridge.
5. Keep progress values clamped to 0–1 and preserve existing accessibility labels/semantics at call sites.
6. Respect reduced-motion settings for animated containers and transitions.
7. Migrate by screen in small increments; don't bulk-replace all Material 3 usages before screenshot/manual review.

## Implemented in this branch

- Added reusable Compose primitives: `JarrlyyButton`, `JarrlyySecondaryButton`, `JarrlyyCard`, `JarrlyyTextField`, `JarrlyySwitchRow`, `JarrlyyProgressBar`, and `JarrlyyDialog`.
- Routed the shared guess-entry panel's buttons and text field through the new primitives.
- Routed the shared feedback panel through the new SDK-style card while preserving its status-color mapping and reduced-motion behavior.
- Left screen-specific UI migrations listed above as explicit follow-up targets so this initial parity layer can be visually checked before expanding it everywhere.

## Manual test checklist

- [ ] Android: open the guess screen; confirm input, submit and new-game actions look right and still behave exactly as before.
- [ ] Android: verify feedback colors for ready, too-low, too-high and correct states.
- [ ] Android: check a long localized input label and a small screen; confirm there is no clipping or horizontal overflow.
- [ ] Android: check the primary and secondary buttons' disabled/enabled states.
- [ ] Windows desktop: open the shared guess screen and verify the same controls remain usable with mouse and keyboard.
- [ ] Accessibility: navigate the input and buttons with keyboard/switch access; confirm labels and focus remain clear.
- [ ] Reduced motion: enable the app's reduced-motion setting and confirm feedback changes do not animate.
- [ ] Compare against the [Jarrlyy UI SDK demo](https://ne0gl1tch20.github.io/GuessTheNumber/jarrlyyuisdk/demo.html); verify the controls feel consistent without changing screen layout.
