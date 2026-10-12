package com.jarrlyyy.guessthenumber.scripting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.luaj.vm2.LuaError

class LuaSandboxTest {
    @Test
    fun executesActivityDefinitionWithoutExposingPlatformLibraries() {
        val logs = mutableListOf<String>()
        val result = LuaSandbox(logs::add).execute(
            "sandbox_test",
            """
            gtn.log("script ready")
            return {
              sandboxed = os == nil and io == nil and luajava == nil and debug == nil,
              options = {"A", "B"}
            }
            """.trimIndent()
        )

        assertTrue(result.get("sandboxed").toboolean())
        assertEquals(2, result.get("options").length())
        assertEquals(listOf("script ready"), logs)
    }

    @Test
    fun rejectsOversizedScriptSource() {
        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            LuaSandbox {}.execute("oversized", " ".repeat(LuaSandbox.MAX_SOURCE_CHARS + 1))
        }
        assertTrue(error.message!!.contains("character limit"))
    }

    @Test
    fun terminatesInfiniteLoopsUsingInstructionBudget() {
        val error = org.junit.Assert.assertThrows(LuaBudgetExceededError::class.java) {
            LuaSandbox {}.execute("loop", "while true do local x = 1 + 1 end")
        }
        assertTrue(error.message!!.contains("instruction budget"))
    }

    @Test
    fun instructionBudgetCannotBeBypassedWithPcall() {
        val error = org.junit.Assert.assertThrows(LuaBudgetExceededError::class.java) {
            LuaSandbox {}.execute(
                "protected_loop",
                "pcall(function() while true do local x = 1 + 1 end end)"
            )
        }
        assertTrue(error.message!!.contains("instruction budget"))
    }

    @Test
    fun doesNotAllowDynamicCodeLoadingOrMetatableMutation() {
        val result = LuaSandbox {}.execute(
            "restricted",
            "return {load_removed = load == nil, metatable_removed = setmetatable == nil, package_removed = package == nil}"
        )
        assertTrue(result.get("load_removed").toboolean())
        assertTrue(result.get("metatable_removed").toboolean())
        assertTrue(result.get("package_removed").toboolean())
    }

    @Test
    fun boundsScriptLoggingByLineCountAndLength() {
        val logs = mutableListOf<String>()
        val longMessage = "x".repeat(700)
        val source = buildString {
            repeat(40) {
                append("print(\"")
                append(longMessage)
                append("\")\n")
            }
            append("return {ok = true}")
        }

        LuaSandbox(logs::add).execute("bounded_logs", source)

        assertEquals(32, logs.size)
        assertTrue(logs.all { it.length <= 500 })
    }

    @Test
    fun runtimeFailureDoesNotPreventSubsequentScriptExecution() {
        val sandbox = LuaSandbox {}

        assertThrows(LuaError::class.java) {
            sandbox.execute("runtime_failure", "error('script failed')")
        }

        val recovered = sandbox.execute(
            "after_runtime_failure",
            "return {recovered = true}"
        )
        assertTrue(recovered.get("recovered").toboolean())
    }

    @Test
    fun instructionBudgetFailureDoesNotPreventSubsequentScriptExecution() {
        val sandbox = LuaSandbox {}

        assertThrows(LuaBudgetExceededError::class.java) {
            sandbox.execute("budget_failure", "while true do local x = 1 + 1 end")
        }

        val recovered = sandbox.execute(
            "after_budget_failure",
            "return {recovered = true}"
        )
        assertTrue(recovered.get("recovered").toboolean())
    }
}
