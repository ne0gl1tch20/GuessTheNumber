# Game data archive

Android APK builds package the generated archive only:

```text
assets/game_data/
  manifest.bin
  01.bin
  02.bin
  ...
```

The APK-only manifest is authenticated and encrypted with AES-256-GCM. Original source assets remain ordinary files in the repository. The archive builder encrypts all game-data files (including JSON, Lua, HTML, CSS, JavaScript, and other assets) into independently authenticated ZIP chunks before APK packaging, so their source contents are not stored as plaintext APK assets.

## Build and test locally

Install the one build dependency:

```bash
python -m pip install -r tools/requirements-game-data.txt
python tools/binarify_game_data.py --output /tmp/gtn-game-data
python -m unittest discover -s tools -p 'test_binarify_game_data.py' -v
```

The default builder mode writes a plaintext `manifest.json` for the archive publishing workflow. Android Gradle builds pass `--encrypt-manifest`, which writes `manifest.bin` instead. The GitHub-hosted `game_data/manifest.json` remains plaintext so existing remote version discovery and update checks continue working.

## Runtime behavior

At startup, while the existing save-loading screen is visible, Android:
1. Reads and decrypts the APK-bundled `manifest.bin`; remote sync continues to parse the published plaintext JSON manifest.
2. Verifies each chunk hash and AES-GCM authentication tag.
3. Decompresses into an app-private staging directory while enforcing path and size limits.
4. Verifies every extracted file hash.
5. Atomically activates the new cache version.
6. Checks the published `master/game_data/manifest.json` and downloads changed chunks when available. Source commit timestamps prevent an older published bundle from replacing newer APK assets or a newer verified cache.

The decrypted manifest is kept in app-private cache storage because the runtime needs its metadata. Extracted game-data files are also readable by the app in its private storage after decryption; this change protects the APK contents, not runtime memory or private app storage against a determined device owner.

If a download or archive validation fails, the last verified cache remains active and the app falls back to its bundled assets. Save data is stored separately and is not touched.

## Security and migration

AES-256-GCM authenticates each encrypted chunk and the APK-only manifest. The key is embedded in the client, so this is asset obfuscation and integrity protection, **not** a way to make assets impossible to extract. A determined reverse engineer can recover client-side key material and decrypt the assets.

The APK validator rejects a plaintext `manifest.json`, requires `manifest.bin`, decrypts it, and verifies every chunk and extracted file digest. Remote publishing intentionally keeps its JSON manifest unchanged for backwards compatibility.
