# LiveOps Reliability Test Plan

Use this checklist after the LiveOps reliability workflow passes and after installing the APK produced from the tested commit. Do not publish a production bundle just to test version handling.

## Automated checks

- [ ] Android unit tests pass, including duplicate script IDs and duplicate filenames.
- [ ] Android lint passes.
- [ ] LiveOps content repository validation workflow passes.
- [ ] Publisher tests reject duplicate IDs, duplicate filenames, unsafe paths, empty scripts and changed content that reuses or lowers a bundle version.

## Device checks

Use a development bundle and keep a known-good bundled activity available.

- [ ] **Online first install:** refresh a signed bundle and confirm the app reports the expected bundle version.
- [ ] **Idempotent refresh:** refresh the same version twice; the second attempt should report already up to date.
- [ ] **Correct and incorrect answers:** verify both answer paths and result messages.
- [ ] **Offline launch:** after a successful download, disable Wi-Fi and mobile data, force-stop the app, reopen it and launch the cached activity.
- [ ] **Offline refresh:** attempt refresh without a connection; it should report a recoverable error while cached activities remain available.
- [ ] **Tampered cache:** in a disposable debug install only, alter a cached Lua file and confirm it is rejected and recovery chooses a previously verified bundle or falls back to bundled content.
- [ ] **Interrupted update:** in a disposable debug install, interrupt a test download before activation; the currently active bundle should remain usable.
- [ ] **Rollback recovery:** with verified bundles 2 and 3 cached, make bundle 3 unusable in a disposable install. Confirm recovery can use bundle 2 while the highest accepted version remains 3. A subsequent manifest at version 2 must be rejected; version 3 should be eligible for reactivation.
- [ ] **No cache:** clear app data or use a fresh test install with networking disabled; the bundled featured activity should still work.

## Lua resource behavior

- [ ] Confirm an infinite loop is stopped by the instruction budget.
- [ ] Test a large table/string allocation on a disposable debug install and record memory behavior.
- [ ] Do not treat the instruction budget as a heap-memory limit. Only trusted scripts should be published until a separate memory-control strategy is evaluated.

## Pass criteria

A test passes only when the observed UI and logs match the expected behavior. Record APK run number, app version code, bundle version, connectivity state and relevant log lines. Do not use a production user's save data for tampering or interruption tests.
