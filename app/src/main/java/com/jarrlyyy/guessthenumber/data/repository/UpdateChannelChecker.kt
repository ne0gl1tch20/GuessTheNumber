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

    private const val REPOSITORY = "ne0gl1tch20/GuessTheNumber"

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.UPDATE_CHANNEL == "debug") checkDebugArtifact() else checkRelease()
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

    /** Checks the newest successful Stable Debug workflow artifact, not the master-only Release. */
    private fun checkDebugArtifact(): Result {
        val runsResponse = getJson(
            "https://api.github.com/repos/$REPOSITORY/actions/workflows/stable-debug-apk.yml/runs?status=success&per_page=100"
        )
        val runs = runsResponse.optJSONArray("workflow_runs") ?: return Result.Unavailable
        val successfulRuns = (0 until runs.length())
            .mapNotNull { runs.optJSONObject(it) }
            .filter {
                it.optString("path") == ".github/workflows/stable-debug-apk.yml" &&
                    it.optString("status") == "completed" &&
                    it.optString("conclusion") == "success"
            }
            .sortedByDescending { it.optLong("run_number", 0L) }

        for (run in successfulRuns) {
            val runId = run.optLong("id", 0L)
            val runNumber = run.optLong("run_number", 0L)
            val targetCommit = run.optString("head_sha")
            if (runId <= 0L || runNumber <= 0L ||
                !targetCommit.matches(Regex("[a-fA-F0-9]{40}"))
            ) continue

            val artifactsResponse = runCatching {
                getJson("https://api.github.com/repos/$REPOSITORY/actions/runs/$runId/artifacts")
            }.getOrNull() ?: continue
            val artifacts = artifactsResponse.optJSONArray("artifacts") ?: continue
            val artifact = (0 until artifacts.length())
                .mapNotNull { artifacts.optJSONObject(it) }
                .firstOrNull {
                    it.optString("name") == "GuessTheNumber-debug" &&
                        !it.optBoolean("expired", true)
                } ?: continue

            val downloadUrl = artifact.optString("archive_download_url")
            val pageUrl = run.optString("html_url")
            if (!isGithubUrl(downloadUrl) || !isGithubUrl(pageUrl)) continue

            val installedBuildNumber = BuildConfig.STABLE_DEBUG_BUILD_NUMBER.toLong()
            if (installedBuildNumber > 0L && runNumber <= installedBuildNumber) {
                return Result.UpToDate
            }

            if (installedBuildNumber <= 0L &&
                BuildConfig.GIT_COMMIT_SHA.matches(Regex("[a-fA-F0-9]{40}")) &&
                targetCommit.equals(BuildConfig.GIT_COMMIT_SHA, ignoreCase = true)
            ) {
                return Result.UpToDate
            }

            val buildGradle = runCatching {
                getText("https://raw.githubusercontent.com/$REPOSITORY/$targetCommit/app/build.gradle.kts")
            }.getOrNull() ?: return Result.Unavailable
            val versionName = Regex("appVersionName\\s*=\\s*\"([^\"]+)\"")
                .find(buildGradle)?.groupValues?.getOrNull(1) ?: return Result.Unavailable
            val versionCode = Regex("appVersionCode\\s*=\\s*(\\d+)")
                .find(buildGradle)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return Result.Unavailable
            if (versionCode < BuildConfig.VERSION_CODE.toLong()) return Result.UpToDate

            val installedCommit = BuildConfig.GIT_COMMIT_SHA
            val commitOrder = if (installedBuildNumber <= 0L &&
                installedCommit.matches(Regex("[a-fA-F0-9]{40}"))
            ) {
                runCatching {
                    getJson("https://api.github.com/repos/$REPOSITORY/compare/$targetCommit...$installedCommit")
                        .optString("status")
                }.getOrNull()
            } else null
            if (installedBuildNumber <= 0L && commitOrder in setOf("ahead", "identical")) {
                return Result.UpToDate
            }

            val title = "Stable Debug #$runNumber"
            val details = buildString {
                appendLine("Stable Debug #$runNumber")
                appendLine("Version: $versionName ($versionCode)")
                append("Commit: $targetCommit")
            }
            return Result.Available(
                AvailableUpdate(
                    title = "$title · v$versionName ($versionCode)",
                    url = pageUrl,
                    details = details,
                    downloadUrl = downloadUrl,
                    expectedCommitSha = targetCommit,
                    requireChecksum = true
                )
            )
        }

        return Result.Unavailable
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


    internal fun shouldTreatSameCommitAsCurrent(
        remoteBuildNumber: Long?,
        installedBuildNumber: Long,
        sameVersionAndCommit: Boolean
    ): Boolean {
        val hasComparableBuildNumbers = remoteBuildNumber != null && installedBuildNumber > 0L
        return sameVersionAndCommit && !hasComparableBuildNumbers
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
