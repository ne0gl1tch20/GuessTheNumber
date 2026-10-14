# Temporary Bug Notes

## Play screen shows an unresolved Nebula reward placeholder

- **Status:** Fixed in PR #110.
- **Root cause:** `LocaleManager` had overlapping `getString(key, vararg)` and `getString(key, default, vararg)` overloads. A call such as `getString("reward_format", "25")` was resolved as the fallback overload, so `Reward: +%s Nebula` was returned without formatting.
- **Fix:** The locale layer now detects a two-argument format-string call and formats the catalog value while preserving normal fallback calls. A regression test verifies `Reward: +25 Nebula`.
