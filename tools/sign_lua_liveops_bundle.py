#!/usr/bin/env python3
"""Create the signed manifest envelope consumed by the Android Lua LiveOps client.

The private key must be supplied by a protected environment variable and must never
be committed. Uses OpenSSL for RSA/SHA-256 so no Python crypto package is required.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import subprocess
import tempfile
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--payload", type=Path, required=True, help="Payload JSON with a scripts array")
    parser.add_argument("--scripts-dir", type=Path, required=True, help="Directory containing referenced .lua files")
    parser.add_argument("--private-key", default=os.environ.get("LUA_LIVEOPS_PRIVATE_KEY"), help="Protected RSA PEM key path")
    parser.add_argument("--output", type=Path, default=Path("manifest.json"))
    args = parser.parse_args()

    if not args.private_key:
        parser.error("provide --private-key or set LUA_LIVEOPS_PRIVATE_KEY to a protected PEM file path")
    key_path = Path(args.private_key)
    if not key_path.is_file():
        parser.error("private key file does not exist")

    payload = json.loads(args.payload.read_text(encoding="utf-8"))
    if payload.get("schemaVersion") != 1 or payload.get("hostApiVersion") != 1:
        parser.error("unsupported payload schema or host API version")
    scripts = payload.get("scripts")
    if not isinstance(scripts, list) or not 1 <= len(scripts) <= 50:
        parser.error("payload must contain 1 to 50 scripts")

    seen: set[str] = set()
    for entry in scripts:
        script_id = entry.get("id", "")
        file_name = entry.get("fileName", "")
        if not isinstance(script_id, str) or not __import__("re").fullmatch(r"[a-z0-9_-]{1,64}", script_id):
            parser.error(f"invalid script ID: {script_id!r}")
        if script_id in seen:
            parser.error(f"duplicate script ID: {script_id}")
        seen.add(script_id)
        if not isinstance(file_name, str) or not __import__("re").fullmatch(r"[A-Za-z0-9_-]{1,80}\.lua", file_name):
            parser.error(f"invalid script file name: {file_name!r}")
        script_path = args.scripts_dir / file_name
        if not script_path.is_file():
            parser.error(f"missing script file: {script_path}")
        script_bytes = script_path.read_bytes()
        if not script_bytes or len(script_bytes) > 32768:
            parser.error(f"script must be 1..32768 bytes: {script_path}")
        entry["sha256"] = hashlib.sha256(script_bytes).hexdigest()
        entry.setdefault("featured", False)
        entry.setdefault("hostApiVersion", 1)
        entry.setdefault("capabilities", ["log"])
        if entry["hostApiVersion"] != 1 or any(cap != "log" for cap in entry["capabilities"]):
            parser.error(f"unsupported host API or capability in script {script_id}")

    payload_bytes = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    with tempfile.TemporaryDirectory(prefix="gtn-lua-sign-") as temp_dir:
        payload_path = Path(temp_dir) / "payload.json"
        signature_path = Path(temp_dir) / "signature.bin"
        payload_path.write_bytes(payload_bytes)
        subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", str(key_path), "-out", str(signature_path), str(payload_path)],
            check=True,
        )
        signature = base64.b64encode(signature_path.read_bytes()).decode("ascii")

    envelope = {
        "payloadBase64": base64.b64encode(payload_bytes).decode("ascii"),
        "signatureBase64": signature,
    }
    args.output.write_text(json.dumps(envelope, indent=2) + "\n", encoding="utf-8")
    print(f"Signed bundle version {payload['bundleVersion']} with {len(scripts)} script(s): {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
