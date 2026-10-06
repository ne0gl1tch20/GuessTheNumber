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

v1.14.0 / versionCode 14.
