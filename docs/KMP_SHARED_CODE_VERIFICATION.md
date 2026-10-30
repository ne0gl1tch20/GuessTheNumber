# KMP shared-code and host-boundary verification

**Scope:** Android and Windows Desktop share production Kotlin behavior without forcing JVM APIs into `commonMain`.

## Existing implementation verified

- `shared/src/commonMain` contains platform-neutral rules and contracts, including deterministic guess feedback/state-transition rules and their `commonTest` coverage.
- `shared/src/jvmAndAndroidMain` contains the production `GameState` graph, `BigNumber` and the production `GameEngine`. This is intentional: `BigNumber` uses Java `BigDecimal`/math context behavior, and Android plus Windows Desktop are both JVM consumers. Moving this graph into `commonMain` or replacing decimal arithmetic with `Double` would risk save and economy compatibility.
- `shared/src/androidMain` and `shared/src/jvmMain` are thin host-specific source sets that depend on the shared JVM/Android layer.
- Android's `GameViewModel` and `SaveManager` remain Android host adapters. Windows uses the shared production state/engine with `DesktopSaveStore`, a desktop-native filesystem adapter. Do not make Windows depend on Android DataStore or Android lifecycle APIs.
- Both hosts therefore use the same game-state model and production engine; storage, lifecycle, notifications, audio, windowing and update behavior remain host-owned.

## Automated checks

Run the source-set boundary audit:

```sh
python tools/audit_kmp_source_boundaries.py
```

Run the JVM shared and desktop tests plus desktop compilation:

```sh
./gradlew :shared:jvmTest :desktopApp:jvmTest :desktopApp:compileKotlinJvm
```

On Windows, use `gradlew.bat` instead.

Existing automated coverage includes:

- `shared/src/commonTest/.../GuessRulesTest.kt`: common-source deterministic guess feedback and boundary cases.
- `shared/src/jvmTest/.../DesktopSaveStoreTest.kt`: save/reload, reset, backup recovery, corruption, legacy JSON defaults, slot operations and failed-write safety.
- `desktopApp/src/jvmTest/.../ProductionGuessSaveParityTest.kt`: production guess transitions, disk persistence, reload and subsequent transition using the same production `GameEngine`.

## Limits and remaining product work

Passing compilation and tests proves source compatibility and the covered deterministic/save paths; it does not prove full Windows feature parity or interactive runtime behavior. Continue using `docs/WINDOWS_FEATURE_PARITY_PLAN.md` as the single parity tracker. The Windows UI is not yet a complete port of every Android destination. Full save-slot/import/export/recovery parity, remaining economy/progression screens, desktop runtime checks and the planned shared HTML/CSS/JS player-facing UI migration remain separate work.

Do not move Java-dependent production models into `commonMain` merely to increase the shared-code percentage. Move additional code there only when it is genuinely platform-neutral and covered by common tests.
