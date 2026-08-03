package com.nerdginger.projectmate.core.focus

import com.nerdginger.projectmate.core.Fixtures
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.core.time.DueDates
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodayRulesTest {

    private val zone: ZoneId = ZoneId.of("Europe/London")
    private val now = LocalDateTime.parse("2026-08-04T10:00:00").atZone(zone).toInstant()

    private fun allDay(date: String) = DueDates.allDayOn(LocalDate.parse(date), zone)

    private val backlog = Fixtures.status("backlog", StatusCategory.BACKLOG)
    private val building = Fixtures.status("building", StatusCategory.ACTIVE, isFocus = true)
    private val active = Fixtures.status("active", StatusCategory.ACTIVE)
    private val blocked = Fixtures.status("blocked", StatusCategory.BLOCKED)
    private val done = Fixtures.status("done", StatusCategory.DONE)
    private val cancelled = Fixtures.status("cancelled", StatusCategory.CANCELLED)

    private val statuses = listOf(backlog, building, active, blocked, done, cancelled)
        .associateBy { it.id }

    private fun today(vararg items: com.nerdginger.projectmate.core.model.Item) =
        TodayRules.today(items.toList(), statuses, now, zone)

    @Test
    fun `overdue, due today and focus work all appear`() {
        val overdue = Fixtures.item("overdue", active.id, dueAt = allDay("2026-08-01"))
        val dueToday = Fixtures.item("dueToday", active.id, dueAt = allDay("2026-08-04"))
        val inFocus = Fixtures.item("inFocus", building.id)

        val result = today(overdue, dueToday, inFocus)

        assertEquals(listOf("overdue", "dueToday", "inFocus"), result.map { it.item.id })
        assertEquals(
            listOf(FocusReason.OVERDUE, FocusReason.DUE_TODAY, FocusReason.IN_FOCUS_STATUS),
            result.map { it.reason },
        )
    }

    @Test
    fun `work with no due date in a non-focus status stays off the screen`() {
        val quiet = Fixtures.item("quiet", backlog.id)

        assertTrue(today(quiet).isEmpty())
    }

    @Test
    fun `future due dates stay off the screen`() {
        val tomorrow = Fixtures.item("tomorrow", active.id, dueAt = allDay("2026-08-05"))
        val nextWeek = Fixtures.item("nextWeek", active.id, dueAt = allDay("2026-08-20"))

        assertTrue(today(tomorrow, nextWeek).isEmpty())
    }

    /**
     * The noisiest possible bug: a finished item with an old due date nagging
     * forever. Terminal statuses are excluded whatever the date says.
     */
    @Test
    fun `finished and abandoned work never appears, however overdue`() {
        val shipped = Fixtures.item("shipped", done.id, dueAt = allDay("2026-01-01"))
        val shelved = Fixtures.item("shelved", cancelled.id, dueAt = allDay("2026-01-01"))

        assertTrue(today(shipped, shelved).isEmpty())
    }

    @Test
    fun `blocked work still surfaces when it is overdue`() {
        // Blocked is not terminal — something stalled and past its date is
        // exactly what you want to be reminded of.
        val stalled = Fixtures.item("stalled", blocked.id, dueAt = allDay("2026-07-30"))

        assertEquals(listOf("stalled"), today(stalled).map { it.item.id })
    }

    @Test
    fun `archived and deleted items are excluded`() {
        val archived = Fixtures.item("archived", active.id, dueAt = allDay("2026-08-01"), archivedAt = 1L)
        val deleted = Fixtures.item("deleted", active.id, dueAt = allDay("2026-08-01"), deletedAt = 1L)

        assertTrue(today(archived, deleted).isEmpty())
    }

    @Test
    fun `an item whose status is missing is skipped rather than crashing`() {
        val orphan = Fixtures.item("orphan", "no-such-status", dueAt = allDay("2026-08-01"))

        assertTrue(today(orphan).isEmpty())
    }

    @Test
    fun `within a group, urgency wins then the earlier due date`() {
        val normal = Fixtures.item("normal", active.id, dueAt = allDay("2026-07-28"), priority = Priority.NORMAL)
        val urgent = Fixtures.item("urgent", active.id, dueAt = allDay("2026-08-01"), priority = Priority.URGENT)
        val alsoNormalEarlier =
            Fixtures.item("earlier", active.id, dueAt = allDay("2026-07-20"), priority = Priority.NORMAL)

        val result = today(normal, urgent, alsoNormalEarlier)

        assertEquals(listOf("urgent", "earlier", "normal"), result.map { it.item.id })
    }

    @Test
    fun `an item both overdue and in a focus status is listed once, as overdue`() {
        val both = Fixtures.item("both", building.id, dueAt = allDay("2026-08-01"))

        val result = today(both)

        assertEquals(1, result.size)
        assertEquals(FocusReason.OVERDUE, result.single().reason)
    }

    @Test
    fun `results span every board`() {
        val fromProjects = Fixtures.item("project", building.id, boardId = "projects")
        val fromLife = Fixtures.item("chore", active.id, dueAt = allDay("2026-08-04"), boardId = "life")

        val result = today(fromProjects, fromLife)

        assertEquals(setOf("projects", "life"), result.map { it.item.boardId }.toSet())
    }
}
