# Seasonal Game Asset Audit

**Audited against:** `master` source tree  
**Scope:** `app/src/main/assets/` and the existing encrypted `game_data` pipeline  
**Status:** Source-tree inventory complete; runtime appearance and third-party licensing review remain open.

## Existing reusable assets

- The seasonal trait catalog contains ten dedicated SVG trait icons:
  - `AutumnOrbitTrait`
  - `BloomChaserTrait`
  - `EquinoxEchoTrait`
  - `FrostWhisperTrait`
  - `HarvestGlowTrait`
  - `LeaflightTrait`
  - `SolsticeSparkTrait`
  - `SpringOrbitTrait`
  - `SummerNovaTrait`
  - `WinterCometlightTrait`
- Each trait folder also contains declarative trait metadata, rules and English locale data. These are trait assets, not full-screen seasonal artwork.
- The Web Play icon folder currently contains the GuessTheNumber mark, sparkle and star SVGs.
- The UI SDK has a shared icon sprite. The repository also contains fonts, but fonts are not seasonal artwork.
- Existing UI celebration code emits small DOM particles and a `jarrlyy:particle-burst` event. This is a generic reward effect, not a seasonal snow/weather system.

## Confirmed gaps

The source asset tree does not currently contain a dedicated seasonal background pack, seasonal decorative overlay pack, or reusable snow particle artwork/configuration. The existing seasonal trait SVGs are small identity icons and should not be stretched into backgrounds.

Therefore, the first visual release needs purpose-made, lightweight seasonal art for the actual Play surface. Prefer a compact set of layered SVGs with transparent backgrounds and CSS-driven motion over a large raster pack or new rendering dependency. Keep original trait icons unchanged and reuse them only in contexts where a small emblem makes sense.

## Architecture constraints

- Add source assets under the canonical app asset tree so `tools/binarify_game_data.py` includes them in the existing encrypted game-data chunks.
- Do not add plaintext seasonal assets under APK `assets/` or bypass `GameDataArchive`.
- Keep theme configuration declarative and validated. It must not contain executable JavaScript or Lua.
- Use the existing reduced-motion and reduce-flashes preferences. Snow and ambient effects must have a static/off fallback.
- The existing four-season `TraitSeasonPolicy` controls seasonal trait eligibility. The new event-theme calendar must remain separate so event presentation never changes trait unlock eligibility.
- Android and Windows must consume the same shared theme calendar and canonical Web Play presentation.

## Recommended first art set

1. Christmas: lightweight snow particle layer and a subtle winter Play-surface treatment.
2. Halloween: decorative silhouettes/accents and a dark seasonal palette that preserves contrast.
3. New Year: celebratory sparkle/confetti accents using the existing particle event where appropriate.
4. Easter and Valentine's: small themed accents before considering full backgrounds.
5. Regional Philippine events: tasteful summer and Independence Day accents, with Back to School as a short configurable theme.

Do not mark any season visually complete until the real Play screen uses the assets and the reduced-motion fallback is verified.
