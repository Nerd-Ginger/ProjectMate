package com.nerdginger.projectmate.data

import com.nerdginger.projectmate.core.id.Uuid7
import com.nerdginger.projectmate.core.model.BoardType
import com.nerdginger.projectmate.core.sort.SortKey
import com.nerdginger.projectmate.core.template.BoardTemplate
import com.nerdginger.projectmate.core.template.BoardTemplates
import com.nerdginger.projectmate.data.dao.AppMetaDao
import com.nerdginger.projectmate.data.dao.BoardDao
import com.nerdginger.projectmate.data.dao.StatusDao
import com.nerdginger.projectmate.data.entity.AppMetaEntity
import com.nerdginger.projectmate.data.entity.BoardEntity
import com.nerdginger.projectmate.data.entity.StatusEntity
import com.nerdginger.projectmate.data.entity.SyncColumns

/**
 * Turns a [BoardTemplate] into rows, and seeds first-run state.
 *
 * Templates live in `:core` as code rather than as database rows, so they can
 * be unit-tested and so a template change never needs a migration. This is
 * where they become data.
 */
class DatabaseSeeder(
    private val boardDao: BoardDao,
    private val statusDao: StatusDao,
    private val appMetaDao: AppMetaDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = Uuid7::generate,
) {

    /**
     * Runs once, on an empty database: mints a device identity and creates the
     * system Inbox.
     *
     * Starter boards are offered in onboarding rather than forced here — a
     * first launch that silently invents boards you didn't ask for is a worse
     * introduction than an empty screen with a clear next step.
     */
    suspend fun seedIfEmpty() {
        val timestamp = now()

        appMetaDao.insertIfAbsent(
            AppMetaEntity(deviceId = newId(), createdAt = timestamp),
        )

        if (boardDao.getSystemInbox() == null) {
            createBoard(
                template = BoardTemplates.Inbox,
                isSystem = true,
                isPinned = true,
            )
        }
    }

    /** Creates a board and its statuses from a template. */
    suspend fun createBoard(
        template: BoardTemplate,
        name: String = template.name,
        isSystem: Boolean = false,
        isPinned: Boolean = false,
    ): String {
        val timestamp = now()
        val boardId = newId()
        val sync = SyncColumns.new(timestamp)

        boardDao.insert(
            BoardEntity(
                id = boardId,
                name = name,
                description = template.description,
                boardType = template.boardType.id,
                templateId = template.id,
                emoji = template.emoji,
                accentColor = template.accentColor,
                defaultViewMode = template.defaultViewMode.id,
                defaultGroupBy = template.defaultGroupBy.id,
                portalSlug = null,
                portalDefaultStatusId = null,
                sortKey = SortKey.between(boardDao.lastSortKey(), null),
                isPinned = isPinned,
                isSystem = isSystem,
                sync = sync,
            ),
        )

        // One sequence pass so column order matches the template exactly.
        val sortKeys = SortKey.sequence(null, null, template.statuses.size)
        val statuses = template.statuses.mapIndexed { index, status ->
            StatusEntity(
                id = newId(),
                boardId = boardId,
                name = status.name,
                category = status.category.id,
                colorArgb = status.colorArgb,
                sortKey = sortKeys[index],
                isDefault = status.isDefault,
                isFocus = status.isFocus,
                wipLimit = status.wipLimit,
                sync = sync,
            )
        }
        statusDao.insertAll(statuses)

        // A feature-request board is the natural landing place for imports, so
        // wire its slug and triage status up front rather than asking later.
        if (template.boardType == BoardType.FEATURE_REQUESTS) {
            val landing = statuses.firstOrNull { it.isDefault } ?: statuses.first()
            boardDao.getById(boardId)?.let { board ->
                boardDao.update(board.copy(portalDefaultStatusId = landing.id))
            }
        }

        return boardId
    }
}
