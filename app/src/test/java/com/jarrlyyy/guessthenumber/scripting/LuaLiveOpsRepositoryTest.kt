package com.jarrlyyy.guessthenumber.scripting

import com.jarrlyyy.guessthenumber.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LuaLiveOpsRepositoryTest {
    @Test
    fun refreshFailsClosedWithoutSigningKey() = runBlocking {
        if (BuildConfig.LIVEOPS_PUBLIC_KEY_BASE64.isBlank()) {
            val result = LuaLiveOpsRepository(RuntimeEnvironment.getApplication()).refresh()
            assertTrue(result is LuaLiveOpsRefreshResult.Unavailable)
            assertTrue((result as LuaLiveOpsRefreshResult.Unavailable).reason.contains("signing key"))
        }
    }
}
