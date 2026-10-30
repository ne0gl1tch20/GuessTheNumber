#!/usr/bin/env python3
"""Guard the KMP source-set boundary used by Android and Windows Desktop.

commonMain must remain platform-neutral. Compose Multiplatform APIs are valid
there; Android framework and JVM-only APIs are not. Android and Windows are
both JVM consumers, so JVM production models that rely on java.math belong in
jvmAndAndroidMain.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COMMON = ROOT / "shared" / "src" / "commonMain"
SHARED_BUILD = ROOT / "shared" / "build.gradle.kts"

# androidx.compose is supported by Compose Multiplatform in commonMain.
# Other AndroidX libraries are not assumed to have common targets.
FORBIDDEN_IMPORTS = re.compile(
    r"^\\s*import\\s+(?:java\\.|javax\\.|android\\.|"
    r"androidx\\.(?!compose(?:\\.|$))|com\\.android\\.|org\\.jvm\\.)",
    re.MULTILINE,
)
FORBIDDEN_REFERENCES = re.compile(
    r"\\b(?:System\\.(?:currentTimeMillis|nanoTime|out|err)|"
    r"java\\.io\\.|java\\.nio\\.|java\\.math\\.|android\\.|"
    r"androidx\\.(?!compose(?:\\.|$)))"
)


def main() -> int:
    errors: list[str] = []
    if not COMMON.is_dir():
        errors.append("Missing shared/src/commonMain source directory.")
    else:
        files = sorted(COMMON.rglob("*.kt"))
        if not files:
            errors.append("commonMain contains no Kotlin source files.")
        for path in files:
            source = path.read_text(encoding="utf-8")
            relative = path.relative_to(ROOT)
            if FORBIDDEN_IMPORTS.search(source):
                errors.append(f"{relative}: platform-specific import found.")
            if FORBIDDEN_REFERENCES.search(source):
                errors.append(f"{relative}: platform-specific API reference found.")

    if not SHARED_BUILD.is_file():
        errors.append("Missing shared/build.gradle.kts.")
    else:
        build = SHARED_BUILD.read_text(encoding="utf-8")
        required = (
            "val jvmAndAndroidMain by creating",
            "val androidMain by getting",
            "val jvmMain by getting",
            "dependsOn(jvmAndAndroidMain)",
        )
        for marker in required:
            if marker not in build:
                errors.append(
                    "shared/build.gradle.kts is missing expected source-set wiring: "
                    + marker
                )

    model = ROOT / "shared/src/jvmAndAndroidMain/kotlin/com/jarrlyyy/guessthenumber/domain/model/GameState.kt"
    if not model.is_file():
        errors.append("Shared production GameState model is missing from jvmAndAndroidMain.")

    if errors:
        print("KMP source-boundary audit failed:")
        for error in errors:
            print(f" - {error}")
        return 1

    print("KMP source-boundary audit passed.")
    print(" - commonMain contains no detected JVM/Android-only APIs.")
    print(" - Compose Multiplatform common APIs are allowed.")
    print(" - Android and Desktop JVM source sets share jvmAndAndroidMain.")
    print(" - Production GameState remains in the shared JVM/Android source set.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
