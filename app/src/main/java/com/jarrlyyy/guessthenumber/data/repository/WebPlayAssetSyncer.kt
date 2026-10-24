package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Offline-first updater for repository-owned web UI, fonts and locales. */
class WebPlayAssetSyncer(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "web-play-sync")
    private val versions = File(root, "versions")
    private val activePointer = File(root, "active-version")
    private val syncMutex = PROCESS_MUTEX

    data class SyncResult(val changed: Boolean, val version: String?, val message: String)

    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) { syncMutex.withLock {
        try {
            root.mkdirs()
            versions.mkdirs()
            val manifest = JSONObject(String(request(MANIFEST_URL), Charsets.UTF_8))
            require(manifest.optInt("schemaVersion") == 1) { "Unsupported manifest schema" }
            val version = manifest.getString("version")
            require(version.matches(Regex("[a-f0-9]{7,40}"))) { "Invalid web-play version" }
            require(manifest.optInt("minAppVersionCode", Int.MAX_VALUE) <= APP_VERSION_CODE) { "Update requires a newer app" }
            val entries = manifest.getJSONArray("files")
            require(entries.length() in 3..256) { "Invalid manifest file count" }
            val files = linkedMapOf<String, String>()
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                val path = entry.getString("path")
                val hash = entry.getString("sha256")
                require(isAllowedPath(path)) { "Rejected manifest path" }
                require(hash.matches(Regex("[a-f0-9]{64}"))) { "Invalid hash for $path" }
                require(files.put(path, hash) == null) { "Duplicate manifest path" }
            }
            require(files.keys.containsAll(REQUIRED_FILES)) { "Manifest is missing required files" }

            val currentVersion = activeVersion()
            val currentRoot = currentVersion?.let { File(versions, it) }
            if (currentVersion == version && currentRoot?.isDirectory == true &&
                files.all { (path, hash) -> File(currentRoot, path).isFile && sha256(File(currentRoot, path)) == hash }) {
                return@withContext SyncResult(false, version, "Already up to date")
            }

            val staging = File(root, "staging-$version")
            staging.deleteRecursively()
            staging.mkdirs()
            try {
                files.forEach { (path, expectedHash) ->
                    val target = File(staging, path)
                    target.parentFile?.mkdirs()
                    val cached = currentRoot?.let { File(it, path) }
                    when {
                        cached?.isFile == true && sha256(cached) == expectedHash ->
                            cached.copyTo(target, overwrite = true)
                        bundledAssetMatches(path, expectedHash) ->
                            appContext.assets.open(path).use { input -> target.outputStream().use { input.copyTo(it) } }
                        else -> {
                            val bytes = request("$RAW_BASE/$version/app/src/main/assets/$path")
                            require(sha256(bytes) == expectedHash) { "Hash mismatch for $path" }
                            target.writeBytes(bytes)
                        }
                    }
                    require(target.isFile && sha256(target) == expectedHash) { "Staged file failed validation: $path" }
                }
                require(REQUIRED_FILES.all { File(staging, it).isFile && File(staging, it).length() > 0L }) {
                    "Required web-play file is empty or missing"
                }
                val finalDir = File(versions, version)
                if (finalDir.exists()) finalDir.deleteRecursively()
                check(staging.renameTo(finalDir)) { "Could not activate staged assets" }
                val tempPointer = File(root, "active-version.tmp")
                tempPointer.writeText(version)
                check(tempPointer.renameTo(activePointer)) { "Could not update active version pointer" }
                versions.listFiles()?.filter { it.isDirectory && it.name != version && it.name != currentVersion }
                    ?.forEach { it.deleteRecursively() }
                SyncResult(true, version, "Updated web-play assets")
            } catch (error: Exception) {
                staging.deleteRecursively()
                throw error
            }
        } catch (error: Exception) {
            SyncResult(false, activeVersion(), "Using last working assets (\${error.javaClass.simpleName})")
        }
    } }

    fun rollbackToPrevious(): Boolean {
        val current = activeVersion() ?: return false
        val previous = versions.listFiles()?.filter { it.isDirectory && it.name != current && it.name.matches(Regex("[a-f0-9]{7,40}")) }
            ?.filter { REQUIRED_FILES.all { path -> File(it, path).isFile && File(it, path).length() > 0L } }
            ?.maxByOrNull { it.lastModified() } ?: return false
        return runCatching {
            val temp = File(root, "active-version.rollback.tmp")
            temp.writeText(previous.name)
            check(temp.renameTo(activePointer))
            true
        }.getOrDefault(false)
    }

    fun activeVersion(): String? = runCatching { activePointer.takeIf { it.isFile }?.readText()?.trim() }.getOrNull()

    fun pathHandler(assetRoot: String): WebViewAssetLoader.PathHandler = WebViewAssetLoader.PathHandler { path ->
        val safePath = path.trimStart('/')
        if (safePath.isEmpty() || safePath.split('/').any { it == ".." || it.isEmpty() } || safePath.contains('\\')) {
            return@PathHandler null
        }
        val relative = "$assetRoot/$safePath"
        val version = activeVersion()
        val activeRoot = version?.let { File(versions, it) }
        val cached = activeRoot?.let { File(it, relative) }
        val input = if (cached?.isFile == true && activeRoot != null &&
            cached.canonicalPath.startsWith(activeRoot.canonicalPath + File.separator)) {
            cached.inputStream()
        } else {
            runCatching { appContext.assets.open(relative) }.getOrNull()
        } ?: return@PathHandler null
        WebResourceResponse(mimeType(relative), null, input)
    }

    private fun bundledAssetMatches(path: String, expectedHash: String): Boolean =
        runCatching { appContext.assets.open(path).use { sha256(it) == expectedHash } }.getOrDefault(false)

    private fun request(url: String): ByteArray {
        require(url == MANIFEST_URL || url.startsWith("$RAW_BASE/")) { "Untrusted update URL" }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 1800
            readTimeout = 2500
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "GuessTheNumber-WebPlaySync")
        }
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Repository HTTP \${connection.responseCode}" }
            return connection.inputStream.use { stream ->
                val bytes = stream.readBytes()
                require(bytes.size <= MAX_DOWNLOAD_BYTES) { "Download exceeds size limit" }
                bytes
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun isAllowedPath(path: String): Boolean {
        if (path.isBlank() || path.startsWith('/') || path.contains('\\') ||
            path.split('/').any { it == ".." || it.isEmpty() }) return false
        return path.startsWith("web-play/") || path.startsWith("fonts/") || path.startsWith("locales/")
    }

    companion object {
        const val APP_VERSION_CODE = 21
        private val PROCESS_MUTEX = Mutex()
        private const val MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024
        private const val MANIFEST_URL = "https://raw.githubusercontent.com/ne0gl1tch20/GuessTheNumber/master/web-play-manifest.json"
        private const val RAW_BASE = "https://raw.githubusercontent.com/ne0gl1tch20/GuessTheNumber"
        private val REQUIRED_FILES = setOf("web-play/index.html", "web-play/app.js", "web-play/styles.css")

        internal fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        private fun sha256(file: File): String = file.inputStream().use(::sha256)
        private fun sha256(input: InputStream): String = sha256(input.readBytes())
        private fun mimeType(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
            "html" -> "text/html"
            "js" -> "application/javascript"
            "css" -> "text/css"
            "json" -> "application/json"
            "svg" -> "image/svg+xml"
            "ttf" -> "font/ttf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }
}
