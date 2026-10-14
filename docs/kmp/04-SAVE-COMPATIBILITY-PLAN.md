# Save format and cross-platform compatibility plan

## Non-negotiable requirement

The Windows app must not make existing Android saves unreadable. A KMP migration is not permission to change the game's data model or encryption format without a separate, tested migration.

## Before moving persistence code

Inventory and document:
- current save version identifiers and JSON/schema shape,
- encrypted versus plaintext fields and key derivation/storage locations,
- save-slot behavior and backup/recovery paths,
- transaction/atomic-write behavior,
- settings and tutorial flags,
- localization/config data paths,
- import/export behavior, if present,
- and what happens when a save is truncated, malformed, or from a newer version.

Do not include secrets, key material, or actual user save contents in the audit document.

## Separate game state from storage

Shared code should model and validate game state. A platform-facing repository should own reading/writing bytes, atomic replacement, backup rotation, encryption key access, and recovery.

Possible conceptual boundary:

```kotlin
interface SaveStore {
    suspend fun read(slotId: String): ByteArray?
    suspend fun writeAtomically(slotId: String, bytes: ByteArray)
}
```

This is an example contract only, not an implemented API. The real interface must reflect the current encrypted format and transaction/recovery code.

Android can keep its current storage implementation while Windows receives a desktop implementation. If encryption uses platform keystore APIs, isolate key management from common game models instead of trying to share the Android keystore implementation.

## Compatibility tests required before a persistence migration

- Load a known legacy fixture.
- Save then reload without losing values.
- Verify all slots and settings survive.
- Simulate truncated/corrupt input and verify recovery behavior.
- Verify a failed write does not destroy the last known-good save.
- Verify a newer/unknown schema fails gracefully rather than silently resetting progress.
- Test Android upgrade from a save made by the currently released app.
- Test desktop-created saves and their intended import/export policy explicitly.

## Rollout rule

Keep the existing Android storage path until the replacement has equivalent behavior. Avoid dual-writing two formats unless a concrete migration needs it and the rollback semantics are specified. Never silently reset progression to make a new schema work.
