# 🎮 GAMIFIED GUESS THE NUMBER SIMULATOR

Build a complete polished Android game combining **Guess the Number + incremental simulator progression**, inspired by Roblox-style upgrade/reset games but using completely original UI, assets and mechanics. Deliver a fully playable offline game, NOT a static UI mockup, fake UI, placeholder buttons or TODO-only systems.

## 🛠️ STACK

Use Kotlin + Jetpack Compose + Material 3 + AndroidX + Navigation Compose + DataStore + JSON + modern Android architecture. Support edge-to-edge, lifecycle-aware state, accessibility, adaptive portrait/landscape, phones/tablets/foldables, split-screen and efficient rendering. Use unidirectional state flow, immutable/stable state and avoid unnecessary recomposition.

## 🎯 CORE LOOP

**PLAY → EARN MONEY → BUY UPGRADES → PRESTIGE → REBUILD → ULTRA → REBUILD**
Guess a random number; show Too Low/Too High/Correct. Correct guesses award Money, apply multipliers and generate a new target. Add animations, sounds, haptics, streaks, combos, critical guesses, milestones and reward feedback.

Currencies: 💰 Money, ⭐ Prestige, 🌌 Ultra, ✦ Nebula. Use an arbitrary/huge-number decimal abstraction, NEVER floating-point for authoritative currency. Support + − × ÷ powers comparison min/max floor parsing formatting. Never allow NaN, Infinity, negative currency or overflow. Persist deterministically.

## 🔢 LARGE NUMBERS

JSON-driven canonical suffixes: `K,M,B,T,Qa,Qi,Sx,Sp,Oc,No,Dc,Ud,Dd,Td,Qad,Qid,Sxd,Spd,Ocd,Nod,Vg,UVg,DVg,TVg,QaVg,QiVg,SxVg,SpVg,OcVg,NoVg,Tg` with exponents from 1e3 through 1e93 respectively. Support aliases such as Vgn while keeping one canonical table. Unsupported exponents use scientific notation. Configurable precision, trim zeros and NEVER alter authoritative values. Support values beyond primitive floating-point ranges.

## 🎲 GAME ENGINE

Keep authoritative logic independent from Compose. Use seedable deterministic RNG with separate gameplay/minigame streams. Exact order: **input → validate → resolve guess → calculate reward → multipliers/streaks → milestones/achievements → persist → UI feedback**. Prevent double-submit and duplicate rewards.

## 📱 NAVIGATION

Bottom navigation: Play, Upgrade, Shop, More. Data-driven through JSON where practical.

## 🕹️ PLAY

Show Money, range, input, Guess, feedback, attempts, multipliers and bonuses. Animate rewards. Add first-run tutorial: guess → earn → upgrade → Prestige → deeper progression; allow skip/replay.

## ⬆️ UPGRADES

Create visual branching Upgrade Tree. Each upgrade has ID, name, description, level, max level, cost, effect, prerequisites and lock state. Include Reward Multiplier, Starting Money, Better Guess Range, Lucky Guess, Critical Reward, Auto Guess, Money Multiplier and Prestige Gain. Cost=`baseCost×growthRate^level`; JSON-driven.

## 🔄 PRESTIGE

Soft reset requiring configurable/scaling Money minimum. Preview reward. Reset configured normal progression, award Prestige and preserve permanent Prestige upgrades. Add permanent tree with Money multiplier, starting Money, Prestige gain and progression boosts.

## 🌌 ULTRA

Stronger reset requiring scaling Money minimum. Reset configured progression/Prestige progression, award Ultra and preserve Ultra tree. Reward scales smoothly with hard **2,000 Ultra maximum per reset**. Include Cosmic Growth, Money Boost, Prestige Boost and Eternal Growth.

## 🛍️ SHOP + ✦ NEBULA

Shop contains gameplay features, boosts and Arcade. Nebula is earned through Arcade/gameplay. **No real-money purchases or ads.** Initial Shop items cost 25 Nebula each, JSON-configurable. Purchases atomic; never spend unavailable currency.

## 🕹️ ARCADE

Minigames: Quick Guess, Reaction, Lucky Number, Memory and Number Rush. Track scores/bests/rewards/plays. Validate and cap rewards; prevent duplicate/infinite rewards. Deterministic seeds where appropriate.

