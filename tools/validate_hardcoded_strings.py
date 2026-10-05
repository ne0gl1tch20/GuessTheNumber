#!/usr/bin/env python3
"""Audit likely hardcoded user-visible Compose strings."""
from __future__ import annotations
import argparse,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app"/"src"/"main"/"java"
PATTERNS=[
 re.compile(r'\bText\s*\(\s*"([^"\\]{2,})"'),
 re.compile(r'\bToast\.makeText\s*\([^,]+,\s*"([^"\\]{2,})"'),
 re.compile(r'\bcontentDescription\s*=\s*"([^"\\]{2,})"')]
def main()->int:
    parser=argparse.ArgumentParser(); parser.add_argument("--strict",action="store_true"); args=parser.parse_args()
    findings=[]
    for path in SRC.rglob("*.kt"):
        rel=str(path.relative_to(ROOT)).replace("\\","/")
        for n,line in enumerate(path.read_text(encoding="utf-8").splitlines(),1):
            for pattern in PATTERNS:
                for m in pattern.finditer(line):
                    value=m.group(1).strip()
                    if value and value!="•" and not value.startswith(("http://","https://","file://")):
                        findings.append(f"{rel}:{n}: {value}")
    print(f"Hardcoded-string audit: {len(findings)} candidate(s).")
    for f in findings: print(f"  - {f}")
    if args.strict and findings: return 1
    return 0
if __name__=="__main__": sys.exit(main())
