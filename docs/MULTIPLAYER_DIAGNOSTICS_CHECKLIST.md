# Multiplayer Diagnostics and Two-Device Regression Checklist

**Status:** Planning checklist for the local multiplayer workstream. Do not mark a scenario as passed until it has actually been exercised. Simulations and protocol unit tests do not replace testing on two physical Android devices.

## Scope

The intended first multiplayer release is opt-in local play. Prefer a supported Wi-Fi/local-network transport and offer Bluetooth fallback when Wi-Fi is unavailable or unusable and both devices support the selected method. There is no internet matchmaking, public rooms, accounts, cloud relay, or requirement to be online.

This checklist prepares the diagnostics and validation work. Runtime diagnostics must be connected to the real transport/session implementation once that implementation exists; do not add fake status values or pretend a simulated connection is a live connection.

## Debug-only diagnostics requirements

- [ ] Show the selected transport (Wi-Fi/local network or Bluetooth) and current connection/session state.
- [ ] Show protocol version compatibility and a sanitized disconnect/error reason.
- [ ] Summarize message-flow events such as connect, handshake, accepted/rejected message type, timeout, reconnect, and disconnect.
- [ ] Never log guesses, secret target values, private payload contents, authentication material, device identifiers, or other sensitive data.
- [ ] Keep diagnostic controls and verbose transport details out of Release builds.
- [ ] Make diagnostic events bounded in memory and clearable; avoid continuous high-volume logging.
- [ ] Separate simulated events from live transport status and label simulations clearly.
- [ ] Ensure diagnostics cannot change match results, bypass validation, grant rewards, or mutate single-player saves.

## Deterministic protocol and state tests

- [ ] Valid handshake with matching protocol versions.
- [ ] Reject an incompatible protocol version with an actionable error.
- [ ] Reject malformed, oversized, unknown-type, wrong-session, and out-of-order messages.
- [ ] Ignore or safely reject duplicate/replayed messages without resolving a round twice.
- [ ] Timeout while discovering, connecting, handshaking, and waiting for a response.
- [ ] Host exits during lobby and during a round.
- [ ] Disconnect, retry, reconnect, cancel, and leave paths clean up jobs, sockets, callbacks, and discovery resources.
- [ ] Permission denied, Bluetooth disabled/unavailable, and unsupported transport all fail gracefully.
- [ ] Rapid repeated actions cannot create duplicate sessions or duplicate score/round resolution.
- [ ] Multiplayer errors do not corrupt single-player saves or award single-player currency, achievements, or progression.

## Manual two-device regression matrix

Record both device models, Android versions, app versions, transport, result, and observed error. Do not record personal device identifiers.

| Scenario | Expected result | Result |
| --- | --- | --- |
| Both devices on the same Wi-Fi | Host and join complete the handshake and enter the lobby | Not run |
| Host device hotspot | Join succeeds when supported by both devices and Android networking behavior | Not run |
| Wi-Fi unavailable, Bluetooth supported | Player can choose/use Bluetooth fallback and sees the active transport | Not run |
| Bluetooth disabled | Clear prompt and retry path; no crash or stuck spinner | Not run |
| Bluetooth permission denied | Explain the permission needed and offer retry/cancel | Not run |
| Local-network permission/discovery unavailable | Actionable error or supported fallback; no silent hang | Not run |
| Incompatible app/protocol versions | Connection rejected before gameplay with a clear explanation | Not run |
| Host exits from lobby | Peer receives/observes session termination and returns to a usable state | Not run |
| Host exits during a round | Round does not resolve twice or grant unintended rewards | Not run |
| App backgrounds and resumes | Session remains valid or disconnects cleanly with a recovery path | Not run |
| Wi-Fi drops / Bluetooth connection drops | Timeout and reconnect/retry/cancel behavior is clear | Not run |
| Rapid repeated join/retry taps | At most one active session; no leaked resources | Not run |
| Repeated or delayed messages | No duplicate turn, score, or round resolution | Not run |
| Leave match and start another | Old session callbacks/messages cannot affect the new session | Not run |
| Single-player after multiplayer failure | Existing offline gameplay and save remain intact | Not run |

## Release evidence required

- [ ] Focused deterministic protocol tests have actual results recorded.
- [ ] Wi-Fi/local-network scenarios have been completed on two physical Android devices.
- [ ] Bluetooth fallback scenarios have been completed on two physical Android devices where supported.
- [ ] Unsupported/denied-permission paths have been exercised.
- [ ] Debug diagnostics have been verified absent from Release builds.
- [ ] All rows are marked Pass, Fail, or Skipped with a reason; skipped checks are never described as passing.
- [ ] Privacy, resource cleanup, localization, accessibility, and single-player isolation are reviewed before the release gate.

**Authorization rule:** Creating this checklist does not authorize starting builds, tests, GitHub Actions, or physical-device checks. Those remain manual and require explicit approval.
