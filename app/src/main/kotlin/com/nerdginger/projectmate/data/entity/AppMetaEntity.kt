package com.nerdginger.projectmate.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Single-row table holding facts about this installation.
 *
 * [deviceId] is minted once on first run. It has no use yet — it exists because
 * sync needs to tell devices apart, and adding it later would mean inventing an
 * identity for an installation that already has history. See
 * docs/DECISIONS.md D-003.
 */
@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val deviceId: String,
    val createdAt: Long,
    val lastSyncAt: Long? = null,
    val syncCursor: String? = null,
) {
    companion object {
        /** There is only ever one row. */
        const val SINGLETON_ID: Int = 1
    }
}
