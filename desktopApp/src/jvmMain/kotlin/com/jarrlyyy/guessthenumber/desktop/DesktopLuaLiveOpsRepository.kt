package com.jarrlyyy.guessthenumber.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.jarrlyyy.guessthenumber.desktop.persistence.defaultDesktopSaveDirectory
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Comparator
import java.util.Properties

@Serializable
private data class DesktopSignedLuaManifest(
    val payloadBase64: String,
    val signatureBase64: String
)

@Serializable
private data class DesktopLuaRemotePayload(
    val schemaVersion: Int,
    val hostApiVersion: Int,
    val minAppVersionCode: Int,
    val maxAppVersionCode: Int,
    val bundleVersion: Int,
    val scripts: List<DesktopLuaRemoteScript>,
    val eventManifestVersion: Int = 1,
    val events: List<DesktopLiveOpsEvent> = emptyList()
)

@Serializable
private data class DesktopLuaRemoteScript(
    val id: String,
    val fileName: String,
    val title: String,
    val description: String,
    val sha256: String,
    val featured: Boolean = false,
    val hostApiVersion: Int = 1,
    val capabilities: List<String> = listOf("log")
)

private fun desktopLuaBuildConfigValue(name: String): String = runCatching {
    val properties = Properties()
    DesktopLuaLiveOpsRepository::class.java.classLoader
        .getResourceAsStream("desktop-build.properties")?.use(properties::load)
    properties.getProperty(name).orEmpty()
}.getOrDefault("")

internal sealed interface DesktopLuaLiveOpsRefreshResult {
    data class Updated(val version: Int, val scriptCount: Int) : DesktopLuaLiveOpsRefreshResult
    data class Unchanged(val version: Int, val scriptCount: Int) : DesktopLuaLiveOpsRefreshResult
    data class Unavailable(val reason: String) : DesktopLuaLiveOpsRefreshResult
}

