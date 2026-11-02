#!/usr/bin/env python3
"""Guard the Kotlin-owned localization contract for shared Web Play UI."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "app/src/main/assets/web-play/index.html"
JS = ROOT / "app/src/main/assets/web-play/app.js"
ENGLISH = ROOT / "app/src/main/assets/locales/en_us.json"
ANDROID_LOCALE = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber/data/repository/LocaleManager.kt"
ANDROID_BRIDGE = ROOT / "app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ProductionWebPlayScreen.kt"
DESKTOP_BRIDGE = ROOT / "desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopWebPlayState.kt"

html = HTML.read_text(encoding="utf-8")
js = JS.read_text(encoding="utf-8")
catalog = json.loads(ENGLISH.read_text(encoding="utf-8"))

# Catch malformed duplicate HTML attributes before browsers silently choose one.
for tag in re.findall(r"<[a-z][^>]*>", html, flags=re.IGNORECASE):
    attributes = re.findall(r'\b([a-z][\w:-]*)\s*=', tag, flags=re.IGNORECASE)
    duplicates = sorted(name for name in set(attributes) if attributes.count(name) > 1)
    assert not duplicates, f"Duplicate HTML attributes {duplicates}: {tag[:240]}"

for match in re.finditer(r'<input\b([^>]*)\bplaceholder="([^"]*)"([^>]*)>', html, flags=re.IGNORECASE):
    placeholder = match.group(2)
    attributes = match.group(1) + match.group(3)
    if placeholder.strip():
        assert re.search(r'\bdata-i18n-placeholder="[^"]+"', attributes), (
            f"Input placeholder is missing a Kotlin locale key: {match.group(0)}"
        )
android_locale = ANDROID_LOCALE.read_text(encoding="utf-8")
android_bridge = ANDROID_BRIDGE.read_text(encoding="utf-8")
desktop_bridge = DESKTOP_BRIDGE.read_text(encoding="utf-8")

# Static labels, placeholders and accessible names must resolve through Kotlin's
# complete English-backed catalog. Locale overlays replace these defaults.
static_keys = set()
for attribute in ("data-i18n", "data-i18n-placeholder", "data-i18n-aria-label", "data-i18n-alt"):
    keys = re.findall(rf'{attribute}="([^"]+)"', html)
    static_keys.update(keys)
    missing = sorted(set(key for key in keys if not catalog.get(key)))
    assert not missing, f"{attribute} keys missing from English fallback: {missing}"

# Literal uiText keys used by JS-generated screens must also exist in Kotlin's
# active locale payload rather than silently using local-only English strings.
ui_keys = re.findall(r"""uiText\(['"]([^'"]+)['"]\s*,\s*['"`]""", js)
missing_ui = sorted(set(key for key in ui_keys if not catalog.get(key)))
assert not missing_ui, f"JS uiText keys missing from Kotlin catalog: {missing_ui}"

assert "fun getAllStrings(): JSONObject = JSONObject(translations.toString())" in android_locale
assert "fun getEnglishStrings(): JSONObject = JSONObject(englishCatalog.toString())" in android_locale
assert '.put("englishUiStrings", activeLocaleManager.getEnglishStrings())' in android_bridge
assert '.put("englishUiStrings", desktopEnglishWebUiStrings())' in desktop_bridge
assert "function localizeCatalogText(value)" in js
assert "const english=lastState?.englishUiStrings,active=lastState?.uiStrings" in js
assert "createTreeWalker(document.body,NodeFilter.SHOW_TEXT)" in js
assert "englishTextEntries.sort((a,b)=>b[0].length-a[0].length)" in js
assert '.put("uiStrings", activeLocaleManager.getAllStrings()' in android_bridge
assert "return merged" in desktop_bridge
assert "const locale=String(lastState?.settings?.locale||'en-US')" in js
assert "document.documentElement.dir=" in js
assert "[data-i18n-aria-label]" in js
assert "ring.setAttribute('aria-label',uiText('web_guess_accuracy_aria'" in js
assert "input.setAttribute('aria-label',uiText('web_creative_level_aria'" in js
assert "textNodes=Array.from(node.childNodes)" in js
assert "node.textContent=value" not in js, "Localization must preserve nested SVG icons"
assert "renderTraitLoadout(s);applyAuxiliaryTranslations();" in js

print(
    f"Kotlin-owned Web Play localization passed "
    f"({len(static_keys)} static keys, "
    f"{len(set(ui_keys))} JS keys, {len(catalog)} English fallback entries)."
)
