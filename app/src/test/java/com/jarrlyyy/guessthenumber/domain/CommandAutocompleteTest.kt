package com.jarrlyyy.guessthenumber.domain

import com.jarrlyyy.guessthenumber.domain.command.CommandAutocomplete
import com.jarrlyyy.guessthenumber.domain.command.CommandParser
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandAutocompleteTest {
    @Test
    fun partialCommandSuggestsRegisteredCommand() {
        val suggestions = CommandAutocomplete.suggest("/gi")
        assertTrue(suggestions.any { it.insertText == "/give " })
    }

    @Test
    fun partialCurrencyArgumentSuggestsValidChoices() {
        val suggestions = CommandAutocomplete.suggest("/give m")
        assertTrue(suggestions.any { it.insertText == "/give money " })
    }

    @Test
    fun autoclickerArgumentSuggestsOnAndOff() {
        val suggestions = CommandAutocomplete.suggest("/autoclicker o")
        assertEquals(setOf("/autoclicker on", "/autoclicker off"), suggestions.map { it.insertText }.toSet())
    }

    @Test
    fun unknownCommandHasNoSuggestions() {
        assertTrue(CommandAutocomplete.suggest("/not-a-command").isEmpty())
    }

    @Test
    fun parserSplitsCommandAndArguments() {
        val parsed = CommandParser.parse("/give money 1000")
        assertEquals("/give", parsed?.name)
        assertEquals(listOf("money", "1000"), parsed?.arguments)
    }

    @Test
    fun parserRejectsTextWithoutCommandPrefix() {
        assertEquals(null, CommandParser.parse("give money 1000"))
    }
}
