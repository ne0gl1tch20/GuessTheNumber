package com.jarrlyyy.guessthenumber.scripting

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
            // LuaJ's library installers register themselves in package.loaded.
            // Install PackageLib first, then remove the package surface below.
            load(PackageLib())
            load(TableLib())
            load(MathLib())
            load(Bit32Lib())
            load(DebugLib())
        }

        // LuaJ's hook belongs to a LuaThread. Configure that thread directly
        // rather than calling debug.sethook from the main Kotlin thread.
        val setHook = globals.get("debug").get("sethook")
        globals.set("debug", LuaValue.NIL)

        listOf(
            "dofile", "loadfile", "load", "require", "collectgarbage",
            "getmetatable", "setmetatable", "rawset", "rawget"
        ).forEach { globals.set(it, LuaValue.NIL) }
        globals.get("package").let { if (!it.isnil()) globals.set("package", LuaValue.NIL) }
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
        var instructions = 0
        val hook = object : org.luaj.vm2.lib.ZeroArgFunction() {
            override fun call(): LuaValue {
                instructions += HOOK_INTERVAL
                if (instructions > MAX_INSTRUCTIONS) {
                    // LuaError can be swallowed by a script's pcall. A JVM Error
                    // escapes Lua pcall and is surfaced when the thread resumes.
                    throw LuaBudgetExceededError("Lua script exceeded its instruction budget.")
                }
                return LuaValue.NIL
            }
        }

        val thread = LuaThread(globals, chunk)
        setHook.invoke(
            LuaValue.varargsOf(
                arrayOf(
                    thread,
                    hook,
                    LuaValue.EMPTYSTRING,
                    LuaValue.valueOf(HOOK_INTERVAL)
                )
            )
        )

        return try {
            val outcome = thread.resume(LuaValue.NIL)
            // LuaThread.resume follows Lua's coroutine.resume contract:
            // (true, returnedValue) on success or (false, errorMessage) on failure.
            if (!outcome.arg1().toboolean()) {
                val message = outcome.arg(2).tojstring()
                if (message.contains("instruction budget", ignoreCase = true)) {
                    throw LuaBudgetExceededError("Lua script exceeded its instruction budget.")
                }
                throw LuaError(message)
            }
            outcome.arg(2)
        } catch (error: LuaBudgetExceededError) {
            throw error
        } finally {
            // Drop the hook reference so completed threads can be collected.
            setHook.invoke(LuaValue.varargsOf(arrayOf(thread, LuaValue.NIL)))
        }
    }

    private fun checkSource(source: String) {
        require(source.length <= MAX_SOURCE_CHARS) {
            "Script exceeds the $MAX_SOURCE_CHARS character limit."
        }
        require(source.isNotBlank()) { "Script is empty." }
    }
}

internal class LuaBudgetExceededError(message: String) : Error(message)
