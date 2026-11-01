package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import android.webkit.WebResourceResponse
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Offline-first reader for independently encrypted game-data chunks. */
object GameDataArchive {
    private const val ROOT_ASSET = "game_data"
    private const val MAGIC = "GTNGBIN1"
    private const val FORMAT_VERSION = 1
    private const val MAX_CHUNK_BYTES = 16 * 1024 * 1024
    private const val MAX_EXTRACTED_BYTES = 256L * 1024 * 1024
    private val lock = Any()
    private val key: ByteArray by lazy {
        MessageDigest.getInstance("SHA-256")
            .digest("GuessTheNumber|game-data|archive-v1|2026".toByteArray(Charsets.UTF_8))
    }

    data class PrepareResult(val ready: Boolean, val version: String?, val message: String)

    fun prepare(context: Context): PrepareResult = synchronized(lock) {
        val app = context.applicationContext
        val root = File(app.filesDir, "game-data-cache")
        val versions = File(root, "versions")
        val pointer = File(root, "active-version")
        try {
            versions.mkdirs()
            val manifest = app.assets.open("$ROOT_ASSET/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
            require(manifest.optInt("schemaVersion") == 1 && manifest.optInt("archiveFormatVersion") == FORMAT_VERSION)
            require(manifest.optString("cipher") == "AES-256-GCM")
            val version = manifest.getString("version")
            require(version.matches(Regex("[a-zA-Z0-9._-]{1,80}")))
            val finalDir = File(versions, version)
            if (finalDir.isDirectory && filesValid(finalDir, manifest)) {
                activate(pointer, version, root)
                return PrepareResult(true, version, "Game data cache is current")
            }
            val staging = File(root, "staging-$version")
            staging.deleteRecursively()
            check(staging.mkdirs())
            try {
                val chunks = manifest.getJSONArray("chunks")
                require(chunks.length() in 1..512)
                var total = 0L
                for (index in 0 until chunks.length()) {
                    val entry = chunks.getJSONObject(index)
                    val id = entry.getString("id")
                    val name = entry.getString("file")
                    val group = entry.getString("group")
                    require(id.matches(Regex("[0-9]{2,4}")) && name == "$id.bin")
                    require(group.matches(Regex("[a-z-]{1,40}")))
                    val blob = app.assets.open("$ROOT_ASSET/$name").use { readBounded(it, MAX_CHUNK_BYTES) }
                    require(sha256(blob) == entry.getString("sha256"))
                    require(blob.size >= 37 && String(blob, 0, 8, Charsets.US_ASCII) == MAGIC)
                    require(blob[8].toInt() == FORMAT_VERSION)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, blob.copyOfRange(9, 21)))
                    cipher.updateAAD("$FORMAT_VERSION:$group:$id".toByteArray(Charsets.UTF_8))
                    ZipInputStream(cipher.doFinal(blob.copyOfRange(21, blob.size)).inputStream()).use { zip ->
                        while (true) {
                            val item = zip.nextEntry ?: break
                            require(safePath(item.name))
                            if (!item.isDirectory) {
                                val target = File(staging, item.name)
                                target.parentFile?.mkdirs()
                                var fileSize = 0L
                                target.outputStream().buffered().use { output ->
                                    val buffer = ByteArray(8192)
                                    while (true) {
                                        val count = zip.read(buffer)
                                        if (count < 0) break
                                        fileSize += count
                                        total += count
                                        require(fileSize <= MAX_EXTRACTED_BYTES && total <= MAX_EXTRACTED_BYTES)
                                        output.write(buffer, 0, count)
                                    }
                                }
                            }
                            zip.closeEntry()
                        }
                    }
                }
                require(filesValid(staging, manifest)) { "Extracted game data failed validation" }
                if (finalDir.exists()) finalDir.deleteRecursively()
                check(staging.renameTo(finalDir))
                activate(pointer, version, root)
                versions.listFiles()?.filter { it.isDirectory && it.name != version }
                    ?.sortedByDescending { it.lastModified() }?.drop(1)?.forEach { it.deleteRecursively() }
                PrepareResult(true, version, "Prepared " + manifest.getJSONArray("files").length() + " game-data files")
            } catch (error: Exception) {
                staging.deleteRecursively()
                throw error
            }
        } catch (error: Exception) {
            PrepareResult(false, activeVersion(pointer), "Using bundled fallback (" + error.javaClass.simpleName + ")")
        }
    }

    fun open(context: Context, relativePath: String): InputStream {
        require(safePath(relativePath))
        val app = context.applicationContext
        val root = File(app.filesDir, "game-data-cache")
        val active = activeVersion(File(root, "active-version"))?.let { File(root, "versions/$it") }
        val cached = active?.let { File(it, relativePath) }
        if (cached != null && cached.isFile && cached.canonicalPath.startsWith(active!!.canonicalPath + File.separator)) {
            return cached.inputStream()
        }
        return app.assets.open(relativePath)
    }

    fun pathHandler(context: Context, assetRoot: String): androidx.webkit.WebViewAssetLoader.PathHandler =
        androidx.webkit.WebViewAssetLoader.PathHandler { path ->
            val relative = "$assetRoot/" + path.trimStart('/')
            if (!safePath(relative)) return@PathHandler null
            val input = runCatching { open(context, relative) }.getOrNull() ?: return@PathHandler null
            WebResourceResponse(mimeType(relative), null, input)
        }

    private fun filesValid(root: File, manifest: JSONObject): Boolean = runCatching {
        val files = manifest.getJSONArray("files")
        if (files.length() !in 1..10000) return false
        for (index in 0 until files.length()) {
            val entry = files.getJSONObject(index)
            val path = entry.getString("path")
            if (!safePath(path)) return false
            val file = File(root, path)
            if (!file.isFile || file.length() != entry.getLong("size") || sha256(file) != entry.getString("sha256")) return false
        }
        true
    }.getOrDefault(false)

    private fun activate(pointer: File, version: String, root: File) {
        val temporary = File(root, "active-version.tmp")
        temporary.writeText(version)
        if (pointer.exists()) check(pointer.delete())
        check(temporary.renameTo(pointer))
    }

    private fun activeVersion(pointer: File): String? = runCatching {
        pointer.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.matches(Regex("[a-zA-Z0-9._-]{1,80}")) }
    }.getOrNull()

    private fun readBounded(input: InputStream, maxBytes: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= maxBytes)
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun safePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith('/') && !path.contains('\\') &&
            path.split('/').all { it.isNotBlank() && it != "." && it != ".." }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun sha256(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun mimeType(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "html" -> "text/html"; "js" -> "application/javascript"; "css" -> "text/css"
        "json" -> "application/json"; "svg" -> "image/svg+xml"; "ttf" -> "font/ttf"
        "woff" -> "font/woff"; "woff2" -> "font/woff2"; "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"; "webp" -> "image/webp"
        else -> "application/octet-stream"
    }
}
