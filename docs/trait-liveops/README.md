# Trait LiveOps catalog

The app downloads **declarative trait data only**. It does not download executable Kotlin, JavaScript, Lua or scripts through this endpoint. Built-in trait IDs are reserved and cannot be replaced remotely.

## Manifest format

The HTTPS manifest is a JSON envelope:

```json
{
  "payloadBase64": "<base64 of the exact UTF-8 payload.json bytes>",
  "payloadSha256": "<lowercase SHA-256 hex of those exact bytes>",
  "signatureBase64": "<base64 RSA/SHA-256 signature over those exact bytes>"
}
```

The decoded payload follows this schema (include every required field):

```json
{
  "schemaVersion": 1,
  "hostApiVersion": 1,
  "minAppVersionCode": 21,
  "maxAppVersionCode": 21,
  "catalogVersion": 1,
  "traits": [
    {
      "id": "autumn_echo",
      "titleKey": "trait.autumn_echo.title",
      "descriptionKey": "trait.autumn_echo.description",
      "category": "SEASONAL",
      "visibility": "VISIBLE",
      "definitionVersion": 1,
      "minHostApiVersion": 1,
      "maxHostApiVersion": 1,
      "minimumLevel": 1,
      "minimumPrestige": 0,
      "minimumCorrectGuesses": 0,
      "dependencies": [],
      "conflicts": [],
      "effects": ["COSMETIC_BADGE"],
      "seasonId": "autumn"
    }
  ]
}
```

Supported categories: `PROGRESSION`, `GAMEPLAY`, `COLLECTION`, `SEASONAL`. Supported effects: `XP_MULTIPLIER`, `NEBULA_BONUS`, `STREAK_PROTECTION`, `COSMETIC_BADGE`. Seasonal traits require `seasonId`; other categories must omit it or set it to null. Dependencies and conflicts must reference a trait in the combined built-in + remote catalog. Never reuse a built-in ID.

## Signing and publishing

1. Keep the RSA private key offline. Do not commit it or put it in the app.
2. Create `payload.json` with UTF-8 JSON, then sign those exact bytes:

   ```sh
   openssl dgst -sha256 -sign trait-liveops-private.pem -out signature.bin payload.json
   openssl pkey -in trait-liveops-private.pem -pubout -outform DER | base64 -w0 > public-key.der.b64
   base64 -w0 payload.json > payload.b64
   sha256sum payload.json
   base64 -w0 signature.bin > signature.b64
   ```

3. Place the payload's base64, SHA-256 hex and signature's base64 in `manifest.json`. Serve it over HTTPS.
4. Configure Gradle properties `traitLiveOpsManifestUrl` and `traitLiveOpsPublicKeyBase64`; the public key property must contain the base64 DER public key from step 2.
5. Publish a strictly higher `catalogVersion` for every new catalog. App and host compatibility, schema, trait references, signature and hash are checked before activation.

The app rejects unsigned/invalid data, unsupported compatibility ranges, oversized catalogs, duplicate IDs and rollback attempts. Catalogs are staged and verified before the active pointer changes. If a refresh fails, the built-in offline catalog remains available. A verified previous catalog can be selected for recovery without lowering the anti-rollback high-water mark.

## Healthy engagement

Use optional seasonal themes and fresh trait combinations as invitations, not obligations. Do not add streak-loss punishment, forced check-ins, expiring rewards that create pressure, or notifications designed to guilt players into returning. The game remains playable offline.
