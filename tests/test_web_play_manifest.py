import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from tools.generate_web_play_manifest import build_manifest


class WebPlayManifestTest(unittest.TestCase):
    def test_manifest_contains_required_files_and_sha256(self):
        manifest = build_manifest("a" * 40)
        by_path = {item["path"]: item["sha256"] for item in manifest["files"]}
        self.assertTrue({"web-play/index.html", "web-play/app.js", "web-play/styles.css"} <= by_path.keys())
        for path, digest in by_path.items():
            self.assertEqual(len(digest), 64, path)
            self.assertEqual(digest, digest.lower())
        self.assertEqual(manifest["schemaVersion"], 1)
        self.assertEqual(manifest["version"], "a" * 40)

    def test_manifest_hashes_match_repository_bytes(self):
        manifest = build_manifest("b" * 40)
        assets = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets"
        for item in manifest["files"]:
            expected = hashlib.sha256((assets / item["path"]).read_bytes()).hexdigest()
            self.assertEqual(item["sha256"], expected, item["path"])


if __name__ == "__main__":
    unittest.main()
