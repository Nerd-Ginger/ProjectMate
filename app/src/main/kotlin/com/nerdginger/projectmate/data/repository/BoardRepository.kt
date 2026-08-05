package com.nerdginger.projectmate.data.repository

import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.BoardProgress
import com.nerdginger.projectmate.core.model.BoardWithStatuses
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.template.BoardTemplate
import com.nerdginger.projectmate.data.DatabaseSeeder
import com.nerdginger.projectmate.data.dao.BoardDao
import com.nerdginger.projectmate.data.dao.StatusDao
import com.nerdginger.projectmate.data.mapper.toDomain
import com.nerdginger.projectmate.data.mapper.toProgressByBoard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** A board card on the home screen: the board plus its progress. */
data class BoardSummary(
    val board: Board,
    val progress: BoardProgress,
)

/**
 * The only thing UI code talks to for boards. Room types never travel upward.
 */
class BoardRepository(
    private val boardDao: BoardDao,
    private val statusDao: StatusDao,
    private val seeder: DatabaseSeeder,
) {

    /**
     * Board cards with their progress.
     *
     * Counts come from a grouped SQL query rather than by loading items, so
     * this stays cheap however many items a board accumulates.
     */
    fun observeBoardSummaries(): Flow<List<BoardSummary>> =
        combine(
            boardDao.observeActive(),
            boardDao.observeCategoryCounts(),
        ) { boards, counts ->
            val progressByBoard = counts.toProgressByBoard()
            boards.map { entity ->
                val board = entity.toDomain()
                BoardSummary(
                    board = board,
                    progress = progressByBoard[board.id] ?: BoardProgress(emptyMap()),
                )
            }
        }

    fun observeArchived(): Flow<List<Board>> =
        boardDao.observeArchived().map { list -> list.map { it.toDomain() } }

    /**
     * Every live board keyed by id.
     *
     * The cross-board screens — Today, Search, Inbox — show which board a row
     * came from, and they hold items rather than boards. This is the lookup
     * that turns an item's `boardId` into a name, an accent and a monogram.
     */
    fun observeBoardsById(): Flow<Map<String, Board>> =
        boardDao.observeActive().map { list ->
            list.associate { entity -> entity.id to entity.toDomain() }
        }

    /**
     * Every status on every board, keyed by id.
     *
     * [com.nerdginger.projectmate.core.focus.TodayRules] needs this: it reads a
     * status's category and focus flag to decide what is pressing, and it works
     * across boards, so a per-board query would not do.
     */
    fun observeStatusesById(): Flow<Map<String, Status>> =
        statusDao.observeAll().map { list ->
            list.associate { entity -> entity.id to entity.toDomain() }
        }

    /**
     * A board with its columns.
     *
     * Composed from two flows rather than a Room `@Relation` — see the note in
     * [BoardDao]. Both halves stay independently observable, so editing a
     * status updates this without re-reading the board.
     */
    fun observeBoardWithStatuses(boardId: String): Flow<BoardWithStatuses?> =
        combine(
            boardDao.observeById(boardId),
            statusDao.observeForBoard(boardId),
        ) { board, statuses ->
            board?.let {
                BoardWithStatuses(
                    board = it.toDomain(),
                    statuses = statuses.map { status -> status.toDomain() },
                )
            }
        }

    fun observeStatuses(boardId: String): Flow<List<Status>> =
        statusDao.observeForBoard(boardId).map { list -> list.map { it.toDomain() } }

    suspend fun createFromTemplate(template: BoardTemplate, name: String = template.name): String =
        seeder.createBoard(template = template, name = name)

    suspend fun archive(boardId: String, now: Long = System.currentTimeMillis()) =
        boardDao.archive(boardId, now)

    suspend fun unarchive(boardId: String, now: Long = System.currentTimeMillis()) =
        boardDao.unarchive(boardId, now)

    /** Soft delete. The system Inbox is protected by the query itself. */
    suspend fun delete(boardId: String, now: Long = System.currentTimeMillis()) =
        boardDao.softDelete(boardId, now)

    suspend fun setPinned(boardId: String, pinned: Boolean, now: Long = System.currentTimeMillis()) {
        val board = boardDao.getById(boardId) ?: return
        boardDao.update(
            board.copy(isPinned = pinned, sync = board.sync.copy(updatedAt = now)),
        )
    }

    suspend fun seedIfEmpty() = seeder.seedIfEmpty()
}
