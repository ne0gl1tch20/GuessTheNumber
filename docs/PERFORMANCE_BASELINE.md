# Performance Baseline (PR #89)

**Status:** source review started; device measurements are pending. No performance numbers in this document should be treated as measured results.

## Purpose

Capture a repeatable before-optimization baseline for startup, first usable frame, navigation, save loading, memory, and battery. Use the same procedure and app commit for later comparisons. This is measurement and diagnosis work, not permission to optimize by guesswork.

## Current source-review observations

These are inspection notes, not proof of a user-visible performance problem:

- `MainActivity.onCreate` collects several `GameViewModel` state flows in Compose. Startup and first-frame timing should include the save-loading and crash-recovery paths, not only Activity creation.
- `SaveManager` reads and writes Preferences DataStore and serializes game state. Measure cold save load, save-slot switching, and persistence after a gameplay action separately.
- `WidgetRefresh` debounces widget refresh requests by 350 ms and performs updates in an IO coroutine. Check that bursts coalesce as expected and that refresh work doesn't repeatedly contend with foreground save operations.
- `MusicPlaybackService` schedules `updateSessionAndNotification()` on the main thread every 500 ms while the service is alive. Inspect its actual cost and lifecycle behavior during profiling before changing its cadence; do not assume the polling is a bottleneck from source inspection alone.
- `GameViewModel` owns game state and coordinates save operations, LiveOps, widgets, and audio. Profile representative actions before moving work between dispatchers or changing state-flow collection.

No main-thread blocking, leak, jank, battery drain, or memory regression is declared confirmed by these observations alone.

## Measurement setup

Record the following for every run:

| Field | Value |
|---|---|
| Commit SHA | Pending |
| App version / build variant | 3.0.2 / Debug or Release (record exact variant) |
| Device model | Pending |
| Android version and security patch | Pending |
| Build/install source | Pending |
| Battery level / power mode | Pending |
| Thermal state / device temperature if available | Pending |
| Locale, theme, font scale, reduced-motion setting | Pending |
| Save fixture | Fresh install / existing save / large-progress save |
| Run type | Cold / warm |
| Tool and version | Pending |
| Date and time | Pending |

Use a non-production test save and do not include personal save contents or secrets in reports.

## Repeatable scenarios

Run each scenario at least five times on each representative device, keeping the commit, variant, settings, and save fixture consistent. Report median and range; keep individual samples so outliers remain visible. Do not combine cold and warm runs.

1. **Cold launch:** force-stop the app, launch it, and measure from launch request to first frame and to the first usable Play screen. Repeat separately for a fresh install, a valid existing save, and a recoverable-save scenario if a safe fixture is available.
2. **Warm launch/resume:** background and resume the app, measuring time until the interface is responsive.
3. **First interactive frame:** capture system/app traces and identify the first frame where primary gameplay controls are visible and usable. Record launch-to-first-frame separately from launch-to-ready.
4. **Navigation:** measure transitions among Play, Upgrades, Shop, More, and one heavier screen such as World Map. Record dropped frames or jank from a system trace rather than estimating by feel.
5. **Save loading and slot switching:** measure existing-save load and switching between populated slots. Record failures and loading-state duration, not only successful runs.
6. **Save write:** perform a repeatable gameplay action that persists state; measure operation duration and verify the saved value after reopening. Do not stress-write real user data.
7. **Memory:** record process memory after startup, after visiting representative screens, after returning to Play, and after a repeatable longer session. Note whether memory stabilizes or keeps increasing.
8. **Battery/background behavior:** compare a fixed-duration idle session with the app foregrounded and with it backgrounded. Record device conditions and whether optional music playback is enabled. Do not infer battery drain from a short single run.
9. **Audio-service lifecycle:** when testing music, measure service startup and notification/session updates, then pause/stop playback and verify the service and periodic work stop as intended.

Prefer Android Studio's System Trace/CPU Profiler/Memory Profiler and Android's Macrobenchmark or equivalent repeatable launch tracing when available. If the device/tooling cannot support a scenario, mark it **SKIPPED** with the reason; do not substitute a guessed number.

## Results table

Populate only after measurements are actually collected.

| Scenario | Device / variant | Samples | Median | Range | Evidence link or trace file | Status |
|---|---|---:|---:|---:|---|---|
| Cold launch to first frame | Pending | — | — | — | — | NOT MEASURED |
| Cold launch to usable Play | Pending | — | — | — | — | NOT MEASURED |
| Warm resume to usable UI | Pending | — | — | — | — | NOT MEASURED |
| Play ↔ Upgrades transition | Pending | — | — | — | — | NOT MEASURED |
| Play ↔ World Map transition | Pending | — | — | — | — | NOT MEASURED |
| Existing-save load | Pending | — | — | — | — | NOT MEASURED |
| Populated save-slot switch | Pending | — | — | — | — | NOT MEASURED |
| Memory after repeatable session | Pending | — | — | — | — | NOT MEASURED |
| Fixed-duration battery observation | Pending | — | — | — | — | NOT MEASURED |

## Diagnostic checklist

- [ ] Look for synchronous disk access, `runBlocking`, or expensive work on the main thread along launch, navigation, and save paths.
- [ ] Check Compose recomposition counts/traces on frequently updating screens; confirm expensive calculations are not repeated unnecessarily.
- [ ] Check frame timeline/jank around navigation, large progression screens, animations, and state updates.
- [ ] Check save serialization, DataStore operations, and slot metadata queries separately.
- [ ] Check periodic work, widget refreshes, LiveOps refresh, and media-service callbacks for redundant or overlapping work.
- [ ] Check retained bitmaps, media players, coroutine jobs, callbacks, and screen state after repeated navigation and process recreation.
- [ ] Check CPU, memory, and battery under the same fixed-duration scenarios; record the evidence before changing code.
- [ ] Propose a focused fix only when a trace or repeatable measurement identifies a meaningful bottleneck.
- [ ] Repeat the same scenario after a change and report before/after measurements from the same setup.

## Acceptance criteria

- The baseline has exact commit, build variant, device/OS, scenario, and tool information.
- Startup-to-first-frame and startup-to-usable are distinct measurements.
- Cold and warm launches are reported separately with repeated samples.
- Save loading, slot switching, screen transitions, memory, and battery have measured results or an explicit **SKIPPED** reason.
- Source-review suspicions remain labeled as suspicions until profiling evidence supports them.
- No optimization or dependency is added merely to produce a benchmark.
- No build, test, emulator run, or GitHub Actions workflow is considered run unless there is verifiable evidence for that exact commit.
