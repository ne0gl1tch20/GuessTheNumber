package com.jarrlyyy.guessthenumber.scripting

import android.content.Context
import android.util.Base64
import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.security.MessageDigest

@Serializable
private data class SignedLuaManifest(
    val payloadBase64: String,
    val signatureBase64: String
)

@Serializable
private data class LuaRemotePayload(
    val schemaVersion: Int,
    val hostApiVersion: Int,
    val minAppVersionCode: Int,
    val maxAppVersionCode: Int,
    val bundleVersion: Int,
    val scripts: List<LuaRemoteScript>
)

@Serializable
private data class LuaRemoteScript(
    val id: String,
    val fileName: String,
    val title: String,
    val description: String,
    val sha256: String,
    val featured: Boolean = false,
    val hostApiVersion: Int = 1,
    val capabilities: List<String> = listOf("log")
)

sealed interface LuaLiveOpsRefreshResult {
    data class Updated(val bundleVersion: Int, val scriptCount: Int) : LuaLiveOpsRefreshResult
    data class Unavailable(val reason: String) : LuaLiveOpsRefreshResult
}

/**
 * Fetches signed LiveOps bundles from HTTPS only. The signature covers the exact
 * payload bytes; every script is SHA-256 checked before an atomic bundle activation.
 * An unset signing key fails closed, so an unconfigured remote repository cannot execute.
 */
class LuaLiveOpsRepository(private val context: Context) {
    companion object {
        private const val MAX_MANIFEST_BYTES = 256 * 1024
        private const val MAX_SCRIPT_BYTES = LuaSandbox.MAX_SOURCE_CHARS
        private const val MAX_SCRIPTS = 50
    }

    private val json = Json { ignoreUnknownKeys = false }
    private val root = File(context.filesDir, "lua-liveops")
    private val activePointer = File(root, "active_bundle.txt")

    suspend fun refresh(): LuaLiveOpsRefreshResult = withContext(Dispatchers.IO) {
        try {
            require(BuildConfig.LIVEOPS_PUBLIC_KEY_BASE64.isNotBlank()) {
                "LiveOps signing key is not configured. Bundled activities remain available."
            }
            val manifestUrl = URL(BuildConfig.LIVEOPS_MANIFEST_URL)
            require(manifestUrl.protocol == "https") { "LiveOps requires HTTPS." }
            val envelopeBytes = readHttps(manifestUrl, MAX_MANIFEST_BYTES)
            val envelope = json.decodeFromString<SignedLuaManifest>(envelopeBytes.toString(Charsets.UTF_8))
            val payloadBytes = Base64.decode(envelope.payloadBase64, Base64.DEFAULT)
            verifySignature(payloadBytes, envelope.signatureBase64)
            val payload = json.decodeFromString<LuaRemotePayload>(payloadBytes.toString(Charsets.UTF_8))
            validatePayload(payload)
            val currentVersion = activePointer.takeIf { it.isFile }?.readText()?.trim()?.toIntOrNull() ?: 0
            require(payload.bundleVersion > currentVersion) { "LiveOps bundle version must increase to preserve rollback." }

            val staging = File(root, "bundle-${payload.bundleVersion}.staging")
            staging.deleteRecursively()
            check(staging.mkdirs()) { "Could not create LiveOps staging directory." }
            File(staging, "signed_manifest.json").writeBytes(envelopeBytes)
            payload.scripts.forEach { script ->
                val scriptUrl = URL(manifestUrl, "scripts/${script.fileName}")
                val bytes = readHttps(scriptUrl, MAX_SCRIPT_BYTES)
                val source = bytes.toString(Charsets.UTF_8)
                require(source.isNotBlank()) { "Script ${script.id} is empty." }
                require(sha256(bytes).equals(script.sha256, ignoreCase = true)) {
                    "Script hash mismatch for ${script.id}."
                }
                LuaSandbox {}.validate(script.id, source)
                File(staging, script.fileName).writeBytes(bytes)
            }

            val active = File(root, "bundle-${payload.bundleVersion}")
            active.deleteRecursively()
            check(staging.renameTo(active)) { "Could not activate the verified LiveOps bundle." }
            val pointerTemp = File(root, "active_bundle.tmp")
            pointerTemp.writeText(payload.bundleVersion.toString())
            android.system.Os.rename(pointerTemp.absolutePath, activePointer.absolutePath)

            GameLogger.log(
                LogLevel.INFO,
                LoggerCategory.UI,
                "LUA_LIVEOPS_UPDATED",
                "Activated verified LiveOps bundle ${payload.bundleVersion} (${payload.scripts.size} scripts)."
            )
            LuaLiveOpsRefreshResult.Updated(payload.bundleVersion, payload.scripts.size)
        } catch (error: Exception) {
            val reason = error.message?.take(240) ?: "LiveOps refresh failed."
            GameLogger.log(LogLevel.WARN, LoggerCategory.UI, "LUA_LIVEOPS_REFRESH_FAILED", reason)
            LuaLiveOpsRefreshResult.Unavailable(reason)
        }
    }

    fun loadVerifiedCachedEntries(): List<LuaScriptEntry> {
        val verified = activeVerifiedBundle() ?: run {
            rollbackToPreviousVerifiedBundle()
            activeVerifiedBundle()
        } ?: return emptyList()
        val (payload, _) = verified
        return payload.scripts.map { script ->
            LuaScriptEntry(
                id = script.id,
                file = script.fileName,
                title = script.title,
                description = script.description,
                featured = script.featured,
                source = "liveops",
                hostApiVersion = script.hostApiVersion
            )
        }
    }

