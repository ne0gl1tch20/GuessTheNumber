#!/usr/bin/env python3
"""Static contract checks for the player-facing gameplay trait loadout."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCALE = ROOT / "app/src/main/assets/locales/en_us.json"
WEB_JS = ROOT / "app/src/main/assets/web-play/app.js"
WEB_HTML = ROOT / "app/src/main/assets/web-play/index.html"
HOST = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ProductionWebPlayScreen.kt"

required = {
    "traits_title", "traits_intro", "traits_intro_short", "traits_loading",
    "traits_summary_format", "traits_empty", "traits_active", "traits_locked_format",
    "traits_choose_variant", "traits_automatic", "traits_unlock_hint", "traits_footer",
    "traits_saving", "trait_combo_momentum_title", "trait_combo_momentum_description",
    "trait_range_scout_title", "trait_range_scout_description",
    "trait_mistake_insurance_title", "trait_mistake_insurance_description",
    "trait_risk_reward_title", "trait_risk_reward_description",
    "trait_streak_guard_title", "trait_streak_guard_description",
    "trait_arm_steady_title", "trait_arm_steady_description",
    "trait_arm_burst_title", "trait_arm_burst_description",
    "trait_arm_half_refund_title", "trait_arm_half_refund_description",
    "trait_arm_careful_title", "trait_arm_careful_description",
    "trait_arm_balanced_title", "trait_arm_balanced_description",
    "trait_arm_high_risk_title", "trait_arm_high_risk_description",
}
strings = json.loads(LOCALE.read_text(encoding="utf-8"))
missing = sorted(key for key in required if not strings.get(key))
assert not missing, f"Missing English fallback strings: {missing}"

js = WEB_JS.read_text(encoding="utf-8")
for key in ("traits_summary_format", "traits_empty", "traits_active", "traits_locked_format",
            "traits_choose_variant", "traits_automatic", "traits_unlock_hint", "traits_saving"):
    assert f"uiText('{key}'" in js, f"Trait UI does not use localized key {key}"
assert "data-bridge-action='trait-arm'" in js or 'data-bridge-action="trait-arm"' in js
assert "select_trait_arm" in js
html = WEB_HTML.read_text(encoding="utf-8")
for key in ("traits_title", "traits_intro", "traits_loading", "traits_footer"):
    assert f'data-i18n="{key}"' in html, f"Trait screen is missing localized HTML key {key}"
host = HOST.read_text(encoding="utf-8")
for key in required:
    assert f'"{key}"' in host, f"Host does not send localized string {key}"
print(f"Gameplay trait loadout localization passed ({len(required)} fallback strings).")
