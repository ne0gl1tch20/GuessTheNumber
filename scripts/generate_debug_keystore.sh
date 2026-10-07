#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/debug-signing/debug.keystore"

mkdir -p "$(dirname "$OUT")"

if [[ -f "$OUT" ]]; then
  echo "Debug keystore already exists: $OUT"
  exit 0
fi

keytool -genkeypair \
  -v \
  -keystore "$OUT" \
  -storepass android \
  -keypass android \
  -alias guessthenumber-debug \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=GuessTheNumber Debug,O=ne0gl1tch20,OU=GuessTheNumber,C=PH"

echo
echo "Created stable project debug keystore:"
echo "  $OUT"
echo "Do not delete it if you want debug APKs to remain update-compatible."
