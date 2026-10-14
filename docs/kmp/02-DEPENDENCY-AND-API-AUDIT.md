# Kotlin Multiplatform dependency and API audit

## Current baseline observed

The current `master` configuration records Kotlin Gradle plugin `2.2.10`, Android Gradle Plugin `9.4.1`, Gradle wrapper `9.8.0`, JVM bytecode target 11, Android `:app` module and Android-specific Compose UI.

The initial KMP scaffold on `feat/kmp-shared-guess-rules` adds `:shared` with a Kotlin Multiplatform JVM target and `commonMain`; it does not yet add a Windows desktop launcher.

## Dependency classification

| Dependency / API | Classification | Decision |
|---|---|---|
| Pure Kotlin deterministic rules | Common candidate | Extract incrementally; common tests added for the existing guess-feedback rule |
| kotlinx.serialization JSON | Potentially multiplatform | Keep out of common code until the exact serializers and artifact variant are confirmed |
| kotlinx.coroutines core | Multiplatform-capable | Use only common APIs in shared code; Android dispatcher/UI integration stays platform-specific |
| Jetpack Compose / Material 3 | Android UI today | Keep existing UI; introduce Compose Multiplatform only after version compatibility is selected |
| Navigation Compose | Existing Android navigation | Retain in Android app; separately verify desktop-compatible navigation APIs before sharing screens |
| Preferences DataStore | Android-oriented current usage | Preserve current implementation behind a boundary before considering portable persistence |
| AndroidX lifecycle/activity/core/appcompat | Android-only | Keep in `:app` / Android-specific code |
| `org.luaj:luaj-jme:3.0.1` | Java/JVM-oriented; exact artifact behavior unverified | Do not place in `commonMain`; verify the exact artifact and sandbox on Android and Windows |
| Android media, Glance, widgets, notifications, WorkManager | Android-only | Keep platform-specific; desktop equivalents are separate adapters only if needed |
| C++ / NDK component, if used | Native per target | Android `.so` files cannot be reused as Windows binaries; keep optional and separate |

These classifications are based on the checked Gradle configuration and imports/search results. They are not proof that the full project compiles on all targets.

## Toolchain compatibility gate

The repository's current versions are outside the official Kotlin Multiplatform compatibility row for Kotlin/KGP 2.2.10: that row lists Gradle 7.6.3–8.14 and AGP 7.3.1–8.10.0. The repository currently records Gradle 9.8.0 and AGP 9.4.1.

Official reference: https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html

Compose Multiplatform has its own compatibility guidance and requires the Compose compiler Gradle plugin version to match the Kotlin Multiplatform plugin version. Official reference: https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html

Do not select or claim a validated toolchain by extrapolation. Choose one mutually supported version set, review the app's compile SDK and Android plugin behavior, then run an explicitly authorized local build before declaring the gate passed. No build or workflow has been run as part of this audit.

## Source audit decisions

- `commonMain`: plain Kotlin deterministic rules, portable models and contracts only.
- `jvmMain`: candidate for Java/JVM-dependent adapters shared by Android and Windows; LuaJ remains here or in an application-specific JVM module only after verification.
- Android app: current Android framework, DataStore, encryption/recovery, widgets, notifications, LiveOps and media-service implementations remain intact.
- Desktop app: desktop launcher/window, keyboard/mouse behavior, filesystem storage and desktop audio are separate platform-specific implementations.
- Native: any JNI/NDK component needs a separate build and binary per target; no native runtime is required merely to achieve KMP.

## Local audit commands (not run)

Run manually when authorized:

```bash
./gradlew projects
./gradlew :app:dependencies
./gradlew :shared:allTests
```

## Checklist

- [x] Record current Gradle, Kotlin/KGP, AGP and JVM target settings from repository files.
- [x] Identify current Android-only dependency families and the declared LuaJ artifact.
- [x] Select a first pure Kotlin extraction candidate: guess feedback rules.
- [x] Add common tests for low/high/correct feedback and range-clamping boundaries (test source added; not executed).
- [x] Record the KMP toolchain compatibility blocker with official references.
- [ ] Select a mutually supported Gradle/KGP/AGP/Compose version set.
- [ ] Confirm the chosen Compose Multiplatform version against the selected Kotlin plugin.
- [ ] Run and inspect Gradle dependency reports and a local Android/shared build.
- [ ] Confirm the exact LuaJ runtime artifact, license and runtime behavior on Android and Windows.
- [ ] Inventory all save/encryption/resource/native API call sites before moving persistence or runtime code.
- [ ] Verify Android save compatibility and recovery through targeted regression tests.
- [ ] Verify desktop build and packaging on Windows.

No production save format, encryption, persistence behavior, Android UI or native integration was changed by this audit.
