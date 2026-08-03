package com.nerdginger.projectmate.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.nerdginger.projectmate.data.entity.ChecklistEntryEntity
import com.nerdginger.projectmate.data.entity.FeatureRequestMetaEntity
import com.nerdginger.projectmate.data.entity.ItemLinkEntity
import com.nerdginger.projectmate.data.entity.ItemTagCrossRef
import com.nerdginger.projectmate.data.entity.SavedViewEntity
import com.nerdginger.projectmate.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    @Query(
        """
        SELECT * FROM checklist_entries
        WHERE itemId = :itemId AND deletedAt IS NULL
        ORDER BY sortKey ASC
        """,
    )
    fun observeForItem(itemId: String): Flow<List<ChecklistEntryEntity>>

    @Query(
        """
        SELECT sortKey FROM checklist_entries
        WHERE itemId = :itemId AND deletedAt IS NULL
        ORDER BY sortKey DESC LIMIT 1
        """,
    )
    suspend fun lastSortKey(itemId: String): String?

    @Upsert
    suspend fun upsert(entry: ChecklistEntryEntity)

    @Query("UPDATE checklist_entries SET isDone = :done, doneAt = :doneAt, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, doneAt: Long?, now: Long)

    @Query("UPDATE checklist_entries SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface TagDao {

    @Query("SELECT * FROM tags WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name AND deletedAt IS NULL")
    suspend fun getByName(name: String): TagEntity?

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN item_tags it ON it.tagId = t.id
        WHERE it.itemId = :itemId AND it.deletedAt IS NULL AND t.deletedAt IS NULL
        ORDER BY t.name ASC
        """,
    )
    fun observeForItem(itemId: String): Flow<List<TagEntity>>

    /** Every live item-to-tag pair, for filtering across boards. */
    @Query("SELECT * FROM item_tags WHERE deletedAt IS NULL")
    fun observeAllLinks(): Flow<List<ItemTagCrossRef>>

    @Query("SELECT COUNT(*) FROM item_tags WHERE tagId = :tagId AND deletedAt IS NULL")
    suspend fun usageCount(tagId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Upsert
    suspend fun link(crossRef: ItemTagCrossRef)

    @Query("UPDATE item_tags SET deletedAt = :now WHERE itemId = :itemId AND tagId = :tagId")
    suspend fun unlink(itemId: String, tagId: String, now: Long)

    @Query("UPDATE tags SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface ItemLinkDao {

    @Query(
        """
        SELECT * FROM item_links
        WHERE (fromItemId = :itemId OR toItemId = :itemId) AND deletedAt IS NULL
        """,
    )
    fun observeForItem(itemId: String): Flow<List<ItemLinkEntity>>

    @Upsert
    suspend fun upsert(link: ItemLinkEntity)

    @Query("UPDATE item_links SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface FeatureRequestMetaDao {

    @Query("SELECT * FROM feature_request_meta WHERE itemId = :itemId")
    fun observeForItem(itemId: String): Flow<FeatureRequestMetaEntity?>

    @Query("SELECT * FROM feature_request_meta WHERE itemId IN (:itemIds)")
    suspend fun getForItems(itemIds: List<String>): List<FeatureRequestMetaEntity>

    @Upsert
    suspend fun upsert(meta: FeatureRequestMetaEntity)

    @Upsert
    suspend fun upsertAll(meta: List<FeatureRequestMetaEntity>)

    /**
     * Refreshes portal-owned fields only.
     *
     * There is deliberately no title, notes or status here: the portal is a
     * source of requests, not the owner of your triage. See
     * docs/FEATURE_REQUEST_SCHEMA.md.
     */
    @Query(
        """
        UPDATE feature_request_meta
        SET votes = :votes,
            requesterName = :requesterName,
            requesterEmail = :requesterEmail,
            contactOptIn = :contactOptIn,
            importedUpdatedAt = :importedUpdatedAt,
            rawPayloadJson = :rawPayloadJson,
            updatedAt = :now
        WHERE itemId = :itemId
        """,
    )
    suspend fun refreshPortalFields(
        itemId: String,
        votes: Int,
        requesterName: String?,
        requesterEmail: String?,
        contactOptIn: Boolean,
        importedUpdatedAt: Long?,
        rawPayloadJson: String,
        now: Long,
    )
}

@Dao
interface SavedViewDao {

    @Query("SELECT * FROM saved_views WHERE deletedAt IS NULL ORDER BY sortKey ASC")
    fun observeAll(): Flow<List<SavedViewEntity>>

    @Query("SELECT * FROM saved_views WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): SavedViewEntity?

    @Query("SELECT COUNT(*) FROM saved_views WHERE isBuiltIn = 1")
    suspend fun builtInCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(views: List<SavedViewEntity>)

    @Upsert
    suspend fun upsert(view: SavedViewEntity)

    @Query("UPDATE saved_views SET deletedAt = :now, updatedAt = :now WHERE id = :id AND isBuiltIn = 0")
    suspend fun softDelete(id: String, now: Long)
}
