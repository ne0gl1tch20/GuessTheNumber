package com.jarrlyyy.guessthenumber.scripting

import android.content.Context
import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.luaj.vm2.LuaValue
import java.util.UUID

@Serializable
data class LuaScriptEntry(
    val id: String,
    val file: String,
    val title: String,
    val description: String,
    val featured: Boolean = false,
    val source: String = "bundled",
    val hostApiVersion: Int = 1
)

@Serializable
private data class LuaScriptCatalog(
    val schemaVersion: Int = 1,
    val hostApiVersion: Int = 1,
    val scripts: List<LuaScriptEntry>
)

data class LuaActivityDefinition(
    val id: String,
    val title: String,
    val description: String,
    val prompt: String,
    val options: List<String>,
    val correctOption: Int,
    val successMessage: String,
    val failureMessage: String,
    val instanceId: String
)

data class LuaEngineStatus(
    val runtimeAvailable: Boolean,
    val hostApiVersion: Int,
    val activeInstanceId: String?,
    val lastError: String?
)

sealed interface LuaStartResult {
    data class Started(val activity: LuaActivityDefinition) : LuaStartResult
    data class Rejected(val reason: String) : LuaStartResult
}

sealed interface LuaAnswerResult {
    data class Answered(val correct: Boolean, val message: String) : LuaAnswerResult
    data class Rejected(val reason: String) : LuaAnswerResult
}

/**
 * Shared app-level facade used by the player GO flow and debug console.
 * Kotlin owns lifecycle and state; scripts only describe temporary activity content.
 */
