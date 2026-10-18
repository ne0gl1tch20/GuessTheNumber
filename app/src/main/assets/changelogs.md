## Unreleased (Shared HTML Play polish)

- 🎨 The shared Play header now uses the canonical `GTN_APP_LOGO.png` artwork, packaged locally for offline use.
- 🧹 Removed the extra branding tagline and restored the Guess The Number name for a cleaner header.
- 🧭 Documented the target to migrate all player-facing Android screens to the same shared HTML/CSS/JavaScript interface in staged, verified route groups. Existing Compose screens are not yet all migrated.

## Unreleased (Windows Progression Parity)

- 🔄 **Windows Debug updates**: The About menu now checks the latest successful Windows Desktop CI run on `master`, downloads its short-lived Actions artifact, verifies the bundled manifest and SHA-256 checksum, and launches the MSI installer. The channel remains separate from production releases; interactive update/install testing is still pending.
- 🌳 **Windows Talent Web**: Replaced the flat talent catalog with a connected six-node tree, selectable node details, zoom controls, drag/pinch navigation, parent-lock feedback and transaction-backed unlocks. Runtime screenshot parity remains unverified.
- 🗺️ **World and boss progression**: Added Windows boss battle start/leave actions and connected guesses to shared boss damage, miss regression and victory-reward transactions. The interactive map canvas and Windows runtime parity checks remain unfinished.
- 💎 **Relic transactions**: Relic claiming now checks boss unlock requirements and duplicate ownership; equip/unequip actions validate ownership and the four-slot limit through shared transactions.
- 📖 **Explorer's Codex**: Added the 12 canonical world, boss and secret entries, saved discovery progress, and a one-time 100 Nebula completion reward.
- 🏁 **Endgame tracker**: Added six saved progression goals covering bosses, secrets, relics, Home Base level, Endless Rift tier and world mastery.
- 🎲 **Lucky Stake**: Added a Windows coin-flip screen with positive/affordable stake validation and a streak-based payout multiplier. Localization and runtime parity remain unverified.
- 🧪 **Candidate status**: These are in-progress Windows ports, not a parity-complete release. On PR #215 head `c2138c0ff8ce2aba8813dd3104a560b489c82575`, Android Validation passed and Windows Desktop CI failed; see the parity audit before treating this work as validated.

## Unreleased (Windows Android visual parity)

- 🎨 **Android theme parity pass**: aligned Windows Material 3 light/dark colors, expressive corner shapes and typography letter spacing with the Android theme tokens. Windows-only system chrome stays native. AMOLED/custom color settings and screenshot comparison remain to be verified.

- 🧭 **Android-style Windows navigation**: Windows now uses the Android app's bottom navigation pattern with Play, Upgrades, Shop and More labels, matching Material icons and selection indicators. Declared Compose Material icon-core explicitly so the desktop target does not rely on a removed transitive dependency. Runtime visual verification is still pending.

## Unreleased (Windows exact-port UI parity)

- 🛒 **Upgrades screen**: Windows now mirrors Android's Money balance header, buy-multiplier dropdown, upgrade descriptions and levels, combined batch costs, affordability states and purchase-highlight animation. Gameplay transactions still use the existing economy transaction layer.
- ✨ **Nebula Shop parity pass**: Added the Nebula balance/equipped-cosmetic header, Android's All / Automation / Boosts / Cosmetics filters, catalog-driven shop cards and owned-cosmetic Equip/Equipped states. Cosmetic equipping validates ownership and persists through the desktop economy transaction flow.

- 🎯 **Shared guess feedback presentation**: Windows Play now uses the same shared status-colored feedback card as Android, including matching ready/invalid, too-low, too-high and correct states. Blind-mutator guesses keep direction hints hidden, and the card respects the saved reduced-motion setting.

## Unreleased (Windows Play Screen Parity)

- ⌨️ **Shared guess entry**: Windows Play now uses the same shared controlled numeric input component as Android, including the displayed allowed range and Enter-to-submit behavior, while keeping desktop validation, save-before-feedback, and gameplay rules unchanged.

## Unreleased (Windows Stage 2 Economy Parity)

