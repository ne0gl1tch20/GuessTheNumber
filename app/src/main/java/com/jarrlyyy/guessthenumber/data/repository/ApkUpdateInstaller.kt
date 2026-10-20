package com.jarrlyyy.guessthenumber.data.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.content.pm.PackageInstaller
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Downloads, validates and safely extracts an update before handing APK files to Android PackageInstaller. */
class ApkUpdateInstaller(private val context: Context) {
    companion object {
        private const val MAX_ARCHIVE_BYTES = 250L * 1024L * 1024L
        private const val MAX_EXTRACTED_BYTES = 400L * 1024L * 1024L
        private const val MAX_ENTRIES = 128
        private const val EXPECTED_DEBUG_APK = "GuessTheNumber-debug.apk"
    }

    fun install(downloadUrl: String, requireChecksum: Boolean = false, expectedCommitSha: String? = null): Result<Unit> {
        val source = runCatching { java.net.URI(downloadUrl) }.getOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid update URL."))
        if (source.scheme != "https" || source.host !in setOf("github.com", "api.github.com")) {
            return Result.failure(IllegalArgumentException("Updates must come from GitHub over HTTPS."))
        }
        val root = File(context.cacheDir, "updates").apply { mkdirs() }
        val archive = File(root, "update.zip")
        val directApk = File(root, "update.apk")
        val extracted = File(root, "extracted-${System.currentTimeMillis()}")
        return runCatching {
            if (URL(downloadUrl).path.lowercase().endsWith(".apk")) {
                require(!requireChecksum) { "The Stable Debug update must be a ZIP artifact." }
                download(downloadUrl, directApk)
                validateApk(directApk)
                installApks(listOf(directApk))
            } else {
                download(downloadUrl, archive)
                extractSafely(archive, extracted)
                val files = extracted.walkTopDown().filter { it.isFile }.toList()
                require(files.all { it.parentFile?.canonicalFile == extracted.canonicalFile }) {
                    "Update archive contains unexpected nested paths."
                }
                val apks = files.filter { it.extension.equals("apk", true) }
                require(apks.size == 1) { "Update archive must contain exactly one APK." }
                val apk = apks.single()
                val names = files.map { it.name }.toSet()
                require(names.size == files.size) { "Update archive contains duplicate file names." }
                require(names.all { it in setOf(apk.name, "SHA256SUMS", "update-manifest.json") }) {
                    "Update archive contains unexpected files."
                }
                if (requireChecksum) {
                    require(apk.name == EXPECTED_DEBUG_APK) { "Unexpected Stable Debug APK name." }
                    require(names == setOf(EXPECTED_DEBUG_APK, "SHA256SUMS", "update-manifest.json")) {
                        "Stable Debug artifact is missing required metadata."
                    }
                }
                if ("SHA256SUMS" in names) verifyChecksum(File(extracted, "SHA256SUMS"), apk)
                else require(!requireChecksum) { "Update checksum is missing." }
                if ("update-manifest.json" in names) verifyManifest(File(extracted, "update-manifest.json"), apk, expectedCommitSha)
                else require(!requireChecksum) { "Update manifest is missing." }
                validateApk(apk)
                installApks(listOf(apk))
            }
        }.also {
            archive.delete()
            directApk.delete()
            extracted.deleteRecursively()
        }
    }

