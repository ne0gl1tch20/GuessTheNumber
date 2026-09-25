# 🚀 Guess The Number Simulator - Changelog

## v1.3 (Current Release)
- ✨ Added a welcoming startup loading screen with circular progress animation to safely catch up and validate save state on app launch.
- 🛠️ Fixed Dev Settings Save Editor error message ("Failed to apply invalid save json") when submitting invalid save JSON strings.
- 🎨 Converted dev settings action buttons (Run, Load Current Save, Apply Save JSON) into space-saving icon buttons.
- 📤 Added dedicated share crash logs icon button alongside copy support in the crash recovery screen.
- 📜 Enclosed the Dev Console live logs terminal viewer in a fully independent, scrollable console box with color-coded syntax.

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