## 🤖 AUTO-CLICKER

In-game Auto-clicker ONLY, never Android system accessibility automation. Automate eligible gameplay with configurable speed/efficiency/upgrades. Respect lifecycle/background limits and prevent duplicate ticks.

## 🏆 ACHIEVEMENTS + CHALLENGES

JSON-driven persistent achievements and optional offline Daily Challenges using deterministic date seeds. Prevent duplicate rewards; handle timezone/date changes.

## 💤 OFFLINE

Record validated last-save timestamp. On return calculate capped elapsed time and rewards through the same economy services, validate, apply once, save and show summary. Protect against absurd clock manipulation.

## ⚙️ SETTINGS + ABOUT

Settings: Music, Sound, Vibration, Notifications, Theme, Reduced Motion, Export/Import Save, Reset Data, About. About includes version, description, technologies and actual third-party/open-source licenses. Never invent licenses.

## 💾 SAVE SYSTEM

Use DataStore for progression/settings. Implement `SaveManager,SaveSerializer,SaveValidator,SaveMigration,SaveBackup,SaveTransaction`. Use schema versions, migrations, atomic updates, checksum/integrity validation, corruption detection, last-valid backup recovery and safe reset. Import/export validates untrusted data and previews before replacement; never partially apply invalid imports.

## 📦 JSON/DATA-DRIVEN

Use `assets/game/` JSON for currencies, economy, suffixes, upgrades, Prestige, Ultra, shop, minigames, achievements, challenges, navigation and assets. Validate schemas at startup with safe fallback/error reporting. New currencies/suffixes/upgrades/reset layers/minigames/commands/items should be addable without rewriting systems. Original assets only.

## 💰 ECONOMY

Centralize formulas in domain services. Configure rewards, multiplier order, critical chance, streak scaling, upgrade growth, Prestige/Ultra formulas, caps, prices, Nebula and offline rewards via JSON. Deterministic calculations and explicit rounding. Previews MUST use the same services as actual transactions. Track earned/spent totals.

## 🛡️ TRANSACTIONS + STATE

Every currency change, purchase, upgrade, reset, reward and developer command follows: **Request → Validate → Calculate → Requirements → Apply Atomically → Record Transaction → Persist → Emit Event**. Unique transaction IDs reject duplicates. UI never mutates authoritative state. `GameStateValidator` detects negative currencies, impossible levels/unlocks/resets/stats, duplicate rewards, malformed numbers and inconsistent state. Recover last valid snapshot. Treat saves/JSON as untrusted; reject malformed, oversized, negative, NaN, Infinity, overflow and impossible values.

## 🧵 UI THREAD + CONCURRENCY

The **main/UI thread must remain responsive at all times**. Compose UI code must only perform lightweight rendering/state observation and fast validation. NEVER perform blocking file I/O, DataStore operations, JSON parsing of large files, save/export/import work, license scanning, huge-number stress calculations, network calls, expensive simulations, image processing or long loops directly on the UI thread. Use structured Kotlin coroutines with appropriate dispatchers: `Dispatchers.Main` for UI state/events, `Dispatchers.Default` for CPU-heavy calculations, `Dispatchers.IO` for file/storage I/O. Use `withContext` correctly and never use `runBlocking` from UI code. Avoid `Thread.sleep` on Main. Use lifecycle-aware coroutine scopes and cancel work when screens/jobs are no longer needed. Prevent concurrent writes/race conditions using `Mutex`, serialized transactions or an equivalent safe mechanism. Never update Compose state from uncontrolled background threads; expose results through lifecycle-aware state flows. Keep expensive recomposition work out of composables. Add tests that detect accidental blocking operations and concurrent state corruption.

## 💥 CRASH HANDLING + RECOVERY

Implement centralized runtime error handling with `AppErrorHandler`, `CrashHandler`, `ErrorReporter` and safe recovery policies. Install a default uncaught-exception handler for unexpected crashes to capture sanitized diagnostic information, persist a small crash marker/report when possible and allow the next launch to detect the previous crash. NEVER attempt unsafe infinite restart loops or silently hide repeated fatal crashes.