- 💰 **Desktop economy actions**: Windows now exposes the shared production economy through upgrade purchases, Prestige and Ultra resets, Prestige/Ultra upgrades, Nebula shop purchases and talent unlocks. Transactions validate affordability and prerequisites, preserve permanent progress across resets and save before updating the visible state.
- 🧪 **Economy parity coverage**: Added tests for exact upgrade costs, insufficient funds, maximum levels, Prestige/Ultra reset requirements and preserved state, duplicate shop purchases, talent prerequisites and bundled catalog loading.
- 📦 **Shared catalog data**: Bundled the existing Android upgrade and shop definitions with the Windows desktop app so the desktop catalog starts from the same game data.
- 🎁 **Daily quests and treasure drops**: Windows records daily guess/correct/streak progress, resets stale daily claims on a new day, lets players claim the four Android-matching daily rewards once per day, tracks common/rare/epic chest drops, and shows quest progress and chest counts in the Play screen.
- 🧮 **Reward parity fixtures**: Added deterministic coverage for stacked talent, event, mastery, relic-set, Home Base and secret bonuses, including Money/Nebula statistics.
- 🛍️ **Shop effects**: Windows now supports Nebula Crystal's 2× future Nebula rewards and Golden Guess Pass's critical-sized bonus on every tenth lifetime guess when that guess is correct. Unknown shop effects remain unavailable.
- 🏆 **Progression hooks**: Achievement milestones award their configured Nebula rewards once and update lifetime earned statistics. World unlocks check boss, correct-guess, upgrade, Prestige and Ultra requirements, with explicit unlock/select controls on Play.

## Unreleased (Windows Relic Gameplay Parity)

- 💎 **Relic gameplay parity**: Windows now presents the four canonical Android boss relics, uses the same relic IDs as saved gameplay state, allows all four relics to be equipped and surfaces the real 2-piece, 3-piece and 4-piece bonuses. The existing post-guess reward calculation applies the equipped relic effects, including the four-piece +1 Nebula reward.
- 🧪 **Relic set coverage**: Added tests for the catalog's canonical boss relic IDs, four-slot equipment limit and actual 2/3/4-piece gameplay reward effects.

## v3.0.3 (First-Run Tours & Update Reliability)

- 🧭 **Feature tours after navigation**: Eligible feature screens now render first, then show their own first-visit tour. Opening a feature from More no longer interrupts navigation with a generic popup first. Tour completion is saved per feature.
- 🌐 **Feature-aware tour titles**: Tour titles use the localized feature name where available, with localized tour controls and a delayed presentation after screen entry.
- 🐛 **Nebula reward formatting**: Fixed the locale-string overload bug that could leave `Reward: +%s Nebula` unformatted; added a regression test for a concrete reward value.
- 📦 **Installable channel updates**: About can discover the correct release/debug asset, download release APKs or debug ZIP archives, validate package identity and signing certificate, safely extract APK sets when needed, and pass compatible APKs to Android's installer.
- 🔐 **Update installation handling**: Added the scoped update cache path, install-permission flow, installer result receiver, archive size/path limits and signing metadata compatibility fixes.
- 🧪 **Build and test maintenance**: Fixed Compose named-argument errors and nullable signing metadata found by the first CI run.
- 🏷️ **App version**: versionCode 21 / versionName 3.0.3.

## Unreleased (Player Experience)

- 🧭 **More Hub feature discovery**: Selecting an unlocked feature now opens its destination immediately; the destination shows its localized first-run tour after rendering. Tour completion or dismissal is saved per feature, while Settings, About and developer/testing routes remain excluded.
- 🔊 **Independent SFX mixer foundation**: Added a separate sound-effects volume preference and a resilient asset lookup/player API. Missing `assets/sfx/` folders or clips return a not-found result without interrupting gameplay; no bundled sound assets are added in this phase.
- 🛍️ **Purchase feedback**: Successful Nebula shop purchases briefly highlight the purchased item, and successful upgrades highlight the upgrade card. Feedback respects reduced-motion preferences and uses the existing vibration setting.
- 🏆 **Queued achievement celebrations**: When several achievements unlock together, each announcement is shown in sequence instead of being overwritten. Achievement celebrations respect reduced motion and the existing vibration setting.
- 🎯 **Accurate guess feedback**: Keyboard and button submissions now share one handler, and rate-limited guesses show a localized retry message instead of misleading high/low/correct feedback. Feedback uses the same clamped number the game engine evaluates.
- 🎯 **Clearer guess feedback**: High, low, and correct guesses now use distinct icons and theme-aware feedback cards. Feedback transitions respect the reduced-motion setting, and Blindfolded Oracle hides feedback from both the Guess button and keyboard submission paths.

## Unreleased (Cross-Core Runtime Hardening)

- 💾 **Preserve recoverable saves**: Saving over a corrupted primary no longer overwrites a valid last-known-good backup.
- 💾 **Reject impossible save state**: Save validation now rejects out-of-range targets and negative economy/progression counters so recovery can prefer a valid backup.
- 🎵 **Bound music imports**: Imported tracks are capped at 100 MiB; empty, oversized, or failed imports are rejected and partial files are cleaned up.
- 🎵 **Lower idle progress polling**: Music progress refresh now polls less often while paused or idle.
- 🌐 **Locale resilience**: Locale catalog references are safely published across threads, and invalid currency codes fall back to a plain localized number instead of throwing.
- 🧪 **Regression coverage**: Added tests for recoverable save backups, invalid saved state, and invalid currency formatting.

## Unreleased (Post-v3.0.2 Development Updates)

- 🎨 **Live theme preview**: Settings now previews the selected primary, secondary, and tertiary colors in a sample game card and action button, updating immediately as theme colors change.

