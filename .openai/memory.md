# GuessTheNumber Project Memory

Last updated: 2026-10-13

This file is the durable project context for future work on GuessTheNumber. Keep it accurate as decisions and repository state change. Distinguish implemented behavior from planned work; do not treat this document as proof that code or tests exist.

## 1. Repository and app identity

- Repository: https://github.com/ne0gl1tch20/GuessTheNumber
- Package/application ID: `com.jarrlyyy.guessthenumber`
- Platform: Android
- Main stack: Kotlin, Jetpack Compose, Material 3, Navigation, DataStore, JSON assets, and sandboxed LuaJ scripts.
- Kotlin owns the app UI and authoritative gameplay systems. Lua is for bounded reusable scripts/minigames/content. C++/NDK is a possible later optimization for one narrowly scoped component, not a goal by itself.
- Core progression systems include Money, Prestige, Ultra, and Nebula.
- The app is intended to work offline, with no ads, analytics, telemetry, accounts, or unnecessary network access.

## 2. Non-negotiable product and architecture principles

1. **Stability and player experience first.** Fix crashes, stuck loading, incorrect behavior, localization defects, and save risks before adding scope.
2. **Kotlin remains authoritative** for gameplay state, save/persistence, rewards, eligibility, claims, timing, and sensitive logic. Lua and any future native code must not bypass those rules.
3. **Preserve save compatibility, fair rewards, and offline play.** Any schema or migration work must be deliberate, validated, and regression-tested.
4. **Keep Lua sandboxed.** Scripts should have bounded responsibilities and validated inputs/outputs. They must not gain unrestricted filesystem, network, JNI/native, save, or reward access.
5. **C++ is conditional.** Add native code only for a narrow, well-defined component when measurements show a justified benefit. Keep a Kotlin implementation/fallback and test parity and failure handling. Do not migrate the entire engine to C++ just because it is possible.
6. **No speculative rewrites or unrelated changes.** Split broad improvements into focused, reviewable PRs.
7. **Debug and Release must be treated deliberately.** Developer-only features and consoles must not accidentally ship enabled in Release.
8. **No invented status.** Never say a build, test, workflow, merge, release, or device check passed unless the result was actually observed.

## 3. User workflow and response preferences

- The user often works from an Android phone using GitHub and ChatGPT.
- Give concise, direct explanations with simple headings and checklists.
- For code changes, give copy-ready details and exact repository paths; avoid unrelated edits.
- Prefer focused PRs and conserve GitHub Actions usage.
- Do not start builds, tests, emulator/device runs, or GitHub Actions unless the user explicitly authorizes them. If the user authorizes a run, report only verified results.
- Do not create Codex PR reviews unless explicitly requested.
- For any repository-change response, include the Actions page link and the exact sentence: **“You can build it using GitHub Actions.”**
- Actions page: https://github.com/ne0gl1tch20/GuessTheNumber/actions

## 4. Versioning and official release plan

- The current development baseline was last recorded as versionName `3.0.2`, versionCode `20`.
- Do not bump the current development version unless the user asks for release work or a change requires it.
- **The user's release-day decision:** on official release day, set the app version to **`1.0.0`**.
- Treat the older versioning history as the Alpha-to-Beta development era; the official public release starts at 1.0.0. Preserve this as an intentional release/versioning decision rather than assuming normal numeric continuation from 3.0.2.
- When release work begins, review all version metadata and release notes consistently; do not perform the release or change version numbers early without authorization.

## 5. Localization and UI requirements

- All player-visible strings must be localized. Avoid hardcoded fallback UI text that bypasses the localization system.
- Supported locales have included English, English (UK), Filipino, Chinese, Japanese, and Korean; check the current repository assets before assuming this is the complete current list.
- Keep JSON locale keys and placeholders consistent across locales. Test missing keys, invalid placeholders, and language switching.
- Known localization problems previously reported include screens not refreshing completely after a language change, visible `%s`/`%d` placeholders, older screens containing hardcoded UI text, and a changelog-related crash.
- **Every Settings option must have both a localized title and a localized plain-language description.** This applies to toggles, selectors, sliders, dropdowns, and advanced options, not only switches. The description should explain what the setting controls and its practical effect. For example:
  - “Enable haptics” should explain when vibration feedback occurs.
  - “Enable background music” should explain playback behavior and that music is optional.
- Descriptions must accurately reflect implemented behavior, update when the language changes, and make global versus per-save scope clear. Do not leave dead or misleading controls in the UI.
- UI goals include Material 3 Expressive patterns, responsive layouts, useful animation, theme presets and previews, accessible touch targets, TalkBack semantics, keyboard navigation, scalable text, contrast, reduced-motion behavior, and clear loading/empty/error states.
- User has planned work for hover guidance and Compose previews, but touch and keyboard alternatives must remain usable on devices that do not support hover.

## 6. Saves, exports, privacy, and security

