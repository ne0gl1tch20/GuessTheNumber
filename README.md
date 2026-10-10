# Guess The Number (Incremental & Arcade Edition)

An advanced, feature-rich incremental and arcade Android game built with **Jetpack Compose** and **Kotlin**. Start with a simple number guess, then turn it into a full progression loop: build upgrades, chase streaks, trigger Frenzy, discover events, collect cosmetics, stack mutators, and create challenges that make your next run feel different.

**Why try it?** 🎮 You can play casually, optimize an automation build, hunt achievement tiers, or make a ridiculous custom mutator challenge. There is always another upgrade, reset, challenge, or combination waiting in the next run.

## 🧩 Planned Lua Scripting Layer

Lua is **planned, not implemented yet**. The Kotlin engine remains authoritative, and implementation is gated on a stable baseline with passing builds and tests. The plan covers reusable local scripts, temporary minigames, a constrained UI/game host API, shared developer-console diagnostics, and a future signed GitHub LiveOps bundle pipeline.

- [Lua implementation roadmap](docs/LUA_SCRIPTING_PLAN.md)
- [Draft Lua host API contract](docs/LUA_API_DESIGN.md)
- [GitHub LiveOps bundle delivery plan](docs/LIVEOPS_SCRIPT_BUNDLES_PLAN.md)
- [Reserved reusable scripts directory](app/src/main/assets/scripts/README.md)

These documents do not add a Lua dependency or enable script execution.

---

## 🚀 Key Features

### 1. **Core Gameplay & Incremental Mechanics**
- **Number Guessing / Incremental Engine**: Earn currency by guessing numbers, accelerated by fluid quick-buy multipliers (`1x`, `10x`, `100x`, `MAX`) and autonomous background guessing bots.
- **Upgrades & Progression**: Buy standard upgrades (`upgrades.json`), prestige upgrades (`prestige_upgrades.json`), ultra upgrades (`ultra_upgrades.json`), and interconnected Prestige Talent Web skill nodes (`talents.json`) via JSON configuration files.
- **Prestige & Ultra Systems**: Reset or ascend through multiple layers of prestige with scaling requirements and custom shops.
- **Weekly Challenges**: Rotating weekly challenge sets now mix accuracy, streak, money, guess volume, Prestige, Ultra, Frenzy, and playtime goals with week-scoped claim IDs and varied Nebula rewards.
- **Random Events**: Occasional varied offline-friendly events such as Cash Burst, Nebula Rain, Jackpot, Streak Surge, Range Shuffle, Tax Refund, and rare Cosmic Gifts.
- **Achievement Tier System**: Expanded the achievement catalog to 27 milestones across Bronze, Silver, Gold, and Diamond tiers with localized names/descriptions and escalating Nebula rewards.
- **Combo / Frenzy System**: Consecutive correct guesses build streaks, with Frenzy tiers adding escalating reward multipliers up to 5x.
- **Achievements & Challenges**: Track milestones, complete daily seeded challenges with Nebula rewards, and play high-stakes 50/50 staking mode (`StakingScreen`).
- **Arcade Minigames**: Integrated side activities and minigames (`minigames.json`) to keep gameplay engaging.

### 🎯 **What You Can Do Right Now**
- 🔥 **Build a streak and hit Frenzy**: Keep landing correct guesses to push your reward multiplier higher.
- 🧩 **Make your own challenge**: Combine mutators, choose a goal, set a target, and chase the Nebula reward.
- ✨ **Collect cosmetics**: Browse the shop by category and equip cosmetics as your collection grows.
- 🏆 **Climb achievement tiers**: Work through Bronze, Silver, Gold, and Diamond milestones.
- 🎲 **React to random events**: Keep playing and see what the next event throws at your run.
- 💾 **Keep multiple runs**: Use the expanded save-slot system for separate profiles and difficulty choices.

### 2. **Architecture & Tech Stack**
- **UI Toolkit**: 100% Jetpack Compose with Google's **Material 3 Expressive** design system, organic spring-physics micro-animations, and pure black AMOLED mode.
- **Navigation**: Jetpack Navigation Compose (`NavGraph.kt`, `Screen.kt`) with smooth horizontal swiping between tabs.
- **State Management**: Reactive state handling using `GameViewModel.kt`, Coroutines, and Flows, backed by a persistent `SaveManager.kt` supporting encrypted save export/import and cloud backup.
- **Data & Configuration**: JSON-driven configuration repository (`JsonConfigRepository.kt`) for items, upgrades, achievements, and challenges.
- **Custom Numeric Model**: Supports extremely large numbers using a robust custom `BigNumber` data model with multiple number notations (Scientific, Engineering, Short Scale, Alpha Suffixes).

### 3. **Utilities & Advanced Systems**
- **Anti-Cheat & Time-Travel Protection**: Protects game progression against unauthorized manipulation and time cheating.
- **Background Audio & Notifications**: Ambient background music manager and WorkManager-based notifications/reminders (`GameReminderWorker.kt`, `NotificationHelper.kt`).
- **Crash Recovery & Logging**: Comprehensive in-app crash handler (`AppErrorHandler.kt`) and a custom game logger (`GameLogger.kt`) with dedicated inspection screens.
- **Android Home Screen Widgets**: Glance-based app widgets (`GuessWidget`) supporting real-time currency display and quick-guess action triggers.
- **Developer Tools**: Feature-packed developer console with dev commands (`/timeskip`, `/max_upgrades`, etc.), raw JSON save editor, and process inspector.

---

## 📂 Project Structure

```text
com.jarrlyyy.guessthenumber/
│
├── data/
│   ├── audio/              # Background music manager
│   ├── crash/              # Error handler and crash recovery
│   ├── logger/             # Game logging and log entries
│   ├── notification/       # Notifications, receivers, and workers
│   ├── repository/         # JSON config repository
│   └── store/              # Save data manager
│
├── domain/
│   ├── command/            # Command executor system
│   ├── engine/             # Game engine and anti-cheat service
│   └── model/              # BigNumber and GameState models
│
└── ui/
    ├── navigation/         # Navigation graph and screen definitions
    ├── screens/            # Play, Shop, Prestige, Ultra, Arcade, Stats, Settings, etc.
    ├── theme/              # Material 3 colors and theme setup
    └── viewmodel/          # GameViewModel (core game logic & state)
```

---

## 🛠️ Getting Started

1. **Prerequisites**: Android Studio Jellyfish / Koala or later with Kotlin support and Android SDK 34.
2. **Open Project**: Open the root folder `GuessTheNumber` in Android Studio.
3. **Build & Run**: Sync Gradle and run the `app` module on an emulator or physical Android device (minSdk 26, targetSdk 35).
