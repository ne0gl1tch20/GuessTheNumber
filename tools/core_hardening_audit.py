#!/usr/bin/env python3
"""Read-only cross-core hardening audit for GuessTheNumber.

This audit is intentionally static and offline. It does not build the app, start
Android emulators, access the network, or modify saves. Missing optional audio
assets are reported as informational because the app must work without them.
"""
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
LOCALE_DIR = ROOT / "app/src/main/assets/locales"
KOTLIN = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber"
TESTS = ROOT / "app/src/test/java/com/jarrlyyy/guessthenumber"
SCRIPTS = ROOT / "app/src/main/assets/scripts"
SFX_DIR = ROOT / "app/src/main/assets/sfx"

PLACEHOLDER = re.compile(r"%(?:[0-9]+[$])?[-#+ 0,(]*[0-9]*(?:[.][0-9]+)?[a-zA-Z%]")
REQUIRED_SETTINGS_DESCRIPTIONS = (
    "settings_desc_language",
    "settings_desc_theme_mode",
    "settings_desc_theme_preset",
    "settings_desc_theme_colors",
    "settings_desc_number_notation",
    "settings_desc_notifications",
    "settings_desc_reminder_frequency",
    "settings_desc_sound_fx",
    "settings_desc_vibration",
    "settings_desc_reduced_motion",
    "settings_desc_master_volume",
    "settings_desc_sfx_volume",
)

errors: list[str] = []
warnings: list[str] = []
notes: list[str] = []


def load_json(path: Path) -> dict[str, Any] | None:
    duplicates: list[str] = []

    def object_pairs(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for key, value in pairs:
            if key in result:
                duplicates.append(key)
            result[key] = value
        return result

    try:
        parsed = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=object_pairs)
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        errors.append(f"{path.relative_to(ROOT)}: invalid/unreadable JSON ({exc})")
        return None
    if not isinstance(parsed, dict):
        errors.append(f"{path.relative_to(ROOT)}: catalog root must be a JSON object")
        return None
    if duplicates:
        errors.append(f"{path.relative_to(ROOT)}: duplicate keys: {', '.join(sorted(set(duplicates)))}")
    return parsed


def placeholders(value: Any) -> Counter[str]:
    return Counter(PLACEHOLDER.findall(value)) if isinstance(value, str) else Counter()


def main() -> int:
    if not LOCALE_DIR.is_dir():
        print(f"ERROR: locale directory not found: {LOCALE_DIR}")
        return 2

    locale_files = sorted(LOCALE_DIR.glob("*.json"))
    if not locale_files:
        errors.append("No locale JSON catalogs were found.")

    catalogs = {path.name: load_json(path) for path in locale_files}
    catalogs = {name: data for name, data in catalogs.items() if data is not None}
    english = catalogs.get("en_us.json")
    if english is None:
        errors.append("Required baseline catalog app/src/main/assets/locales/en_us.json is missing.")

    if english is not None:
        english_keys = set(english)
        for name, catalog in catalogs.items():
            missing = sorted(english_keys - set(catalog))
            extra = sorted(set(catalog) - english_keys)
            if missing:
                warnings.append(f"{name}: {len(missing)} English fallback keys are absent (first 8: {', '.join(missing[:8])})")
            if extra:
                notes.append(f"{name}: {len(extra)} locale-specific keys")

            for key in sorted(set(catalog) & english_keys):
                if key.startswith("settings_desc_") and not isinstance(catalog[key], str):
                    errors.append(f"{name}: {key} must be a string")
                expected = placeholders(english[key])
                actual = placeholders(catalog[key])
                if expected != actual and isinstance(english[key], str) and isinstance(catalog[key], str):
                    errors.append(
                        f"{name}: placeholder mismatch for {key}: "
                        f"English={dict(expected)} locale={dict(actual)}"
                    )

        for name, catalog in catalogs.items():
            absent = [key for key in REQUIRED_SETTINGS_DESCRIPTIONS if key not in catalog]
            if absent:
                warnings.append(f"{name}: missing settings descriptions: {', '.join(absent)}")

    # Inventory checks keep the audit useful even when a subsystem is optional.
    expected_paths = {
        "save validation tests": TESTS / "data/SaveValidationTest.kt",
        "localization tests": TESTS / "data/repository/LocaleManagerTest.kt",
        "game engine tests": TESTS / "domain/GameEngineTest.kt",
        "save manager": KOTLIN / "data/store/SaveManager.kt",
        "locale manager": KOTLIN / "data/repository/LocaleManager.kt",
        "game view model": KOTLIN / "ui/viewmodel/GameViewModel.kt",
        "settings screen": KOTLIN / "ui/screens/SettingsScreen.kt",
    }
    for label, path in expected_paths.items():
        if not path.is_file():
            errors.append(f"Expected {label} file not found: {path.relative_to(ROOT)}")

    lua_files = sorted(SCRIPTS.rglob("*.lua")) if SCRIPTS.is_dir() else []
    if not lua_files:
        warnings.append("No bundled Lua scripts found; verify whether the Lua activity path is enabled.")
    else:
        notes.append(f"Lua inventory: {len(lua_files)} bundled script(s); this static audit does not prove runtime sandbox enforcement.")

    if SFX_DIR.is_dir():
        sfx_count = sum(1 for path in SFX_DIR.rglob("*") if path.is_file())
        notes.append(f"SFX inventory: {sfx_count} file(s) under assets/sfx.")
    else:
        notes.append("assets/sfx/ is absent; this is allowed. Missing SFX assets must remain a non-fatal condition.")

    print("GuessTheNumber cross-core hardening audit")
    print(f"Locale catalogs inspected: {len(catalogs)}")
    print(f"Bundled Lua scripts: {len(lua_files)}")
    for item in notes:
        print(f"INFO: {item}")
    for item in warnings:
        print(f"WARN: {item}")
    for item in errors:
        print(f"ERROR: {item}")
    print(f"Result: {len(errors)} error(s), {len(warnings)} warning(s)")
    if errors:
        return 1
    if warnings:
        print("Static checks passed with warnings; warnings are review items, not proof of runtime defects.")
    else:
        print("Static checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
