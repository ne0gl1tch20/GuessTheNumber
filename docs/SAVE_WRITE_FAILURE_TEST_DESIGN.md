# Save-write failure test design

**Status:** design only. This document proposes a narrow seam for deterministic persistence-failure tests. It does not change save behavior or claim that failures are already handled correctly.

## Current behavior to preserve

- `SaveManager.saveGame` returns `true` only after its DataStore edit completes and returns `false` when an exception is caught.
- `SaveManager.updateGameAtomically` returns the updated state after its edit completes, and `null` for invalid/missing primary data or a caught exception.
- Both methods update primary and backup within a single Preferences DataStore edit. Tests must prove that a failed edit leaves the last committed primary and backup unchanged.
- The current ViewModel challenge-claim path can publish state before asynchronous persistence completes. Do not change claim ordering until the persistence result contract and concurrency behavior are tested.

## Proposed test seam

Prefer constructor injection of a `DataStore<Preferences>` into `SaveManager`, with the normal Android constructor continuing to use `context.saveDataStore`. Keep the seam internal/test-oriented and avoid a global mutable override or production-only failure flag.

A test DataStore wrapper can delegate reads and normal writes to the real test DataStore, while throwing a deterministic `IOException` from `updateData` when failure is enabled. This exercises the same DataStore edit boundary that production uses, rather than simulating a validation failure. If the project's DataStore version or constructor shape makes this wrapper awkward, use a tiny internal storage interface around the exact read/edit operations instead; do not add a broad storage framework.

## Required tests before changing transaction semantics

1. **saveGame write failure:** seed a valid primary and valid recovery backup, inject an edit failure, call `saveGame`, assert `false`, then read with a non-failing manager and assert both stored JSON values are byte-for-byte unchanged.
2. **atomic update write failure:** seed a valid primary and backup, inject an edit failure, call `updateGameAtomically`, assert `null`, and assert both persisted values remain unchanged.
3. **success control:** run the same setup without failure and verify primary advances and backup contains the previous valid primary.
4. **failure recovery:** after the injected failure is removed, load through a fresh manager and verify the last committed state is still readable.
5. **ViewModel behavior:** only after the storage-level tests are stable, add a deterministic persistence result seam around the claim-save operation. Verify a failed save does not report a durable success or permit a duplicate reward, and verify newer gameplay state is not replaced by a stale disk snapshot.

## Constraints

- No real disk-full simulation, emulator dependency, sleeps, or workflow polling.
- Do not modify save JSON, keys, backup format, migration behavior, slot count, or the current XOR/Base64 format in this work.
- Do not treat the XOR/Base64 backup encoding as modern cryptographic protection.
- Keep production changes separate from tests if the seam reveals any ambiguity about desired failure behavior.
- Run Android unit tests and Windows shared/desktop validation. Fix genuine failures before merging code-bearing PRs.

## Acceptance criteria

The injected exception must reach the same catch path as a real DataStore edit failure; tests must assert return values and persisted primary/backup state, not only that an exception occurred. A follow-up implementation PR may change ViewModel state publication only after these tests establish the current storage contract.