    private fun download(address: String, destination: File) {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "GuessTheNumber-Updater")
        try {
            require(connection.responseCode in 200..299) { "Download failed: HTTP ${connection.responseCode}" }
            val length = connection.contentLengthLong
            require(length <= MAX_ARCHIVE_BYTES || length < 0) { "Update archive is too large." }
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    var total = 0L
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_ARCHIVE_BYTES) { "Update archive is too large." }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } finally { connection.disconnect() }
    }

    private fun extractSafely(archive: File, destination: File) {
        destination.mkdirs()
        val root = destination.canonicalFile
        var entries = 0
        var extractedBytes = 0L
        val seenNames = mutableSetOf<String>()
        ZipInputStream(FileInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                require(entries <= MAX_ENTRIES) { "Update archive contains too many files." }
                require(!entry.isDirectory) { "Directories are not accepted in the update archive." }
                require(seenNames.add(entry.name)) { "Update archive contains duplicate paths." }
                val target = File(destination, entry.name).canonicalFile
                require(target.path.startsWith(root.path + File.separator)) { "Unsafe ZIP path." }
                target.parentFile?.mkdirs()
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        extractedBytes += count
                        require(extractedBytes <= MAX_EXTRACTED_BYTES) { "Extracted update is too large." }
                        output.write(buffer, 0, count)
                    }
                }
            }
        }
    }

    private fun verifyChecksum(checksumFile: File, apk: File) {
        val lines = checksumFile.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
        require(lines.size == 1) { "Update checksum file is malformed." }
        val match = Regex("^([a-fA-F0-9]{64})\\s+\\*?([A-Za-z0-9._-]+)$").matchEntire(lines.single().trim())
            ?: error("Update checksum file is malformed.")
        require(match.groupValues[2] == apk.name) { "Update checksum names an unexpected file." }
        val actual = MessageDigest.getInstance("SHA-256").digest(apk.readBytes()).joinToString("") { "%02x".format(it) }
        require(actual.equals(match.groupValues[1], ignoreCase = true)) { "Update checksum does not match the APK." }
    }

    private fun verifyManifest(manifestFile: File, apk: File, expectedCommitSha: String?) {
        val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
        require(manifest.optString("packageName") == context.packageName) { "Update manifest package mismatch." }
        val manifestCommit = manifest.optString("commit")
        require(manifestCommit.matches(Regex("[a-fA-F0-9]{40}"))) { "Update manifest commit is invalid." }
        if (expectedCommitSha != null) require(manifestCommit.equals(expectedCommitSha, ignoreCase = true)) { "Artifact commit does not match the selected workflow run." }
        require(manifest.optString("versionName").isNotBlank()) { "Update manifest version is missing." }
        require(manifest.optLong("versionCode", -1L) > 0L) { "Update manifest version code is invalid." }
        val archiveInfo = packageInfoFor(apk)
        require(manifest.optString("versionName") == archiveInfo.versionName) { "APK version name does not match its manifest." }
        require(manifest.optLong("versionCode", -1L) == versionCode(archiveInfo)) { "APK version code does not match its manifest." }
    }

    private fun validateApk(apk: File): PackageInfo {
        val info = packageInfoFor(apk)
        require(info.packageName == context.packageName) { "APK package mismatch: ${info.packageName}" }
        require(!info.versionName.isNullOrBlank()) { "APK version name is missing." }
        val installedVersion = context.packageManager.getPackageInfo(context.packageName, 0)
        require(versionCode(info) >= versionCode(installedVersion)) { "This update is older than the installed version." }
        require(hasMatchingSignature(info)) { "APK signing certificate does not match the installed app." }
        return info
    }

    private fun packageInfoFor(apk: File): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: error("Invalid APK: ${apk.name}")
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun hasMatchingSignature(candidate: PackageInfo): Boolean {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val installedSignatures: Array<out Signature> = if (Build.VERSION.SDK_INT >= 28) {
            installed.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            installed.signatures ?: emptyArray()
        }
        val candidateSignatures: Array<out Signature> = if (Build.VERSION.SDK_INT >= 28) {
            candidate.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            candidate.signatures ?: emptyArray()
        }
        return installedSignatures.isNotEmpty() && installedSignatures.any { installedSig ->
            candidateSignatures.any { candidateSig -> sha256(installedSig) == sha256(candidateSig) }
        }
    }

    private fun sha256(signature: Signature): String =
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun installApks(apks: List<File>) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setSize(apks.sumOf { it.length() })
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apks.sortedWith(compareBy<File> { if (it.name.equals("base.apk", true)) 0 else 1 }.thenBy { it.name }).forEach { apk ->
                FileInputStream(apk).use { input ->
                    session.openWrite(apk.name, 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java).setAction(UpdateInstallReceiver.ACTION_RESULT)
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            session.commit(pending.intentSender)
        }
    }
}
