# Jarrlyy DSP Engine

The native library libjarrlyyydsp.so is a reusable Android procedural-audio engine. It synthesizes UI and arcade sounds from oscillator patterns instead of requiring a separate file for every sound.

## API
- Public C API header (C/C++): app/src/main/cpp/jarrlyy_dsp.h
- Internal C++ engine header: app/src/main/cpp/jarrlyy_dsp_engine.h
- C API: jarrlyy_dsp_play_preset, jarrlyy_dsp_stop_all, jarrlyy_dsp_is_supported
- Kotlin facade: com.jarrlyyy.guessthenumber.audio.JarrlyyDspEngine

Stable preset IDs: 0 tap, 1 confirm, 2 back, 3 error, 4 reward, 5 arcade, 6 guess-too-low, 7 guess-too-high.

Effect flags: low-pass=1, reverb=2, chorus=4, distortion=8. Volume is clamped to 0.0–0.45.

## Runtime
- Uses Android AAudio with float stereo output and requests low-latency mode.
- Opens audio lazily on the first play request, not when Developer Settings opens.
- Uses a fixed voice pool and bounded command queue. The audio callback does not allocate or perform file I/O.
- Returns the callback to STOP after silence to avoid continuously rendering silent audio.
- No permissions, network requests, audio assets, saves, or rewards are required.
- Android-native only; other platforms need a platform adapter.

## Developer tests
Debug Developer Settings contains eight generated presets, volume control, and low-pass, reverb, chorus, and distortion toggles. Test buttons only produce audio and do not touch game state. Speaker output and device-specific AAudio behavior still require manual device verification.
