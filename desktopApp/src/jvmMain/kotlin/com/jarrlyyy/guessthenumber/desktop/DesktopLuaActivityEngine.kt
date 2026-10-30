package com.jarrlyyy.guessthenumber.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.MathLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.LoadState
import java.util.UUID

@Serializable
internal data class DesktopLuaScriptEntry(
    val id: String,
    val file: String,
    val title: String,
    val description: String,
    val featured: Boolean = false,
    val hostApiVersion: Int = 1
)

@Serializable
private data class DesktopLuaScriptCatalog(
    val schemaVersion: Int = 1,
    val hostApiVersion: Int = 1,
    val scripts: List<DesktopLuaScriptEntry>
)

internal data class DesktopLuaActivityDefinition(
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

internal sealed interface DesktopLuaStartResult {
    data class Started(val activity: DesktopLuaActivityDefinition) : DesktopLuaStartResult
    data class Rejected(val reason: String) : DesktopLuaStartResult
}

internal data class DesktopLuaAnswerResult(val correct: Boolean, val message: String)

/**
 * Runs only registered bundled activities. Lua can return bounded activity content,
 * but has no save, currency, filesystem, networking or arbitrary host access.
 */
internal class DesktopLuaActivityEngine {
    companion object {
        const val HOST_API_VERSION = 1
        private const val MANIFEST_PATH = "scripts/manifest.json"
    }

    private val json = Json { ignoreUnknownKeys = false }
    private var active: DesktopLuaActivityDefinition? = null
    private var answered: Boolean? = null

    @Synchronized
    fun listScripts(): List<DesktopLuaScriptEntry> = loadCatalog()

    @Synchronized
    fun currentActivity(): DesktopLuaActivityDefinition? = active

    @Synchronized
    fun start(scriptId: String): DesktopLuaStartResult {
        active?.let {
            return if (it.id == scriptId) DesktopLuaStartResult.Started(it)
            else DesktopLuaStartResult.Rejected("Another activity is already running.")
        }
        val entry = loadCatalog().firstOrNull { it.id == scriptId }
            ?: return DesktopLuaStartResult.Rejected("Unknown activity.")
        return try {
            val source = javaClass.classLoader.getResourceAsStream("scripts/${entry.file}")
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: return DesktopLuaStartResult.Rejected("The bundled activity script is missing.")
            val output = DesktopLuaSandbox().execute(entry.id, source)
            val activity = parseActivity(entry, output)
            active = activity
            answered = null
            DesktopLuaStartResult.Started(activity)
        } catch (error: DesktopLuaBudgetExceeded) {
            DesktopLuaStartResult.Rejected(error.message ?: "Activity exceeded its instruction budget.")
        } catch (error: Exception) {
            DesktopLuaStartResult.Rejected(error.message?.take(240) ?: "Activity could not be started.")
        }
    }

    @Synchronized
    fun submitAnswer(optionIndex: Int): DesktopLuaAnswerResult? {
        val activity = active ?: return null
        if (answered != null || optionIndex !in activity.options.indices) return null
        val correct = optionIndex == activity.correctOption
        answered = correct
        return DesktopLuaAnswerResult(
            correct = correct,
            message = if (correct) activity.successMessage else activity.failureMessage
        )
    }

    @Synchronized
    fun stop() {
        active = null
        answered = null
    }

    private fun loadCatalog(): List<DesktopLuaScriptEntry> {
        val raw = javaClass.classLoader.getResourceAsStream(MANIFEST_PATH)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("The bundled Lua activity catalog is missing.")
        val catalog = json.decodeFromString<DesktopLuaScriptCatalog>(raw)
        require(catalog.schemaVersion == 1 && catalog.hostApiVersion == HOST_API_VERSION) {
            "Unsupported Lua activity catalog or host API version."
        }
        require(catalog.scripts.isNotEmpty() && catalog.scripts.map { it.id }.distinct().size == catalog.scripts.size) {
            "The Lua activity catalog is empty or contains duplicate IDs."
        }
        catalog.scripts.forEach { entry ->
            require(entry.id.matches(Regex("[a-z0-9_-]{1,64}"))) { "Invalid Lua activity ID." }
            require(entry.file.matches(Regex("[a-zA-Z0-9_./-]{1,128}")) && !entry.file.contains("..")) {
                "Invalid Lua activity script path."
            }
            require(entry.title.isNotBlank() && entry.title.length <= 100 &&
                entry.description.isNotBlank() && entry.description.length <= 240 &&
                entry.hostApiVersion == HOST_API_VERSION
            ) { "Lua activity metadata is invalid." }
        }
        return catalog.scripts
    }

    private fun parseActivity(entry: DesktopLuaScriptEntry, result: LuaValue): DesktopLuaActivityDefinition {
        require(result.istable()) { "Lua activity must return a table." }
        fun requiredText(key: String, maximum: Int): String {
            val value = result.get(key)
            require(value.type() == LuaValue.TSTRING) { "Activity field '$key' must be text." }
            val text = value.tojstring().trim()
            require(text.isNotEmpty() && text.length <= maximum) { "Activity field '$key' is blank or too long." }
            return text
        }
        val optionsTable = result.get("options")
        require(optionsTable.istable() && optionsTable.length() in 2..6) {
            "Activity must provide 2 to 6 answer options."
        }
        val options = (1..optionsTable.length()).map { index ->
            val option = optionsTable.get(index)
            require(option.type() == LuaValue.TSTRING) { "Activity answer options must be text." }
            option.tojstring().trim().also {
                require(it.isNotEmpty() && it.length <= 120) { "Activity answer option is blank or too long." }
            }
        }
        val answer = result.get("correct_option")
        require(answer.type() == LuaValue.TNUMBER) { "Activity answer key must be a number." }
        val answerNumber = answer.todouble()
        require(answerNumber.isFinite() && answerNumber % 1.0 == 0.0 &&
            answerNumber >= 1.0 && answerNumber <= options.size.toDouble()
        ) { "Activity answer key is outside the option list." }
        return DesktopLuaActivityDefinition(
            id = entry.id,
            title = requiredText("title", 100),
            description = requiredText("description", 240),
            prompt = requiredText("prompt", 240),
            options = options,
            correctOption = answerNumber.toInt() - 1,
            successMessage = requiredText("success_message", 240),
            failureMessage = requiredText("failure_message", 240),
            instanceId = UUID.randomUUID().toString()
        )
    }
}

private class DesktopLuaSandbox {
    companion object {
        private const val MAX_SOURCE_CHARS = 32_768
        private const val MAX_INSTRUCTIONS = 100_000
        private const val HOOK_INTERVAL = 1_000
        private const val MAX_LOG_LINES = 16
        private const val MAX_LOG_CHARS = 240
    }

    private val compilerGlobals = Globals().apply {
        load(BaseLib())
        LoadState.install(this)
        LuaC.install(this)
    }

    fun execute(scriptId: String, source: String): LuaValue {
        require(source.isNotBlank() && source.length <= MAX_SOURCE_CHARS) {
            "Lua activity source is empty or exceeds the size limit."
        }
        var logLines = 0
        val globals = Globals().apply {
            load(BaseLib())
            LoadState.install(this)
            LuaC.install(this)
            load(PackageLib())
            load(TableLib())
            load(MathLib())
            load(Bit32Lib())
            load(DebugLib())
        }
        val setHook = globals.get("debug").get("sethook")
        globals.set("debug", LuaValue.NIL)
        listOf(
            "dofile", "loadfile", "load", "require", "collectgarbage",
            "getmetatable", "setmetatable", "rawset", "rawget"
        ).forEach { globals.set(it, LuaValue.NIL) }
        if (!globals.get("package").isnil()) globals.set("package", LuaValue.NIL)
        globals.get("table").set("concat", LuaValue.NIL)

        val api = LuaValue.tableOf()
        api.set("log", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                if (logLines < MAX_LOG_LINES) logLines++
                return LuaValue.NIL
            }
        })
        api.set("input", LuaValue.tableOf())
        globals.set("gtn", api)
        globals.set("print", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (logLines < MAX_LOG_LINES) {
                    (1..args.narg()).joinToString(" ") { args.arg(it).tojstring() }.take(MAX_LOG_CHARS)
                    logLines++
                }
                return LuaValue.NONE
            }
        })

        val chunk = compilerGlobals.load(source, scriptId, globals)
        var instructions = 0
        val hook = object : ZeroArgFunction() {
            override fun call(): LuaValue {
                instructions += HOOK_INTERVAL
                if (instructions > MAX_INSTRUCTIONS) {
                    throw DesktopLuaBudgetExceeded("Lua activity exceeded its instruction budget.")
                }
                return LuaValue.NIL
            }
        }
        val thread = LuaThread(globals, chunk)
        setHook.invoke(
            LuaValue.varargsOf(
                arrayOf(thread, hook, LuaValue.EMPTYSTRING, LuaValue.valueOf(HOOK_INTERVAL))
            )
        )
        return try {
            val outcome = thread.resume(LuaValue.NIL)
            if (!outcome.arg1().toboolean()) {
                val message = outcome.arg(2).tojstring()
                if (message.contains("instruction budget", ignoreCase = true)) {
                    throw DesktopLuaBudgetExceeded("Lua activity exceeded its instruction budget.")
                }
                throw LuaError(message)
            }
            outcome.arg(2)
        } finally {
            setHook.invoke(LuaValue.varargsOf(arrayOf(thread, LuaValue.NIL)))
        }
    }
}

private class DesktopLuaBudgetExceeded(message: String) : Error(message)
