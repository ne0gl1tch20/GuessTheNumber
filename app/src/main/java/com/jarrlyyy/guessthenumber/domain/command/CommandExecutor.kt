package com.jarrlyyy.guessthenumber.domain.command

import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState

object DeveloperConfig {
    val ENABLED = BuildConfig.DEBUG
}

class CommandExecutor {

    fun execute(commandLine: String, currentState: GameState, onStateUpdate: (GameState) -> Unit): String {
        if (!DeveloperConfig.ENABLED) return "Developer mode is disabled."
        val parsed = CommandParser.parse(commandLine) ?: return "Invalid command. Commands must start with '/'."
        val cmd = parsed.name
        val args = parsed.arguments

        GameLogger.log(LogLevel.INFO, LoggerCategory.COMMAND, "EXEC_COMMAND", "Executing: $commandLine")

        return when (cmd) {
            "/help" -> "Available commands:\n/give <currency> <amount>\n/set <currency> <amount>\n/reset <progression|prestige|ultra|all>\n/timeskip <seconds>\n/max_upgrades\n/unlock_all or /unlock all\n/win or /completeguess\n/speed <clicksPerSecond>\n/autoclicker <on|off>\n/setlevel <upgradeId> <level>\n/newnumber\n/state or /inspect\n/version\n/logs <clear|pause|resume>\n/stats\n/test antitimetravel\n/matrix\n/konami\n/moneyprinter\n/easteregg\n/crash_test"
            
            "/give" -> {
                if (args.size < 2) return "Usage: /give <currency> <amount>"
                val type = args[0].lowercase()
                val amount = BigNumber(args[1])
                val newState = when (type) {
                    "money" -> currentState.copy(money = currentState.money + amount)
                    "prestige" -> currentState.copy(prestige = currentState.prestige + amount)
                    "ultra" -> currentState.copy(ultra = currentState.ultra + amount)
                    "nebula" -> currentState.copy(nebula = currentState.nebula + amount)
                    "all" -> currentState.copy(
                        money = currentState.money + amount,
                        prestige = currentState.prestige + amount,
                        ultra = currentState.ultra + amount,
                        nebula = currentState.nebula + amount
                    )
                    else -> return "Unknown currency type: $type"
                }
                onStateUpdate(newState)
                "Given $amount $type."
            }

            "/set" -> {
                if (args.size < 2) return "Usage: /set <currency> <amount>"
                val type = args[0].lowercase()
                val amount = BigNumber(args[1])
                val newState = when (type) {
                    "money" -> currentState.copy(money = amount)
                    "prestige" -> currentState.copy(prestige = amount)
                    "ultra" -> currentState.copy(ultra = amount)
                    "nebula" -> currentState.copy(nebula = amount)
                    else -> return "Unknown currency type: $type"
                }
                onStateUpdate(newState)
                "Set $type to $amount."
            }

            "/reset" -> {
                val target = args.getOrNull(0)?.lowercase() ?: "progression"
                val newState = when (target) {
                    "progression" -> currentState.copy(money = BigNumber.ZERO, attempts = 0)
                    "prestige" -> currentState.copy(money = BigNumber.ZERO, prestige = BigNumber.ZERO, prestigeUpgradeLevels = emptyMap())
                    "ultra" -> currentState.copy(money = BigNumber.ZERO, prestige = BigNumber.ZERO, ultra = BigNumber.ZERO, ultraUpgradeLevels = emptyMap())
                    "all" -> GameState()
                    else -> currentState
                }
                onStateUpdate(newState)
                "Reset $target executed."
            }

            "/timeskip", "/skip" -> {
                val seconds = args.getOrNull(0)?.toLongOrNull() ?: 3600L
                val idleMoney = BigNumber(500) * BigNumber(seconds.toDouble())
                val newState = currentState.copy(
                    money = currentState.money + idleMoney,
                    statistics = currentState.statistics.copy(
                        playtimeSeconds = currentState.statistics.playtimeSeconds + seconds,
                        moneyEarned = currentState.statistics.moneyEarned + idleMoney
                    ),
                    lastSaveTimestamp = System.currentTimeMillis()
                )
                onStateUpdate(newState)
                "Time skipped by $seconds seconds. Earned $idleMoney passive money!"
            }

            "/max_upgrades", "/maxupgrades" -> {
                val maxNormal = mapOf(
                    "reward_multiplier" to 100,
                    "better_range" to 20,
                    "critical_boost" to 50,
                    "starting_money_boost" to 25,
                    "auto_efficiency" to 30,
                    "prestige_boost" to 40,
                    "lucky_multiplier" to 20,
                    "critical_multiplier" to 25,
                    "auto_income_boost" to 50,
                    "ultra_efficiency" to 20,
                    "discount_bargain" to 15
                )
                val maxPrestige = mapOf(
                    "prestige_money_boost" to 50,
                    "prestige_gain_boost" to 25,
                    "prestige_starting_capital" to 30
                )
                val maxUltra = mapOf(
                    "ultra_money_boost" to 20,
                    "ultra_gain_boost" to 10,
                    "ultra_nebula_boost" to 15
                )
                val newState = currentState.copy(
                    upgradeLevels = maxNormal,
                    prestigeUpgradeLevels = maxPrestige,
                    ultraUpgradeLevels = maxUltra
                )
                onStateUpdate(newState)
                "All standard, prestige, and ultra upgrades maxed to peak levels!"
            }

            "/unlock_all", "/unlock" -> {
                val newState = currentState.copy(
                    achievements = setOf("correct_10", "streak_5", "millionaire"),
                    completedChallenges = setOf("daily_guess_50"),
                    shopPurchases = setOf("auto_clicker_basic", "nebula_boost", "golden_guess"),
                    prestigeShopPurchases = setOf("prestige_auto_speed", "prestige_luck_charm"),
                    ultraShopPurchases = setOf("ultra_universal_harmony", "ultra_infinite_multiplier", "ultra_flex_badge"),
                    tutorialCompleted = true
                )
                onStateUpdate(newState)
                "All achievements, challenges, shop items (including Nebula, Prestige, and Ultra shops), and tutorial unlocked!"
            }

            "/win", "/completeguess" -> {
                val reward = BigNumber(10000)
                val newStreak = currentState.streak + 1
                val newBestStreak = maxOf(currentState.bestStreak, newStreak)
                val newState = currentState.copy(
                    money = currentState.money + reward,
                    correctGuesses = currentState.correctGuesses + 1,
                    attempts = currentState.attempts + 1,
                    streak = newStreak,
                    bestStreak = newBestStreak,
                    autoClickerActive = true,
                    statistics = currentState.statistics.copy(
                        correctGuesses = currentState.statistics.correctGuesses + 1,
                        totalGuesses = currentState.statistics.totalGuesses + 1,
                        moneyEarned = currentState.statistics.moneyEarned + reward
                    )
                )
                onStateUpdate(newState)
                "Forced win! +10,000 Money awarded, streak is now $newStreak."
            }

            "/test" -> {
                val sub = args.getOrNull(0)?.lowercase()
                if (sub == "antitimetravel" || sub == "time_travel") {
                    val fakePastSave = System.currentTimeMillis() + 86400000L // 24 hours in future
                    val penaltyUntil = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
                    val newState = currentState.copy(
                        lastSaveTimestamp = fakePastSave,
                        timeTravelPenaltyUntil = penaltyUntil,
                        autoClickerActive = false
                    )
                    onStateUpdate(newState)
                    "🧪 Anti-time travel simulated! Device clock is now set behind last save. 24-hour penalty applied."
                } else {
                    "Usage: /test antitimetravel"
                }
            }

            "/speed", "/clickspeed" -> {
                val speed = args.getOrNull(0)?.toDoubleOrNull() ?: 5.0
                val newState = currentState.copy(
                    autoClickerActive = true,
                    autoClickerSpeed = speed
                )
                onStateUpdate(newState)
                "Auto-clicker speed set to $speed clicks/sec."
            }


            "/autoclicker" -> {
                val requested = args.getOrNull(0)?.lowercase()
                    ?: return "Usage: /autoclicker <on|off>"
                val enabled = when (requested) {
                    "on", "true", "1" -> true
                    "off", "false", "0" -> false
                    else -> return "Use /autoclicker on or /autoclicker off."
                }
                onStateUpdate(currentState.copy(autoClickerActive = enabled))
                "Auto-clicker ${if (enabled) "enabled" else "disabled"}."
            }

            "/setlevel" -> {
                if (args.size < 2) return "Usage: /setlevel <upgradeId> <level>"
                val id = args[0].lowercase()
                if (!id.matches(Regex("[a-z0-9_]{1,64}"))) return "Invalid upgrade ID."
                val level = args[1].toIntOrNull()?.coerceIn(0, 1000)
                    ?: return "Level must be an integer from 0 to 1000."
                onStateUpdate(currentState.copy(upgradeLevels = currentState.upgradeLevels + (id to level)))
                "Set upgrade '$id' to level $level."
            }

            "/newnumber" -> {
                if (currentState.currentRangeMin >= currentState.currentRangeMax) return "Current guess range is invalid."
                val target = java.util.concurrent.ThreadLocalRandom.current().nextLong(
                    currentState.currentRangeMin,
                    currentState.currentRangeMax + 1
                )
                onStateUpdate(currentState.copy(targetNumber = target, attempts = 0))
                "Generated a new target in the current range."
            }

            "/state", "/inspect" -> {
                "State: range=${currentState.currentRangeMin}..${currentState.currentRangeMax}, attempts=${currentState.attempts}, correct=${currentState.correctGuesses}, streak=${currentState.streak}, autoClicker=${currentState.autoClickerActive}, upgrades=${currentState.upgradeLevels.size}, activeBoss=${currentState.activeBossBattleWorldId ?: "none"}."
            }

            "/version" -> "GuessTheNumber ${BuildConfig.VERSION_NAME} (versionCode ${BuildConfig.VERSION_CODE}; debug=${BuildConfig.DEBUG})."

            "/logs" -> {
                when (args.getOrNull(0)?.lowercase()) {
                    "clear" -> { GameLogger.clear(); "Developer logs cleared." }
                    "pause" -> { GameLogger.pause(); "Developer log stream paused." }
                    "resume" -> { GameLogger.resume(); "Developer log stream resumed." }
                    else -> "Usage: /logs <clear|pause|resume>"
                }
            }
\n            "/stats" -> {
                val stats = currentState.statistics
                "Stats: Playtime=${stats.playtimeSeconds}s | Guesses=${currentState.correctGuesses}/${currentState.attempts} | Streak=${currentState.streak} (Best ${currentState.bestStreak}) | Money=${currentState.money} | Prestige=${currentState.prestige} | Ultra=${currentState.ultra} | Nebula=${currentState.nebula}"
            }

            // Easter Eggs
            "/matrix" -> {
                GameLogger.log(LogLevel.INFO, LoggerCategory.GAME, "EASTER_EGG", "Wake up, Neo... The Matrix has you. 01010011 01100101 01100011 01110010 01100101 01110100")
                "🟢 Entering the Matrix... Check your logs for the digital rain message!"
            }

            "/konami" -> {
                val newState = currentState.copy(nebula = currentState.nebula + BigNumber(1000))
                onStateUpdate(newState)
                "🎮 KONAMI CODE ACCEPTED! ↑↑↓↓←→←→BA. +1,000 Nebula Crystals awarded!"
            }

            "/moneyprinter" -> {
                val newState = currentState.copy(money = currentState.money + BigNumber("1000000000"))
                onStateUpdate(newState)
                "💸 BRRRRRRRRRR! Federal Reserve money printer activated! +1,000,000,000 Money printed!"
            }

            "/easteregg" -> {
                "🔮 Secret Riddle: 'What do you get when you multiply six by nine? Or what is the ultimate answer?' Try guessing 42!"
            }

            "/crash_test" -> {
                GameLogger.log(LogLevel.FATAL, LoggerCategory.CRASH, "CRASH_TEST", "Deliberate crash test invoked.")
                throw RuntimeException("Developer Mode Crash Test Triggered")
            }

            else -> "Unknown command: $cmd. Type /help for assistance."
        }
    }
}
