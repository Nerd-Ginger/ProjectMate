package com.nerdginger.projectmate.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Upsert
import com.nerdginger.projectmate.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query(
        """
        SELECT * FROM items
        WHERE boardId = :boardId AND deletedAt IS NULL AND archivedAt IS NULL
        ORDER BY sortKey ASC
        """,
    )
    fun observeForBoard(boardId: String): Flow<List<ItemEntity>>

    /**
     * Every live item, across every board.
     *
     * The Today screen filters this in Kotlin using `TodayRules` from `:core`
     * rather than in SQL — those rules are subtle (all-day versus timed due
     * dates, terminal statuses) and worth having under unit test in the module
     * that can actually be tested locally. At personal-tracker scale the cost
     * of loading them is irrelevant.
     */
    @Query("SELECT * FROM items WHERE deletedAt IS NULL AND archivedAt IS NULL")
    fun observeAllLive(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): ItemEntity?

    /**
     * Everything sitting on the system Inbox board, awaiting triage.
     *
     * Joined rather than composed from `getSystemInbox()` then
     * `observeForBoard()`, so this stays a single observable query — the Inbox
     * badge in the bottom bar reads it continuously.
     */
    @Query(
        """
        SELECT i.* FROM items i
        JOIN boards b ON b.id = i.boardId
        WHERE b.isSystem = 1
          AND b.deletedAt IS NULL
          AND i.deletedAt IS NULL
          AND i.archivedAt IS NULL
        ORDER BY i.sortKey ASC
        """,
    )
    fun observeSystemInbox(): Flow<List<ItemEntity>>

    @Query(
        """
        SELECT * FROM items
        WHERE deletedAt IS NULL AND archivedAt IS NULL
          AND (title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY sortKey ASC
        LIMIT :limit
        """,
    )
    fun search(query: String, limit: Int = 200): Flow<List<ItemEntity>>

    @Query(
        """
        SELECT sortKey FROM items
        WHERE statusId = :statusId AND deletedAt IS NULL
        ORDER BY sortKey DESC LIMIT 1
        """,
    )
    suspend fun lastSortKeyInStatus(statusId: String): String?

    /** Everything already imported from the portal, for dedup. */
    @Query(
        """
        SELECT id, externalRequestId FROM items
        WHERE externalRequestId IS NOT NULL AND deletedAt IS NULL
        """,
    )
    suspend fun importedRequestIds(): List<ImportedRequestRow>

    @Query("SELECT COUNT(*) FROM items WHERE statusId = :statusId AND deletedAt IS NULL AND archivedAt IS NULL")
    fun observeCountInStatus(statusId: String): Flow<Int>

    @Insert
    suspend fun insert(item: ItemEntity)

    @Insert
    suspend fun insertAll(items: List<ItemEntity>)

    @Upsert
    suspend fun upsert(item: ItemEntity)

    @Update
    suspend fun update(item: ItemEntity)

    /**
     * Moves an item to a status — **including onto a different board.**
     *
     * `boardId` is written as well as `statusId`, and it must be. A status
     * belongs to exactly one board, so setting one without the other leaves the
     * row claiming a board whose columns it isn't in: it vanishes from the
     * destination kanban, stays in whatever list it came from, and renders a
     * foreign status name. Invisible for a kanban drag, where the board doesn't
     * change; immediate the first time Inbox triage moved something across.
     */
    @Query(
        """
        UPDATE items
        SET boardId = :boardId,
            statusId = :statusId,
            sortKey = :sortKey,
            completedAt = :completedAt,
            updatedAt = :now
        WHERE id = :id
        """,
    )
    suspend fun move(
        id: String,
        boardId: String,
        statusId: String,
        sortKey: String,
        completedAt: Long?,
        now: Long,
    )

    @Query("UPDATE items SET sortKey = :sortKey, updatedAt = :now WHERE id = :id")
    suspend fun reorder(id: String, sortKey: String, now: Long)

    @Query("UPDATE items SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE items SET archivedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: String, now: Long)
}

data class ImportedRequestRow(
    val id: String,
    val externalRequestId: String,
)
