# Joy Phase Roadmap

**Last updated:** 2026-10-12  
**Planned scope:** 14 focused PRs (#24–#37), subject to adjustment after implementation and test results.

The goal is to make GuessTheNumber feel more rewarding and lively without destabilizing existing gameplay, saves, localization, or offline behavior. Keep changes focused and validate each PR before continuing.

| PR | Focus | Planned work |
|---|---|---|
| #24 | Winning-streak celebrations | Give existing correct-guess celebrations a stronger payoff at streaks of 5, 10, 25, and 50. **Current PR; open at time of writing.** |
| #25 | High/low feedback | Make higher/lower clues clearer and more satisfying while retaining the existing guessing rules. |
| #26 | Correct-guess feel | Improve the feedback and motion around a correct guess using existing UI patterns and reduced-motion preferences. |
| #27 | Money and upgrades | Give purchase success, affordability, and upgrade progress clearer feedback without changing reward balance. |
| #28 | Prestige, Ultra, Nebula | Celebrate existing progression milestones without altering reset or reward logic. |
| #29 | Achievement unlocks | Improve achievement unlock presentation while preserving existing achievement definitions and rewards. |
| #30 | Random events | Make existing random-event announcements more distinctive and readable without changing event odds or effects. |
| #31 | Early-game goals | Improve discoverability and feedback for existing early-game goals and progression; avoid unnecessary new systems. |
| #32 | Lua activity localization | Use approved locale keys and arguments for Lua minigame titles, descriptions, choices, and feedback. Inspect the current Lua engine, payload validator, and locale resolver before finalizing the payload contract. |
| #33 | Lua random-event localization | Route player-facing Lua random-event messages through the same controlled localization resolver. Do not leave hardcoded player-facing text in scripts. |
| #34 | Localization verification | Test supported locales, language switching, formatting arguments, missing keys, malformed payloads, and defined fallback behavior. |
| #35 | Temporary LiveOps locale packs | Define the locale-pack schema and integrate delivery of temporary translation data for limited-time activities, without requiring an APK release for each translation update. |
| #36 | Locale-pack lifecycle and recovery | Validate packs, enforce allowed keys and size limits, cache accepted packs, handle versions and expiry, reject malformed content, and fall back to built-in locales when packs are unavailable or expired. |
| #37 | Final regression review | Review gameplay feel, locale coverage, Lua boundaries, LiveOps failure/offline behavior, save compatibility, and regression tests. |

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
- Keep each PR focused. Avoid unrelated dependencies and scope expansion.
- Do not claim a PR is merged or checks passed without verifying GitHub status. Workflows are manual-only unless explicitly requested.
