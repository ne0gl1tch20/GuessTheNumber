#!/usr/bin/env python3
"""Generate a SHA-256 manifest for repository-owned Android web-play assets."""
import argparse
import hashlib
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
INCLUDED_DIRS = ("web-play", "fonts", "locales")
REQUIRED = {"web-play/index.html", "web-play/app.js", "web-play/styles.css"}


def build_manifest(version: str) -> dict:
    files = []
    for folder in INCLUDED_DIRS:
        base = ASSETS / folder
        for path in sorted(base.rglob("*")):
            if not path.is_file():
                continue
            relative = path.relative_to(ASSETS).as_posix()
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            files.append({"path": relative, "sha256": digest})
    paths = {entry["path"] for entry in files}
    missing = REQUIRED - paths
    if missing:
        raise ValueError(f"Missing required web-play assets: {sorted(missing)}")
    return {
        "schemaVersion": 1,
        "version": version,
        "minAppVersionCode": 21,
        "files": files,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="web-play-manifest.json")
    parser.add_argument("--version", default=None)
    args = parser.parse_args()
    version = args.version or subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
    ).strip()
    manifest = build_manifest(version)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(manifest['files'])} assets for {version} to {output}")


if __name__ == "__main__":
    main()