Handle expected failures at boundaries: DataStore, JSON/config parsing, save migration, import/export, number parsing, command parsing, transactions, minigames and background jobs. Convert recoverable failures into typed errors and user-friendly UI messages instead of crashing. Use `Result`, sealed error types or equivalent where appropriate.

On corrupted configuration, use validated safe fallback/default configuration where possible and log the exact failure. On corrupted saves, recover the last valid snapshot/backup and clearly inform the player. On failed imports, reject the entire import without modifying the current save. On failed non-critical background tasks, log the failure and continue safely. Never catch `Throwable` broadly and ignore it; fatal errors must remain diagnosable. Do not use empty catch blocks.

Add a **Crash Recovery UI** shown after a detected previous crash: explain that the app recovered, provide options to continue from the last valid state, inspect diagnostics in Developer Mode or reset only if recovery fails. Normal players must not see sensitive stack traces. Developer builds may show sanitized stack traces/details.

Persist only minimal sanitized crash metadata such as app/build version, timestamp, screen/context, error type and correlation ID. Never store secrets, credentials, tokens or sensitive data. Add crash-loop protection: if repeated startup failures occur, fall back to safe initialization/configuration instead of repeatedly entering the same failing path.

## 👨‍💻 DEVELOPER MODE

Create `object DeveloperConfig { const val ENABLED=false }`. Prefer BuildConfig/build variants so debug enables it and release disables/excludes it. When disabled hide/reject developer tools. When enabled provide Roblox-style commands:
`/help /give money 1000000 /give prestige 100 /give ultra 50 /give nebula 500 /give all /set money 999999 /set currency <amount> /setlevel <upgradeId> <level> /set upgrade <id> <level> /multiply <currency> <amount> /unlock all /reset progression /reset prestige /reset ultra /completeguess /newnumber /autoclicker on|off /speed <value> /freeze /inspect /stats /state /save /reload /config reload /version /logs /logs clear /logs pause /logs resume /logs filter <category|level> /logs search <text> /logs export /crash_test`
Add aliases, history, hints, clear errors and confirmations for destructive commands. Commands MUST use normal game services and validation. `/crash_test` must exist only in debug/dev builds and intentionally test crash recovery without affecting release builds.

## ⌨️ COMMAND AUTOCOMPLETE

Generate autocomplete from registered definitions, never hard-code UI. Suggest commands, aliases, arguments, types, currencies, suffixes, upgrade IDs and on/off. Support partial matching, descriptions, hints, Tab, Up/Down + Enter and tap-to-insert. Implement `DevCommand,CommandRegistry,CommandParser,CommandExecutor,CommandAutocomplete`.

## 🔎 VERBOSE LOGGING + DEV CONSOLE LOGS

Create centralized `GameLogger`/`Logger` with categories `GAME,ECONOMY,GUESS,UPGRADE,PRESTIGE,ULTRA,NEBULA,SHOP,ARCADE,AUTOCLICKER,SAVE,JSON,COMMAND,STATE,NAVIGATION,UI,PERFORMANCE,ERROR,CRASH,TEST`. Log major state transitions, transactions, commands, save/load, validation, JSON/config loads, reward calculations, resets, purchases, upgrades, offline calculations, thread/dispatcher violations and errors. Each log includes timestamp, severity, category, event and sanitized details. NEVER log secrets, passwords, tokens, API keys or sensitive user data.

Support `TRACE,DEBUG,INFO,WARN,ERROR,FATAL`, filtering, bounded in-memory buffer and spam throttling. Verbose by default in debug/dev builds, minimized/disabled in release. Integrate with Android Logcat through the same abstraction. When Developer Mode is enabled, show a live **Developer Console Logs** panel with chronological logs, severity/category/event/message, expandable details, search/filter, pause/resume, clear, copy, export, auto-scroll toggle and bounded storage. Add correlation IDs for guesses, purchases, resets, saves, commands and crashes. Log transaction stages and sanitized exceptions with stack traces in debug. Log invalid state, corruption, migration failures, rejected transactions, duplicates, overflow/NaN/Infinity, clock anomalies, parser failures and unexpected crashes. Logs must never change game state or gameplay outcomes. Release builds must exclude developer console/log internals.

## 🧪 DEVELOPER PANEL

When enabled show currencies, upgrade levels, reset counts, guess state, multipliers, save state, JSON/config status, build/version, economy calculations, coroutine/job status and quick commands. Include test simulation tools. Keep developer code isolated.

