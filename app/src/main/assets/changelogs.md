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

## Unreleased (Windows Relic Gameplay Parity)

- 💎 **Relic gameplay parity**: Windows now presents the four canonical Android boss relics, uses the same relic IDs as saved gameplay state, allows all four relics to be equipped and surfaces the real 2-piece, 3-piece and 4-piece bonuses. The existing post-guess reward calculation applies the equipped relic effects, including the four-piece +1 Nebula reward.
- 🧪 **Relic set coverage**: Added tests for the catalog's canonical boss relic IDs, four-slot equipment limit and actual 2/3/4-piece gameplay reward effects.
