package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopMusicBridgeTest {
    @Test
    fun emptyLibraryReturnsACompleteSharedPlayerState() {
        val state = DesktopMusicBridge.state(GameSettings())

        assertEquals(0, state.getJSONArray("library").length())
        assertEquals(0, state.getJSONArray("queue").length())
        assertEquals(0, state.getJSONObject("playlists").length())
        assertFalse(state.getBoolean("isPlaying"))
        assertEquals("OFF", state.getString("repeatMode"))
    }

    @Test
    fun unsupportedActionsReturnAnErrorInsteadOfExecuting() {
        val request = JSONObject()
            .put("version", 1)
            .put("id", "music-invalid-action")
            .put("type", "music_action")
            .put("action", "run_external_command")

        val state = DesktopMusicBridge.perform("run_external_command", request, GameSettings()) {}

        assertTrue(state.getString("status").contains("Unsupported music action"))
        assertFalse(state.getBoolean("isPlaying"))
    }
}