## 📊 STATISTICS

Track guesses, correct/failed, best streak, highest multiplier, currencies earned/spent, resets, playtime, minigames, scores, achievements and challenges. Read-only in normal UI; update through domain events.

## 🎨 UI + ACCESSIBILITY

Material 3, rounded cards, hierarchy, animated counters, upgrade-tree connections, locked states, reward popups, confirmations and responsive layouts. Do NOT look like a calculator/business app. Support TalkBack, content descriptions, keyboard navigation, large fonts, minimum touch targets, color-independent feedback, contrast, reduced motion and accessible dialogs. Error/crash messages must be understandable and actionable without exposing internal stack traces.

## 🧱 ARCHITECTURE

`UI → ViewModel → Game/Domain → Repository → DataStore/JSON`. Separate state, economy, currencies, upgrades, Prestige, Ultra, Shop, minigames, achievements, challenges, commands, saves, settings, logging, crash handling and navigation. Use `CurrencyRegistry,UpgradeRegistry,ResetLayerRegistry,ShopRegistry,MinigameRegistry,AchievementRegistry,ChallengeRegistry,CommandRegistry,NavigationRegistry,NumberFormatRegistry`. Avoid giant Compose files.

## ⚡ PERFORMANCE + RELEASE

Optimize Compose state/recomposition, lazy lists, rendering, startup and lifecycle. Minimize background/battery work, prevent leaks, handle configuration/process recreation. Detect Main-thread blocking. Release builds expose no developer tools, verbose logs or test crash commands.

## 🧪 TESTING

Test guessing/reward order, formulas, costs, reset requirements/rewards, 2,000 Ultra cap, Nebula rewards, resets, huge numbers, every suffix boundary, scientific fallback, overflow/NaN/Infinity, transactions, duplicate events, save/load/corruption/migrations/import/export, offline progression, achievements/challenges, JSON validation, commands/autocomplete, rapid input, process recreation, accessibility, large fonts/adaptive layouts, logger levels/filters/buffer/export, correlation IDs, background concurrency, cancellation, Main-thread blocking, crash handling, recovery, corrupted startup state, repeated crashes and release-build developer-feature exclusion. Run stress simulations with thousands of guesses/purchases/resets/minigames and assert no invalid state.

## 🔍 IMPLEMENTATION AUDIT

Before completion trace every visible button, screen, route, currency, suffix, upgrade, reset, shop item, minigame, achievement, challenge, setting, save operation and developer feature from UI → ViewModel → domain → persistence. Verify every asynchronous operation uses an appropriate dispatcher and every failure path has handling. Remove dead code, fake data, placeholders and TODO-only functionality. Every visible feature MUST work.

## 📌 PROJECT MEMORY

Always create and maintain `MEMORY.md` in the project root. Store current state, architecture, major features, implementation decisions, known issues, TODOs and continuation context. **Read MEMORY.md before significant changes and update it whenever the project meaningfully changes.** Never store secrets, API keys, passwords, tokens or sensitive credentials.

## 🚀 ACCEPTANCE

Complete only when: clean build compiles; APK installs/launches; game works fully offline; Play→Money→Upgrade works; Prestige/Ultra reset and preserve correctly; Ultra never exceeds 2,000/reset; progression persists; Arcade validates Nebula; Shop is atomic; Auto-clicker works only in-game; achievements/challenges persist without duplicates; offline progression is capped; settings/import/export work; corrupted saves recover; JSON validation/fallback works; developer commands/autocomplete/logging work only in enabled dev builds; crash recovery works; invalid transactions cannot corrupt state; no blocking work runs on Main; huge numbers/formatting work; accessibility/adaptive layouts work; no fake buttons/mock systems/unfinished screens remain.

## 🚀 FINAL RESULT

A new player can **Play → earn Money → upgrade → Prestige → use Prestige tree → reach Ultra → Ultra reset → use Ultra tree → earn Nebula in Arcade → unlock achievements/challenges → buy Shop items → use Auto-clicker → configure Settings → view About/licenses.** Prioritize stable gameplay first, then polish. Deliver a complete compilable, installable, playable offline game, NOT a prototype or UI demonstration.
