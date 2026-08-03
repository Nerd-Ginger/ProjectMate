package com.nerdginger.projectmate.data.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * A dynamic collection — a stored query rather than a container.
 *
 * Boards are static collections: an item lives in exactly one. Saved views cut
 * across them. **Today, Inbox and Overdue are built-in rows in this table**, so
 * there is one filter engine and no special cases.
 */
@Entity(
    tableName = "saved_views",
    indices = [Index(value = ["sortKey"])],
)
data class SavedViewEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String? = null,
    /** Serialized `ViewFilter` from `:core`. */
    val filterJson: String,
    val sortKey: String,
    val isPinned: Boolean = false,
    /** Shipped with the app; can be hidden but not deleted. */
    val isBuiltIn: Boolean = false,
    @Embedded val sync: SyncColumns,
)

/**
 * Pending local changes awaiting a push.
 *
 * Created empty in schema v1 and unused until sync ships. One `CREATE TABLE`
 * now avoids a migration later. Deliberately the one table with an
 * autoincrement key — it is local-only and never synced, so it has none of the
 * cross-device collision problems that make UUIDs necessary elsewhere.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [Index(value = ["entityType", "entityId"])],
)
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val opType: Int,
    val payloadJson: String? = null,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)
