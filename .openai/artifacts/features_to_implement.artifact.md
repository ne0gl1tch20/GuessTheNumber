# Quality of Life (QoL) and Logic Features Proposal

This document outlines recommended quality of life (QoL) enhancements and deep logic features for **Guess The Number**, taking into account its existing rich mechanics (idle upgrades, prestige systems, minigames, dev console, big numbers, and custom JSON configs).

---

## 1. Quality of Life (QoL) Improvements

### A. Smart Number Formatting & Notation Toggles
- **Description:** While `BigNumber` handles massive numbers, players often enjoy different formatting styles.
- **Features:**
  - Allow players to toggle between **Scientific Notation** ($1.23 \times 10^{45}$), **Engineering Notation**, **Standard Short Scale** (Million, Billion, Trillion, Quadrillion, Quintillion), and **Custom Alpha Suffixes**.
  - Add a "Compact / Detailed" view toggle in [SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt) to prevent UI clutter on high-tier prestige runs.

### B. Quick-Buy Multipliers & Max-Buy Buttons
- **Description:** Managing upgrades in [UpgradeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/UpgradeScreen.kt), Prestige shops, and Ultra shops can become tedious when currency scales exponentially.
- **Features:**
  - Add buy multipliers: `1x`, `10x`, `100x`, `MAX`, and a toggleable `Percentage of Currency` (e.g., spend 25% or 50% of current currency).
  - Add a **"Max Buy All Available"** button for non-scaling utility upgrades to speed up late-game progression.

### C. Advanced Notification & Offline Progression Summary Dialog
- **Description:** The game already includes [GameReminderWorker.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/data/notification/GameReminderWorker.kt) and save management.
- **Features:**
  - When returning from an extended absence, display a detailed **Offline Earnings & Progress Modal** breaking down:
    - Total time away.
    - Currency generated via idle multipliers.
    - Minigame tickets accumulated.
    - Any milestones reached while offline.
  - Granular notification settings (toggle streak reminders, prestige milestone alerts, and full-energy notifications).

### D. Cloud Saves & Local Export/Import JSON Backups
- **Description:** Deep incremental and prestige games require robust save protection.
- **Features:**
  - Add an export/import save string feature (Base64-encoded JSON) in [SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/SettingsScreen.kt) so players can back up or transfer progress across devices.
  - Optional Google Play Games Services (GPGS) cloud save integration for automatic synchronization.

### E. Haptic Feedback & Audio Fine-Tuning
- **Description:** [BackgroundMusicManager.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/data/audio/BackgroundMusicManager.kt) handles audio.
- **Features:**
  - Introduce contextual haptic feedback (subtle ticks on number guessing, strong vibrations on critical hits, prestige resets, or minigame jackpots).
  - Independent volume sliders for SFX, Background Music, and UI interaction clicks.

### F. Quick Stat Dashboard & Run History Analytics
- **Description:** Giving players insight into their current run efficiency and lifetime statistics.
- **Features:**
  - A dedicated stats panel tracking: average guesses per correct number, fastest solve time, total prestige resets, highest combo multiplier, and currency earned per minute.
  - Graph/chart visualization of currency growth over the last 24 hours.

### G. Interactive Home Screen / Lock Screen Widgets (Streak & Summary Widgets)
- **Description:** Bringing core metrics right to the Android desktop or lock screen to keep players engaged without needing to open the app.
- **Features:**
  - **Streak & Progress Widget:** Displays current active guessing streak, daily challenge status, and current idle currency generation rate at a glance.
  - **Quick Summary Widget:** Shows quick stats (total prestige level, active multipliers, time until next milestone) with a quick-launch shortcut to dive straight back into a run.

---

## 2. Advanced Game Logic & Progression Features

### A. Dynamic Challenge Mode (Mutators & Daily Seeded Runs)
- **Description:** Expanding beyond the standard guessing loop and current arcade/minigame setup ([ArcadeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ArcadeScreen.kt)).
- **Features:**
  - **Daily Seeded Challenge:** Every player gets the exact same random number generation seed and ruleset each day. Compete on a local/global efficiency score (fewest guesses / fastest time).
  - **Challenge Mutators:**
    - *Blindfolded:* The game only tells you "Warmer / Colder" instead of "Higher / Lower".
    - *Limited Fuel:* You only have a fixed number of guesses before a hard reset.
    - *Taxes:* Each guess drains a small percentage of your currency.

### B. Synergy & Set Bonuses for Upgrades
- **Description:** Deepening the upgrade tree in [upgrades.json](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/assets/game/upgrades.json) and [GameViewModel.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/viewmodel/GameViewModel.kt).
- **Features:**
  - Introduce **Upgrade Sets** (e.g., owning Tier 1 through 5 of "Probability Boosters" unlocks a permanent synergy bonus like `+50% Idle Income`).
  - Milestone perks: Reaching level 25, 50, 100 on an upgrade unlocks unique passive modifier slots (e.g., chance to refund a guess, chance for double currency drop).

### C. Prestige Skill Tree / Talent Web
- **Description:** Complementing [PrestigeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/PrestigeScreen.kt) and [UltraScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/UltraScreen.kt).
- **Features:**
  - Move from linear prestige upgrades to a 2D **Talent Tree** with branches:
    - **Path of Precision:** Better rewards for guessing in fewer attempts.
    - **Path of Abundance:** Massive multipliers to idle generation and offline earnings.
    - **Path of Fortune:** Enhanced minigame drop rates and critical guess probabilities.

### D. Achievement Milestones with In-Game Perks
- **Description:** Leveraging [achievements.json](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/assets/game/achievements.json).
- **Features:**
  - Make achievements active rather than cosmetic: unlocking achievements grants permanent passive multipliers or unique cosmetic themes (e.g., Matrix theme, Neon Arcade theme, Retro 8-bit theme).
  - Tiered achievements (Bronze, Silver, Gold, Diamond) with scaling rewards.

### E. Automated Guessing Bots (Idle Automation)
- **Description:** Perfect for late-game incremental scaling.
- **Features:**
  - Introduce an **AI Guess Bot** upgrade that automatically makes calculated guesses every few seconds based on binary search logic.
  - Bot efficiency upgrades (speed, accuracy, and logic tier) so players can let the game idle-solve numbers while focusing on prestige progression and minigames.

### F. Combo Streaks & "Hot Streak" Multipliers
- **Description:** Rewarding consistent accurate guessing without wasteful guesses.
- **Features:**
  - Guessing numbers within fewer attempts than the median builds a **Streak Multiplier** (up to 10x currency rewards).
  - Risk/reward "Lucky Guess" mode: Stake a portion of your current currency on guessing the exact number within 3 tries for a massive payout.

---

## Summary Recommendation for Implementation Order
1. **QoL First:** Quick-Buy multipliers (`10x`/`MAX`), Offline Progress Summary Modal, Save Export/Import, and **Streak & Summary Home Screen Widgets**.
2. **Logic & Replayability:** Daily Seeded Challenges, Prestige Skill Tree, and Combo Streaks.
3. **Immersion:** Haptic feedback, customizable number formatting, and thematic achievement unlocks.
