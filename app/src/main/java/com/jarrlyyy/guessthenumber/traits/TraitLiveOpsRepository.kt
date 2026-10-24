package com.jarrlyyy.guessthenumber.traits

import android.content.Context
import android.util.Base64
import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.scripting.LuaSandbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

@Serializable
internal data class SignedTraitManifest(val payloadBase64: String, val payloadSha256: String, val signatureBase64: String)
@Serializable
internal data class TraitLiveOpsPayload(
    val schemaVersion: Int, val hostApiVersion: Int, val minAppVersionCode: Int,
    val maxAppVersionCode: Int, val catalogVersion: Int, val traits: List<RemoteTraitDefinition>
)
@Serializable
internal data class RemoteTraitDefinition(
    val id: String, val titleKey: String, val descriptionKey: String, val category: String,
    val visibility: String = "VISIBLE", val definitionVersion: Int = 1,
    val minHostApiVersion: Int = 1, val maxHostApiVersion: Int = 1,
    val minimumLevel: Int = 1, val minimumPrestige: Int = 0, val minimumCorrectGuesses: Long = 0L,
    val dependencies: List<String> = emptyList(), val conflicts: List<String> = emptyList(),
    val effects: List<String>, val seasonId: String? = null,
    val folderName: String? = null,
    val files: List<RemoteTraitFile> = emptyList()
)
@Serializable
internal data class RemoteTraitFile(val path: String, val sha256: String, val kind: String)

/** Process-local snapshot. Replaced only after a verified catalog has been loaded. */
object ActiveTraitCatalog {
    @Volatile private var active: TraitRegistry = BuiltInTraits.registry()
    fun current(): TraitRegistry = active
    fun install(registry: TraitRegistry) { active = registry }
}

sealed interface TraitLiveOpsRefreshResult {
    data class Updated(val catalogVersion: Int, val traitCount: Int) : TraitLiveOpsRefreshResult
    data class Unchanged(val catalogVersion: Int, val traitCount: Int) : TraitLiveOpsRefreshResult
    data class Unavailable(val reason: String) : TraitLiveOpsRefreshResult
}