- ⚙️ **Clearer Settings sections**: Grouped notification controls, audio/feedback controls, and visual-comfort preferences under distinct localized headings and dividers, while preserving existing settings behavior.

- ⚙️ **Clearer Settings descriptions**: Added localized plain-language descriptions for language, theme appearance and colors, number notation, reminders, sound effects, vibration, visual comfort, reduced motion, master volume, and the destructive data-reset action across all supported locale catalogs.

- 🛡️ **LiveOps claim-state validation**: Reject reward claims when saved progression counters or permanent event boosts are negative, rather than allowing a corrupted state to be normalized during reward settlement.

- ♿ **Accessible progression maps**: World Map and Prestige Skill Tree nodes now expose localized names, lock/unlock status, costs where applicable, and selected state to accessibility services instead of relying on color, borders, or emoji alone.

- ♿ **Guidance and UI previews**: Added a TalkBack-labeled guidance button with hover/long-press tooltip support and preview fixtures for loading, empty, error, app-themed light/dark modes, and Filipino-localized states. Essential actions remain normal button clicks.

- 🎯 **Early progression goals**: Added live progress and reward spotlights for the existing Novice Guesser, Streak Master, and Number Millionaire milestones on the Play screen.
- 🧮 **Optional Math Strategy Lab**: Added a collapsible midpoint guide that can suggest a guess without submitting it automatically or revealing the hidden target.
- 🔒 **Progression-locked More Hub entries**: Relic Collection and Explorer's Codex remain visible until the first world boss is defeated; Endgame Challenges unlock after all four world bosses are defeated. Locked entries explain the requirements and current progress, and navigation enforces the same gates.
- 🔄 **Build-channel-aware update discovery**: The About screen can check GitHub Releases for release builds and recent successful Stable Debug APK workflow artifacts for debug builds. Results link to official GitHub pages; the app does not silently download or install APKs.
- 🌐 **Localization updates**: Added the new More Hub lock/requirements text across the supported locale catalogs.
- 🛠️ **Project organization**: Moved internal AI planning notes and roadmaps under `.openai/`; these internal documents are not part of the player-facing app.
- ℹ️ **Version status**: These are development updates on top of versionCode 20 / versionName 3.0.2. No app version bump is included in this changelog-only update.

## v3.0.2 (Winning-Streak Celebrations)

- 🎉 **Streak milestone celebrations**: Correct-guess celebrations now switch to an achievement-style banner at winning streaks of 5, 10, 25, and 50, reusing the existing localized achievement names.
- 🎮 **Version**: versionCode 20 / versionName 3.0.2.

## v3.0.1 (Streamlined More Hub)

- Removed the redundant Interactive Tutorial and Save Export / Import shortcuts from More. The action-driven tutorial remains available through its existing button action, and encrypted backup/restore remains on Save Slots.
- Removed the standalone tutorial and cloud-backup navigation destinations and their unused screen implementations.
- Existing saved More-menu orders automatically discard the retired shortcuts while preserving the user's remaining order.
- Version: versionCode 19 / versionName 3.0.1.

# Changelog

## v3.0.0 (Beyond the Numbers: Ultimate Expansion)

- 🌌 **Endless Rift**: After defeating all four world bosses, unlock a persistent, repeatable boss gauntlet. Each victory advances the tier, raises boss HP, and scales Money and Nebula rewards.
- ⚔️ **Escalating boss encounters**: Rift rematches use the existing phase-based guessing combat and miss penalties, with tier-scaled HP and rewards.
- 💾 **Save-compatible progression**: Added defaulted per-save Rift tier, best-tier and active-run fields so older saves load without migration.
- 🌍 **Integrated endgame flow**: Start or leave the Rift from Endgame Challenges, continue the current boss on the World Map, and see its scaled HP and current tier.
- 🌐 **Localization**: Added the Rift interface strings to all 23 supported locale catalogs.
- 📱 **Version**: Bumped to versionCode 18 / versionName 3.0.0.

# 🚀 Guess The Number Simulator - Changelog

