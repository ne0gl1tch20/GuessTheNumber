# Windows Stage 2-D Shop Effects

## Scope

This pass wires the permanent Prestige/Ultra shop effects into the shared game engine so Windows and Android use the same effect rules.

## Implemented

- `prestige_luck_charm`
  - Adds 5 percentage points to critical-guess chance.
  - Critical chance is capped at 100%.
- `ultra_universal_harmony`
  - Guarantees a critical reward on every correct guess.
- `ultra_infinite_multiplier`
  - Doubles correct-guess currency rewards permanently while purchased.
- `prestige_auto_speed`
  - Defines a shared 1.5x auto-clicker frequency multiplier.
  - The Windows auto-clicker integration can consume this shared multiplier without duplicating the rule.
- `ultra_flex_badge`
  - Remains cosmetic/prestige-only because the catalog does not define a gameplay effect.

## Verification

Unit coverage was added for the permanent shop effects and the combined auto-clicker multiplier contract.

Actual Windows runtime verification remains separate until a Windows runtime session is available. This PR does not claim runtime validation of the desktop UI or auto-clicker timing.
