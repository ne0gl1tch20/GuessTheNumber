# Android + Windows Parity Implementation Plan

**Repository:** `ne0gl1tch20/GuessTheNumber`  
**Target branch for this plan:** `master`  
**Status:** Planning / implementation not yet started  
**Architecture rule:** Android is the behavioral reference. The existing HTML + JarrlyyUI Settings experience is shared by Android and Windows. Kotlin owns authoritative state, validation, persistence and platform-neutral rules; platform adapters handle OS-specific APIs.

## Goal

Deliver a Windows desktop build that matches Android's supported game behavior without maintaining duplicate, weaker implementations. Preserve the existing HTML-driven game UI and shared JarrlyyUI components. Add only the platform adapters required by Windows.

This work is complete only when all four phases are implemented, tested and the finish criteria at the end of this document pass.

---

## Phase 1 — Audit, shared architecture and Settings parity

### Work
1. Map the Android, shared KMP, desktop JVM, HTML and JarrlyyUI SDK code paths before editing.
2. Compare Android and Windows implementations of Settings, Save Slots, developer tools, navigation, onboarding and WebView bridge behavior. Record the exact source files and current gaps.
3. Keep **HTML as the Settings UI on both Android and Windows**. Do not replace it with a separate native Windows or Compose Settings screen.
4. Keep JarrlyyUI SDK components, tokens, typography, spacing and interaction patterns as the source of truth for Settings styling.
5. Move/share platform-neutral Settings models, defaults, validation and persistence behavior in shared Kotlin code where practical.
6. Keep Windows-only settings conditional and backed by Windows adapters:
   - Navigation bar position.
   - Launch at Windows startup.
7. Keep Android-only capabilities behind Android adapters. Do not show unsupported platform controls.
8. Make Settings state persist across app restarts and ensure HTML-to-host messages are validated and acknowledged.
9. Compare and align the shared Save Slots and Developer Settings UI entry points. Keep shared UI and behavior shared; use adapters only for platform-specific operations.
10. Audit long HTML labels and values. Truncate only the visible presentation when needed, while preserving the full value through an appropriate tooltip/title and accessible name. Hover must not be the only way to access the full text.
11. Trace and identify the root causes of:
    - Beyond the Numbers opening as a blank screen.
    - Tutorial completion causing the desktop game/bridge to disconnect.
    - Missing or incorrectly gated desktop developer overlay and right-click context menu.

### Exit checks
- Android and Windows both render the same HTML Settings screen and JarrlyyUI component system.
- Windows-only options appear only on Windows and perform real Windows operations.
- Settings values survive restart and invalid host messages cannot corrupt settings.
- No duplicate native Windows Settings UI has been introduced.
- The Save Slots / Developer Settings parity gaps and the three desktop regressions above have reproducible causes and regression tests or documented manual test steps.

---

## Phase 2 — Save system parity and reliable web asset synchronization

### Save system work
1. Treat Android SaveManager behavior and save compatibility as the reference.
2. Compare every persisted field, preference, schema version and mode restriction before sharing or porting logic.
3. Share save models, schema validation, migration rules and platform-neutral use cases through KMP where feasible.
4. Implement Windows storage adapters without weakening Android behavior.
5. Match supported behavior for:
   - Creating, loading, switching and deleting/resetting slots.
   - Duplicating slots and editing profile information.
   - Importing and exporting individual saves and complete bundles.
   - Backup creation, listing, restoration and retention.
   - Corruption detection, recovery and safe fallback.
   - Legacy save migration and schema upgrades.
   - Creative-mode data and permanent mode restrictions.
   - Atomic writes and protection against partial/corrupt replacement.
   - Encryption or equivalent platform-appropriate protection where the Android implementation requires it.
6. Preserve existing user data. Never silently reset, overwrite or downgrade a save when migration or validation fails.
7. Connect the shared Save Slots UI callbacks on Windows, including profile edit and reset actions.

### Web asset sync work
1. Port the Android WebPlayAssetSyncer lifecycle to Windows.
2. During startup/loading, read the lightweight version manifest and compare the installed version and file hashes.
3. Download only changed files. Cover HTML, CSS, JavaScript, fonts, locales and required libraries/assets.
4. Validate manifest structure, safe relative paths, required files, file sizes where specified and SHA-256 hashes before activation.
5. Stage updates separately from the active asset directory.
6. Activate the staged version atomically only after all validation passes.
7. Retain the previous known-good version and roll back if activation or startup validation fails.
8. Use bundled assets when offline, when the manifest is unavailable or when a downloaded update is invalid.
9. Never replace files underneath a running WebView. Define a safe activation point and preserve the current session until it is safe to switch.

### Exit checks
- Windows save behavior passes parity tests for every supported operation listed above.
- Existing saves remain readable; failed migrations leave the original data intact.
- Corrupt or incomplete writes recover safely without silent data loss.
- Asset sync downloads only changed files, rejects invalid hashes/paths, works offline and successfully rolls back.
- A clean install and an upgrade from an existing desktop data directory both work.

