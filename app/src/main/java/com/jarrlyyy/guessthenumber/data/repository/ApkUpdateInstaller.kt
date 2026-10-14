package com.jarrlyyy.guessthenumber.data.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.content.pm.PackageInstaller
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Downloads, validates, safely extracts and hands APKs to Android PackageInstaller. */
class ApkUpdateInstaller(private val context: Context) {
    companion object {
        private const val MAX_ARCHIVE_BYTES = 250L * 1024L * 1024L
        private const val MAX_EXTRACTED_BYTES = 400L * 1024L * 1024L
        private const val MAX_ENTRIES = 128
    }

    fun install(downloadUrl: String): Result<Unit> {
        val root = File(context.cacheDir, "updates").apply { mkdirs() }
        val archive = File(root, "update.zip")
        val extracted = File(root, "extracted-${System.currentTimeMillis()}")
        return runCatching {
            download(downloadUrl, archive)
            extractSafely(archive, extracted)
            val apks = extracted.walkTopDown().filter { it.isFile && it.extension.equals("apk", true) }.toList()
            require(apks.isNotEmpty()) { "No APK found in update archive." }
            require(apks.size <= 32) { "Too many APK candidates." }
            apks.forEach(::validateApk)
            installApks(apks)
        }.also {
            archive.delete()
            extracted.deleteRecursively()
        }
    }

    private fun download(url: String, destination: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
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
        ZipInputStream(FileInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                require(entries <= MAX_ENTRIES) { "Update archive contains too many files." }
                require(!entry.isDirectory) { "Directories are not accepted in the update archive." }
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

    private fun validateApk(apk: File) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: error("Invalid APK: ${apk.name}")
        require(info.packageName == context.packageName) { "APK package mismatch: ${info.packageName}" }
        require(hasMatchingSignature(info)) { "APK signing certificate does not match the installed app." }
    }

    private fun hasMatchingSignature(candidate: PackageInfo): Boolean {
        val installed = context.packageManager.getPackageInfo(context.packageName, if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
        val installedSignatures = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo.apkContentsSigners else @Suppress("DEPRECATION") installed.signatures
        val candidateSignatures = if (Build.VERSION.SDK_INT >= 28) candidate.signingInfo.apkContentsSigners else @Suppress("DEPRECATION") candidate.signatures
        return installedSignatures.any { installedSig -> candidateSignatures.any { candidateSig -> sha256(installedSig) == sha256(candidateSig) } }
    }

    private fun sha256(signature: Signature): String = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }

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
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            session.commit(pending.intentSender)
        }
    }
}
