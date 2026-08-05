package com.nerdginger.projectmate.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.data.entity.BoardEntity
import com.nerdginger.projectmate.data.entity.ItemEntity
import com.nerdginger.projectmate.data.entity.StatusEntity
import com.nerdginger.projectmate.data.entity.SyncColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Moving an item, including across boards.
 *
 * Exists because `ItemDao.move` originally set `statusId` and not `boardId`.
 * A kanban drag never noticed — the board doesn't change there — but Inbox
 * triage moves *between* boards, and the row ended up claiming a board whose
 * columns it wasn't in.
 */
class ItemMoveTest {

    private lateinit var db: ProjectMateDatabase

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<ProjectMateDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun `moving across boards takes the item's board with it`() = runTest {
        seedBoard("inbox", "untriaged", isSystem = true)
        seedBoard("life", "todo")
        db.itemDao().insert(item("i1", boardId = "inbox", statusId = "untriaged"))

        db.itemDao().move(
            id = "i1",
            boardId = "life",
            statusId = "todo",
            sortKey = "a1",
            completedAt = null,
            now = 10L,
        )

        val moved = db.itemDao().getById("i1")
        assertNotNull(moved)
        assertEquals("life", moved.boardId, "board must follow the status")
        assertEquals("todo", moved.statusId)
    }

    @Test
    fun `a triaged item is no longer in the inbox`() = runTest {
        seedBoard("inbox", "untriaged", isSystem = true)
        seedBoard("life", "todo")
        db.itemDao().insert(item("i1", boardId = "inbox", statusId = "untriaged"))

        db.itemDao().move("i1", "life", "todo", "a1", null, 10L)

        val stillWaiting = db.itemDao().observeSystemInbox().first()
        assertEquals(emptyList(), stillWaiting.map { it.id })
    }

    @Test
    fun `the inbox query only returns the system board`() = runTest {
        seedBoard("inbox", "untriaged", isSystem = true)
        seedBoard("life", "todo")
        db.itemDao().insert(item("waiting", boardId = "inbox", statusId = "untriaged"))
        db.itemDao().insert(item("filed", boardId = "life", statusId = "todo"))

        val waiting = db.itemDao().observeSystemInbox().first()

        assertEquals(listOf("waiting"), waiting.map { it.id })
    }

    @Test
    fun `archived items drop out of the inbox — dismissing is archiving`() = runTest {
        seedBoard("inbox", "untriaged", isSystem = true)
        db.itemDao().insert(item("i1", boardId = "inbox", statusId = "untriaged"))

        db.itemDao().archive("i1", now = 10L)

        assertEquals(emptyList(), db.itemDao().observeSystemInbox().first().map { it.id })
    }

    // ------------------------------------------------------------- fixtures

    private suspend fun seedBoard(boardId: String, statusId: String, isSystem: Boolean = false) {
        db.boardDao().insert(
            BoardEntity(
                id = boardId,
                name = boardId,
                boardType = 0,
                accentColor = 0,
                defaultViewMode = 0,
                defaultGroupBy = 0,
                sortKey = "a0",
                isSystem = isSystem,
                sync = SyncColumns.new(now = 0L),
            ),
        )
        db.statusDao().insertAll(
            listOf(
                StatusEntity(
                    id = statusId,
                    boardId = boardId,
                    name = statusId,
                    category = StatusCategory.BACKLOG.id,
                    colorArgb = 0,
                    sortKey = "a0",
                    isDefault = true,
                    sync = SyncColumns.new(now = 0L),
                ),
            ),
        )
    }

    private fun item(id: String, boardId: String, statusId: String) = ItemEntity(
        id = id,
        boardId = boardId,
        statusId = statusId,
        title = id,
        itemType = 0,
        priority = 0,
        sortKey = "a0",
        sync = SyncColumns.new(now = 0L),
    )
}
