# Workflow Memory

## Android CI build policy

- The regular Android PR/validation workflow at `.github/workflows/build-apks.yml` should run localization/version/JSON audits, Kotlin/build preflight, unit tests, lint, and `./gradlew check`, then upload test/lint reports. It must **not** run `assembleDebug` or `assembleRelease`, and must not upload ordinary Debug or Release APK artifacts.
- **Exception: Stable Debug APK only.** `.github/workflows/stable-debug-apk.yml` is the designated workflow that builds the signed, update-compatible Debug APK using the stable debug keystore, packages the installable ZIP, and publishes the `debug-latest` asset. Keep this workflow intact.
- Do not build a Release APK as part of routine Android CI. Production/release builds remain a separate, intentional release process.
- Keep the Windows Desktop workflow unchanged. Do not manually dispatch workflows just to verify a code change; rely on the normal PR-triggered checks and inspect their results.
- Preserve the stable Debug signing identity so updates install over existing stable Debug builds.
