# GTN Flutter workspace

This directory is the isolated destination for the reliability-first Flutter migration.

The app here is intentionally only a scaffold. It is not yet a playable replacement for the Kotlin application. The existing Android, Windows and Web Play implementations remain intact while migration work proceeds.

## Local commands

```sh
cd flutter
flutter pub get
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
flutter build apk --debug
```

## Architecture target

- `lib/app`: application composition and navigation.
- `lib/core`: shared errors, clock/RNG abstractions and validation.
- `lib/domain`: deterministic game rules and value objects.
- `lib/data`: persistence, content loading and migration adapters.
- `lib/features`: player-facing feature modules.
- `test/fixtures`: historical save/content fixtures used by compatibility tests.

Do not add gameplay calculations to widgets. UI must dispatch typed commands to domain services. Persistence and platform integrations must be behind interfaces so they can be tested without a device.

See [the migration plan](../docs/FLUTTER_MIGRATION_PLAN.md) for the migration sequence and release gates.
