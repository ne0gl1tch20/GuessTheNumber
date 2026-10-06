#!/usr/bin/env python3
"""Fast Kotlin preflight checks before Gradle compilation.

This is intentionally lightweight: it catches common source mistakes quickly
without starting the Kotlin/Gradle compiler.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app" / "src" / "main"

# Common Compose symbols that frequently fail when their import is missing.
SYMBOL_IMPORTS = {
    "remember": "androidx.compose.runtime.remember",
    "mutableStateOf": "androidx.compose.runtime.mutableStateOf",
    "LaunchedEffect": "androidx.compose.runtime.LaunchedEffect",
    "DisposableEffect": "androidx.compose.runtime.DisposableEffect",
    "Check": "androidx.compose.material.icons.filled.Check",
    "consumePositionChange": "androidx.compose.ui.input.pointer.consumePositionChange",
    "LocalContext": "androidx.compose.ui.platform.LocalContext",
}

def check_balanced(path: Path, text: str, errors: list[str]) -> None:
    pairs = {"(": ")", "[": "]", "{": "}"}
    closing = set(pairs.values())
    stack: list[tuple[str, int]] = []
    in_string = False
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

        if not in_string and ch == "/" and nxt == "/":
            in_line_comment = True
            i += 2
            continue

        if not in_string and ch == "/" and nxt == "*":
            in_block_comment = True
            i += 2
            continue

        if ch == '"' and not escaped:
            in_string = not in_string

        if not in_string:
            if ch in pairs:
                stack.append((ch, line))
            elif ch in closing:
                if not stack or pairs[stack[-1][0]] != ch:
                    errors.append(f"{path.relative_to(ROOT)}:{line}: unmatched '{ch}'")
                else:
                    stack.pop()

        escaped = (ch == "\\") and not escaped
        if ch != "\\":
            escaped = False
        i += 1

    if in_string:
        errors.append(f"{path.relative_to(ROOT)}:{line}: unterminated string literal")
    if in_block_comment:
        errors.append(f"{path.relative_to(ROOT)}:{line}: unterminated block comment")
    for opening, opening_line in stack:
        errors.append(f"{path.relative_to(ROOT)}:{opening_line}: unclosed '{opening}'")

def check_imports(path: Path, text: str, errors: list[str]) -> None:
    imports = re.findall(r"^\s*import\s+([A-Za-z_][\w.]*)", text, re.MULTILINE)
    if len(imports) != len(set(imports)):
        seen: set[str] = set()
        for item in imports:
            if item in seen:
                errors.append(f"{path.relative_to(ROOT)}: duplicate import '{item}'")
            seen.add(item)

    for symbol, fqcn in SYMBOL_IMPORTS.items():
        if not re.search(rf"\b{re.escape(symbol)}\b", text):
            continue
        if re.search(rf"\bimport\s+{re.escape(fqcn)}\s*$", text, re.MULTILINE):
            continue
        # Don't flag declarations/qualified calls where an import is unnecessary.
        if re.search(rf"\b(?:fun|class|object|interface|typealias|val|var)\s+{re.escape(symbol)}\b", text):
            continue
        if f".{symbol}" in text and symbol not in {"remember", "Check"}:
            continue
        errors.append(
            f"{path.relative_to(ROOT)}: possible missing import for '{symbol}' "
            f"(expected {fqcn})"
        )

def main() -> int:
    kotlin_files = sorted(SRC.rglob("*.kt"))
    errors: list[str] = []

    for path in kotlin_files:
        text = path.read_text(encoding="utf-8")
        check_balanced(path, text, errors)
        check_imports(path, text, errors)

    print(f"Kotlin preflight: scanned {len(kotlin_files)} file(s).")
    if errors:
        print(f"Kotlin preflight: FAILED with {len(errors)} finding(s).")
        for error in errors:
            print(f"  - {error}")
        return 1

    print("Kotlin preflight: PASS — no quick-fail source issues found.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
