package com.jarrlyyy.guessthenumber.data.repository

import com.jarrlyyy.guessthenumber.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Channel-aware update discovery. It only returns a verified source URL; installation is handled separately. */
object UpdateChannelChecker {
    data class AvailableUpdate(
        val title: String,
        val url: String,
        val details: String,
        val downloadUrl: String
    )

    sealed interface Result {
        data class Available(val update: AvailableUpdate) : Result
        data object UpToDate : Result
        data object Unavailable : Result
    }

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.UPDATE_CHANNEL == "debug") checkDebug() else checkRelease()
        } catch (_: Exception) {
            Result.Unavailable
        }
    }

    private fun checkRelease(): Result {
        val release = getJson("https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/releases/latest")
        val tag = release.optString("tag_name").removePrefix("v").removePrefix("V")
        if (tag.isBlank() || compareVersions(tag, BuildConfig.VERSION_NAME) <= 0) return Result.UpToDate

        val assets = release.optJSONArray("assets")
        val asset = (0 until (assets?.length() ?: 0))
            .mapNotNull { assets?.optJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) || it.optString("name").endsWith(".zip", ignoreCase = true) }
            ?: return Result.Unavailable
        val downloadUrl = asset.optString("browser_download_url")
        if (downloadUrl.isBlank()) return Result.Unavailable

        return Result.Available(
            AvailableUpdate(
                title = "v$tag",
                url = release.optString("html_url", "https://github.com/ne0gl1tch20/GuessTheNumber/releases/latest"),
                details = release.optString("body").take(1200),
                downloadUrl = downloadUrl
            )
        )
    }

    private fun checkDebug(): Result {
        val release = getJson("https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/releases/tags/debug-latest")
        val assets = release.optJSONArray("assets") ?: return Result.Unavailable
        val asset = (0 until assets.length())
            .mapNotNull { assets.optJSONObject(it) }
            .firstOrNull { it.optString("name") == "GuessTheNumber-debug.zip" }
            ?: return Result.Unavailable
        val downloadUrl = asset.optString("browser_download_url")
        if (downloadUrl.isBlank()) return Result.Unavailable

        val latestTag = release.optString("target_commitish")
        if (latestTag.isNotBlank() && BuildConfig.GIT_COMMIT_SHA != "unknown" && latestTag == BuildConfig.GIT_COMMIT_SHA) return Result.UpToDate

        return Result.Available(
            AvailableUpdate(
                title = "Debug #${release.optString("name", "latest")}",
                url = release.optString("html_url"),
                details = "Stable debug channel",
                downloadUrl = downloadUrl
            )
        )
    }

    private fun getJson(address: String): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 7000
        connection.readTimeout = 7000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", "GuessTheNumber-UpdateChecker")
        return try {
            if (connection.responseCode !in 200..299) error("Update endpoint returned ${connection.responseCode}")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

    internal fun compareVersions(left: String, right: String): Int {
        fun parts(value: String): List<Int> = Regex("\\d+").findAll(value).take(4).map { it.value.toIntOrNull() ?: 0 }.toList()
        val a = parts(left); val b = parts(right)
        for (index in 0 until maxOf(a.size, b.size)) {
            val first = a.getOrElse(index) { 0 }; val second = b.getOrElse(index) { 0 }
            if (first != second) return first.compareTo(second)
        }
        return 0
    }
}
