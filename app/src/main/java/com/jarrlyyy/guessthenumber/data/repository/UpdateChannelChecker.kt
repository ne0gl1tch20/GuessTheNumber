package com.jarrlyyy.guessthenumber.data.repository

import com.jarrlyyy.guessthenumber.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Discovers updates without embedding credentials or downloading executable files.
 * Release and Debug builds intentionally use separate feeds.
 */
object UpdateChannelChecker {
    data class AvailableUpdate(
        val title: String,
        val url: String,
        val details: String
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
        if (tag.isBlank() || compareVersions(tag, BuildConfig.VERSION_NAME) <= 0) {
            return Result.UpToDate
        }

        val releaseUrl = release.optString(
            "html_url",
            "https://github.com/ne0gl1tch20/GuessTheNumber/releases/latest"
        )
        return Result.Available(
            AvailableUpdate(
                title = "v$tag",
                url = releaseUrl,
                details = release.optString("body").take(1200)
            )
        )
    }

    private fun checkDebug(): Result {
        val runs = getJson(
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/workflows/stable-debug-apk.yml/runs?status=success&per_page=10"
        ).optJSONArray("workflow_runs") ?: return Result.Unavailable

        for (index in 0 until runs.length()) {
            val run = runs.optJSONObject(index) ?: continue
            if (run.optString("status") != "completed" || run.optString("conclusion") != "success") continue

            val runId = run.optLong("id")
            if (runId <= 0L) continue
            val artifacts = getJson(
                "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/runs/$runId/artifacts"
            ).optJSONArray("artifacts") ?: continue
            val hasStableApk = (0 until artifacts.length()).any { artifactIndex ->
                val artifact = artifacts.optJSONObject(artifactIndex) ?: return@any false
                !artifact.optBoolean("expired", true) &&
                    artifact.optString("name").startsWith("GuessTheNumber-debug-")
            }
            if (!hasStableApk) continue

            val builtSha = run.optString("head_sha")
            val installedSha = BuildConfig.GIT_COMMIT_SHA
            if (installedSha != "unknown" && builtSha == installedSha) return Result.UpToDate

            return Result.Available(
                AvailableUpdate(
                    title = "#${run.optLong("run_number")}",
                    url = run.optString("html_url"),
                    details = ""
                )
            )
        }
        return Result.Unavailable
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
        fun parts(value: String): List<Int> =
            Regex("\\d+").findAll(value).take(4).map { it.value.toIntOrNull() ?: 0 }.toList()

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
