package com.nerdginger.projectmate.core.model

import kotlinx.serialization.Serializable

/**
 * Enums are persisted and serialized by their stable [id], never by ordinal or
 * name. Reordering a declaration or renaming a constant must not silently
 * reinterpret existing rows.
 */
interface IdentifiedEnum {
    val id: Int
}

private fun <T> Array<T>.byId(id: Int, fallback: T): T where T : IdentifiedEnum =
    firstOrNull { it.id == id } ?: fallback

/**
 * The semantic meaning of a status, independent of what the user named it.
 *
 * This is what makes freely editable per-board statuses safe: anything working
 * across boards — progress bars, "hide completed", the Today view — reads the
 * category and never the label. Rename "Shipped" to "Launched" and nothing
 * downstream notices. See docs/DECISIONS.md D-002.
 */
@Serializable
enum class StatusCategory(override val id: Int) : IdentifiedEnum {
    /** Captured but not yet triaged. */
    INBOX(0),

    /** Agreed, but not being worked on. */
    BACKLOG(1),

    /** Actively in progress. */
    ACTIVE(2),

    /** Wanted, but stalled on something external. */
    BLOCKED(3),

    /** Finished. */
    DONE(4),

    /** Abandoned. Deliberately distinct from [DONE] so completion rates stay honest. */
    CANCELLED(5),
    ;

    /** No further work is expected. */
    val isTerminal: Boolean get() = this == DONE || this == CANCELLED

    /** Counts toward "things still on my plate". */
    val isOpen: Boolean get() = !isTerminal

    /** Contributes to a board's completion percentage as *completed*. */
    val countsAsComplete: Boolean get() = this == DONE

    companion object {
        fun fromId(id: Int): StatusCategory = entries.toTypedArray().byId(id, BACKLOG)
    }
}

@Serializable
enum class Priority(override val id: Int) : IdentifiedEnum {
    NONE(0),
    LOW(1),
    NORMAL(2),
    URGENT(3),
    ;

    companion object {
        fun fromId(id: Int): Priority = entries.toTypedArray().byId(id, NONE)
    }
}

@Serializable
enum class ItemType(override val id: Int) : IdentifiedEnum {
    PROJECT(0),
    TASK(1),
    FEATURE_REQUEST(2),
    NOTE(3),
    ;

    companion object {
        fun fromId(id: Int): ItemType = entries.toTypedArray().byId(id, TASK)
    }
}

/** Where a record came from. Drives the Inbox and import dedup. */
@Serializable
enum class Origin(override val id: Int) : IdentifiedEnum {
    LOCAL(0),
    SHARE_INTENT(1),
    FILE_IMPORT(2),
    WEB_PORTAL(3),
    ;

    /** Anything not created by hand lands in the Inbox for triage. */
    val needsTriage: Boolean get() = this != LOCAL

    companion object {
        fun fromId(id: Int): Origin = entries.toTypedArray().byId(id, LOCAL)
    }
}

@Serializable
enum class SyncState(override val id: Int) : IdentifiedEnum {
    LOCAL_ONLY(0),
    PENDING_PUSH(1),
    SYNCED(2),
    CONFLICT(3),
    ;

    companion object {
        fun fromId(id: Int): SyncState = entries.toTypedArray().byId(id, LOCAL_ONLY)
    }
}

/**
 * A hint about what a board is for, used to pick sensible defaults. Never a
 * behaviour switch — a `CUSTOM` board can do everything a `PROJECTS` board can.
 */
@Serializable
enum class BoardType(override val id: Int) : IdentifiedEnum {
    PROJECTS(0),
    LIFE(1),
    FEATURE_REQUESTS(2),
    CUSTOM(3),
    ;

    companion object {
        fun fromId(id: Int): BoardType = entries.toTypedArray().byId(id, CUSTOM)
    }
}

@Serializable
enum class ViewMode(override val id: Int) : IdentifiedEnum {
    KANBAN(0),
    LIST(1),
    AGENDA(2),
    ;

    companion object {
        fun fromId(id: Int): ViewMode = entries.toTypedArray().byId(id, KANBAN)
    }
}

@Serializable
enum class GroupBy(override val id: Int) : IdentifiedEnum {
    STATUS(0),
    DUE(1),
    PRIORITY(2),
    TAG(3),
    ;

    companion object {
        fun fromId(id: Int): GroupBy = entries.toTypedArray().byId(id, STATUS)
    }
}

@Serializable
enum class LinkRelation(override val id: Int) : IdentifiedEnum {
    RELATES(0),
    BLOCKS(1),
    DUPLICATE_OF(2),
    IMPLEMENTS(3),
    ;

    companion object {
        fun fromId(id: Int): LinkRelation = entries.toTypedArray().byId(id, RELATES)
    }
}