- The intended design includes encrypted internal saves using Android Keystore-backed AES-GCM and password-protected export/import.
- Do not log raw save data or put sensitive save contents on the clipboard.
- Validate imported files, URIs, JSON, script inputs, and update metadata before use.
- Review encrypted saves/exports, file and URI handling, update artifact verification, external intents, logs, secret exposure, and privacy boundaries during hardening.
- Keep network use absent unless a feature explicitly needs it and is approved; offline-first behavior remains a requirement.
- Test migration and error recovery without silently discarding player progress.
- Avoid expanding permissions or exposing data unnecessarily.

## 7. Developer tooling and build/release workflow

- Developer settings/console work has been discussed, including more commands, optional verbose logging, autocomplete refactoring, and a cleaner developer-settings UI.
- Developer-only controls should be feature-gated and removed or disabled in Release where appropriate.
- Prior work was reported around schemaVersion on 15 JSON assets, validation, centralized feature flags, data-driven LiveOps, and automated tests; verify the repository before assuming all of this remains present or complete.
- Preserve manual-only execution expectations. Do not spend Actions quota by triggering builds on every commit unless the user explicitly changes this rule.
- A prior project instruction requested disabling workflow triggers on commits to conserve Actions usage. Check actual workflow triggers before changing them; do not assume a workflow is manual-only based on this note alone.

## 8. Engine boundaries: Kotlin, Lua, and possible C++

### Kotlin
- Owns authoritative gameplay state, UI, saves, reward and claim eligibility, timing, and critical validation.
- Reward/claim operations should be idempotent and validated.
- Keep state restoration, lifecycle cancellation, and error/retry handling reliable.

### Lua
- Scripts are stored under `app/src/main/assets/scripts/` (verify current paths before editing).
- Use Lua for bounded, reusable scripted content or minigames, not for authoritative rewards or persistence.
- Validate script payloads and outputs. Apply time/instruction, memory, output-size, and other resource limits where supported.
- Malformed scripts, infinite loops, exceptions, and unsupported operations must fail safely without hanging or crashing the app.
- Keep JSON fallback behavior where intended.
- A prior architecture note identified Activity Hub route `lua_hub`, the More menu gated by `lua_scripting`, and the GO flow; verify current implementation before relying on these names.

### C++ / NDK
- C++/NDK is a later, conditional phase. Define a narrow responsibility and capture correctness/performance baselines first.
- Keep the JNI interface small and versioned, validate data sizes and inputs, handle failures explicitly, and avoid unsafe ownership/lifetime behavior.
- Never expose saves, UI, network access, secrets, or unrestricted reward mutation to native code.
- Compare Kotlin and native results across edge cases and supported ABIs, including 32-bit compatibility where required.
- Test malformed/oversized inputs, integer overflow risks, native/JNI failures, cleanup, and Kotlin fallback.
- Keep the native implementation only if it is stable and delivers a measured, justified benefit. Otherwise retain Kotlin.

## 9. Gameplay and feature scope

- Progression currently centers on Money, Prestige, Ultra, and Nebula.
- The app has or has planned Play, Upgrade, Shop, Settings, and More areas; verify current screens and routes in the repo before editing.
- Planned work has included save slots (up to 10), each with difficulty/name/icon; import on first start; global app preferences; encrypted export of global preferences and all slots; developer JSON editing separated between app preferences and the selected save; an interactive tutorial available from Play/More with a global completion flag; and theme reordering only after the user explicitly enters reorder mode.
- Treat these as historical/planned goals, not proof that they are implemented. Check current code and roadmap before changing them.
- Preserve existing balance, progression rules, rewards, and saves unless a scoped change explicitly calls for a change.

## 10. Current roadmap and planned PRs

Roadmap file: `.openai/docs/JOY_PHASE_ROADMAP.md`

The roadmap currently states **71 focused PRs, #24–#94**. This is planned work, not a claim that all PRs are open or implemented.

### PR #93: Compose previews, hover guidance, and extended accessibility
Planned scope includes helpful hover guidance/tooltips, Compose previews, keyboard and touch alternatives, TalkBack/accessibility semantics, keyboard navigation, scalable text, contrast, touch targets, reduced motion, and previews for loading/empty/error/theme/localized states.

### PR #94: App-wide screen, settings, feature, logic, engine, and security hardening
Planned scope includes:
- Audit all screens, existing features, navigation/back behavior, state restoration, loading/empty/error/retry paths, lifecycle cancellation, input validation, persistence/migrations, reward idempotency, offline behavior, permissions/privacy, update flows, and Debug/Release feature gating.
- Review encrypted saves/exports, file/URI handling, update artifact verification, logging, external intents, and data/secret exposure.
- Audit Kotlin/Lua/C++ boundaries where present; enforce script input/output/resource limits, prevent scripts from reaching restricted APIs or modifying saves/rewards, and test malformed scripts, native failures, deterministic behavior, and fallback paths.
- Require a localized title and localized practical description for every setting, with accurate behavior, live language switching, valid placeholders, complete locale coverage, and explicit global versus per-save scope.
- Add focused regression tests for concrete findings; preserve saves, offline play, accessibility, and reward rules.
- C++ improvements are conditional on a justified native boundary and measured benefit. Improve Kotlin/Lua even if C++ is not appropriate.
- Keep the work split into focused PRs rather than one risky rewrite.
- Do not start builds, emulator/device tests, or Actions without authorization.

