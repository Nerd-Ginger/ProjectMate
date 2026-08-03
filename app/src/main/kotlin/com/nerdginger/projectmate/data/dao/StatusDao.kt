package com.nerdginger.projectmate.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Upsert
import com.nerdginger.projectmate.data.entity.StatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {

    @Query(
        """
        SELECT * FROM statuses
        WHERE boardId = :boardId AND deletedAt IS NULL
        ORDER BY sortKey ASC
        """,
    )
    fun observeForBoard(boardId: String): Flow<List<StatusEntity>>

    /** Every status on every board — the Today view needs categories globally. */
    @Query("SELECT * FROM statuses WHERE deletedAt IS NULL")
    fun observeAll(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE boardId = :boardId AND deletedAt IS NULL ORDER BY sortKey ASC")
    suspend fun getForBoard(boardId: String): List<StatusEntity>

    @Query("SELECT * FROM statuses WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): StatusEntity?

    @Query(
        """
        SELECT * FROM statuses
        WHERE boardId = :boardId AND deletedAt IS NULL
        ORDER BY isDefault DESC, sortKey ASC
        LIMIT 1
        """,
    )
    suspend fun getDefaultForBoard(boardId: String): StatusEntity?

    @Query("SELECT COUNT(*) FROM items WHERE statusId = :statusId AND deletedAt IS NULL AND archivedAt IS NULL")
    suspend fun countItems(statusId: String): Int

    @Insert
    suspend fun insertAll(statuses: List<StatusEntity>)

    @Upsert
    suspend fun upsert(status: StatusEntity)

    @Update
    suspend fun updateAll(statuses: List<StatusEntity>)

    /**
     * Moves every item off a status, which must happen before it can be
     * deleted — the foreign key is RESTRICT precisely so this can't be skipped.
     */
    @Query("UPDATE items SET statusId = :toStatusId, updatedAt = :now WHERE statusId = :fromStatusId")
    suspend fun reassignItems(fromStatusId: String, toStatusId: String, now: Long)

    @Query("UPDATE statuses SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE statuses SET isDefault = 0 WHERE boardId = :boardId")
    suspend fun clearDefault(boardId: String)
}
