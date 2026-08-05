package com.nerdginger.projectmate.core.inbox

import com.nerdginger.projectmate.core.Fixtures
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.Origin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InboxRulesTest {

    private fun captured(id: String, origin: Origin): Item {
        val base = Fixtures.item(id, statusId = "untriaged")
        return base.copy(sync = base.sync.copy(origin = origin))
    }

    @Test
    fun `groups by where each thing came from`() {
        val result = InboxRules.group(
            listOf(
                captured("a", Origin.SHARE_INTENT),
                captured("b", Origin.WEB_PORTAL),
                captured("c", Origin.FILE_IMPORT),
            ),
        )

        assertEquals(
            listOf(Origin.WEB_PORTAL, Origin.SHARE_INTENT, Origin.FILE_IMPORT),
            result.map { it.origin },
        )
    }

    @Test
    fun `portal requests come first — someone is waiting on those`() {
        val result = InboxRules.group(
            listOf(captured("a", Origin.LOCAL), captured("b", Origin.WEB_PORTAL)),
        )

        assertEquals(Origin.WEB_PORTAL, result.first().origin)
    }

    @Test
    fun `a source with nothing in it gets no heading`() {
        val result = InboxRules.group(listOf(captured("a", Origin.SHARE_INTENT)))

        assertEquals(1, result.size)
        assertEquals(Origin.SHARE_INTENT, result.single().origin)
    }

    @Test
    fun `an empty inbox produces no groups`() {
        assertTrue(InboxRules.group(emptyList()).isEmpty())
    }

    @Test
    fun `deleted and archived things are not waiting for triage`() {
        val deleted = captured("deleted", Origin.WEB_PORTAL).let {
            it.copy(sync = it.sync.copy(deletedAt = 1L))
        }
        val archived = captured("archived", Origin.WEB_PORTAL).copy(archivedAt = 1L)
        val live = captured("live", Origin.WEB_PORTAL)

        val result = InboxRules.group(listOf(deleted, archived, live))

        assertEquals(listOf("live"), result.single().items.map { it.id })
        assertEquals(1, InboxRules.count(listOf(deleted, archived, live)))
    }

    @Test
    fun `each group counts its own rows`() {
        val result = InboxRules.group(
            listOf(
                captured("a", Origin.WEB_PORTAL),
                captured("b", Origin.WEB_PORTAL),
                captured("c", Origin.SHARE_INTENT),
            ),
        )

        assertEquals(2, result.first { it.origin == Origin.WEB_PORTAL }.count)
        assertEquals(1, result.first { it.origin == Origin.SHARE_INTENT }.count)
    }

    @Test
    fun `every source has a heading and a badge`() {
        // A source with no label would render a blank heading rather than fail.
        Origin.entries.forEach { origin ->
            val group = InboxGroup(origin, listOf(captured("x", origin)))
            assertTrue(group.label.isNotBlank(), "$origin has no heading")
            assertTrue(group.badge.isNotBlank(), "$origin has no badge")
        }
    }
}