## v2.0.0 (Beyond the Numbers Major Expansion)
- 🗺️ **Interactive World Map**: Added four selectable worlds, zoom and pan controls, connected routes, secret-area markers, unlock requirements, boss progress and world mastery upgrades.
- ⚔️ **Guessing-Based Boss Combat**: Boss battles now start from the map; correct guesses deal damage, and missed guesses push progress back every 4 misses in phase 1, every 3 in phase 2 and every 2 in phase 3, and victory grants one-time Money, Nebula, relic and Codex rewards.
- 🌳 **Zoomable Skill Tree**: Rebuilt the Prestige Talent Web as a connected, clickable tree with pinch-to-zoom, drag-to-pan, zoom controls, node selection and parent requirements.
- 💠 **Real Talent Effects**: Automation talents increase auto-clicker speed; precision talents add critical reward chances; Nebula Resonance boosts correct-guess earnings; Cosmic Oracle grants Nebula on correct guesses.
- 💎 **Relic Collection and Bonuses**: Bosses award persistent relics; equip up to two to gain Money bonuses, critical rewards and extra Nebula.
- 🏡 **Evolving Home Base**: Added persistent sanctuary levels, building milestones and a Money bonus that scales with restoration progress.
- 📖 **Explorer's Codex**: Added collectible world, boss and secret-area entries plus a one-time 100 Nebula completion reward.
- 🏆 **Endgame Goals**: Added long-term goals for boss victories, secret areas, relic collection and sanctuary restoration.
- 🧭 **Navigation Integration**: Added direct More-menu entry points for World Map, Relics, Home Base, Codex, Endgame, Cloud Backup, Lucky Stake and Tutorial; registered their navigation destinations.
- 🎮 **Play Hub Integration**: Added a live Beyond the Numbers card to Play with unlocked-world and defeated-boss counts plus direct World Map, Relics, Home Base and Codex shortcuts. The new hub labels and progress summary are present in all 23 locale files.
- 💾 **Save Compatibility**: Added new progression fields with defaults so older save data can load without a reset.
- 🌐 **Locale Coverage**: Added all new progression keys to the 23 locale JSON files. Filipino strings are translated; several newly added strings in other locales currently retain English values and need native-language review.
- 🚀 **Major Version**: Bumped versionName to 2.0.0 and versionCode to 17.

## v1.16.0 (Expressive Motion & Gameplay Feedback)
- 🌍 **World Progression**: Added four unlockable realms, each with explicit requirements based on correct guesses, upgrade levels, Prestige, Ultra, and previous boss victories.
- ⚔️ **Multi-Hit Boss Battles**: Added four bosses with persistent HP progress, increasing hit counts, one-time victories, and escalating Money + Nebula rewards.
- 🗝️ **World Gates**: Later realms require defeating the previous realm's boss plus account milestones; Nebula Rift requires Ultra progression.
- 🌐 **Full Localization**: Added world names, unlock requirements, boss names, battle requirements, rewards, and boss-health text across all 23 locale files.
- 🎁 **Treasure Drops**: Correct guesses can drop collectible Common, Rare, and Epic chests. Open stored chests for random Money or Nebula rewards, with rarer tiers offering bigger prizes.
- 📦 **Loot Inventory**: Added persistent per-save chest counts, total drops found, opened-chest stats, and a latest-drop banner on the Play screen.
- 🌐 **Full Locale Coverage**: Added all treasure-drop strings to all 23 locale JSON files.

- 🎯 **Daily Bounty Board**: Added four rotating-by-day contracts for guesses, correct answers, streaks, and upgrade purchases, each with claimable rewards.
- 💎 **Reward Loop**: Daily contracts award Money or Nebula, track progress inside each save slot, and reset their progress and claims when the local calendar day changes.
- 🌐 **Bounty Localization**: Added English and Filipino text for the new board, quest progress, rewards, and claim states.

- 🌀 **Expressive Save Loading**: Added a pulsing save icon, indeterminate progress ring, and animated loading dots tied to the real save-switch loading state.
- 🎮 **Gameplay Feedback**: Added animated currency and guess feedback, a morphing guess button, tactile press responses, and a celebration banner for correct guesses.
- ✨ **Progression Celebrations**: Added event-driven banners for achievement unlocks, Prestige resets, and Ultra resets.
- 🧭 **Animated Screens & Navigation**: Expanded spring-based motion across save slots, upgrades, shop, achievements, arcade results, tutorial steps, crash recovery, and navigation transitions.
- ♿ **Reduced Motion Support**: New motion effects respect the existing reduced-motion preference.
- 📱 **Version**: Bumped to versionCode 16 / versionName 1.16.0.

## v1.15.0 (Expressive More Menu & Persistent Reordering)

- 🎨 **More Expressive Material 3 UI**: Expanded the More Hub's Material 3 Expressive treatment with richer surfaces, rounded shapes, drag elevation, animated selection feedback, and clearer visual hierarchy.
- ↕️ **Real More Menu Reordering**: Reordered shortcuts now control the actual More menu instead of only changing the preview list.
- 💾 **Persistent Default Order**: Added a stable default More shortcut order that is saved with game settings and automatically repairs missing or legacy entries.
- 🧭 **Reorder UX Polish**: Long-press drag-and-drop now clearly communicates that the new order is saved and reused by the menu.
- 🌐 **What's New Localization**: Moved the release popup copy fully into the locale catalog and changed its version text to a localized %s placeholder, so the popup no longer contains hardcoded release text in Kotlin.
- 🧪 **Localization Regression Coverage**: Added coverage for localized What's New formatting and updated release-version assertions.
- 📱 **Version**: Bumped to versionCode 15 / versionName 1.15.0.

## v1.14.0 (Global Localization & Internationalization Expansion)

