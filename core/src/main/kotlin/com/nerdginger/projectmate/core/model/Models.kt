package com.nerdginger.projectmate.core.model

import kotlinx.serialization.Serializable

/**
 * Bookkeeping carried by every synced record.
 *
 * Present from schema v1 even though phase 1 has no server, because adding it
 * later means backfilling identity for rows that exist on exactly one device
 * and nowhere else. See docs/DECISIONS.md D-003.
 */
@Serializable
data class SyncMeta(
    val createdAt: Long,
    val updatedAt: Long,
    /** Non-null means deleted. Reads must filter these out; they are never hard-deleted. */
    val deletedAt: Long? = null,
    val remoteId: String? = null,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    /**
     * Server-incremented revision. Comparing [updatedAt] alone silently loses
     * an edit when two devices' clocks disagree; a revision makes the conflict
     * detectable.
     */
    val version: Long = 0,
    val origin: Origin = Origin.LOCAL,
) {
    val isDeleted: Boolean get() = deletedAt != null
}

/**
 * A collection — a board with its own set of statuses.
 *
 * The single concept behind projects, life tasks and feature requests. What
 * distinguishes a "Projects" board from a "Life" board is nothing but the
 * statuses it owns. See docs/PURPOSE.md.
 */
@Serializable
data class Board(
    val id: String,
    val name: String,
    val description: String? = null,
    val boardType: BoardType = BoardType.CUSTOM,
    val templateId: String? = null,
    val emoji: String? = null,
    val accentColor: Int,
    val defaultViewMode: ViewMode = ViewMode.KANBAN,
    val defaultGroupBy: GroupBy = GroupBy.STATUS,
    /** Routes an incoming feature request's `projectSlug` to this board. */
    val portalSlug: String? = null,
    val portalDefaultStatusId: String? = null,
    val sortKey: String,
    val isPinned: Boolean = false,
    /** The Inbox. Cannot be deleted. */
    val isSystem: Boolean = false,
    val archivedAt: Long? = null,
    val sync: SyncMeta,
) {
    val isArchived: Boolean get() = archivedAt != null
}

/** A column on a board. Freely renamable; its [category] is what code reads. */
@Serializable
data class Status(
    val id: String,
    val boardId: String,
    val name: String,
    val category: StatusCategory,
    val colorArgb: Int,
    val sortKey: String,
    /** Where newly created items land. */
    val isDefault: Boolean = false,
    /** Surfaces in Today regardless of due date — "what I'm actually on". */
    val isFocus: Boolean = false,
    /** A soft warning only; nothing is prevented. */
    val wipLimit: Int? = null,
    val sync: SyncMeta,
)

/** A project, a task, or a feature request — the distinction is [itemType]. */
@Serializable
data class Item(
    val id: String,
    val boardId: String,
    val statusId: String,
    val title: String,
    val notes: String? = null,
    val itemType: ItemType = ItemType.TASK,
    val priority: Priority = Priority.NONE,
    val dueAt: Long? = null,
    /**
     * Whether [dueAt] carries a meaningful time of day.
     *
     * Without this, "due Tuesday" is stored as Tuesday 00:00 and reads as
     * overdue for the whole of Tuesday.
     */
    val dueHasTime: Boolean = false,
    val startAt: Long? = null,
    val remindAt: Long? = null,
    val completedAt: Long? = null,
    val parentItemId: String? = null,
    /** Dedup key for imports. Unique across all items. */
    val externalRequestId: String? = null,
    val sortKey: String,
    val isPinned: Boolean = false,
    val archivedAt: Long? = null,
    val sync: SyncMeta,
) {
    val isArchived: Boolean get() = archivedAt != null
}

@Serializable
data class ChecklistEntry(
    val id: String,
    val itemId: String,
    val text: String,
    val isDone: Boolean = false,
    val doneAt: Long? = null,
    val sortKey: String,
    val sync: SyncMeta,
)

/** Global rather than per-board, so tags cut across collections. */
@Serializable
data class Tag(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val sync: SyncMeta,
)

/** Connects a feature request to the work item that implements it. */
@Serializable
data class ItemLink(
    val id: String,
    val fromItemId: String,
    val toItemId: String,
    val relation: LinkRelation = LinkRelation.RELATES,
    val sync: SyncMeta,
)

/**
 * Portal-specific fields for an imported feature request, kept out of the hot
 * [Item] table.
 */
@Serializable
data class FeatureRequestMeta(
    val itemId: String,
    val requesterName: String? = null,
    /** Personal data. See the privacy note in docs/FEATURE_REQUEST_SCHEMA.md. */
    val requesterEmail: String? = null,
    val contactOptIn: Boolean = false,
    val votes: Int = 0,
    val portalSlug: String? = null,
    val sourceUrl: String? = null,
    val submittedAt: Long,
    /** The original request, verbatim — you can't lose fields you haven't modelled. */
    val rawPayloadJson: String,
    val sync: SyncMeta,
)

/** A board plus its columns, in display order. */
data class BoardWithStatuses(
    val board: Board,
    val statuses: List<Status>,
) {
    val defaultStatus: Status?
        get() = statuses.firstOrNull { it.isDefault }
            ?: statuses.firstOrNull { it.category == StatusCategory.BACKLOG }
            ?: statuses.firstOrNull()
}

/** Counts per status category, for progress without loading every item. */
data class BoardProgress(
    val countsByCategory: Map<StatusCategory, Int>,
) {
    val total: Int get() = countsByCategory.values.sum()
    val completed: Int get() = countsByCategory[StatusCategory.DONE] ?: 0
    val open: Int get() = countsByCategory.filterKeys { it.isOpen }.values.sum()
    val blocked: Int get() = countsByCategory[StatusCategory.BLOCKED] ?: 0

    /**
     * Completion in `0f..1f`, excluding cancelled work.
     *
     * Cancelled items are dropped from the denominator rather than counted as
     * done — abandoning half a board should not read as 50% complete.
     */
    val fraction: Float
        get() {
            val cancelled = countsByCategory[StatusCategory.CANCELLED] ?: 0
            val considered = total - cancelled
            return if (considered <= 0) 0f else completed.toFloat() / considered
        }
}
