#!/usr/bin/env python3
"""Validate the repository's JSON locale overlays and formatting contracts."""
from __future__ import annotations
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCALES_DIR = ROOT / "app" / "src" / "main" / "assets" / "locales"
BASELINE = "en_us.json"
EXPECTED = {
    "en_us.json": "en-US", "en_gb.json": "en-GB", "fil_ph.json": "fil-PH",
    "zh_cn.json": "zh-CN", "zh_tw.json": "zh-TW", "ja.json": "ja-JP",
    "ko.json": "ko-KR", "es.json": "es-ES", "fr.json": "fr-FR",
    "de.json": "de-DE", "it.json": "it-IT", "pt_pt.json": "pt-PT",
    "pt_br.json": "pt-BR", "ru.json": "ru-RU", "hi_in.json": "hi-IN",
    "id.json": "id-ID", "th.json": "th-TH", "vi.json": "vi-VN",
    "tr.json": "tr-TR", "pl.json": "pl-PL", "uk.json": "uk-UA",
    "nl.json": "nl-NL", "ar.json": "ar-SA",
}
PLACEHOLDER_RE = re.compile(r"%(?!%)(?:(?:\d+)\$)?[-+#0(]*\d*(?:\.\d+)?[a-zA-Z]")
REQUIRED_SHARED_KEYS = {
    "music_player_title", "music_no_track", "music_import", "music_library",
    "music_queue_format", "music_queue_position", "music_playlist_name",
    "music_create_playlist", "music_playlists", "music_playlist_tracks",
    "music_empty_library", "music_empty_queue", "music_empty_playlists",
    "music_shuffle", "music_repeat",
    "accessibility_warning",
    "save_slot_name_label",
    "music_control_previous", "music_control_pause", "music_control_play",
    "music_control_next", "music_control_stop",
}

def load(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        value = json.load(handle)
    if not isinstance(value, dict):
        raise ValueError(f"{path.name}: locale root must be an object")
    return value

def placeholders(value: object) -> list[str]:
    return PLACEHOLDER_RE.findall(value) if isinstance(value, str) else []

def main() -> int:
    errors: list[str] = []
    baseline_path = LOCALES_DIR / BASELINE
    if not baseline_path.exists():
        print("ERROR: missing en_us.json")
        return 1
    try:
        baseline = load(baseline_path)
    except Exception as exc:
        print(f"ERROR: invalid baseline: {exc}")
        return 1
    baseline_keys = set(baseline)

    for filename, tag in EXPECTED.items():
        path = LOCALES_DIR / filename
        if not path.exists():
            errors.append(f"missing locale file: {filename}")
            continue
        try:
            data = load(path)
        except Exception as exc:
            errors.append(f"{filename}: invalid JSON: {exc}")
            continue

        if data.get("locale_tag") != tag:
            errors.append(f"{filename}: locale_tag must be {tag!r}")
        if not data.get("locale_language"):
            errors.append(f"{filename}: missing locale_language")
        if not data.get("locale_code"):
            errors.append(f"{filename}: missing locale_code")

        for key in REQUIRED_SHARED_KEYS:
            if key not in data:
                errors.append(f"{filename}: missing required shared UI key {key!r}")
            elif not isinstance(data[key], str):
                errors.append(f"{filename}: shared UI key {key!r} must be a string")

        for key in data:
            if key not in baseline_keys:
                # Metadata is allowed to differ from the canonical translation set.
                if key not in {"locale_language", "locale_code", "locale_tag", "rtl"}:
                    errors.append(f"{filename}: unknown translation key {key!r}")
        for key in set(data) & baseline_keys:
            expected = placeholders(baseline[key])
            actual = placeholders(data[key])
            if expected != actual:
                errors.append(f"{filename}: placeholder mismatch for {key!r}: expected {expected}, got {actual}")

    if errors:
        print("Localization validation FAILED:")
        for error in errors:
            print(f"  - {error}")
        return 1

    print(f"Localization validation PASSED: {len(EXPECTED)} locale overlays + canonical locale.")
    print(f"Canonical translation keys: {len(baseline_keys)}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
