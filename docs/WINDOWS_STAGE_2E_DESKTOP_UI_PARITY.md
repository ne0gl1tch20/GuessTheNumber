# Windows Stage 2-E: Desktop UI Organization

**Status:** Candidate implementation on `feat/windows-stage-2e-desktop-ui-parity`. Runtime verification remains open.

## What changed

- Added dedicated Windows destinations for **Play**, **Progress**, **Economy** and **About**.
- Kept guessing input, submission feedback, mutators and auto-clicker controls on Play.
- Grouped daily quests, daily progress, treasure chest counts, achievements and world progression on Progress.
- Grouped Money/Prestige/Ultra upgrades, resets, Nebula shop items and talents on Economy.
- Moved Windows Debug update discovery/install controls to About so they no longer occupy the gameplay screen.
- Added Ctrl+1, Ctrl+2, Ctrl+3 and Ctrl+4 navigation shortcuts. Escape returns to Play, and Enter is not consumed by shell navigation.
- Updated shell shortcut tests and the player-facing development changelog.

## Validation status

- **Source review:** completed for the destination routing and section gates.
- **Automated tests:** updated; not independently run from this session.
- **Windows runtime:** not verified in this session. Check resize behavior, keyboard focus, all four destinations, purchase/reset interactions, updater controls and save/reload.
- **Parity gate:** still open. This UI slice does not implement the missing Android destinations or claim full Windows parity.

## Scope boundary

No save schema, economy rules, reward calculation, update-channel behavior or MSI identity changes are intended in this slice.
