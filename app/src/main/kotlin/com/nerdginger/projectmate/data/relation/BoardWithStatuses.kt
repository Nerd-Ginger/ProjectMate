package com.nerdginger.projectmate.data.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import com.nerdginger.projectmate.data.entity.BoardEntity
import com.nerdginger.projectmate.data.entity.StatusEntity

/**
 * A board plus its columns, loaded in one round trip.
 *
 * The annotations are imported rather than written fully-qualified inline —
 * Room's processor resolves them by import, and a fully-qualified
 * `@androidx.room3.Relation` is silently not recognised, which makes the
 * property look unannotated and produces a confusing "cannot find setter"
 * error instead of anything pointing at the real cause.
 */
data class BoardWithStatuses(
    @Embedded val board: BoardEntity,
    @Relation(parentColumn = "id", entityColumn = "boardId")
    val statuses: List<StatusEntity>,
)
