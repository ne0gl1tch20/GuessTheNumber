#!/usr/bin/env python3
"""Repository-wide HTML/CSS/JS quality and hardcoded UI text audit."""
from __future__ import annotations
import argparse, re, subprocess, sys
from collections import defaultdict
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXTENSIONS = {".html", ".htm", ".css", ".js", ".mjs", ".cjs"}
SKIP_PARTS = {".git", "node_modules", "build", "dist", "target", ".gradle", ".idea", "vendor", "third_party", "third-party"}
TEXT_ATTRS = {"placeholder", "title", "aria-label"}
VOID_TAGS = {"area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"}
WORD_RE = re.compile(r"[A-Za-zÀ-ž\u3400-\u9fff\u3040-\u30ff\uac00-\ud7af]{2,}")
I18N_ATTRS = {"data-i18n", "data-i18n-html", "data-i18n-placeholder", "data-i18n-title", "data-i18n-aria-label", "data-i18n-alt"}
JS_UI_PATTERNS = [
    ("DOM text assignment", re.compile(r"\.(?:textContent|innerText|innerHTML)\s*=\s*(['\"])([^'\"\n]{2,})\1")),
    ("dialog/alert text", re.compile(r"\b(?:alert|confirm|prompt)\s*\(\s*(['\"])([^'\"\n]{2,})\1")),
    ("placeholder/title assignment", re.compile(r"\.(?:placeholder|title|ariaLabel)\s*=\s*(['\"])([^'\"\n]{2,})\1")),
    ("DOM label attribute", re.compile(r"\.setAttribute\s*\(\s*['\"](?:aria-label|title|placeholder)['\"]\s*,\s*(['\"])([^'\"\n]{2,})\1")),
]
CSS_CONTENT_RE = re.compile(r"\bcontent\s*:\s*(['\"])(.*?)\1", re.I)

