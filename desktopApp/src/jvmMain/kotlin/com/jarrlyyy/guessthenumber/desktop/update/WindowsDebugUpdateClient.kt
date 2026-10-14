package com.jarrlyyy.guessthenumber.desktop.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
    val sha256: String
)

/**
 * Discovers public Windows Debug prereleases. It deliberately uses GitHub's public
 * Releases API rather than private Actions artifact endpoints, so the app needs no token.
 */
internal class WindowsDebugUpdateClient(
    private val connectionFactory: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    }
) {
    fun findLatestUpdate(installedVersion: String): WindowsDebugUpdate? {
        val releases = getJson(URL(RELEASES_API)).jsonArray
        val latest = releases
            .mapNotNull { it as? JsonObject }
            .filter { it["prerelease"]?.jsonPrimitive?.content == "true" }
            .mapNotNull { release ->
                val tag = release["tag_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val order = debugTagOrder(tag) ?: return@mapNotNull null
                Triple(release, tag, order)
            }
            .maxByOrNull { it.third }
            ?: return null

        val assets = latest.first.assets()
        val manifestAsset = assets.mapNotNull { it as? JsonObject }.firstOrNull {
            it["name"]?.jsonPrimitive?.content == MANIFEST_ASSET
        } ?: return null
        val manifestUrl = manifestAsset["browser_download_url"]?.jsonPrimitive?.content ?: return null
        val manifest = getJson(URL(manifestUrl)).jsonObject
        val version = manifest["version"]?.jsonPrimitive?.content ?: return null
        val assetName = manifest["assetName"]?.jsonPrimitive?.content ?: return null
        val checksum = manifest["sha256"]?.jsonPrimitive?.content?.lowercase() ?: return null
        if (!CHECKSUM_REGEX.matches(checksum)) return null
        if (manifest["channel"]?.jsonPrimitive?.content != "windows-debug") return null
        if (manifest["tag"]?.jsonPrimitive?.content != latest.second) return null
        if (compareVersions(version, installedVersion) <= 0) return null

        val installerAsset = assets.mapNotNull { it as? JsonObject }.firstOrNull {
            it["name"]?.jsonPrimitive?.content == assetName
        } ?: return null
        val downloadUrl = installerAsset["browser_download_url"]?.jsonPrimitive?.content ?: return null
        if (!downloadUrl.startsWith("https://github.com/")) return null

        return WindowsDebugUpdate(
            version = version,
            tagName = latest.second,
            downloadUrl = downloadUrl,
            sha256 = checksum
        )
    }

    private fun JsonObject.assets(): JsonArray = this["assets"]?.jsonArray ?: JsonArray(emptyList())

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

    private fun debugTagOrder(tag: String): Long? {
        val match = DEBUG_TAG_REGEX.matchEntire(tag) ?: return null
        val run = match.groupValues[1].toLongOrNull() ?: return null
        val attempt = match.groupValues[2].toLongOrNull() ?: return null
        return run * 1_000_000L + attempt
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
        const val RELEASES_API =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/releases?per_page=100"
        const val MANIFEST_ASSET = "windows-debug-manifest.json"
        const val MAX_RESPONSE_CHARS = 1_000_000
        val DEBUG_TAG_REGEX = Regex("windows-debug-(\\d+)-(\\d+)")
        val CHECKSUM_REGEX = Regex("[0-9a-f]{64}")
    }
}