### Planned C++ phase (#40–#45)
- #40: Native engine architecture, trust boundaries, and correctness/performance baseline.
- #41: Small NDK/CMake build and native test scaffold; Kotlin-only behavior remains functional.
- #42: Minimal, versioned, validated JNI bridge with safe error handling and Kotlin fallback.
- #43: Port one bounded deterministic component only if evidence supports it.
- #44: Kotlin/native parity, boundary, malformed-input, fuzz/property-style, and failure tests.
- #45: Native integration, ABI, sandbox, save compatibility, cleanup, crash, and measured-performance gate. Keep C++ only if justified.

The roadmap's planned ordering places the C++/NDK phase after joy, math-strategy, Lua localization, and LiveOps locale work. Build-channel-aware updating is a separate delivery-system task. Multiplayer is later and opt-in, initially local-network peer-to-peer.

## 11. Recent pull request and branch context

### PR #39: reproducible dependency and artifact metadata
- URL: https://github.com/ne0gl1tch20/GuessTheNumber/pull/39
- Title: `build(ci): improve dependency and artifact reproducibility`
- Last known state: open, on branch `build/pr87-reproducible-artifacts`. Re-check GitHub for current state before relying on this.
- Changes previously recorded:
  - `.github/workflows/build-apks.yml`: setup-gradle action update and metadata/SHA-256 sidecars for Debug and Release APK artifacts.
  - `gradle/wrapper/gradle-wrapper.properties`: Gradle 9.8.0 distribution checksum pin, retries, and backoff.
  - `tools/write_build_artifact_metadata.py`: metadata JSON with artifact hash, channel, commit/ref/run information, Gradle version/checksum, Java version, and app version, plus SHA-256 sidecar.
  - `tools/fast_ci_smoke_tests.py`: checks for wrapper checksum pin, metadata script, and workflow integration.
  - `.openai/docs/JOY_PHASE_ROADMAP.md`: scope extended to 71 planned PRs and PR #93/#94 added.
- Latest recorded branch commit after roadmap update: `04986488a1dc2f1ea5d14ad080ae2fbc67c423e8`. Verify the branch head before using it.
- No builds/tests/Actions were started for this work; do not claim they passed.
- PR creation reported a non-mergeable status at the time. Re-check current mergeability/conflicts if relevant.
- Important workflow policy: user has generally requested manual-only Actions runs and conserving quota. Inspect actual workflow triggers and discuss/obtain authorization before starting any run.

### PR #38: performance baseline protocol
- URL: https://github.com/ne0gl1tch20/GuessTheNumber/pull/38
- User reported it merged.
- It was docs-only and added `docs/PERFORMANCE_BASELINE.md`, documenting a measurement protocol with values marked `NOT MEASURED`.
- The user explicitly said they do **not** need actual device traces. Do not initiate device profiling or imply measurements were collected.

## 12. Known historical issues to re-check when relevant

Previously reported by the user:
- Language switching did not refresh all UI.
- Visible `%s`/`%d` placeholders.
- Hardcoded strings in older screens.
- Changelog crash and app stuck loading a save after that crash.
- Debug APK install/package signing conflict.
- Old laptop build performance and storage constraints, which are environment context rather than app requirements.

Treat these as historical reports. Verify current source and reproduce only when authorized before claiming a bug is still present or fixed.

## 13. Changelog and documentation rules

- Whenever app code/UI/gameplay/localization/config/player-facing behavior changes, update `app/src/main/assets/changelogs.md` in the same PR.
- The changelog should list only implemented changes. Planned work belongs in `.openai/docs/JOY_PHASE_ROADMAP.md`.
- Keep project decisions in this file current when the user explicitly changes a standing requirement.
- Do not silently change release versioning, save formats, game balance, or the scope of a PR.

## 14. Final checklist before any future change

- [ ] Confirm current branch, PR status, and target branch from GitHub.
- [ ] Read the actual current files; do not rely only on this memory.
- [ ] Keep the change focused and preserve Kotlin's gameplay/save/reward authority.
- [ ] Localize every new user-visible string in all supported locales.
- [ ] For any setting, include a localized title and useful localized description.
- [ ] Update the changelog for implemented player-facing changes.
- [ ] Add focused regression coverage where appropriate.
- [ ] Preserve offline behavior, saves, privacy, accessibility, and existing balance.
- [ ] Do not run builds/tests/emulators/Actions without authorization.
- [ ] Report only verified outcomes and include the GitHub Actions link plus “You can build it using GitHub Actions.”
