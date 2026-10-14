#!/usr/bin/env python3
"""Validate app version metadata."""
from __future__ import annotations
import re, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
text=(ROOT/"app"/"build.gradle.kts").read_text(encoding="utf-8")
name=re.search(r'\bversionName\s*=\s*"([^"]+)"',text)
code=re.search(r"\bversionCode\s*=\s*(\d+)",text)
if not name or not code:
    print("Version validation FAILED: missing versionName/versionCode")
    raise SystemExit(1)
version_name=name.group(1); version_code=int(code.group(1))
if version_code < 1 or not re.fullmatch(r"\d+\.\d+(?:\.\d+)?(?:[-+][0-9A-Za-z.-]+)?",version_name):
    print(f"Version validation FAILED: {version_code=} {version_name=}")
    raise SystemExit(1)
print(f"Version validation PASSED: versionCode={version_code}, versionName={version_name}")
