#!/usr/bin/env python3
"""Release-contract checks for the shared Jarrlyy UI SDK."""
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools.generate_web_play_manifest import build_manifest
BUNDLED = ROOT / "app/src/main/assets/libraries/jarrlyyuisdk"
PUBLIC = ROOT / "website/jarrlyyuisdk"
WORKFLOW = ROOT / ".github/workflows/web-play-manifest.yml"
PAGES_WORKFLOW = ROOT / ".github/workflows/pages.yml"
WEB_PLAY_HTML = ROOT / "app/src/main/assets/web-play/index.html"

MIRRORED_ASSETS = (
    "components/jarrlyy-ui.js",
    "components/jarrlyy-ui-advanced.js",
    "components/jarrlyy-ui-host.js",
    "components/demo.js",
    "styles/jarrlyy-ui.css",
    "styles/jarrlyy-ui-advanced.css",
    "styles/demo.css",
    "icons/jarrlyy-icons.svg",
    "motion-demo.js",
)


class JarrlyySdkReleaseTests(unittest.TestCase):
    def test_public_and_bundled_sdk_assets_stay_in_lockstep(self):
        for relative in MIRRORED_ASSETS:
            with self.subTest(asset=relative):
                bundled = (BUNDLED / relative).read_bytes()
                public = (PUBLIC / relative).read_bytes()
                self.assertEqual(bundled, public, f"SDK copies differ: {relative}")

    def test_host_adapter_is_loaded_by_game_and_both_component_labs(self):
        web_play = WEB_PLAY_HTML.read_text(encoding="utf-8")
        bundled_demo = (BUNDLED / "demo.html").read_text(encoding="utf-8")
        public_demo = (PUBLIC / "demo.html").read_text(encoding="utf-8")
        self.assertIn("../libraries/jarrlyyuisdk/components/jarrlyy-ui-host.js", web_play)
        self.assertIn('src="components/jarrlyy-ui-host.js"', bundled_demo)
        self.assertIn('src="components/jarrlyy-ui-host.js"', public_demo)

    def test_host_adapter_exposes_framework_free_lifecycle_and_events(self):
        adapter = (BUNDLED / "components/jarrlyy-ui-host.js").read_text(encoding="utf-8")
        for feature in ("function mount(options = {})", "function setTheme(root, theme)", "function validEventName(name)", "on:", "emit:", "JarrlyyUIAdvanced.init()", "JarrlyyUIMotion.init(root)"):
            with self.subTest(feature=feature):
                self.assertIn(feature, adapter)
        self.assertIn('data-jui-theme', adapter)
        self.assertNotIn("addJavascriptInterface", adapter)

    def test_motion_demo_uses_the_published_motion_global(self):
        script = (BUNDLED / "motion-demo.js").read_text(encoding="utf-8")
        self.assertIn("motion.count(label, 76", script)
        self.assertNotIn("ui.motion", script)

    def test_manifest_includes_all_sdk_assets_for_no_apk_sync(self):
        manifest = build_manifest("a" * 40)
        paths = {entry["path"] for entry in manifest["files"]}
        for relative in MIRRORED_ASSETS:
            with self.subTest(asset=relative):
                self.assertIn("libraries/jarrlyyuisdk/" + relative, paths)
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn('"app/src/main/assets/libraries/**"', workflow)
        self.assertIn("python tools/generate_web_play_manifest.py", workflow)
        self.assertIn("git push origin HEAD:master", workflow)
        self.assertIn("github.event_name == 'push'", workflow)

    def test_pages_pipeline_validates_host_adapter_and_motion_demo(self):
        workflow = PAGES_WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("website/jarrlyyuisdk/components/jarrlyy-ui-host.js", workflow)
        self.assertIn("website/jarrlyyuisdk/motion-demo.js", workflow)


if __name__ == "__main__":
    unittest.main(verbosity=2)
