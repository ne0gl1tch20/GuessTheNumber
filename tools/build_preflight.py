#!/usr/bin/env python3
"""Fast repository/toolchain preflight before Gradle work.

This is intentionally static: it catches configuration mismatches that can
otherwise waste a full CI run before Gradle has a chance to report them.
It does not replace Kotlin compilation or unit tests.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github" / "workflows" / "build-apks.yml"
APP_GRADLE = ROOT / "app" / "build.gradle.kts"
VERSIONS = ROOT / "gradle" / "libs.versions.toml"
WRAPPER = ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties"
LOCALE_DIR = ROOT / "app" / "src" / "main" / "assets" / "locales"
MAIN_SRC = ROOT / "app" / "src" / "main"
TEST_SRC = ROOT / "app" / "src" / "test"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def version_tuple(value: str) -> tuple[int, ...]:
    return tuple(int(part) for part in re.findall(r"\d+", value))


def extract(text: str, pattern: str, label: str, errors: list[str]) -> str | None:
    match = re.search(pattern, text, re.MULTILINE)
    if not match:
        errors.append(f"{label}: could not find expected setting")
        return None
    return match.group(1)


def check_required_files(errors: list[str]) -> None:
    required = [
        Path("gradlew"),
        Path("settings.gradle.kts"),
        Path("build.gradle.kts"),
        Path("gradle/libs.versions.toml"),
        Path("gradle/wrapper/gradle-wrapper.properties"),
        Path("app/build.gradle.kts"),
        Path(".github/workflows/build-apks.yml"),
        Path("tools/preflight_compile_check.py"),
    ]
    for relative in required:
        if not (ROOT / relative).is_file():
            errors.append(f"missing required file: {relative}")


def check_toolchain(errors: list[str], notes: list[str]) -> None:
    try:
        app = read(APP_GRADLE)
        versions = read(VERSIONS)
        wrapper = read(WRAPPER)
    except (OSError, UnicodeDecodeError) as exc:
        errors.append(f"unable to read build configuration: {exc}")
        return

    compile_sdk = extract(
        app, r"compileSdk\s*=\s*(\d+)", "app/build.gradle.kts compileSdk", errors
    )
    target_sdk = extract(
        app, r"targetSdk\s*=\s*(\d+)", "app/build.gradle.kts targetSdk", errors
    )
    min_sdk = extract(
        app, r"minSdk\s*=\s*(\d+)", "app/build.gradle.kts minSdk", errors
    )
    if compile_sdk and target_sdk and int(target_sdk) > int(compile_sdk):
        errors.append(f"targetSdk {target_sdk} is higher than compileSdk {compile_sdk}")
    if min_sdk and target_sdk and int(min_sdk) > int(target_sdk):
        errors.append(f"minSdk {min_sdk} is higher than targetSdk {target_sdk}")

    agp = extract(versions, r'^agp\s*=\s*"([^"]+)"', "AGP version", errors)
    kotlin = extract(versions, r'^kotlin\s*=\s*"([^"]+)"', "Kotlin version", errors)
    gradle = extract(
        wrapper,
        r"distributionUrl=.*gradle-([0-9.]+)-bin\.zip",
        "Gradle wrapper version",
        errors,
    )

    if agp and gradle:
        agp_major_minor = version_tuple(agp)[:2]
        min_gradle_by_agp = {
            (9, 4): (9, 6, 0),
            (9, 3): (9, 5, 0),
            (9, 2): (9, 4, 1),
            (9, 1): (9, 3, 1),
            (9, 0): (9, 1, 0),
            (8, 13): (8, 13, 0),
            (8, 12): (8, 13, 0),
            (8, 11): (8, 13, 0),
            (8, 10): (8, 11, 1),
            (8, 9): (8, 11, 1),
            (8, 8): (8, 10, 2),
            (8, 7): (8, 9, 0),
            (8, 6): (8, 7, 0),
            (8, 5): (8, 7, 0),
            (8, 4): (8, 6, 0),
            (8, 3): (8, 4, 0),
            (8, 2): (8, 2, 0),
            (8, 1): (8, 0, 0),
            (8, 0): (8, 0, 0),
        }
        minimum = min_gradle_by_agp.get(agp_major_minor)
        if minimum and version_tuple(gradle) < minimum:
            errors.append(
                f"AGP {agp} requires Gradle >= {'.'.join(map(str, minimum))}, "
                f"but wrapper is {gradle}"
            )
        elif minimum:
            notes.append(f"AGP {agp} / Gradle {gradle}: compatible")

    if agp and compile_sdk:
        agp_max_api = {
            (9, 4): 37,
            (9, 3): 37,
            (9, 2): 37,
            (9, 1): 36,
            (9, 0): 36,
            (8, 13): 36,
            (8, 12): 36,
            (8, 11): 36,
            (8, 10): 35,
            (8, 9): 35,
            (8, 8): 35,
            (8, 7): 35,
        }
        max_api = agp_max_api.get(version_tuple(agp)[:2])
        if max_api and int(compile_sdk) > max_api:
            errors.append(
                f"AGP {agp} supports up to API {max_api}, but compileSdk is {compile_sdk}"
            )

    robo_match = re.search(
        r'org\.robolectric:robolectric:([0-9.]+)', app
    )
    if robo_match and target_sdk:
        robo = robo_match.group(1)
        robo_max_api = {
            "4.17": 37,
            "4.16": 36,
            "4.15": 35,
            "4.14": 35,
            "4.13": 34,
            "4.12": 34,
            "4.11": 34,
        }
        max_api = robo_max_api.get(robo)
        if max_api and int(target_sdk) > max_api:
            errors.append(
                f"Robolectric {robo} supports Android API <= {max_api}, "
                f"but targetSdk is {target_sdk}; upgrade Robolectric or lower the test API"
            )
        elif max_api:
            notes.append(f"Robolectric {robo} covers target API {target_sdk}")

    if kotlin:
        notes.append(f"Kotlin Gradle plugin: {kotlin}")


def check_ci_contract(errors: list[str]) -> None:
    try:
        workflow = read(WORKFLOW)
    except (OSError, UnicodeDecodeError) as exc:
        errors.append(f"unable to read CI workflow: {exc}")
        return

    setup = workflow.find("- name: Setup Gradle")
    preflight = workflow.find("- name: Build Preflight")
    if setup < 0:
        errors.append("CI workflow is missing the 'Setup Gradle' step")
        return
    if preflight < 0 or preflight > setup:
        errors.append(
            "CI workflow must run 'Build Preflight' immediately before 'Setup Gradle'"
        )
        return

    between = workflow[preflight:setup]
    step_headers = re.findall(r"^      - name:", between, re.MULTILINE)
    if len(step_headers) != 1:
        errors.append(
            "'Build Preflight' is not immediately before 'Setup Gradle' "
            "(another CI step exists between them)"
        )


def check_python_tools(errors: list[str], notes: list[str]) -> None:
    tools = sorted((ROOT / "tools").glob("*.py")) if (ROOT / "tools").exists() else []
    for path in tools:
        try:
            compile(read(path), str(path), "exec")
        except SyntaxError as exc:
            errors.append(
                f"{path.relative_to(ROOT)}:{exc.lineno}: Python syntax error: {exc.msg}"
            )
    notes.append(f"Python tool syntax: {len(tools)} file(s) checked")


def check_json_assets(errors: list[str], notes: list[str]) -> None:
    if not LOCALE_DIR.is_dir():
        errors.append(f"missing locale asset directory: {LOCALE_DIR.relative_to(ROOT)}")
        return

    files = sorted(LOCALE_DIR.rglob("*.json"))
    for path in files:
        try:
            json.loads(read(path))
        except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
            errors.append(f"{path.relative_to(ROOT)}: invalid JSON: {exc}")
    notes.append(f"Locale JSON syntax: {len(files)} file(s) checked")


def check_kotlin_sources(errors: list[str], notes: list[str]) -> None:
    files = sorted(MAIN_SRC.rglob("*.kt")) if MAIN_SRC.exists() else []
    errors_found = 0

    for path in files:
        try:
            text = read(path)
        except (OSError, UnicodeDecodeError) as exc:
            errors.append(f"{path.relative_to(ROOT)}: unreadable Kotlin source: {exc}")
            continue

        pairs = {"(": ")", "[": "]", "{": "}"}
        stack: list[tuple[str, int]] = []
        line = 1
        i = 0
        in_line = in_block = in_string = in_char = False
        escaped = False

        while i < len(text):
            ch = text[i]
            nxt = text[i + 1] if i + 1 < len(text) else ""

            if ch == "\n":
                line += 1
                in_line = False
                i += 1
                continue

            if in_line:
                i += 1
                continue

            if in_block:
                if ch == "*" and nxt == "/":
                    in_block = False
                    i += 2
                else:
                    i += 1
                continue

            if not in_string and not in_char:
                if ch == "/" and nxt == "/":
                    in_line = True
                    i += 2
                    continue
                if ch == "/" and nxt == "*":
                    in_block = True
                    i += 2
                    continue

            if in_string:
                if text[i:i + 3] == '"""':
                    in_string = False
                    i += 3
                    continue
                if ch == "\\" and not escaped:
                    escaped = True
                else:
                    if ch == '"' and not escaped:
                        in_string = False
                    escaped = False
                i += 1
                continue

            if in_char:
                if ch == "\\" and not escaped:
                    escaped = True
                else:
                    if ch == "'" and not escaped:
                        in_char = False
                    escaped = False
                i += 1
                continue

            if text[i:i + 3] == '"""':
                in_string = True
                i += 3
                continue
            if ch == '"':
                in_string = True
                i += 1
                continue
            if ch == "'":
                in_char = True
                i += 1
                continue

            if ch in pairs:
                stack.append((ch, line))
            elif ch in pairs.values():
                if not stack or pairs[stack[-1][0]] != ch:
                    errors.append(
                        f"{path.relative_to(ROOT)}:{line}: unmatched '{ch}'"
                    )
                else:
                    stack.pop()
            i += 1

        if in_string:
            errors.append(f"{path.relative_to(ROOT)}:{line}: unterminated string")
        if in_char:
            errors.append(f"{path.relative_to(ROOT)}:{line}: unterminated character")
        if in_block:
            errors.append(f"{path.relative_to(ROOT)}:{line}: unterminated block comment")
        for opening, opening_line in stack:
            errors.append(
                f"{path.relative_to(ROOT)}:{opening_line}: unclosed '{opening}'"
            )

        imports: set[str] = set()
        for line_number, source_line in enumerate(text.splitlines(), 1):
            stripped = source_line.strip()
            if stripped.startswith("import "):
                imported = stripped[7:].strip()
                if imported in imports:
                    errors.append(
                        f"{path.relative_to(ROOT)}:{line_number}: duplicate import '{imported}'"
                    )
                imports.add(imported)

    notes.append(f"Kotlin structure: {len(files)} main source file(s) checked")


def main() -> int:
    errors: list[str] = []
    notes: list[str] = []

    check_required_files(errors)
    check_toolchain(errors, notes)
    check_ci_contract(errors)
    check_python_tools(errors, notes)
    check_json_assets(errors, notes)
    check_kotlin_sources(errors, notes)

    print("=== GuessTheNumber Build Preflight ===")
    for note in notes:
        print(f"PASS  {note}")

    if errors:
        print(f"FAIL  {len(errors)} preflight finding(s)")
        for error in errors:
            print(f"  - {error}")
        print("Gradle setup/build is intentionally blocked until preflight passes.")
        return 1

    print("PASS  Build preflight complete — safe to continue to Gradle.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
