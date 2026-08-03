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

    @Query(
        """
        UPDATE items
        SET statusId = :statusId, sortKey = :sortKey, completedAt = :completedAt, updatedAt = :now
        WHERE id = :id
        """,
    )
    suspend fun move(id: String, statusId: String, sortKey: String, completedAt: Long?, now: Long)

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
