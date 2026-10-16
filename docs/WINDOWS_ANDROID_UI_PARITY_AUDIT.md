# Android-to-Windows UI parity audit

**Status:** First corrective batch in progress. Full visual and interaction parity is not claimed.

## Android reference

Android's `NavGraph.kt` declares four primary bottom-navigation destinations in this order: Play (`PlayScreen`), Upgrades (`UpgradeScreen`), Shop (`ShopScreen`) and More (`MoreScreen`). The previous Windows Play/About-only shell did not match that navigation structure.

## First batch changes

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
| Upgrades | `ui/screens/UpgradeScreen.kt` | Regular Money upgrade purchases are isolated under Upgrades. Android balance header, cards, animations and all details are not yet visually matched. |
| Shop | `ui/screens/ShopScreen.kt` | Existing Nebula purchases are isolated under Shop. Android category filters, cosmetic equip flow and card presentation still need porting. |
| More | `ui/screens/MoreScreen.kt` | Entry point exists. Prestige, Ultra and Talent are separate desktop subroutes reached from More. The reorderable, feature-flagged, locked Android menu is not yet ported. |
| Prestige | `ui/screens/PrestigeScreen.kt` | Current purchase/reset controls exist under More; dedicated Android screen layout and interactions need review. |
| Ultra | `ui/screens/UltraScreen.kt` | Current purchase/reset controls exist under More; dedicated Android screen layout and interactions need review. |
| Talent Web | `ui/screens/TalentScreen.kt` | Current talent purchases exist under More; the Android tree/web presentation is not ported. |
| Mutators | `ui/screens/MutatorsScreen.kt` | Basic toggles exist on Play. Challenge-builder selection and completion flow are incomplete. |
| Achievements | `ui/screens/AchievementsScreen.kt` | Basic achievement information exists on Play; dedicated Android screen and reward presentation are missing. |
| World Map | `ui/screens/WorldMapScreen.kt` | Basic world progression exists on Play; dedicated map and boss navigation are missing. |
| Statistics | `ui/screens/StatsScreen.kt` | Dedicated Windows screen not implemented. |
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
- Automated tests have not been run in this session. Windows runtime and screenshot comparison remain unverified.
- This is the start of the real UI port, not a claim that the full Android UI has been reproduced.