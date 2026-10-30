package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopDeveloperCommandExecutorTest {
    @Test
    fun blocksCommandsWhenBuildOrLaunchGateIsDisabled() {
        val executor = DesktopDeveloperCommandExecutor { false }
        val state = GameState()
        val result = executor.execute("/give nebula 100", state)

        assertTrue(result.message.contains("disabled"))
        assertNull(result.updatedState)
    }

    @Test
    fun givesCurrencyOnlyAfterStrictTypedValidation() {
        val executor = DesktopDeveloperCommandExecutor { true }
        val state = GameState(nebula = BigNumber(5))

        val result = executor.execute("/give nebula 25", state)

        assertEquals(BigNumber(30), result.updatedState?.nebula)
        assertEquals(BigNumber(5), state.nebula)
    }

    @Test
    fun rejectsNegativeCurrencyAndUnknownCommands() {
        val executor = DesktopDeveloperCommandExecutor { true }

        assertNull(executor.execute("/give money -1", GameState()).updatedState)
        assertTrue(executor.execute("/exec println(1)", GameState()).message.contains("unsupported"))
    }

    @Test
    fun validatesUpgradeIdsAndLevels() {
        val executor = DesktopDeveloperCommandExecutor { true }

        assertNull(executor.execute("/setlevel ../../file 10", GameState()).updatedState)
        assertNull(executor.execute("/setlevel reward_multiplier 1001", GameState()).updatedState)
        assertEquals(10, executor.execute("/setlevel reward_multiplier 10", GameState()).updatedState?.upgradeLevels?.get("reward_multiplier"))
    }

    @Test
    fun autoclickerAndSpeedCommandsAreBounded() {
        val executor = DesktopDeveloperCommandExecutor { true }

        assertEquals(true, executor.execute("/autoclicker on", GameState()).updatedState?.autoClickerActive)
        assertNull(executor.execute("/speed 500", GameState()).updatedState)
        assertEquals(5.0, executor.execute("/speed 5", GameState()).updatedState?.autoClickerSpeed!!, 0.0)
    }

    @Test
    fun rejectsOversizedCommandLines() {
        val executor = DesktopDeveloperCommandExecutor { true }
        assertNull(executor.execute("/" + "x".repeat(300), GameState()).updatedState)
    }
}
