# Temporary Bug Notes

## Play screen shows an unresolved Nebula reward placeholder

- **Location:** Play screen.
- **Observed text:** `Reward: +%s Nebula`
- **Problem:** The `%s` format placeholder is being displayed literally instead of being replaced with the actual reward amount.
- **Expected behavior:** Display the formatted reward amount, for example `Reward: +25 Nebula` when the reward is 25.
- **Investigate:** Check the Play screen's reward label and localization string formatting. Ensure the format string receives the correct amount and uses the formatting API expected by that localization system. Check all relevant locales so the placeholder is consistently handled.
- **Acceptance criteria:** No raw `%s` placeholder appears in the Play screen; the displayed amount matches the actual Nebula reward and remains correctly localized.
