# Joy Phase Roadmap

**Last updated:** 2026-10-12  
**Planned scope:** 23 focused PRs (#24–#46), subject to adjustment after implementation and test results. The C++/NDK phase is planned after the joy, math-strategy, Lua localization, and LiveOps locale work; build-channel-aware updating follows as a separate delivery-system task.

The goal is to make GuessTheNumber feel more rewarding and lively while growing it beyond random guessing into number sense and efficient mathematical strategy, without destabilizing existing gameplay, saves, localization, or offline behavior. Keep changes focused and validate each PR before continuing.

| PR | Focus | Planned work |
|---|---|---|
| #24 | Winning-streak celebrations | Give existing correct-guess celebrations a stronger payoff at streaks of 5, 10, 25, and 50. **Merged.** |
| #25 | High/low feedback | Make higher/lower clues clearer and more satisfying while retaining the existing guessing rules. |
| #26 | Correct-guess feel | Improve the feedback and motion around a correct guess using existing UI patterns and reduced-motion preferences. |
| #27 | Money and upgrades | Give purchase success, affordability, and upgrade progress clearer feedback without changing reward balance. |
| #28 | Prestige, Ultra, Nebula | Celebrate existing progression milestones without altering reset or reward logic. |
| #29 | Achievement unlocks | Improve achievement unlock presentation while preserving existing achievement definitions and rewards. |
| #30 | Random events | Make existing random-event announcements more distinctive and readable without changing event odds or effects. |
| #31 | Early-game goals | Improve discoverability and feedback for existing early-game goals and progression; avoid unnecessary new systems. |
| #32 | Math strategy and number mastery | Build on existing high/low clues and dynamic math analysis so players learn to narrow ranges efficiently using interval bounds, midpoints, parity, deltas, and other appropriate number patterns. Add optional guided examples and challenges that reward fewer guesses or efficient reasoning without revealing the hidden target, forcing school-style tests, or changing core fairness/reward rules. Localize every explanation and validate the math against the actual guessing rules. |
| #33 | More Hub unlock requirements | Make progression-based More Hub features unlock through clear, testable requirements. Prefer visible locked entries with a lock state; tapping one while locked opens a localized requirements panel showing each unmet condition and current progress. Hide only entries that are debug-only or explicitly disabled/unavailable, not ordinary progression-locked content. Recheck requirements at navigation/action time and keep feature flags separate from player progression unlocks. Preserve saved shortcut order and never expose debug-only routes in release. |
| #34 | Lua activity localization | Use approved locale keys and arguments for Lua minigame titles, descriptions, choices, and feedback. Inspect the current Lua engine, payload validator, and locale resolver before finalizing the payload contract. |
| #35 | Lua random-event localization | Route player-facing Lua random-event messages through the same controlled localization resolver. Do not leave hardcoded player-facing text in scripts. |
| #36 | Localization verification | Test supported locales, language switching, formatting arguments, missing keys, malformed payloads, and defined fallback behavior. |
| #37 | Temporary LiveOps locale packs | Define the locale-pack schema and integrate delivery of temporary translation data for limited-time activities, without requiring an APK release for each translation update. |
| #38 | Locale-pack lifecycle and recovery | Validate packs, enforce allowed keys and size limits, cache accepted packs, handle versions and expiry, reject malformed content, and fall back to built-in locales when packs are unavailable or expired. |
| #39 | Final joy/localization regression review | Review gameplay feel, locale coverage, Lua boundaries, LiveOps failure/offline behavior, save compatibility, and regression tests before native-engine work. |
| #40 | Native engine architecture and baseline | Audit the current Kotlin/Lua engine boundaries, define a narrow C++/NDK responsibility, capture baseline tests and performance measurements, and document ABI/32-bit compatibility requirements before adding native code. |
| #41 | NDK build and native test scaffold | Add the smallest maintainable CMake/NDK setup and native test path; keep Kotlin-only builds and existing app behavior working. No gameplay migration in this step. |
| #42 | Minimal JNI bridge | Add a small, versioned JNI interface with input validation, explicit error handling, and Kotlin fallback behavior. Do not expose save files, UI, network access, or unrestricted reward mutation to native code. |
| #43 | Port one bounded engine component | Move only one well-defined, deterministic computation into C++ after baseline evidence supports it. Keep Kotlin as the source of truth for game state, persistence, eligibility, and rewards. |
| #44 | Kotlin/native parity and failure tests | Compare Kotlin and native outputs for the selected component, test edge cases, malformed inputs, JNI failures, and fallback behavior across supported ABIs. |
| #45 | Native integration regression and performance review | Verify Debug/Release builds, device ABI coverage, gameplay/save compatibility, Lua sandbox boundaries, offline behavior, and measured performance. Keep the native path only if it is stable and provides a justified benefit. |
| #46 | Build-channel-aware updates and About menu | Add an About-menu update section that identifies the installed build channel and checks the matching update source. **Production/release builds** check GitHub Releases and offer compatible production releases only. **Debug builds** check the latest designated stable Debug APK published through GitHub Actions artifacts, not production Releases. Detect the channel from trusted build configuration rather than a user-editable setting; never offer a debug APK to a production build. Show installed version/channel, available version, release notes and a clear install action. Handle artifact expiry, API/download access, rate limits, missing metadata, incompatible versions, interrupted downloads and offline use gracefully. Validate checksums/signatures where the publishing pipeline provides trusted verification, and test both build variants without making updates mandatory. |

## Temporary LiveOps locale pack design

LiveOps may deliver **translation data only**, for example locale ID, pack ID, version, expiry, and a map of approved translation keys to localized strings. This data must not be treated as executable Lua or remotely downloaded code.

Expected flow:

1. LiveOps supplies a versioned temporary locale pack.
2. Kotlin validates its schema, locale, approved key set, size, version, and expiry.
3. The app activates and caches the accepted pack.
4. Lua scripts return approved localization keys and formatting arguments; Kotlin resolves them using the active locale and valid downloaded pack.
5. If a pack is missing, invalid, unavailable offline, or expired, the app uses the built-in locale and existing fallback rules. Built-in translations are never overwritten.

**Installed-app requirement:** the downloader, validator, cache/expiry handling, and locale resolver must already exist in the installed app. LiveOps cannot add these capabilities to an older APK on its own.

## Architecture and acceptance rules

- Kotlin remains authoritative for locale resolution, payload validation, lifecycle, persistence, eligibility, and rewards.
- Lua remains sandboxed and must not gain direct reward/save mutation capabilities.
- Preserve existing save compatibility, reward rules, claim limits, and offline play.
- Every player-facing string, including Lua activity and random-event text, must use the localization system.
- Test language switching, formatting arguments, missing/malformed translations, pack expiry, corrupted packs, offline startup, and fallback behavior.
- Build updates must be channel-aware: production uses GitHub Releases; debug uses the designated stable Debug APK from GitHub Actions artifacts. Keep release and debug update feeds separate and identify the active channel in About.
- Define how the stable debug artifact is selected and retrieved, including artifact retention/expiry, metadata, version compatibility, access limits, and integrity verification. Do not assume an Actions artifact is permanent or anonymously downloadable.
- Test update discovery and failure cases for both Debug and Release variants. Updating must remain optional and offline gameplay must continue to work.
- Keep each PR focused. Avoid unrelated dependencies and scope expansion.
- Do not claim a PR is merged or checks passed without verifying GitHub status. Workflows are manual-only unless explicitly requested.

## Planned C++/NDK engine integration (#40–#45)

The project will plan a staged native-engine integration rather than rewriting the whole app in C++. Kotlin remains the application and gameplay authority. C++/NDK is intended for a narrowly scoped engine computation with a clear reason to be native; it must not become a second source of truth for game state.

- Start with an architecture audit and Kotlin baseline tests/measurements.
- Add CMake/NDK scaffolding independently from gameplay changes.
- Introduce a small JNI boundary with validated inputs, explicit errors, and a Kotlin fallback.
- Port one bounded deterministic component at a time, after confirming the selected boundary fits the existing engine.
- Add parity, ABI, failure-path, and regression tests before relying on native results.
- Keep persistence, UI, localization, LiveOps networking, eligibility, and authoritative reward settlement in Kotlin.
- Preserve the Lua sandbox. Lua must not gain direct JNI/native access or unrestricted save/reward mutation.
- Native code must not be described as a performance improvement without measurements. If profiling does not justify a native component, document the result and keep the integration minimal rather than adding complexity for its own sake.
- Verify supported ABIs and the actual NDK/CMake configuration before implementation; do not assume a particular ABI is available.
