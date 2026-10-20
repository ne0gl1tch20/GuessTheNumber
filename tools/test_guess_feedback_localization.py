#!/usr/bin/env python3
"""Regression tests for localized TOO_LOW / TOO_HIGH feedback data flow."""
from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCALES = ROOT / "app/src/main/assets/locales"
BRIDGE = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ProductionWebPlayScreen.kt"
APP_JS = ROOT / "app/src/main/assets/web-play/app.js"


class GuessFeedbackLocalizationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.bridge = BRIDGE.read_text(encoding="utf-8")
        cls.js = APP_JS.read_text(encoding="utf-8")

    def test_every_locale_has_both_thirty_message_pools(self) -> None:
        files = sorted(LOCALES.glob("*.json"))
        self.assertGreater(len(files), 0, "No locale assets found")
        for path in files:
            locale = json.loads(path.read_text(encoding="utf-8"))
            for index in range(30):
                for prefix in ("feedback_toolow_", "feedback_toohigh_"):
                    key = f"{prefix}{index}"
                    self.assertIsInstance(locale.get(key), str, f"{path.name} missing {key}")
                    self.assertTrue(locale[key].strip(), f"{path.name} has empty {key}")

    def test_kotlin_bridge_sends_all_localized_feedback_keys(self) -> None:
        self.assertIn('(0..29).forEach { index ->', self.bridge)
        self.assertIn('put("feedback_toolow_$index", activeLocaleManager.getString("feedback_toolow_$index"))', self.bridge)
        self.assertIn('put("feedback_toohigh_$index", activeLocaleManager.getString("feedback_toohigh_$index"))', self.bridge)

    def test_kotlin_message_pools_use_the_correct_localized_keys(self) -> None:
        pools = self.bridge.split('.put("incorrectGuessMessages"', 1)[1].split('.put("money"', 1)[0]
        self.assertIn('val key = "feedback_toolow_$index"', pools)
        self.assertIn('val key = "feedback_toohigh_$index"', pools)
        self.assertNotIn('for (index in 1..50)', pools)

    def test_renderer_selects_only_the_active_direction(self) -> None:
        self.assertIn("const lastFeedbackByDirection", self.js)
        self.assertIn("if(status==='TOO_LOW'){feedbackMessage=chooseFeedback", self.js)
        self.assertIn("else if(status==='TOO_HIGH'){feedbackMessage=chooseFeedback", self.js)
        self.assertNotIn("TOO_LOW:chooseFeedback", self.js)
        self.assertNotIn("TOO_HIGH:chooseFeedback", self.js)

    def test_localized_pool_precedes_legacy_fallback(self) -> None:
        start = self.js.index("function chooseFeedback(")
        end = self.js.index("\nfunction render(s)", start)
        chooser = self.js[start:end]
        self.assertIn("const localized=Array.from({length:30}", chooser)
        self.assertIn("localized.length?localized:(provided.length?provided:fallback)", chooser)
        self.assertIn("lastFeedbackByDirection[direction]", chooser)


if __name__ == "__main__":
    unittest.main(verbosity=2)
