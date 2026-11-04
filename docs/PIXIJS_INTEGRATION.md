# PixiJS particle effects

GuessTheNumber uses PixiJS only for the optional celebration-particle layer. It does not replace the HTML interface, Vue component lifecycle, Kotlin gameplay authority or Jarrlyy UI SDK.

## Dependency

PixiJS is pinned to **8.22.0** and is copied into the WebView assets as a local browser bundle. The app does not load PixiJS from a CDN at runtime. The generated bundle and its MIT license are placed in:

- `app/src/main/assets/libraries/pixi/pixi.min.js`
- `app/src/main/assets/libraries/pixi/LICENSE.txt`
- `app/src/main/assets/libraries/pixi/VERSION.txt`

To prepare a local Android/Desktop build:

```sh
cd tools/pixi-vendor
npm install --no-package-lock --no-audit --no-fund
cd ../..
node tools/vendor_pixi.js
```

The normal WebView CSP remains local-only. CI vendors the pinned package before frontend validation and before the stable debug APK build.

## Rendering and fallback

- Vue owns mount/unmount lifecycle.
- PixiJS uses WebGL for the particle overlay only.
- The existing Canvas 2D renderer remains the fallback when PixiJS is unavailable or WebGL initialization fails.
- Both renderers respect reduced motion, pause/clear when the document is hidden, cap the particle count and ignore pointer input.
- The effect layer is decorative and never receives gameplay state or changes Kotlin-owned game data.

The PixiJS bundle is generated from the exact npm version in `tools/pixi-vendor/package.json`. Do not replace it with an unpinned CDN URL.
