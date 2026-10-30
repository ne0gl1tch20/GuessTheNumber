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
    val artifactDownloadUrl: String,
    val commitSha: String,
    val runNumber: Long,
    val runAttempt: Long
)

/** Finds the newest unexpired Windows Debug artifact from a successful master CI run. */
internal class WindowsDebugUpdateClient(
    private val connectionFactory: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    }
) {
    fun findLatestUpdate(installedVersion: String): WindowsDebugUpdate? {
        val runs = getJson(URL(WORKFLOW_RUNS_API)).jsonObject["workflow_runs"]?.jsonArray ?: return null
        val candidates = runs.mapNotNull { it as? JsonObject }
            .filter {
                it["event"]?.jsonPrimitive?.content == "push" &&
                    it["head_branch"]?.jsonPrimitive?.content == "master" &&
                    it["status"]?.jsonPrimitive?.content == "completed" &&
                    it["conclusion"]?.jsonPrimitive?.content == "success"
            }
            .sortedByDescending { it["run_number"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L }

        val latest = candidates.firstOrNull() ?: return null
        val runId = latest["id"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: throw IllegalStateException("The latest successful Windows Desktop CI run has no run ID.")
        val runNumber = latest["run_number"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: throw IllegalStateException("The latest successful Windows Desktop CI run has no run number.")
        val runAttempt = latest["run_attempt"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: throw IllegalStateException("The latest successful Windows Desktop CI run has no run attempt.")
        val commitSha = latest["head_sha"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("The latest successful Windows Desktop CI run has no commit SHA.")
        require(commitSha.matches(Regex("[0-9a-fA-F]{40}"))) {
            "The latest successful Windows Desktop CI run has an invalid commit SHA."
        }
        val artifacts = runArtifacts(runId)
            ?: throw IllegalStateException("Could not read artifacts for the latest successful Windows Desktop CI run.")
        val artifact = artifacts.firstOrNull {
            it["name"]?.jsonPrimitive?.content == WINDOWS_ARTIFACT_NAME
        } ?: throw IllegalStateException("The latest Windows Debug build artifact is missing.")
        if (artifact["expired"]?.jsonPrimitive?.content != "false") {
            throw IllegalStateException("The latest Windows Debug artifact has expired. CI artifacts are retained for seven days.")
        }
        val archiveUrl = artifact["archive_download_url"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("The latest Windows Debug artifact has no download URL.")
        if (!isArtifactDownloadUrl(archiveUrl, runId)) {
            throw IllegalStateException("GitHub returned an unexpected Windows Debug artifact URL.")
        }
        val version = "1.0.$runNumber"
        if (!version.matches(Regex("1\\\\.0\\\\.\\\\d+"))) {
            throw IllegalStateException("The latest Windows Debug build has an incompatible version.")
        }
        if (compareVersions(version, installedVersion) <= 0) return null
        return WindowsDebugUpdate(
            version = version,
            tagName = "windows-debug-$runNumber-$runAttempt",
            artifactDownloadUrl = archiveUrl,
            commitSha = commitSha,
            runNumber = runNumber,
            runAttempt = runAttempt
        )
    }

    private fun runArtifacts(runId: Long): List<JsonObject>? {
        val response = getJson(URL("$ACTIONS_API/$runId/artifacts")).jsonObject
        return response["artifacts"]?.jsonArray?.mapNotNull { it as? JsonObject }
    }

    private fun isArtifactDownloadUrl(value: String, runId: Long): Boolean =
        value.matches(Regex("https://api\\.github\\.com/repos/ne0gl1tch20/GuessTheNumber/actions/artifacts/\\d+/zip"))

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
            val comparison = leftParts.getOrElse(index) { 0 }
                .compareTo(rightParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private companion object {
        const val WORKFLOW_RUNS_API =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/workflows/windows-desktop.yml/runs?branch=master&status=success&per_page=20"
        const val ACTIONS_API =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/runs"
        const val WINDOWS_ARTIFACT_NAME = "GuessTheNumber-Windows-Debug"
        const val MAX_RESPONSE_CHARS = 1_000_000
    }
}
