package com.jarrlyyy.guessthenumber.scripting

import org.luaj.vm2.LuaValue

/**
 * Strictly validates Lua activity output before the Kotlin UI model is created.
 * Rejects malformed values instead of silently converting or truncating script content.
 */
internal object LuaActivityPayloadValidator {
    fun validate(result: LuaValue) {
        require(result.istable()) { "Lua activity must return a table." }

        result.requiredText("title", 100)
        result.requiredText("description", 240)
        result.requiredText("prompt", 240)
        result.requiredText("success_message", 240)
        result.requiredText("failure_message", 240)

        val options = result.get("options")
        require(options.istable()) { "Lua activity must define an options list." }
        val count = options.length()
        require(count in 2..6) { "Activity must provide 2 to 6 answer options." }
        (1..count).forEach { index ->
            val option = options.get(index)
            require(option.isstring()) { "Activity answer options must be strings." }
            val text = option.tojstring().trim()
            require(text.isNotEmpty() && text.length <= 120) {
                "Activity answer options must be nonblank and at most 120 characters."
            }
        }

        val correct = result.get("correct_option")
        require(correct.isnumber()) { "Activity answer key must be an integer option number." }
        val correctNumber = correct.todouble()
        require(correctNumber.isFinite() && correctNumber % 1.0 == 0.0 &&
            correctNumber >= 1.0 && correctNumber <= count.toDouble()
        ) { "Activity answer key is outside the option list." }
    }

    private fun LuaValue.requiredText(field: String, maxLength: Int): String {
        val value = get(field)
        require(value.isstring()) { "Lua activity field '$field' must be a string." }
        val text = value.tojstring().trim()
        require(text.isNotEmpty() && text.length <= maxLength) {
            "Lua activity field '$field' must be nonblank and at most $maxLength characters."
        }
        return text
    }
}
