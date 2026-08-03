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

    // Muted, distinguishable, and readable against both light and dark
    // surfaces. Kept here so templates and the status colour picker agree.
    private const val SLATE = 0xFF64748B.toInt()
    private const val BLUE = 0xFF3B82F6.toInt()
    private const val INDIGO = 0xFF6366F1.toInt()
    private const val AMBER = 0xFFF59E0B.toInt()
    private const val RED = 0xFFEF4444.toInt()
    private const val VIOLET = 0xFF8B5CF6.toInt()
    private const val GREEN = 0xFF22C55E.toInt()
    private const val TEAL = 0xFF14B8A6.toInt()
    private const val STONE = 0xFF78716C.toInt()

    /** Work you're building. Long-lived, moves through phases, sometimes stalls. */
    val Projects = BoardTemplate(
        id = "projects",
        name = "Projects",
        description = "Things you're building, from first idea to shipped.",
        emoji = "🛠️",
        boardType = BoardType.PROJECTS,
        accentColor = INDIGO,
        defaultViewMode = ViewMode.KANBAN,
        defaultGroupBy = GroupBy.STATUS,
        defaultItemType = ItemType.PROJECT,
        statuses = listOf(
            StatusTemplate("Idea", StatusCategory.INBOX, SLATE),
            StatusTemplate("Planning", StatusCategory.BACKLOG, BLUE, isDefault = true),
            // A WIP limit here is the whole point of the board: the warning
            // when you're building five things at once is the useful signal.
            StatusTemplate("Building", StatusCategory.ACTIVE, INDIGO, isFocus = true, wipLimit = 3),
            StatusTemplate("Blocked", StatusCategory.BLOCKED, RED),
            StatusTemplate("In Review", StatusCategory.ACTIVE, VIOLET, isFocus = true),
            StatusTemplate("Shipped", StatusCategory.DONE, GREEN),
            StatusTemplate("Shelved", StatusCategory.CANCELLED, STONE),
        ),
    )

    /** Everything that just needs doing. Rarely "in review". */
    val Life = BoardTemplate(
        id = "life",
        name = "Life",
        description = "Errands, admin and obligations — the things that just need doing.",
        emoji = "🌱",
        boardType = BoardType.LIFE,
        accentColor = TEAL,
        defaultViewMode = ViewMode.LIST,
        defaultGroupBy = GroupBy.DUE,
        defaultItemType = ItemType.TASK,
        statuses = listOf(
            StatusTemplate("Inbox", StatusCategory.INBOX, SLATE, isDefault = true),
            StatusTemplate("Today", StatusCategory.ACTIVE, TEAL, isFocus = true),
            StatusTemplate("This Week", StatusCategory.ACTIVE, BLUE),
            StatusTemplate("Waiting On", StatusCategory.BLOCKED, AMBER),
            StatusTemplate("Someday", StatusCategory.BACKLOG, STONE),
            StatusTemplate("Done", StatusCategory.DONE, GREEN),
        ),
    )

    /** Where requests from the website land. See docs/FEATURE_REQUEST_SCHEMA.md. */
    val FeatureRequests = BoardTemplate(
        id = "feature-requests",
        name = "Feature Requests",
        description = "Requests from your website, waiting to be triaged.",
        emoji = "📥",
        boardType = BoardType.FEATURE_REQUESTS,
        accentColor = VIOLET,
        defaultViewMode = ViewMode.KANBAN,
        defaultGroupBy = GroupBy.STATUS,
        defaultItemType = ItemType.FEATURE_REQUEST,
        statuses = listOf(
            StatusTemplate("Triage", StatusCategory.INBOX, SLATE, isDefault = true),
            StatusTemplate("Accepted", StatusCategory.BACKLOG, BLUE),
            StatusTemplate("In Progress", StatusCategory.ACTIVE, INDIGO, isFocus = true),
            StatusTemplate("Shipped", StatusCategory.DONE, GREEN),
            StatusTemplate("Declined", StatusCategory.CANCELLED, STONE),
            StatusTemplate("Duplicate", StatusCategory.CANCELLED, STONE),
        ),
    )

    /** For when none of the above fit. */
    val Simple = BoardTemplate(
        id = "simple",
        name = "Simple",
        description = "Three columns. Nothing clever.",
        emoji = "📋",
        boardType = BoardType.CUSTOM,
        accentColor = SLATE,
        defaultViewMode = ViewMode.KANBAN,
        defaultGroupBy = GroupBy.STATUS,
        defaultItemType = ItemType.TASK,
        statuses = listOf(
            StatusTemplate("To Do", StatusCategory.BACKLOG, SLATE, isDefault = true),
            StatusTemplate("Doing", StatusCategory.ACTIVE, BLUE, isFocus = true),
            StatusTemplate("Done", StatusCategory.DONE, GREEN),
        ),
    )

    /** The Inbox, seeded on first run. Capture now, triage later. */
    val Inbox = BoardTemplate(
        id = "inbox",
        name = "Inbox",
        description = "Anything shared or imported, waiting to be sorted.",
        emoji = "📨",
        boardType = BoardType.CUSTOM,
        accentColor = AMBER,
        defaultViewMode = ViewMode.LIST,
        defaultGroupBy = GroupBy.STATUS,
        defaultItemType = ItemType.NOTE,
        statuses = listOf(
            StatusTemplate("Untriaged", StatusCategory.INBOX, AMBER, isDefault = true),
            StatusTemplate("Sorted", StatusCategory.DONE, GREEN),
        ),
    )

    /** Offered when creating a board, in the order shown. */
    val all: List<BoardTemplate> = listOf(Projects, Life, FeatureRequests, Simple)

    fun byId(id: String): BoardTemplate? = (all + Inbox).firstOrNull { it.id == id }
}
