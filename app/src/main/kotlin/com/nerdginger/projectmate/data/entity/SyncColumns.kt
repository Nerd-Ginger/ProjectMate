package com.nerdginger.projectmate.data.entity

import com.nerdginger.projectmate.core.model.Origin
import com.nerdginger.projectmate.core.model.SyncMeta
import com.nerdginger.projectmate.core.model.SyncState

/**
 * The bookkeeping every synced row carries, declared once and `@Embedded`
 * everywhere rather than repeated seven times.
 *
 * Present in schema v1 despite phase 1 having no server, because adding it
 * later means backfilling identity for rows that exist on exactly one device
 * and nowhere else. See docs/DECISIONS.md D-003.
 */
data class SyncColumns(
    val createdAt: Long,
    val updatedAt: Long,
    /** Non-null means deleted. Every read filters these out. */
    val deletedAt: Long? = null,
    val remoteId: String? = null,
    val syncState: Int = SyncState.LOCAL_ONLY.id,
    val version: Long = 0,
    val origin: Int = Origin.LOCAL.id,
) {
    fun toDomain(): SyncMeta = SyncMeta(
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        remoteId = remoteId,
        syncState = SyncState.fromId(syncState),
        version = version,
        origin = Origin.fromId(origin),
    )

    companion object {
        fun from(meta: SyncMeta): SyncColumns = SyncColumns(
            createdAt = meta.createdAt,
            updatedAt = meta.updatedAt,
            deletedAt = meta.deletedAt,
            remoteId = meta.remoteId,
            syncState = meta.syncState.id,
            version = meta.version,
            origin = meta.origin.id,
        )

        /** A fresh record created on this device. */
        fun new(now: Long, origin: Origin = Origin.LOCAL): SyncColumns =
            SyncColumns(createdAt = now, updatedAt = now, origin = origin.id)
    }
}
