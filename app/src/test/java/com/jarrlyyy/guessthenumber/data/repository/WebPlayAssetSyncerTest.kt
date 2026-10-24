package com.jarrlyyy.guessthenumber.data.repository

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebPlayAssetSyncerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val syncer = WebPlayAssetSyncer(context)

    @Test
    fun sha256MatchesKnownDigest() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            WebPlayAssetSyncer.sha256("abc".toByteArray())
        )
    }

    @Test
    fun pathHandlerServesBundledHtmlWhenNoCacheExists() {
        val response = syncer.pathHandler("web-play").handle("index.html")
        assertNotNull(response)
        assertEquals("text/html", response!!.mimeType)
        val html = response.data!!.bufferedReader().use { it.readText() }
        assertTrue(html.contains("<html") || html.contains("<!DOCTYPE html"))
    }

    @Test
    fun pathHandlerRejectsTraversal() {
        assertNull(syncer.pathHandler("web-play").handle("../locales/en_us.json"))
    }
}
