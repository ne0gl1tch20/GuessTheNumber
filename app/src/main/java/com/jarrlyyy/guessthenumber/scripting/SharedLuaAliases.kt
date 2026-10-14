package com.jarrlyyy.guessthenumber.scripting

import org.luaj.vm2.LuaValue

/** Android compatibility facade for the shared JVM Lua runtime. */
typealias LuaSandbox = com.jarrlyyy.guessthenumber.shared.scripting.LuaSandbox
typealias LuaBudgetExceededError = com.jarrlyyy.guessthenumber.shared.scripting.LuaBudgetExceededError

object LuaActivityPayloadValidator {
    fun validate(result: LuaValue) =
        com.jarrlyyy.guessthenumber.shared.scripting.LuaActivityPayloadValidator.validate(result)
}
