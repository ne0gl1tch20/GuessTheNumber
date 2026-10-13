# Material 3 Expressive UI Overhaul Implementation Plan

Transform the current generic Android Jetpack Compose UI in **Guess The Number** into a vibrant, playful, and modern **Material 3 Expressive** design system. This overhaul introduces dynamic color themes, expressive typography, playful shapes, fluid motion/transitions, and component styling tailored for an engaging idle/clicker numerical game experience.

## User Review Required

> [!IMPORTANT]
> This plan focuses heavily on visual and architectural design upgrades using Jetpack Compose and Material 3 Expressive guidelines (vibrant color palettes, large structural containers, expressive cards, bottom navigation/rail adaptive patterns, and playful motion/haptics).

- **Dynamic Color Support**: We will enhance `Theme.kt` to support wallpaper-based Dynamic Color (Android 12+) with vibrant custom fallbacks optimized for an arcade/idle game vibe (vibrant neon/purple/emerald tones).
- **Expressive Components**: We will replace generic boxes and flat cards with rounded, playful shapes, staggered entry animations, and high-contrast badges/chips.

## Open Questions

- Should we introduce a theme toggle in settings allowing players to switch between "Midnight Arcade", "Cyber Neon", and "Classic Expressive" palettes? *(Defaulting to a rich, vibrant M3 Expressive theme with dynamic color support).*

## Proposed Changes

### Theme & Design System
#### [MODIFY] [Color.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/theme/Color.kt)
- Define expanded Material 3 Expressive color tokens (vibrant primaries, high-contrast containers, surface tint variants for arcade/idle elements).

#### [MODIFY] [Theme.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/theme/Theme.kt)
- Update `GuessTheNumberTheme` to incorporate dynamic coloring (`dynamicDarkColorScheme` / `dynamicLightColorScheme`), refined typography scales, and expressive shape schemes (extra-large rounded corners for primary cards, pill shapes for badges).

### UI Components & Screens
#### [MODIFY] [PlayScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/PlayScreen.kt)
- Redesign the core guessing and number generation interface with oversized expressive metric cards, tactile feedback buttons, and smooth spring animations for number increments.

#### [MODIFY] [ShopScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ShopScreen.kt)
- Apply Material 3 Expressive card layouts with elevated surface colors, clear progress indicators, and badge chips for upgrades.

#### [MODIFY] [ArcadeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ArcadeScreen.kt)
- Enhance mini-game tiles with expressive containers, lively grid alignments, and distinct interactive states.

#### [MODIFY] [NavGraph.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt)
- Incorporate smooth screen transition animations and ensure seamless navigation flow adhering to M3 Expressive motion guidelines.

## Verification Plan

### Automated Tests
- Build verification via Gradle (`app:assembleDebug`) to ensure all theme tokens and composables compile correctly.

### Manual Verification
- Deploy the app to an emulator or connected device and inspect the UI screens ([PlayScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/PlayScreen.kt), [ShopScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ShopScreen.kt), [ArcadeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/GuessTheNumber/app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ArcadeScreen.kt)) to verify the vibrant Material 3 Expressive styling, color vibrancy, and typography.