- 🌍 **Global locale expansion**: Added 23 supported locale overlays covering English (US/UK), Filipino, Simplified/Traditional Chinese, Japanese, Korean, Spanish, French, German, Italian, Portuguese (Portugal/Brazil), Russian, Hindi, Indonesian, Thai, Vietnamese, Turkish, Polish, Ukrainian, Dutch, and Arabic.
- 🧩 **Locale fallback architecture**: Locale JSON files can now provide translated overlays while safely falling back to the complete English catalog for keys that are not yet translated.
- 🔢 **Locale-aware formatting**: Number, decimal, percent, currency, date, time, date-time, and duration formatting now follows the active locale.
- ↔️ **RTL foundation**: Arabic is registered as an RTL locale and locale metadata is available to the UI for future layout-direction enforcement.
- 📱 **Android per-app language support**: Declared the supported language configuration so Android 13+ can expose app-specific language preferences.
- ⚙️ **Expanded language picker**: Settings now exposes the complete supported locale list instead of only English and Filipino.
- 🔎 **Repository-wide JSON inspection**: Added automated validation for JSON assets and data-driven localization keys, including the game's JSON catalogs.
- 🧪 **Localization tests**: Added locale loading, fallback, formatting, and RTL unit coverage.
- 🛠️ **CI expansion**: Manual GitHub Actions now validates locale contracts, scans the localization surface, validates JSON assets, runs unit tests, runs lint, runs the full Gradle check, and uploads test/lint reports.
- 🧹 **De-hardcoding foundation**: Added repository-wide high-confidence localization auditing so remaining user-facing literals can be migrated without touching machine-readable IDs or internal schema values.
- 📱 **Version**: Bumped to versionCode 14 / versionName 1.14.0.

## v1.13.0 (Material 3 Expressive UI Overhaul)

- 🎨 **Unified Material 3 Expressive Design System**: Centralized expressive shapes, typography, theme roles, and reusable motion primitives across the app.
- 🌀 **App-Wide Expressive Motion**: Added spring-based screen entrance/exit motion and animated navigation selection states for a consistent Android interaction language.
- 🧭 **Expressive Navigation**: Bottom navigation icons now animate their selected state while preserving navigation and state restoration.
- 🎮 **Primary Screen Polish**: Play, Upgrades, Shop, and More now use the shared expressive screen-motion foundation without changing game logic.
- ♿ **Reduced Motion Support**: The existing reduced-motion setting now disables the new screen and navigation transitions instead of leaving the preference disconnected.
- 📐 **Adaptive UI Foundation**: Shared Material 3 shapes, typography, and motion primitives provide a consistent base for phone, foldable, tablet, and split-screen layouts.
- ⚡ **Performance-Oriented Motion**: Animation specs are centralized and reused rather than recreated independently across screens.
- 🔍 **Guideline Alignment Pass**: Preserved Material components, semantic color roles, custom theme picker, localization, save system, tutorial, and offline architecture.
- 📱 **Version**: Bumped to versionCode 13 / versionName 1.13.0.

## v1.12.1 (Global Preferences, Complete Encrypted Backups, Save Creation & Theme Customization)

- 💾 **Save Slots First Launch**: Devices with no valid saves now open the Save Slots screen first, with encrypted Import/Export available above the slot list.
- 🔐 **Complete Encrypted Backup**: Export now packages global App Preferences plus all 10 save slots and slot backups into one encrypted string; importing restores the complete package.
- 🌐 **Global App Preferences**: App settings are stored globally instead of inside individual game saves.
- 🔄 **Settings Migration**: Existing per-save settings receive a one-time OK confirmation, followed by an in-dialog migration/loading state before global migration completes.
- 🎮 **New Save Setup**: New saves can choose a name, icon, and permanent per-save difficulty during creation.
- 🧭 **Interactive Tutorial**: New saves now enter an action-driven tutorial requiring highlighted controls to be pressed before advancing.
- 🎨 **Theme Customization**: Theme presets now live in Settings as a polished dropdown, with Custom HSV/brightness controls, live preview, and hex entry for primary, secondary, and tertiary colors.
- ↕️ **Animated More Reordering**: More shortcut reordering stays locked until Reorder is pressed and now gives the dragged item lift, scale, elevation, and smoother motion.
- 🧭 **Guided In-App Tutorial**: Replaced the generic tutorial screen with an action-required coachmark flow that uses the real Play, Upgrades, More, and Settings screens. Tutorial completion remains global across save slots.
- 🛠️ **Developer JSON Editors**: Split raw preference editing into global App Preferences and Save Slots, with a Save 1–10 selector.
- 📱 **Version**: Bumped to versionCode 12 / versionName 1.12.1.


## v1.12 (Shop Categories, Cosmetics, Mutator Combinations & Challenge Builder)

**Ready to play? v1.12 gives you more ways to build your run your way.** Customize your shop, unlock cosmetics, stack mutators, and create your own challenges. Then head back to Play and see how far your build can go. 🎮✨

