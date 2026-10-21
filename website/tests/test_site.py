#!/usr/bin/env python3
"""Regression checks for the static website."""
from html.parser import HTMLParser
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
PAGES = [ROOT / "index.html", ROOT / "downloads.html"]
SPRITE = ROOT / "assets" / "icons.svg"
REQUIRED = {"arrow-up-right", "arrow-up", "arrow-down", "chevron-up", "sparkles", "rotate-ccw", "target", "trending-up", "trophy", "house", "save", "shield-check", "smartphone", "monitor"}

class Parser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.ids, self.links, self.icons, self.scripts = [], [], [], []
        self.active_script = False
        self.script_parts = []
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if "id" in attrs: self.ids.append(attrs["id"])
        if tag == "a" and attrs.get("href"): self.links.append(attrs["href"])
        if tag == "use" and attrs.get("href", "").startswith("assets/icons.svg#"):
            self.icons.append(attrs["href"].split("#", 1)[1])
        if tag == "script" and not attrs.get("src"):
            self.active_script, self.script_parts = True, []
    def handle_data(self, data):
        if self.active_script: self.script_parts.append(data)
    def handle_endtag(self, tag):
        if tag == "script" and self.active_script:
            self.scripts.append("".join(self.script_parts))
            self.active_script = False

root = ET.parse(SPRITE).getroot()
symbols = {node.attrib["id"] for node in root if node.tag.endswith("symbol")}
assert REQUIRED <= symbols, "SVG sprite is missing required symbols"
for page in PAGES:
    parser = Parser()
    parser.feed(page.read_text(encoding="utf-8"))
    parser.close()
    duplicates = {item for item in parser.ids if parser.ids.count(item) > 1}
    assert not duplicates, f"{page.name}: duplicate IDs: {sorted(duplicates)}"
    assert "main" in parser.ids, f"{page.name}: missing main landmark"
    assert set(parser.icons) <= symbols, f"{page.name}: unknown SVG symbol reference"
    for link in parser.links:
        if link.startswith("#"):
            assert link[1:] in parser.ids, f"{page.name}: broken anchor {link}"
    assert parser.scripts, f"{page.name}: no inline script found"
    for number, script in enumerate(parser.scripts, 1):
        with tempfile.NamedTemporaryFile("w", suffix=".js", encoding="utf-8", delete=False) as temp:
            temp.write(script)
            path = Path(temp.name)
        try:
            subprocess.run(["node", "--check", str(path)], check=True, capture_output=True, text=True)
        except subprocess.CalledProcessError as error:
            raise AssertionError(f"{page.name}: JavaScript syntax error in script {number}:\n{error.stderr}") from error
        finally:
            path.unlink(missing_ok=True)
    print(f"PASS {page.name}: IDs, anchors, SVG references and JavaScript syntax")
print(f"PASS icons.svg: valid XML and {len(symbols)} symbols")
