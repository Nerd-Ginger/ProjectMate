package com.nerdginger.projectmate.data.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * A collection — a board with its own set of statuses.
 *
 * See docs/DATA_MODEL.md. Enum-valued columns store the stable `id` of the
 * corresponding `:core` enum, never an ordinal.
 */
@Entity(
    tableName = "boards",
    indices = [
        Index(value = ["portalSlug"], unique = true),
        Index(value = ["sortKey"]),
        Index(value = ["archivedAt"]),
    ],
)
data class BoardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val boardType: Int,
    val templateId: String? = null,
    val emoji: String? = null,
    val accentColor: Int,
    val defaultViewMode: Int,
    val defaultGroupBy: Int,
    /** Routes an incoming feature request's `projectSlug` here. */
    val portalSlug: String? = null,
    val portalDefaultStatusId: String? = null,
    val sortKey: String,
    val isPinned: Boolean = false,
    /** The Inbox. Cannot be deleted. */
    val isSystem: Boolean = false,
    val archivedAt: Long? = null,
    @Embedded val sync: SyncColumns,
)

/**
 * A column on a board.
 *
 * `category` is what code reads; `name` is the user's to change freely. See
 * docs/DECISIONS.md D-002.
 */
@Entity(
    tableName = "statuses",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["boardId", "sortKey"]),
        Index(value = ["boardId", "category"]),
    ],
)
data class StatusEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val name: String,
    val category: Int,
    val colorArgb: Int,
    val sortKey: String,
    /** Where newly created items land. */
    val isDefault: Boolean = false,
    /** Surfaces in Today regardless of due date. */
    val isFocus: Boolean = false,
    /** A soft warning only; nothing is prevented. */
    val wipLimit: Int? = null,
    @Embedded val sync: SyncColumns,
)

/** A project, a task, or a feature request — the distinction is `itemType`. */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StatusEntity::class,
            parentColumns = ["id"],
            childColumns = ["statusId"],
            // RESTRICT on purpose: deleting a status must force its items to be
            // reassigned rather than silently orphaning or destroying them.
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["boardId", "statusId", "sortKey"]),
        Index(value = ["statusId"]),
        Index(value = ["dueAt"]),
        Index(value = ["archivedAt"]),
        Index(value = ["parentItemId"]),
        Index(value = ["externalRequestId"], unique = true),
    ],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val statusId: String,
    val title: String,
    val notes: String? = null,
    val itemType: Int,
    val priority: Int,
    val dueAt: Long? = null,
    /**
     * Whether [dueAt] carries a meaningful time of day. Without it, "due
     * Tuesday" reads as overdue from one minute past midnight on Tuesday.
     */
    val dueHasTime: Boolean = false,
    val startAt: Long? = null,
    val remindAt: Long? = null,
    val completedAt: Long? = null,
    val parentItemId: String? = null,
    /** Import dedup key. Unique across all items. */
    val externalRequestId: String? = null,
    val sortKey: String,
    val isPinned: Boolean = false,
    val archivedAt: Long? = null,
    @Embedded val sync: SyncColumns,
)
