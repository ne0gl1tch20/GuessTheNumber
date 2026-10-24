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


    def test_game_dialogs_live_in_a_viewport_level_popup_layer(self):
        html = (WEB / "index.html").read_text(encoding="utf-8")
        css = (WEB / "styles.css").read_text(encoding="utf-8")
        main_end = html.index("</main>")
        layer_start = html.index('<div id="popupLayer" class="popup-layer"')
        layer_end = html.index('</div>\n  <div id="toast"', layer_start)
        self.assertLess(main_end, layer_start)
        self.assertLess(html.index('id="offlineRewardOverlay"', layer_start), layer_end)
        self.assertLess(html.index('id="dailyRewardOverlay"', layer_start), layer_end)
        self.assertLess(html.index('id="resetMovedDialog"', layer_start), layer_end)
        self.assertLess(html.index('id="resetEverythingDialog"', layer_start), layer_end)
        self.assertLess(html.index('id="nimmyTutorial"', layer_start), layer_end)
        self.assertIn(".popup-layer {", css)
        self.assertIn("position: fixed;", css)
        self.assertIn("overflow: visible;", css)
        self.assertIn("#nimmyTutorial .nimmy-dialog", css)
        self.assertIn("width: min(440px, calc(100vw - 24px))", css)
        self.assertIn("max-height: calc(100dvh - 24px", css)
        self.assertIn("overflow-y: auto", css)
        self.assertIn("backdrop-filter: none;", css)

        # Nimmy must stay inside its viewport overlay without a nested fixed box.
        nimmy_rules = css.split("#nimmyTutorial .nimmy-dialog", 1)[1].split("}", 1)[0]
        self.assertIn("position: absolute;", nimmy_rules)
        self.assertIn("transform: translateX(-50%);", nimmy_rules)

        # Reset prompts are already centered by the popup-layer grid. Their
        # legacy translate(-50%, -50%) must not shift them out of that grid.
        for dialog_id in ("resetEverythingDialog", "resetMovedDialog"):
            with self.subTest(dialog_id=dialog_id):
                dialog_rules = css.split(f"#{dialog_id} .nimmy-dialog", 1)[1].split("}", 1)[0]
                self.assertIn("transform: none;", dialog_rules)
                self.assertIn("position: relative;", dialog_rules)
                self.assertIn("display: block;", dialog_rules)
                self.assertIn("min-height: min-content;", dialog_rules)

        # The reset redirect overlay must accept input and size itself to the
        # viewport; the old Nimmy base class has pointer-events disabled.
        moved_overlay = css.split("#resetMovedDialog {", 1)[1].split("}", 1)[0]
        self.assertIn("pointer-events: auto;", moved_overlay)
        self.assertIn("place-items: center;", moved_overlay)
        self.assertIn("overflow-y: auto;", moved_overlay)



    def test_settings_toggles_use_jarrlyy_sdk_switches(self):
        html = (WEB / "index.html").read_text(encoding="utf-8")
        css = (WEB / "styles.css").read_text(encoding="utf-8")
        self.assertIn('class="setting-row jui-switch"', html)
        self.assertIn('class="jui-switch-track" aria-hidden="true"', html)
        self.assertIn('#screen-settings .setting-row.jui-switch input:checked + .jui-switch-track', css)

    def test_light_theme_overrides_legacy_dark_text_colors(self):
        css = (WEB / "styles.css").read_text(encoding="utf-8")
        self.assertIn(':root[data-theme-mode="Light"] body.jui-game-compat', css)
        self.assertIn('color: var(--text);', css)
        self.assertIn(':root[data-theme-mode="Light"] .setting-row small', css)
        self.assertIn('color: var(--muted);', css)


    def test_shared_popup_sizing_standard_covers_nimmy_arcade_and_sdk_prompts(self):
        app_css = (WEB / "styles.css").read_text(encoding="utf-8")
        sdk_css = (SDK / "styles/jarrlyy-ui.css").read_text(encoding="utf-8")
        sdk_js = (SDK / "components/jarrlyy-ui.js").read_text(encoding="utf-8")
        for css, label in ((app_css, "app"), (sdk_css, "sdk")):
            with self.subTest(stylesheet=label):
                self.assertIn("max-height: calc(100dvh - 24px", css)
                self.assertIn("overflow-wrap: anywhere", css)
                self.assertIn("overscroll-behavior: contain", css)
        self.assertIn(".popup-layer .nimmy-dialog", app_css)
        self.assertIn("body > #arcadeGame.arcade-game:not([hidden])", app_css)
        self.assertIn(".jui-dialog-actions > .jui-button", sdk_css)
        self.assertIn("function makeDialog", sdk_js)
        self.assertIn("function tour", sdk_js)


    def test_daily_reward_popup_uses_viewport_pinned_scrollable_layout(self):
        css = (WEB / "styles.css").read_text(encoding="utf-8")
        # Android WebView can report a stale/tiny vh value. The Daily Rewards
        # overlay must be pinned by top/bottom edges, and the dialog must scroll
        # internally instead of inheriting its older overflow:hidden styling.
        daily_overlay = css.rsplit("#dailyRewardOverlay {", 1)[1].split("}", 1)[0]
        self.assertIn("top: max(12px, env(safe-area-inset-top));", daily_overlay)
        self.assertIn("bottom: max(12px, env(safe-area-inset-bottom));", daily_overlay)
        daily_dialog = css.rsplit("#dailyRewardOverlay .daily-reward-dialog {", 1)[1].split("}", 1)[0]
        self.assertIn("max-height: 100%;", daily_dialog)
        self.assertIn("overflow-x: hidden;", daily_dialog)
        self.assertIn("overflow-y: auto;", daily_dialog)


    def test_arcade_popup_closes_when_claim_button_sends_reward(self):
        app = (WEB / "app.js").read_text(encoding="utf-8")
        # Close immediately after the bridge accepts the send, so the popup
        # does not depend on a later state response to dismiss itself.
        self.assertIn(
            "claim.onclick=()=>{if(!send('arcade_reward',{gameId:activeArcade,score:String(score)}))return;closeArcade();",
            app,
        )
        # Keep the response-side close as a harmless fallback on success.
        self.assertIn(
            "else if(completedType==='arcade_reward'){toast('Arcade reward saved! ✨');closeArcade()}",
            app,
        )


if __name__ == "__main__":
    unittest.main(verbosity=2)
