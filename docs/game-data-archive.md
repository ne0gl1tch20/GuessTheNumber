# Game data archive

Android builds generate `app/src/main/assets/game_data/` with a manifest and numbered encrypted chunks:

```text
game_data/
  manifest.json
  01.bin
  02.bin
  ...
```

The builder groups files by their original asset subsystem (for example `game/`, `locales/`, `web-play/`, `libraries/`, `fonts/`, `scripts/`, and `traits/`). Large groups are split into bounded chunks. The manifest maps every original relative path to its chunk and SHA-256 digest.

## Build and test locally

Install the one build dependency:

```bash
python -m pip install -r tools/requirements-game-data.txt
python tools/binarify_game_data.py --output /tmp/gtn-game-data
python -m unittest discover -s tools -p 'test_binarify_game_data.py' -v
```

Android Gradle builds invoke the same builder automatically before asset merging.

## Runtime behavior

At startup, while the existing save-loading screen is visible, Android:
1. Reads and validates the APK-bundled manifest.
2. Verifies each chunk hash and AES-GCM authentication tag.
3. Decompresses into an app-private staging directory while enforcing path and size limits.
4. Verifies every extracted file hash.
5. Atomically activates the new cache version.
6. Checks the published `master/game_data/manifest.json` and downloads changed chunks when available.

If a download or archive validation fails, the last verified cache remains active and the app falls back to its bundled assets. Save data is stored separately and is not touched.

## Security and migration

AES-256-GCM authenticates each encrypted chunk and ZIP-DEFLATE compresses its contents. The current key is embedded in the client, so this is asset obfuscation and integrity protection, **not** a way to make assets impossible to extract. A determined reverse engineer can recover client-side key material.

Plaintext assets are intentionally retained in the APK during this migration because existing Kotlin, Lua, audio, and other consumers still read them directly. Do not remove those source assets until each consumer has been migrated and tested. The Android implementation currently uses the platform AES-GCM provider; `libgtn.so` is renamed for the native runtime but native C++ decryption is a separate follow-up.
