# G.T.N. Global Localization + Internationalization Master Plan

## Scope

This is a single implementation pass covering localization expansion, de-hardcoding, locale-aware formatting, RTL/CJK readiness, repository-wide asset inspection, and automated validation.

## Supported locales

- en-US
- en-GB
- fil-PH
- zh-CN
- zh-TW
- ja-JP
- ko-KR
- es-ES
- fr-FR
- de-DE
- it-IT
- pt-PT
- pt-BR
- ru-RU
- hi-IN
- id-ID
- th-TH
- vi-VN
- tr-TR
- pl-PL
- uk-UA
- nl-NL
- ar-SA

## Repository scan contract

Inspect the complete repository surface during the implementation pass, including:

- Kotlin/Compose source
- Android resources and XML
- `app/src/main/assets/**`
- `app/src/main/assets/locales/**`
- game/config JSON files
- tutorial/changelog/notification assets
- Markdown/text assets that are shipped or displayed
- tests
- Gradle configuration
- GitHub Actions workflows
- developer tooling and validation scripts

Machine-readable identifiers, schema keys, log tags, package names, encryption metadata, DataStore keys, and other internal constants remain stable and are not translated.

## Localization architecture

- Keep `en_us.json` as the complete canonical catalog.
- Use locale JSON overlays for translated content.
- Merge each selected overlay over the English catalog so missing translations safely fall back to English.
- Resolve locales through BCP-47 tags.
- Keep placeholders identical between baseline and translated values.
- Make adding future locales a file + registry change rather than a Kotlin architecture rewrite.

## De-hardcoding

Migrate user-visible strings from Kotlin/Compose, widgets, notifications, dialogs, tutorials, settings, developer UI, and data-driven assets into locale resources.

Do not localize machine-readable identifiers or internal schema values.

## Formatting

All user-visible dynamic values must use locale-aware formatting for:

- numbers
- decimals
- percentages
- currency
- dates
- times
- date-time
- durations
- plural-sensitive text
- large game numbers where applicable

## Android integration

Declare the supported locales for Android per-app language settings and keep the in-app picker synchronized with Android's application locale APIs.

## RTL/CJK

Verify Arabic RTL behavior and test Chinese, Japanese, and Korean for text expansion, wrapping, navigation, dialogs, cards, accessibility, and adaptive layouts.

## Notifications + widget engagement upgrade

Upgrade notifications and the Glance/widget system so they surface meaningful game state and give the player a natural reason to return, without relying on notification spam or fake urgency.

### Notification system

- Add a context-aware notification engine driven by the active save and real game state.
- Support daily guess challenges, meaningful progress reminders, upgrade proximity, Prestige/Ultra/Nebula milestones, Arcade score/streak events, achievements, and seasonal/live-ops events where applicable.
- Prefer event-driven notifications over generic "come back" messages.
- Add notification deduplication, cooldowns, per-category limits, and stale-event prevention.
- Respect notification permission state, Android notification-channel behavior, quiet hours, and user-configured preferences.
- Deep-link every actionable notification to the correct screen/save slot.
- Keep notification content fully localized and use locale-aware numbers, dates, times, durations, and game-number formatting.
- Ensure switching save slots, importing/exporting saves, migrating saves, deleting saves, and resetting progress cannot leave stale notification jobs behind.

### Widget system

- Upgrade the widget into a live mini game dashboard rather than a static shortcut.
- Small widget: current Money/progression plus a primary Play action.
- Medium widget: next-upgrade progress, current streak/best, progression state, and quick actions.
- Large widget: progression dashboard with upgrade progress, Arcade best, milestone state, and useful quick actions.
- Make widget content adapt to the player's current progression and recent events.
- Provide meaningful empty/no-save/import states.
- Refresh widgets after gameplay, upgrades, Prestige/Ultra/Nebula changes, save import/export, migration, save switching, settings changes, and relevant live-ops updates.
- Keep widget actions resilient when the referenced save slot no longer exists.
- Support localization, RTL, CJK text, accessibility labels, responsive sizing, and stale-data recovery.

### Notification + widget integration

- Use the same source-of-truth game state so notifications and widgets never disagree about progression.
- Example loop: widget shows upgrade progress → meaningful progress notification → notification opens the relevant screen → game action updates the widget.
- Do not manufacture progress, rewards, urgency, or activity solely to drive re-engagement.
- Keep a user-controllable notification settings surface with categories such as progress, milestones, Arcade, streaks, challenges, and live ops.

### Notification/widget automated test matrix

Add extensive automated coverage for:

- notification scheduling and cancellation
- duplicate notification prevention
- notification cooldowns
- per-category notification limits
- stale notification prevention
- notification permission denied/granted states
- Android notification channels and channel settings
- notification deep links
- deep links to the correct save slot
- save-slot switching while notifications are scheduled
- deleted save slots with pending notifications
- save migration with pending notifications
- save import replacing existing state
- save export/import round trips with notification state
- corrupted or missing save data
- app reinstall/first-launch notification state
- Android reboot/reschedule behavior
- timezone changes
- locale changes
- 12/24-hour formatting
- date/time formatting
- locale-aware large-number formatting
- notification placeholder integrity
- every notification category's localized title/body
- notification rate-limit behavior
- quiet-hours behavior
- user-disabled notification categories
- widget rendering with no save
- widget rendering with an active save
- widget rendering after save switching
- widget rendering after save deletion
- widget rendering after migration
- widget rendering after import/export
- widget rendering after gameplay changes
- widget rendering after upgrades
- widget rendering after Prestige/Ultra/Nebula changes
- widget rendering after Arcade score/streak changes
- widget rendering after settings changes
- widget refresh/recomposition behavior
- stale widget data detection and recovery
- widget action routing
- widget action routing when the target save is missing
- widget action routing from each supported size
- small/medium/large widget content contracts
- widget accessibility content descriptions
- RTL widget layouts
- Chinese/Japanese/Korean text wrapping
- long translated strings and text expansion
- Arabic RTL notification/widget content
- missing locale fallback to English
- notification/widget consistency against the canonical game state
- notification/widget behavior after app process death
- worker retry/idempotency behavior
- WorkManager constraint handling
- duplicate worker prevention
- widget update failure recovery
- notification failure recovery
- regression tests for every discovered notification/widget bug

## Automated tests

CI must remain manual-only through `workflow_dispatch`.

The workflow validates:

1. locale JSON contracts
2. all JSON assets
3. data-driven localization keys
4. repository localization surface
5. hardcoded UI string candidates
6. version metadata
7. Kotlin unit tests
8. Android lint
9. full Gradle check
10. test/lint report publication

Add more tests as new localization or UI systems are discovered. New functionality should not be accepted without appropriate automated coverage.

## Acceptance criteria

- Every supported locale has a valid overlay.
- Missing translations safely fall back to English.
- Placeholder signatures cannot silently diverge.
- Every applicable game JSON item has matching localization keys.
- User-visible hardcoded strings are progressively eliminated.
- Locale-aware formatting is used for dynamic display values.
- Android per-app language configuration matches the supported locale registry.
- RTL/CJK layouts remain usable.
- Manual CI remains the final verification gate.

## Current release

v1.15.0 / versionCode 15.