- 🛍️ **Shop Categories**: Reworked the Nebula Shop into filterable All, Automation, Boosts, and Cosmetics categories.
- ✨ **Cosmetics**: Added purchasable profile cosmetics with persistent ownership and equip selection.
- 🌐 **Cosmetic Localization**: Added English and Filipino names and descriptions for the new cosmetic catalog and shop controls.
- 🧩 **Mutator Combinations**: Mutators can now be activated together and their reward multipliers stack.
- 🎯 **Mutator Gameplay Effects**: Hardcore expands the active range, Hyper Speed accelerates the auto-clicker, Blindfolded Oracle hides directional feedback, and Heavy Taxation applies its penalty.
- 🛠️ **Challenge Builder**: Added custom challenge creation with selectable mutators, goal types, targets, calculated Nebula rewards, activation, and one-time claims.
- 🌐 **Challenge Builder Localization**: Added English and Filipino builder controls and status text.
- 🧪 **Mutator Tests**: Added unit coverage for stacked mutator rewards, Hardcore range expansion, and unknown-mutator sanitization.
- 🧪 **Phase 15 Validation**: Added final state validation and migration-oriented tests before the v1.12 build.

## v1.11 (Weekly Challenges, Random Events, Achievement Tiers & Combo / Frenzy)
- 📅 **Weekly Challenges**: Reworked the challenge system into rotating weekly sets with varied goal types and week-scoped claim IDs.
- 🎯 **Challenge Variety**: Added accuracy, streak, money, guess volume, Prestige, Ultra, Frenzy, and playtime objectives with different Nebula rewards.
- ⚡ **Random Events**: Added seven varied occasional events with cash, Nebula, streak, range, refund, and jackpot effects. Events are checked periodically rather than firing constantly.
- 🌐 **Random Event Localization**: Added English and Filipino event names and descriptions.
- 🏆 **Achievement Tiers**: Expanded the system to 27 achievements across Bronze, Silver, Gold, and Diamond tiers, with escalating Nebula rewards.
- 🌐 **Achievement Localization**: Added localized names and descriptions for the expanded catalog in English and Filipino.
- 🔥 **Combo Frenzy**: Consecutive correct guesses now unlock Frenzy tiers every 5-streak steps.
- 📈 **Frenzy Rewards**: Each Frenzy tier adds a 25% reward multiplier, capped at 5x, on top of the existing streak multiplier.
- ✨ **Live Frenzy Indicator**: The Play screen shows the active Frenzy multiplier once the streak reaches 5.
- 🌐 **Localized Frenzy UI**: Added English and Filipino Frenzy text.
- 🧪 **Frenzy Tests**: Added engine coverage for tier scaling and reward increases.

## v1.10 (Save Slot Expansion, Profiles, Difficulty & Backup Improvements)
- 💾 **10 Save Slots**: Expanded save management from 3 slots to 10 with dynamic slot handling and metadata support.
- 🎚️ **Per-Slot Difficulty**: Added Classic, Hard, and Extreme difficulty profiles at the save-slot level.
- 👤 **Save Profiles**: Added customizable profile names and icons with sanitized save metadata.
- 📋 **Slot Duplication & Backup Restore**: Added duplication into empty slots and backup restoration with protection against overwriting occupied slots.
- 🛠️ **About Screen License Fix**: Fixed open-source license author and license fields displaying `%s` instead of their actual values.
- 🧪 **Localization Validation**: Improved placeholder validation so literal percent signs in localized text are handled correctly.
- 🎮 **Difficulty Mechanics**: Classic, Hard, and Extreme now have distinct range sizes, reward multipliers, and incorrect-guess money penalties.
- 📈 **Difficulty-Aware Ranges**: Better Range upgrades now scale from each difficulty's own base range.
- 🔄 **Reset Consistency**: Prestige resets preserve the selected difficulty's range.
- 🧪 **Engine Tests**: Added coverage for difficulty penalties, rewards, and range behavior.
- 🤖 **Smarter Guessing Bot**: Reworked the auto-clicker to use feedback-driven binary search instead of reading the hidden target directly.
- 🧪 **Bot Tests**: Added coverage for feedback-based target finding and difficulty-sized ranges.

## v1.9 (Comprehensive Internationalization & Localization Overhaul)
- 🌐 **Full Locale & Internationalization Support**: Localized 100% of user-visible strings across all screens (Play, Prestige, Ultra, Upgrades, Shop, Arcade, More, Mutators, Achievements, Cloud Backup, Crash Recovery, Settings, Dev Settings) into `locales/en_us.json`.
- 💬 **Data-Driven Localized Assets**: Fully wired all JSON config assets (`upgrades.json`, `shop_items.json`, `achievements.json`, `prestige_shop.json`, `prestige_upgrades.json`, `ultra_shop.json`, `ultra_upgrades.json`, `minigames.json`, `mutators.json`, `challenges.json`, `talents.json`) and randomized feedback/hint messages through `LocaleManager`.
- 🧮 **Dynamic Math & Clue Analysis**: Upgraded guess feedback tips and math clues to dynamically inject real-time game state variables (midpoints, parities, deltas, interval bounds, moduli).
- 🛠️ **UI Polish & Architecture**: Unified all unused and secondary screens into the More Hub with polished Material 3 design consistency.

