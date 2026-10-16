# Android-to-Windows exact-port audit

**Goal:** Windows is the same GuessTheNumber app as Android, implemented with desktop platform APIs. Android source, content, shared rules and assets are authoritative. A screen is not complete based on compilation alone.

## Current baseline

PR #214 is merged. It connected saved Android-equivalent theme mode and custom colors, reduced-motion-aware destination transitions, Settings & Save Management and configurable Windows navigation placement. It did not complete the port.

## Stage 1: Shell and visual foundation
- [x] Four primary navigation destinations (Play, Upgrades, Shop and More).
- [x] Saved theme mode, custom theme colors and reduced-motion-aware transitions.
- [x] Windows navigation can be positioned left, right, top or bottom and persists locally.
- [ ] Compare Android and Windows screenshots at matching sizes and theme settings.
- [ ] Verify focus, hover, resizing, keyboard navigation and selected/unselected states on a Windows runtime.

## Stage 2: Screen coverage

| Android screen | Windows status |
| --- | --- |
| Play, Upgrades, Shop | Partial implementations; exact visual and interaction parity unverified |
| More | Links to the existing screens and the remaining parity routes |
| Prestige, Ultra, Talent Web, Statistics | Dedicated partial screens exist; exact parity unverified |
| Settings & Save Management | First pass exists; localization and OS notification delivery remain incomplete |
| About | Windows update page exists; Android content parity incomplete |
| Achievements, World Map, Mutators, Save Slots, Relics, Home Base, Codex, Endgame, Lucky Stake | Navigation routes added; feature data/actions are still missing |
| Developer Settings, Developer Console, Process Inspector | Navigation routes added; diagnostics/command behavior is still missing |
| Music Player, Arcade, Activity Hub, Lua Testing Hub, Changelog, Live Ops | Navigation routes added; feature-specific behavior is still missing |
| Crash Recovery | Windows boot/save recovery parity is not verified |
| Guided Tutorial / Feature Tour | Android-equivalent overlays are not ported |

## Stage 3: Behavior and shared rules
- [ ] Reuse shared domain rules, repositories, catalog data, localization resources and assets wherever possible.
- [ ] Match Android actions, purchases, rewards, unlocks, resets, settings, confirmations and validation.
- [ ] Preserve offline behavior, save compatibility and recovery behavior, with no ads, analytics or accounts.
- [ ] Gate development and Live Ops features using equivalent flags and sandbox validation.
- [ ] Test persisted state and actions, not just route rendering.

## Stage 4: Polish and verification
- [ ] Match screen motion and reduced-motion behavior, audio controls, feedback and visual effects where supported.
- [ ] Adapt only platform-specific behavior such as mouse/keyboard input, file pickers and native window chrome.
- [ ] Compare screenshots screen by screen and keep a mismatch checklist.
- [ ] Run focused JVM tests and CI; perform Windows runtime checks before claiming runtime parity.
- [ ] Mark a feature verified only when appearance and behavior both have evidence.

## This batch

Adds explicit navigation to the remaining Android screen families and per-screen parity notes so gaps are visible rather than hidden behind the More menu. These new routes are tracking shells, not finished feature ports. Next work should replace each shell with the Android screen real content and shared behavior in small verified batches.
