# Kotlin Multiplatform dependency and API audit

## Current baseline observed

The master baseline before PR #100 records Kotlin Gradle plugin (KGP) 2.2.10, Android Gradle Plugin (AGP) 9.4.1, Gradle wrapper 9.8.0, JVM bytecode target 11, Android :app module and Android-specific Compose UI. PR #99 has now been merged and adds :shared with a Kotlin Multiplatform JVM target, commonMain and extracted guess-feedback rules. It does not add a Windows desktop launcher.

PR #100 proposes a supported toolchain tuple: Kotlin/KGP 2.4.20, AGP 9.3.1 and Gradle 9.7.0. The version catalog keeps the Kotlin serialization and Compose compiler plugins aligned to Kotlin via version.ref = "kotlin".

## Dependency classification

| Dependency / API | Classification | Decision |
|---|---|---|
| Pure Kotlin deterministic rules | Common candidate | Extract incrementally; common tests cover the existing guess-feedback rule |
| kotlinx.serialization JSON | Potentially multiplatform | Keep out of common code until the exact serializers and artifact variant are confirmed |
| kotlinx.coroutines core | Multiplatform-capable | Use only common APIs in shared code; Android dispatcher/UI integration stays platform-specific |
| Jetpack Compose / Material 3 | Android UI today | Keep existing UI; introduce Compose Multiplatform only after its own version compatibility is checked |
| Navigation Compose | Existing Android navigation | Retain in Android app; separately verify desktop-compatible navigation APIs before sharing screens |
| Preferences DataStore | Android-oriented current usage | Preserve current implementation behind a boundary before considering portable persistence |
| AndroidX lifecycle/activity/core/appcompat | Android-only | Keep in :app / Android-specific code |
| org.luaj:luaj-jme:3.0.1 | Java/JVM-oriented; exact artifact behavior unverified | Do not place in commonMain; verify the exact artifact and sandbox on Android and Windows |
| Android media, Glance, widgets, notifications, WorkManager | Android-only | Keep platform-specific; desktop equivalents are separate adapters only if needed |
| C++ / NDK component, if used | Native per target | Android .so files cannot be reused as Windows binaries; keep optional and separate |

These classifications are based on the checked Gradle configuration and imports/search results. They are not proof that the full project compiles on all targets.

## Toolchain compatibility gate

The Kotlin Multiplatform compatibility table lists KGP 2.4.20 as fully supported with Gradle 7.6.3–9.7.0 and AGP 8.5.2–9.3.1. AGP 9.3 supports Android API 37 and requires Gradle 9.5.0 or newer, so the proposed Gradle 9.7.0 / AGP 9.3.1 pair fits those published ranges. The Gradle wrapper checksum is set to the official SHA-256 for the 9.7.0 binary distribution.

References:
- https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html
- https://developer.android.com/build/releases/agp-9-3-0-release-notes
- https://developer.android.com/build/releases/about-agp
- https://gradle.org/release-checksums/

The existing Compose compiler plugin alias references the Kotlin version, so it follows KGP 2.4.20. The app currently uses AndroidX Compose via the Compose BOM 2026.09.00; this is not a Compose Multiplatform version. Do not add shared Compose UI until a Compose Multiplatform release compatible with the chosen Kotlin version is explicitly selected.

**Status:** the version set is selected and proposed in PR #100, but this gate is not build-verified yet. Do not call the toolchain validated until CI or an authorized local build checks it.

## Source audit decisions

- commonMain: plain Kotlin deterministic rules, portable models and contracts only.
- jvmMain: candidate for Java/JVM-dependent adapters shared by Android and Windows; LuaJ remains here or in an application-specific JVM module only after verification.
- Android app: current Android framework, DataStore, encryption/recovery, widgets, notifications, LiveOps and media-service implementations remain intact.
- Desktop app: desktop launcher/window, keyboard/mouse behavior, filesystem storage and desktop audio are separate platform-specific implementations.
- Native: any JNI/NDK component needs a separate build and binary per target; no native runtime is required merely to achieve KMP.

## Validation status

GitHub Actions run #343 for PR #99's head commit 4dee392d94eb0731a3a3dff7e270c3e11b30dbb5 reported:
- Unit tests: passed.
- Full Gradle check: passed.
- Debug APK build: passed.
- Release APK build: in progress at the last status check; final workflow result was not confirmed.

These results apply to the pre-toolchain-change commit, not PR #100. No local build or dependency report was run for the new version set.

## Local audit commands (not run)

Run when explicitly authorized:

```bash
./gradlew projects
./gradlew :app:dependencies
./gradlew :shared:allTests
```

## Checklist

- [x] Record current Gradle, Kotlin/KGP, AGP and JVM target settings from repository files.
- [x] Identify current Android-only dependency families and the declared LuaJ artifact.
- [x] Select a first pure Kotlin extraction candidate: guess feedback rules.
- [x] Add common tests for low/high/correct feedback and range-clamping boundaries; CI unit-test step passed on PR #99's pre-toolchain-change commit.
- [x] Record the KMP toolchain compatibility blocker with official references.
- [x] Select a mutually supported Gradle/KGP/AGP version set in PR #100: Gradle 9.7.0 / KGP 2.4.20 / AGP 9.3.1.
- [x] Keep the Compose compiler plugin version aligned with Kotlin via the version catalog's shared version reference.
- [ ] Select and confirm a Compose Multiplatform version against Kotlin before adding shared Compose UI.
- [ ] Run and inspect Gradle dependency reports and an Android/shared build with PR #100's toolchain.
- [ ] Confirm the exact LuaJ runtime artifact, license and runtime behavior on Android and Windows.
- [ ] Inventory all save/encryption/resource/native API call sites before moving persistence or runtime code.
- [ ] Verify Android save compatibility and recovery through targeted regression tests.
- [ ] Verify desktop build and packaging on Windows.

No production save format, encryption, persistence behavior, Android UI or native integration was changed by this audit.
