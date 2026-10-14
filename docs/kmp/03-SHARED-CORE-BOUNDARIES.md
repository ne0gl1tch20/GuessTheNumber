# Shared game-core extraction boundaries

## Purpose

Define what can be shared between Android and Windows without forcing Android-specific services into common code.

## First candidates to inspect

1. Pure numeric calculations and upgrade-cost formulas.
2. Economy invariants and reward validation.
3. Progression requirement checks and unlock rules.
4. Stable value models and enums that do not expose Android types.
5. Multiplayer protocol envelopes and payload validation.
6. JSON schema/data validation that uses a compatible serialization library.
7. Deterministic challenge rules and other calculations with no UI or platform side effects.

The list is a candidate inventory. A file should not move until its imports, callers, and persistence assumptions are checked.

## Keep out of common code initially

- Compose screens tied to Android-only libraries.
- Activities, Android `Context`, lifecycle owners, and Android navigation.
- DataStore and direct filesystem access.
- Notifications, widgets, haptics, audio playback, and update delivery.
- LuaJ runtime construction and JVM reflection/class loading.
- JNI/NDK code and binaries.
- Code that reads wall-clock time or random values directly when a deterministic input would make it testable.

## Boundary rule

Common game logic should accept values and return results. It should not reach into an Android service to save a game, play a sound, vibrate, display a notification, fetch a LiveOps manifest, or open a file.

For example, a shared purchase operation should decide whether a purchase is valid and what the new game state should be. The host app can then persist the result and play feedback using platform-specific services.

## Suggested first extraction

Choose one small, pure function or immutable model that has:
- no Android imports,
- no direct I/O,
- no Java-only API requirement,
- stable input/output behavior,
- and a clear regression test.

Move only that slice first, add shared tests if supported by the module configuration, and keep the Android call site behavior unchanged.

## Validation expectations

- Existing Android tests/build must continue to work.
- Add deterministic tests for the extracted behavior.
- Compare representative old and new results where the logic already has established behavior.
- Avoid changing balance, rounding, save serialization, or game rules during a structural move.
