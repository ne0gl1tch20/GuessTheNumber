package com.jarrlyyy.guessthenumber.scripting

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.MathLib
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.LoadState

/**
 * Per-script Lua environment. No Android context, Java bridge, filesystem, network,
 * package loader, OS library, or coroutine library is exposed to Lua.
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
        require(source.isNotBlank()) { "Script is empty." }
        require(source.length <= MAX_SOURCE_CHARS) { "Script exceeds the $MAX_SOURCE_CHARS character limit." }
        compilerGlobals.load(source, scriptId, Globals())
    }

    fun execute(scriptId: String, source: String): LuaValue {
        require(source.isNotBlank()) { "Script is empty." }
        require(source.length <= MAX_SOURCE_CHARS) { "Script exceeds the $MAX_SOURCE_CHARS character limit." }

        var logLines = 0
        val globals = Globals().apply {
            load(BaseLib())
            load(TableLib())
            load(MathLib())
            load(Bit32Lib())
            load(DebugLib())
        }

        // Capture the hook setter internally, then remove the entire debug surface.
        val setHook = globals.get("debug").get("sethook")
        globals.set("debug", LuaValue.NIL)

        listOf(
            "dofile", "loadfile", "load", "require", "collectgarbage",
            "getmetatable", "setmetatable", "rawset", "rawget"
        ).forEach { globals.set(it, LuaValue.NIL) }

        // Large single-call allocations are unnecessary for bundled minigame content.
        globals.get("table").set("concat", LuaValue.NIL)
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
        val thread = LuaThread(globals, chunk)
        var instructions = 0
        val hook = object : org.luaj.vm2.lib.ZeroArgFunction() {
            override fun call(): LuaValue {
                instructions += HOOK_INTERVAL
                if (instructions > MAX_INSTRUCTIONS) {
                    throw LuaBudgetExceededError("Lua script exceeded its instruction budget.")
                }
                return LuaValue.NIL
            }
        }
        setHook.invoke(
            LuaValue.varargsOf(
                arrayOf(thread, hook, LuaValue.valueOf(""), LuaValue.valueOf(HOOK_INTERVAL))
            )
        )

        val resumed = thread.resume(LuaValue.NIL)
        if (!resumed.arg1().toboolean()) {
            val message = resumed.arg(2).tojstring()
            if (message.contains("instruction budget", ignoreCase = true)) {
                throw LuaBudgetExceededError(message)
            }
            throw IllegalStateException(message.take(500))
        }
        return resumed.arg(2)
    }
}

internal class LuaBudgetExceededError(message: String) : Error(message)
