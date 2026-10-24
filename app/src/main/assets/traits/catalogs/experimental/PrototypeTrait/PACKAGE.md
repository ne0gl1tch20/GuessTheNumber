# PrototypeTrait

Catalog: `experimental`

An isolated test trait requiring explicit compatibility checks before activation.

A production package may contain `trait.json`, `logic.lua`, `presentation.js`, `styles.css`, icons and other supported assets. Declare each file and its SHA-256 in the signed catalog manifest. Script entry points are not automatically executed just because they are present; any runtime must use a restricted API and Kotlin must validate all authoritative actions.
