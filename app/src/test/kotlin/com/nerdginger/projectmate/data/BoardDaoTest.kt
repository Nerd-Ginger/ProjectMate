package com.nerdginger.projectmate.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nerdginger.projectmate.data.dao.BoardDao
import com.nerdginger.projectmate.data.entity.BoardEntity
import com.nerdginger.projectmate.data.entity.SyncColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Exercises the claim in docs/DECISIONS.md D-011: that Room 3 with the bundled
 * SQLite driver lets DAO tests run as ordinary JVM unit tests, with no
 * emulator, no Robolectric, and no Android [android.content.Context].
 *
 * If this file compiles and passes on a plain `:app:test` run, the decision
 * holds. If it does not, D-011 needs correcting rather than defending.
 */
class BoardDaoTest {

    private lateinit var db: ProjectMateDatabase
    private lateinit var dao: BoardDao

    @BeforeTest
    fun setUp() {
        // The no-Context, in-memory builder is the whole point of D-011.
        db = Room.inMemoryDatabaseBuilder<ProjectMateDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dao = db.boardDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun `inserts and reads a board back`() = runTest {
        dao.insert(board(id = "b1", name = "Projects"))

        val found = dao.getById("b1")

        assertNotNull(found)
        assertEquals("Projects", found.name)
    }

    @Test
    fun `soft-deleted boards are invisible to reads`() = runTest {
        dao.insert(board(id = "b1", name = "Projects"))

        dao.softDelete("b1", now = 1_000L)

        assertNull(dao.getById("b1"), "a tombstoned board must not come back from getById")
        assertEquals(0, dao.count(), "count must exclude tombstones")
    }

    @Test
    fun `the system inbox cannot be soft-deleted`() = runTest {
        dao.insert(board(id = "inbox", name = "Inbox", isSystem = true))

        dao.softDelete("inbox", now = 1_000L)

        assertNotNull(dao.getSystemInbox(), "softDelete is guarded by isSystem = 0")
    }

    @Test
    fun `lastSortKey returns the highest key among live boards`() = runTest {
        dao.insert(board(id = "b1", name = "A", sortKey = "a0"))
        dao.insert(board(id = "b2", name = "B", sortKey = "a2"))
        dao.insert(board(id = "b3", name = "C", sortKey = "a1"))

        assertEquals("a2", dao.lastSortKey())
    }

    private fun board(
        id: String,
        name: String,
        sortKey: String = "a0",
        isSystem: Boolean = false,
    ) = BoardEntity(
        id = id,
        name = name,
        boardType = 0,
        accentColor = 0,
        defaultViewMode = 0,
        defaultGroupBy = 0,
        sortKey = sortKey,
        isSystem = isSystem,
        sync = SyncColumns.new(now = 0L),
    )
}
