import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from io import BytesIO

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
import binarify_game_data as builder
import validate_game_data_apk as apk_validator


class BinarifyGameDataTest(unittest.TestCase):
    def test_chunks_round_trip_and_keep_original_paths(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            assets = root / "assets"
            (assets / "game").mkdir(parents=True)
            (assets / "traits" / "seasonal").mkdir(parents=True)
            (assets / "licenses").mkdir(parents=True)
            (assets / "licenses" / "MBEDTLS-LICENSE.txt").write_text("Mbed TLS license")
            (assets / "changelogs.md").write_text("release notes")
            (assets / "game" / "config.json").write_text('{"schemaVersion":1}')
            (assets / "traits" / "seasonal" / "rule.lua").write_text("return {}")
            previous = builder.ASSETS
            builder.ASSETS = assets
            try:
                output = root / "out"
                manifest = builder.build(output, "test-version", 65536)
                second_output = root / "out-second"
                builder.build(second_output, "test-version", 65536)
                self.assertEqual(
                    sorted(path.read_bytes() for path in output.glob("*.bin")),
                    sorted(path.read_bytes() for path in second_output.glob("*.bin")),
                )
                self.assertEqual(
                    (output / "manifest.json").read_bytes(),
                    (second_output / "manifest.json").read_bytes(),
                )
            finally:
                builder.ASSETS = previous

            self.assertEqual(manifest["schemaVersion"], 1)
            self.assertEqual({entry["path"] for entry in manifest["files"]},
                             {"game/config.json", "traits/seasonal/rule.lua", "licenses/MBEDTLS-LICENSE.txt", "changelogs.md"})
            self.assertTrue((output / "01.bin").is_file())
            self.assertTrue((output / "manifest.json").is_file())

            for chunk in manifest["chunks"]:
                blob = (output / chunk["file"]).read_bytes()
                self.assertEqual(blob[:8], builder.MAGIC)
                self.assertEqual(hashlib.sha256(blob).hexdigest(), chunk["sha256"])
                version = blob[8]
                nonce = blob[9:21]
                associated_data = f"{version}:{chunk['group']}:{chunk['id']}".encode()
                plaintext = AESGCM(builder.KEY).decrypt(nonce, blob[21:], associated_data)
                with zipfile.ZipFile(BytesIO(plaintext)) as archive:
                    self.assertTrue(archive.namelist())

    def test_content_addressed_version_is_stable(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            assets = root / "assets"
            (assets / "game").mkdir(parents=True)
            (assets / "game" / "config.json").write_text('{"value":1}')
            previous = builder.ASSETS
            builder.ASSETS = assets
            try:
                first = builder.build(root / "first", None, 65536)
                second = builder.build(root / "second", None, 65536)
            finally:
                builder.ASSETS = previous
            self.assertRegex(first["version"], r"^[a-f0-9]{20}$")
            self.assertEqual(first["version"], second["version"])

    def test_apk_payload_validator_accepts_generated_archive(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            assets = root / "assets"
            (assets / "game").mkdir(parents=True)
            (assets / "game" / "config.json").write_text('{"value":2}')
            previous = builder.ASSETS
            builder.ASSETS = assets
            try:
                output = root / "game_data"
                builder.build(output, None, 65536, encrypt_manifest=True)
            finally:
                builder.ASSETS = previous
            apk = root / "test.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                for path in output.iterdir():
                    archive.write(path, f"assets/game_data/{path.name}")
            chunks, files = apk_validator.validate(apk)
            self.assertEqual(chunks, 1)
            self.assertEqual(files, 1)

    def test_apk_validator_rejects_plaintext_asset_duplicates(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            assets = root / "assets"
            (assets / "game").mkdir(parents=True)
            (assets / "game" / "config.json").write_text('{"value":3}')
            previous = builder.ASSETS
            builder.ASSETS = assets
            try:
                output = root / "game_data"
                builder.build(output, None, 65536, encrypt_manifest=True)
            finally:
                builder.ASSETS = previous
            apk = root / "test-plaintext.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                for path in output.iterdir():
                    archive.write(path, f"assets/game_data/{path.name}")
                archive.writestr("assets/game/config.json", '{"value":3}')
            with self.assertRaises(AssertionError):
                apk_validator.validate(apk)

    def test_generated_asset_can_be_included_under_virtual_path(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            assets = root / "assets"
            assets.mkdir()
            logo = root / "GTN_APP_LOGO.png"
            logo.write_bytes(b"test-logo-bytes")
            previous = builder.ASSETS
            builder.ASSETS = assets
            try:
                manifest = builder.build(
                    root / "out",
                    None,
                    65536,
                    [(logo, "web-play/gtn-app-logo.png")],
                )
            finally:
                builder.ASSETS = previous
            entry = next(item for item in manifest["files"] if item["path"] == "web-play/gtn-app-logo.png")
            self.assertEqual(entry["size"], len(b"test-logo-bytes"))

    def test_version_changes_when_chunk_layout_changes(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            assets = root / "assets"
            (assets / "game").mkdir(parents=True)
            (assets / "game" / "first.bin").write_bytes(b"a" * 40000)
            (assets / "game" / "second.bin").write_bytes(b"b" * 40000)
            previous = builder.ASSETS
            builder.ASSETS = assets
            try:
                split = builder.build(root / "split", None, 65536)
                combined = builder.build(root / "combined", None, 131072)
            finally:
                builder.ASSETS = previous
            self.assertEqual(len(split["chunks"]), 2)
            self.assertEqual(len(combined["chunks"]), 1)
            self.assertNotEqual(split["version"], combined["version"])

    def test_rejects_too_small_chunk_limit(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaises(ValueError):
                builder.build(Path(temporary), "test", 1024)

    def test_apk_manifest_is_encrypted_and_round_trips(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            assets = root / "assets"
            (assets / "game").mkdir(parents=True)
            source_json = '{"secretConfig":"not-readable-in-apk"}'
            (assets / "game" / "config.json").write_text(source_json)
            previous = builder.ASSETS
            builder.ASSETS = assets
            try:
                output = root / "game_data"
                manifest = builder.build(output, "encrypted-test", 65536, encrypt_manifest=True)
            finally:
                builder.ASSETS = previous

            encrypted_manifest = (output / "manifest.bin").read_bytes()
            self.assertFalse((output / "manifest.json").exists())
            self.assertNotIn(b'"schemaVersion"', encrypted_manifest)
            self.assertEqual(builder.decrypt_manifest_blob(encrypted_manifest), manifest)

            apk = root / "encrypted-manifest.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                for path in output.iterdir():
                    archive.write(path, f"assets/game_data/{path.name}")
            with zipfile.ZipFile(apk) as archive:
                packaged_bytes = b"".join(archive.read(name) for name in archive.namelist())
            self.assertNotIn(source_json.encode(), packaged_bytes)
            chunks, files = apk_validator.validate(apk)
            self.assertEqual((chunks, files), (1, 1))

    def test_apk_validator_rejects_plaintext_json_inside_game_data_directory(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            assets = root / "assets"
            (assets / "game").mkdir(parents=True)
            (assets / "game" / "config.json").write_text('{"value":"hidden"}')
            previous = builder.ASSETS
            builder.ASSETS = assets
            try:
                output = root / "game_data"
                builder.build(output, None, 65536, encrypt_manifest=True)
            finally:
                builder.ASSETS = previous
            apk = root / "test-plaintext-game-data.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                for path in output.iterdir():
                    archive.write(path, f"assets/game_data/{path.name}")
                archive.writestr("assets/game_data/game/config.json", '{"value":"hidden"}')
            with self.assertRaises(AssertionError):
                apk_validator.validate(apk)

    def test_encrypted_manifest_rejects_tampering(self):
        blob = bytearray(builder.encrypt_manifest_blob({"version": "test"}))
        blob[-1] ^= 1
        with self.assertRaises(Exception):
            builder.decrypt_manifest_blob(bytes(blob))


if __name__ == "__main__":
    unittest.main()
