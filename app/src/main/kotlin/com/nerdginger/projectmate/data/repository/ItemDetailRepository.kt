package com.nerdginger.projectmate.data.repository

import com.nerdginger.projectmate.core.id.Uuid7
import com.nerdginger.projectmate.core.model.ChecklistEntry
import com.nerdginger.projectmate.core.model.FeatureRequestMeta
import com.nerdginger.projectmate.core.model.ItemLink
import com.nerdginger.projectmate.core.model.Tag
import com.nerdginger.projectmate.core.sort.SortKey
import com.nerdginger.projectmate.core.template.Palette
import com.nerdginger.projectmate.data.dao.ChecklistDao
import com.nerdginger.projectmate.data.dao.FeatureRequestMetaDao
import com.nerdginger.projectmate.data.dao.ItemLinkDao
import com.nerdginger.projectmate.data.dao.TagDao
import com.nerdginger.projectmate.data.entity.ItemTagCrossRef
import com.nerdginger.projectmate.data.entity.SyncColumns
import com.nerdginger.projectmate.data.mapper.toDomain
import com.nerdginger.projectmate.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue

/**
 * Everything that hangs off an item — its checklist, tags, links and, for
 * imported requests, its portal metadata.
 *
 * One repository rather than four thin ones. These only ever appear together,
 * on the item detail screen, and four objects wired through DI to serve one
 * screen is ceremony without a payoff.
 */
class ItemDetailRepository(
    private val checklistDao: ChecklistDao,
    private val tagDao: TagDao,
    private val itemLinkDao: ItemLinkDao,
    private val featureRequestMetaDao: FeatureRequestMetaDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = Uuid7::generate,
) {

    // ------------------------------------------------------------- checklist

    fun observeChecklist(itemId: String): Flow<List<ChecklistEntry>> =
        checklistDao.observeForItem(itemId).map { rows -> rows.map { it.toDomain() } }

    /** Appends to the end of the list. */
    suspend fun addChecklistEntry(itemId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val timestamp = now()
        checklistDao.upsert(
            ChecklistEntry(
                id = newId(),
                itemId = itemId,
                text = trimmed,
                sortKey = SortKey.append(checklistDao.lastSortKey(itemId)),
                sync = SyncColumns.new(timestamp).toDomain(),
            ).toEntity(),
        )
    }

    /** `doneAt` is set on completion and cleared on un-completion. */
    suspend fun setChecklistDone(entryId: String, done: Boolean) {
        val timestamp = now()
        checklistDao.setDone(
            id = entryId,
            done = done,
            doneAt = if (done) timestamp else null,
            now = timestamp,
        )
    }

    suspend fun renameChecklistEntry(entry: ChecklistEntry, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed == entry.text) return
        checklistDao.upsert(
            entry.copy(text = trimmed, sync = entry.sync.copy(updatedAt = now())).toEntity(),
        )
    }

    suspend fun deleteChecklistEntry(entryId: String) {
        checklistDao.softDelete(entryId, now())
    }

    // ------------------------------------------------------------------ tags

    fun observeTags(itemId: String): Flow<List<Tag>> =
        tagDao.observeForItem(itemId).map { rows -> rows.map { it.toDomain() } }

    fun observeAllTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /**
     * Attaches a tag by name, creating it if it's new.
     *
     * Names are matched and stored lowercase so "API" and "api" are the same
     * tag — the alternative is a tag list that slowly fills with near-duplicates.
     */
    suspend fun addTag(itemId: String, name: String) {
        val normalised = name.trim().lowercase()
        if (normalised.isEmpty()) return

        val existing = tagDao.getByName(normalised)
        val tagId = existing?.id ?: newId().also { id ->
            tagDao.insert(
                Tag(
                    id = id,
                    name = normalised,
                    colorArgb = colorFor(normalised),
                    sync = SyncColumns.new(now()).toDomain(),
                ).toEntity(),
            )
        }

        tagDao.link(ItemTagCrossRef(itemId = itemId, tagId = tagId, createdAt = now()))
    }

    suspend fun removeTag(itemId: String, tagId: String) {
        tagDao.unlink(itemId, tagId, now())
    }

    /**
     * A stable colour for a new tag, chosen from the shared palette by hashing
     * the name.
     *
     * Deterministic rather than random so the same tag is the same colour on
     * every device once sync exists, and so a test can assert it.
     */
    private fun colorFor(name: String): Int =
        Palette.statusColors[name.hashCode().absoluteValue % Palette.statusColors.size]

    // ----------------------------------------------------------------- links

    fun observeLinks(itemId: String): Flow<List<ItemLink>> =
        itemLinkDao.observeForItem(itemId).map { rows -> rows.map { it.toDomain() } }

    suspend fun removeLink(linkId: String) {
        itemLinkDao.softDelete(linkId, now())
    }

    // -------------------------------------------------------- feature request

    fun observeFeatureRequest(itemId: String): Flow<FeatureRequestMeta?> =
        featureRequestMetaDao.observeForItem(itemId).map { it?.toDomain() }
}
