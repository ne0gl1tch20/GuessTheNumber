# Android-to-Windows UI parity audit

**Status:** Initial navigation and Upgrades presentation changes are implemented. A Windows Statistics destination is now reachable from More and renders core game metrics. CI for the latest changes is pending; Windows runtime visual and interaction parity is not claimed.

## Android reference

Android's `NavGraph.kt` declares four primary bottom-navigation destinations in this order: Play (`PlayScreen`), Upgrades (`UpgradeScreen`), Shop (`ShopScreen`) and More (`MoreScreen`). The previous Windows Play/About-only shell did not match that navigation structure.

## First batch changes

- Windows now declares the Compose Multiplatform Material icon-core dependency explicitly and uses the same four familiar Android navigation icons (Play, Star, Shopping Cart and Menu).
- The primary navigation is now a bottom `NavigationBar` with labels and Material selection indicators, matching Android's navigation pattern instead of a row of text-only buttons at the top. Runtime screenshot comparison and animation feel still require Windows verification.

- Windows primary navigation now exposes Play, Upgrades, Shop and More in Android's order.
- Existing guessing, mutator, daily quest, achievement and world progression controls stay together on Play instead of being split into a separate Progress destination.
- Regular Money upgrades are under Upgrades.
- Nebula purchases are under Shop.
- Prestige, Ultra and Talent have separate desktop routes reachable from More, instead of being crammed into the More menu itself.
- About and Windows Debug update controls are reachable from More instead of taking a primary-navigation slot.
- Added shortcut mapping and tests for Ctrl+1 through Ctrl+5. Ctrl+1 through Ctrl+4 follow the four primary destinations; Ctrl+5 opens About.

## Source-backed destination inventory

| Android destination / route | Android source | Windows status after this batch |
| --- | --- | --- |
| Play | `ui/screens/PlayScreen.kt` | Production guess loop and currently implemented quest, achievement and world controls remain on Play. Exact layout and all Android interactions still need comparison. |
| Upgrades | `ui/screens/UpgradeScreen.kt` | First visual-parity pass added the Money balance header, multiplier dropdown, matching upgrade-card hierarchy, level badge, batch cost and purchase highlight. CI passes, but runtime screenshot comparison and full interaction review remain pending. |
| Shop | `ui/screens/ShopScreen.kt` | Existing Nebula purchases are isolated under Shop. Android category filters, cosmetic equip flow and card presentation still need porting. |
| More | `ui/screens/MoreScreen.kt` | Entry point exists. Prestige, Ultra and Talent are separate desktop subroutes reached from More. The reorderable, feature-flagged, locked Android menu is not yet ported. |
| Prestige | `ui/screens/PrestigeScreen.kt` | Current purchase/reset controls exist under More; dedicated Android screen layout and interactions need review. |
| Ultra | `ui/screens/UltraScreen.kt` | Current purchase/reset controls exist under More; dedicated Android screen layout and interactions need review. |
| Talent Web | `ui/screens/TalentScreen.kt` | Current talent purchases exist under More; the Android tree/web presentation is not ported. |
| Mutators | `ui/screens/MutatorsScreen.kt` | Basic toggles exist on Play. Challenge-builder selection and completion flow are incomplete. |
| Achievements | `ui/screens/AchievementsScreen.kt` | Basic achievement information exists on Play; dedicated Android screen and reward presentation are missing. |
| World Map | `ui/screens/WorldMapScreen.kt` | Basic world progression exists on Play; dedicated map and boss navigation are missing. |
| Statistics | `ui/screens/StatsScreen.kt` | Windows Statistics destination is reachable from More and shows guessing accuracy, correct/failed/total guesses, money earned/spent, current/best streak, Prestige/Ultra resets, Nebula earned, playtime, arcade rounds and current number range. CI and runtime visual comparison are pending. |
| Settings | `ui/screens/SettingsScreen.kt` | Dedicated Windows screen not implemented. |
| About | `ui/screens/AboutScreen.kt` | Windows About/update page exists and is now reached from More; Android content parity is incomplete. |
| Developer Settings | `ui/screens/DevSettingsScreen.kt` | Debug-only Android feature; Windows equivalent not ported. |
| Process Inspector | `ui/screens/ProcessInspectorScreen.kt` | Debug-only Android feature; Windows equivalent not ported. |
| Save Slots | `ui/screens/SaveSlotsScreen.kt` | Windows has local save persistence, not Android slot-management UI. |
| Music Player | `ui/screens/MusicPlayerScreen.kt` | Dedicated Windows screen not implemented. |
| Arcade | `ui/screens/ArcadeScreen.kt` | Dedicated Windows screen not implemented. |
| Activity Hub | `ui/screens/LuaActivityHubScreen.kt` | Dedicated Windows screen not implemented. |
| Lua Testing Hub | `ui/screens/LuaTestingHubScreen.kt` | Debug-only Android feature; Windows equivalent not ported. |
| Changelog | `ui/screens/MarkdownViewerScreen.kt` | Dedicated Windows screen not implemented. |
| Lucky Stake | `ui/screens/StakingScreen.kt` | Dedicated Windows screen not implemented. |
| Relics | `ui/screens/RelicsScreen.kt` | Dedicated Windows screen not implemented. |
| Home Base | `ui/screens/HomeBaseScreen.kt` | Dedicated Windows screen not implemented. |
| Codex | `ui/screens/CodexScreen.kt` | Dedicated Windows screen not implemented. |
| Endgame | `ui/screens/EndgameScreen.kt` | Dedicated Windows screen not implemented. |
| Live Ops | `NavGraph.kt` route `live_ops` | Dedicated Windows screen not implemented; Android route is feature-flagged. |

