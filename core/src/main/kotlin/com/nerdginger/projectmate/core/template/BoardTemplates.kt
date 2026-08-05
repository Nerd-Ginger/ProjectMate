package com.nerdginger.projectmate.core.template

import com.nerdginger.projectmate.core.model.BoardType
import com.nerdginger.projectmate.core.model.GroupBy
import com.nerdginger.projectmate.core.model.ItemType
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.core.model.ViewMode

/** One column in a template. */
data class StatusTemplate(
    val name: String,
    val category: StatusCategory,
    val colorArgb: Int,
    val isDefault: Boolean = false,
    /** Appears on the Today screen regardless of due dates. */
    val isFocus: Boolean = false,
    val wipLimit: Int? = null,
)

/**
 * A starting point for a new board.
 *
 * Templates seed a status set and some defaults, and then get out of the way —
 * everything they create is editable. They exist so that a Projects board and a
 * Life board are available immediately without either being hardcoded into the
 * app. See docs/DECISIONS.md D-001.
 */
data class BoardTemplate(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val boardType: BoardType,
    val accentColor: Int,
    val defaultViewMode: ViewMode,
    val defaultGroupBy: GroupBy,
    val defaultItemType: ItemType,
    val statuses: List<StatusTemplate>,
) {
    init {
        require(statuses.isNotEmpty()) { "template '$id' has no statuses" }
        require(statuses.count { it.isDefault } == 1) {
            "template '$id' must have exactly one default status"
        }
        require(statuses.map { it.name }.toSet().size == statuses.size) {
            "template '$id' has duplicate status names"
        }
    }
}

object BoardTemplates {

    // Colours come from [Palette], which is shared with the status colour
    // picker so the two can't drift apart. Statuses are coloured by what they
    // mean — not started, queued, in flight, blocked, finished — so a board
    // reads consistently even after every status has been renamed.

    /** Work you're building. Long-lived, moves through phases, sometimes stalls. */
    val Projects = BoardTemplate(
        id = "projects",
        name = "Projects",
        description = "Things you're building, from first idea to shipped.",
        emoji = "🛠️",
        boardType = BoardType.PROJECTS,
        accentColor = Palette.VIOLET,
        defaultViewMode = ViewMode.KANBAN,
        defaultGroupBy = GroupBy.STATUS,
        defaultItemType = ItemType.PROJECT,
        statuses = listOf(
            StatusTemplate("Idea", StatusCategory.INBOX, Palette.GREY),
            StatusTemplate("Planning", StatusCategory.BACKLOG, Palette.STONE, isDefault = true),
            // A WIP limit here is the whole point of the board: the warning
            // when you're building five things at once is the useful signal.
            StatusTemplate(
                "Building",
                StatusCategory.ACTIVE,
                Palette.ORANGE,
                isFocus = true,
                wipLimit = 3,
            ),
            StatusTemplate("Blocked", StatusCategory.BLOCKED, Palette.RED),
            StatusTemplate("In Review", StatusCategory.ACTIVE, Palette.AMBER, isFocus = true),
            StatusTemplate("Shipped", StatusCategory.DONE, Palette.TAUPE),
            StatusTemplate("Shelved", StatusCategory.CANCELLED, Palette.TAUPE),
        ),
    )

    /** Everything that just needs doing. Rarely "in review". */
    val Life = BoardTemplate(
        id = "life",
        name = "Life",
        description = "Errands, admin and obligations — the things that just need doing.",
        emoji = "🌱",
        boardType = BoardType.LIFE,
        accentColor = Palette.TEAL,
        defaultViewMode = ViewMode.LIST,
        defaultGroupBy = GroupBy.DUE,
        defaultItemType = ItemType.TASK,
        statuses = listOf(
            StatusTemplate("Inbox", StatusCategory.INBOX, Palette.GREY, isDefault = true),
            StatusTemplate("Today", StatusCategory.ACTIVE, Palette.ORANGE, isFocus = true),
            StatusTemplate("This Week", StatusCategory.ACTIVE, Palette.AMBER),
            StatusTemplate("Waiting On", StatusCategory.BLOCKED, Palette.RED),
            StatusTemplate("Someday", StatusCategory.BACKLOG, Palette.STONE),
            StatusTemplate("Done", StatusCategory.DONE, Palette.TAUPE),
        ),
    )

    /** Where requests from the website land. See docs/FEATURE_REQUEST_SCHEMA.md. */
    val FeatureRequests = BoardTemplate(
        id = "feature-requests",
        name = "Feature Requests",
        description = "Requests from your website, waiting to be triaged.",
        emoji = "📥",
        boardType = BoardType.FEATURE_REQUESTS,
        accentColor = Palette.PINK,
        defaultViewMode = ViewMode.KANBAN,
        defaultGroupBy = GroupBy.STATUS,
        defaultItemType = ItemType.FEATURE_REQUEST,
        statuses = listOf(
            StatusTemplate("Triage", StatusCategory.INBOX, Palette.GREY, isDefault = true),
            StatusTemplate("Accepted", StatusCategory.BACKLOG, Palette.STONE),
            StatusTemplate("In Progress", StatusCategory.ACTIVE, Palette.ORANGE, isFocus = true),
            StatusTemplate("Shipped", StatusCategory.DONE, Palette.TAUPE),
            StatusTemplate("Declined", StatusCategory.CANCELLED, Palette.TAUPE),
            StatusTemplate("Duplicate", StatusCategory.CANCELLED, Palette.TAUPE),
        ),
    )

    /** For when none of the above fit. */
    val Simple = BoardTemplate(
        id = "simple",
        name = "Simple",
        description = "Three columns. Nothing clever.",
        emoji = "📋",
        boardType = BoardType.CUSTOM,
        accentColor = Palette.BLUE,
        defaultViewMode = ViewMode.KANBAN,
        defaultGroupBy = GroupBy.STATUS,
        defaultItemType = ItemType.TASK,
        statuses = listOf(
            StatusTemplate("To Do", StatusCategory.BACKLOG, Palette.GREY, isDefault = true),
            StatusTemplate("Doing", StatusCategory.ACTIVE, Palette.ORANGE, isFocus = true),
            StatusTemplate("Done", StatusCategory.DONE, Palette.TAUPE),
        ),
    )

    /** The Inbox, seeded on first run. Capture now, triage later. */
    val Inbox = BoardTemplate(
        id = "inbox",
        name = "Inbox",
        description = "Anything shared or imported, waiting to be sorted.",
        emoji = "📨",
        boardType = BoardType.CUSTOM,
        accentColor = Palette.GOLD,
        defaultViewMode = ViewMode.LIST,
        defaultGroupBy = GroupBy.STATUS,
        defaultItemType = ItemType.NOTE,
        statuses = listOf(
            StatusTemplate("Untriaged", StatusCategory.INBOX, Palette.STONE, isDefault = true),
            StatusTemplate("Sorted", StatusCategory.DONE, Palette.TAUPE),
        ),
    )

    /** Offered when creating a board, in the order shown. */
    val all: List<BoardTemplate> = listOf(Projects, Life, FeatureRequests, Simple)

    fun byId(id: String): BoardTemplate? = (all + Inbox).firstOrNull { it.id == id }
}