class LuaEngine private constructor(context: Context) {
    companion object {
        const val HOST_API_VERSION = 1
        private const val CATALOG_PATH = "scripts/manifest.json"

        @Volatile private var instance: LuaEngine? = null

        fun get(context: Context): LuaEngine =
            instance ?: synchronized(this) {
                instance ?: LuaEngine(context.applicationContext).also { instance = it }
            }
    }

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = false }
    private val remote = LuaLiveOpsRepository(appContext)
    private val diagnostics = mutableListOf<String>()
    private var activeActivity: LuaActivityDefinition? = null
    private var lastError: String? = null
    private var lastAnswerCorrect: Boolean? = null

    @Synchronized
    fun status(): LuaEngineStatus = LuaEngineStatus(
        runtimeAvailable = runCatching { catalog().isNotEmpty() }.getOrDefault(false),
        hostApiVersion = HOST_API_VERSION,
        activeInstanceId = activeActivity?.instanceId,
        lastError = lastError
    )

    @Synchronized
    fun currentActivity(): LuaActivityDefinition? = activeActivity

    @Synchronized
    fun listScripts(): List<LuaScriptEntry> {
        val bundled = runCatching { catalog() }.getOrElse { emptyList() }
        val remoteScripts = runCatching { remote.loadVerifiedCachedEntries() }.getOrElse { emptyList() }
        return (bundled + remoteScripts).distinctBy { it.id }
    }

    @Synchronized
    fun inspectScript(scriptId: String): LuaScriptEntry? =
        listScripts().firstOrNull { it.id == scriptId }

    @Synchronized
    fun startFeaturedActivity(): LuaStartResult {
        activeActivity?.let { return LuaStartResult.Started(it) }
        val scripts = listScripts()
        val remoteFeatured = scripts.firstOrNull { it.featured && it.source == "liveops" }
        if (remoteFeatured != null) {
            val remoteResult = start(remoteFeatured.id)
            if (remoteResult is LuaStartResult.Started) return remoteResult
            if (remote.rollbackToPreviousVerifiedBundle()) {
                val previousResult = start(remoteFeatured.id)
                if (previousResult is LuaStartResult.Started) return previousResult
            }
            log(remoteFeatured.id, "Falling back to the bundled featured activity.", LogLevel.WARN)
        }
        val bundledFeatured = scripts.firstOrNull { it.featured && it.source == "bundled" }
            ?: return reject("No featured Lua activity is available.")
        return start(bundledFeatured.id)
    }

    @Synchronized
    fun start(scriptId: String): LuaStartResult {
        activeActivity?.let {
            return if (it.id == scriptId) LuaStartResult.Started(it)
            else reject("Another Lua activity is already running.")
        }
        val entry = inspectScript(scriptId) ?: return reject("Unknown script ID: $scriptId")
        if (entry.hostApiVersion != HOST_API_VERSION) return reject("This script requires an unsupported host API version.")
        return try {
            val source = if (entry.source == "bundled") {
                appContext.assets.open("scripts/${entry.file}").bufferedReader().use { it.readText() }
            } else {
                remote.readVerifiedCachedScript(entry)
                    ?: return reject("The signed LiveOps script is not available in the verified cache.")
            }
            val result = LuaSandbox { line -> log(scriptId, line) }.execute(scriptId, source)
            val definition = parseActivity(entry, result)
            activeActivity = definition
            lastAnswerCorrect = null
            lastError = null
            log(scriptId, "Activity started.")
            LuaStartResult.Started(definition)
        } catch (error: LuaBudgetExceededError) {
            val reason = error.message?.take(240) ?: "Lua script exceeded its instruction budget."
            lastError = reason
            log(scriptId, "Activity rejected: $reason", LogLevel.WARN)
            reject(reason)
        } catch (error: Exception) {
            val reason = error.message?.take(240) ?: "Lua script failed."
            lastError = reason
            log(scriptId, "Activity rejected: $reason", LogLevel.WARN)
            reject(reason)
        }
    }

    @Synchronized
    fun submitAnswer(optionIndex: Int): LuaAnswerResult {
        val activity = activeActivity ?: return LuaAnswerResult.Rejected("No Lua activity is running.")
        if (optionIndex !in activity.options.indices) return LuaAnswerResult.Rejected("Invalid answer option.")
        if (lastAnswerCorrect != null) return LuaAnswerResult.Rejected("This activity has already been answered.")
        val correct = optionIndex == activity.correctOption
        lastAnswerCorrect = correct
        val message = if (correct) activity.successMessage else activity.failureMessage
        log(activity.id, "Answer submitted; correct=$correct")
        return LuaAnswerResult.Answered(correct, message)
    }

    @Synchronized
    fun stop(instanceId: String? = null): Boolean {
        val current = activeActivity ?: return false
        if (instanceId != null && instanceId != current.instanceId) return false
        log(current.id, "Activity stopped.")
        activeActivity = null
        lastAnswerCorrect = null
        return true
    }

    suspend fun refreshLiveOps(): LuaLiveOpsRefreshResult = remote.refresh()

    /** Typed debug-only commands; arbitrary Lua source evaluation is deliberately unavailable. */
    @Synchronized
    fun executeConsoleCommand(command: String): String {
        if (!BuildConfig.DEBUG) return "Lua developer commands are disabled in release builds."
        val args = command.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (args.firstOrNull() != "/lua") return "Use /lua help."
        return when (args.getOrNull(1)?.lowercase()) {
            "help" -> "/lua status | list | inspect <id> | run <id> | stop [instance-id] | logs"
            "status" -> {
                val current = status()
                "Lua runtime: ${if (current.runtimeAvailable) "ready" else "unavailable"}; host API v${current.hostApiVersion}; active instance: ${current.activeInstanceId ?: "none"}; last error: ${current.lastError ?: "none"}"
            }
            "list" -> listScripts().joinToString("\n") { "${it.id} — ${it.title} [${it.source}, API v${it.hostApiVersion}]" }
                .ifBlank { "No registered Lua scripts." }
            "inspect" -> {
                val id = args.getOrNull(2) ?: return "Usage: /lua inspect <id>"
                val entry = inspectScript(id) ?: return "Unknown script ID: $id"
                "ID: ${entry.id}\nTitle: ${entry.title}\nSource: ${entry.source}\nFile: ${entry.file}\nHost API: ${entry.hostApiVersion}\nFeatured: ${entry.featured}"
            }
            "run" -> {
                val id = args.getOrNull(2) ?: return "Usage: /lua run <id>"
                when (val result = start(id)) {
                    is LuaStartResult.Started -> "Started ${result.activity.title} (instance ${result.activity.instanceId})."
                    is LuaStartResult.Rejected -> "Lua start rejected: ${result.reason}"
                }
            }
            "stop" -> {
                val requestedId = args.getOrNull(2)
                if (stop(requestedId)) "Lua activity stopped."
                else "No matching active Lua instance."
            }
            "logs" -> recentDiagnostics().takeLast(40).joinToString("\n").ifBlank { "No Lua diagnostics yet." }
            "eval" -> "Lua REPL is disabled. Run a registered script with /lua run <id>."
            null -> "Use /lua help."
            else -> "Unknown Lua command. Use /lua help."
        }
    }

    @Synchronized
    fun recentDiagnostics(): List<String> = diagnostics.toList()

    private fun catalog(): List<LuaScriptEntry> {
        val raw = appContext.assets.open(CATALOG_PATH).bufferedReader().use { it.readText() }
        val decoded = json.decodeFromString<LuaScriptCatalog>(raw)
        require(decoded.schemaVersion == 1 && decoded.hostApiVersion == HOST_API_VERSION) {
            "Unsupported bundled Lua catalog schema or host API version."
        }
        require(decoded.scripts.isNotEmpty()) { "The bundled Lua catalog is empty." }
        require(decoded.scripts.map { it.id }.distinct().size == decoded.scripts.size) {
            "The bundled Lua catalog contains duplicate IDs."
        }
        decoded.scripts.forEach { entry ->
            require(entry.id.matches(Regex("[a-z0-9_-]{1,64}"))) { "Invalid script ID." }
            require(entry.file.matches(Regex("[a-zA-Z0-9_./-]{1,128}")) && !entry.file.contains("..")) {
                "Invalid bundled script path."
            }
            require(entry.title.isNotBlank() && entry.title.length <= 100 && entry.description.isNotBlank() && entry.description.length <= 240) {
                "Script metadata is incomplete or too long."
            }
            require(entry.hostApiVersion == HOST_API_VERSION) { "Unsupported script API version." }
        }
        return decoded.scripts.map { it.copy(source = "bundled") }
    }

    private fun parseActivity(entry: LuaScriptEntry, result: LuaValue): LuaActivityDefinition {
        require(result.istable()) { "Lua activity must return a table." }
        val optionsTable = result.get("options")
        require(optionsTable.istable()) { "Lua activity must define an options list." }
        require(optionsTable.length() in 2..6) { "Activity must provide 2 to 6 answer options." }
        val options = (1..optionsTable.length()).map { index -> optionsTable.get(index).tojstring().take(120) }
        require(options.all { it.isNotBlank() }) { "Activity must provide 2 to 6 answer options." }
        val correct = result.get("correct_option").checkint() - 1
        require(correct in options.indices) { "Activity answer key is outside the option list." }
        val title = result.get("title").checkjstring().trim().take(100)
        val description = result.get("description").checkjstring().trim().take(240)
        val prompt = result.get("prompt").checkjstring().trim().take(240)
        val successMessage = result.get("success_message").checkjstring().trim().take(240)
        val failureMessage = result.get("failure_message").checkjstring().trim().take(240)
        require(title.isNotBlank() && description.isNotBlank() && prompt.isNotBlank()) {
            "Activity title, description, and prompt must not be blank."
        }
        require(successMessage.isNotBlank() && failureMessage.isNotBlank()) {
            "Activity result messages must not be blank."
        }
        return LuaActivityDefinition(
            id = entry.id,
            title = title,
            description = description,
            prompt = prompt,
            options = options,
            correctOption = correct,
            successMessage = successMessage,
            failureMessage = failureMessage,
            instanceId = UUID.randomUUID().toString()
        )
    }

    private fun reject(reason: String): LuaStartResult.Rejected {
        lastError = reason
        return LuaStartResult.Rejected(reason)
    }

    private fun log(scriptId: String, message: String, level: LogLevel = LogLevel.INFO) {
        val line = "[lua:$scriptId] ${message.take(500)}"
        synchronized(diagnostics) {
            diagnostics.add(line)
            while (diagnostics.size > 200) diagnostics.removeAt(0)
        }
        GameLogger.log(level, LoggerCategory.UI, "LUA_SCRIPT", line)
    }
}
