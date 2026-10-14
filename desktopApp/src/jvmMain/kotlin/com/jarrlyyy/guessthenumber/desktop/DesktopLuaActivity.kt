package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.shared.scripting.LuaActivityPayloadValidator
import com.jarrlyyy.guessthenumber.shared.scripting.LuaSandbox
import org.luaj.vm2.LuaValue

internal data class DesktopLuaActivity(
    val title: String,
    val description: String,
    val prompt: String,
    val options: List<String>,
    val correctOption: Int,
    val successMessage: String,
    val failureMessage: String
) {
    companion object {
        fun load(resourcePath: String): DesktopLuaActivity {
            val source = DesktopLuaActivity::class.java.classLoader
                .getResourceAsStream(resourcePath)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Bundled Lua activity '$resourcePath' was not found.")

            val result = LuaSandbox {}.execute(resourcePath, source)
            LuaActivityPayloadValidator.validate(result)
            val optionsTable = result.get("options")
            val options = (1..optionsTable.length()).map { optionsTable.get(it).tojstring() }
            val correctOption = result.get("correct_option").toint() - 1

            return DesktopLuaActivity(
                title = result.get("title").tojstring(),
                description = result.get("description").tojstring(),
                prompt = result.get("prompt").tojstring(),
                options = options,
                correctOption = correctOption,
                successMessage = result.get("success_message").tojstring(),
                failureMessage = result.get("failure_message").tojstring()
            )
        }
    }
}
