package com.jarrlyyy.guessthenumber.shared.scripting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LuaSandboxTest {
    @Test
    fun acceptsValidActivityPayload() {
        val source = """
            return {
                title = "Number Rush",
                description = "A short offline activity.",
                prompt = "Choose the right answer.",
                success_message = "Correct!",
                failure_message = "Try again.",
                options = { "One", "Two" },
                correct_option = 1
            }
        """.trimIndent()

        val result = LuaSandbox {}.execute("test_activity", source)
        LuaActivityPayloadValidator.validate(result)

        assertEquals("Number Rush", result.get("title").tojstring())
    }

    @Test
    fun rejectsOversizedScriptBeforeExecution() {
        val error = assertFailsWith<IllegalArgumentException> {
            LuaSandbox {}.execute("oversized", " ".repeat(LuaSandbox.MAX_SOURCE_CHARS + 1))
        }
        assertEquals(true, error.message?.contains("character limit"))
    }
}
