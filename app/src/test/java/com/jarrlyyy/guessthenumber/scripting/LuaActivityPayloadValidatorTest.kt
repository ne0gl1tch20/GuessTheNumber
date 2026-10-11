package com.jarrlyyy.guessthenumber.scripting

import org.junit.Assert.assertThrows
import org.junit.Test

class LuaActivityPayloadValidatorTest {
    @Test
    fun acceptsValidActivityPayload() {
        LuaActivityPayloadValidator.validate(payload())
    }

    @Test
    fun rejectsNonStringAnswerOptions() {
        val result = execute(
            """
            return {
              title = "Test", description = "Test", prompt = "Pick one",
              options = {"A", 42}, correct_option = 1,
              success_message = "Good", failure_message = "Try again"
            }
            """.trimIndent()
        )

        assertThrows(IllegalArgumentException::class.java) {
            LuaActivityPayloadValidator.validate(result)
        }
    }

    @Test
    fun rejectsOversizedTextInsteadOfSilentlyTruncatingIt() {
        val result = execute(
            """
            return {
              title = "${"x".repeat(101)}", description = "Test", prompt = "Pick one",
              options = {"A", "B"}, correct_option = 1,
              success_message = "Good", failure_message = "Try again"
            }
            """.trimIndent()
        )

        assertThrows(IllegalArgumentException::class.java) {
            LuaActivityPayloadValidator.validate(result)
        }
    }

    @Test
    fun rejectsFractionalAndOutOfRangeAnswerKeys() {
        listOf("1.5", "3", "0").forEach { answer ->
            val result = execute(
                """
                return {
                  title = "Test", description = "Test", prompt = "Pick one",
                  options = {"A", "B"}, correct_option = $answer,
                  success_message = "Good", failure_message = "Try again"
                }
                """.trimIndent()
            )
            assertThrows(IllegalArgumentException::class.java) {
                LuaActivityPayloadValidator.validate(result)
            }
        }
    }

    @Test
    fun rejectsMissingOrBlankRequiredText() {
        val result = execute(
            """
            return {
              title = "  ", description = "Test", prompt = "Pick one",
              options = {"A", "B"}, correct_option = 1,
              success_message = "Good", failure_message = "Try again"
            }
            """.trimIndent()
        )

        assertThrows(IllegalArgumentException::class.java) {
            LuaActivityPayloadValidator.validate(result)
        }
    }

    private fun payload() = execute(
        """
        return {
          title = "Test", description = "A test", prompt = "Pick one",
          options = {"A", "B"}, correct_option = 1,
          success_message = "Good", failure_message = "Try again"
        }
        """.trimIndent()
    )

    private fun execute(source: String) = LuaSandbox {}.execute("activity_payload_test", source)
}