---

## Phase 3 — Desktop runtime, developer tools and installer migration

### Runtime and developer tools
1. Fix the confirmed root causes from Phase 1 for Beyond the Numbers and tutorial/bridge disconnections.
2. Verify bridge request IDs, pending-request handling, timeouts, lifecycle events, navigation and WebView reload behavior. One pending request must not silently prevent a required screen-state read.
3. Restore the developer overlay in both debug and non-debug builds where intended by the product behavior.
4. Keep Inspect/DevTools restricted to debug builds.
5. Restore the right-click context menu in both debug and non-debug builds. Do not accidentally hide it behind the debug-tools flag.
6. Keep native developer diagnostics and platform-specific tools behind the correct build/platform policy. Shared developer UI should remain shared.
7. Ensure missing optional SFX/assets or unavailable diagnostics do not crash the game.
8. Add regression coverage for screen routing, tutorial completion, bridge disconnects, context-menu behavior and debug-only Inspect access.

### Installer and update pipeline
1. Replace MSI/WiX packaging with an Inno Setup installer executable (`.exe`).
2. Add and maintain the Inno Setup script, including app metadata, install location, shortcuts, upgrade behavior and uninstall behavior.
3. Update Gradle packaging tasks and remove obsolete MSI/WiX assumptions where no longer needed.
4. Update Windows archive validation and the desktop debug updater to verify and launch the Inno Setup executable instead of invoking `msiexec.exe`.
5. Update GitHub Actions packaging, artifact naming, checksums/manifests and installation tests.
6. Preserve existing user saves and settings across upgrades and uninstall/reinstall scenarios according to the existing product data-retention policy.
7. Keep CI artifact retention at seven days or less. Avoid triggering stable debug package publication on every PR or arbitrary commit; publish it only on the intended merged-to-master path and explicit manual dispatch.

### Exit checks
- The desktop runtime passes the WebView, tutorial, routing and context-menu regression tests.
- Inspect/DevTools is unavailable in release builds while the overlay and right-click menu follow the intended availability in both build types.
- CI produces and validates the Inno Setup `.exe`; no updater path still expects MSI.
- Fresh install, in-place upgrade, uninstall and reinstall tests pass without unintended user-data loss.
- Workflow triggers and artifact retention match the rules above.

---

## Phase 4 — Full verification, compatibility and delivery

### Verification
1. Run shared KMP tests, desktop JVM tests, Android tests relevant to shared behavior, desktop compilation and packaging validation.
2. Run Windows-specific installer/update tests on a Windows runner. A successful compile alone is not runtime verification.
3. Test both debug and release behavior, including overlay, Inspect/DevTools and right-click menu availability.
4. Test Settings on Android and Windows, including restart persistence and Windows-only options.
5. Test save creation, loading, switching, editing, duplication, import/export, backup restore, corruption recovery, migration and creative-mode restrictions.
6. Test asset sync for first launch, no-change launch, changed assets, bad hash, invalid manifest/path, offline startup, failed activation and rollback.
7. Test Beyond the Numbers, screen re-entry, tutorial start/completion and repeated navigation while bridge requests are pending.
8. Test long text at narrow and wide window sizes, keyboard navigation and accessible names/tooltips.
9. Fix failures and rerun the affected checks. Do not dismiss a failing check or replace real behavior with a mock to make CI green.
10. Review the final diff for accidental data/schema changes, duplicated UI, placeholder implementations, secrets, debug-only code leaking into release and unrelated workflow churn.

### Delivery
- Keep implementation changes on a dedicated feature branch based on the current master.
- Open one consolidated pull request against `master` with a concise summary, test evidence, known limitations and a copy-pastable branch name.
- Wait for required checks and Windows runtime/installer verification. Fix failures and rerun them.
- Merge only when the implementation is complete and all required checks pass. Do not merge a plan-only or partially implemented PR.

### Final definition of done

This project is **DONE** only when every item below is true:

- [ ] The same HTML + JarrlyyUI Settings experience runs on Android and Windows.
- [ ] Shared settings behavior is backed by shared Kotlin logic; OS operations use platform adapters.
- [ ] Windows-only Settings options work and do not leak into Android.
- [ ] Windows save behavior matches the supported Android save features, including safety, recovery and compatibility.
- [ ] Windows web assets synchronize securely with staged activation, offline fallback and rollback.
- [ ] Beyond the Numbers no longer opens blank.
- [ ] Completing tutorials does not disconnect the game or its bridge.
- [ ] The developer overlay and right-click context menu work in both build types; Inspect/DevTools remains debug-only.
- [ ] Long text remains readable and accessible when visually truncated.
- [ ] Inno Setup produces a working installer and the updater/CI use the new format end-to-end.
- [ ] Existing user data survives supported upgrade and recovery paths.
- [ ] Required Android, shared, desktop and Windows CI/runtime checks pass.
- [ ] The consolidated PR is reviewed, all required checks are green and it is merged to `master`.

**No phase is considered complete because code merely exists or compiles. Each phase must pass its exit checks.**
