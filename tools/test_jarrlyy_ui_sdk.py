#!/usr/bin/env python3
"""Structural smoke tests for the local Jarrlyy UI SDK and its demo."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SDK = ROOT / "app/src/main/assets/libraries/jarrlyyuisdk"
WEB = ROOT / "app/src/main/assets/web-play"

class JarrlyyUiSdkTests(unittest.TestCase):
    def test_sdk_files_exist(self):
        for relative in (
            "README.md",
            "styles/jarrlyy-ui.css",
            "components/jarrlyy-ui.js",
            "icons/jarrlyy-icons.svg",
            "styles/demo.css",
            "components/demo.js",
            "demo.html",
        ):
            with self.subTest(path=relative):
                self.assertTrue((SDK / relative).is_file(), f"Missing SDK file: {relative}")

    def test_main_web_bundle_loads_local_sdk(self):
        html = (WEB / "index.html").read_text(encoding="utf-8")
        self.assertIn('../libraries/jarrlyyuisdk/styles/jarrlyy-ui.css', html)
        self.assertIn('../libraries/jarrlyyuisdk/components/jarrlyy-ui.js', html)

    def test_demo_references_local_files_only(self):
        html = (SDK / "demo.html").read_text(encoding="utf-8")
        self.assertIn('styles/jarrlyy-ui.css', html)
        self.assertIn('components/jarrlyy-ui.js', html)
        self.assertIn('icons/jarrlyy-icons.svg#', html)
        self.assertNotRegex(html, r'''(?:src|href)\s*=\s*["']https?://''')

    def test_interactive_api_and_components_are_present(self):
        css = (SDK / "styles/jarrlyy-ui.css").read_text(encoding="utf-8")
        js = (SDK / "components/jarrlyy-ui.js").read_text(encoding="utf-8")
        demo = (SDK / "demo.html").read_text(encoding="utf-8")
        for selector in (".jui-button", ".jui-switch", ".jui-table", ".jui-dialog", ".jui-xp", ".jui-slot", ".jui-achievement"):
            with self.subTest(selector=selector):
                self.assertIn(selector, css)
        for feature in ("toast", "dialog", "initContextMenus", "initCarousels", "initPagination", "initTables", "initCharts", "initTimers", "initDropzones"):
            with self.subTest(feature=feature):
                self.assertIn(feature, js)
        for component in ("data-jui-carousel", "data-jui-pagination", "data-jui-chart", "data-jui-timer", "data-jui-dropzone", 'type="datetime-local"'):
            with self.subTest(component=component):
                self.assertIn(component, demo)

    def test_sdk_collection_queries_use_query_selector_all(self):
        js = (SDK / "components/jarrlyy-ui.js").read_text(encoding="utf-8")
        # Regression guard: querySelector() returns one node, not a collection.
        # Any plural initializer accidentally using querySelector() instead of querySelectorAll() will fail here.
        self.assertEqual([], re.findall(r'(?<!\$)\$\(([^)]*)\)\.forEach', js))
        for selector in (
            '[data-jui-menu-toggle]',
            '.jui-carousel[data-jui-carousel]',
            '[data-jui-pagination]',
            '[data-jui-sort-key]',
            'svg[data-jui-chart]',
            '[data-jui-timer]',
            '[data-jui-dropzone]',
            '[data-jui-action]',
            '[data-jui-color-target]',
        ):
            with self.subTest(selector=selector):
                self.assertIn('$$("' + selector + '")', js)

    def test_sdk_interactions_and_accessibility_regressions(self):
        js = (SDK / "components/jarrlyy-ui.js").read_text(encoding="utf-8")
        css = (SDK / "styles/jarrlyy-ui.css").read_text(encoding="utf-8")
        self.assertIn('menu.addEventListener("keydown"', js)
        self.assertIn('items[next].focus({ preventScroll: true })', js)
        self.assertIn('button.removeAttribute("aria-current")', js)
        self.assertIn('heading.setAttribute("aria-sort"', js)
        self.assertIn('previousFocus.focus({ preventScroll: true })', js)
        self.assertIn(':focus-visible', css)
        self.assertIn('min-height: 44px', css)
        self.assertIn('prefers-reduced-motion: reduce', css)

    def test_player_notifications_use_sdk_toast_with_legacy_fallback(self):
        js = (WEB / "app.js").read_text(encoding="utf-8")
        self.assertIn("window.JarrlyyUI&&typeof window.JarrlyyUI.toast==='function'", js)
        self.assertIn("window.JarrlyyUI.toast(message);return", js)
        self.assertIn("el.toast.textContent=message", js)
        self.assertIn('role="status"', (WEB / "index.html").read_text(encoding="utf-8"))

    def test_demo_uses_external_assets_under_csp(self):
        html = (SDK / "demo.html").read_text(encoding="utf-8")
        self.assertIn("styles/demo.css", html)
        self.assertIn("components/demo.js", html)
        self.assertNotIn("<style>", html)
        self.assertNotIn("<script>", html)
        self.assertNotRegex(html, r"\sstyle\s*=")

    def test_android_developer_settings_can_open_the_sdk_lab(self):
        screen = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/JarrlyyUiSdkScreen.kt"
        nav = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber/ui/navigation/NavGraph.kt"
        dev_settings = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/DevSettingsScreen.kt"
        screen_text = screen.read_text(encoding="utf-8")
        nav_text = nav.read_text(encoding="utf-8")
        dev_text = dev_settings.read_text(encoding="utf-8")
        self.assertIn('"/assets/libraries/jarrlyyuisdk/"', screen_text)
        self.assertIn('JUI_DEMO_URL', screen_text)
        self.assertIn('WebViewAssetLoader', screen_text)
        self.assertIn('settings.allowFileAccess = false', screen_text)
        self.assertIn('settings.allowContentAccess = false', screen_text)
        self.assertNotIn('addJavascriptInterface', screen_text)
        self.assertIn('"jarrlyy_ui_sdk"', nav_text)
        self.assertIn('onNavigate("jarrlyy_ui_sdk")', dev_text)

if __name__ == "__main__":
    unittest.main(verbosity=2)
