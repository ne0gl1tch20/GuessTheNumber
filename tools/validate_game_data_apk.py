#!/usr/bin/env python3
"""Validate the encrypted game-data payload embedded in an Android APK."""
import argparse
import hashlib
import json
import zipfile
from io import BytesIO
from pathlib import Path

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from binarify_game_data import FORMAT_VERSION, KEY, MAGIC


def validate(apk_path: Path) -> tuple[int, int]:
    with zipfile.ZipFile(apk_path) as apk:
        asset_files = {name for name in apk.namelist() if name.startswith("assets/") and not name.endswith("/")}
        approved = {
            "assets/info.txt",
        }
        unexpected = {
            name for name in asset_files
            if not name.startswith("assets/game_data/") and name not in approved
        }
        assert not unexpected, (
            "Unexpected APK assets (plaintext or unapproved bootstrap files): "
            + ", ".join(sorted(unexpected))
        )
        manifest_path = "assets/game_data/manifest.json"
        manifest = json.loads(apk.read(manifest_path))
        assert manifest["schemaVersion"] == 1
        assert manifest["archiveFormatVersion"] == FORMAT_VERSION
        assert manifest["minAppVersionCode"] == 24
        assert manifest["cipher"] == "AES-256-GCM"
        assert manifest["compression"] == "ZIP-DEFLATE"

        extracted = {}
        chunks = manifest["chunks"]
        assert 1 <= len(chunks) <= 512
        for chunk in chunks:
            name = chunk["file"]
            assert name == f'{chunk["id"]}.bin'
            blob = apk.read(f"assets/game_data/{name}")
            assert len(blob) == chunk["size"]
            assert hashlib.sha256(blob).hexdigest() == chunk["sha256"]
            assert blob[:8] == MAGIC and blob[8] == FORMAT_VERSION
            nonce = blob[9:21]
            aad = f'{FORMAT_VERSION}:{chunk["group"]}:{chunk["id"]}'.encode()
            compressed = AESGCM(KEY).decrypt(nonce, blob[21:], aad)
            with zipfile.ZipFile(BytesIO(compressed)) as archive:
                for path in archive.namelist():
                    assert path not in extracted, f"Duplicate archived path: {path}"
                    extracted[path] = archive.read(path)

        expected = {entry["path"]: entry for entry in manifest["files"]}
        assert set(extracted) == set(expected), (
            f"APK archive coverage mismatch: missing={set(expected)-set(extracted)}, "
            f"extra={set(extracted)-set(expected)}"
        )
        for path, data in extracted.items():
            entry = expected[path]
            assert len(data) == entry["size"], f"Size mismatch: {path}"
            assert hashlib.sha256(data).hexdigest() == entry["sha256"], f"Hash mismatch: {path}"
        return len(chunks), len(extracted)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    chunks, files = validate(args.apk)
    print(f"Validated APK game data: {chunks} encrypted chunks, {files} source files")


if __name__ == "__main__":
    main()
