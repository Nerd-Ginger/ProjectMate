package com.nerdginger.projectmate.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import com.nerdginger.projectmate.data.entity.BoardEntity
import com.nerdginger.projectmate.data.entity.StatusEntity
import kotlinx.coroutines.flow.Flow

/** Per-board counts of open items by status category, for progress bars. */
data class BoardCategoryCount(
    val boardId: String,
    val category: Int,
    val count: Int,
)

/**
 * Every read filters `deletedAt IS NULL`. Deletes are soft so they can
 * propagate to a server later — see docs/DECISIONS.md D-003.
 */
@Dao
interface BoardDao {

    @Query(
        """
        SELECT * FROM boards
        WHERE deletedAt IS NULL AND archivedAt IS NULL
        ORDER BY isPinned DESC, sortKey ASC
        """,
    )
    fun observeActive(): Flow<List<BoardEntity>>

    @Query(
        """
        SELECT * FROM boards
        WHERE deletedAt IS NULL AND archivedAt IS NOT NULL
        ORDER BY archivedAt DESC
        """,
    )
    fun observeArchived(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<BoardEntity?>

    @Query("SELECT * FROM boards WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): BoardEntity?

    @Query("SELECT * FROM boards WHERE portalSlug = :slug AND deletedAt IS NULL")
    suspend fun getByPortalSlug(slug: String): BoardEntity?

    @Query("SELECT * FROM boards WHERE isSystem = 1 AND deletedAt IS NULL LIMIT 1")
    suspend fun getSystemInbox(): BoardEntity?

    @Query("SELECT COUNT(*) FROM boards WHERE deletedAt IS NULL")
    suspend fun count(): Int

    /** Highest sort key among active boards, for appending a new one. */
    @Query("SELECT sortKey FROM boards WHERE deletedAt IS NULL ORDER BY sortKey DESC LIMIT 1")
    suspend fun lastSortKey(): String?

    /**
     * Open-item counts per board and status category.
     *
     * Grouped in SQL so the home screen can draw a progress bar without
     * loading a single item.
     */
    @Query(
        """
        SELECT b.id AS boardId, s.category AS category, COUNT(i.id) AS count
        FROM boards b
        LEFT JOIN statuses s ON s.boardId = b.id AND s.deletedAt IS NULL
        LEFT JOIN items i ON i.statusId = s.id AND i.deletedAt IS NULL AND i.archivedAt IS NULL
        WHERE b.deletedAt IS NULL
        GROUP BY b.id, s.category
        """,
    )
    fun observeCategoryCounts(): Flow<List<BoardCategoryCount>>

    @Insert
    suspend fun insert(board: BoardEntity)

    @Upsert
    suspend fun upsert(board: BoardEntity)

    @Update
    suspend fun update(board: BoardEntity)

    @Query("UPDATE boards SET deletedAt = :now, updatedAt = :now WHERE id = :id AND isSystem = 0")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE boards SET archivedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: String, now: Long)

    @Query("UPDATE boards SET archivedAt = NULL, updatedAt = :now WHERE id = :id")
    suspend fun unarchive(id: String, now: Long)

    @Transaction
    @Query("SELECT * FROM boards WHERE id = :id AND deletedAt IS NULL")
    fun observeWithStatuses(id: String): Flow<BoardWithStatuses?>
}

/** A board plus its columns, loaded in one round trip. */
data class BoardWithStatuses(
    @androidx.room3.Embedded val board: BoardEntity,
    @androidx.room3.Relation(parentColumn = "id", entityColumn = "boardId")
    val statuses: List<StatusEntity>,
)
