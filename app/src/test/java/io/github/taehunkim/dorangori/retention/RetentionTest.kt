package io.github.taehunkim.dorangori.retention

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RetentionTest {
    @Test fun offKeepsEverything() {
        assertNull(Retention.cutoffMs(1_000_000, 0))
    }

    @Test fun cutoffIsDaysBack() {
        assertEquals(1_000_000_000L - 7 * 86_400_000L, Retention.cutoffMs(1_000_000_000L, 7))
    }

    private val seoul = ZoneId.of("Asia/Seoul")

    @Test fun nextRunTodayWhenStillAhead() {
        val now = ZonedDateTime.of(2026, 11, 13, 1, 0, 0, 0, seoul)
        assertEquals(2 * 60L, Timing.delayUntilNext(3 * 60, now).toMinutes())
    }

    @Test fun nextRunTomorrowWhenPassed() {
        val now = ZonedDateTime.of(2026, 11, 13, 4, 0, 0, 0, seoul)
        assertEquals(23 * 60L, Timing.delayUntilNext(3 * 60, now).toMinutes())
    }
}
