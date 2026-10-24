# Signed Trait LiveOps and Built-in Trait Packages

## Built-in catalog

The offline catalog contains **316 validated trait definitions across 30 catalog folders**. Each trait has its own folder with four checked-in package files:

- `trait.json`: stable ID, catalog, category, visibility, effects and display keys
- `rules.json`: activation thresholds, dependencies, conflicts and allowed effects
- `locales/en.json`: English title and description
- `icon.svg`: local icon asset

Catalog folders contain a `catalog.json` index listing their exact trait IDs. Unit tests compare every package against the Kotlin registry, verify all required files exist, parse the JSON, check the activation thresholds/effects and reject obvious active SVG content. This prevents a folder from counting as a trait unless its definition is present in the authoritative Kotlin registry.

## Authority and execution

Kotlin remains the source of truth for trait eligibility, hidden discoveries, supported effects and reward mutations. The package JSON files are validated content metadata, not arbitrary executable code. Remote Lua/JavaScript/HTML/CSS is **not** executed by this trait catalog. Do not add scripts to a package unless a restricted, tested runtime is implemented for that file type.

Current supported effect types are a closed allowlist. Nebula claim bonuses are applied through `TraitRewardPolicy` with the existing cap. Cosmetic badge traits are represented in the activation result. Existing XP/streak effect types remain descriptive until their dedicated gameplay systems consume them.

## Remote catalog protocol

- Signed, versioned JSON manifest; RSA signature over exact payload bytes and SHA-256 payload verification.
- HTTPS only, strict schema/host/app compatibility, stable IDs, dependency/conflict validation and no built-in ID replacement.
- Catalog-specific file paths use `assets/traits/catalogs/{catalogId}/{folderName}/...`.
- Per-file and total bundle limits, file hashes, path boundary checks, staging and active pointer switching.
- Cached manifests and assets are re-verified before loading. Rollback protection remains enabled.
- Empty signing configuration fails closed; built-in traits remain available offline when remote updates are unavailable.

## Adding a trait

1. Add a stable ID and declarative definition to `BuiltInTraits.kt`.
2. Give it a unique catalog ID and package folder.
3. Add the four package files and update the catalog's `catalog.json` index.
4. Run the registry/package validation tests and Android unit tests.
5. Keep rewards and state changes inside Kotlin. Never trust package metadata to award currency directly.

## Healthy engagement

Traits are optional gameplay variety and collection milestones. No forced daily check-ins, streak-loss punishment, FOMO timers or guilt notifications are part of this system.
