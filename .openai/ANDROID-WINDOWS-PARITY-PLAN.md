# Android + Windows Parity Implementation Plan

**Repository:** `ne0gl1tch20/GuessTheNumber`  
**Target:** `master`  
**Phases:** 1–4 only  
**Architecture rule:** Android is the behavioral reference. HTML + JarrlyyUI remains the player-facing Settings UI on both platforms. Kotlin/shared KMP owns authoritative state, validation, persistence and platform-neutral rules. Android and Windows only provide platform adapters.

## Phase 1 — Audit, shared UI and Settings parity

- Compare Android, shared KMP, Windows, HTML and JarrlyyUI implementations before changing behavior.
- Keep **HTML as the complete Settings UI** on Windows. Do not replace it with a native Windows Settings screen.
- Keep JarrlyyUI components/tokens for Windows-only Settings controls.
- Validate and persist Windows navigation position and launch-at-startup host messages.
- Keep Android-only controls platform-gated.
- Reuse shared Save Slot presentation and shared validation rules where practical.
- Audit developer overlay, Inspect/DevTools, right-click context menu and WebView lifecycle behavior.
- Audit long labels/values so visual truncation does not remove the full accessible value.

**Phase 1 finish:** shared HTML Settings is authoritative on both platforms, Windows-only controls work through validated host adapters, Save Slots use shared rules, and WebView/developer-tool regressions have contract coverage.

## Phase 2 — Save parity and WebPlay asset synchronization

- Treat Android `SaveManager` and its `EncryptedBackupBundle` format as the compatibility reference.
- Share save validation and Creative Mode models/rules through KMP.
- Windows supports slot creation, loading, switching, reset, duplication, profile editing, JSON import/export, complete bundle import/export, recovery backups, corruption fallback and atomic writes.
- Preserve Creative Mode data and permanent-mode restrictions.
- Windows supports Android-compatible formatVersion 2/3 encrypted Base64/XOR backup import/export, including settings, primary saves, backups and Creative Mode data.
- Failed imports restore the previous Windows files and active slot.
- Windows WebPlay sync validates manifest version/minimum app version, safe paths, required files and SHA-256 hashes, downloads only changed assets, stages activation, keeps an offline cache and rolls back once to the previous known-good version when startup validation fails.

**Phase 2 finish:** Windows save operations preserve user data and remain compatible with Android's supported backup format. Web assets update safely without replacing files under a running WebView.

## Phase 3 — Desktop runtime and Inno Setup migration

- Keep shared HTML destinations ahead of native fallbacks. Settings therefore stays HTML-driven.
- Queue screen-state reads behind active WebView requests instead of dropping them.
- Queue tutorial completion until the active bridge request finishes and always return a matching response.
- Keep popup routing in capture phase and preserve request IDs.
- Keep the developer overlay available in intended debug/release builds while Inspect/DevTools stays debug-only.
- Keep the WebView right-click context menu enabled in both build types.
- Replace MSI/WiX packaging with Inno Setup `.exe` packaging.
- Update Gradle, archive validation, updater, workflows and installation/upgrade/uninstall tests to use the Inno Setup executable.
- Preserve `%APPDATA%` player data across install, upgrade and uninstall.
- Keep CI artifacts at 7 days and only publish the stable debug Windows package on merged `master` pushes or explicit workflow dispatch.

**Phase 3 finish:** desktop runtime regressions are covered, Inno Setup is the only Windows installer path, updater/CI no longer require MSI, and user data survives supported installer operations.

## Phase 4 — Verification and delivery

Run and pass:

- Android validation/tests.
- Native/security validation.
- HTML/CSS/JS tests.
- Web asset manifest publication validation.
- Windows PR build tests.
- Windows desktop compile, JVM tests, Inno Setup packaging and Windows installer install/upgrade/uninstall/data-preservation checks.
- WebView bridge contract tests covering popup routing, pending requests, tutorial completion, callback validation, Settings routing, asset rollback and developer-tool policy.
- Save-store tests covering recovery, duplication, Creative Mode, JSON backups, Android-compatible backups and failed-import rollback.

Review the final diff for placeholder implementations, accidental schema/data loss, duplicated Settings UI, debug-only code leaking into release and unrelated workflow changes.

## Final Definition of Done

- [x] Four-phase plan is implemented on one dedicated branch.
- [x] HTML + JarrlyyUI is the authoritative Settings experience on Android and Windows.
- [x] Windows-only Settings operations are real, validated and persisted.
- [x] Windows save behavior covers the supported Android save features and recovery paths.
- [x] Android-compatible encrypted backup compatibility is implemented and tested.
- [x] WebPlay asset synchronization has validation, staging, offline fallback and rollback.
- [x] Beyond the Numbers/screen-state bridge regression is covered by the WebView contract.
- [x] Tutorial completion no longer gets lost behind an active bridge request.
- [x] Developer overlay/right-click behavior is covered; Inspect/DevTools remains debug-only.
- [x] Long navigation text uses accessible truncation patterns already present in the shared HTML surface.
- [x] Inno Setup is the Windows packaging/update path and MSI/WiX assumptions are removed.
- [x] Windows CI verifies install, upgrade, uninstall and data preservation.
- [x] All six PR-head GitHub Actions workflows pass on the final implementation commit.
- [x] One consolidated PR is ready to merge to `master`.

**Clear finish:** merge PR #392 only after the final PR head remains green. No phase is complete merely because code compiles.
