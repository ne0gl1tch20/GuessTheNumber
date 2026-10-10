package com.jarrlyyy.guessthenumber.scripting

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.MathLib
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.LoadState

/**
 * Per-script Lua environment. Only the small, explicit host API is exposed.
 * Keep this sandbox isolated from Android, Java interop, files, and networking.
 */
internal class LuaSandbox(
    private val onLog: (String) -> Unit
) {
    companion object {
        const val MAX_SOURCE_CHARS = 32_768
        const val MAX_INSTRUCTIONS = 100_000
        private const val HOOK_INTERVAL = 1_000
        private const val MAX_LOG_LINES = 32
        private const val MAX_LOG_CHARS = 500
    }

    private val compilerGlobals = Globals().apply {
        load(BaseLib())
        load(MathLib())
        LoadState.install(this)
        LuaC.install(this)
    }

    fun validate(scriptId: String, source: String) {
        checkSource(source)
        compilerGlobals.load(source, scriptId, Globals())
    }

    fun execute(scriptId: String, source: String): LuaValue {
        checkSource(source)

        var logLines = 0
        val globals = Globals().apply {
            load(BaseLib())
            load(TableLib())
            load(MathLib())
            load(Bit32Lib())
            load(DebugLib())
            LoadState.install(this)
            LuaC.install(this)
        }

        // Capture the hook setter before removing the entire debug surface.
        val setHook = globals.get("debug").get("sethook")
        globals.set("debug", LuaValue.NIL)

        listOf(
            "dofile", "loadfile", "load", "require", "collectgarbage",
            "getmetatable", "setmetatable", "rawset", "rawget"
        ).forEach { globals.set(it, LuaValue.NIL) }
        // Avoid an easy unbounded string-allocation path in event scripts.
        globals.get("table").set("concat", LuaValue.NIL)

        // Bound output and avoid exposing an unbounded print sink.
        globals.set("print", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (logLines < MAX_LOG_LINES) {
                    val line = (1..args.narg()).joinToString(" ") { args.arg(it).tojstring() }
                        .take(MAX_LOG_CHARS)
                    onLog(line)
                    logLines++
                }
                return LuaValue.NONE
            }
        })

        val api = LuaValue.tableOf()
        api.set("log", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                if (logLines < MAX_LOG_LINES) {
                    onLog(arg.tojstring().take(MAX_LOG_CHARS))
                    logLines++
                }
                return LuaValue.NIL
            }
        })
        globals.set("gtn", api)

        val chunk = compilerGlobals.load(source, scriptId, globals)
        var instructions = 0
        val hook = object : org.luaj.vm2.lib.ZeroArgFunction() {
            override fun call(): LuaValue {
                instructions += HOOK_INTERVAL
                if (instructions > MAX_INSTRUCTIONS) {
                    throw LuaError("Lua script exceeded its instruction budget.")
                }
                return LuaValue.NIL
            }
        }

        // LuaJ's debug.sethook resolves the currently running Lua thread. Calling
        // it directly from Kotlin happens outside that thread and can fail. Install
        // the hook from a tiny Lua wrapper, then hide debug before the script runs.
        globals.set("__gtn_run", chunk)
        globals.set("__gtn_hook", hook)
        val guardedChunk = compilerGlobals.load(
            """
            local run = __gtn_run
            local hook = __gtn_hook
            local sethook = debug.sethook
            sethook(hook, "", $HOOK_INTERVAL)
            debug = nil
            __gtn_run = nil
            __gtn_hook = nil
            local ok, result = pcall(run)
            sethook(nil, "", 0)
            if not ok then error(result) end
            return result
            """.trimIndent(),
            "${scriptId}_guard",
            globals
        )

        return try {
            guardedChunk.call()
        } catch (error: LuaBudgetExceededError) {
            throw error
        } catch (error: LuaError) {
            if (error.message.orEmpty().contains("instruction budget", ignoreCase = true) ||
                error.cause is LuaBudgetExceededError
            ) {
                throw LuaBudgetExceededError("Lua script exceeded its instruction budget.")
            }
            throw error
        } finally {
            // The wrapper normally clears the hook; remove temporary host references
            // even if compilation or execution fails unexpectedly.
            globals.set("__gtn_run", LuaValue.NIL)
            globals.set("__gtn_hook", LuaValue.NIL)
        }
    }

    private fun checkSource(source: String) {
        // Check size first so whitespace-only oversized inputs still hit the size limit.
        require(source.length <= MAX_SOURCE_CHARS) {
            "Script exceeds the $MAX_SOURCE_CHARS character limit."
        }
        require(source.isNotBlank()) { "Script is empty." }
    }
}

internal class LuaBudgetExceededError(message: String) : Error(message)
