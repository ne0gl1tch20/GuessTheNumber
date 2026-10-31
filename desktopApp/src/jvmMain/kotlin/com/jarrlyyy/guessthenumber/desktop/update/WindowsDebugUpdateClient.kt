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
    val archiveDownloadUrl: String,
    val manifestDownloadUrl: String,
    val archiveSha256: String,
    val commitSha: String,
    val runNumber: Long,
    val runAttempt: Long
)

/** Reads public assets from the replaceable debug-latest GitHub Release. */
internal class WindowsDebugUpdateClient(
    private val connectionFactory: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    }
) {
    fun findLatestUpdate(installedVersion: String): WindowsDebugUpdate? {
        val release = getJson(URL(RELEASE_API)).jsonObject
        val assets = release["assets"]?.jsonArray ?: return null
        val assetUrls = assets.mapNotNull { it as? JsonObject }
            .mapNotNull { asset ->
                val name = asset["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val url = asset["browser_download_url"]?.jsonPrimitive?.content ?: return@mapNotNull null
                name to url
            }.toMap()

        val archiveUrl = assetUrls[WINDOWS_ARCHIVE_NAME]
            ?: throw IllegalStateException("The latest debug-latest release has no Windows Debug installer archive.")
        val manifestUrl = assetUrls[WINDOWS_MANIFEST_NAME]
            ?: throw IllegalStateException("The latest debug-latest release has no Windows Debug update manifest.")
        require(isPublicReleaseAssetUrl(archiveUrl) && isPublicReleaseAssetUrl(manifestUrl)) {
            "GitHub returned an unexpected debug-latest release asset URL."
        }

        val manifest = getJson(URL(manifestUrl)).jsonObject
        require(manifest["channel"]?.jsonPrimitive?.content == "windows-debug") {
            "The debug-latest Windows asset is not a Windows Debug build."
        }
        val version = manifest["version"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("The Windows Debug release manifest has no version.")
        require(version.matches(Regex("1\\.0\\.\\d+"))) {
            "The latest Windows Debug release has an incompatible version."
        }
        val runNumber = manifest["runNumber"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: throw IllegalStateException("The Windows Debug release manifest has no run number.")
        val runAttempt = manifest["runAttempt"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: throw IllegalStateException("The Windows Debug release manifest has no run attempt.")
        val commitSha = manifest["commit"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("The Windows Debug release manifest has no commit SHA.")
        require(commitSha.matches(Regex("[0-9a-fA-F]{40}"))) {
            "The Windows Debug release manifest has an invalid commit SHA."
        }
        require(manifest["assetName"]?.jsonPrimitive?.content == WINDOWS_ARCHIVE_NAME) {
            "The Windows Debug release manifest names an unexpected archive."
        }
        val checksum = manifest["sha256"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("The Windows Debug release manifest has no SHA-256 checksum.")
        require(checksum.matches(Regex("[0-9a-fA-F]{64}"))) {
            "The Windows Debug release manifest has an invalid SHA-256 checksum."
        }
        val tagName = manifest["tag"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("The Windows Debug release manifest has no build tag.")

        if (compareVersions(version, installedVersion) <= 0) return null
        return WindowsDebugUpdate(
            version = version,
            tagName = tagName,
            archiveDownloadUrl = archiveUrl,
            manifestDownloadUrl = manifestUrl,
            archiveSha256 = checksum,
            commitSha = commitSha,
            runNumber = runNumber,
            runAttempt = runAttempt
        )
    }

    private fun isPublicReleaseAssetUrl(value: String): Boolean {
        val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host == "github.com" &&
            uri.path.startsWith("/$REPOSITORY/releases/download/$DEBUG_RELEASE_TAG/")
    }

    private fun getJson(url: URL) = connectionFactory(url).run {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 20_000
        instanceFollowRedirects = true
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "GuessTheNumber-Windows-Debug-Updater")
        try {
            if (responseCode !in 200..299) {
                throw IllegalStateException("GitHub release update check returned HTTP $responseCode")
            }
            val body = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (body.length > MAX_RESPONSE_CHARS) {
                throw IllegalStateException("GitHub release response exceeded the size limit")
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
            val comparison = leftParts.getOrElse(index) { 0 }
                .compareTo(rightParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private companion object {
        const val REPOSITORY = "ne0gl1tch20/GuessTheNumber"
        const val DEBUG_RELEASE_TAG = "debug-latest"
        const val RELEASE_API =
            "https://api.github.com/repos/$REPOSITORY/releases/tags/$DEBUG_RELEASE_TAG"
        const val WINDOWS_ARCHIVE_NAME = "GuessTheNumber-Windows-Desktop.zip"
        const val WINDOWS_MANIFEST_NAME = "windows-debug-manifest.json"
        const val MAX_RESPONSE_CHARS = 1_000_000
    }
}
