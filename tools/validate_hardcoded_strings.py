#!/usr/bin/env python3
"""Audit likely hardcoded user-visible Kotlin UI strings across all platforms."""
from __future__ import annotations
import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC_ROOTS = [
    ROOT / "app" / "src" / "main" / "java",
    ROOT / "shared" / "src",
    ROOT / "desktopApp" / "src",
]
PATTERNS = [
    re.compile(r'\bText\s*\(\s*"([^"\\]{2,})"'),
    re.compile(r'\bToast\.makeText\s*\([^,]+,\s*"([^"\\]{2,})"'),
    re.compile(r'\bcontentDescription\s*=\s*"([^"\\]{2,})"'),
]


def main() -> int:
    # Windows PowerShell runners may default to cp1252; audit findings can
    # contain Unicode punctuation that the active console encoding cannot print.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")

    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()
    findings = []
    files = sorted({path for root in SRC_ROOTS if root.exists() for path in root.rglob("*.kt")})
    for path in files:
        rel = str(path.relative_to(ROOT)).replace("\\", "/")
        for n, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            for pattern in PATTERNS:
                for match in pattern.finditer(line):
                    value = match.group(1).strip()
                    static_text = re.sub(r"\$\{[^}]+\}|\$[A-Za-z_]\w*", "", value)
                    if not re.search(r"[A-Za-z]{2,}|[^\W\d_]{2,}", static_text, re.UNICODE):
                        continue
                    if value and value != "•" and not value.startswith(("http://", "https://", "file://")):
                        findings.append(f"{rel}:{n}: {value}")
    print(f"Hardcoded-string audit: scanned {len(files)} Kotlin file(s), found {len(findings)} candidate(s).")
    for finding in findings:
        print(f"  - {finding}")
    if args.strict and findings:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
