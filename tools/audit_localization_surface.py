#!/usr/bin/env python3
"""Repository-wide localization surface audit.

This intentionally reports candidates rather than blindly failing on every string:
machine-readable IDs, schema keys, logs, tests, and developer internals are valid
hardcoded values. User-facing Compose literals are the high-confidence targets.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KOTLIN_ROOT = ROOT / "app" / "src" / "main" / "java"
PATTERNS = [
    re.compile(r'\bText\s*\(\s*"([^"\\]{2,})"'),
    re.compile(r'\bcontentDescription\s*=\s*"([^"\\]{2,})"'),
    re.compile(r'\bToast\.makeText\s*\([^,]+,\s*"([^"\\]{2,})"'),
    re.compile(r'\bSnackbarHostState.*?showSnackbar\s*\(\s*"([^"\\]{2,})"'),
]
IGNORE = ("http://", "https://", "file://", "TAG", "UTF-8")

def main() -> int:
    findings = []
    for path in KOTLIN_ROOT.rglob("*.kt"):
        rel = str(path.relative_to(ROOT)).replace("\\", "/")
        for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            for pattern in PATTERNS:
                for match in pattern.finditer(line):
                    value = match.group(1).strip()
                    if value and not value.startswith(IGNORE):
                        findings.append(f"{rel}:{line_no}: {value}")
    print(f"Localization surface audit: {len(findings)} high-confidence candidate(s).")
    for finding in findings:
        print(f"  - {finding}")
    # This audit is informational while the repository-wide de-hardcoding pass is
    # still being completed. The dedicated hardcoded-string validator remains the
    # enforcement gate once all candidates have been migrated.
    return 0

if __name__ == "__main__":
    sys.exit(main())