    fun readVerifiedCachedScript(entry: LuaScriptEntry): String? {
        if (entry.source != "liveops") return null
        val (payload, bundleDir) = activeVerifiedBundle() ?: return null
        val remote = payload.scripts.firstOrNull { it.id == entry.id && it.fileName == entry.file }
            ?: return null
        val file = File(bundleDir, remote.fileName)
        if (!file.isFile || file.length() > MAX_SCRIPT_BYTES) return null
        val bytes = file.readBytes()
        if (!sha256(bytes).equals(remote.sha256, ignoreCase = true)) return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun rollbackToPreviousVerifiedBundle(): Boolean {
        val currentVersion = runCatching {
            activePointer.takeIf { it.isFile }?.readText()?.trim()?.toIntOrNull()
        }.getOrNull() ?: return false
        val candidates = root.listFiles()
            .orEmpty()
            .mapNotNull { file ->
                Regex("bundle-(\\d+)").matchEntire(file.name)?.groupValues?.get(1)?.toIntOrNull()
                    ?.takeIf { file.isDirectory && it < currentVersion }
            }
            .sortedDescending()
        for (version in candidates) {
            val verified = runCatching { verifiedBundleAt(version) }.getOrNull() ?: continue
            val pointerTemp = File(root, "active_bundle.rollback.tmp")
            return runCatching {
                pointerTemp.writeText(verified.first.bundleVersion.toString())
                android.system.Os.rename(pointerTemp.absolutePath, activePointer.absolutePath)
                GameLogger.log(
                    LogLevel.WARN,
                    LoggerCategory.UI,
                    "LUA_LIVEOPS_ROLLBACK",
                    "Rolled back to verified LiveOps bundle ${verified.first.bundleVersion}."
                )
                true
            }.getOrDefault(false)
        }
        return false
    }

    private fun activeVerifiedBundle(): Pair<LuaRemotePayload, File>? = runCatching {
        val version = activePointer.readText().trim().toInt()
        verifiedBundleAt(version)
    }.getOrNull()

    private fun verifiedBundleAt(version: Int): Pair<LuaRemotePayload, File> {
        val dir = File(root, "bundle-$version")
        val envelopeFile = File(dir, "signed_manifest.json")
        require(envelopeFile.isFile && envelopeFile.length() <= MAX_MANIFEST_BYTES)
        val envelope = json.decodeFromString<SignedLuaManifest>(envelopeFile.readText())
        val payloadBytes = Base64.decode(envelope.payloadBase64, Base64.DEFAULT)
        verifySignature(payloadBytes, envelope.signatureBase64)
        val payload = json.decodeFromString<LuaRemotePayload>(payloadBytes.toString(Charsets.UTF_8))
        validatePayload(payload)
        require(payload.bundleVersion == version)
        payload.scripts.forEach { script ->
            val file = File(dir, script.fileName)
            require(file.isFile && file.length() <= MAX_SCRIPT_BYTES)
            val bytes = file.readBytes()
            require(sha256(bytes).equals(script.sha256, ignoreCase = true))
        }
        return payload to dir
    }

    private fun validatePayload(payload: LuaRemotePayload) {
        require(payload.schemaVersion == 1) { "Unsupported LiveOps bundle schema." }
        require(payload.hostApiVersion == LuaEngine.HOST_API_VERSION) { "LiveOps bundle requires an unsupported host API." }
        require(BuildConfig.VERSION_CODE in payload.minAppVersionCode..payload.maxAppVersionCode) {
            "LiveOps bundle is not compatible with this app version."
        }
        require(payload.bundleVersion > 0) { "Invalid LiveOps bundle version." }
        require(payload.minAppVersionCode > 0 && payload.maxAppVersionCode >= payload.minAppVersionCode) {
            "Invalid LiveOps app compatibility range."
        }
        require(payload.scripts.size in 1..MAX_SCRIPTS) { "LiveOps script count is outside the supported limit." }
        require(payload.scripts.map { it.id }.distinct().size == payload.scripts.size) { "Duplicate LiveOps script IDs." }
        payload.scripts.forEach { script ->
            require(script.id.matches(Regex("[a-z0-9_-]{1,64}"))) { "Invalid LiveOps script ID." }
            require(script.fileName.matches(Regex("[a-zA-Z0-9_-]{1,80}\\.lua"))) { "Invalid LiveOps script file name." }
            require(script.title.isNotBlank() && script.title.length <= 100 && script.description.isNotBlank() && script.description.length <= 240) {
                "LiveOps script metadata is incomplete or too long."
            }
            require(script.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Invalid LiveOps script hash." }
            require(script.hostApiVersion == LuaEngine.HOST_API_VERSION) { "Unsupported LiveOps script host API." }
            require(script.capabilities.all { it == "log" }) { "LiveOps requested an unsupported capability." }
        }
    }

    private fun verifySignature(payload: ByteArray, signatureBase64: String) {
        val encodedKey = Base64.decode(BuildConfig.LIVEOPS_PUBLIC_KEY_BASE64, Base64.DEFAULT)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encodedKey))
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(publicKey)
        verifier.update(payload)
        require(verifier.verify(Base64.decode(signatureBase64, Base64.DEFAULT))) {
            "LiveOps manifest signature is invalid."
        }
    }

    private fun readHttps(url: URL, maxBytes: Int): ByteArray {
        require(url.protocol == "https") { "Only HTTPS LiveOps URLs are allowed." }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json, text/plain")
            setRequestProperty("User-Agent", "GuessTheNumber-LuaLiveOps/1")
        }
        try {
            val status = connection.responseCode
            require(status == HttpURLConnection.HTTP_OK) { "LiveOps server returned HTTP $status." }
            val declaredLength = connection.contentLengthLong
            require(declaredLength < 0 || declaredLength <= maxBytes) { "LiveOps response is too large." }
            return connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= maxBytes) { "LiveOps response is too large." }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
