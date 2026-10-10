#!/usr/bin/env python3
"""Deep repository-wide localization surface audit.

Reports user-facing literals that can remain hidden in older Compose/XML screens.
It is intentionally informational until the de-hardcoding pass is complete.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KOTLIN_ROOT = ROOT / "app" / "src" / "main" / "java"
XML_ROOT = ROOT / "app" / "src" / "main" / "res"

PATTERNS = [
    ("Compose Text", re.compile(r'\bText\s*\(\s*(?:text\s*=\s*)?"([^"\\]{2,})"')),
    ("Compose Text template", re.compile(r'\bText\s*\(\s*(?:text\s*=\s*)?"([^"]*\$\{[^}]+\}[^"]*)"')),
    ("Icon/content description", re.compile(r'\bcontentDescription\s*=\s*"([^"\\]{2,})"')),
    ("Toast", re.compile(r'\b(?:android\.widget\.)?Toast\.makeText\s*\([^,]+,\s*"([^"\\]{2,})"')),
    ("Snackbar", re.compile(r'\bshowSnackbar\s*\(\s*"([^"\\]{2,})"')),
    ("Dialog title", re.compile(r'\btitle\s*=\s*"([^"\\]{2,})"')),
    ("Label", re.compile(r'\blabel\s*=\s*"([^"\\]{2,})"')),
]

IGNORE_PREFIXES = ("http://", "https://", "file://", "TAG", "UTF-8", "/help", "/reset", "BUY_")
SKIP_PATH_PARTS = ("/data/", "/domain/", "/test/", "/androidTest/")

def probable_user_text(value: str) -> bool:
    value = value.strip()
    if len(value) < 2 or value.startswith(IGNORE_PREFIXES):
        return False
    if re.fullmatch(r"[A-Za-z0-9_./:-]+", value) and (
        "_" in value or "/" in value or value.startswith(("http", "file"))
    ):
        return False
    return any(ch.isalpha() for ch in value)

def scan_kotlin():
    findings = []
    for path in KOTLIN_ROOT.rglob("*.kt"):
        rel = str(path.relative_to(ROOT)).replace("\\", "/")
        if any(part in rel for part in SKIP_PATH_PARTS):
            continue
        for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            for kind, pattern in PATTERNS:
                for match in pattern.finditer(line):
                    value = match.group(1).strip()
                    if probable_user_text(value):
                        findings.append((rel, line_no, kind, value))
    return findings

def scan_xml():
    findings = []
    for path in XML_ROOT.rglob("*.xml"):
        rel = str(path.relative_to(ROOT)).replace("\\", "/")
        for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            for attr in ("text", "contentDescription", "hint", "label"):
                match = re.search(rf'\b{attr}\s*=\s*"([^"]+)"', line)
                if match and probable_user_text(match.group(1)):
                    findings.append((rel, line_no, f"XML {attr}", match.group(1).strip()))
    return findings

def scan_catalog_calls():
    """Find English phrases that bypass or miss the canonical locale catalog."""
    locale_path = ROOT / "app" / "src" / "main" / "assets" / "locales" / "en_us.json"
    try:
        import json
        with locale_path.open("r", encoding="utf-8") as handle:
            catalog = json.load(handle)
    except Exception as exc:
        return [(str(locale_path.relative_to(ROOT)), 1, "Catalog", f"cannot load en_us.json: {exc}")]

    values = {value for value in catalog.values() if isinstance(value, str)}
    keys = set(catalog)
    findings = []
    for path in KOTLIN_ROOT.rglob("*.kt"):
        rel = str(path.relative_to(ROOT)).replace("\\\\", "/")
        if any(part in rel for part in SKIP_PATH_PARTS):
            continue
        for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            for match in re.finditer(r'\\blocalizedText\\s*\\(\\s*"([^"\\\\]*(?:\\\\.[^"\\\\]*)*)"', line):
                value = match.group(1).strip()
                if "$" in value:
                    findings.append((rel, line_no, "Dynamic localizedText", value))
                elif probable_user_text(value) and value not in values:
                    findings.append((rel, line_no, "Missing English catalog value", value))

            for match in re.finditer(r'\\b[A-Za-z_]\\w*\\.getString\\s*\\(\\s*"([^"]+)"', line):
                key = match.group(1).strip()
                # IDs composed from data-driven asset IDs are validated by the
                # asset-specific localization contract, not as literal keys here.
                if "$" in key:
                    continue
                if not re.fullmatch(r"[a-z0-9_]+", key):
                    findings.append((rel, line_no, "English phrase used as key", key))
                elif key not in keys:
                    findings.append((rel, line_no, "Missing locale key", key))
    return findings

def main() -> int:
    findings = scan_kotlin() + scan_xml() + scan_catalog_calls()
    print(f"Localization surface audit: {len(findings)} candidate(s), including missing catalog values and format-key issues.")
    for rel, line_no, kind, value in findings:
        print(f"  - {rel}:{line_no}: [{kind}] {value}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
