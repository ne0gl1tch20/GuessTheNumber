import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from io import BytesIO

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
import binarify_game_data as builder


class BinarifyGameDataTest(unittest.TestCase):
    def test_chunks_round_trip_and_keep_original_paths(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            assets = root / "assets"
            (assets / "game").mkdir(parents=True)
            (assets / "traits" / "seasonal").mkdir(parents=True)
            (assets / "game" / "config.json").write_text('{"schemaVersion":1}')
            (assets / "traits" / "seasonal" / "rule.lua").write_text("return {}")
            previous = builder.ASSETS
            builder.ASSETS = assets
            try:
                output = root / "out"
                manifest = builder.build(output, "test-version", 65536)
            finally:
                builder.ASSETS = previous

            self.assertEqual(manifest["schemaVersion"], 1)
            self.assertEqual({entry["path"] for entry in manifest["files"]},
                             {"game/config.json", "traits/seasonal/rule.lua"})
            self.assertTrue((output / "01.bin").is_file())
            self.assertTrue((output / "manifest.json").is_file())

            for chunk in manifest["chunks"]:
                blob = (output / chunk["file"]).read_bytes()
                self.assertEqual(blob[:8], builder.MAGIC)
                self.assertEqual(hashlib.sha256(blob).hexdigest(), chunk["sha256"])
                version = blob[8]
                nonce = blob[9:21]
                associated_data = f"{version}:{chunk['group']}:{int(chunk['id'])}".encode()
                plaintext = AESGCM(builder.KEY).decrypt(nonce, blob[21:], associated_data)
                with zipfile.ZipFile(BytesIO(plaintext)) as archive:
                    self.assertTrue(archive.namelist())

    def test_rejects_too_small_chunk_limit(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaises(ValueError):
                builder.build(Path(temporary), "test", 1024)


if __name__ == "__main__":
    unittest.main()
