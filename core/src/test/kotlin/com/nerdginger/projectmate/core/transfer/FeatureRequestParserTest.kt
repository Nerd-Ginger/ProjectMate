package com.nerdginger.projectmate.core.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeatureRequestParserTest {

    private fun envelope(requests: String) = """
        {
          "schemaVersion": 1,
          "kind": "projectmate.featureRequests",
          "generatedAt": "2026-08-03T14:22:00Z",
          "source": { "type": "web-portal", "siteId": "nerdginger.com" },
          "requests": [$requests]
        }
    """.trimIndent()

    private val fullRequest = """
        {
          "id": "fr_01",
          "projectSlug": "projectmate",
          "title": "Dark mode for the kanban board",
          "body": "Long-form description.",
          "type": "feature",
          "priorityHint": "high",
          "submittedAt": "2026-08-01T10:04:11Z",
          "updatedAt": "2026-08-02T09:00:00Z",
          "status": "new",
          "votes": 3,
          "tags": ["ui", "android"],
          "requester": { "name": "Sam", "email": "sam@example.com", "contactOptIn": true },
          "url": "https://nerdginger.com/requests/fr_01"
        }
    """.trimIndent()

    private fun parseSuccess(raw: String): ParsedPayload {
        val outcome = FeatureRequestParser.parse(raw)
        assertIs<ParseOutcome.Success>(outcome, "expected success, got $outcome")
        return outcome.payload
    }

    private fun parseFailure(raw: String): ParseOutcome.Failure {
        val outcome = FeatureRequestParser.parse(raw)
        assertIs<ParseOutcome.Failure>(outcome, "expected failure, got $outcome")
        return outcome
    }

    // -------------------------------------------------------------- happy path

    @Test
    fun `reads a complete request`() {
        val payload = parseSuccess(envelope(fullRequest))
        val request = payload.valid.single()

        assertEquals("fr_01", request.id)
        assertEquals("projectmate", request.projectSlug)
        assertEquals("Dark mode for the kanban board", request.title)
        assertEquals(RequestType.FEATURE, request.type)
        assertEquals(PriorityHint.HIGH, request.priorityHint)
        assertEquals(3, request.votes)
        assertEquals(listOf("ui", "android"), request.tags)
        assertEquals("Sam", request.requester?.name)
        assertTrue(request.requester?.contactOptIn == true)
        assertTrue(payload.rejected.isEmpty())
    }

    @Test
    fun `reads envelope metadata including the pagination cursor`() {
        val raw = """
            {
              "schemaVersion": 1,
              "kind": "projectmate.featureRequests",
              "generatedAt": "2026-08-03T14:22:00Z",
              "cursor": { "since": "2026-07-27T00:00:00Z", "next": "eyJvZmZzZXQiOjUwfQ" },
              "requests": []
            }
        """.trimIndent()

        val payload = parseSuccess(raw)

        assertEquals("eyJvZmZzZXQiOjUwfQ", payload.envelope.cursor?.next)
        assertTrue(payload.valid.isEmpty())
    }

    @Test
    fun `applies documented defaults for optional fields`() {
        val minimal = """
            {
              "id": "fr_min",
              "projectSlug": "projectmate",
              "title": "Minimal",
              "submittedAt": "2026-08-01T10:00:00Z"
            }
        """.trimIndent()

        val request = parseSuccess(envelope(minimal)).valid.single()

        assertEquals(RequestType.FEATURE, request.type)
        assertEquals(PriorityHint.NORMAL, request.priorityHint)
        assertEquals(0, request.votes)
        assertTrue(request.tags.isEmpty())
        assertNull(request.requester)
        assertNull(request.body)
    }

    @Test
    fun `an empty request list is valid, not an error`() {
        // The Worker returns this whenever nothing is new since the cursor.
        val payload = parseSuccess(envelope(""))

        assertTrue(payload.valid.isEmpty())
        assertTrue(payload.rejected.isEmpty())
    }

    @Test
    fun `unknown fields are ignored so the worker can add them freely`() {
        val withExtras = """
            {
              "id": "fr_x", "projectSlug": "p", "title": "T",
              "submittedAt": "2026-08-01T10:00:00Z",
              "meta": { "turnstileVerified": true, "countryCode": "US" },
              "somethingInventedNextYear": { "nested": [1, 2, 3] }
            }
        """.trimIndent()

        assertEquals("fr_x", parseSuccess(envelope(withExtras)).valid.single().id)
    }

    // ------------------------------------------------------- per-row rejection

    @Test
    fun `one bad request does not lose the good ones`() {
        // The property that matters most: a 200-row export with one broken row
        // should import 199 rows, not zero.
        val broken = """{ "projectSlug": "p", "title": "No id", "submittedAt": "2026-08-01T10:00:00Z" }"""
        val good = """
            { "id": "fr_ok", "projectSlug": "p", "title": "Fine", "submittedAt": "2026-08-01T10:00:00Z" }
        """.trimIndent()

        val payload = parseSuccess(envelope("$broken, $good"))

        assertEquals(listOf("fr_ok"), payload.valid.map { it.id })
        assertEquals(1, payload.rejected.size)
        assertEquals(0, payload.rejected.single().index)
    }

    @Test
    fun `rejects requests missing a title`() {
        val noTitle = """
            { "id": "fr_1", "projectSlug": "p", "title": "", "submittedAt": "2026-08-01T10:00:00Z" }
        """.trimIndent()

        val payload = parseSuccess(envelope(noTitle))

        assertEquals("fr_1", payload.rejected.single().id)
        assertTrue(payload.rejected.single().reason.contains("title", ignoreCase = true))
    }

    @Test
    fun `rejects a duplicate id inside one payload`() {
        val one = """{ "id": "dup", "projectSlug": "p", "title": "A", "submittedAt": "2026-08-01T10:00:00Z" }"""
        val two = """{ "id": "dup", "projectSlug": "p", "title": "B", "submittedAt": "2026-08-01T10:00:00Z" }"""

        val payload = parseSuccess(envelope("$one, $two"))

        assertEquals(1, payload.valid.size)
        assertTrue(payload.rejected.single().reason.contains("Duplicate", ignoreCase = true))
    }

    @Test
    fun `rejects a request whose timestamp is not iso-8601`() {
        val badDate = """
            { "id": "fr_1", "projectSlug": "p", "title": "T", "submittedAt": "last Tuesday" }
        """.trimIndent()

        val payload = parseSuccess(envelope(badDate))

        assertTrue(payload.valid.isEmpty())
        assertTrue(payload.rejected.single().reason.contains("ISO-8601"))
    }

    @Test
    fun `rejections carry their position so the preview can point at them`() {
        val first = """{ "id": "a", "projectSlug": "p", "title": "A", "submittedAt": "2026-08-01T10:00:00Z" }"""
        val bad = """{ "projectSlug": "p", "title": "B", "submittedAt": "2026-08-01T10:00:00Z" }"""
        val third = """{ "id": "c", "projectSlug": "p", "title": "C", "submittedAt": "2026-08-01T10:00:00Z" }"""

        val payload = parseSuccess(envelope("$first, $bad, $third"))

        assertEquals(listOf("a", "c"), payload.valid.map { it.id })
        assertEquals(1, payload.rejected.single().index)
    }

    // ------------------------------------------------------- whole-payload failures

    @Test
    fun `refuses a newer schema version rather than dropping fields silently`() {
        val future = envelope(fullRequest).replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")

        val failure = parseFailure(future)

        assertEquals(ParseFailure.UNSUPPORTED_VERSION, failure.reason)
        assertTrue(failure.message.contains("update the app", ignoreCase = true))
    }

    @Test
    fun `an older schema version is still accepted`() {
        val older = envelope(fullRequest).replace("\"schemaVersion\": 1", "\"schemaVersion\": 0")

        assertEquals(1, parseSuccess(older).valid.size)
    }

    @Test
    fun `refuses the wrong kind of export`() {
        val backup = envelope(fullRequest)
            .replace("projectmate.featureRequests", "projectmate.backup")

        assertEquals(ParseFailure.WRONG_KIND, parseFailure(backup).reason)
    }

    @Test
    fun `refuses malformed json`() {
        assertEquals(ParseFailure.MALFORMED_JSON, parseFailure("{ not json at all").reason)
        assertEquals(ParseFailure.MALFORMED_JSON, parseFailure("").reason)
    }

    @Test
    fun `refuses a payload that is not an object`() {
        assertEquals(ParseFailure.NOT_AN_OBJECT, parseFailure("[1, 2, 3]").reason)
    }

    @Test
    fun `refuses a payload with no schema version`() {
        val raw = """{ "kind": "projectmate.featureRequests", "requests": [] }"""

        assertEquals(ParseFailure.MISSING_SCHEMA_VERSION, parseFailure(raw).reason)
    }

    @Test
    fun `refuses an oversized payload instead of exhausting memory`() {
        val huge = envelope("") + " ".repeat(FeatureRequestParser.MAX_PAYLOAD_BYTES)

        assertEquals(ParseFailure.TOO_LARGE, parseFailure(huge).reason)
    }

    // ------------------------------------------------------------- timestamps

    @Test
    fun `accepts both utc and explicit offsets`() {
        val utc = FeatureRequestParser.parseTimestamp("2026-08-01T10:00:00Z")
        val offset = FeatureRequestParser.parseTimestamp("2026-08-01T11:00:00+01:00")

        assertEquals(utc, offset, "the same instant expressed two ways should agree")
    }

    @Test
    fun `returns null for unusable timestamps rather than throwing`() {
        assertNull(FeatureRequestParser.parseTimestamp(null))
        assertNull(FeatureRequestParser.parseTimestamp(""))
        assertNull(FeatureRequestParser.parseTimestamp("2026-08-01"))
        assertNull(FeatureRequestParser.parseTimestamp("1754042651000"))
    }
}
