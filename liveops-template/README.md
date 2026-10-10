# GuessTheNumber-LiveOps repository template

Copy this directory's contents into the separate public repository named `ne0gl1tch20/GuessTheNumber-LiveOps`. The Android client expects `manifest.json` at the repository root and scripts under `scripts/`.

## One-time setup

1. Create an RSA 3072-bit private key outside Git and keep it secret:
   `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out lua-liveops-private.pem`
2. Add the complete PEM contents as the repository Actions secret `LUA_LIVEOPS_PRIVATE_KEY_PEM`.
3. Extract the public key for the Android build:
   `openssl pkey -in lua-liveops-private.pem -pubout -outform DER | base64 -w0`
4. Configure the app build with `-PluaLiveOpsPublicKeyBase64=...`. Never commit the private key.
5. Run the **Publish signed Lua LiveOps manifest** workflow manually.

The workflow signs the exact payload bytes, updates `manifest.json`, and commits the signed envelope. The Android app verifies the RSA signature and every script hash before activating a bundle. Increment `bundleVersion` for each publication. Bundled activities remain available offline.

The initial sample is a content-only activity. Scripts may use only the `gtn.log` host function and return a bounded activity table. They cannot access Android APIs, files, the network, save data, or reward-granting operations.
