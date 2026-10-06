#!/usr/bin/env python3
"""Parse every JSON asset in the repository and validate localization source contracts."""
from __future__ import annotations
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOTS = [ROOT / "app" / "src" / "main" / "assets", ROOT / "assets"]
GAME_RULES = {
    "upgrades.json": "upgrade",
    "shop_items.json": "shop_item",
    "achievements.json": "achievement",
    "prestige_upgrades.json": "prestige",
    "prestige_shop.json": "prestige_shop",
    "ultra_upgrades.json": "ultra",
    "ultra_shop.json": "ultra_shop",
    "minigames.json": "minigame",
    "challenges.json": "challenge",
    "talents.json": "talent",
}

def load(path: Path):
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)

def items(root):
    if isinstance(root, dict):
        for value in root.values():
            if isinstance(value, list):
                for item in value:
                    if isinstance(item, dict):
                        yield item

def localization_key(prefix: str, item_id: str, suffix: str) -> str:
    """Build the canonical key without duplicating a prefix already in the asset ID."""
    normalized_id = str(item_id)
    if normalized_id.startswith(prefix + "_"):
        return f"{normalized_id}_{suffix}"
    return f"{prefix}_{normalized_id}_{suffix}"

def main() -> int:
    errors = []
    json_files = []
    for root in ASSET_ROOTS:
        if root.exists():
            json_files.extend(root.rglob("*.json"))

    for path in sorted(set(json_files)):
        try:
            load(path)
        except Exception as exc:
            errors.append(f"{path.relative_to(ROOT)}: invalid JSON: {exc}")

    locale_path = ROOT / "app" / "src" / "main" / "assets" / "locales" / "en_us.json"
    try:
        locale = load(locale_path)
    except Exception as exc:
        errors.append(f"canonical locale unavailable: {exc}")
        locale = {}

    game_root = ROOT / "app" / "src" / "main" / "assets" / "game"
    for filename, prefix in GAME_RULES.items():
        path = game_root / filename
        if not path.exists():
            continue
        try:
            root = load(path)
            for item in items(root):
                item_id = item.get("id")
                if not item_id:
                    continue
                for suffix in ("name", "desc"):
                    key = localization_key(prefix, item_id, suffix)
                    if key not in locale:
                        # Accept the legacy duplicated-prefix key while existing
                        # locale catalogs are migrated to the canonical form.
                        legacy_key = f"{prefix}_{item_id}_{suffix}"
                        if legacy_key not in locale:
                            errors.append(
                                f"{path.relative_to(ROOT)}: missing localization key {key!r}"
                            )
        except Exception:
            pass

    if errors:
        print("JSON asset validation FAILED:")
        for error in errors:
            print(f"  - {error}")
        return 1

    print(f"JSON asset validation PASSED: scanned {len(set(json_files))} JSON file(s).")
    return 0

if __name__ == "__main__":
    sys.exit(main())
