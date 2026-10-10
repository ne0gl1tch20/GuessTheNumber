package com.jarrlyyy.guessthenumber.domain

import com.jarrlyyy.guessthenumber.domain.command.CommandAutocomplete
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
}
