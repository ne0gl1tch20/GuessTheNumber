#!/usr/bin/env python3
"""Build independently encrypted GuessTheNumber asset chunks.

The APK/runtime format is intentionally versioned. Each chunk is a ZIP payload
compressed before AES-256-GCM encryption. The manifest preserves original paths.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
import zipfile
from pathlib import Path
from typing import Iterable

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
OUTPUT = ROOT / "app" / "build" / "generated" / "gameDataAssets" / "game_data"
MAGIC = b"GTNGBIN1"
FORMAT_VERSION = 1
KEY_ID = "gtn-assets-v1"
# This key provides at-rest obfuscation only while embedded in an APK.
# A determined reverse engineer can recover client-side key material.
KEY = hashlib.sha256(b"GuessTheNumber|game-data|archive-v1|2026").digest()
GROUPS = (
    "game", "locales", "web-play", "web-app", "web-prototype",
    "web-ui", "libraries", "fonts", "scripts", "traits", "changelogs",
)
MAX_CHUNK_BYTES = 4 * 1024 * 1024


def safe_files(folder: Path) -> list[Path]:
    if not folder.exists():
        return []
    result = []
    for path in sorted(folder.rglob("*")):
        if path.is_symlink():
            raise ValueError(f"Symlinks are not allowed in game data: {path}")
        if path.is_file():
            relative = path.relative_to(ASSETS).as_posix()
            if relative.startswith("/") or ".." in Path(relative).parts:
                raise ValueError(f"Unsafe asset path: {relative}")
            result.append(path)
    return result


def grouped_files() -> dict[str, list[Path]]:
    groups: dict[str, list[Path]] = {}
    for name in GROUPS:
        files = safe_files(ASSETS / name)
        if files:
            groups[name] = files
    root_files = [path for path in sorted(ASSETS.iterdir()) if path.is_file()]
    if root_files:
        groups["core"] = root_files
    if not groups:
        raise ValueError(f"No game data found under {ASSETS}")
    return groups


def make_zip(files: Iterable[Path]) -> bytes:
    from io import BytesIO
    output = BytesIO()
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in files:
            info = zipfile.ZipInfo(path.relative_to(ASSETS).as_posix(), date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = 0o100600 << 16
            archive.writestr(info, path.read_bytes(), compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    return output.getvalue()


def split_group(name: str, files: list[Path], max_bytes: int) -> list[list[Path]]:
    # Split by estimated uncompressed bytes so a single subsystem can be fetched
    # in small independently verifiable units.
    chunks: list[list[Path]] = []
    current: list[Path] = []
    current_size = 0
    for path in files:
        size = path.stat().st_size
        if size > max_bytes:
            raise ValueError(f"Asset exceeds configured chunk size ({size} > {max_bytes}): {path}")
        if current and current_size + size > max_bytes:
            chunks.append(current)
            current, current_size = [], 0
        current.append(path)
        current_size += size
    if current:
        chunks.append(current)
    return chunks


def build(output: Path, version: str | None, max_chunk_bytes: int = MAX_CHUNK_BYTES) -> dict:
    if max_chunk_bytes < 64 * 1024:
        raise ValueError("Chunk size must be at least 64 KiB")
    output.mkdir(parents=True, exist_ok=True)
    for child in output.iterdir():
        if child.is_dir():
            shutil.rmtree(child)
        else:
            child.unlink()

    aes = AESGCM(KEY)
    file_entries = []
    chunk_entries = []
    chunk_number = 1
    for group, files in grouped_files().items():
        for group_chunk in split_group(group, files, max_chunk_bytes):
            archive_bytes = make_zip(group_chunk)
            associated_data = f"{FORMAT_VERSION}:{group}:{chunk_number}".encode()
            # Deterministic per plaintext chunk: unchanged assets publish byte-identical bundles.
            nonce = hashlib.sha256(b"gtn-game-data-nonce-v1:" + associated_data + archive_bytes).digest()[:12]
            encrypted = aes.encrypt(nonce, archive_bytes, associated_data)
            name = f"{chunk_number:02d}.bin"
            blob = MAGIC + bytes([FORMAT_VERSION]) + nonce + encrypted
            target = output / name
            target.write_bytes(blob)
            chunk_entries.append({
                "id": name[:-4],
                "file": name,
                "group": group,
                "size": len(blob),
                "sha256": hashlib.sha256(blob).hexdigest(),
                "nonceBytes": len(nonce),
                "fileCount": len(group_chunk),
            })
            for path in group_chunk:
                relative = path.relative_to(ASSETS).as_posix()
                data = path.read_bytes()
                file_entries.append({
                    "path": relative,
                    "chunk": name[:-4],
                    "size": len(data),
                    "sha256": hashlib.sha256(data).hexdigest(),
                })
            chunk_number += 1

    if version is None:
        version_payload = json.dumps(file_entries, sort_keys=True, separators=(",", ":")).encode("utf-8")
        version = hashlib.sha256(version_payload).hexdigest()[:20]
    try:
        source_commit = subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True, stderr=subprocess.DEVNULL
        ).strip()
        source_commit_timestamp = int(subprocess.check_output(
            ["git", "show", "-s", "--format=%ct", "HEAD"], cwd=ROOT, text=True, stderr=subprocess.DEVNULL
        ).strip())
    except (OSError, subprocess.CalledProcessError, ValueError):
        source_commit, source_commit_timestamp = "unknown", 0
    manifest = {
        "schemaVersion": 1,
        "archiveFormatVersion": FORMAT_VERSION,
        "minAppVersionCode": 24,
        "version": version,
        "sourceCommit": source_commit,
        "sourceCommitTimestamp": source_commit_timestamp,
        "keyId": KEY_ID,
        "cipher": "AES-256-GCM",
        "compression": "ZIP-DEFLATE",
        "chunkNaming": "zero-padded-sequential",
        "chunks": chunk_entries,
        "files": file_entries,
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=OUTPUT)
    parser.add_argument("--version", default=None)
    parser.add_argument("--max-chunk-bytes", type=int, default=MAX_CHUNK_BYTES)
    args = parser.parse_args()
    manifest = build(args.output, args.version, args.max_chunk_bytes)
    print(f"Built {len(manifest['chunks'])} encrypted chunks containing {len(manifest['files'])} files at {args.output}")


if __name__ == "__main__":
    main()
