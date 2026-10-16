#!/usr/bin/env python3
"""Keep Windows' base Compose theme aligned with Android's source-of-truth theme."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_COLOR = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber/ui/theme/Color.kt"
ANDROID_THEME = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber/ui/theme/Theme.kt"
WINDOWS_THEME = ROOT / "desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopTheme.kt"


def fail(message: str) -> None:
    print(f"Android/Windows theme parity FAILED: {message}")
    raise SystemExit(1)


def block_after(source: str, marker: str, opening: str = "(") -> str:
    start = source.find(marker)
    if start < 0:
        fail(f"could not find {marker!r}")
    start = source.find(opening, start)
    if start < 0:
        fail(f"could not find opening {opening!r} after {marker!r}")
    depth = 0
    for index in range(start, len(source)):
        if source[index] == opening:
            depth += 1
        elif source[index] == ")":
            depth -= 1
            if depth == 0:
                return source[start + 1:index]
    fail(f"unclosed block after {marker!r}")


def arguments(block: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in block.splitlines():
        match = re.match(r"\s*(\w+)\s*=\s*(.+?)\s*,?\s*$", line)
        if match:
            result[match.group(1)] = match.group(2).rstrip(",")
    return result


def android_colors() -> dict[str, str]:
    source = ANDROID_COLOR.read_text(encoding="utf-8")
    return dict(re.findall(r"\bval\s+(\w+)\s*=\s*Color\((0x[0-9A-Fa-f]+)\)", source))


def resolve_color(expression: str, constants: dict[str, str]) -> str:
    expression = expression.strip()
    direct = re.search(r"Color\((0x[0-9A-Fa-f]+)\)", expression)
    if direct:
        return direct.group(1).upper()
    if expression in constants:
        return constants[expression].upper()
    fail(f"cannot resolve Android color expression {expression!r}")


def parse_scheme(source: str, name: str, constants: dict[str, str]) -> dict[str, str]:
    block = block_after(source, name)
    values = arguments(block)
    return {key: resolve_color(value, constants) for key, value in values.items()}


def main() -> None:
    for path in (ANDROID_COLOR, ANDROID_THEME, WINDOWS_THEME):
        if not path.is_file():
            fail(f"required theme source is missing: {path.relative_to(ROOT)}")

    color_constants = android_colors()
    android_source = ANDROID_THEME.read_text(encoding="utf-8")
    windows_source = WINDOWS_THEME.read_text(encoding="utf-8")

    android_schemes = {
        "dark": parse_scheme(android_source, "ExpressiveDarkColorScheme = darkColorScheme", color_constants),
        "light": parse_scheme(android_source, "ExpressiveLightColorScheme = lightColorScheme", color_constants),
    }
    windows_schemes = {
        "dark": parse_scheme(windows_source, "val dark = darkColorScheme", color_constants),
        "light": parse_scheme(windows_source, "val light = lightColorScheme", color_constants),
    }

    errors: list[str] = []
    for mode in ("dark", "light"):
        expected, actual = android_schemes[mode], windows_schemes[mode]
        for token in sorted(set(expected) | set(actual)):
            if token not in expected:
                errors.append(f"{mode}.{token}: Windows defines a base token absent from Android's base scheme")
            elif token not in actual:
                errors.append(f"{mode}.{token}: Windows is missing Android's base token")
            elif expected[token] != actual[token]:
                errors.append(f"{mode}.{token}: Android={expected[token]}, Windows={actual[token]}")

    android_shapes = block_after(android_source, "val ExpressiveShapes = Shapes")
    windows_shapes = block_after(windows_source, "val shapes = androidx.compose.material3.Shapes")
    shape_pattern = r"(extraSmall|small|medium|large|extraLarge)\s*=\s*.*?RoundedCornerShape\((\d+)\.dp\)"
    expected_shapes = dict(re.findall(shape_pattern, android_shapes))
    actual_shapes = dict(re.findall(shape_pattern, windows_shapes))
    if expected_shapes != actual_shapes:
        errors.append(f"corner sizes differ: Android={expected_shapes}, Windows={actual_shapes}")

    typography_pattern = r"(displayLarge|headlineLarge|titleLarge)\s*=\s*\1\.copy\(letterSpacing\s*=\s*\(?(-?[\d.]+)\)?\.sp\)"
    expected_typography = dict(re.findall(typography_pattern, block_after(android_source, "val ExpressiveTypography = Typography().run")))
    actual_typography = dict(re.findall(typography_pattern, block_after(windows_source, "val typography = androidx.compose.material3.Typography().run")))
    if expected_typography != actual_typography:
        errors.append(f"typography letter spacing differs: Android={expected_typography}, Windows={actual_typography}")

    print("Reminder: Android is the source of truth. Keep Windows' compatible theme tokens matched to Android.")
    if errors:
        for error in errors:
            print(f"  - {error}")
        raise SystemExit(1)
    print("Android/Windows theme parity PASSED: base light/dark colors, shapes, and typography match.")


if __name__ == "__main__":
    main()
