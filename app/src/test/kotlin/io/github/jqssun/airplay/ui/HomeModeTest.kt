package io.github.jqssun.airplay.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModeTest {
    @Test fun mirrorWins() = assertEquals(HomeMode.MIRROR, homeMode(mirroring = true, audioOnly = true, idleMs = 999_999))
    @Test fun audioOnly() = assertEquals(HomeMode.AUDIO, homeMode(false, true, 999_999))
    @Test fun stepsBeforeTimeout() = assertEquals(HomeMode.STEPS, homeMode(false, false, 59_999))
    @Test fun ambientAtTimeout() = assertEquals(HomeMode.AMBIENT, homeMode(false, false, 60_000))

    @Test fun nameOk() = assertTrue(isValidTvName("Living Room TV"))
    @Test fun nameEmojiOk() = assertTrue(isValidTvName("Kids' TV 📺"))
    @Test fun nameBlank() = assertFalse(isValidTvName("   "))
    @Test fun nameTooLongChars() = assertFalse(isValidTvName("a".repeat(41)))
    @Test fun nameTooLongBytes() = assertFalse(isValidTvName("📺".repeat(13)))  // 52 UTF-8 bytes
    @Test fun nameMaxOk() = assertTrue(isValidTvName("a".repeat(40)))
}
