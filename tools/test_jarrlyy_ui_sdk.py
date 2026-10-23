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
            "styles/jarrlyy-ui-advanced.css",
            "components/jarrlyy-ui-advanced.js",
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

    def test_context_menu_keyboard_navigation_handles_multiple_items(self):
        js = (SDK / "components/jarrlyy-ui.js").read_text(encoding="utf-8")
        self.assertIn('const items = $$("button:not(:disabled)", menu);', js)
        self.assertIn('if (event.key === "Tab") { close(); return; }', js)
        self.assertIn('current < 0 ? 0 : (current + 1) % items.length', js)
        self.assertIn('current < 0 ? items.length - 1 : (current - 1 + items.length) % items.length', js)
        self.assertIn('if (!menu) return;\n      button.dataset.juiMenuReady = "true";', js)

    def test_timer_finishes_once_and_skips_interval_for_zero(self):
        js = (SDK / "components/jarrlyy-ui.js").read_text(encoding="utf-8")
        self.assertIn('let ended = false;', js)
        self.assertIn('if (!ended) {', js)
        self.assertIn('node.dispatchEvent(new global.CustomEvent("jui:timer-end"', js)
        self.assertIn('if (!paint()) return;\n      const timer = global.setInterval', js)

    def test_reduced_motion_covers_embedded_sdk_feedback(self):
        css = (SDK / "styles/jarrlyy-ui.css").read_text(encoding="utf-8")
        reduced_motion = css.split('@media (prefers-reduced-motion: reduce)', 1)[1]
        for selector in ('.jui-toast', '.jui-dialog', '.jui-button', '.jui-icon-button', '.jui-tooltip[data-tooltip]::after'):
            with self.subTest(selector=selector):
                self.assertIn(selector, reduced_motion)
        self.assertIn('animation-duration: .01ms !important', reduced_motion)
        self.assertIn('transition-duration: .01ms !important', reduced_motion)

    def test_public_multistep_tour_is_accessible_and_used_by_demo(self):
        js = (SDK / "components/jarrlyy-ui.js").read_text(encoding="utf-8")
        self.assertIn("function tour(steps, options = {})", js)
        self.assertIn('dialog.setAttribute("aria-labelledby", heading.id)', js)
        self.assertIn('dialog.setAttribute("aria-describedby", body.id)', js)
        self.assertIn('body.setAttribute("aria-live", "polite")', js)
        self.assertIn('dialog.addEventListener("cancel", () => {', js)
        self.assertIn('progress.setAttribute("aria-live", "polite")', js)
        self.assertIn('if (typeof options.onComplete === "function") options.onComplete()', js)
        self.assertIn('if (typeof options.onSkip === "function") options.onSkip()', js)
        self.assertRegex(js, r'global\.JarrlyyUI = Object\.freeze\(\{ init, (?:enhance, )?toast, dialog: makeDialog, tour \}\);')
        self.assertIn('if (action === "tour") tour([', js)

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


class JarrlyyUiAdvancedTests(unittest.TestCase):
    def test_advanced_assets_are_present_and_loaded_by_both_hosts(self):
        for relative in ("styles/jarrlyy-ui-advanced.css", "components/jarrlyy-ui-advanced.js"):
            with self.subTest(path=relative):
                self.assertTrue((SDK / relative).is_file())
        web_html = (WEB / "index.html").read_text(encoding="utf-8")
        demo_html = (SDK / "demo.html").read_text(encoding="utf-8")
        self.assertIn("jarrlyy-ui-advanced.css", web_html)
        self.assertIn("jarrlyy-ui-advanced.js", web_html)
        self.assertIn("jarrlyy-ui-advanced.css", demo_html)
        self.assertIn("jarrlyy-ui-advanced.js", demo_html)

    def test_advanced_components_have_real_behaviors_and_accessible_states(self):
        js = (SDK / "components/jarrlyy-ui-advanced.js").read_text(encoding="utf-8")
        css = (SDK / "styles/jarrlyy-ui-advanced.css").read_text(encoding="utf-8")
        demo = (SDK / "demo.html").read_text(encoding="utf-8")
        for behavior in ("initTabs", "initSegmentedControls", "initRangeGroups", "initOverlays", "initBanners", "setLoading", "setProgress", "jui:tab-change", "jui:range-change"):
            with self.subTest(behavior=behavior):
                self.assertIn(behavior, js)
        for selector in (".jui-tabs", ".jui-segmented", ".jui-range-group", ".jui-bottom-sheet", ".jui-drawer", ".jui-banner-success", ".jui-banner-danger", ".jui-skeleton", ".jui-circular-progress", ".jui-fab"):
            with self.subTest(selector=selector):
                self.assertIn(selector, css)
        for component in ("data-jui-tabs", "data-jui-segmented", "data-jui-range", "data-jui-overlay-open", "data-jui-banner-dismiss", "data-jui-loading-demo", "data-jui-progress-demo"):
            with self.subTest(component=component):
                self.assertIn(component, demo)

    def test_advanced_gallery_uses_local_assets_and_no_inline_script_or_style(self):
        html = (SDK / "demo.html").read_text(encoding="utf-8")
        self.assertNotRegex(html, r'''(?:src|href)\s*=\s*["']https?://''')
        self.assertNotIn("<style>", html)
        self.assertNotIn("<script>", html)
        self.assertNotRegex(html, r"\sstyle\s*=")
        self.assertIn("aria-label=", html)
        self.assertIn("prefers-reduced-motion", (SDK / "styles/jarrlyy-ui-advanced.css").read_text(encoding="utf-8"))


    def test_nimmy_tutorial_is_unblurred_and_uses_a_sized_popup_box(self):
        css = (WEB / "styles.css").read_text(encoding="utf-8")
        nimmy_rules = css.split("/* Popup regression fix: Nimmy is a floating guide", 1)[1]
        nimmy_rules = nimmy_rules.split("/*", 1)[0]
        self.assertIn("#nimmyTutorial .nimmy-tutorial-backdrop", nimmy_rules)
        self.assertIn("backdrop-filter: none !important", nimmy_rules)
        self.assertIn("width: min(440px, calc(100vw - 24px))", nimmy_rules)
        self.assertIn("max-height: calc(100dvh - 24px", nimmy_rules)
        self.assertIn("overflow-y: auto", nimmy_rules)
        self.assertIn(".nimmy-tutorial:not(#nimmyTutorial) .nimmy-dialog", nimmy_rules)
        self.assertIn("width: min(520px, calc(100vw - 24px))", nimmy_rules)


if __name__ == "__main__":
    unittest.main(verbosity=2)
