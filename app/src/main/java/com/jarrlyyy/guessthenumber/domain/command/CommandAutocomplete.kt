package com.jarrlyyy.guessthenumber.domain.command

/**
 * Single source of truth for command hints and autocomplete.
 * Keep entries aligned with CommandExecutor so the UI never advertises a fake command.
 */
data class DevCommandDefinition(
    val name: String,
    val usage: String,
    val description: String,
    val argumentOptions: List<String> = emptyList()
)

data class CommandSuggestion(
    val insertText: String,
    val title: String,
    val description: String
)

object CommandRegistry {
    val commands = listOf(
        DevCommandDefinition("/help", "/help", "Show all developer commands"),
        DevCommandDefinition("/give", "/give <currency> <amount>", "Add currency", listOf("/give money ", "/give prestige ", "/give ultra ", "/give nebula ", "/give all ")),
        DevCommandDefinition("/set", "/set <currency> <amount>", "Set a currency amount", listOf("/set money ", "/set prestige ", "/set ultra ", "/set nebula ")),
        DevCommandDefinition("/reset", "/reset <progression|prestige|ultra|all>", "Reset selected progression", listOf("/reset progression", "/reset prestige", "/reset ultra", "/reset all")),
        DevCommandDefinition("/timeskip", "/timeskip <seconds>", "Simulate offline progress"),
        DevCommandDefinition("/max_upgrades", "/max_upgrades", "Max out available upgrades"),
        DevCommandDefinition("/unlock_all", "/unlock_all", "Unlock developer test content"),
        DevCommandDefinition("/unlock", "/unlock all", "Alias for unlock_all", listOf("/unlock all")),
        DevCommandDefinition("/win", "/win", "Grant a test win"),
        DevCommandDefinition("/completeguess", "/completeguess", "Alias for the test win"),
        DevCommandDefinition("/speed", "/speed <clicksPerSecond>", "Set auto-clicker speed"),
        DevCommandDefinition("/autoclicker", "/autoclicker <on|off>", "Toggle the in-game auto-clicker", listOf("/autoclicker on", "/autoclicker off")),
        DevCommandDefinition("/setlevel", "/setlevel <upgradeId> <level>", "Set a normal upgrade level"),
        DevCommandDefinition("/newnumber", "/newnumber", "Generate a new target number"),
        DevCommandDefinition("/stats", "/stats", "Show progression statistics"),
        DevCommandDefinition("/state", "/state", "Inspect core game state"),
        DevCommandDefinition("/inspect", "/inspect", "Show a compact runtime state snapshot"),
        DevCommandDefinition("/version", "/version", "Show app build information"),
        DevCommandDefinition("/logs", "/logs <clear|pause|resume>", "Control the developer log stream", listOf("/logs clear", "/logs pause", "/logs resume")),
        DevCommandDefinition("/test", "/test antitimetravel", "Test anti-time-travel handling", listOf("/test antitimetravel")),
        DevCommandDefinition("/matrix", "/matrix", "Trigger a harmless log easter egg"),
        DevCommandDefinition("/konami", "/konami", "Grant the test Nebula reward"),
        DevCommandDefinition("/moneyprinter", "/moneyprinter", "Grant test Money"),
        DevCommandDefinition("/easteregg", "/easteregg", "Show the secret riddle"),
        DevCommandDefinition("/crash_test", "/crash_test", "Trigger crash recovery test (debug only)")
    )
}

object CommandAutocomplete {
    fun suggest(input: String, limit: Int = 8): List<CommandSuggestion> {
        val query = input.trimStart()
        if (query.isBlank()) return emptyList()
        val parts = query.split(Regex("\\s+"))
        val isTypingArgument = query.endsWith(" ") || parts.size > 1
        val matches = if (!isTypingArgument) {
            CommandRegistry.commands
                .filter { it.name.startsWith(parts.first(), ignoreCase = true) }
                .map { CommandSuggestion(it.name + if (it.usage.contains("<") || it.argumentOptions.isNotEmpty()) " " else "", it.usage, it.description) }
        } else {
            val command = parts.first().lowercase()
            val typed = if (query.endsWith(" ")) "" else parts.last()
            val definition = CommandRegistry.commands.firstOrNull { it.name == command } ?: return emptyList()
            definition.argumentOptions
                .filter { it.startsWith(query, ignoreCase = true) || (typed.isNotEmpty() && it.substringAfterLast(' ').startsWith(typed, ignoreCase = true)) }
                .map { CommandSuggestion(it, it.trim(), definition.description) }
        }
        return matches.distinctBy { it.insertText }.take(limit)
    }
}
