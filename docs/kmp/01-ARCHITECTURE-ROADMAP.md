# Kotlin Multiplatform roadmap: Android + Windows

## Goal

Evolve GuessTheNumber into an Android app and a Windows desktop app while preserving the existing Android experience, save data, offline-first behavior, localization, and sandboxed Lua activities.

This document is a migration plan, not a claim that the repository already builds as a multiplatform project.

## Target architecture

- **Android host:** retain the existing application module and Android-only integrations during the early migration.
- **Shared Kotlin core:** move only platform-neutral game rules, economy calculations, progression models, deterministic validation, and protocol/data models after their dependencies are audited.
- **Windows host:** add a Compose Multiplatform desktop target after the shared core has a stable boundary.
- **Platform adapters:** keep storage, notifications, audio, haptics, file picking, update delivery, and OS integration behind interfaces with Android and desktop implementations.
- **Lua runtime:** treat the interpreter as JVM/platform-specific until the exact artifact and sandbox behavior have been verified on both Android and Windows.
- **Native C++:** keep optional and isolated. An Android `libgtn.so` is not a Windows binary and must never be loaded by the desktop target.

## Delivery principles

1. Keep Android buildable and playable after every merged migration step.
2. Do not rewrite the whole app or change the existing save schema just to achieve code sharing.
3. Prefer incremental extraction over moving all source files at once.
4. Keep offline play functional; no ads, accounts, or analytics are introduced by this migration.
5. Every PR must identify which targets it affects and what validation was actually run.
6. Do not mark a build or test as passing unless a corresponding result exists.

## Eight-PR preparation sequence

| PR | Scope | Exit condition |
|---|---|---|
| 1 | Architecture and migration roadmap | Target boundaries and guardrails agreed |
| 2 | Dependency and Android API inventory | Every dependency has a target classification |
| 3 | Shared-core extraction boundaries | First candidate files and blockers identified |
| 4 | Save compatibility plan | Existing save formats and rollback expectations documented |
| 5 | Lua runtime compatibility plan | JVM placement, artifact checks, and sandbox constraints documented |
| 6 | Windows desktop target plan | Desktop entry point, UI scope, and packaging target defined |
| 7 | Platform service adapters | Platform-only capabilities have explicit interfaces and implementations planned |
| 8 | Build and packaging matrix | Android and Windows validation/package jobs specified |

These eight preparation PRs are intentionally documentation-first. Actual Gradle/module migration follows the audit so that unverified assumptions do not become source-level breaking changes.

## Proposed eventual source layout

The exact module names may change after the audit. A possible end state is:

```text
app/ or composeApp/
  src/androidMain/     Android entry point and Android-only integrations
  src/desktopMain/     Windows desktop entry point and desktop integrations
  src/commonMain/      UI and logic proven portable
shared/
  src/commonMain/      Pure Kotlin game rules and shared models
  src/jvmMain/         JVM-only adapters where useful
```

Do not move the current Android source tree wholesale into `commonMain`. Android imports, AndroidX storage, Android lifecycle, and platform-specific libraries must first be isolated.

## Risks to actively track

- Existing Gradle plugins and Android Gradle Plugin compatibility with the selected Kotlin/Compose Multiplatform versions.
- Java/JVM-only dependencies in code intended for `commonMain`.
- LuaJ artifact differences, Android API/desugaring requirements, and sandbox integrity.
- Save compatibility and encrypted data migration.
- Locale resource loading and file paths on desktop.
- Audio, haptics, notifications, background work, LiveOps, and update-channel differences.
- Different native binaries for Android and Windows.
