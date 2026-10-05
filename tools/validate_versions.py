#!/usr/bin/env python3
"""Validate app version metadata and obvious stale version literals."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GRADLE = ROOT / "app" / "build.gradle.kts"
KOTLIN_ROOT = ROOT / "app" / "src" / "main" / "java"

VERSION_NAME_RE = re.compile(r'\bversionName\s*=\s*"([^"]+)"')
VERSION_CODE_RE = re.compile(r"\bversionCode\s*=\s*(\d+)")
VERSION_LITERAL_RE = re.compile(r'(?<![\d.])v?(\d+\.\d+)(?![\d.])')

def main() -> int:
    errors: list[str] = []
    warnings: list[str] = []

    text = GRADLE.read_text(encoding="utf-8")
    name_match = VERSION_NAME_RE.search(text)
    code_match = VERSION_CODE_RE.search(text)

    if not name_match:
        errors.append("app/build.gradle.kts: versionName not found")
    if not code_match:
        errors.append("app/build.gradle.kts: versionCode not found")
    if errors:
        print("Version validation FAILED:")
        for error in errors:
            print(f"  - {error}")
        return 1

    version_name = name_match.group(1)
    version_code = int(code_match.group(1))
    if version_code < 1:
        errors.append(f"versionCode must be positive, got {version_code}")
    if not re.fullmatch(r"\d+\.\d+(?:\.\d+)?(?:[-+][0-9A-Za-z.-]+)?", version_name):
        errors.append(f"versionName has unexpected format: {version_name!r}")

    for path in KOTLIN_ROOT.rglob("*.kt"):
        source = path.read_text(encoding="utf-8")
        for match in VERSION_LITERAL_RE.finditer(source):
            literal = match.group(1)
            line = source.count("\n", 0, match.start()) + 1
            warnings.append(f"{path.relative_to(ROOT)}:{line}: version-like literal {literal}")

    print(f"Version metadata: versionCode={version_code}, versionName={version_name}")
    if warnings:
        print("Version audit warnings:")
        for warning in warnings:
            print(f"  - {warning}")
    else:
        print("Version audit PASSED: no Kotlin version literals found.")
    return 1 if errors else 0

if __name__ == "__main__":
    sys.exit(main())
