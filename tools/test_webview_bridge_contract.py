#!/usr/bin/env python3
"""Cross-platform contract checks for the Android and Windows WebView bridge."""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.is_file():
        raise AssertionError(f"Required bridge source is missing: {relative}")
    return path.read_text(encoding="utf-8")


def require(source: str, pattern: str, description: str) -> None:
    if re.search(pattern, source, re.MULTILINE | re.DOTALL) is None:
        raise AssertionError(f"Bridge contract check failed: {description}")


def main() -> int:
    web = read("app/src/main/assets/web-play/app.js")
    android_host = read(
        "app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ProductionWebPlayScreen.kt"
    )
    android_protocol = read(
        "app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/WebUiBridgeProtocol.kt"
    )
    android_tests = read(
        "app/src/test/java/com/jarrlyyy/guessthenumber/ui/screens/WebUiBridgeProtocolTest.kt"
    )
    desktop_host = read(
        "desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopEdgeWebViewController.kt"
    )
    desktop_payload = read(
        "desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopBridgePayload.kt"
    )
    desktop_adapter = read("desktopApp/src/jvmMain/resources/web-play/desktop-host.js")
    desktop_tests = read(
        "desktopApp/src/jvmTest/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopBridgePayloadTest.kt"
    )

    # Shared frontend emits one versioned JSON-string envelope on both hosts.
    require(web, r"JSON\\.stringify\\(\\{\\s*version\\s*:\s*1\\s*,\\s*id\\s*,\\s*type", "shared HTML must serialize versioned requests")
    require(web, r"host\\.onmessage\\s*=\\s*event\\s*=>\\s*receive\\(event\\.data\\)", "shared HTML must receive host replies")
    require(web, r"send\\('read_onboarding_state'\\)", "shared HTML must perform its initial bridge handshake")

    # Android accepts only the expected local page/origin and parses raw message data.
    require(android_host, r"addWebMessageListener\\(this,\\s*\"gtnHost\",\\s*setOf\\(WEB_UI_ORIGIN\\)", "Android listener must be origin-scoped")
    require(android_host, r"isAllowedSource\\(isMainFrame,\\s*sourceOrigin,\\s*view\\.url", "Android must validate origin, frame and page")
    require(android_host, r"WebUiBridgeProtocol\\.parse\\(message\\.data\\s*\\?\\?\\s*\"\"\\)", "Android must parse the raw WebMessage payload")
    require(android_protocol, r"MAX_MESSAGE_LENGTH\\s*=\\s*2048", "Android request size must stay bounded")
    require(android_protocol, r"rawVersion\\s*!is\\s*Int\\s*\\|\\|\\s*rawVersion\\s*!=\\s*1", "Android must reject invalid protocol versions")
    require(android_protocol, r"validId.*rawType", "Android must validate request IDs and action types")
    require(android_tests, r"@Test", "Android protocol regression tests must exist")
    require(android_tests, r"invalid_json|invalid_request|message_too_large|origin_not_allowed", "Android tests must cover malformed, invalid, oversized and origin-rejected requests")

    # Windows callback wrappers may return raw JSON or a JSON-encoded string.
    require(desktop_adapter, r"postMessage:\\s*function\\s*\\(raw\\)", "Windows adapter must expose postMessage")
    require(desktop_adapter, r"JSON\\.stringify\\(\\{\\s*version\\s*:\s*1", "Windows-only settings requests must use the shared envelope")
    require(desktop_host, r"DesktopBridgePayload\\.normalize\\(raw\\)", "Windows native host must normalize callback payloads")
    require(desktop_payload, r"MAX_CALLBACK_LENGTH|MAX_REQUEST_LENGTH", "Windows callback and decoded request sizes must be bounded")
    require(desktop_payload, r"isSingleJsonObject", "Windows must validate a complete JSON object")
    require(desktop_payload, r"decodeSingleJsonString", "Windows must safely decode JSON-string callback payloads")
    require(desktop_tests, r"decodesJsonEncodedStringPayload", "Windows tests must cover encoded callback payloads")
    require(desktop_tests, r"rejectsTrailingGarbageAfterObject|rejectsTrailingGarbageAfterEncodedString", "Windows tests must reject trailing garbage")
    require(desktop_tests, r"rejectsOversizedCallbackBeforeParsing|rejectsOversizedDecodedRequest", "Windows tests must cover payload limits")

    # Both hosts return the same versioned reply shape consumed by the page.
    require(android_host, r"put\\(\"version\",\\s*1\\).*put\\(\"id\",\\s*parsed\\.id\\).*put\\(\"type\",\\s*\"state\"", "Android replies must retain version/id/type")
    require(desktop_host, r"put\\(\"version\",\\s*1\\).*put\\(\"id\",\\s*request", "Windows replies must retain version/id")
    require(desktop_adapter, r"messageHandler\\(\\{\\s*data:\\s*message\\s*\\}\\)", "Windows replies must reach the same event.data receiver")

    print("WebView bridge contract checks passed.")
    print("- Shared HTML request/reply envelope")
    print("- Android origin, page, schema and size validation")
    print("- Windows callback normalization and payload limits")
    print("- Android and Windows regression-test coverage")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as error:
        print(str(error), file=sys.stderr)
        raise SystemExit(1)
