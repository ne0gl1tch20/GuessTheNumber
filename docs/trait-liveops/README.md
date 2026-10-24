# Trait LiveOps catalogs and packages

Traits are organized into **separate catalog folders**, with each trait living in its own folder. A trait package may mix supported formats; it does not have to use every format.

## Repository layout

The built-in catalog directory is `app/src/main/assets/traits/catalogs/`. The recommended catalog groups are:

```text
assets/traits/catalogs/
├── core/
│   └── 100ConsecutiveGuessesTrait/
├── gameplay/
│   └── PrecisionGuessTrait/
├── challenges/
│   └── NoHintChallengeTrait/
├── exploration/
│   └── NebulaFinderTrait/
├── seasonal/
│   └── AutumnEchoTrait/
├── collection/
│   └── RareDiscoveryTrait/
├── experimental/
│   └── PrototypeTrait/
└── community/
    └── CommunitySpotlightTrait/
```

Each package may contain a `trait.json`, Lua logic, JavaScript presentation, CSS, HTML templates, SVG/PNG/WebP icons, fonts, and other explicitly supported assets. These are examples of the layout, not a promise that every example trait is implemented.

## Signed manifest

The HTTPS manifest is a JSON envelope:

```json
{
  "payloadBase64": "<base64 of exact UTF-8 payload bytes>",
  "payloadSha256": "<SHA-256 hex of those exact bytes>",
  "signatureBase64": "<RSA/SHA-256 signature over those exact bytes>"
}
```

The decoded payload includes compatibility and catalog version metadata. Each trait includes a `catalogId`, a `folderName` and a list of files. File paths must start with `assets/traits/catalogs/{catalogId}/{folderName}/`; paths cannot escape their trait package. Every file entry declares its SHA-256 hash and kind.

Example trait entry:

```json
{
  "id": "nebula_finder",
  "titleKey": "trait.nebula_finder.title",
  "descriptionKey": "trait.nebula_finder.description",
  "category": "COLLECTION",
  "catalogId": "exploration",
  "folderName": "NebulaFinderTrait",
  "effects": ["COSMETIC_BADGE"],
  "files": [
    {
      "path": "assets/traits/catalogs/exploration/NebulaFinderTrait/logic.lua",
      "sha256": "<64-character SHA-256 hex>",
      "kind": "lua"
    },
    {
      "path": "assets/traits/catalogs/exploration/NebulaFinderTrait/presentation.js",
      "sha256": "<64-character SHA-256 hex>",
      "kind": "js"
    },
    {
      "path": "assets/traits/catalogs/exploration/NebulaFinderTrait/theme.css",
      "sha256": "<64-character SHA-256 hex>",
      "kind": "css"
    },
    {
      "path": "assets/traits/catalogs/exploration/NebulaFinderTrait/icon.svg",
      "sha256": "<64-character SHA-256 hex>",
      "kind": "svg"
    }
  ]
}
```

Supported package kinds: `lua`, `json`, `js`, `css`, `html`, `svg`, `txt`, `png`, `webp`, `ttf` and `woff2`. Lua files pass through the existing Lua sandbox validator. JSON is parsed before staging. SVG rejects obvious active-content patterns. Other text assets are checked for non-empty UTF-8 content and every asset is hash-verified. **Accepting a file format does not mean the runtime is allowed to execute it**: JavaScript/Lua execution must use a restricted host API and cannot directly write saves or award currency.

## Safety and offline behavior

- HTTPS only, no automatic redirects.
- RSA signature and SHA-256 checks before package activation.
- Per-file hashes, strict catalog/trait path boundaries and size/count limits.
- Staging and verification before the active catalog pointer changes.
- Built-in IDs cannot be replaced by remote traits.
- Kotlin remains authoritative for save state, game rules, trait activation, validation and rewards.
- Failed updates leave the built-in offline catalog usable.
- Keep the private signing key offline and never commit it.
- Catalogs should provide optional variety, not FOMO timers, forced check-ins, streak-loss punishment or guilt-based notifications.

## Signing and publishing

1. Keep the RSA private key offline.
2. Generate the payload JSON and sign its exact bytes:

   ```sh
   openssl dgst -sha256 -sign trait-liveops-private.pem -out signature.bin payload.json
   openssl pkey -in trait-liveops-private.pem -pubout -outform DER | base64 -w0 > public-key.der.b64
   base64 -w0 payload.json > payload.b64
   sha256sum payload.json
   base64 -w0 signature.bin > signature.b64
   ```

3. Publish the envelope and the package files at the paths listed in the manifest over HTTPS.
4. Configure `traitLiveOpsManifestUrl` and `traitLiveOpsPublicKeyBase64` Gradle properties. An empty public key intentionally disables remote catalog updates.
5. Increase `catalogVersion` for every release. Check app/host compatibility and run Android, Windows and HTML tests before shipping.

The current loader stages mixed-format package files, but the presence of a JavaScript or HTML file does not automatically grant it access to the Kotlin bridge. Runtime integration must preserve a capability allowlist and keep authoritative game operations in Kotlin.
