package com.nerdginger.projectmate.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Where a due date falls relative to now, from the user's point of view. */
enum class DueBucket {
    OVERDUE,
    TODAY,
    TOMORROW,
    THIS_WEEK,
    LATER,
    NONE,
    ;

    /** Buckets that belong on the Today screen. */
    val isPressing: Boolean get() = this == OVERDUE || this == TODAY
}

/**
 * Due-date arithmetic, kept here rather than in a ViewModel so it can be tested
 * without an Android device.
 *
 * The distinction that matters is **all-day versus timed**. An item due
 * "Tuesday" is not late at one minute past midnight on Tuesday — it is late
 * once Tuesday is over. Getting this wrong makes the Today screen cry wolf all
 * day, and a Today screen you don't trust is worse than none.
 */
object DueDates {

    /**
     * @param dueAt epoch millis, or null if the item has no due date
     * @param hasTime whether [dueAt] carries a meaningful time of day
     * @param now current instant
     * @param zone the user's timezone — all "which day is it" questions are local
     */
    fun bucket(
        dueAt: Long?,
        hasTime: Boolean,
        now: Instant,
        zone: ZoneId,
    ): DueBucket {
        if (dueAt == null) return DueBucket.NONE

        val today = LocalDate.ofInstant(now, zone)
        val dueDate = LocalDate.ofInstant(Instant.ofEpochMilli(dueAt), zone)

        if (isOverdue(dueAt, hasTime, now, zone)) return DueBucket.OVERDUE

        return when (ChronoUnit.DAYS.between(today, dueDate)) {
            0L -> DueBucket.TODAY
            1L -> DueBucket.TOMORROW
            in 2L..7L -> DueBucket.THIS_WEEK
            else -> DueBucket.LATER
        }
    }

    /**
     * A timed item is late once its instant has passed. An all-day item is late
     * only once its whole day has passed.
     */
    fun isOverdue(
        dueAt: Long?,
        hasTime: Boolean,
        now: Instant,
        zone: ZoneId,
    ): Boolean {
        if (dueAt == null) return false

        return if (hasTime) {
            dueAt < now.toEpochMilli()
        } else {
            val dueDate = LocalDate.ofInstant(Instant.ofEpochMilli(dueAt), zone)
            val endOfDueDay = dueDate.plusDays(1).atStartOfDay(zone).toInstant()
            !now.isBefore(endOfDueDay)
        }
    }

    /** True if [dueAt] falls on today's local date, late or not. */
    fun isDueToday(dueAt: Long?, now: Instant, zone: ZoneId): Boolean {
        if (dueAt == null) return false
        return LocalDate.ofInstant(Instant.ofEpochMilli(dueAt), zone) ==
            LocalDate.ofInstant(now, zone)
    }

    /**
     * Normalises a picked date to an all-day due value: the start of that day
     * in the user's zone.
     */
    fun allDayOn(date: LocalDate, zone: ZoneId): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()
}
