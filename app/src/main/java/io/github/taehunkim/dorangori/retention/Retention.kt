package io.github.taehunkim.dorangori.retention

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

object Retention {
    /** Recordings made before the returned time are expired; null when [days] keeps everything. */
    fun cutoffMs(nowMs: Long, days: Int): Long? = if (days <= 0) null else nowMs - days * 24L * 60 * 60 * 1000
}

object Timing {
    /** Delay from [now] to the next time the clock reads [minutesOfDay] (today if still ahead, else tomorrow). */
    fun delayUntilNext(minutesOfDay: Int, now: ZonedDateTime = ZonedDateTime.now(), zone: ZoneId = now.zone): Duration {
        val today = now.withZoneSameInstant(zone).toLocalDate().atStartOfDay(zone).plusMinutes(minutesOfDay.toLong())
        val next = if (today.isAfter(now)) today else today.plusDays(1)
        return Duration.between(now, next)
    }
}
