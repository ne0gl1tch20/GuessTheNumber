# Platform service adapter boundaries

## Purpose

Give shared game logic a stable way to request platform capabilities without importing Android APIs or assuming Windows behaves like a phone.

## Service boundaries to introduce only when needed

| Capability | Shared-facing contract | Android implementation | Windows implementation |
|---|---|---|---|
| Save storage | Read/write bytes or versioned save records | Existing storage/encryption path | App-data directory plus desktop key/storage strategy |
| Audio and SFX | Play/stop/change volume by logical ID | Existing Android audio stack | Desktop/JVM audio stack |
| Haptics | Optional feedback request | Android vibration/haptics | No-op or desktop-specific feedback; never required for gameplay |
| Notifications | Schedule/cancel a logical reminder | Android notifications/permissions | Optional desktop notifications |
| File selection | Request a user-selected media file | Android picker/URI handling | Windows file chooser/path handling |
| LiveOps transport | Fetch bytes/metadata through a narrow API | Existing client | JVM desktop client |
| Trusted key access | Verify using configured public key; isolate key loading | Existing configuration | Packaged public key/configuration |
| Update delivery | Query update availability and open/download artifact | Current Android channel rules | Separate Windows channel and package strategy |
| Logging | Structured diagnostic event | Existing logger | Desktop logger/file policy |

These are boundaries, not instructions to implement every service immediately. The first desktop milestone should only wire services required for basic offline gameplay and save/load.

## Design rules

- Shared code describes intent; adapters perform I/O and operating-system work.
- Optional services must not crash the game when unsupported or unavailable.
- Missing audio assets remain non-fatal.
- Haptics are optional and never affect game rules.
- The common game engine must not depend on Android `Context`, Android URI types, or Windows file path assumptions.
- Keep network access out of core game calculations.
- Keep public-key verification and LiveOps signature checks fail-closed on every platform.
- Do not weaken Lua sandboxing or move secret/private key material into the app.

## Suggested implementation order

1. Save repository boundary (after the existing save behavior is documented).
2. Audio/SFX interface, since missing assets should remain harmless.
3. Resource/config loading.
4. LiveOps/update abstractions only after desktop policy is defined.
5. Optional OS integrations such as notifications and haptics.

## Acceptance criteria

Each adapter has a clear owner, failure behavior, and tests appropriate to its target. Shared game logic can be exercised without a real Android device or Windows desktop.
