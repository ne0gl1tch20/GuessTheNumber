package com.jarrlyyy.guessthenumber.data.repository

import com.jarrlyyy.guessthenumber.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Selects the update feed for this build channel; APK installation is handled separately. */
object UpdateChannelChecker {
    data class AvailableUpdate(
        val title: String,
        val url: String,
        val details: String,
        val downloadUrl: String,
        val expectedCommitSha: String? = null
    )

    sealed interface Result {
        data class Available(val update: AvailableUpdate) : Result
        data object UpToDate : Result
        data object Unavailable : Result
    }

    private const val REPOSITORY = "ne0gl1tch20/GuessTheNumber"
    private const val DEBUG_WORKFLOW = "stable-debug-apk.yml"
    private const val DEBUG_ARTIFACT_NAME = "GuessTheNumber-debug"

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.UPDATE_CHANNEL == "debug") checkDebugArtifact() else checkRelease()
        } catch (_: Exception) {
            Result.Unavailable
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

    /** Uses the latest successful, non-expired Stable Debug APK Actions artifact from master. */
    private fun checkDebugArtifact(): Result {
        val workflow = getJson("https://api.github.com/repos/$REPOSITORY/actions/workflows/$DEBUG_WORKFLOW/runs?status=success&per_page=20")
        val runs = workflow.optJSONArray("workflow_runs") ?: return Result.Unavailable
        val latest = (0 until runs.length())
            .mapNotNull { runs.optJSONObject(it) }
            .firstOrNull {
                it.optString("conclusion") == "success" &&
                    it.optString("head_branch") == "master" &&
                    it.optString("status") == "completed"
            } ?: return Result.Unavailable

        val runId = latest.optLong("id", -1L)
        val runNumber = latest.optInt("run_number", 0)
        val commit = latest.optString("head_sha")
        if (runId <= 0L || commit.isBlank()) return Result.Unavailable
        val artifactResponse = getJson("https://api.github.com/repos/$REPOSITORY/actions/runs/$runId/artifacts")
        val artifacts = artifactResponse.optJSONArray("artifacts") ?: return Result.Unavailable
        val artifact = (0 until artifacts.length())
            .mapNotNull { artifacts.optJSONObject(it) }
            .firstOrNull {
                it.optString("name") == DEBUG_ARTIFACT_NAME &&
                    !it.optBoolean("expired", true) &&
                    it.optLong("size_in_bytes", 0L) > 0L
            } ?: return Result.Unavailable

        val downloadUrl = artifact.optString("archive_download_url")
        if (downloadUrl.isBlank() || !isGithubUrl(downloadUrl)) return Result.Unavailable
        if (BuildConfig.GIT_COMMIT_SHA != "unknown" && commit == BuildConfig.GIT_COMMIT_SHA) return Result.UpToDate
        val htmlUrl = latest.optString("html_url", "https://github.com/$REPOSITORY/actions/workflows/$DEBUG_WORKFLOW")
        if (!isGithubUrl(htmlUrl)) return Result.Unavailable
        val buildGradle = getText("https://raw.githubusercontent.com/$REPOSITORY/$commit/app/build.gradle.kts")
        val versionName = Regex("versionName\\\\s*=\\\\s*\\\"([^\\\"]+)\\\"").find(buildGradle)?.groupValues?.getOrNull(1)
            ?: return Result.Unavailable
        val versionCode = Regex("versionCode\\\\s*=\\\\s*(\\\\d+)").find(buildGradle)?.groupValues?.getOrNull(1)
            ?: return Result.Unavailable
        val commitMessage = latest.optJSONObject("head_commit")?.optString("message").orEmpty().take(900)
        return Result.Available(
            AvailableUpdate(
                title = "v$versionName ($versionCode) · Debug #$runNumber",
                url = htmlUrl,
                details = listOf(commitMessage.ifBlank { "Stable Debug APK update" }, "Commit ${commit.take(7)} · ZIP artifact").joinToString("\n\n"),
                downloadUrl = downloadUrl,
                expectedCommitSha = commit
            )
        )
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
            if (connection.responseCode !in 200..299) error("Version metadata returned ${connection.responseCode}")
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
            if (connection.responseCode !in 200..299) error("Update endpoint returned ${connection.responseCode}")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

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
