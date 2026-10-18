package io.github.jqssun.airplay

import org.junit.Assert.assertEquals
import org.junit.Test

class TierTest {
    @Test fun freeLandscape1080() = assertEquals(1280 to 720, capForTier(1920, 1080, false))
    @Test fun freeLandscape4k() = assertEquals(1280 to 720, capForTier(3840, 2160, false))
    @Test fun freePortrait() = assertEquals(720 to 1280, capForTier(1080, 1920, false))
    @Test fun freeFourByThree() = assertEquals(960 to 720, capForTier(1024, 768, false))
    @Test fun freeOddPanelRoundsDownToEven() = assertEquals(1280 to 718, capForTier(1366, 768, false))
    @Test fun freeAlreadySmall() = assertEquals(1280 to 720, capForTier(1280, 720, false))
    @Test fun neverUpscales() = assertEquals(960 to 540, capForTier(960, 540, false))
    @Test fun unlocked4kCapsTo1080() = assertEquals(1920 to 1080, capForTier(3840, 2160, true))
    @Test fun unlocked1080Unchanged() = assertEquals(1920 to 1080, capForTier(1920, 1080, true))
    @Test fun unlockedPortrait() = assertEquals(1080 to 1920, capForTier(2160, 3840, true))

    @Test fun fitKeepsAspectForPortraitIntoLandscapeMax() = assertEquals(612 to 1088, fitWithin(1080, 1920, 1920, 1088))
    @Test fun fitInsideMaxUnchanged() = assertEquals(1280 to 720, fitWithin(1280, 720, 1920, 1088))
}
