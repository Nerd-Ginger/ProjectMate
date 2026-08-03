package com.nerdginger.projectmate.core.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DueDatesTest {

    private val zone: ZoneId = ZoneId.of("Europe/London")

    private fun at(text: String) =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    private fun allDay(date: String) =
        DueDates.allDayOn(LocalDate.parse(date), zone)

    private fun timed(text: String) =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    // ------------------------------------------------------------ all-day items

    @Test
    fun `an all-day item is not overdue during its own day`() {
        val due = allDay("2026-08-04")

        // The bug this guards against: 00:00 storage making the item look late
        // from the first second of the day it's actually due.
        assertFalse(DueDates.isOverdue(due, hasTime = false, now = at("2026-08-04T00:00:01"), zone = zone))
        assertFalse(DueDates.isOverdue(due, hasTime = false, now = at("2026-08-04T13:00:00"), zone = zone))
        assertFalse(DueDates.isOverdue(due, hasTime = false, now = at("2026-08-04T23:59:59"), zone = zone))
    }

    @Test
    fun `an all-day item is overdue once its day has ended`() {
        val due = allDay("2026-08-04")

        assertTrue(DueDates.isOverdue(due, hasTime = false, now = at("2026-08-05T00:00:00"), zone = zone))
        assertTrue(DueDates.isOverdue(due, hasTime = false, now = at("2026-08-06T09:00:00"), zone = zone))
    }

    @Test
    fun `an all-day item due today buckets as today all day long`() {
        val due = allDay("2026-08-04")

        assertEquals(DueBucket.TODAY, DueDates.bucket(due, false, at("2026-08-04T00:30:00"), zone))
        assertEquals(DueBucket.TODAY, DueDates.bucket(due, false, at("2026-08-04T22:00:00"), zone))
    }

    // -------------------------------------------------------------- timed items

    @Test
    fun `a timed item is overdue the moment it passes`() {
        val due = timed("2026-08-04T09:00:00")

        assertFalse(DueDates.isOverdue(due, hasTime = true, now = at("2026-08-04T08:59:59"), zone = zone))
        assertTrue(DueDates.isOverdue(due, hasTime = true, now = at("2026-08-04T09:00:01"), zone = zone))
    }

    @Test
    fun `a timed item earlier today is overdue, not due-today`() {
        val due = timed("2026-08-04T09:00:00")

        assertEquals(DueBucket.OVERDUE, DueDates.bucket(due, true, at("2026-08-04T14:00:00"), zone))
    }

    @Test
    fun `a timed item later today is due today`() {
        val due = timed("2026-08-04T18:00:00")

        assertEquals(DueBucket.TODAY, DueDates.bucket(due, true, at("2026-08-04T14:00:00"), zone))
    }

    // ----------------------------------------------------------------- buckets

    @Test
    fun `buckets cover the near future`() {
        val now = at("2026-08-04T10:00:00")

        assertEquals(DueBucket.TOMORROW, DueDates.bucket(allDay("2026-08-05"), false, now, zone))
        assertEquals(DueBucket.THIS_WEEK, DueDates.bucket(allDay("2026-08-06"), false, now, zone))
        assertEquals(DueBucket.THIS_WEEK, DueDates.bucket(allDay("2026-08-11"), false, now, zone))
        assertEquals(DueBucket.LATER, DueDates.bucket(allDay("2026-08-12"), false, now, zone))
        assertEquals(DueBucket.NONE, DueDates.bucket(null, false, now, zone))
    }

    @Test
    fun `only overdue and today count as pressing`() {
        assertTrue(DueBucket.OVERDUE.isPressing)
        assertTrue(DueBucket.TODAY.isPressing)
        assertFalse(DueBucket.TOMORROW.isPressing)
        assertFalse(DueBucket.THIS_WEEK.isPressing)
        assertFalse(DueBucket.LATER.isPressing)
        assertFalse(DueBucket.NONE.isPressing)
    }

    // ----------------------------------------------------------------- timezone

    @Test
    fun `which day it is depends on the users timezone`() {
        // 23:30 in London on the 4th is already the 5th in Tokyo.
        val instant = at("2026-08-04T23:30:00")
        val dueLondon = allDay("2026-08-04")

        assertEquals(DueBucket.TODAY, DueDates.bucket(dueLondon, false, instant, ZoneId.of("Europe/London")))
        assertEquals(
            DueBucket.OVERDUE,
            DueDates.bucket(dueLondon, false, instant, ZoneId.of("Asia/Tokyo")),
        )
    }

    @Test
    fun `all-day normalisation lands on the start of the local day`() {
        val due = DueDates.allDayOn(LocalDate.parse("2026-08-04"), zone)

        assertTrue(DueDates.isDueToday(due, at("2026-08-04T12:00:00"), zone))
        assertFalse(DueDates.isDueToday(due, at("2026-08-05T00:00:01"), zone))
    }

    @Test
    fun `an item with no due date is never overdue`() {
        assertFalse(DueDates.isOverdue(null, hasTime = false, now = at("2026-08-04T10:00:00"), zone = zone))
        assertFalse(DueDates.isDueToday(null, at("2026-08-04T10:00:00"), zone))
    }
}
