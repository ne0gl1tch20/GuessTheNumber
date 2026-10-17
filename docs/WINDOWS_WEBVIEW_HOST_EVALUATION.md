# Windows Shared Web UI Host Evaluation

**Status:** Source-based feasibility assessment only. No Windows web host is implemented or selected by this document.
**Reviewed:** 2026-10-17
**Scope:** Find the smallest reversible way to render the same bundled HTML/CSS/JS in the existing Windows desktop app while keeping Kotlin authoritative.

## What the repository currently does

- `desktopApp` is a Kotlin Multiplatform JVM target using Compose Multiplatform.
- `desktopApp/build.gradle.kts` depends on `compose.desktop.currentOs`, packages an MSI, and defines the current Windows package version and a stable MSI `upgradeUuid`.
- The app entry point is `desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/Main.kt`, which creates a Compose Desktop `Window`.
- JNA and JNA Platform are already dependencies, but the source currently has no WebView2 host.
- The shared architecture decision requires bundled/offline web assets and a narrow, versioned bridge. Production screens remain Compose; this assessment does not authorize migration.

## Candidate assessment

| Candidate | Potential fit | Main cost / risk | Current decision |
|---|---|---|---|
| Microsoft Edge WebView2 hosted from the existing JVM process | Best target for an Edge-compatible web runtime on Windows and closest to the intended WebView2 direction | Microsoft's documented SDK surfaces focus on .NET, C++ and WinRT rather than a first-party Java API. JVM integration, native window embedding, COM/event lifetime, runtime detection and MSI install behavior must be proven. | **Preferred to investigate first, not yet selected.** |
| JavaFX WebView | JVM-facing web component may be easier to prototype | Uses JavaFX's WebKit-based engine rather than WebView2, so behavior and CSS/JS compatibility can differ; adds JavaFX packaging/integration considerations alongside Compose Desktop. | Fallback experiment only if a measurable requirement justifies it. |
| Bundled Chromium/JCEF or another browser engine | Can provide a consistent modern engine | Potentially large distribution footprint, native packaging and update/security maintenance; needs evidence it is worth duplicating a browser runtime. | Do not add without a measured need. |
| Handwritten WebView2 COM/JNA integration | Reuses existing JNA dependency and can keep the app JVM-based | Low-level native API and event/lifecycle complexity; HWND embedding into a Compose window and reliable disposal are unproven. | Avoid as the first implementation unless a minimal spike demonstrates the surface is maintainable. |

These are engineering candidates, not claims that any candidate already works in this application. Confirm supported versions, licenses and maintained bindings before selecting a dependency.

## Distribution constraints

Microsoft's WebView2 documentation says the runtime must be present for either Evergreen or Fixed Version deployment. Evergreen is generally recommended and updates separately from the app; the app still needs a missing-runtime path. For an entirely offline install path, Microsoft documents using the standalone Evergreen installer. Fixed Version provides tighter runtime control but adds a large runtime payload and requires the app publisher to service it.

For this project, the next spike must decide and document:
- What happens when the WebView2 Runtime is missing, damaged or too old.
- Whether the existing MSI installs or links to a runtime installer, and whether an offline installer is required.
- Whether runtime installation can happen without silently breaking existing upgrade/uninstall behavior or the MSI's existing UpgradeCode.
- Whether the runtime/user-data directory is writable and isolated appropriately.
- How startup errors are shown without preventing the existing Compose app from opening.
- Whether the exact same local asset bundle can load without network access.
- Whether the bridge accepts messages only from the expected local document/origin, validates a strict schema and exposes only named actions.
- How the host cleans up native handles, browser processes and callbacks when a screen closes or the app exits.

## Recommended next step: a Windows-only throwaway host spike

Keep the existing Compose screens untouched. Create a debug-only route/window that loads a tiny bundled HTML page and exchanges only `read_state` and `increment_counter` messages with transient Kotlin state. Do not connect it to game saves, currency, rewards, Lua/LiveOps or production navigation.

Acceptance checklist:
1. Build and launch on a supported Windows machine using the existing JVM/Compose toolchain.
2. Load packaged local HTML/CSS/JS with networking disabled.
3. Demonstrate a strict, versioned request/response contract with origin/page validation and bounded payloads.
4. Exercise resize, keyboard focus, tab order, screen-reader labels, back/close behavior and repeated open/close without leaked processes or handles.
5. Test the missing-runtime path and document the installer experience before touching MSI packaging.
6. Compare bundle size, startup time, idle memory and interaction latency against the current Compose-only app.
7. Confirm the existing MSI upgrade identity and save locations remain unchanged.
8. Add focused host-contract tests where feasible; separately record which tests and real-device/manual checks actually ran.

Only after the spike passes should a dependency and runtime distribution model be chosen. The Android prototype in PR #228 does not prove Windows feasibility or cross-platform visual parity.
