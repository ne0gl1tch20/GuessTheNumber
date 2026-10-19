package com.jarrlyyy.guessthenumber.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DailyRewardPolicyTest {
    @Test fun firstClaimStartsAtDayOne() {
        assertEquals(1, DailyRewardPolicy.nextStreak("", 0, "2026-10-20"))
        assertEquals(5L, DailyRewardPolicy.nextReward("", 0, "2026-10-20"))
    }
    @Test fun consecutiveClaimsAdvanceAndCycleAfterSevenDays() {
        assertEquals(2, DailyRewardPolicy.nextStreak("2026-10-19", 1, "2026-10-20"))
        assertEquals(10L, DailyRewardPolicy.nextReward("2026-10-19", 1, "2026-10-20"))
        assertEquals(1, DailyRewardPolicy.nextStreak("2026-10-19", 7, "2026-10-20"))
    }
    @Test fun missedDayResetsStreak() {
        assertEquals(1, DailyRewardPolicy.nextStreak("2026-10-17", 3, "2026-10-20"))
    }
    @Test fun sameDayClaimKeepsCurrentRewardDay() {
        assertEquals(3, DailyRewardPolicy.nextStreak("2026-10-20", 3, "2026-10-20"))
        assertEquals(15L, DailyRewardPolicy.nextReward("2026-10-20", 3, "2026-10-20"))
    }
}
