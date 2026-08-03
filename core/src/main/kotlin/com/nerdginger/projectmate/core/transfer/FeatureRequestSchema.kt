package com.nerdginger.projectmate.core.transfer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire format shared by the website and the app.
 *
 * This is the Kotlin side of the contract documented in
 * `docs/FEATURE_REQUEST_SCHEMA.md`. The phase-2 Cloudflare Worker will serve
 * exactly this shape from `GET /api/requests`, which is why file import and API
 * sync can share one parser — see docs/DECISIONS.md D-008.
 *
 * **Changing anything here changes a published contract.** Additive fields are
 * safe (parsing ignores unknown keys). Removing or retyping a field requires a
 * [SCHEMA_VERSION] bump and a matching update to the document.
 */
const val SCHEMA_VERSION: Int = 1

const val KIND_FEATURE_REQUESTS: String = "projectmate.featureRequests"
const val KIND_BACKUP: String = "projectmate.backup"

@Serializable
data class PortalEnvelope(
    val schemaVersion: Int,
    val kind: String,
    val generatedAt: String? = null,
    val source: PortalSource? = null,
    val cursor: PortalCursor? = null,
    val requests: List<FeatureRequestDto> = emptyList(),
)

@Serializable
data class PortalSource(
    val type: String? = null,
    val siteId: String? = null,
    val exportId: String? = null,
)

@Serializable
data class PortalCursor(
    val since: String? = null,
    /** Opaque pagination token. The app stores it and sends it back. */
    val next: String? = null,
)

@Serializable
data class FeatureRequestDto(
    /** Opaque, stable, unique. Becomes `items.externalRequestId`. */
    val id: String,
    val projectSlug: String,
    val title: String,
    val body: String? = null,
    val type: RequestType = RequestType.FEATURE,
    val priorityHint: PriorityHint = PriorityHint.NORMAL,
    val submittedAt: String,
    val updatedAt: String? = null,
    /** Portal-side status. Advisory only — the app owns its own triage. */
    val status: String? = null,
    val votes: Int = 0,
    val tags: List<String> = emptyList(),
    val requester: RequesterDto? = null,
    val url: String? = null,
)

@Serializable
data class RequesterDto(
    val name: String? = null,
    /** Personal data. See the privacy note in docs/FEATURE_REQUEST_SCHEMA.md. */
    val email: String? = null,
    val contactOptIn: Boolean = false,
)

@Serializable
enum class RequestType {
    @SerialName("feature")
    FEATURE,

    @SerialName("bug")
    BUG,

    @SerialName("question")
    QUESTION,

    @SerialName("other")
    OTHER,
}

@Serializable
enum class PriorityHint {
    @SerialName("low")
    LOW,

    @SerialName("normal")
    NORMAL,

    @SerialName("high")
    HIGH,
}