def tracked_frontend_files():
    try:
        raw = subprocess.run(["git", "ls-files", "-z"], cwd=ROOT, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout
        names = [item.decode("utf-8", "replace") for item in raw.split(b"\0") if item]
    except (OSError, subprocess.CalledProcessError):
        names = [str(p.relative_to(ROOT)) for p in ROOT.rglob("*") if p.is_file()]
    return sorted({ROOT / Path(n) for n in names if Path(n).suffix.lower() in EXTENSIONS and not any(part in SKIP_PARTS for part in Path(n).parts) and (ROOT / n).is_file()})

def changed_added_lines():
    if subprocess.run(["git", "rev-parse", "--verify", "origin/master"], cwd=ROOT, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode:
        return {}
    diff = subprocess.run(["git", "diff", "--unified=0", "origin/master...HEAD", "--", "*.html", "*.htm", "*.css", "*.js", "*.mjs", "*.cjs"], cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    result, current, new_line = defaultdict(set), None, 0
    for line in diff.stdout.splitlines():
        if line.startswith("+++ b/"):
            current = line[6:]
        elif line.startswith("@@"):
            match = re.search(r"\+(\d+)(?:,(\d+))?", line)
            if match: new_line = int(match.group(1)) - 1
        elif line.startswith("+") and not line.startswith("+++"):
            new_line += 1
            if current: result[current].add(new_line)
        elif line.startswith(" "):
            new_line += 1
    return result

class HtmlAudit(HTMLParser):
    def __init__(self, path, added):
        super().__init__(convert_charrefs=True)
        self.path, self.added = path, added
        self.stack, self.ids, self.errors, self.findings, self.local_refs, self.labels = [], set(), [], [], [], set()
    def handle_starttag(self, tag, attrs):
        values = {k: (v or "") for k, v in attrs}
        line = self.getpos()[0]
        if tag == "label" and values.get("for"): self.labels.add(values["for"])
        element_id = values.get("id")
        if element_id:
            if element_id in self.ids: self.errors.append(f"{self.path}:{line}: duplicate id '{element_id}'")
            self.ids.add(element_id)
        inherited = set(self.stack[-1][1]) if self.stack else set()
        inherited.update(k for k in values if k in I18N_ATTRS)
        for attr in ("src", "href"):
            value = values.get(attr, "").strip()
            if value and not value.startswith(("#", "https://", "http://", "data:", "mailto:", "tel:", "javascript:")):
                cleaned = value.split("?", 1)[0].split("#", 1)[0]
                if cleaned: self.local_refs.append((line, cleaned))
        for attr in TEXT_ATTRS:
            value = values.get(attr, "").strip()
            if value and WORD_RE.search(value) and not inherited and not any(k.startswith("data-i18n-") for k in values):
                self.findings.append((line, attr, value, line in self.added))
        if tag not in VOID_TAGS: self.stack.append((tag, inherited))
    def handle_startendtag(self, tag, attrs):
        self.handle_starttag(tag, attrs)
        if tag not in VOID_TAGS and self.stack: self.stack.pop()
    def handle_endtag(self, tag):
        for i in range(len(self.stack)-1, -1, -1):
            if self.stack[i][0] == tag:
                del self.stack[i:]
                break
    def handle_data(self, data):
        value = " ".join(data.split())
        if not value or not WORD_RE.search(value): return
        if any(tag in {"script", "style", "code", "pre"} for tag, _ in self.stack): return
        if any(attrs.intersection(I18N_ATTRS) for _, attrs in self.stack): return
        line = self.getpos()[0]
        self.findings.append((line, "text node", value[:140], line in self.added))

def check_css(path, source):
    errors, depth, quote, escaped, comment, line = [], 0, None, False, False, 1
    for i, char in enumerate(source):
        nxt = source[i+1] if i+1 < len(source) else ""
        if char == "\n": line += 1
        if comment:
            if char == "*" and nxt == "/": comment = False
            continue
        if quote:
            if escaped: escaped = False
            elif char == "\\": escaped = True
            elif char == quote: quote = None
            continue
        if char == "/" and nxt == "*": comment = True
        elif char in ("'", '"'): quote = char
        elif char == "{": depth += 1
        elif char == "}":
            depth -= 1
            if depth < 0:
                errors.append(f"{path}:{line}: unexpected closing CSS brace")
                depth = 0
    if quote: errors.append(f"{path}: unterminated CSS string")
    if comment: errors.append(f"{path}: unterminated CSS comment")
    if depth: errors.append(f"{path}: {depth} unclosed CSS brace(s)")
    for m in CSS_CONTENT_RE.finditer(source):
        value = m.group(2).strip()
        if WORD_RE.search(value):
            line_no = source.count("\n", 0, m.start()) + 1
            errors.append(f"{path}:{line_no}: CSS generated content may contain hardcoded UI text: {value[:100]}")
    return errors

def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser()
    parser.add_argument("--report-file", help="Write full audit report to this path")
    args = parser.parse_args()
    files, added = tracked_frontend_files(), changed_added_lines()
    structural_errors, text_findings, checked = [], [], defaultdict(int)
    for path in files:
        relative = path.relative_to(ROOT).as_posix()
        try: source = path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            structural_errors.append(f"{relative}: cannot read UTF-8 source ({exc})")
            continue
        ext = path.suffix.lower()
        checked[ext] += 1
        if ext in {".html", ".htm"}:
            audit = HtmlAudit(relative, added.get(relative, set()))
            try: audit.feed(source); audit.close()
            except Exception as exc: structural_errors.append(f"{relative}: HTML parser error: {exc}")
            structural_errors.extend(audit.errors)
            for line, ref in audit.local_refs:
                target = (ROOT / ref.lstrip("/")).resolve() if ref.startswith("/") else (path.parent / ref).resolve()
                try: target.relative_to(ROOT)
                except ValueError:
                    structural_errors.append(f"{relative}:{line}: local reference escapes repository: {ref}")
                    continue
                if not target.exists(): structural_errors.append(f"{relative}:{line}: missing local asset reference: {ref}")
            text_findings.extend((relative, line, kind, value, added_line) for line, kind, value, added_line in audit.findings)
            for m in re.finditer(r"<(input|select|textarea)\b([^>]*)>", source, re.I):
                attrs = m.group(2)
                if re.search(r"\b(?:aria-label|aria-labelledby)\s*=", attrs, re.I): continue
                idm = re.search(r"\bid\s*=\s*['\"]([^'\"]+)", attrs, re.I)
                if idm and idm.group(1) in audit.labels: continue
                if re.search(r"\btype\s*=\s*['\"]hidden", attrs, re.I): continue
                line = source.count("\n", 0, m.start()) + 1
                text_findings.append((relative, line, "accessibility: unlabeled form control", m.group(1), line in added.get(relative, set())))
        elif ext == ".css":
            structural_errors.extend(check_css(relative, source))
            for m in CSS_CONTENT_RE.finditer(source):
                value = m.group(2).strip()
                if WORD_RE.search(value):
                    line = source.count("\n", 0, m.start()) + 1
                    text_findings.append((relative, line, "CSS content", value[:140], line in added.get(relative, set())))
        elif ext in {".js", ".mjs", ".cjs"}:
            for kind, pattern in JS_UI_PATTERNS:
                for m in pattern.finditer(source):
                    value = m.group(2).strip()
                    if WORD_RE.search(value):
                        line = source.count("\n", 0, m.start()) + 1
                        text_findings.append((relative, line, kind, value[:140], line in added.get(relative, set())))
    text_findings = list(dict.fromkeys(text_findings))
    report = [
        "Repository-wide frontend audit",
        f"Scanned {len(files)} tracked HTML/CSS/JS files: " + ", ".join(f"{checked[e]} {e}" for e in sorted(checked)),
        f"Structural errors: {len(structural_errors)}",
        f"Possible hardcoded UI strings: {len(text_findings)} (reported for review; legacy findings do not fail CI)",
        "", "STRUCTURAL ERRORS", *(structural_errors or ["None"]), "",
        "POSSIBLE HARDCODED UI STRINGS (review; use localization keys/data-i18n for user-facing copy)",
        *(f"{p}:{line}: {kind}: {value}" + (" [added line]" if added_line else "") for p, line, kind, value, added_line in text_findings),
    ]
    output = "\n".join(report) + "\n"
    print(output)
    if args.report_file:
        report_path = ROOT / args.report_file
        report_path.parent.mkdir(parents=True, exist_ok=True)
        report_path.write_text(output, encoding="utf-8")
    return 1 if structural_errors else 0

if __name__ == "__main__":
    raise SystemExit(main())
