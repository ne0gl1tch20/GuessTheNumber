package com.jarrlyyy.guessthenumber.scripting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LuaEngineTest {
    @Test
    fun goStartsBundledActivityAndValidatesAnswerLifecycle() {
        val engine = LuaEngine.get(RuntimeEnvironment.getApplication())
        engine.stop()
        val started = engine.startFeaturedActivity()

        assertTrue(started is LuaStartResult.Started)
        val activity = (started as LuaStartResult.Started).activity
        assertEquals("number_rush", activity.id)
        assertEquals(4, activity.options.size)
        assertTrue(engine.status().activeInstanceId == activity.instanceId)

        val answer = engine.submitAnswer(activity.correctOption)
        assertTrue(answer is LuaAnswerResult.Answered && answer.correct)
        assertTrue(engine.submitAnswer(activity.correctOption) is LuaAnswerResult.Rejected)
        assertTrue(engine.stop(activity.instanceId))
        assertFalse(engine.status().activeInstanceId != null)
    }

    @Test
    fun developerConsoleUsesRegisteredScriptIdsInsteadOfRawSource() {
        val engine = LuaEngine.get(RuntimeEnvironment.getApplication())
        engine.stop()
        assertTrue(engine.executeConsoleCommand("/lua list").contains("number_rush"))
        assertTrue(engine.executeConsoleCommand("/lua run number_rush").contains("Started Number Rush"))
        assertTrue(engine.executeConsoleCommand("/lua eval os.execute('bad')").contains("disabled"))
        assertTrue(engine.executeConsoleCommand("/lua stop").contains("stopped"))
    }

    @Test
    fun unknownScriptAndInvalidAnswerAreRecoverable() {
        val engine = LuaEngine.get(RuntimeEnvironment.getApplication())
        engine.stop()
        assertTrue(engine.start("not_registered") is LuaStartResult.Rejected)
        assertTrue(engine.submitAnswer(999) is LuaAnswerResult.Rejected)
    }

    @Test
    fun repeatedStartIsIdempotentAndStaleStopCannotEndCurrentActivity() {
        val engine = LuaEngine.get(RuntimeEnvironment.getApplication())
        engine.stop()

        val first = engine.startFeaturedActivity()
        assertTrue(first is LuaStartResult.Started)
        val activity = (first as LuaStartResult.Started).activity

        val repeated = engine.start(activity.id)
        assertTrue(repeated is LuaStartResult.Started)
        assertEquals(activity.instanceId, (repeated as LuaStartResult.Started).activity.instanceId)

        assertFalse(engine.stop("stale-instance-id"))
        assertEquals(activity.instanceId, engine.status().activeInstanceId)

        assertTrue(engine.stop(activity.instanceId))
        assertTrue(engine.startFeaturedActivity() is LuaStartResult.Started)
        engine.stop()
    }

}
