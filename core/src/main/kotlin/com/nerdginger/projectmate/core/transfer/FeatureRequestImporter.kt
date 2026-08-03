package com.nerdginger.projectmate.core.transfer

import com.nerdginger.projectmate.core.id.Uuid7
import com.nerdginger.projectmate.core.model.FeatureRequestMeta
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.ItemType
import com.nerdginger.projectmate.core.model.Origin
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.model.SyncMeta
import com.nerdginger.projectmate.core.sort.SortKey

/** An item already in the database that came from the portal. */
data class ExistingRequest(
    val itemId: String,
    val externalRequestId: String,
    /** `updatedAt` of the *request* as last imported, not of the local item. */
    val importedUpdatedAt: Long?,
)

/** A request ready to be written, with its ids and sort key already minted. */
data class PreparedRequest(
    val dto: FeatureRequestDto,
    val item: Item,
    val meta: FeatureRequestMeta,
    val tagNames: List<String>,
)

/**
 * Refreshed portal-owned fields for a request already in the database.
 *
 * Deliberately does **not** carry title, notes or status: the portal is a
 * source of new requests, not the owner of your triage. Re-importing must never
 * undo a retitle or a status change you made locally.
 */
data class RequestMetadataUpdate(
    val itemId: String,
    val dto: FeatureRequestDto,
    val votes: Int,
    val requesterName: String?,
    val requesterEmail: String?,
    val contactOptIn: Boolean,
    val submittedAt: Long,
    val rawPayloadJson: String,
    val tagNames: List<String>,
)

/**
 * What an import would do, computed before anything is written.
 *
 * The import preview renders this. Nothing imports silently.
 */
data class ImportPlan(
    val toCreate: List<PreparedRequest>,
    val toUpdate: List<RequestMetadataUpdate>,
    /** Already present and not newer — nothing to do. */
    val unchanged: List<FeatureRequestDto>,
    val rejected: List<Rejection>,
) {
    val isEmpty: Boolean get() = toCreate.isEmpty() && toUpdate.isEmpty()
    val totalConsidered: Int
        get() = toCreate.size + toUpdate.size + unchanged.size + rejected.size
}

/**
 * Turns parsed requests into a plan against what's already stored.
 *
 * Dedup is on [FeatureRequestDto.id] → `items.externalRequestId`, which carries
 * a unique index. Importing the same payload twice is a no-op; importing a
 * payload where a request has a newer `updatedAt` refreshes its metadata only.
 */
object FeatureRequestImporter {

    fun plan(
        payload: ParsedPayload,
        existing: List<ExistingRequest>,
        boardId: String,
        statusId: String,
        /** Highest existing sort key on the target status, or null if empty. */
        lastSortKey: String? = null,
        now: Long = System.currentTimeMillis(),
        newId: () -> String = Uuid7::generate,
    ): ImportPlan {
        val existingByExternalId = existing.associateBy { it.externalRequestId }

        val toCreate = mutableListOf<PreparedRequest>()
        val toUpdate = mutableListOf<RequestMetadataUpdate>()
        val unchanged = mutableListOf<FeatureRequestDto>()

        // Keys are minted in sequence so imported requests keep the order the
        // export listed them in.
        var previousKey = lastSortKey

        payload.valid.forEach { dto ->
            val match = existingByExternalId[dto.id]
            val submittedAt = FeatureRequestParser.parseTimestamp(dto.submittedAt) ?: now
            val updatedAt = FeatureRequestParser.parseTimestamp(dto.updatedAt)

            if (match == null) {
                val sortKey = SortKey.between(previousKey, null)
                previousKey = sortKey
                toCreate += prepare(dto, boardId, statusId, sortKey, submittedAt, now, newId())
                return@forEach
            }

            val isNewer = updatedAt != null &&
                (match.importedUpdatedAt == null || updatedAt > match.importedUpdatedAt)

            if (isNewer) {
                toUpdate += RequestMetadataUpdate(
                    itemId = match.itemId,
                    dto = dto,
                    votes = dto.votes,
                    requesterName = dto.requester?.name,
                    requesterEmail = dto.requester?.email,
                    contactOptIn = dto.requester?.contactOptIn ?: false,
                    submittedAt = submittedAt,
                    rawPayloadJson = BackupJson.encodeRequest(dto),
                    tagNames = dto.normalisedTags(),
                )
            } else {
                unchanged += dto
            }
        }

        return ImportPlan(toCreate, toUpdate, unchanged, payload.rejected)
    }

    private fun prepare(
        dto: FeatureRequestDto,
        boardId: String,
        statusId: String,
        sortKey: String,
        submittedAt: Long,
        now: Long,
        itemId: String,
    ): PreparedRequest {
        val sync = SyncMeta(
            createdAt = submittedAt,
            updatedAt = now,
            origin = Origin.WEB_PORTAL,
        )

        val item = Item(
            id = itemId,
            boardId = boardId,
            statusId = statusId,
            title = dto.title.trim(),
            notes = dto.body?.takeIf { it.isNotBlank() },
            itemType = ItemType.FEATURE_REQUEST,
            priority = dto.priorityHint.toPriority(),
            externalRequestId = dto.id,
            sortKey = sortKey,
            sync = sync,
        )

        val meta = FeatureRequestMeta(
            itemId = itemId,
            requesterName = dto.requester?.name,
            requesterEmail = dto.requester?.email,
            contactOptIn = dto.requester?.contactOptIn ?: false,
            votes = dto.votes,
            portalSlug = dto.projectSlug,
            sourceUrl = dto.url,
            submittedAt = submittedAt,
            // Stored verbatim so fields this version doesn't model aren't lost.
            rawPayloadJson = BackupJson.encodeRequest(dto),
            sync = sync,
        )

        return PreparedRequest(dto, item, meta, dto.normalisedTags())
    }

    private fun PriorityHint.toPriority(): Priority = when (this) {
        PriorityHint.LOW -> Priority.LOW
        PriorityHint.NORMAL -> Priority.NORMAL
        PriorityHint.HIGH -> Priority.URGENT
    }

    /** Trimmed, lowercased, de-duplicated — tags are matched case-insensitively. */
    private fun FeatureRequestDto.normalisedTags(): List<String> =
        tags.map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
}
