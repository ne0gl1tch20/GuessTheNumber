# KMP Foundation Status and Exit Criteria

**Status:** Foundation implementation milestone reached; device-level runtime parity remains a separate QA task.
**Reviewed:** 2026-10-15

## Purpose

This replaces the old temporary step-by-step migration checklist. Stop treating every reusable UI component as another migration phase. Separate the KMP foundation from Windows product feature parity.

## Track A: KMP foundation

**Goal:** Kotlin Multiplatform shares production logic, models/contracts and canonical assets wherever practical. The newly proposed UI direction is to share player-facing screens through one HTML/CSS/JavaScript frontend while Kotlin remains authoritative for game behavior. Windows still needs desktop-specific input/windowing and platform-service adapters. This proposal does not erase the existing KMP foundation or claim that a web migration has shipped.

### Implemented

- Shared deterministic guessing, reward, penalty, progression requirement and challenge-claim rules with common tests.
- Shared guess transition/state contracts with Android adapters retaining platform RNG and full GameState mutation.
- Platform-neutral destination/action contracts and a controlled shared guessing screen.
- Shared production presentation adapter, controlled numeric input, status-driven feedback card and feedback icon semantics.
- Android production Play uses shared input and feedback presentation without moving localization, rate limiting, rewards, persistence or platform services into common code.
- Android validation and Windows Desktop CI passed for shared UI integration PRs #159, #161, #163, #165, #167, #169 and #177.

### Architecture rules

- shared/src/commonMain: deterministic rules, shared models/contracts and platform-neutral logic without platform APIs.
- Current Android/Windows hosts still use their existing Compose UI until a web migration is approved and verified.
- Proposed web frontend: canonical HTML/CSS/JavaScript assets, accessible shared screens, presentation state and typed bridge calls. It must not become a second authority for rewards, eligibility or persistence.
- Android host: Android lifecycle/services, RNG integration, authoritative GameState mutation, BigNumber currency math, DataStore/save recovery, notifications, audio, haptics and Android-only features. Cordova is a candidate host/plugin bridge, subject to prototype validation.
- Windows host: Compose Desktop shell, desktop lifecycle/input/focus, filesystem/update integration, MSI packaging and a maintained embedded WebView implementation. Do not rely on Cordova's legacy Windows platform.
- Do not move a dependency into common code or add web dependencies just to increase a sharing percentage.
- Preserve Android save format, save slots, backup/import and recovery behavior.
- Keep the app offline-first: no ads, analytics or accounts.

### Foundation exit criteria

- [x] Shared domain rules and contracts exist with common tests.
- [x] Selected controlled guessing UI is shared and integrated into Android production Play.
- [x] Platform-specific services remain outside common code.
- [x] Recent Android and Windows CI checks passed for the cited integration PRs.
- [ ] Perform a source audit that commonMain has no accidental platform-only dependency.
- [ ] Record the shared-vs-host boundary if architecture changes materially.
- [ ] If the shared-web proposal is approved, prototype one small screen and a Kotlin-authoritative action bridge before expanding the migration.

The implementation milestone can now be treated as **KMP foundation complete for the current agreed scope**. The unchecked source audit is a short verification task, not permission to reopen endless component extraction. Android runtime checks remain post-foundation QA; unrun checks must not be reported as passing.

## Track B: Windows feature parity

Windows parity is the exact-port product goal, not merely a feature checklist. Follow the [Windows Feature Parity Plan](WINDOWS_FEATURE_PARITY_PLAN.md), which defines Android as the source of truth and requires screen-by-screen visual, interaction and behavior evidence. Reuse shared Kotlin code and assets wherever practical; keep host-specific services behind adapters.

## Post-foundation QA

Use the [runtime parity checklist](../.openai/kmp_runtime_parity_checklist.md) for Android runtime checks. Record actual outcomes and evidence. CI success does not establish device-level behavior.

For Windows, record feature-specific results for keyboard/mouse behavior, resizing, locale, offline behavior, persistence, update-channel isolation and MSI upgrades.

## Working rules

1. New KMP changes must fix a demonstrated issue or directly support an agreed cross-platform feature. No tiny extraction PRs just to increase reuse.
2. Plan feature work as a complete slice: behavior/state, UI/actions, localization, persistence impact, tests and platform-specific implementation.
3. Mark platform-specific features explicitly instead of forcing identical implementations.
4. Do not claim runtime or installer checks passed without evidence.
5. Normal app feature work may proceed while runtime QA is scheduled separately.