## v1.8 (Lucky Number Overhaul, 3 Save Slot System & Legacy Migration, Clipboard Encryption & Data-Driven Feedback)
- 🎲 **True Lucky Number Box Minigame**: Overhauled the Lucky Number Box arcade minigame so that exactly one random box is the winning jackpot, while other boxes reveal a miss, adding genuine risk and reward.
- 🔒 **Encrypted Clipboard Transfer**: Enhanced save export and import in Settings and Cloud Backup with robust XOR cipher combined with Base64 obfuscation and salt validation.
- 📊 **Variable & Process Inspector Expansion**: Updated the Process & Variable Inspector screen to display real-time target guess numbers, range bounds, and complete runtime state variables.
- 💬 **Data-Driven Randomized Guess Feedback**: Implemented data-driven JSON configuration (`guess_feedback_messages.json`) featuring 30 varied too-low and 30 varied too-high feedback messages.
- ✨ **Hidden Background Money Generation (`/s`)**: Polished passive income calculation and live rate indicator on the Play screen.
- ✨ **3 Distinct Save Slots System**: Implemented a comprehensive 3 save slot system (`Slot 1`, `Slot 2`, `Slot 3`) allowing users to manage separate game saves with independent progression, money, prestige, ultra currency, attempts, and timestamps.
- 🔄 **Legacy Save Migration & Safety Prompt**: Added automatic detection for legacy single-saves from prior versions. When opening the Save Slots screen, users are prompted to securely transfer their legacy progress into Slot 1. If declined or dismissed without confirmation, the app closes immediately (`System.exit(0)`) to preserve data integrity.
- 🛠️ **Save Slot Management UI**: Created a dedicated `SaveSlotsScreen` accessible from the More Hub, featuring slot summary cards, active slot highlighting, new game creation, individual slot resetting, and direct resume actions.

## v1.7 (Security & Integrity Hardening Pass)
- ✨ **Expressive Material 3 Detailed Statistics**: Redesigned the Stastistics Screen with Material Expressive design principles, featuring a prominent hero accuracy gauge card, financial metrics cards, active and best streak counters, reset metadata, session playtime duration, and icon-badged detail breakdown rows.
- 🛡️ **Authoritative Game Engine & Save Integrity**: Overhauled `GameEngine`, `AntiCheatService`, and `SaveManager` to strictly validate and sanitize all game state operations, preventing negative currency exploits, invalid upgrade levels, and corrupted BigNumbers.
- 🔒 **Robust Save Validation & Migration**: Implemented rigorous JSON validation rejecting tampered saves, impossible ranges, and corrupted timestamps.
- ⏰ **Precise Anti-Time-Travel & Rate Limiting**: Added backward-clock tampering detection with a 24-hour penalty while supporting normal forward time synchronization and preventing rapid input abuse without disrupting automation.
- 🧪 **Comprehensive Unit Test Suite**: Added comprehensive local unit tests covering guess resolution, reward calculations, criticals/streaks, upgrades, prestige/ultra, talents, mutators, challenge duplicate claims, save validation, and anti-time-travel.
- 🛠️ **Performance Fixes**: Fixed app freeze and ANR when selecting the **MAX** buy multiplier with uncapped upgrades (such as Reward Multiplier) by optimizing affordable level and cost calculations.
- ✨ **Material 3 Expressive & Organic Animations**: Overhauled the entire app UI with Google's Material Expressive design system, featuring organic spring-physics interactions, scale-down press effects, and smooth micro-animations.
- ✨ **Uncramped Currency & Stats Dashboard**: Redesigned the Play screen header with spacious, dedicated surface bubble cards for Prestige, Ultra, and Nebula metrics.
- ✨ **Settings Dropdowns & AMOLED Mode**: Converted theme appearance, number notation, and reminder frequency selector buttons into clean Material 3 dropdown menus, and added pure black AMOLED theme mode.
- ✨ **Money Generation Rate Display**: Added live money generation rate (`/s`) indicator right below the money balance on the Play screen.
- ✨ **Advanced Idle Automation & AI Guessing Bots**: Implemented autonomous background guessing bots with adjustable speed and auto-clicker optimization.
- ✨ **Prestige Talent Web & Mutators Polish**: Fixed UI rendering bugs (latex symbols fixed to clean multiplier icons like `×2`, `×3`, `×5`), added robust data-driven talent nodes (`talents.json`) with higher prestige costs, and fully integrated daily seeded challenges with Nebula rewards.
- ✨ **Adaptive Max-Buy Upgrades**: Enhanced upgrade scaling to support fluid `1x`, `10x`, `100x`, and `MAX` calculations based on current currency and max level caps.
- ✨ **Interactive Android Home Screen Widgets (Jetpack Glance)**: Implemented Glance-based app widgets (`GuessWidget`) supporting real-time currency display and quick-guess action triggers.
- ✨ **Combo Streaks & High-Stakes "Lucky Guess" Staking**: Added high-stakes 50/50 staking mode (`StakingScreen`) with streak multiplier bonuses.
- ✨ **Save Export/Import & Cloud Backup UI**: Created a dedicated Cloud Backup and encrypted save transfer UI (`CloudBackupScreen`).

