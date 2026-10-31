#!/usr/bin/env python3
"""Cross-platform contract checks for the Android and Windows WebView bridge."""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.is_file():
        raise AssertionError(f"Required bridge source is missing: {relative}")
    return path.read_text(encoding="utf-8")


def require(source: str, fragment: str, description: str) -> None:
    if fragment not in source:
        raise AssertionError(f"Bridge contract check failed: {description}")


def main() -> int:
    web = read("app/src/main/assets/web-play/app.js")
    android_host = read("app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ProductionWebPlayScreen.kt")
    android_protocol = read("app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/WebUiBridgeProtocol.kt")
    android_tests = read("app/src/test/java/com/jarrlyyy/guessthenumber/ui/screens/WebUiBridgeProtocolTest.kt")
    desktop_host = read("desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopEdgeWebViewController.kt")
    desktop_payload = read("desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopBridgePayload.kt")
    desktop_adapter = read("desktopApp/src/jvmMain/resources/web-play/desktop-host.js")
    desktop_tests = read("desktopApp/src/jvmTest/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopBridgePayloadTest.kt")
    desktop_game = read("desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/ProductionGuessGameScreen.kt")

    # Shared frontend emits one versioned JSON-string envelope on both hosts.
    require(web, "JSON.stringify({version:1,id,type,...payload})", "shared HTML must serialize versioned requests")
    require(web, "host.onmessage=event=>receive(event.data)", "shared HTML must receive host replies")
    require(web, "document.querySelectorAll('.screen-nav [data-screen-target]')", "main tabs must have one scoped navigation handler")
    require(web, "document.querySelectorAll('.more-links [data-screen-target]')", "More links must have one scoped navigation handler")
    require(web, "event.stopImmediatePropagation();const route=target.dataset.route", "native popup routes must be captured before bubbling handlers")
    require(web, "target.closest('.more-screen [data-route=\"player_profile\"],.more-links [data-route=\"save_slots\"]')", "Save Slots and Player Profile must use delegated native popup routing")
    require(web, "},true);", "native popup routing must run in the capture phase")
    if "document.querySelectorAll('[data-screen-target]').forEach(button=>button.addEventListener" in web:
        raise AssertionError("Global data-screen-target listener duplicates the scoped handlers and can override popup routes.")

    require(web, "send('read_onboarding_state')", "shared HTML must perform its initial bridge handshake")
    require(web, "pendingTimeout=window.setTimeout", "shared HTML must unlock a request when the host stops replying")
    require(web, "host.cancelMessage?.(id)", "shared HTML must cancel timed-out messages still waiting in the desktop queue")
    require(web, "Bridge timeout:", "shared HTML must expose a recoverable timeout diagnostic")
    require(web, "render(m.state)", "shared HTML must render complete Kotlin game snapshots")
    require(desktop_game, "fun currentNavigationPayload(message: String)", "desktop popup navigation must build a full Kotlin-backed snapshot")
    require(desktop_game, "return desktopWebPlayState(", "desktop popup acknowledgements must preserve app version and saved theme")
    for route in ('if (type == "open_arcade")', 'if (type == "navigate") {'):
        start = desktop_game.find(route)
        if start < 0:
            raise AssertionError(f"Desktop popup route is missing: {route}")
        end = desktop_game.find("return@launch", start)
        branch = desktop_game[start:end if end >= 0 else len(desktop_game)]
        require(branch, "currentNavigationPayload(", f"{route} must reply with a complete game snapshot")
        if 'put("state", org.json.JSONObject().put("accepted", true))' in branch:
            raise AssertionError(f"{route} must not send a partial state that resets the HTML UI")


    # Android accepts only the expected local page/origin and parses raw message data.
    require(android_host, 'addWebMessageListener(this, "gtnHost", setOf(WEB_UI_ORIGIN)', "Android listener must be origin-scoped")
    require(android_host, "isAllowedSource(isMainFrame, sourceOrigin, view.url", "Android must validate origin, frame and page")
    require(android_host, 'WebUiBridgeProtocol.parse(message.data ?: "")', "Android must parse the raw WebMessage payload")
    require(android_protocol, "const val MAX_MESSAGE_LENGTH = 2048", "Android request size must stay bounded")
    require(android_protocol, "rawVersion !is Int || rawVersion != 1", "Android must reject invalid protocol versions")
    require(android_protocol, 'request.opt("id")', "Android must validate request IDs")
    require(android_tests, "@Test", "Android protocol regression tests must exist")
    for case in ("invalid_json", "invalid_request", "message_too_large"):
        require(android_tests, case, f"Android tests must cover {case}")

    # Windows callback wrappers may return raw JSON or a JSON-encoded string.
    require(desktop_adapter, "postMessage: function (raw)", "Windows adapter must expose postMessage")
    require(desktop_adapter, "cancelMessage: function (id)", "Windows adapter must discard stale queued requests after a timeout")
    require(desktop_adapter, "JSON.stringify({ version: 1", "Windows-only settings requests must use the shared envelope")
    require(desktop_host, "DesktopBridgePayload.normalize(raw)", "Windows native host must normalize callback payloads")
    require(desktop_payload, "MAX_CALLBACK_LENGTH", "Windows callback size must be bounded")
    require(desktop_payload, "MAX_REQUEST_LENGTH", "Windows decoded request size must be bounded")
    require(desktop_payload, "isSingleJsonObject", "Windows must validate a complete JSON object")
    require(desktop_payload, "decodeSingleJsonString", "Windows must safely decode JSON-string callback payloads")
    require(desktop_payload, "fun recoverRequestId", "Windows must preserve a recoverable request ID when rejecting malformed callbacks")
    for case in (
        "decodesJsonEncodedStringPayload",
        "rejectsTrailingGarbageAfterObject",
        "rejectsTrailingGarbageAfterEncodedString",
        "rejectsOversizedCallbackBeforeParsing",
        "rejectsOversizedDecodedRequest",
        "recoversRequestIdFromRawJson",
        "recoversRequestIdFromJsonEncodedString",
        "rejectsInvalidOrUnrecoverableRequestIds",
    ):
        require(desktop_tests, case, f"Windows tests must cover {case}")

    # Both hosts return the versioned envelope consumed by the shared page.
    require(android_host, '.put("version", 1)', "Android replies must include protocol version")
    require(android_host, '.put("id", parsed.id)', "Android replies must preserve request ID")
    require(android_host, '.put("type", "state")', "Android replies must identify response type")
    require(desktop_host, '.put("version", 1)', "Windows replies must include protocol version")
    require(desktop_host, '.put("id", request', "Windows replies must preserve request ID")
    require(desktop_adapter, "messageHandler({ data: message })", "Windows replies must reach the same event.data receiver")

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
