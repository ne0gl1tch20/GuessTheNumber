# Android-to-Windows exact-port audit

**Goal:** Windows is the same GuessTheNumber app as Android, implemented with desktop platform APIs. Android source, content, shared rules and assets are authoritative. A screen is not complete based on compilation alone.

## Repository scan

The current repository tree contains 28 Android UI screen Kotlin files, a separate JVM/Compose desktop app and a shared module with common UI contracts and game/domain rules. Android's primary navigation is Play, Upgrades, Shop and More. Its theme supports System, Dark, Light and AMOLED modes, custom primary/secondary/tertiary colors and reduced motion.

## Ordered implementation stages

### 1. Shared visual foundation and shell

- [x] Match Android's four primary navigation destinations, order, labels and Material icons.
- [x] Use the saved Android-equivalent theme mode and custom theme colors on Windows.
- [x] Add Android-style destination transitions and respect the saved reduced-motion preference.
- [ ] Verify real Android/Windows screenshots at matching sizes and theme settings.
- [ ] Verify selected/unselected navigation states, focus, hover, resizing and keyboard navigation at runtime.

### 2. Port every screen and visual element

The audit must cover every Android screen and each element within it, including labels, icons, typography, spacing, cards, empty/loading/error/locked states, menus, dialogs, animations and original assets.

| Android screen | Windows status |
| --- | --- |
| Play | Partial implementation; exact visual and interaction parity unverified |
| Upgrades | Partial implementation; exact visual and interaction parity unverified |
| Shop | Partial implementation; catalog, filters and cosmetic equip exist; exact parity unverified |
| More | Partial implementation; full reorderable/feature-flagged menu parity unverified |
| Prestige | Partial controls exist; dedicated screen parity unverified |
| Ultra | Partial controls exist; dedicated screen parity unverified |
| Talent Web | Partial purchases exist; Android tree/web UI not ported |
| Mutators | Basic toggles exist; challenge-builder flow incomplete |
| Achievements | Basic information exists; dedicated Android presentation incomplete |
| World Map | Basic progression exists; map and boss-navigation parity incomplete |
| Statistics | A dedicated Windows screen exists; exact parity unverified |
| Settings & Save Management | First Windows screen pass added in PR #214: theme, colors, notation, audio levels, accessibility, stored notification preferences, language preference and confirmed reset. Full localization and OS notification delivery remain incomplete. |
| About | Windows update page exists; Android content parity incomplete |
| Developer Settings | Not ported |
| Developer Console | Not ported |
| Process Inspector | Not ported |
| Save Slots | Local save exists; Android slot-management UI not ported |
| Music Player | Not ported |
| Arcade | Not ported |
| Activity Hub | Not ported |
| Lua Testing Hub | Not ported |
| Changelog Viewer | Not ported |
| Lucky Stake | Not ported |
| Relics | Not ported |
| Home Base | Not ported |
| Codex | Not ported |
| Endgame | Not ported |
| Live Ops | Not ported; Android route is feature-flagged |
| Crash Recovery | Windows boot/save recovery parity not verified |
| Guided Tutorial / Feature Tour | Not ported as Android-equivalent overlays |

### 3. Connect all behavior and shared game rules

- Reuse shared domain rules, repositories, catalog data, localization resources and assets wherever possible.
- Match every Android action, purchase, reward, unlock, reset, setting, confirmation, validation message and navigation path.
- Preserve offline behavior, existing save compatibility, recovery behavior and current no-ads/no-analytics/no-accounts requirements.
- Add a dedicated Windows route for each Android route; feature-gated Android features must follow equivalent flags or explicit platform capability handling.
- Test each interaction and its persisted state, not just navigation or rendering.

### 4. Polish and verification

- Match Android motion and reduced-motion behavior, sound/music controls, feedback and visual effects where the platform supports them.
- Adapt only genuinely platform-specific behavior: mouse, keyboard, window sizing, file pickers and native window chrome.
- Compare Android and Windows screenshots screen by screen and retain a mismatch checklist.
- Run focused JVM tests and existing CI; perform Windows runtime checks before claiming runtime parity.
- Mark a feature verified only when both appearance and behavior have evidence.

## Current batch

This batch connects saved Android-equivalent theme settings to the Windows root theme, adds AMOLED/custom-color handling, Android-style destination motion and the first dedicated Settings & Save Management screen. Settings persist through the desktop save adapter; confirmed reset replaces the active slot and removes its previous backup. Windows localization coverage, native notification delivery and runtime screenshot comparisons remain pending. Other Android screens are still partial or missing.
