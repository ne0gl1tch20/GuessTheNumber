# Kotlin Multiplatform dependency and API audit

## Current baseline observed

The repository currently has a single Android application module included by `settings.gradle.kts`. The app applies the Android application, Kotlin serialization, and Compose compiler plugins. The app targets JVM bytecode 11 and uses Jetpack Compose.

The version catalog currently includes these relevant libraries:

| Dependency | Initial classification | Required action |
|---|---|---|
| Kotlin standard library | Likely common | Confirm code uses only common APIs |
| kotlinx.serialization JSON | Potentially multiplatform | Verify the selected artifact/version and serializers used |
| kotlinx.coroutines core | Multiplatform-capable | Keep Android dispatcher/UI dependencies out of common code |
| Jetpack Compose / Material 3 | Android UI today | Add Compose Multiplatform only after version compatibility is checked |
| Navigation Compose | Android-oriented current usage | Audit desktop navigation support and API differences |
| Preferences DataStore | Android-oriented current usage | Hide behind a storage interface before sharing |
| AndroidX lifecycle/activity/core/appcompat | Android-only | Keep in Android source sets |
| `org.luaj:luaj-jme:3.0.1` | JVM library, artifact-specific behavior to verify | Test exact runtime on Android and Windows; do not place in commonMain |
| Android media, Glance, widgets, notifications | Android-only | Keep platform-specific or replace with desktop equivalents where useful |
| C++ / NDK component, if added | Native per target | Separate Android ABI binaries from Windows binaries |

This is an initial classification based on the current version catalog, not proof that every dependency is compatible with every target.

## Audit checklist for each source file

Record the file, imports, dependency usage, proposed source set, and blocker.

- **Common candidate:** plain Kotlin, deterministic business rules, no Android/JVM/Java APIs.
- **JVM candidate:** depends on Java APIs but can run on Android and Windows JVMs; keep outside `commonMain`.
- **Android-only:** uses Android framework, AndroidX lifecycle/storage, widgets, haptics, notifications, or app context.
- **Desktop-only:** uses AWT/Swing, desktop filesystem conventions, Windows APIs, or desktop windowing.
- **Native-specific:** uses JNI/NDK or platform-specific binaries.

## Commands for a local audit

Run manually on a development machine when ready; this PR does not run builds:

```bash
./gradlew projects
./gradlew :app:dependencies
```

Search source code for Android/JVM-bound imports such as `android.`, `androidx.`, `java.io.`, `java.nio.file.`, `java.net.`, `javax.crypto.`, and direct use of Android `Context`. These imports are not all inherently bad; they identify code that needs classification before being moved.

## Exit criteria

- [ ] Gradle, Kotlin, Android Gradle Plugin, and Compose versions recorded.
- [ ] Each direct dependency has a target classification and version-compatibility check.
- [ ] LuaJ artifact and APIs used by the current implementation are identified.
- [ ] Save, encryption, resource loading, networking, and native-library dependencies are inventoried.
- [ ] First shared-core extraction candidates are selected.
- [ ] No existing dependency is removed or upgraded solely to make the audit look cleaner.
