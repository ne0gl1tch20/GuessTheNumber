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
    private const val DEBUG_RELEASE_TAG = "debug-latest"

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.UPDATE_CHANNEL == "debug") checkDebugRelease() else checkRelease()
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

    /** Uses the public debug-latest release asset instead of private Actions artifact downloads. */
    private fun checkDebugRelease(): Result {
        val release = getJson("https://api.github.com/repos/$REPOSITORY/releases/tags/$DEBUG_RELEASE_TAG")
        val targetCommit = resolveDebugReleaseCommit(release) ?: return Result.Unavailable

        val assets = release.optJSONArray("assets")
        val asset = (0 until (assets?.length() ?: 0))
            .mapNotNull { assets?.optJSONObject(it) }
            .firstOrNull { it.optString("name") == "GuessTheNumber-debug.zip" }
            ?: return Result.Unavailable
        val downloadUrl = asset.optString("browser_download_url")
        val pageUrl = release.optString("html_url", "https://github.com/$REPOSITORY/releases/tag/$DEBUG_RELEASE_TAG")
        if (!isGithubUrl(downloadUrl) || !isGithubUrl(pageUrl)) return Result.Unavailable

        val buildGradle = getText("https://raw.githubusercontent.com/$REPOSITORY/$targetCommit/app/build.gradle.kts")
        val versionName = Regex("versionName\\s*=\\s*\"([^\"]+)\"").find(buildGradle)?.groupValues?.getOrNull(1)
            ?: return Result.Unavailable
        val versionCode = Regex("versionCode\\s*=\\s*(\\d+)").find(buildGradle)?.groupValues?.getOrNull(1)?.toLongOrNull()
            ?: return Result.Unavailable
        val currentVersionCode = BuildConfig.VERSION_CODE.toLong()
        val body = release.optString("body").trim()
        val remoteBuildNumber = Regex("(?m)^Stable Debug #(\\d+)").find(body)
            ?.groupValues?.getOrNull(1)?.toLongOrNull()
        val installedCommit = BuildConfig.GIT_COMMIT_SHA
        val installedBuildNumber = BuildConfig.STABLE_DEBUG_BUILD_NUMBER.toLong()

        if (versionCode < currentVersionCode) return Result.UpToDate

        // When both build numbers are available, they are authoritative: rerunning
        // the workflow can publish #78 from the same source commit as #77.
        val hasComparableBuildNumbers = remoteBuildNumber != null && installedBuildNumber > 0L
        val sameVersionAndCommit = versionCode == currentVersionCode &&
            installedCommit != "unknown" &&
            targetCommit.equals(installedCommit, ignoreCase = true)
        if (shouldTreatSameCommitAsCurrent(remoteBuildNumber, installedBuildNumber, sameVersionAndCommit)) {
            return Result.UpToDate
        }

        // Commit ancestry is the fallback for older installs that predate embedded
        // build numbers. Do not let an identical commit hide a newer numbered build.
        val commitOrder = if (!hasComparableBuildNumbers &&
            installedCommit.matches(Regex("[a-fA-F0-9]{40}"))
        ) {
            runCatching {
                getJson("https://api.github.com/repos/$REPOSITORY/compare/$targetCommit...$installedCommit")
                    .optString("status")
            }.getOrNull()
        } else null

        val alreadyCurrent = if (hasComparableBuildNumbers) {
            isStableDebugBuildCurrent(remoteBuildNumber!!, installedBuildNumber)
        } else {
            when (commitOrder) {
                "ahead", "identical" -> true // installed commit is at least as new as the release
                "behind" -> false // the release commit is newer than the installed commit
                else -> remoteBuildNumber != null &&
                    isStableDebugBuildCurrent(remoteBuildNumber, installedBuildNumber)
            }
        }
        if (alreadyCurrent) return Result.UpToDate

        // When ancestry cannot be checked, retain the legacy version-code guard.
        if (commitOrder == null && versionCode < BuildConfig.VERSION_CODE) return Result.UpToDate
        if (commitOrder == null && remoteBuildNumber == null &&
            versionCode == BuildConfig.VERSION_CODE.toLong() &&
            installedCommit != "unknown" && targetCommit.equals(installedCommit, ignoreCase = true)
        ) return Result.UpToDate

        val title = remoteBuildNumber?.let { "Stable Debug #$it" } ?: "Stable Debug"
        val details = if (body.isNotBlank()) body.take(1200) else "Stable Debug v$versionName ($versionCode)"
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

    /**
     * GitHub's release target_commitish is a branch/commit hint, not guaranteed to be
     * a commit SHA. Resolve the actual release tag ref when that hint is not a SHA.
     */
    private fun resolveDebugReleaseCommit(release: JSONObject): String? {
        val target = release.optString("target_commitish")
        if (target.matches(Regex("[a-fA-F0-9]{40}"))) return target

        val tagRef = getJson("https://api.github.com/repos/$REPOSITORY/git/ref/tags/$DEBUG_RELEASE_TAG")
        val tagObject = tagRef.optJSONObject("object") ?: return null
        val objectSha = tagObject.optString("sha")
        if (!objectSha.matches(Regex("[a-fA-F0-9]{40}"))) return null

        return when (tagObject.optString("type")) {
            "commit" -> objectSha
            "tag" -> {
                val annotatedTag = getJson("https://api.github.com/repos/$REPOSITORY/git/tags/$objectSha")
                val targetObject = annotatedTag.optJSONObject("object") ?: return null
                targetObject.optString("sha")
                    .takeIf { targetObject.optString("type") == "commit" && it.matches(Regex("[a-fA-F0-9]{40}")) }
            }
            else -> null
        }
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
