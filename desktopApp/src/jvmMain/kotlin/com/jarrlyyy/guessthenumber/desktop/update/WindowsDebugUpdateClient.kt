package com.jarrlyyy.guessthenumber.desktop.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

internal data class WindowsDebugUpdate(
    val version: String,
    val tagName: String,
    val downloadUrl: String,
    val manifestUrl: String,
    val runNumber: Long,
    val runAttempt: Long
)

/** Finds the latest Windows Debug package from the public debug-latest release. */
internal class WindowsDebugUpdateClient(
    private val connectionFactory: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    }
) {
    fun findLatestUpdate(installedVersion: String): WindowsDebugUpdate? {
        val release = getJson(URL(RELEASE_API)).jsonObject
        val assets = release["assets"]?.jsonArray ?: return null
        val assetObjects = assets.mapNotNull { it as? JsonObject }
        val archiveUrl = assetObjects.firstOrNull {
            it["name"]?.jsonPrimitive?.content == WINDOWS_ARCHIVE_NAME
        }?.get("browser_download_url")?.jsonPrimitive?.content ?: return null
        val manifestUrl = assetObjects.firstOrNull {
            it["name"]?.jsonPrimitive?.content == WINDOWS_MANIFEST_NAME
        }?.get("browser_download_url")?.jsonPrimitive?.content ?: return null
        if (!isReleaseAssetUrl(archiveUrl) || !isReleaseAssetUrl(manifestUrl)) return null

        val manifest = getJson(URL(manifestUrl)).jsonObject
        if (manifest["channel"]?.jsonPrimitive?.content != "windows-debug") return null
        if (manifest["assetName"]?.jsonPrimitive?.content != WINDOWS_ARCHIVE_NAME) return null
        val version = manifest["version"]?.jsonPrimitive?.content ?: return null
        val runNumber = manifest["runNumber"]?.jsonPrimitive?.content?.toLongOrNull() ?: return null
        val runAttempt = manifest["runAttempt"]?.jsonPrimitive?.content?.toLongOrNull() ?: return null
        val tag = manifest["tag"]?.jsonPrimitive?.content ?: return null
        if (runNumber <= 0L || runAttempt <= 0L || tag != "windows-debug-$runNumber-$runAttempt") return null
        if (!version.matches(Regex("1\\.0\\.\\d+")) || compareVersions(version, installedVersion) <= 0) return null

        return WindowsDebugUpdate(
            version = version,
            tagName = tag,
            downloadUrl = archiveUrl,
            manifestUrl = manifestUrl,
            runNumber = runNumber,
            runAttempt = runAttempt
        )
    }

    private fun isReleaseAssetUrl(value: String): Boolean =
        value.startsWith(RELEASE_DOWNLOAD_PREFIX) && !value.contains("..")

    private fun getJson(url: URL) = connectionFactory(url).run {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 15_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "GuessTheNumber-Windows-Debug-Updater")
        try {
            if (responseCode !in 200..299) {
                throw IllegalStateException("GitHub returned HTTP $responseCode")
            }
            val body = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (body.length > MAX_RESPONSE_CHARS) {
                throw IllegalStateException("GitHub response exceeded the size limit")
            }
            Json.parseToJsonElement(body)
        } finally {
            disconnect()
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split('.').map { it.toIntOrNull() ?: return left.compareTo(right) }
        val rightParts = right.split('.').map { it.toIntOrNull() ?: return left.compareTo(right) }
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val comparison = (leftParts.getOrElse(index) { 0 })
                .compareTo(rightParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private companion object {
        const val RELEASE_API =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/releases/tags/debug-latest"
        const val RELEASE_DOWNLOAD_PREFIX =
            "https://github.com/ne0gl1tch20/GuessTheNumber/releases/download/debug-latest/"
        const val WINDOWS_ARCHIVE_NAME = "GuessTheNumber-Windows-Desktop.zip"
        const val WINDOWS_MANIFEST_NAME = "windows-debug-manifest.json"
        const val MAX_RESPONSE_CHARS = 1_000_000
    }
}
