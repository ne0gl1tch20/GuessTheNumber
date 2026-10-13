#!/usr/bin/env python3
"""Release gate: keep locale asset metadata aligned with LocaleManager."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCALES_DIR = ROOT / "app" / "src" / "main" / "assets" / "locales"
MANAGER = ROOT / "app" / "src" / "main" / "java" / "com" / "jarrlyyy" / "guessthenumber" / "data" / "repository" / "LocaleManager.kt"
SUPPORTED_LOCALE = re.compile(
    r'SupportedLocale\(\s*"(?P<tag>[^"]+)"\s*,\s*"(?P<file>[^"]+)"(?P<tail>[^)]*)\)'
)


def main() -> int:
    errors: list[str] = []
    if not MANAGER.is_file():
        print(f"ERROR: missing locale manager: {MANAGER.relative_to(ROOT)}")
        return 1

    source = MANAGER.read_text(encoding="utf-8")
    supported: dict[str, tuple[str, bool]] = {}
    for match in SUPPORTED_LOCALE.finditer(source):
        tag = match.group("tag")
        filename = match.group("file")
        rtl = re.search(r"\brtl\s*=\s*true\b", match.group("tail")) is not None
        if filename in supported:
            errors.append(f"LocaleManager.kt: duplicate locale asset {filename}")
        supported[filename] = (tag, rtl)

    if not supported:
        print("ERROR: could not read supportedLocales from LocaleManager.kt")
        return 1

    asset_files = {path.name for path in LOCALES_DIR.glob("*.json")}
    supported_files = set(supported)
    for filename in sorted(supported_files - asset_files):
        errors.append(f"LocaleManager.kt: supported asset is missing: {filename}")
    for filename in sorted(asset_files - supported_files):
        errors.append(f"unregistered locale asset: {filename}")

    for filename, (expected_tag, expected_rtl) in sorted(supported.items()):
        path = LOCALES_DIR / filename
        if not path.is_file():
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"{filename}: cannot parse locale JSON: {exc}")
            continue
        if not isinstance(data, dict):
            errors.append(f"{filename}: locale root must be an object")
            continue
        if data.get("locale_tag") != expected_tag:
            errors.append(
                f"{filename}: locale_tag {data.get('locale_tag')!r} "
                f"does not match LocaleManager tag {expected_tag!r}"
            )
        asset_rtl = data.get("rtl", False)
        if not isinstance(asset_rtl, bool):
            errors.append(f"{filename}: rtl metadata must be a boolean when present")
        elif asset_rtl != expected_rtl:
            errors.append(
                f"{filename}: rtl metadata {asset_rtl!r} does not match "
                f"LocaleManager rtl={expected_rtl!r}"
            )

    if errors:
        print("Localization release gate FAILED:")
        for error in errors:
            print(f"  - {error}")
        return 1

    print(
        f"Localization release gate PASSED: {len(supported)} supported locale assets "
        "match LocaleManager tags and RTL direction."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