/** Signed declarative data only. Invalid or unavailable updates never disable built-in offline traits. */
class TraitLiveOpsRepository(
    context: Context,
    private val manifestUrl: String = BuildConfig.TRAIT_LIVEOPS_MANIFEST_URL,
    private val publicKeyBase64: String = BuildConfig.TRAIT_LIVEOPS_PUBLIC_KEY_BASE64
) {
    private val root = File(context.applicationContext.filesDir, "trait-liveops")
    private val activePointer = File(root, "active_catalog.txt")
    private val highestVersionFile = File(root, "highest_catalog_version.txt")
    private val json = Json { ignoreUnknownKeys = false }

    suspend fun refresh(): TraitLiveOpsRefreshResult = withContext(Dispatchers.IO) {
        try {
            require(publicKeyBase64.isNotBlank()) { "Trait catalog signing key is not configured." }
            val url = URL(manifestUrl)
            require(url.protocol == "https") { "Trait catalog updates require HTTPS." }
            val envelopeBytes = readHttps(url)
            val envelope = json.decodeFromString<SignedTraitManifest>(envelopeBytes.toString(Charsets.UTF_8))
            val payloadBytes = Base64.decode(envelope.payloadBase64, Base64.DEFAULT)
            require(sha256(payloadBytes).equals(envelope.payloadSha256, ignoreCase = true)) { "Trait catalog payload hash mismatch." }
            verifySignature(payloadBytes, envelope.signatureBase64)
            val payload = json.decodeFromString<TraitLiveOpsPayload>(payloadBytes.toString(Charsets.UTF_8))
            val definitions = validateAndMapTraitPayload(payload, BuildConfig.VERSION_CODE)
            val active = readVersion(activePointer)
            val highest = maxOf(active, readVersion(highestVersionFile))
            require(payload.catalogVersion >= maxOf(1, highest)) { "Trait catalog rollback rejected." }
            if (payload.catalogVersion == active && active == highest) {
                return@withContext TraitLiveOpsRefreshResult.Unchanged(payload.catalogVersion, definitions.size)
            }
            val staging = File(root, "catalog-" + payload.catalogVersion + ".staging")
            staging.deleteRecursively()
            check(staging.mkdirs()) { "Could not create trait catalog staging directory." }
            File(staging, "signed_manifest.json").writeBytes(envelopeBytes)
            var totalBytes = envelopeBytes.size
            var totalFiles = 0
            payload.traits.forEach { trait ->
                require(trait.files.size <= MAX_FILES_PER_TRAIT) { "Trait ${trait.id} has too many files." }
                trait.files.forEach { entry ->
                    totalFiles++
                    require(totalFiles <= MAX_TOTAL_FILES) { "Trait bundle has too many files." }
                    val relative = validateTraitAssetPath(trait, entry.path)
                    val fileUrl = URL(url, relative)
                    val bytes = readHttps(fileUrl, MAX_FILE_BYTES)
                    totalBytes += bytes.size
                    require(totalBytes <= MAX_TOTAL_BUNDLE_BYTES) { "Trait bundle exceeds total size limit." }
                    require(sha256(bytes).equals(entry.sha256, ignoreCase = true)) {
                        "Trait file hash mismatch for ${trait.id}/${relative}."
                    }
                    if (entry.kind == "lua") {
                        LuaSandbox {}.validate(trait.id, bytes.toString(Charsets.UTF_8))
                    } else {
                        require(entry.kind == "json" && relative.endsWith(".json")) {
                            "Unsupported trait file type."
                        }
                        require(bytes.toString(Charsets.UTF_8).isNotBlank()) { "Trait metadata file is empty." }
                    }
                    val output = File(staging, relative)
                    check(output.canonicalPath.startsWith(staging.canonicalPath + File.separator)) {
                        "Trait asset path escapes staging directory."
                    }
                    check(output.parentFile?.let { it.isDirectory || it.mkdirs() } != false) {
                        "Could not create trait asset directory."
                    }
                    output.writeBytes(bytes)
                }
            }
            check(verifyCatalogDirectory(staging, payload.catalogVersion).size == definitions.size) {
                "Staged catalog verification mismatch."
            }
            val finalDir = File(root, "catalog-" + payload.catalogVersion)
            finalDir.deleteRecursively()
            check(staging.renameTo(finalDir)) { "Could not activate verified trait catalog." }
            writeVersionAtomically(highestVersionFile, payload.catalogVersion)
            writeVersionAtomically(activePointer, payload.catalogVersion)
            TraitLiveOpsRefreshResult.Updated(payload.catalogVersion, definitions.size)
        } catch (error: Exception) {
            TraitLiveOpsRefreshResult.Unavailable(error.message?.take(200) ?: "Trait catalog update unavailable.")
        }
    }

    /** Built-ins remain authoritative; remote definitions cannot replace an existing trait ID. */
    fun loadRegistry(): TraitRegistry {
        val remote = runCatching {
            val version = readVersion(activePointer)
            if (version <= 0) emptyList() else verifyCatalogDirectory(File(root, "catalog-" + version), version)
        }.getOrDefault(emptyList())
        val builtIns = BuiltInTraits.definitions
        val ids = builtIns.mapTo(mutableSetOf()) { it.id }
        return runCatching { TraitRegistry(builtIns + remote.filterNot { it.id in ids }) }
            .getOrElse { BuiltInTraits.registry() }
    }

    fun rollbackToPreviousVerifiedCatalog(): Boolean {
        val current = readVersion(activePointer)
        if (current <= 0) return false
        val candidates = root.listFiles().orEmpty().mapNotNull {
            Regex("catalog-(\\d+)").matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull()
        }.filter { it < current }.sortedDescending()
        for (version in candidates) {
            if (runCatching { verifyCatalogDirectory(File(root, "catalog-" + version), version) }.isSuccess) {
                writeVersionAtomically(activePointer, version)
                writeVersionAtomically(highestVersionFile, maxOf(current, readVersion(highestVersionFile)))
                return true
            }
        }
        return false
    }

    private fun verifyCatalogDirectory(dir: File, expectedVersion: Int): List<TraitDefinition> {
        val file = File(dir, "signed_manifest.json")
        require(file.isFile && file.length() in 1..MAX_MANIFEST_BYTES.toLong()) { "Cached trait manifest missing or too large." }
        val envelope = json.decodeFromString<SignedTraitManifest>(file.readText())
        val bytes = Base64.decode(envelope.payloadBase64, Base64.DEFAULT)
        require(sha256(bytes).equals(envelope.payloadSha256, ignoreCase = true)) { "Cached trait catalog hash mismatch." }
        verifySignature(bytes, envelope.signatureBase64)
        val payload = json.decodeFromString<TraitLiveOpsPayload>(bytes.toString(Charsets.UTF_8))
        require(payload.catalogVersion == expectedVersion) { "Cached trait catalog version mismatch." }
        return validateAndMapTraitPayload(payload, BuildConfig.VERSION_CODE)
    }

    private fun verifySignature(payload: ByteArray, signatureBase64: String) {
        val keyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(key)
        verifier.update(payload)
        require(verifier.verify(Base64.decode(signatureBase64, Base64.DEFAULT))) { "Trait catalog signature is invalid." }
    }

    private fun readHttps(url: URL): ByteArray {
        require(url.protocol == "https") { "HTTPS required." }
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = false
        connection.requestMethod = "GET"
        try {
            connection.connect()
            require(connection.responseCode in 200..299) {
                "Trait manifest request failed (" + connection.responseCode + ")."
            }
            require(connection.contentLengthLong < 0 || connection.contentLengthLong <= MAX_MANIFEST_BYTES) {
                "Trait manifest exceeds size limit."
            }
            return connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= MAX_MANIFEST_BYTES) { "Trait manifest exceeds size limit." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readVersion(file: File): Int = runCatching { file.readText().trim().toInt() }.getOrDefault(0)
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
    private fun writeVersionAtomically(file: File, version: Int) {
        check(file.parentFile?.let { it.isDirectory || it.mkdirs() } != false) { "Could not create trait catalog storage." }
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(version.toString())
        check(temp.renameTo(file) || run { file.delete(); temp.renameTo(file) }) {
            "Could not persist trait catalog pointer."
        }
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val HOST_API_VERSION = 1
        const val MAX_TRAITS = 100
        const val MAX_FILES_PER_TRAIT = 24
        const val MAX_TOTAL_FILES = 500
        const val MAX_TOTAL_BUNDLE_BYTES = 8 * 1024 * 1024
        const val MAX_MANIFEST_BYTES = 256 * 1024
        const val MAX_FILE_BYTES = 128 * 1024
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 8_000
    }
}

internal fun validateAndMapTraitPayload(payload: TraitLiveOpsPayload, appVersionCode: Int): List<TraitDefinition> {
    require(payload.schemaVersion == TraitLiveOpsRepository.SCHEMA_VERSION) { "Unsupported trait catalog schema." }
    require(payload.hostApiVersion == TraitLiveOpsRepository.HOST_API_VERSION) { "Unsupported trait catalog host API." }
    require(payload.minAppVersionCode > 0 && payload.maxAppVersionCode >= payload.minAppVersionCode) {
        "Invalid trait catalog app compatibility range."
    }
    require(appVersionCode in payload.minAppVersionCode..payload.maxAppVersionCode) {
        "Trait catalog is incompatible with this app version."
    }
    require(payload.catalogVersion > 0) { "Invalid trait catalog version." }
    require(payload.traits.size in 1..TraitLiveOpsRepository.MAX_TRAITS) { "Trait count is outside the supported limit." }
    require(payload.traits.map { it.id }.distinct().size == payload.traits.size) { "Duplicate remote trait IDs." }
    val builtInIds = BuiltInTraits.definitions.mapTo(mutableSetOf()) { it.id }
    val definitions = payload.traits.map { remote ->
        require(remote.id !in builtInIds) { "Remote traits cannot replace built-in IDs." }
        val definition = TraitDefinition(
            id = remote.id, titleKey = remote.titleKey, descriptionKey = remote.descriptionKey,
            category = TraitCategory.valueOf(remote.category), visibility = TraitVisibility.valueOf(remote.visibility),
            source = TraitSource.LIVEOPS, definitionVersion = remote.definitionVersion,
            minHostApiVersion = remote.minHostApiVersion, maxHostApiVersion = remote.maxHostApiVersion,
            activation = TraitActivationConditions(remote.minimumLevel, remote.minimumPrestige, remote.minimumCorrectGuesses),
            dependencies = remote.dependencies.toSet(), conflicts = remote.conflicts.toSet(),
            effects = remote.effects.mapTo(linkedSetOf()) { TraitEffect.valueOf(it) }, seasonId = remote.seasonId
        )
        TraitDefinitionValidator.validate(definition)
        definition
    }
    val allFiles = payload.traits.flatMap { it.files }
    require(allFiles.map { it.path }.distinct().size == allFiles.size) { "Duplicate trait asset path." }
    payload.traits.forEach { trait ->
        require(trait.files.size <= TraitLiveOpsRepository.MAX_FILES_PER_TRAIT) { "Trait has too many files." }
        if (trait.files.isNotEmpty()) {
            require(!trait.folderName.isNullOrBlank()) { "Trait files require a folderName." }
            require(Regex("[A-Za-z0-9_-]{1,80}").matches(trait.folderName)) { "Invalid trait folder name." }
        }
        trait.files.forEach { file ->
            validateTraitAssetPath(trait, file.path)
            require(file.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Invalid SHA-256 for trait asset." }
            require(file.kind in setOf("lua", "json")) { "Unsupported trait asset kind." }
            require((file.kind == "lua") == file.path.endsWith(".lua")) { "Trait file extension does not match its kind." }
            require(file.kind != "json" || file.path.endsWith(".json")) { "Trait JSON files must end in .json." }
        }
    }
    TraitRegistry(BuiltInTraits.definitions + definitions)
    return definitions
}

private fun validateTraitAssetPath(trait: RemoteTraitDefinition, path: String): String {
    require(!trait.folderName.isNullOrBlank() && Regex("[A-Za-z0-9_-]{1,80}").matches(trait.folderName)) {
        "Trait asset path requires a valid folderName."
    }
    val expectedPrefix = "assets/traits/" + trait.folderName + "/"
    require(path.startsWith(expectedPrefix) && path.length <= 240) { "Trait asset path is outside its trait folder." }
    require(path.split('/').all { it.isNotBlank() && it != "." && it != ".." }) { "Invalid trait asset path." }
    require(path.endsWith(".lua") || path.endsWith(".json")) { "Unsupported trait asset extension." }
    return path
}
