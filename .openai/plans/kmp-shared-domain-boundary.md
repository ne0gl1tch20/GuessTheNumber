# KMP shared domain boundary

**Status:** Implementation in progress; validate with CI before merging.

## Decision

Android and Windows Desktop are both JVM consumers. Put the production save model and large-number implementation in the `shared` module's `jvmAndAndroidMain` intermediate source set. Keep `commonMain` limited to code that is genuinely portable to every KMP target.

This lets Android and Windows use the same `GameState`, `GameStatistics`, `GameSettings`, `BigNumber` and `BigNumberSerializer` classes without forcing Java `BigDecimal` into `commonMain`.

## Why this boundary

- `BigNumber` uses `BigDecimal`, `MathContext.DECIMAL128` and `RoundingMode`. Replacing this with `Double` would lose decimal precision and could alter idle-game currency results.
- The serialized save contract should remain stable: keep model field names, field types, and the decimal-string serializer unchanged.
- `GameState` no longer imports the Android app's `BuildConfig`. The Android app supplies `lastSavedVersion` at app/save boundaries.
- Debug-only More-screen routes stay in the Android presentation layer, not in the shared save model.
- The shared intermediate source set must not import Android framework APIs. Android storage, lifecycle, resources, notifications and windowing remain host responsibilities.

## Follow-up for Stage 2

- [ ] Verify Android and JVM variants compile against the shared model.
- [ ] Run existing save serialization, recovery and BigNumber tests.
- [ ] Add shared deterministic parity tests for gameplay and currency examples.
- [ ] Keep desktop storage separate from Android DataStore while preserving the serialized model contract.
- [ ] Only move additional production logic when its dependencies fit this boundary; do not migrate UI/platform services into the shared model.


## App icon source of truth

- [ ] Use the repository-root `GTN_APP_LOGO.png` as the canonical source image for **both Android mobile and Windows Desktop** app icons.
- [ ] Generate Android launcher resources at the required density buckets from that source, including adaptive/round launcher resources where supported; do not leave the existing Android launcher artwork as a separate source of truth.
- [ ] Keep the Windows installer/app icon derived from the same source artwork.
- [ ] Verify the icon in debug and release Android builds and in the packaged Windows app/MSI. Keep this asset task separate from gameplay behavior and do not claim it is complete until the packaged icons are inspected.
