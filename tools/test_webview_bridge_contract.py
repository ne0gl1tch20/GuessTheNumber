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
    require(web, "queuedNativePopupRoute=null", "native popup routes must have a pending-request queue")
    require(web, "function flushQueuedNativePopupRoute()", "queued popup routes must flush after the active bridge request")
    require(web, "window.setTimeout(flushQueuedNativePopupRoute,0)", "queued popup routes must retry after replies and timeouts")
    if "document.querySelectorAll('[data-screen-target]').forEach(button=>button.addEventListener" in web:
        raise AssertionError("Global data-screen-target listener duplicates the scoped handlers and can override popup routes.")

    require(web, "send('read_onboarding_state')", "shared HTML must perform its initial bridge handshake")
    require(web, "pendingTimeout=window.setTimeout", "shared HTML must unlock a request when the host stops replying")
    require(web, "host.cancelMessage?.(id)", "shared HTML must cancel timed-out messages still waiting in the desktop queue")
    require(web, "let queuedScreenStateRead=false;", "screen state reads must queue behind an active bridge request")
    require(web, "if(pending)queuedScreenStateRead=true;else send('read_state');", "opening Beyond the Numbers or Stats during another request must not lose its state refresh")
    require(web, "window.setTimeout(flushQueuedScreenStateRead,0)", "queued screen state reads must retry after the active request completes")
    require(web, "if(m.type==='state'||completedType==='read_state')queuedScreenStateRead=false;", "a returned state snapshot must clear the queued screen refresh and prevent duplicate request loops")
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

    # Desktop Settings must stay on the canonical HTML/JarrlyyUI surface.
    desktop_fx_host = read("desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopSharedWebView.kt")
    desktop_edge_host = read("desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopEdgeWebViewController.kt")
    desktop_sections = read("desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopGameSection.kt")
    desktop_save_slots = read("desktopApp/src/jvmMain/kotlin/com/jarrlyyy/guessthenumber/desktop/DesktopSaveSlotsContent.kt")
    require(desktop_sections, 'DesktopGameSection.SETTINGS -> "settings"', "Windows Settings must route to the shared HTML settings screen")
    require(desktop_save_slots, "onEditProfile = if (!slot.isEmpty && !slot.isCorrupt)", "Windows shared Save Slot cards must expose profile editing for valid saves")
    require(desktop_save_slots, "onReset = if (!slot.isEmpty && !slot.isCorrupt)", "Windows shared Save Slot cards must expose reset for valid saves")
    require(desktop_save_slots, "SaveProfileRules.isValidIcon(profileIconDraft)", "Windows profile editing must validate icons with the shared rule set")
    require(desktop_game, "onEditProfile = { target, name, iconId ->", "Windows profile editing must persist through the desktop save store")
    require(desktop_game, "saveStore.save(target, updated)", "Windows profile edits must use validated save persistence")
    require(desktop_game, "onResetSlot = { target ->", "Windows save reset must be wired to desktop persistence")
    require(desktop_game, "saveStore.reset(target, fresh)", "Windows reset must clear the old recovery backup and persist a fresh state")
    require(desktop_game, "if (section.sharedHtmlScreen != null)", "shared HTML destinations must be rendered before native fallbacks")
    require(desktop_game, "DesktopSharedWebView(", "Windows Settings must render through the shared WebView")
    require(desktop_adapter, 'class="setting-row jui-switch desktop-startup-setting"', "Windows startup toggle must reuse the shared JarrlyyUI switch component")
    require(desktop_adapter, 'class="jui-switch-track" aria-hidden="true"', "Windows startup toggle must include the SDK switch track required by its CSS")
    require(desktop_adapter, 'aria-label="Launch GuessTheNumber when Windows starts"', "Windows startup toggle must have an accessible name")
    require(desktop_adapter, "class=\"jui-field\"", "Windows navigation position must reuse the shared JarrlyyUI field")

    # The developer overlay is available in both package types, but JavaScript inspection stays debug-only.
    require(desktop_adapter, "function installDebugOverlay()", "desktop developer overlay installer must exist")
    require(desktop_adapter, "if (document.getElementById('gtnDesktopDebugRoot')) return;", "overlay availability must not depend on debug-tools permission")
    require(desktop_adapter, "var canInspect = window.gtnDesktopDebugTools === true;", "inspection controls must be gated by the debug-tools flag")
    require(desktop_adapter, "if (!canInspect || !window.gtnDesktopDebugTools) return;", "release builds must not execute developer-supplied JavaScript")
    require(desktop_adapter, "Read-only desktop diagnostics.", "release builds must retain a read-only developer overlay")
    require(desktop_host, "browser.setDefaultContextMenuEnabled(true)", "Windows WebView2 context menu must be enabled in every build")
    require(desktop_fx_host, "webView.isContextMenuEnabled = true", "JavaFX WebView context menu must be enabled")
    require(desktop_host, "browser.setDebug(debugToolsEnabled)", "WebView2 Inspect/DevTools must remain build-gated")

    # Tutorial completion must always receive a matching response to release the shared bridge request.
    require(web, "send('complete_feature_tutorial',{featureId:id})", "shared HTML must persist completed feature tutorials")
    require(web, "if(m.type==='feature_tutorial_state')", "shared HTML must consume feature tutorial completion replies")
    require(desktop_game, 'if (type == "read_feature_tutorial" || type == "complete_feature_tutorial")', "Windows must handle tutorial reads and completion")
    require(desktop_game, '.put("type", "feature_tutorial_state")', "Windows must reply to tutorial completion and unlock the HTML bridge")

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
