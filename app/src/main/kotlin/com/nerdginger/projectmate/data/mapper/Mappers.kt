package com.nerdginger.projectmate.data.mapper

import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.BoardProgress
import com.nerdginger.projectmate.core.model.BoardType
import com.nerdginger.projectmate.core.model.ChecklistEntry
import com.nerdginger.projectmate.core.model.FeatureRequestMeta
import com.nerdginger.projectmate.core.model.GroupBy
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.ItemLink
import com.nerdginger.projectmate.core.model.ItemType
import com.nerdginger.projectmate.core.model.LinkRelation
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.core.model.Tag
import com.nerdginger.projectmate.core.model.ViewMode
import com.nerdginger.projectmate.data.dao.BoardCategoryCount
import com.nerdginger.projectmate.data.entity.BoardEntity
import com.nerdginger.projectmate.data.entity.ChecklistEntryEntity
import com.nerdginger.projectmate.data.entity.FeatureRequestMetaEntity
import com.nerdginger.projectmate.data.entity.ItemEntity
import com.nerdginger.projectmate.data.entity.ItemLinkEntity
import com.nerdginger.projectmate.data.entity.StatusEntity
import com.nerdginger.projectmate.data.entity.SyncColumns
import com.nerdginger.projectmate.data.entity.TagEntity

/**
 * Entity ↔ domain conversion.
 *
 * The one place Room types are allowed to meet `:core` types. Enum columns are
 * mapped explicitly by their stable id, never by ordinal — see
 * docs/DATA_MODEL.md.
 */

fun BoardEntity.toDomain(): Board = Board(
    id = id,
    name = name,
    description = description,
    boardType = BoardType.fromId(boardType),
    templateId = templateId,
    emoji = emoji,
    accentColor = accentColor,
    defaultViewMode = ViewMode.fromId(defaultViewMode),
    defaultGroupBy = GroupBy.fromId(defaultGroupBy),
    portalSlug = portalSlug,
    portalDefaultStatusId = portalDefaultStatusId,
    sortKey = sortKey,
    isPinned = isPinned,
    isSystem = isSystem,
    archivedAt = archivedAt,
    sync = sync.toDomain(),
)

fun Board.toEntity(): BoardEntity = BoardEntity(
    id = id,
    name = name,
    description = description,
    boardType = boardType.id,
    templateId = templateId,
    emoji = emoji,
    accentColor = accentColor,
    defaultViewMode = defaultViewMode.id,
    defaultGroupBy = defaultGroupBy.id,
    portalSlug = portalSlug,
    portalDefaultStatusId = portalDefaultStatusId,
    sortKey = sortKey,
    isPinned = isPinned,
    isSystem = isSystem,
    archivedAt = archivedAt,
    sync = SyncColumns.from(sync),
)

fun StatusEntity.toDomain(): Status = Status(
    id = id,
    boardId = boardId,
    name = name,
    category = StatusCategory.fromId(category),
    colorArgb = colorArgb,
    sortKey = sortKey,
    isDefault = isDefault,
    isFocus = isFocus,
    wipLimit = wipLimit,
    sync = sync.toDomain(),
)

fun Status.toEntity(): StatusEntity = StatusEntity(
    id = id,
    boardId = boardId,
    name = name,
    category = category.id,
    colorArgb = colorArgb,
    sortKey = sortKey,
    isDefault = isDefault,
    isFocus = isFocus,
    wipLimit = wipLimit,
    sync = SyncColumns.from(sync),
)

fun ItemEntity.toDomain(): Item = Item(
    id = id,
    boardId = boardId,
    statusId = statusId,
    title = title,
    notes = notes,
    itemType = ItemType.fromId(itemType),
    priority = Priority.fromId(priority),
    dueAt = dueAt,
    dueHasTime = dueHasTime,
    startAt = startAt,
    remindAt = remindAt,
    completedAt = completedAt,
    parentItemId = parentItemId,
    externalRequestId = externalRequestId,
    sortKey = sortKey,
    isPinned = isPinned,
    archivedAt = archivedAt,
    sync = sync.toDomain(),
)

fun Item.toEntity(): ItemEntity = ItemEntity(
    id = id,
    boardId = boardId,
    statusId = statusId,
    title = title,
    notes = notes,
    itemType = itemType.id,
    priority = priority.id,
    dueAt = dueAt,
    dueHasTime = dueHasTime,
    startAt = startAt,
    remindAt = remindAt,
    completedAt = completedAt,
    parentItemId = parentItemId,
    externalRequestId = externalRequestId,
    sortKey = sortKey,
    isPinned = isPinned,
    archivedAt = archivedAt,
    sync = SyncColumns.from(sync),
)

fun ChecklistEntryEntity.toDomain(): ChecklistEntry = ChecklistEntry(
    id = id,
    itemId = itemId,
    text = text,
    isDone = isDone,
    doneAt = doneAt,
    sortKey = sortKey,
    sync = sync.toDomain(),
)

fun ChecklistEntry.toEntity(): ChecklistEntryEntity = ChecklistEntryEntity(
    id = id,
    itemId = itemId,
    text = text,
    isDone = isDone,
    doneAt = doneAt,
    sortKey = sortKey,
    sync = SyncColumns.from(sync),
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    colorArgb = colorArgb,
    sync = sync.toDomain(),
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    colorArgb = colorArgb,
    sync = SyncColumns.from(sync),
)

fun ItemLinkEntity.toDomain(): ItemLink = ItemLink(
    id = id,
    fromItemId = fromItemId,
    toItemId = toItemId,
    relation = LinkRelation.fromId(relation),
    sync = sync.toDomain(),
)

fun ItemLink.toEntity(): ItemLinkEntity = ItemLinkEntity(
    id = id,
    fromItemId = fromItemId,
    toItemId = toItemId,
    relation = relation.id,
    sync = SyncColumns.from(sync),
)

fun FeatureRequestMetaEntity.toDomain(): FeatureRequestMeta = FeatureRequestMeta(
    itemId = itemId,
    requesterName = requesterName,
    requesterEmail = requesterEmail,
    contactOptIn = contactOptIn,
    votes = votes,
    portalSlug = portalSlug,
    sourceUrl = sourceUrl,
    submittedAt = submittedAt,
    rawPayloadJson = rawPayloadJson,
    sync = sync.toDomain(),
)

/** Groups the flat SQL count rows into per-board progress. */
fun List<BoardCategoryCount>.toProgressByBoard(): Map<String, BoardProgress> =
    groupBy { it.boardId }
        .mapValues { (_, rows) ->
            BoardProgress(
                rows.filter { it.count > 0 }
                    .associate { StatusCategory.fromId(it.category) to it.count },
            )
        }
