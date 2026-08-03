package com.nerdginger.projectmate.data.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "checklist_entries",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["itemId", "sortKey"])],
)
data class ChecklistEntryEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val text: String,
    val isDone: Boolean = false,
    val doneAt: Long? = null,
    val sortKey: String,
    @Embedded val sync: SyncColumns,
)

/** Global rather than per-board, so tags are the axis that cuts across boards. */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey val id: String,
    /** Stored lowercase; matching is case-insensitive. */
    val name: String,
    val colorArgb: Int,
    @Embedded val sync: SyncColumns,
)

/**
 * Item-to-tag join.
 *
 * Carries a tombstone like every other table. Without one, removing a tag on
 * one device gets undone by another device's stale copy on the next sync.
 */
@Entity(
    tableName = "item_tags",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tagId"])],
)
data class ItemTagCrossRef(
    val itemId: String,
    val tagId: String,
    val createdAt: Long,
    val deletedAt: Long? = null,
    val syncState: Int = 0,
)

/**
 * A typed relation between two items.
 *
 * How a feature request connects to the work item that implements it.
 */
@Entity(
    tableName = "item_links",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["toItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["fromItemId"]),
        Index(value = ["toItemId"]),
        Index(value = ["fromItemId", "toItemId", "relation"], unique = true),
    ],
)
data class ItemLinkEntity(
    @PrimaryKey val id: String,
    val fromItemId: String,
    val toItemId: String,
    val relation: Int,
    @Embedded val sync: SyncColumns,
)

/**
 * Portal-specific fields for an imported feature request.
 *
 * Kept out of the hot `items` table, which also keeps requester email
 * addresses out of every query that lists cards.
 */
@Entity(
    tableName = "feature_request_meta",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FeatureRequestMetaEntity(
    @PrimaryKey val itemId: String,
    val requesterName: String? = null,
    /** Personal data. See docs/FEATURE_REQUEST_SCHEMA.md. */
    val requesterEmail: String? = null,
    val contactOptIn: Boolean = false,
    val votes: Int = 0,
    val portalSlug: String? = null,
    val sourceUrl: String? = null,
    val submittedAt: Long,
    /** `updatedAt` of the request as last imported — the dedup comparison key. */
    val importedUpdatedAt: Long? = null,
    /** The original request verbatim, so unmodelled fields aren't lost. */
    val rawPayloadJson: String,
    @Embedded val sync: SyncColumns,
)