/** Signed, HTTPS-only LiveOps cache. A blank public key fails closed. */
internal class DesktopLuaLiveOpsRepository(
    private val root: Path = defaultDesktopSaveDirectory().parent.resolve("lua-liveops"),
    private val manifestUrl: String = desktopLuaBuildConfigValue("liveOpsManifestUrl"),
    private val publicKeyBase64: String = desktopLuaBuildConfigValue("liveOpsPublicKeyBase64"),
    private val appVersionCode: Int = desktopLuaBuildConfigValue("appVersionCode").toIntOrNull() ?: 24
) {
    private val json = Json { ignoreUnknownKeys = false }
    private val activePointer = root.resolve("active_bundle.txt")
    private val highestVersionFile = root.resolve("highest_bundle_version.txt")

    fun refresh(): DesktopLuaLiveOpsRefreshResult {
        return try {
        require(publicKeyBase64.isNotBlank()) {
            "LiveOps signing key is not configured. Bundled activities remain available."
        }
        val url = URL(manifestUrl)
        require(url.protocol == "https") { "LiveOps requires HTTPS." }
        val envelopeBytes = readHttps(url, MAX_MANIFEST_BYTES)
        val envelope = json.decodeFromString<DesktopSignedLuaManifest>(envelopeBytes.toString(Charsets.UTF_8))
        val payloadBytes = Base64.getMimeDecoder().decode(envelope.payloadBase64)
        verifySignature(payloadBytes, envelope.signatureBase64)
        val payload = json.decodeFromString<DesktopLuaRemotePayload>(payloadBytes.toString(Charsets.UTF_8))
        validatePayload(payload)
        val activeVersion = readVersion(activePointer)
        val highestVersion = maxOf(activeVersion, readVersion(highestVersionFile))
        validateIncomingVersion(payload.bundleVersion, activeVersion, highestVersion)
        if (payload.bundleVersion == activeVersion && activeVersion == highestVersion) {
            return DesktopLuaLiveOpsRefreshResult.Unchanged(payload.bundleVersion, payload.scripts.size)
        }

        Files.createDirectories(root)
        val staging = root.resolve("bundle-${payload.bundleVersion}.staging")
        deleteTree(staging)
        Files.createDirectories(staging)
        Files.write(staging.resolve("signed_manifest.json"), envelopeBytes)
        payload.scripts.forEach { script ->
            val scriptUrl = URL(url, "scripts/${script.fileName}")
            val bytes = readHttps(scriptUrl, MAX_SCRIPT_BYTES)
            require(bytes.isNotEmpty()) { "Script ${script.id} is empty." }
            require(sha256(bytes).equals(script.sha256, ignoreCase = true)) {
                "Script hash mismatch for ${script.id}."
            }
            DesktopLuaSandbox().validateSource(script.id, bytes.toString(Charsets.UTF_8))
            Files.write(staging.resolve(script.fileName), bytes)
        }
        verifyBundleDirectory(staging, payload.bundleVersion)
        val active = root.resolve("bundle-${payload.bundleVersion}")
        deleteTree(active)
        moveAtomically(staging, active)
        writeVersionAtomically(highestVersionFile, payload.bundleVersion)
        writeVersionAtomically(activePointer, payload.bundleVersion)
        DesktopLuaLiveOpsRefreshResult.Updated(payload.bundleVersion, payload.scripts.size)
        } catch (error: Exception) {
            DesktopLuaLiveOpsRefreshResult.Unavailable(error.message?.take(240) ?: "LiveOps refresh failed.")
        }
    }

    fun loadVerifiedCachedEntries(): List<DesktopLuaScriptEntry> {
        val verified = activeVerifiedBundle() ?: run {
            rollbackToPreviousVerifiedBundle()
            activeVerifiedBundle()
        } ?: return emptyList()
        return verified.first.scripts.map { script ->
            DesktopLuaScriptEntry(
                id = script.id,
                file = script.fileName,
                title = script.title,
                description = script.description,
                featured = script.featured,
                hostApiVersion = script.hostApiVersion,
                source = "liveops"
            )
        }
    }

    fun loadVerifiedCachedEvents(): List<DesktopLiveOpsEvent> =
        activeVerifiedBundle()?.first?.events.orEmpty()

    fun readVerifiedCachedScript(entry: DesktopLuaScriptEntry): String? {
        if (entry.source != "liveops") return null
        val (payload, directory) = activeVerifiedBundle() ?: return null
        val script = payload.scripts.firstOrNull { it.id == entry.id && it.fileName == entry.file }
            ?: return null
        val file = directory.resolve(script.fileName)
        if (!Files.isRegularFile(file) || Files.size(file) !in 1..MAX_SCRIPT_BYTES.toLong()) return null
        val bytes = Files.readAllBytes(file)
        if (!sha256(bytes).equals(script.sha256, ignoreCase = true)) return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun rollbackToPreviousVerifiedBundle(): Boolean {
        val currentVersion = runCatching { readVersion(activePointer) }.getOrDefault(0)
        if (currentVersion <= 0) return false
        val candidates = runCatching {
            Files.list(root).use { paths ->
                paths.map { it.fileName.toString() }
                    .map { Regex("bundle-(\\d+)").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
                    .filter { it != null && it < currentVersion }
                    .map { it!! }
                    .sorted(Comparator.reverseOrder())
                    .toList()
            }
        }.getOrDefault(emptyList())
        for (version in candidates) {
            val verified = runCatching { verifyBundleDirectory(root.resolve("bundle-$version"), version) }.getOrNull()
                ?: continue
            return runCatching {
                writeVersionAtomically(highestVersionFile, maxOf(currentVersion, readVersion(highestVersionFile)))
                writeVersionAtomically(activePointer, verified.first.bundleVersion)
                true
            }.getOrDefault(false)
        }
        return false
    }

    internal fun validateIncomingVersion(incomingVersion: Int, activeVersion: Int, highestAcceptedVersion: Int) {
        require(incomingVersion > 0) { "Invalid LiveOps bundle version." }
        require(incomingVersion >= maxOf(activeVersion, highestAcceptedVersion)) {
            "LiveOps bundle version is older than the highest accepted version."
        }
    }

    private fun validatePayload(payload: DesktopLuaRemotePayload) {
        require(payload.schemaVersion == 1) { "Unsupported LiveOps bundle schema." }
        require(payload.hostApiVersion == DesktopLuaActivityEngine.HOST_API_VERSION) {
            "LiveOps bundle requires an unsupported host API."
        }
        require(appVersionCode in payload.minAppVersionCode..payload.maxAppVersionCode) {
            "LiveOps bundle is not compatible with this desktop version."
        }
        require(payload.bundleVersion > 0 && payload.minAppVersionCode > 0 &&
            payload.maxAppVersionCode >= payload.minAppVersionCode
        ) { "Invalid LiveOps bundle version or app compatibility range." }
        require(payload.scripts.size in 1..MAX_SCRIPTS) { "LiveOps script count is outside the supported limit." }
        require(payload.eventManifestVersion in 1..2) { "Unsupported LiveOps event manifest version." }
        require(payload.events.size <= 32) { "LiveOps event count is outside the supported limit." }
        if (payload.events.isNotEmpty()) DesktopLiveOpsRepository.validateRemoteEvents(payload.eventManifestVersion, payload.events)
        require(payload.scripts.map { it.id }.distinct().size == payload.scripts.size) { "Duplicate LiveOps script IDs." }
        require(payload.scripts.map { it.fileName }.distinct().size == payload.scripts.size) {
            "Duplicate LiveOps script file names."
        }
        payload.scripts.forEach { script ->
            require(script.id.matches(Regex("[a-z0-9_-]{1,64}"))) { "Invalid LiveOps script ID." }
            require(script.fileName.matches(Regex("[a-zA-Z0-9_-]{1,80}\\.lua"))) {
                "Invalid LiveOps script file name."
            }
            require(script.title.isNotBlank() && script.title.length <= 100 &&
                script.description.isNotBlank() && script.description.length <= 240
            ) { "LiveOps script metadata is incomplete or too long." }
            require(script.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Invalid LiveOps script hash." }
            require(script.hostApiVersion == DesktopLuaActivityEngine.HOST_API_VERSION) {
                "Unsupported LiveOps script host API."
            }
            require(script.capabilities.all { it == "log" }) { "LiveOps requested an unsupported capability." }
        }
    }

    private fun verifyBundleDirectory(directory: Path, expectedVersion: Int): Pair<DesktopLuaRemotePayload, Path> {
        val manifestFile = directory.resolve("signed_manifest.json")
        require(Files.isRegularFile(manifestFile) && Files.size(manifestFile) <= MAX_MANIFEST_BYTES) {
            "LiveOps bundle is missing its signed manifest or exceeds the size limit."
        }
        val envelopeBytes = Files.readAllBytes(manifestFile)
        val envelope = json.decodeFromString<DesktopSignedLuaManifest>(envelopeBytes.toString(Charsets.UTF_8))
        val payloadBytes = Base64.getMimeDecoder().decode(envelope.payloadBase64)
        verifySignature(payloadBytes, envelope.signatureBase64)
        val payload = json.decodeFromString<DesktopLuaRemotePayload>(payloadBytes.toString(Charsets.UTF_8))
        validatePayload(payload)
        require(payload.bundleVersion == expectedVersion) { "LiveOps bundle version does not match its directory." }
        payload.scripts.forEach { script ->
            val file = directory.resolve(script.fileName).normalize()
            require(file.parent == directory.normalize() && Files.isRegularFile(file) &&
                Files.size(file) in 1..MAX_SCRIPT_BYTES.toLong()
            ) { "LiveOps bundle is missing script ${script.id} or its size is invalid." }
            val bytes = Files.readAllBytes(file)
            require(sha256(bytes).equals(script.sha256, ignoreCase = true)) {
                "Script hash mismatch for ${script.id}."
            }
            DesktopLuaSandbox().validateSource(script.id, bytes.toString(Charsets.UTF_8))
        }
        return payload to directory
    }

    private fun activeVerifiedBundle(): Pair<DesktopLuaRemotePayload, Path>? = runCatching {
        verifyBundleDirectory(root.resolve("bundle-${readVersion(activePointer)}"), readVersion(activePointer))
    }.getOrNull()

    private fun verifySignature(payload: ByteArray, signatureBase64: String) {
        val keyBytes = Base64.getMimeDecoder().decode(publicKeyBase64)
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(key)
        verifier.update(payload)
        require(verifier.verify(Base64.getMimeDecoder().decode(signatureBase64))) {
            "LiveOps manifest signature is invalid."
        }
    }

    private fun readHttps(url: URL, maximumBytes: Int): ByteArray {
        require(url.protocol == "https") { "Only HTTPS LiveOps URLs are allowed." }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json, text/plain")
            setRequestProperty("User-Agent", "GuessTheNumber-Windows-LuaLiveOps/1")
        }
        try {
            val status = connection.responseCode
            require(status == HttpURLConnection.HTTP_OK) { "LiveOps server returned HTTP $status." }
            require(connection.contentLengthLong < 0 || connection.contentLengthLong <= maximumBytes) {
                "LiveOps response is too large."
            }
            return connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= maximumBytes) { "LiveOps response is too large." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readVersion(path: Path): Int {
        if (!Files.exists(path)) return 0
        val version = Files.readString(path).trim().toIntOrNull()
            ?: throw IllegalStateException("Stored LiveOps version marker is invalid.")
        require(version >= 0) { "Stored LiveOps version marker is negative." }
        return version
    }

    private fun writeVersionAtomically(path: Path, version: Int) {
        check(version > 0) { "Bundle version must be positive." }
        Files.createDirectories(path.parent)
        val temp = path.resolveSibling("${path.fileName}.tmp")
        Files.writeString(temp, version.toString())
        moveAtomically(temp, path)
    }

    private fun moveAtomically(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun deleteTree(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }


    private companion object {
        const val MAX_MANIFEST_BYTES = 256 * 1024
        const val MAX_SCRIPT_BYTES = 32 * 1024
        const val MAX_SCRIPTS = 50
    }
}