## v1.5
- ✨ **Smart Number Formatting**: Added support for Scientific, Engineering, Standard Short Scale, and Alpha Suffixes number notations in Settings.
- ✨ **Quick-Buy Multipliers & Max-Buy**: Added `1x`, `10x`, `100x`, and `MAX` buy multipliers in UpgradeShop.
- ✨ **Offline Progress & Summary Modal**: Enhanced offline earnings calculation and summary modal on app startup.
- ✨ **Export/Import Save Backups**: Added Base64 encrypted save string export and import in Settings.
- ✨ **Audio & Haptic Fine-Tuning**: Added master volume sliders, sound effects, vibration, and reduce flashes toggles.
- ✨ **Markdown Changelog Parser**: Changelog dialog now parses and formats markdown headers and list items.
- ✨ **Developer Process & Logic Inspector**: Added dedicated developer process and variable inspection screen in Dev Settings.
- ✨ **Achievements & Perks Navigation**: Integrated achievements and active perks into the More navigation screen.

## v1.4
- ✨ **Scaling Reset Requirements**: Prestige and Ultra money requirements now scale up progressively with each reset (Prestige scales by $2.5\times$ and Ultra by $5.0\times$ per reset).
- ✨ **UI Bug Fixes & QoL**: Fixed cramped buy multiplier buttons in the upgrades screen and added current money balance display.
- ✨ **Prestige & Ultra Requirements & Reset Info**: Updated Ultra requirements to require both Money and 1,000 Prestige. Added explicit reset info text on Prestige and Ultra screens detailing reset costs, scaling, and reset contents.
- ✨ **Quick-Buy Multipliers (`1x`, `10x`, `100x`, `MAX`)**: Added interactive buy multiplier filter chips and multi-level cost scaling to the Upgrade screen.

## v1.3
- ✨ Added a welcoming startup loading screen with circular progress animation to safely catch up and validate save state on app launch.
- 🛠️ Fixed Dev Settings Save Editor error message ("Failed to apply invalid save json") when submitting invalid save JSON strings.
- 🎨 Converted dev settings action buttons (Run, Load Current Save, Apply Save JSON) into space-saving icon buttons.
- 📤 Added dedicated share crash logs icon button alongside copy support in the crash recovery screen.
- 📜 Enclosed the Dev Settings live logs terminal viewer in a fully independent, scrollable console box with color-coded syntax.

## v1.2
- 🛠️ **Essential Developer Commands**: Added robust new dev commands (`/timeskip`, `/max_upgrades`, `/unlock_all`, `/win`, `/speed`, `/stats`) along with autocomplete chips for rapid testing and debugging.
- 🔔 Added notification system with Android 13+ `POST_NOTIFICATIONS` runtime permission, WorkManager background reminders, and `BOOT_COMPLETED` reboot resilience.
- 🔄 Relocated Prestige and Ultra resets behind the More Hub with explicit Reset Role Hierarchy (1. Ultra, 2. Prestige).
- 🎓 Added interactive onboarding tutorial screen for first-time players.
- 💰 Guaranteed minimum +10 money reward on initial correct guesses.
- ⚙️ Reimplemented Developer Console as Developer Settings with a raw JSON save editor and clean log stream (`[13:56:30.000][GUESS][INFO] Example output.`).
- ⚙️ Enhanced Settings with master volume slider, sound/vibration toggles, reduce flashes mode, and checkmark confirmation for data reset.
- ⇄ Added smooth horizontal swiping between navigation tabs (Play, Upgrade, Shop, More).
- 🔒 Encrypted and obfuscated save export/import strings.
- 🎮 Fixed and expanded Arcade minigames to reward exactly 1 Nebula per win.
- ⬆️ Added more rich upgrades to `upgrades.json`.

## v1.0
- ✨ Implemented Material 3 UI across all screens, replacing emojis with polished Material Icons.
- 🎮 Added fully playable Arcade minigames with Nebula & Money rewards.
- ⬆️ Expanded Upgrade tree with rich data-driven upgrades.
- 🌸 Added Moe animations and spring-based bouncy visual feedback throughout gameplay.
- 🛡️ Integrated anti-cheat protection, save export/import, offline progression, and developer tools.
