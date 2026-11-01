package com.jarrlyyy.guessthenumber.shared.rules

/** Marks the next synchronous guess as automation so bot activity never pollutes player stats. */
object GuessSourceContext {
    private var botGuessPending = false

    fun markBotGuess() {
        botGuessPending = true
    }

    fun consumeBotGuess(): Boolean {
        val pending = botGuessPending
        botGuessPending = false
        return pending
    }

    fun clearBotGuess() {
        botGuessPending = false
    }
}
