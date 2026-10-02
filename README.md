# Guess The Number (Incremental & Arcade Edition v1.7)

An advanced, feature-rich incremental and arcade Android game built with **Jetpack Compose** and **Kotlin**. Beyond a simple number-guessing mechanic, this app incorporates deep incremental layers (upgrades, prestige, ultra prestige, achievements, minigames, challenges, and arcade modes), extensive logging, audio management, and developer tools.

---

## 🚀 Key Features

### 1. **Core Gameplay & Incremental Mechanics**
- **Number Guessing / Incremental Engine**: Earn currency by guessing numbers, accelerated by fluid quick-buy multipliers (`1x`, `10x`, `100x`, `MAX`) and autonomous background guessing bots.
- **Upgrades & Progression**: Buy standard upgrades (`upgrades.json`), prestige upgrades (`prestige_upgrades.json`), ultra upgrades (`ultra_upgrades.json`), and interconnected Prestige Talent Web skill nodes (`talents.json`) via JSON configuration files.
- **Prestige & Ultra Systems**: Reset or ascend through multiple layers of prestige with scaling requirements and custom shops.
- **Achievements & Challenges**: Track milestones, complete daily seeded challenges with Nebula rewards, and play high-stakes 50/50 staking mode (`StakingScreen`).
- **Arcade Minigames**: Integrated side activities and minigames (`minigames.json`) to keep gameplay engaging.

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
3. **Build & Run**: Sync Gradle and run the `app` module on an emulator or physical Android device (minSdk 26, targetSdk 34).
