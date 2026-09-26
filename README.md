# Guess The Number (Incremental & Arcade Edition)

An advanced, feature-rich incremental and arcade Android game built with **Jetpack Compose** and **Kotlin**. Beyond a simple number-guessing mechanic, this app incorporates deep incremental layers (upgrades, prestige, ultra prestige, achievements, minigames, challenges, and arcade modes), extensive logging, audio management, and developer tools.

---

## 🚀 Key Features

### 1. **Core Gameplay & Incremental Mechanics**
- **Number Guessing / Incremental Engine**: Earn points/currency by guessing numbers or progressing through incremental multipliers and automation.
- **Upgrades & Progression**: Buy standard upgrades (`upgrades.json`), prestige upgrades (`prestige_upgrades.json`), and ultra upgrades (`ultra_upgrades.json`) via JSON configuration files.
- **Prestige & Ultra Systems**: Reset or ascend through multiple layers of prestige to unlock powerful multipliers and custom shops.
- **Achievements & Challenges**: Track milestones and complete specific challenges to earn rewards.
- **Arcade Minigames**: Integrated side activities and minigames (`minigames.json`) to keep gameplay engaging.

### 2. **Architecture & Tech Stack**
- **UI Toolkit**: 100% Jetpack Compose with Material 3 design and extended Material icons.
- **Navigation**: Jetpack Navigation Compose (`NavGraph.kt`, `Screen.kt`).
- **State Management**: Reactive state handling using `GameViewModel.kt`, Coroutines, and Flows, backed by a persistent `SaveManager.kt`.
- **Data & Configuration**: JSON-driven configuration repository (`JsonConfigRepository.kt`) for items, upgrades, achievements, and challenges.
- **Custom Numeric Model**: Supports extremely large numbers using a robust custom `BigNumber` data model.

### 3. **Utilities & Advanced Systems**
- **Anti-Cheat Service**: Protects game progression against unauthorized manipulation.
- **Background Audio & Notifications**: Ambient background music manager and WorkManager-based notifications/reminders (`GameReminderWorker.kt`, `NotificationHelper.kt`).
- **Crash Recovery & Logging**: Comprehensive in-app crash handler (`AppErrorHandler.kt`) and a custom game logger (`GameLogger.kt`) with dedicated inspection screens.
- **Developer Tools**: Feature-packed developer console, settings, and process inspector for testing and debugging.

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
