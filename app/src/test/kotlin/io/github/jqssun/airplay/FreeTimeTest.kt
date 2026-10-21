package io.github.jqssun.airplay

import org.junit.Assert.assertEquals
import org.junit.Test

class FreeTimeTest {
    @Test fun freshInstallHasTheFullHour() = assertEquals(3600, FreeTime.trialLeft(0))
    @Test fun trialNeverGoesNegative() = assertEquals(0, FreeTime.trialLeft(4000))
    @Test fun trialWhileMinutesLeft() = assertEquals(Plan.TRIAL, FreeTime.plan(unlimited = false, trialLeftSeconds = 5))
    @Test fun freeOnceTrialIsUsed() = assertEquals(Plan.FREE, FreeTime.plan(unlimited = false, trialLeftSeconds = 0))
    @Test fun purchaseBeatsTrial() = assertEquals(Plan.UNLIMITED, FreeTime.plan(unlimited = true, trialLeftSeconds = 3600))
    @Test fun minutesRoundUp() = assertEquals(listOf(0, 1, 2, 2), listOf(0, 60, 61, 120).map(FreeTime::minutes))
}
