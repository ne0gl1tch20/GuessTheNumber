# GitHub LiveOps Script Bundles: Delivery Plan

**Status:** Future design only. Remote Lua loading is not implemented.

## Dedicated LiveOps content repository

Keep remote Lua content outside the Android app repository. The proposed repository name is `GuessTheNumber-LiveOps`; create it only when ready to set up the release pipeline.

Recommended top-level layout:

```text
schemas/
manifests/stable.json
manifests/staging.json
bundles/v1/events/
bundles/v1/minigames/
bundles/v1/ui/
bundles/v1/locales/
release-notes/
```

The Android repository remains the source for the Kotlin engine, Lua runtime, host API, bundled offline scripts, and trusted verification public key. The LiveOps repository contains content, schemas, and release metadata only; it must not be treated as an executable plugin that can access the app freely.

### Client source-selection policy

1. Verify the signed current manifest and compatibility before offering its featured activity.
2. Prefer a valid online activity when available and eligible.
3. If offline, use a previously verified cached bundle that remains compatible.
4. Otherwise launch a bundled activity from `assets/scripts/`.
5. If every source fails validation, show a recoverable unavailable state and keep the core game playable.

Do not download from arbitrary user-provided URLs or execute directly from a branch head. Use immutable release versions, signature verification, bounded staging, atomic activation, rollback, and Kotlin-validated rewards.

## Publishing flow

1. A developer authors an event definition and Lua scripts in the repository.
2. Static validation checks schemas, script IDs, API compatibility, locale keys, asset references, schedules, rewards and capability declarations.
3. CI runs Kotlin tests, script lint/syntax checks and event fixture tests.
4. A release job creates an immutable bundle manifest and content hashes, then signs the release metadata with a protected signing key.
5. The app fetches only a known release manifest from the configured endpoint. It verifies the signature before trusting URLs, hashes, version ranges or capabilities.
6. Files download to a staging directory with size limits. Every file hash, schema, API version and permission is checked before activation.
7. The app atomically switches to the validated bundle and keeps the previous known-good bundle for rollback.
8. Telemetry, if ever added, must be separately reviewed and opt-in/consistent with the game's privacy requirements. Do not add analytics just for Lua.

## GitHub is a content source, not the security boundary

- Do not execute a script just because it came from the repository's `master` branch.
- Do not treat HTTPS or a SHA-256 hash alone as author authentication.
- Verify a signature rooted in a public key embedded in the app or delivered through a separately trusted update mechanism.
- Use immutable release artifacts/tags rather than a mutable branch URL.
- Do not put private signing keys or access tokens in the app or public repository.
- Public content may be downloaded without a user token. Do not embed a personal access token in the APK.
- Limit bundle size, script count, execution time, memory, UI nodes and action frequency.
- Cache last-known-good content. Network failure must leave the installed game playable.
- Validate schedules using trusted host time, and define behavior for device clock changes and offline periods.
- Remote scripts never bypass Kotlin eligibility, claim limits, reward caps or save validation.

## Event manifest fields

Proposed event fields:
- stable event ID and event content version
- start/end timestamps and timezone policy
- script entry point and required host API version
- supported app-version range
- enabled feature flags and capability allowlist
- localized title/description keys
- declared assets
- reward definition approved by Kotlin validation
- eligibility, per-slot claim limit and repeatability
- content hashes and signature metadata
- fallback behavior and rollback compatibility

## Offline and failure behavior

- Ship at least one validated local content bundle when the app requires event UI to work offline.
- If download fails, continue using bundled or last-known-good content.
- If signature/schema/API validation fails, reject the entire candidate bundle.
- If a script crashes or exceeds a resource budget, cancel that script instance, clean up its UI and temporary state, log a bounded diagnostic, and return to the regular game.
- Do not grant partial rewards when an event handler fails midway. Reward application must be a single validated Kotlin transaction.
- Define whether an expired event remains visible, becomes read-only, or is removed; never infer event eligibility from UI visibility alone.

## Release gates

Remote execution stays disabled until:
- The local runtime sandbox and resource limits are proven.
- Signature verification and tamper rejection are tested.
- Bundle activation is atomic and rollback is tested.
- API/schema incompatibility is rejected.
- Offline behavior and cache corruption are tested.
- Save integrity and event reward limits pass automated tests.
- A non-economy event completes successfully on test devices.

The first rollout should be one small, non-economy-changing temporary minigame. Remote economy rewards come later, only after the Kotlin authorization path is independently tested.
