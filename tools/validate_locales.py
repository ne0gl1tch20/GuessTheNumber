#!/usr/bin/env python3
"""Validate Guess The Number locale JSON files.

The English (US) locale is the canonical key set. Every other locale must:
- contain every canonical key
- avoid keys that do not exist in the canonical locale
- use the same printf-style placeholder signature for shared keys
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCALES_DIR = ROOT / "app" / "src" / "main" / "assets" / "locales"
BASELINE = "en_us.json"

# Covers the formats used by LocaleManager/String.format:
# %s, %d, %f, %.2f, %1$s, %02d, etc.
PLACEHOLDER_RE = re.compile(r"%(?!%)(?:(?:\d+)\$)?[-+#0(]*\d*(?:\.\d+)?[a-zA-Z]")


def load_locale(path: Path) -> dict[str, object]:
    try:
        with path.open("r", encoding="utf-8") as handle:
            value = json.load(handle)
    except json.JSONDecodeError as exc:
        raise ValueError(f"{path.name}: invalid JSON at line {exc.lineno}, column {exc.colno}: {exc.msg}") from exc

    if not isinstance(value, dict):
        raise ValueError(f"{path.name}: locale root must be a JSON object")

    return value


def placeholders(value: object) -> list[str]:
    if not isinstance(value, str):
        return []
    return PLACEHOLDER_RE.findall(value)


def main() -> int:
    if not LOCALES_DIR.is_dir():
        print(f"ERROR: Locale directory not found: {LOCALES_DIR}")
        return 1

    files = sorted(LOCALES_DIR.glob("*.json"))
    if not files:
        print(f"ERROR: No locale JSON files found in {LOCALES_DIR}")
        return 1

    baseline_path = LOCALES_DIR / BASELINE
    if not baseline_path.exists():
        print(f"ERROR: Baseline locale not found: {baseline_path}")
        return 1

    errors: list[str] = []

    locales: dict[str, dict[str, object]] = {}
    for path in files:
        try:
            locales[path.name] = load_locale(path)
        except ValueError as exc:
            errors.append(str(exc))

    if BASELINE not in locales:
        return 1

    baseline = locales[BASELINE]
    baseline_keys = set(baseline)

    for filename, locale in locales.items():
        if filename == BASELINE:
            continue

        keys = set(locale)
        missing = sorted(baseline_keys - keys)
        extra = sorted(keys - baseline_keys)

        if missing:
            errors.append(f"{filename}: missing {len(missing)} key(s): {', '.join(missing)}")
        if extra:
            errors.append(f"{filename}: extra {len(extra)} key(s): {', '.join(extra)}")

        for key in sorted(baseline_keys & keys):
            expected = placeholders(baseline[key])
            actual = placeholders(locale[key])
            if expected != actual:
                errors.append(
                    f"{filename}: placeholder mismatch for '{key}': "
                    f"expected {expected or 'none'}, got {actual or 'none'}"
                )

    if errors:
        print("Localization validation FAILED:")
        for error in errors:
            print(f"  - {error}")
        return 1

    print(f"Localization validation PASSED: {len(files)} locale(s), {len(baseline_keys)} key(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
