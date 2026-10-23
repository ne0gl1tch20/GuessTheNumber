package com.jarrlyyy.guessthenumber.data.repository

import com.jarrlyyy.guessthenumber.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** Selects the update feed for this build channel; APK installation is handled separately. */
object UpdateChannelChecker {
    data class AvailableUpdate(
        val title: String,
        val url: String,
        val details: String,
        val downloadUrl: String,
        val expectedCommitSha: String? = null,
        val requireChecksum: Boolean = false
    )

    sealed interface Result {
        data class Available(val update: AvailableUpdate) : Result
        data object UpToDate : Result
        data object Unavailable : Result
        data class Failed(val diagnostics: String) : Result
    }

    internal data class StableDebugRelease(
        val runNumber: Long,
        val versionName: String,
        val versionCode: Long,
        val commitSha: String
    )

    private const val REPOSITORY = "ne0gl1tch20/GuessTheNumber"
    private const val STABLE_DEBUG_TAG = "debug-latest"
    private const val STABLE_DEBUG_ARCHIVE = "GuessTheNumber-debug.zip"

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.UPDATE_CHANNEL == "debug") checkDebugReleaseAsset() else checkRelease()
        } catch (error: Exception) {
            Result.Failed(
                buildString {
                    appendLine("Update check failed")
                    appendLine("Time: ${Instant.now()}")
                    appendLine("Channel: ${BuildConfig.UPDATE_CHANNEL}")
                    appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Failure: ${error.javaClass.name}")
                    appendLine("Message: ${error.message ?: "(no message)"}")
                    appendLine()
                    append(error.stackTraceToString().take(8000))
                }.trim()
            )
        }
    }

    private fun checkRelease(): Result {
        val release = getJson("https://api.github.com/repos/$REPOSITORY/releases/latest")
        val tag = release.optString("tag_name").removePrefix("v").removePrefix("V")
        if (tag.isBlank() || compareVersions(tag, BuildConfig.VERSION_NAME) <= 0) return Result.UpToDate

        val assets = release.optJSONArray("assets")
        val asset = (0 until (assets?.length() ?: 0))
            .mapNotNull { assets?.optJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) || it.optString("name").endsWith(".zip", ignoreCase = true) }
            ?: return Result.Unavailable
        val downloadUrl = asset.optString("browser_download_url")
        val pageUrl = release.optString("html_url", "https://github.com/$REPOSITORY/releases/latest")
        if (!isGithubUrl(downloadUrl) || !isGithubUrl(pageUrl)) return Result.Unavailable

        return Result.Available(
            AvailableUpdate(
                title = "v$tag",
                url = pageUrl,
                details = release.optString("body").take(1200),
                downloadUrl = downloadUrl
            )
        )
    }

    /**
     * Stable Debug already publishes a replaceable public ZIP asset on the debug-latest
     * release. Downloading Actions artifacts through the REST API requires authentication,
     * so use that public release asset instead and keep validating its embedded checksum
     * and manifest before installation.
     */
    private fun checkDebugReleaseAsset(): Result {
        val release = getJson("https://api.github.com/repos/$REPOSITORY/releases/tags/$STABLE_DEBUG_TAG")
        val pageUrl = release.optString("html_url")
        val assets = release.optJSONArray("assets") ?: return Result.Unavailable
        val asset = (0 until assets.length())
            .mapNotNull { assets.optJSONObject(it) }
            .firstOrNull { it.optString("name") == STABLE_DEBUG_ARCHIVE }
            ?: return Result.Unavailable
        val downloadUrl = asset.optString("browser_download_url")
        if (!isGithubUrl(pageUrl) || !isGithubUrl(downloadUrl)) return Result.Unavailable

        val metadata = parseStableDebugReleaseMetadata(release.optString("body"))
            ?: return Result.Unavailable
        val installedBuildNumber = BuildConfig.STABLE_DEBUG_BUILD_NUMBER.toLong()
        if (isStableDebugBuildCurrent(metadata.runNumber, installedBuildNumber)) {
            return Result.UpToDate
        }
        if (installedBuildNumber <= 0L &&
            BuildConfig.GIT_COMMIT_SHA.matches(Regex("[a-fA-F0-9]{40}")) &&
            metadata.commitSha.equals(BuildConfig.GIT_COMMIT_SHA, ignoreCase = true)
        ) {
            return Result.UpToDate
        }
        if (metadata.versionCode < BuildConfig.VERSION_CODE.toLong()) return Result.UpToDate

        val installedCommit = BuildConfig.GIT_COMMIT_SHA
        val commitOrder = if (installedBuildNumber <= 0L &&
            installedCommit.matches(Regex("[a-fA-F0-9]{40}"))
        ) {
            runCatching {
                getJson("https://api.github.com/repos/$REPOSITORY/compare/${metadata.commitSha}...$installedCommit")
                    .optString("status")
            }.getOrNull()
        } else null
        if (installedBuildNumber <= 0L && commitOrder in setOf("ahead", "identical")) {
            return Result.UpToDate
        }

        val details = buildString {
            appendLine("Stable Debug #${metadata.runNumber}")
            appendLine("Version: ${metadata.versionName} (${metadata.versionCode})")
            append("Commit: ${metadata.commitSha}")
        }
        return Result.Available(
            AvailableUpdate(
                title = "Stable Debug #${metadata.runNumber} · v${metadata.versionName} (${metadata.versionCode})",
                url = pageUrl,
                details = details,
                downloadUrl = downloadUrl,
                expectedCommitSha = metadata.commitSha,
                requireChecksum = true
            )
        )
    }

    internal fun parseStableDebugReleaseMetadata(body: String): StableDebugRelease? {
        val runNumber = Regex("(?m)^Stable Debug #(\\d+)\\s*$")
            .find(body)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
        val version = Regex("(?m)^Version: ([^\\s]+) \\((\\d+)\\)\\s*$")
            .find(body) ?: return null
        val versionName = version.groupValues[1]
        val versionCode = version.groupValues[2].toLongOrNull() ?: return null
        val commitSha = Regex("(?m)^Commit: ([a-fA-F0-9]{40})\\s*$")
            .find(body)?.groupValues?.getOrNull(1) ?: return null
        if (versionName.isBlank() || versionCode <= 0L || runNumber <= 0L) return null
        return StableDebugRelease(runNumber, versionName, versionCode, commitSha)
    }

    private fun isGithubUrl(address: String): Boolean {
        val uri = runCatching { java.net.URI(address) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host in setOf("github.com", "api.github.com", "raw.githubusercontent.com")
    }

    private fun getText(address: String): String {
        require(isGithubUrl(address)) { "Metadata must come from GitHub over HTTPS." }
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 7000
        connection.readTimeout = 7000
        connection.setRequestProperty("User-Agent", "GuessTheNumber-UpdateChecker")
        return try {
            if (connection.responseCode !in 200..299) error("GET $address returned HTTP ${connection.responseCode} while reading version metadata")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
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
            if (connection.responseCode !in 200..299) error("GET $address returned HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

    internal fun isStableDebugBuildCurrent(remoteBuildNumber: Long, installedBuildNumber: Long): Boolean =
        installedBuildNumber > 0L && remoteBuildNumber <= installedBuildNumber

    internal fun compareVersions(left: String, right: String): Int {
        fun parts(value: String): List<Int> = Regex("\\d+").findAll(value).take(4).map { it.value.toIntOrNull() ?: 0 }.toList()
        val a = parts(left)
        val b = parts(right)
        for (index in 0 until maxOf(a.size, b.size)) {
            val first = a.getOrElse(index) { 0 }
            val second = b.getOrElse(index) { 0 }
            if (first != second) return first.compareTo(second)
        }
        return 0
    }
}
