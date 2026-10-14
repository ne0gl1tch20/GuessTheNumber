# Temporary Plan: First-Run Tours and Reliable Channel Updates

## 1. Feature-specific first-run tours

- Replace generic “Quick Tour” copy with a short, feature-specific tour for each major screen or feature.
- Localize tour titles, descriptions, button labels, and accessibility text through the app’s existing localization resources. Avoid hard-coded English strings.
- Explain the feature the player is actually looking at: what it does, why it matters, and the next useful action. Keep each tour concise and context-aware.
- Change the trigger flow: when a screen is opened for the first time, render the screen normally first, then show its first-time tour popup after the screen has appeared. Do not show the popup immediately because the user tapped a navigation item.
- Trigger after the destination screen is composed/visible and its transition has settled; avoid duplicate popups during recomposition, rapid navigation, or configuration changes.
- Persist completion/dismissal per feature so each tour appears only when appropriate. Include a way to replay tours from a help/tutorial entry point and preserve a skip/dismiss action.
- Verify first entry, returning visits, rapid navigation, process recreation, localization, accessibility, and replay behavior.

## 2. Make debug and release channel updates actually installable

- Resolve the update source and asset name from the selected channel, not from a shared hard-coded URL:
  - **Debug channel:** latest debug build artifact/archive, named `GuessTheNumber-debug.zip`.
  - **Release channel:** latest production release asset/archive for the release channel, using its corresponding stable asset name.
- Download the selected channel’s archive to app-private temporary storage. Show progress and handle network errors, missing assets, expired artifact links, cancellation, and insufficient storage.
- Safely inspect and extract the ZIP into a dedicated temporary directory. Reject path traversal entries, enforce reasonable size limits, and never overwrite app data or save files.
- Search the extracted directory recursively for `.apk` files. Validate that candidates belong to GuessTheNumber by checking package name/signing identity and version metadata where possible; do not install an arbitrary APK just because it was in the archive.
- If the archive contains a single compatible APK, prepare it for installation. If it contains split APKs, install the matching base and split APK set together using Android’s supported `PackageInstaller` session flow rather than trying to install split files individually. Handle unsupported/multiple candidate sets with a clear error.
- Check Android’s per-app “install unknown apps” permission. If not granted, explain why it is needed and open the system settings screen for this app; resume only after the user returns and grants permission.
- Launch the Android package installation flow. Android still requires the user to confirm the installation/update; do not attempt to bypass the system installer or claim updates can be silently installed.
- Preserve app data by using the same application ID and a compatible signing certificate. If signatures do not match or the version cannot update the installed app, show a clear actionable error rather than attempting a destructive reinstall.
- Keep release and debug channels isolated so selecting one never downloads the other channel’s artifact. Verify the artifact URL and metadata before downloading; for GitHub Actions artifacts, account for expiration and authentication/redirect requirements. For production releases, use the release’s actual published asset.
- Clean up temporary archives and extracted files after installation finishes or is cancelled, while retaining enough diagnostic information to troubleshoot failures.
- Keep updater status accurate: checking, downloading, extracting, validating, awaiting permission, awaiting installer confirmation, installed/failed/cancelled. Never report success before the installer result confirms it.
- Add focused tests for channel-to-asset selection, archive scanning and validation, safe extraction, no APK found, multiple APKs, permission flow, cancellation, network failures, and mismatched package/signature. Avoid emulator-heavy tests unless explicitly requested.

## Acceptance criteria

- First-time tour appears only after the destination screen is visible and is specific/localized to that feature.
- Reopening the same screen does not automatically repeat a completed or dismissed tour.
- Debug channel downloads the latest intended debug archive and release channel downloads the intended release asset.
- The updater safely finds and validates APKs in the downloaded ZIP, then opens the supported Android installation flow.
- Unknown-source permission is requested only when needed, and the user remains in control of confirming installation.
- Existing save data, channel separation, offline gameplay, and app signing expectations remain intact.