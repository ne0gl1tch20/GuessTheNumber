#!/usr/bin/env python3
"""Fast, conservative Kotlin source preflight before Gradle compilation.

This checker deliberately only reports syntax-like source problems that can be
reliably detected without Kotlin's compiler/type resolver. It must not try to
reimplement Kotlin import resolution.
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app" / "src" / "main"


def check_structure(path: Path, text: str, errors: list[str]) -> None:
    """Check delimiters, strings, and comments while ignoring comments/strings."""
    pairs = {"(": ")", "[": "]", "{": "}"}
    closing = set(pairs.values())
    stack: list[tuple[str, int]] = []
    in_string = False
    in_char = False
    escaped = False
    in_line_comment = False
    in_block_comment = False
    i = 0
    line = 1

    while i < len(text):
        ch = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""

        if ch == "\n":
            line += 1
            in_line_comment = False
            escaped = False
            i += 1
            continue

        if in_line_comment:
            i += 1
            continue

        if in_block_comment:
            if ch == "*" and nxt == "/":
                in_block_comment = False
                i += 2
            else:
                i += 1
            continue

        if not in_string and not in_char:
            if ch == "/" and nxt == "/":
                in_line_comment = True
                i += 2
                continue
            if ch == "/" and nxt == "*":
                in_block_comment = True
                i += 2
                continue

        if in_string:
            if ch == '"' and not escaped:
                # Kotlin raw strings use triple quotes; handle their closing separately.
                if text[i:i + 3] == '"""':
                    in_string = False
                    i += 3
                    continue
                in_string = False
            escaped = ch == "\\" and not escaped
            if ch != "\\":
                escaped = False
            i += 1
            continue

        if in_char:
            if ch == "'" and not escaped:
                in_char = False
            escaped = ch == "\\" and not escaped
            if ch != "\\":
                escaped = False
            i += 1
            continue

        if text[i:i + 3] == '"""':
            in_string = True
            i += 3
            continue
        if ch == '"':
            in_string = True
            escaped = False
            i += 1
            continue
        if ch == "'":
            in_char = True
            escaped = False
            i += 1
            continue

        if ch in pairs:
            stack.append((ch, line))
        elif ch in closing:
            if not stack or pairs[stack[-1][0]] != ch:
                errors.append(f"{path.relative_to(ROOT)}:{line}: unmatched '{ch}'")
            else:
                stack.pop()

        i += 1

    if in_string:
        errors.append(f"{path.relative_to(ROOT)}:{line}: unterminated string literal")
    if in_char:
        errors.append(f"{path.relative_to(ROOT)}:{line}: unterminated character literal")
    if in_block_comment:
        errors.append(f"{path.relative_to(ROOT)}:{line}: unterminated block comment")

    for opening, opening_line in stack:
        errors.append(f"{path.relative_to(ROOT)}:{opening_line}: unclosed '{opening}'")


def check_duplicate_imports(path: Path, text: str, errors: list[str]) -> None:
    """Catch exact duplicate imports without attempting semantic import checks."""
    seen: set[str] = set()
    for line_number, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        if not stripped.startswith("import "):
            continue
        imported = stripped[7:].strip()
        if imported in seen:
            errors.append(f"{path.relative_to(ROOT)}:{line_number}: duplicate import '{imported}'")
        seen.add(imported)


def main() -> int:
    if not SRC.exists():
        print(f"Kotlin preflight: source directory not found: {SRC}")
        return 1

    kotlin_files = sorted(SRC.rglob("*.kt"))
    errors: list[str] = []

    for path in kotlin_files:
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError as exc:
            errors.append(f"{path.relative_to(ROOT)}: invalid UTF-8: {exc}")
            continue

        check_structure(path, text, errors)
        check_duplicate_imports(path, text, errors)

    print(f"Kotlin preflight: scanned {len(kotlin_files)} file(s).")
    if errors:
        print(f"Kotlin preflight: FAILED with {len(errors)} finding(s).")
        for error in errors:
            print(f"  - {error}")
        return 1

    print("Kotlin preflight: PASS — no conservative source-structure issues found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
