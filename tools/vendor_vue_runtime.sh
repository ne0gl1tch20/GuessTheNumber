#!/usr/bin/env bash
set -euo pipefail

# Keep the Vue runtime version pinned so local WebView builds never fetch it.
VERSION="3.5.22"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/libraries/vue"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$TMP/package" "$DEST"
npm pack "vue@${VERSION}" --pack-destination "$TMP" >/dev/null
tar -xzf "$TMP/vue-${VERSION}.tgz" -C "$TMP/package"

PACKAGE="$TMP/package/package"
node -e 'const p=require(process.argv[1]); if(p.version!==process.argv[2]) { console.error("Unexpected Vue version: "+p.version); process.exit(1); }' "$PACKAGE/package.json" "$VERSION"
test -s "$PACKAGE/dist/vue.global.prod.js"
grep -qi "vue v${VERSION}" "$PACKAGE/dist/vue.global.prod.js"
test -s "$PACKAGE/LICENSE"

cp "$PACKAGE/dist/vue.global.prod.js" "$DEST/vue.global.prod.js"
cp "$PACKAGE/LICENSE" "$DEST/LICENSE"
printf 'Vue %s (global production build)\n' "$VERSION" > "$DEST/VERSION.txt"
echo "Vendored Vue ${VERSION} into app/src/main/assets/libraries/vue/"
