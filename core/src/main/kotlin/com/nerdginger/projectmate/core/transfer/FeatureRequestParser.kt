package com.nerdginger.projectmate.core.transfer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** A single request that couldn't be read, and why. */
data class Rejection(
    /** Position in the `requests` array, for pointing at it in the UI. */
    val index: Int,
    val id: String?,
    val reason: String,
)

/** Everything readable in a payload, plus everything that wasn't. */
data class ParsedPayload(
    val envelope: PortalEnvelope,
    val valid: List<FeatureRequestDto>,
    val rejected: List<Rejection>,
)

/** Why a payload couldn't be read at all. */
enum class ParseFailure {
    MALFORMED_JSON,
    NOT_AN_OBJECT,
    MISSING_SCHEMA_VERSION,

    /** Produced by a newer app than this one. */
    UNSUPPORTED_VERSION,
    WRONG_KIND,
    TOO_LARGE,
}

sealed interface ParseOutcome {
    data class Success(val payload: ParsedPayload) : ParseOutcome

    data class Failure(val reason: ParseFailure, val message: String) : ParseOutcome
}

/**
 * Reads a feature-request payload, from wherever it came from.
 *
 * Share intent, file open, deep link and the future Worker pull all hand a
 * `String` to this one function — the parser neither knows nor cares which.
 *
 * **A bad row never kills the batch.** Requests are decoded individually and
 * failures are collected as [Rejection]s rather than thrown, so one malformed
 * entry in a 200-request export doesn't lose the other 199. The import preview
 * shows what was rejected and why.
 */
object FeatureRequestParser {

    /** Payloads above this are refused rather than allowed to exhaust memory. */
    const val MAX_PAYLOAD_BYTES: Int = 5 * 1024 * 1024

    private val json = Json {
        ignoreUnknownKeys = true // the Worker may add fields without an app update
        explicitNulls = false
        coerceInputValues = true // an unknown enum value falls back to its default
    }

    fun parse(raw: String): ParseOutcome {
        if (raw.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES) {
            return ParseOutcome.Failure(
                ParseFailure.TOO_LARGE,
                "Payload is larger than ${MAX_PAYLOAD_BYTES / 1024 / 1024} MB.",
            )
        }

        val root = try {
            json.parseToJsonElement(raw)
        } catch (e: Exception) {
            return ParseOutcome.Failure(
                ParseFailure.MALFORMED_JSON,
                e.message ?: "The file isn't valid JSON.",
            )
        }

        if (root !is JsonObject) {
            return ParseOutcome.Failure(
                ParseFailure.NOT_AN_OBJECT,
                "Expected a JSON object at the top level.",
            )
        }

        val version = root["schemaVersion"]?.jsonPrimitive?.intOrNull
            ?: return ParseOutcome.Failure(
                ParseFailure.MISSING_SCHEMA_VERSION,
                "No schemaVersion — this doesn't look like a ProjectMate export.",
            )

        // Refuse loudly rather than silently dropping fields we can't see.
        if (version > SCHEMA_VERSION) {
            return ParseOutcome.Failure(
                ParseFailure.UNSUPPORTED_VERSION,
                "This export uses schema version $version. " +
                    "This version of ProjectMate understands up to $SCHEMA_VERSION — update the app.",
            )
        }

        val kind = root["kind"]?.jsonPrimitive?.contentOrNull
        if (kind != KIND_FEATURE_REQUESTS) {
            return ParseOutcome.Failure(
                ParseFailure.WRONG_KIND,
                "Expected '$KIND_FEATURE_REQUESTS' but found '${kind ?: "nothing"}'.",
            )
        }

        val envelope = PortalEnvelope(
            schemaVersion = version,
            kind = kind,
            generatedAt = root["generatedAt"]?.jsonPrimitive?.contentOrNull,
            source = root["source"]?.let {
                runCatching { json.decodeFromJsonElement(PortalSource.serializer(), it) }.getOrNull()
            },
            cursor = root["cursor"]?.let {
                runCatching { json.decodeFromJsonElement(PortalCursor.serializer(), it) }.getOrNull()
            },
        )

        val rawRequests = root["requests"]
        if (rawRequests == null) {
            return ParseOutcome.Success(ParsedPayload(envelope, emptyList(), emptyList()))
        }

        val elements = runCatching { rawRequests.jsonArray }.getOrElse {
            return ParseOutcome.Failure(
                ParseFailure.NOT_AN_OBJECT,
                "'requests' must be an array.",
            )
        }

        val valid = mutableListOf<FeatureRequestDto>()
        val rejected = mutableListOf<Rejection>()
        val seenIds = mutableSetOf<String>()

        elements.forEachIndexed { index, element ->
            val fallbackId = (element as? JsonObject)
                ?.get("id")?.jsonPrimitive?.contentOrNull

            val decoded = try {
                json.decodeFromJsonElement(FeatureRequestDto.serializer(), element)
            } catch (e: Exception) {
                rejected += Rejection(index, fallbackId, e.readableMessage())
                return@forEachIndexed
            }

            when {
                decoded.id.isBlank() ->
                    rejected += Rejection(index, null, "Missing id — imports can't be deduplicated without it.")

                decoded.title.isBlank() ->
                    rejected += Rejection(index, decoded.id, "Missing title.")

                !seenIds.add(decoded.id) ->
                    rejected += Rejection(index, decoded.id, "Duplicate id within this payload.")

                parseTimestamp(decoded.submittedAt) == null ->
                    rejected += Rejection(
                        index,
                        decoded.id,
                        "submittedAt '${decoded.submittedAt}' isn't an ISO-8601 timestamp.",
                    )

                else -> valid += decoded
            }
        }

        return ParseOutcome.Success(ParsedPayload(envelope, valid, rejected))
    }

    /**
     * ISO-8601 with an explicit offset, per the contract. Accepts both `Z` and
     * `+01:00`; returns null for anything else rather than throwing.
     */
    fun parseTimestamp(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(value).toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun Exception.readableMessage(): String =
        message?.substringBefore('\n')?.take(200) ?: "Could not be read."
}
