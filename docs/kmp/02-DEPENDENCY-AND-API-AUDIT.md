

## Verified toolchain compatibility finding (2026-10-14)

The current repository configuration observed on `master` and the initial shared-module branch records:

- Kotlin Gradle plugin: `2.2.10`
- Android Gradle Plugin: `9.4.1`
- Gradle wrapper: `9.8.0`

The official Kotlin Multiplatform compatibility table lists Kotlin/KGP `2.2.0–2.2.10` with Gradle `7.6.3–8.14` and AGP `7.3.1–8.10.0`. This means the repository's current Gradle/AGP versions are outside that documented compatibility range for KMP 2.2.10. Do not treat the new KMP module as validated until a supported version set is selected.

Official source: https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html

The same official guide lists KGP `2.4.20–2.4.21` with Gradle `7.6.3–9.7.0` and AGP `8.5.2–9.3.1`. This is a possible compatibility direction to evaluate, not an automatic upgrade recommendation: the project should choose and pin a mutually supported Gradle/KGP/AGP/Compose set together, then verify it with an explicitly requested build. In particular, Gradle `9.8.0` and AGP `9.4.1` are newer than the maxima in that KMP compatibility row.

### Immediate migration gate

- [ ] Choose a toolchain set within the official KMP compatibility matrix before expanding shared-module usage.
- [ ] Confirm Compose Multiplatform version compatibility with the chosen Kotlin/KGP version.
- [ ] Keep this compatibility issue visible in the first implementation PR; do not claim compilation success without a build.
- [ ] Avoid unrelated dependency upgrades in the same change.
