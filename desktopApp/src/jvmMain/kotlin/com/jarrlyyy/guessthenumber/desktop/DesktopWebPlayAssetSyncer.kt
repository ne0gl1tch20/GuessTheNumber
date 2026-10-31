package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.desktop.persistence.defaultDesktopSaveDirectory
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Comparator
import java.util.concurrent.Executors

/**
 * Offline-first desktop counterpart to Android's WebPlayAssetSyncer.
 *
 * Cached versions are copied into the per-session WebView directory before it
 * is loaded. Remote synchronization runs in the background and only activates
 * a fully validated version for the next app launch, never replacing files
 * underneath a live WebView.
 */
internal object DesktopWebPlayAssetSyncer {
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/ne0gl1tch20/GuessTheNumber/master/web-play-manifest.json"
    private const val RAW_BASE = "https://raw.githubusercontent.com/ne0gl1tch20/GuessTheNumber"
    private const val APP_VERSION_CODE = 24
    private const val MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024
    private const val MAX_MANIFEST_BYTES = 256 * 1024
    private val requiredFiles = setOf(
        "web-play/index.html",
        "web-play/app.js",
        "web-play/styles.css"
    )
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "GTN-WebPlay-AssetSync").apply { isDaemon = true }
    }
    private val syncLock = Any()

    private data class ManifestFile(val path: String, val sha256: String)
    private data class Manifest(val version: String, val files: List<ManifestFile>, val raw: String)

    private val syncRoot: Path
        get() = defaultDesktopSaveDirectory().parent.resolve("web-play-sync")
    private val versionsRoot: Path
        get() = syncRoot.resolve("versions")
    private val activePointer: Path
        get() = syncRoot.resolve("active-version")

    /** Apply a previously validated cache, then schedule a non-blocking refresh. */
    fun prepare(sessionRoot: Path) {
        applyActiveCache(sessionRoot)
        executor.execute { runCatching { synchronize(sessionRoot) } }
    }

    fun activeVersion(): String? = runCatching {
        val value = activePointer.takeIf(Files::isRegularFile)?.let(Files::readString)?.trim()
        value?.takeIf { it.matches(Regex("[a-f0-9]{7,40}")) }
    }.getOrNull()

    /** Switch the pointer back to the previous validated cache, if one exists. */
    fun rollbackToPrevious(): Boolean = synchronized(syncLock) {
        val current = activeVersion() ?: return@synchronized false
        runCatching {
            val previous = Files.list(versionsRoot).use { paths ->
                paths.filter { Files.isDirectory(it) && it.fileName.toString() != current }
                    .filter { it.fileName.toString().matches(Regex("[a-f0-9]{7,40}")) }
                    .filter { runCatching { readAndValidateStoredManifest(it) }.isSuccess }
                    .maxByOrNull { Files.getLastModifiedTime(it).toMillis() }
            } ?: return@synchronized false
            writePointer(previous.fileName.toString())
            true
        }.getOrDefault(false)
    }

    private fun applyActiveCache(sessionRoot: Path) {
        val version = activeVersion() ?: return
        val cachedRoot = versionsRoot.resolve(version)
        val manifest = runCatching { readAndValidateStoredManifest(cachedRoot) }.getOrNull() ?: return
        runCatching {
            manifest.files.forEach { entry ->
                val source = safeResolve(cachedRoot, entry.path)
                val target = safeResolve(sessionRoot, entry.path)
                Files.createDirectories(target.parent)
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun synchronize(sessionRoot: Path) = synchronized(syncLock) {
        Files.createDirectories(versionsRoot)
        val manifestBytes = request(MANIFEST_URL, MAX_MANIFEST_BYTES)
        val manifestText = String(manifestBytes, Charsets.UTF_8)
        val manifest = parseManifest(manifestText)
        val currentVersion = activeVersion()
        val currentRoot = currentVersion?.let { versionsRoot.resolve(it) }
        if (currentVersion == manifest.version && currentRoot != null &&
            runCatching { readAndValidateStoredManifest(currentRoot) }.isSuccess
        ) return@synchronized

        val staging = syncRoot.resolve("staging-${manifest.version}")
        deleteTree(staging)
        Files.createDirectories(staging)
        try {
            manifest.files.forEach { entry ->
                val target = safeResolve(staging, entry.path)
                Files.createDirectories(target.parent)
                val cached = currentRoot?.let { safeResolve(it, entry.path) }
                val bundled = safeResolve(sessionRoot, entry.path)
                when {
                    cached != null && Files.isRegularFile(cached) && sha256(cached) == entry.sha256 ->
                        Files.copy(cached, target, StandardCopyOption.REPLACE_EXISTING)
                    Files.isRegularFile(bundled) && sha256(bundled) == entry.sha256 ->
                        Files.copy(bundled, target, StandardCopyOption.REPLACE_EXISTING)
                    else -> {
                        val url = "$RAW_BASE/${manifest.version}/app/src/main/assets/${entry.path}"
                        val bytes = request(url, MAX_DOWNLOAD_BYTES)
                        require(sha256(bytes) == entry.sha256) { "Hash mismatch for ${entry.path}" }
                        Files.write(target, bytes)
                    }
                }
                require(Files.isRegularFile(target) && sha256(target) == entry.sha256) {
                    "Staged asset failed validation: ${entry.path}"
                }
            }
            require(requiredFiles.all { path ->
                val file = safeResolve(staging, path)
                Files.isRegularFile(file) && Files.size(file) > 0L
            }) { "A required WebPlay asset is missing or empty." }
            Files.writeString(staging.resolve(".manifest.json"), manifest.raw)
            val finalDirectory = versionsRoot.resolve(manifest.version)
            deleteTree(finalDirectory)
            moveAtomically(staging, finalDirectory)
            writePointer(manifest.version)
            pruneVersions(currentVersion, manifest.version)
        } catch (error: Exception) {
            deleteTree(staging)
            throw error
        }
    }

    private fun parseManifest(raw: String): Manifest {
        val json = JSONObject(raw)
        require(json.optInt("schemaVersion", -1) == 1) { "Unsupported WebPlay manifest schema." }
        val version = json.optString("version")
        require(version.matches(Regex("[a-f0-9]{7,40}"))) { "Invalid WebPlay version." }
        require(json.optInt("minAppVersionCode", Int.MAX_VALUE) <= APP_VERSION_CODE) {
            "WebPlay update requires a newer desktop app."
        }
        val array = json.optJSONArray("files") ?: error("WebPlay manifest has no files array.")
        require(array.length() in 3..256) { "Invalid WebPlay manifest file count." }
        val entries = linkedMapOf<String, String>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val path = item.getString("path")
            val hash = item.getString("sha256")
            require(isAllowedPath(path)) { "Rejected WebPlay manifest path." }
            require(hash.matches(Regex("[a-f0-9]{64}"))) { "Invalid SHA-256 for $path." }
            require(entries.put(path, hash) == null) { "Duplicate WebPlay manifest path." }
        }
        require(entries.keys.containsAll(requiredFiles)) { "WebPlay manifest is missing required files." }
        return Manifest(version, entries.map { ManifestFile(it.key, it.value) }, raw)
    }

    private fun readAndValidateStoredManifest(directory: Path): Manifest {
        val manifestPath = directory.resolve(".manifest.json")
        require(Files.isRegularFile(manifestPath)) { "Cached WebPlay manifest is missing." }
        val manifest = parseManifest(Files.readString(manifestPath))
        require(directory.fileName.toString() == manifest.version) { "Cached WebPlay version does not match its directory." }
        manifest.files.forEach { entry ->
            val file = safeResolve(directory, entry.path)
            require(Files.isRegularFile(file) && sha256(file) == entry.sha256) {
                "Cached WebPlay asset failed validation: ${entry.path}"
            }
        }
        return manifest
    }

    private fun isAllowedPath(path: String): Boolean {
        if (path.isBlank() || path.startsWith('/') || path.contains('\\') ||
            path.split('/').any { it.isEmpty() || it == "." || it == ".." }
        ) return false
        return path.startsWith("web-play/") || path.startsWith("fonts/") ||
            path.startsWith("locales/") || path.startsWith("libraries/")
    }

    private fun safeResolve(root: Path, relative: String): Path {
        require(isAllowedPath(relative)) { "Unsafe WebPlay asset path." }
        val resolved = root.resolve(relative).normalize()
        require(resolved.startsWith(root.normalize())) { "WebPlay asset escaped its root directory." }
        return resolved
    }

    private fun request(address: String, maximumBytes: Int): ByteArray {
        val uri = java.net.URI(address)
        require(uri.scheme == "https" && uri.host == "raw.githubusercontent.com" &&
            (address == MANIFEST_URL || address.startsWith("$RAW_BASE/"))
        ) { "Untrusted WebPlay update URL." }
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 1800
        connection.readTimeout = 3000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "GuessTheNumber-Windows-WebPlaySync")
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "WebPlay asset server returned HTTP ${connection.responseCode}."
            }
            if (connection.contentLengthLong > maximumBytes) {
                error("WebPlay asset exceeded the download size limit.")
            }
            return connection.inputStream.use { readBounded(it, maximumBytes) }
        } finally {
            connection.disconnect()
        }
    }

    private fun readBounded(input: InputStream, maximumBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maximumBytes) { "WebPlay download exceeded the size limit." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun sha256(path: Path): String =
        Files.newInputStream(path).use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun writePointer(version: String) {
        val temporary = syncRoot.resolve("active-version.tmp")
        Files.writeString(temporary, version)
        moveAtomically(temporary, activePointer)
    }

    private fun moveAtomically(source: Path, destination: Path) {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun pruneVersions(previousVersion: String?, currentVersion: String) {
        if (!Files.isDirectory(versionsRoot)) return
        Files.list(versionsRoot).use { paths ->
            paths.filter { Files.isDirectory(it) }
                .filter { it.fileName.toString() != currentVersion && it.fileName.toString() != previousVersion }
                .forEach(::deleteTree)
        }
    }

    private fun deleteTree(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { runCatching { Files.deleteIfExists(it) } }
        }
    }
}
