package com.nerdginger.projectmate.core

import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.core.model.SyncMeta

/** Minimal builders so tests state only what they care about. */
object Fixtures {

    fun sync(createdAt: Long = 0L, deletedAt: Long? = null) =
        SyncMeta(createdAt = createdAt, updatedAt = createdAt, deletedAt = deletedAt)

    fun status(
        id: String,
        category: StatusCategory,
        isFocus: Boolean = false,
        boardId: String = "board",
    ) = Status(
        id = id,
        boardId = boardId,
        name = id,
        category = category,
        colorArgb = 0,
        sortKey = "a0",
        isFocus = isFocus,
        sync = sync(),
    )

    fun item(
        id: String,
        statusId: String,
        title: String = id,
        dueAt: Long? = null,
        dueHasTime: Boolean = false,
        priority: Priority = Priority.NONE,
        archivedAt: Long? = null,
        deletedAt: Long? = null,
        boardId: String = "board",
    ) = Item(
        id = id,
        boardId = boardId,
        statusId = statusId,
        title = title,
        dueAt = dueAt,
        dueHasTime = dueHasTime,
        priority = priority,
        sortKey = "a0",
        archivedAt = archivedAt,
        sync = sync(deletedAt = deletedAt),
    )
}
