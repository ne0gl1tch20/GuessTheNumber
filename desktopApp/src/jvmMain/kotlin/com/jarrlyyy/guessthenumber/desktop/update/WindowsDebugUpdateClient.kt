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
    val runNumber: Long,
    val runAttempt: Long
)

/**
 * Finds the newest successful Windows Desktop CI artifact from pushes to master.
 * Actions artifacts are public but short-lived, so the app never needs a GitHub token.
 */
internal class WindowsDebugUpdateClient(
    private val connectionFactory: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    }
) {
    fun findLatestUpdate(installedVersion: String): WindowsDebugUpdate? {
        val runs = getJson(URL(WORKFLOW_RUNS_API)).jsonObject["workflow_runs"]?.jsonArray
            ?: return null

        // GitHub normally returns runs newest-first, but explicitly sort the candidates
        // so an older run cannot hide a newer build if the API ordering changes.
        val eligibleRuns = runs.mapNotNull { it as? JsonObject }
            .filter { run ->
                run["event"]?.jsonPrimitive?.content == "push" &&
                    run["head_branch"]?.jsonPrimitive?.content == "master" &&
                    run["conclusion"]?.jsonPrimitive?.content == "success"
            }
            .sortedByDescending { run ->
                run["run_number"]?.jsonPrimitive?.content?.toLongOrNull() ?: -1L
            }

        for (run in eligibleRuns) {
            val runId = run["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: continue
            val runNumber = run["run_number"]?.jsonPrimitive?.content?.toLongOrNull() ?: continue
            val runAttempt = run["run_attempt"]?.jsonPrimitive?.content?.toLongOrNull() ?: continue
            val version = "1.0.$runNumber"

            // Skip versions already installed instead of stopping the scan. This keeps
            // discovery resilient to unordered or partially populated run responses.
            if (compareVersions(version, installedVersion) <= 0) continue

            val artifactsUrl = URL("$ACTIONS_API/runs/$runId/artifacts?per_page=100")
            val artifacts = getJson(artifactsUrl).jsonObject["artifacts"]?.jsonArray
                ?: continue
            val artifact = artifacts.mapNotNull { it as? JsonObject }.firstOrNull {
                it["name"]?.jsonPrimitive?.content == DEBUG_ARTIFACT_NAME &&
                    it["expired"]?.jsonPrimitive?.content == "false"
            } ?: continue
            val downloadUrl = artifact["archive_download_url"]?.jsonPrimitive?.content ?: continue
            if (!downloadUrl.startsWith(ARTIFACT_DOWNLOAD_PREFIX) || !downloadUrl.endsWith("/zip")) {
                continue
            }

            return WindowsDebugUpdate(
                version = version,
                tagName = "windows-debug-$runNumber-$runAttempt",
                downloadUrl = downloadUrl,
                runNumber = runNumber,
                runAttempt = runAttempt
            )
        }
        return null
    }

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
        const val ACTIONS_API =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions"
        // Ask GitHub for successful runs directly and inspect a full page, rather than
        // letting recent failed runs crowd an eligible build out of the discovery list.
        const val WORKFLOW_RUNS_API =
            "$ACTIONS_API/workflows/windows-desktop.yml/runs?branch=master&status=success&per_page=100"
        const val DEBUG_ARTIFACT_NAME = "GuessTheNumber-Windows-Debug"
        const val ARTIFACT_DOWNLOAD_PREFIX =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/artifacts/"
        const val MAX_RESPONSE_CHARS = 1_000_000
    }
}
