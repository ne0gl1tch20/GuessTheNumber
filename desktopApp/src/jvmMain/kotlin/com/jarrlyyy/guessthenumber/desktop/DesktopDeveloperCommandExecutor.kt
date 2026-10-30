package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import java.util.concurrent.ThreadLocalRandom

internal data class DesktopDeveloperCommandResult(
    val message: String,
    val updatedState: GameState? = null
)

/**
 * Deliberately small typed command allowlist. It never evaluates code, invokes a shell,
 * reads arbitrary files or accesses the network. Destructive commands are intentionally absent.
 */
internal class DesktopDeveloperCommandExecutor(
    private val developerToolsEnabled: () -> Boolean = { DesktopDebugToolsPolicy.enabled() }
) {
    fun execute(commandLine: String, state: GameState): DesktopDeveloperCommandResult {
        if (!developerToolsEnabled()) {
            return DesktopDeveloperCommandResult("Developer commands are disabled in this build.")
        }
        val clean = commandLine.trim()
        if (clean.length !in 2..256 || !clean.startsWith("/")) {
            return DesktopDeveloperCommandResult("Commands must start with '/' and stay under 256 characters.")
        }
        val tokens = clean.split(Regex("\\s+"))
        if (tokens.size > 4 || tokens.any { it.length > 80 }) {
            return DesktopDeveloperCommandResult("Command contains too many or oversized arguments.")
        }
        val command = tokens[0].lowercase()
        val args = tokens.drop(1)
        return when (command) {
            "/help" -> DesktopDeveloperCommandResult(
                "Commands: /help, /version, /state, /stats, /give <money|prestige|ultra|nebula> <amount>, /set <currency> <amount>, /setlevel <upgradeId> <0..1000>, /autoclicker <on|off>, /speed <0.1..50>, /newnumber."
            )
            "/version" -> DesktopDeveloperCommandResult(
                "GuessTheNumber Windows · JVM ${System.getProperty("java.version")} · Debug tools enabled."
            )
            "/state" -> DesktopDeveloperCommandResult(
                "Range=${state.currentRangeMin}..${state.currentRangeMax} · attempts=${state.attempts} · correct=${state.correctGuesses} · streak=${state.streak} · activeWorld=${state.activeWorldId} · upgrades=${state.upgradeLevels.size}."
            )
            "/stats" -> DesktopDeveloperCommandResult(
                "Playtime=${state.statistics.playtimeSeconds}s · guesses=${state.statistics.totalGuesses} · Money=${state.money} · Prestige=${state.prestige} · Ultra=${state.ultra} · Nebula=${state.nebula}."
            )
            "/give", "/set" -> {
                if (args.size != 2) return DesktopDeveloperCommandResult("Usage: $command <money|prestige|ultra|nebula> <non-negative amount>")
                val amount = runCatching { BigNumber(args[1]) }.getOrNull()
                    ?: return DesktopDeveloperCommandResult("Amount is not a valid number.")
                if (amount < BigNumber.ZERO) return DesktopDeveloperCommandResult("Negative currency values are not allowed.")
                val currency = args[0].lowercase()
                val add = command == "/give"
                val updated = when (currency) {
                    "money" -> state.copy(money = if (add) state.money + amount else amount)
                    "prestige" -> state.copy(prestige = if (add) state.prestige + amount else amount)
                    "ultra" -> state.copy(ultra = if (add) state.ultra + amount else amount)
                    "nebula" -> state.copy(nebula = if (add) state.nebula + amount else amount)
                    else -> return DesktopDeveloperCommandResult("Unknown currency: $currency")
                }
                DesktopDeveloperCommandResult("${if (add) "Added" else "Set"} $amount $currency.", updated)
            }
            "/setlevel" -> {
                if (args.size != 2 || !args[0].matches(Regex("[a-z0-9_]{1,64}"))) {
                    return DesktopDeveloperCommandResult("Usage: /setlevel <upgradeId> <0..1000>")
                }
                val level = args[1].toIntOrNull()?.takeIf { it in 0..1000 }
                    ?: return DesktopDeveloperCommandResult("Level must be an integer from 0 to 1000.")
                DesktopDeveloperCommandResult(
                    "Set ${args[0]} to level $level.",
                    state.copy(upgradeLevels = state.upgradeLevels + (args[0] to level))
                )
            }
            "/autoclicker" -> {
                val enabled = when (args.singleOrNull()?.lowercase()) {
                    "on" -> true
                    "off" -> false
                    else -> return DesktopDeveloperCommandResult("Usage: /autoclicker <on|off>")
                }
                DesktopDeveloperCommandResult(
                    "Auto-clicker ${if (enabled) "enabled" else "disabled"}.",
                    state.copy(autoClickerActive = enabled)
                )
            }
            "/speed" -> {
                val speed = args.singleOrNull()?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.1..50.0 }
                    ?: return DesktopDeveloperCommandResult("Usage: /speed <0.1..50>")
                DesktopDeveloperCommandResult("Auto-clicker speed set to $speed.", state.copy(autoClickerSpeed = speed))
            }
            "/newnumber" -> {
                if (state.currentRangeMin >= state.currentRangeMax) {
                    return DesktopDeveloperCommandResult("Current guessing range is invalid.")
                }
                val target = if (state.currentRangeMax == Long.MAX_VALUE) {
                    ThreadLocalRandom.current().nextLong(state.currentRangeMin, Long.MAX_VALUE)
                } else {
                    ThreadLocalRandom.current().nextLong(state.currentRangeMin, state.currentRangeMax + 1)
                }
                DesktopDeveloperCommandResult(
                    "Generated a new target in the current range.",
                    state.copy(targetNumber = target, attempts = 0)
                )
            }
            else -> DesktopDeveloperCommandResult("Unknown or unsupported command. Use /help.")
        }
    }
}
