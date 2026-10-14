# LuaJ runtime compatibility: Android + Windows

## Short answer

LuaJ is a Java/JVM-based Lua interpreter, so sharing its *runtime* between Android and Windows is more realistic than sharing it in Kotlin Multiplatform `commonMain`. Keep the interpreter behind a JVM/platform boundary first. Do not assume the current `org.luaj:luaj-jme:3.0.1` artifact can simply be moved into common code or that every runtime feature behaves identically on both targets.

The upstream LuaJ project describes itself as a Java-centric interpreter intended for JVM-family environments and documents separate JSE/JME platform setups. That supports investigating Android and desktop JVM use, but the exact artifact, API calls, sandbox configuration, and Android runtime compatibility still need a small proof of concept.

References:
- LuaJ project and platform notes: https://github.com/luaj/luaj
- Kotlin Multiplatform platform-specific APIs: https://kotlinlang.org/docs/multiplatform/multiplatform-connect-to-apis.html

## Proposed architecture

- **Common game core:** define only the platform-neutral activity request/result types and validated host API contracts where practical.
- **JVM runtime adapter:** keep LuaJ initialization, interpreter objects, class loading, Java-specific helpers, and execution budget enforcement out of `commonMain`.
- **Android host:** preserve the existing runtime and its sandbox while compatibility is being evaluated.
- **Windows host:** implement the same host contract only after a desktop JVM smoke test proves the selected LuaJ artifact works.
- **Authoritative game state:** Kotlin remains authoritative for saves, rewards, progression, purchases, and unlocks. Lua scripts must not write save files or grant rewards directly.

## Required proof of concept

1. Identify every LuaJ class and artifact used by the existing implementation.
2. Run a bundled, harmless script on Android using the existing runtime.
3. Run the same script on a Windows/JVM desktop target.
4. Verify script/resource loading works from packaged app resources rather than an assumed working directory.
5. Verify error reporting, execution time/instruction limits, and memory behavior.
6. Verify the host API rejects unknown commands and invalid payloads on both targets.
7. Verify no Java bridge, reflection, filesystem, OS, network, unrestricted package loader, or coroutine access is exposed to scripts.
8. Verify signed LiveOps validation remains fail-closed and uses the same canonical content/hash rules.

## Security and compatibility rules

- Do not replace `luaj-jme` with `luaj-jse` or another fork in the same PR as the first KMP module setup.
- Do not enable LuaJ's Java integration merely because desktop is a JVM target.
- Do not use a working-directory-based script loader for desktop.
- Keep instruction/time limits and strict activity payload validation in place.
- Missing scripts or optional Lua activity content must fail gracefully without crashing the game.
- Do not mark Lua as shared/validated until both packaged targets pass the same conformance cases.

## Decision gate

Prefer a single JVM runtime implementation for Android and Windows only if the proof of concept validates the exact library artifact and security constraints. Otherwise keep platform-specific runtime adapters implementing the same narrow interface. Neither option requires the game engine itself to depend on LuaJ.
