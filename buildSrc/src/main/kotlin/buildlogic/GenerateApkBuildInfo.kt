package buildlogic

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateApkBuildInfo : DefaultTask() {
    @get:Input
    abstract val environmentName: Property<String>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        // The timestamp is intentionally refreshed on every build, even when the commit is unchanged.
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun generate() {
        val repository = repositoryDirectory.get().asFile
        fun gitValue(vararg arguments: String): String? = runCatching {
            val process = ProcessBuilder(*arguments)
                .directory(repository)
                .redirectErrorStream(true)
                .start()
            val value = process.inputStream.bufferedReader().use { it.readText().trim() }
            if (process.waitFor() == 0 && value.isNotBlank()) value else null
        }.getOrNull()

        val githubSha = System.getenv("GITHUB_SHA")
        val fullCommit = githubSha?.takeIf { it.matches(Regex("[0-9a-fA-F]{40,64}")) }
            ?: gitValue("git", "rev-parse", "HEAD")
        val shortCommit = fullCommit?.take(8) ?: "unknown"
        val githubRef = System.getenv("GITHUB_REF").orEmpty()
        val branchCandidate = System.getenv("GITHUB_HEAD_REF")?.takeIf { it.isNotBlank() }
            ?: (if (githubRef.startsWith("refs/heads/")) System.getenv("GITHUB_REF_NAME") else null)
            ?: gitValue("git", "branch", "--show-current")
        val branch = branchCandidate
            ?.replace(Regex("[^A-Za-z0-9._-]"), "-")
            ?.trim('-')
            ?.ifBlank { "unknown" }
            ?: "unknown"

        val zone = ZoneId.of("Asia/Manila")
        val buildInstant = System.getenv("SOURCE_DATE_EPOCH")?.toLongOrNull()
            ?.let { Instant.ofEpochSecond(it).atZone(zone) }
            ?: ZonedDateTime.now(zone)
        val buildDate = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
            .format(buildInstant)
        val version = versionName.get()
        val code = versionCode.get()
        val metadata = listOf(
            "name: GuessTheNumber-v$version-$shortCommit.$branch",
            "env: " + environmentName.get(),
            "git_commit: $shortCommit",
            "git_branch: $branch",
            "version_name: $version",
            "version_code: $code",
            "build_date: $buildDate"
        ).joinToString(separator = "\n", postfix = "\n")

        val outputFile = outputDirectory.get().file("info.txt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(metadata, Charsets.UTF_8)
        logger.lifecycle("Generated APK metadata at " + outputFile.relativeTo(repository))
    }
}