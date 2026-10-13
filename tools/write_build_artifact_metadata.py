#!/usr/bin/env python3
"""Write traceable metadata and a SHA-256 sidecar for a built APK.

This script does not build, sign, or modify the APK. It records the exact source
revision and build inputs alongside the artifact so downloaded APKs can be
identified later without relying on artifact display names.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
APP_GRADLE = ROOT / "app" / "build.gradle.kts"
WRAPPER_PROPERTIES = ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties"


def read_version() -> tuple[str, int]:
    text = APP_GRADLE.read_text(encoding="utf-8")
    name = re.search(r'(?m)^\s*versionName\s*=\s*"([^"]+)"', text)
    code = re.search(r"(?m)^\s*versionCode\s*=\s*(\d+)", text)
    if not name or not code:
        raise ValueError("Could not read versionName/versionCode from app/build.gradle.kts")
    return name.group(1), int(code.group(1))


def read_gradle_version() -> tuple[str, str]:
    text = WRAPPER_PROPERTIES.read_text(encoding="utf-8")
    url = re.search(r"(?m)^distributionUrl=(.+)$", text)
    checksum = re.search(r"(?m)^distributionSha256Sum=([0-9a-fA-F]{64})$", text)
    if not url:
        raise ValueError("Gradle wrapper distributionUrl is missing")
    decoded_url = unquote(url.group(1).strip().replace(r"\:", ":"))
    version_match = re.search(r"/gradle-([^/]+)-bin\.zip$", decoded_url)
    if not version_match:
        raise ValueError("Could not infer Gradle version from distributionUrl")
    if not checksum:
        raise ValueError("Gradle wrapper distribution SHA-256 pin is missing or malformed")
    return version_match.group(1), checksum.group(1).lower()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--channel", choices=("debug", "release"), required=True)
    parser.add_argument("--apk", type=Path, required=True)
    args = parser.parse_args()

    apk = args.apk
    if not apk.is_file():
        parser.error(f"APK does not exist: {apk}")

    version_name, version_code = read_version()
    gradle_version, gradle_checksum = read_gradle_version()
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    metadata = {
        "schemaVersion": 1,
        "artifact": apk.name,
        "artifactSha256": digest,
        "channel": args.channel,
        "commit": os.environ.get("GITHUB_SHA", "unknown"),
        "ref": os.environ.get("GITHUB_REF", "unknown"),
        "runId": os.environ.get("GITHUB_RUN_ID", "unknown"),
        "runAttempt": os.environ.get("GITHUB_RUN_ATTEMPT", "unknown"),
        "gradleVersion": gradle_version,
        "gradleDistributionSha256": gradle_checksum,
        "javaVersion": os.environ.get("JAVA_VERSION", "17"),
        "versionName": version_name,
        "versionCode": version_code,
    }

    metadata_path = apk.with_name(f"{apk.stem}.build-metadata.json")
    checksum_path = apk.with_name(f"{apk.name}.sha256")
    metadata_path.write_text(
        json.dumps(metadata, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    checksum_path.write_text(f"{digest}  {apk.name}\n", encoding="utf-8")
    print(f"Wrote {metadata_path.relative_to(ROOT)}")
    print(f"Wrote {checksum_path.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
