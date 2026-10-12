#!/usr/bin/env python3
"""Dependency-free smoke tests that run immediately after checkout.

These checks are deliberately cheap and static. They catch broken repository
contracts before JDK setup, Gradle setup, dependency resolution, or unit tests.
They do not replace Android resource processing or Kotlin compilation.
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github" / "workflows" / "build-apks.yml"
WRAPPER_PROPERTIES = ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties"


def check_required_paths(errors: list[str]) -> None:
    required = [
        "gradlew",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper/gradle-wrapper.properties",
        "settings.gradle.kts",
        "build.gradle.kts",
        "app/build.gradle.kts",
        "app/src/main/AndroidManifest.xml",
        ".github/workflows/build-apks.yml",
        "tools/build_preflight.py",
        "tools/preflight_compile_check.py",
    ]
    for item in required:
        if not (ROOT / item).is_file():
            errors.append(f"required path missing or not a file: {item}")


def check_wrapper_contract(errors: list[str]) -> None:
    try:
        properties = WRAPPER_PROPERTIES.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError) as exc:
        errors.append(f"cannot read Gradle wrapper properties: {exc}")
        return

    urls = re.findall(r"^distributionUrl=(.+)$", properties, re.MULTILINE)
    if len(urls) != 1:
        errors.append("Gradle wrapper must declare exactly one distributionUrl")
    else:
        parsed = urlparse(urls[0].strip().replace("\\:", ":"))
        if parsed.scheme != "https" or not parsed.netloc:
            errors.append("Gradle distributionUrl must use HTTPS")

    if "distributionSha256Sum=" not in properties:
        print("NOTE  Gradle wrapper has no distributionSha256Sum pin")


def check_xml_well_formed(errors: list[str], notes: list[str]) -> None:
    roots = [ROOT / "app" / "src" / "main"]
    files: list[Path] = []
    for root in roots:
        if root.exists():
            files.extend(root.rglob("*.xml"))

    for path in sorted(files):
        try:
            ET.parse(path)
        except (ET.ParseError, OSError, UnicodeDecodeError) as exc:
            errors.append(f"{path.relative_to(ROOT)}: malformed or unreadable XML: {exc}")

    notes.append(f"Android XML syntax: {len(files)} file(s) parsed")


def check_workflow_python_commands(errors: list[str], notes: list[str]) -> None:
    try:
        workflow = WORKFLOW.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError) as exc:
        errors.append(f"cannot read workflow: {exc}")
        return

    commands = re.findall(r"python3\s+([\w./-]+\.py)", workflow)
    if not commands:
        errors.append("workflow contains no explicit python3 script commands")
        return

    missing = sorted({command for command in commands if not (ROOT / command).is_file()})
    for command in missing:
        errors.append(f"workflow references a missing Python script: {command}")

    notes.append(f"Workflow Python entry points: {len(set(commands))} referenced")


def main() -> int:
    errors: list[str] = []
    notes: list[str] = []

    check_required_paths(errors)
    check_wrapper_contract(errors)
    check_xml_well_formed(errors, notes)
    check_workflow_python_commands(errors, notes)

    print("=== Immediate CI Smoke Tests ===")
    for note in notes:
        print(f"PASS  {note}")

    if errors:
        print(f"FAIL  {len(errors)} smoke-test finding(s)")
        for error in errors:
            print(f"  - {error}")
        print("Gradle and unit tests are blocked until these checks pass.")
        return 1

    print("PASS  Immediate smoke tests complete.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
