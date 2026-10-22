#!/usr/bin/env python3
"""Validate the embedded assets/info.txt metadata in an APK."""
from __future__ import annotations
import argparse
from datetime import datetime
import re
import sys
import zipfile
from pathlib import Path

KEYS = ("name", "env", "git_commit", "git_branch", "version_name", "version_code", "build_date")

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--env", choices=("debug", "release"), required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--version-code", type=int, required=True)
    args = parser.parse_args()
    try:
        with zipfile.ZipFile(args.apk) as apk:
            info = apk.read("assets/info.txt").decode("utf-8")
    except (OSError, KeyError, UnicodeError, zipfile.BadZipFile) as error:
        print(f"APK metadata validation failed: {error}", file=sys.stderr)
        return 1
    values = {}
    for line in info.splitlines():
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        values[key.strip()] = value.strip()
    missing = [key for key in KEYS if not values.get(key)]
    if missing:
        print("Missing metadata fields: " + ", ".join(missing), file=sys.stderr)
        return 1
    expected = {
        "env": args.env,
        "git_commit": args.commit[:8],
        "version_name": args.version_name,
        "version_code": str(args.version_code),
    }
    for key, value in expected.items():
        if values[key] != value:
            print(f"{key} mismatch: expected {value!r}, found {values[key]!r}", file=sys.stderr)
            return 1
    if not re.fullmatch(r"[A-Za-z0-9._/-]+", values["git_branch"]):
        print("git_branch contains unexpected characters", file=sys.stderr)
        return 1
    safe_branch = re.sub(r"[^A-Za-z0-9._-]", "-", values["git_branch"]).strip("-") or "unknown"
    expected_name_prefix = f"GuessTheNumber-v{args.version_name}-{args.commit[:8]}."
    if not values["name"].startswith(expected_name_prefix) or not values["name"].endswith("." + safe_branch):
        print("name must include the expected app version, commit and branch", file=sys.stderr)
        return 1
    try:
        datetime.strptime(values["build_date"], "%a, %d %b %Y %H:%M:%S %z")
    except ValueError:
        print("build_date must use 'Tue, 12 Aug 2025 15:16:35 +0800' format", file=sys.stderr)
        return 1
    print("APK assets/info.txt: OK")
    print(info, end="" if info.endswith("\n") else "\n")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
