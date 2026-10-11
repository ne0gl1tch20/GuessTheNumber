package com.jarrlyyy.guessthenumber.scripting

import com.jarrlyyy.guessthenumber.domain.model.RandomEventType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LuaRandomEventSelectorTest {
    @Test
    fun bundledLuaSelectorReturnsOnlySupportedEventTypesOrNoEvent() {
        val selector = LuaRandomEventSelector(RuntimeEnvironment.getApplication())
        val supported = RandomEventType.values().toSet()
        val results = List(200) { selector.roll() }

        assertTrue("Lua selector should trigger at least one event", results.any { it != null })
        assertTrue(results.filterNotNull().all { it in supported })
        assertTrue("Lua selector should also produce no-event intervals", results.any { it == null })
    }

    @Test
    fun selectorUsesBundledLuaAsset() {
        val context = RuntimeEnvironment.getApplication()
        val source = context.assets.open("scripts/events/random_event.lua")
            .bufferedReader().use { it.readText() }

        assertNotNull(source)
        assertTrue(source.contains("trigger"))
        assertTrue(source.contains("event_types"))
    }
}
