package com.nerdginger.projectmate.core.template

import com.nerdginger.projectmate.core.model.StatusCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardTemplatesTest {

    private val everyTemplate = BoardTemplates.all + BoardTemplates.Inbox

    @Test
    fun `every template has exactly one default status`() {
        everyTemplate.forEach { template ->
            assertEquals(
                1,
                template.statuses.count { it.isDefault },
                "template '${template.id}' must have exactly one default status",
            )
        }
    }

    @Test
    fun `every template offers a way to finish`() {
        // Without a DONE status, progress is stuck at zero forever and "hide
        // completed" has nothing to hide.
        everyTemplate.forEach { template ->
            assertTrue(
                template.statuses.any { it.category == StatusCategory.DONE },
                "template '${template.id}' has no DONE status",
            )
        }
    }

    @Test
    fun `template ids are unique and stable`() {
        val ids = everyTemplate.map { it.id }

        assertEquals(ids.size, ids.toSet().size, "template ids must be unique")
        // These are persisted on boards as templateId — renaming one orphans
        // existing rows.
        assertEquals(
            listOf("projects", "life", "feature-requests", "simple", "inbox"),
            ids,
        )
    }

    @Test
    fun `status names within a template are unique`() {
        everyTemplate.forEach { template ->
            val names = template.statuses.map { it.name }
            assertEquals(names.size, names.toSet().size, "template '${template.id}' repeats a status name")
        }
    }

    @Test
    fun `the projects template models a real project lifecycle`() {
        val projects = BoardTemplates.Projects

        assertEquals(
            listOf("Idea", "Planning", "Building", "Blocked", "In Review", "Shipped", "Shelved"),
            projects.statuses.map { it.name },
        )
        // Blocked and shelved are distinct from done, so a stalled or
        // abandoned project never counts as finished.
        assertEquals(StatusCategory.BLOCKED, projects.statuses.single { it.name == "Blocked" }.category)
        assertEquals(StatusCategory.CANCELLED, projects.statuses.single { it.name == "Shelved" }.category)
        assertEquals(StatusCategory.DONE, projects.statuses.single { it.name == "Shipped" }.category)
    }

    @Test
    fun `the life template starts in an inbox so capture is frictionless`() {
        val life = BoardTemplates.Life
        val default = life.statuses.single { it.isDefault }

        assertEquals("Inbox", default.name)
        assertEquals(StatusCategory.INBOX, default.category)
    }

    @Test
    fun `the feature request template matches the portal workflow`() {
        val requests = BoardTemplates.FeatureRequests

        assertEquals("Triage", requests.statuses.single { it.isDefault }.name)
        // Declined and duplicate are cancelled, not done — they must not
        // inflate the shipped count.
        assertTrue(
            requests.statuses.filter { it.name in setOf("Declined", "Duplicate") }
                .all { it.category == StatusCategory.CANCELLED },
        )
    }

    @Test
    fun `each template marks at least one status as focus`() {
        // Focus statuses are what make the Today screen useful for work that
        // has no due date.
        BoardTemplates.all.forEach { template ->
            assertTrue(
                template.statuses.any { it.isFocus },
                "template '${template.id}' has no focus status",
            )
        }
    }

    @Test
    fun `templates are looked up by id`() {
        assertNotNull(BoardTemplates.byId("projects"))
        assertNotNull(BoardTemplates.byId("inbox"))
        assertNull(BoardTemplates.byId("nope"))
    }

    @Test
    fun `the inbox is not offered as a choice when creating a board`() {
        // It is seeded once on first run and is a system board.
        assertTrue(BoardTemplates.all.none { it.id == "inbox" })
    }

    @Test
    fun `a template without exactly one default is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> {
            BoardTemplates.Simple.copy(
                statuses = BoardTemplates.Simple.statuses.map { it.copy(isDefault = true) },
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BoardTemplates.Simple.copy(statuses = emptyList())
        }
    }

    // The colour picker offers Palette and nothing else. If a template seeded a
    // colour the picker can't offer, editing a status would silently change it
    // to something else — so the two have to stay in step.

    @Test
    fun `every seeded status colour is one the picker can offer`() {
        val offered = Palette.statusColors.toSet()
        (BoardTemplates.all + BoardTemplates.Inbox).forEach { template ->
            template.statuses.forEach { status ->
                assertTrue(
                    status.colorArgb in offered,
                    "${template.id}/${status.name} uses a colour outside Palette.statusColors",
                )
            }
        }
    }

    @Test
    fun `every board accent is one the picker can offer`() {
        val offered = Palette.boardAccents.toSet()
        (BoardTemplates.all + BoardTemplates.Inbox).forEach { template ->
            assertTrue(
                template.accentColor in offered,
                "${template.id} uses an accent outside Palette.boardAccents",
            )
        }
    }

    @Test
    fun `the palette has no duplicates`() {
        assertEquals(Palette.statusColors.size, Palette.statusColors.toSet().size)
        assertEquals(Palette.boardAccents.size, Palette.boardAccents.toSet().size)
    }

    @Test
    fun `blocked statuses all use the one alarm colour`() {
        // The palette has exactly one red. Two would dilute it.
        (BoardTemplates.all + BoardTemplates.Inbox)
            .flatMap { it.statuses }
            .filter { it.category == StatusCategory.BLOCKED }
            .forEach { assertEquals(Palette.RED, it.colorArgb, "${it.name} is not the alarm colour") }
    }
}
