package com.nerdginger.projectmate.data.repository

import com.nerdginger.projectmate.core.id.Uuid7
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.ItemType
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.model.SyncMeta
import com.nerdginger.projectmate.core.sort.SortKey
import com.nerdginger.projectmate.data.dao.ItemDao
import com.nerdginger.projectmate.data.dao.StatusDao
import com.nerdginger.projectmate.data.mapper.toDomain
import com.nerdginger.projectmate.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ItemRepository(
    private val itemDao: ItemDao,
    private val statusDao: StatusDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = Uuid7::generate,
) {

    fun observeForBoard(boardId: String): Flow<List<Item>> =
        itemDao.observeForBoard(boardId).map { list -> list.map { it.toDomain() } }

    /** Every live item, across every board — the Today screen's input. */
    fun observeAllLive(): Flow<List<Item>> =
        itemDao.observeAllLive().map { list -> list.map { it.toDomain() } }

    fun observeById(itemId: String): Flow<Item?> =
        itemDao.observeById(itemId).map { it?.toDomain() }

    fun search(query: String): Flow<List<Item>> =
        itemDao.search(query).map { list -> list.map { it.toDomain() } }

    /** Creates an item at the end of a status column. */
    suspend fun create(
        boardId: String,
        statusId: String,
        title: String,
        itemType: ItemType = ItemType.TASK,
        priority: Priority = Priority.NONE,
    ): String {
        val timestamp = now()
        val id = newId()
        val item = Item(
            id = id,
            boardId = boardId,
            statusId = statusId,
            title = title.trim(),
            itemType = itemType,
            priority = priority,
            sortKey = SortKey.between(itemDao.lastSortKeyInStatus(statusId), null),
            sync = SyncMeta(createdAt = timestamp, updatedAt = timestamp),
        )
        itemDao.insert(item.toEntity())
        return id
    }

    /**
     * Moves an item to a status, appending it to that column.
     *
     * `completedAt` is derived from the destination's category rather than
     * asked for: moving a card into any status the user has marked Done is
     * what "completing" means here, whatever they named the column.
     */
    suspend fun moveTo(item: Item, destination: Status) {
        val timestamp = now()
        val completedAt = when {
            destination.category.countsAsComplete -> item.completedAt ?: timestamp
            else -> null
        }
        itemDao.move(
            id = item.id,
            statusId = destination.id,
            sortKey = SortKey.between(itemDao.lastSortKeyInStatus(destination.id), null),
            completedAt = completedAt,
            now = timestamp,
        )
    }

    /** Reorders within a column, given the new neighbours. */
    suspend fun reorder(itemId: String, before: String?, after: String?) {
        itemDao.reorder(itemId, SortKey.between(before, after), now())
    }

    suspend fun update(item: Item) {
        itemDao.update(item.copy(sync = item.sync.copy(updatedAt = now())).toEntity())
    }

    suspend fun archive(itemId: String) = itemDao.archive(itemId, now())

    suspend fun delete(itemId: String) = itemDao.softDelete(itemId, now())

    suspend fun defaultStatusFor(boardId: String): Status? =
        statusDao.getDefaultForBoard(boardId)?.toDomain()
}
