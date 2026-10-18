#!/usr/bin/env python3
"""Fast structural checks for the shared HTML/CSS/JS bundle."""

from __future__ import annotations

import argparse
import sys
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WEB = ROOT / "app" / "src" / "main" / "assets" / "web-play"


class BundleHTMLParser(HTMLParser):
    VOID_TAGS = {
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
    }

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.ids: set[str] = set()
        self.duplicates: set[str] = set()
        self.stack: list[str] = []
        self.errors: list[str] = []
        self.local_refs: list[str] = []
        self.script_sources: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        values = dict(attrs)
        element_id = values.get("id")
        if element_id:
            if element_id in self.ids:
                self.duplicates.add(element_id)
            self.ids.add(element_id)

        for attr in ("src", "href"):
            value = values.get(attr)
            if value and not value.startswith(("#", "https://", "http://", "data:", "mailto:", "tel:")):
                self.local_refs.append(value.split("?", 1)[0].split("#", 1)[0])
        if tag == "script" and values.get("src"):
            self.script_sources.append(values["src"])

        if tag not in self.VOID_TAGS:
            self.stack.append(tag)

    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.handle_starttag(tag, attrs)
        if tag not in self.VOID_TAGS and self.stack:
            self.stack.pop()

    def handle_endtag(self, tag: str) -> None:
        if tag in self.VOID_TAGS:
            self.errors.append(f"Void element </{tag}> must not have an end tag")
            return
        if tag not in self.stack:
            self.errors.append(f"Unexpected closing tag </{tag}>")
            return
        if self.stack[-1] != tag:
            self.errors.append(
                f"Mismatched closing tag </{tag}> while <{self.stack[-1]}> is open"
            )
            if tag in self.stack:
                self.stack = self.stack[: self.stack.index(tag)]
            return
        self.stack.pop()


def check_css(path: Path) -> list[str]:
    text = path.read_text(encoding="utf-8")
    errors: list[str] = []
    opening: list[int] = []
    quote: str | None = None
    escaped = False
    comment = False
    i = 0

    while i < len(text):
        char = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""
        if comment:
            if char == "*" and nxt == "/":
                comment = False
                i += 2
                continue
            i += 1
            continue
        if quote:
            if escaped:
                escaped = False
            elif char == "\\\":
                escaped = True
            elif char == quote:
                quote = None
            i += 1
            continue
        if char == "/" and nxt == "*":
            comment = True
            i += 2
            continue
        if char in ("'", '"'):
            quote = char
        elif char == "{":
            opening.append(i)
        elif char == "}":
            if not opening:
                errors.append(f"Unexpected '}}' at CSS character {i}")
            else:
                opening.pop()
        i += 1

    if comment:
        errors.append("Unclosed CSS comment")
    if quote:
        errors.append("Unclosed CSS string")
    if opening:
        errors.append(f"{len(opening)} unclosed CSS block(s)")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--css-only", action="store_true")
    args = parser.parse_args()

    css_path = WEB / "styles.css"
    css_errors = check_css(css_path)
    if css_errors:
        for error in css_errors:
            print(f"CSS ERROR: {error}")
        return 1
    print("CSS structure: OK")

    if args.css_only:
        return 0

    html_path = WEB / "index.html"
    html = BundleHTMLParser()
    try:
        html.feed(html_path.read_text(encoding="utf-8"))
        html.close()
    except Exception as error:  # HTMLParser errors should be visible in CI
        print(f"HTML ERROR: {error}")
        return 1

    if html.duplicates:
        html.errors.append("Duplicate id(s): " + ", ".join(sorted(html.duplicates)))
    if html.stack:
        html.errors.append("Unclosed HTML element(s): " + ", ".join(html.stack[-10:]))

    missing = []
    for reference in html.local_refs:
        # Fragment-only and empty references were already filtered above.
        target = (WEB / reference).resolve()
        if not target.is_relative_to(WEB.resolve()):
            missing.append(f"{reference} (path escapes web bundle)")
        elif not target.is_file():
            missing.append(f"{reference} (file not found)")
    if missing:
        html.errors.append("Missing/unsafe local asset(s): " + ", ".join(sorted(set(missing))))

    if html.errors:
        for error in html.errors:
            print(f"HTML ERROR: {error}")
        return 1

    print(f"HTML structure: OK ({len(html.ids)} unique IDs)")
    print(f"Local asset references: OK ({len(html.local_refs)} checked)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
