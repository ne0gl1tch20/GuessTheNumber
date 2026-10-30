package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopLuaActivityEngineTest {
    @Test
    fun bundledCatalogLoadsAndRunsNumberRush() {
        val engine = DesktopLuaActivityEngine()
        val entry = engine.listScripts().single { it.id == "number_rush" }
        assertEquals("Number Rush", entry.title)

        val result = engine.start(entry.id)
        assertTrue(result is DesktopLuaStartResult.Started)
        val activity = (result as DesktopLuaStartResult.Started).activity
        assertEquals("Which number is a prime number?", activity.prompt)
        assertEquals(listOf("21", "29", "39", "51"), activity.options)
        assertEquals(1, activity.correctOption)
    }

    @Test
    fun answerCanBeSubmittedOnlyOnceAndActivityCanBeStopped() {
        val engine = DesktopLuaActivityEngine()
        assertTrue(engine.start("number_rush") is DesktopLuaStartResult.Started)
        val correct = engine.submitAnswer(1)
        assertTrue(correct?.correct == true)
        assertEquals("Correct! You spotted the prime number.", correct?.message)
        assertEquals(null, engine.submitAnswer(0))
        engine.stop()
        assertEquals(null, engine.currentActivity())
    }

    @Test
    fun unknownScriptIsRejectedWithoutStartingAnActivity() {
        val engine = DesktopLuaActivityEngine()
        val result = engine.start("not_registered")
        assertTrue(result is DesktopLuaStartResult.Rejected)
        assertFalse(engine.currentActivity() != null)
    }
}
