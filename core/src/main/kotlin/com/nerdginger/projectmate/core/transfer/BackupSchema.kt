package com.nerdginger.projectmate.core.transfer

import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.ChecklistEntry
import com.nerdginger.projectmate.core.model.FeatureRequestMeta
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.ItemLink
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.model.Tag
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Full-database export.
 *
 * Reuses the envelope conventions of the feature-request format with
 * `kind = "projectmate.backup"`, and carries **all** sync fields — so a backup
 * restored onto a new device keeps its UUIDs and can still take part in sync
 * later. A backup that renumbered ids would quietly become a fork.
 *
 * Note this includes requester email addresses. Treat an exported backup as
 * sensitive; see docs/FEATURE_REQUEST_SCHEMA.md.
 */
@Serializable
data class BackupEnvelope(
    val schemaVersion: Int = SCHEMA_VERSION,
    val kind: String = KIND_BACKUP,
    val generatedAt: String,
    val appVersion: String? = null,
    val deviceId: String? = null,
    val boards: List<Board> = emptyList(),
    val statuses: List<Status> = emptyList(),
    val items: List<Item> = emptyList(),
    val checklistEntries: List<ChecklistEntry> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val itemTags: List<ItemTagPair> = emptyList(),
    val itemLinks: List<ItemLink> = emptyList(),
    val featureRequestMeta: List<FeatureRequestMeta> = emptyList(),
)

@Serializable
data class ItemTagPair(
    val itemId: String,
    val tagId: String,
    val createdAt: Long,
    val deletedAt: Long? = null,
)

/** Shared JSON configuration, so export and import can't drift apart. */
object BackupJson {

    val format: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    /** Compact form, for the verbatim copy stored against each imported request. */
    private val compact: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun encode(envelope: BackupEnvelope): String =
        format.encodeToString(BackupEnvelope.serializer(), envelope)

    fun decode(raw: String): BackupEnvelope =
        format.decodeFromString(BackupEnvelope.serializer(), raw)

    fun encodeRequest(dto: FeatureRequestDto): String =
        compact.encodeToString(FeatureRequestDto.serializer(), dto)

    /** `projectmate-backup-2026-08-03.json` */
    fun filenameFor(isoDate: String): String = "projectmate-backup-$isoDate.json"
}