## Remaining UI parity rules

- Compare the actual Android source screen-by-screen before implementing its desktop counterpart.
- Preserve labels, balances, descriptions, requirements, locked states, purchase actions, menus, confirmation dialogs and loading/error/empty states.
- Adapt mobile navigation and scrolling to desktop sizing and mouse/keyboard input without deleting content or changing its information hierarchy.
- Do not copy Android-only APIs into desktop source. Reuse shared game rules and catalogs where possible.
- A passing build is not visual or interactive parity evidence. Keep screenshot and runtime checks marked unverified until performed.

## Verification

- Source-reviewed `Screen.kt`, `NavGraph.kt`, `PlayScreen.kt`, `UpgradeScreen.kt`, `ShopScreen.kt` and `MoreScreen.kt` on `master`.
- Updated desktop primary navigation, section routing and navigation unit tests in this branch.
- Latest recorded CI for commit `3153f38eb9c34c426c7d4c12c1545d975ae5079c` passed: Windows Desktop CI run [38857902508](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38857902508) and Android Validation and Tests run [38857902488](https://github.com/ne0gl1tch20/GuessTheNumber/actions/runs/38857902488).
- Windows runtime interaction and screenshot comparison remain unverified.
- The Windows Statistics screen is now implemented in the current branch and has a navigation regression test. Its layout is a first functional port of the Android `StatsScreen.kt`; screenshot comparison and Windows runtime behavior remain unverified.
- This is the start of the real UI port, not a claim that the full Android UI has been reproduced.

## Permanent parity guard

- Android is the source of truth for shared UI and theme behavior. Do not hand-copy new Windows values without checking Android first.
- Windows Desktop CI runs `tools/validate_android_windows_theme_parity.py` against Android's real theme source. It fails if the base light/dark color tokens, expressive corner sizes or typography letter spacing drift.
- When changing Android theme tokens, update the compatible Windows theme in the same change. Extend the checker whenever another shared visual token is ported.
- This automated check covers source tokens, not runtime screenshots, animation timing, theme preferences or full-screen parity. Those still need dedicated tests and real app comparisons.
