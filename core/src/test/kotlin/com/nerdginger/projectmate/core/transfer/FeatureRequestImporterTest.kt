package com.nerdginger.projectmate.core.transfer

import com.nerdginger.projectmate.core.model.ItemType
import com.nerdginger.projectmate.core.model.Origin
import com.nerdginger.projectmate.core.model.Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeatureRequestImporterTest {

    private val boardId = "board-feature-requests"
    private val statusId = "status-triage"
    private val now = 1_780_000_000_000L

    private fun payloadOf(vararg requests: String): ParsedPayload {
        val raw = """
            {
              "schemaVersion": 1,
              "kind": "projectmate.featureRequests",
              "generatedAt": "2026-08-03T14:22:00Z",
              "requests": [${requests.joinToString(",")}]
            }
        """.trimIndent()
        val outcome = FeatureRequestParser.parse(raw)
        assertIs<ParseOutcome.Success>(outcome)
        return outcome.payload
    }

    private fun request(
        id: String,
        title: String = "Request $id",
        updatedAt: String? = null,
        votes: Int = 0,
        tags: String = "[]",
        priority: String = "normal",
    ): String {
        val updated = updatedAt?.let { "\"updatedAt\": \"$it\"," } ?: ""
        return """
            {
              "id": "$id",
              "projectSlug": "projectmate",
              "title": "$title",
              "body": "Body of $id",
              "priorityHint": "$priority",
              "submittedAt": "2026-08-01T10:00:00Z",
              $updated
              "votes": $votes,
              "tags": $tags,
              "requester": { "name": "Sam", "email": "sam@example.com", "contactOptIn": true }
            }
        """.trimIndent()
    }

    private fun plan(
        payload: ParsedPayload,
        existing: List<ExistingRequest> = emptyList(),
        lastSortKey: String? = null,
    ) = FeatureRequestImporter.plan(
        payload = payload,
        existing = existing,
        boardId = boardId,
        statusId = statusId,
        lastSortKey = lastSortKey,
        now = now,
    )

    // ------------------------------------------------------------------ create

    @Test
    fun `new requests become feature request items on the target board`() {
        val result = plan(payloadOf(request("fr_1")))

        val prepared = result.toCreate.single()
        assertEquals(boardId, prepared.item.boardId)
        assertEquals(statusId, prepared.item.statusId)
        assertEquals(ItemType.FEATURE_REQUEST, prepared.item.itemType)
        assertEquals(Origin.WEB_PORTAL, prepared.item.sync.origin)
        assertEquals("fr_1", prepared.item.externalRequestId)
        assertEquals("Request fr_1", prepared.item.title)
        assertEquals("Body of fr_1", prepared.item.notes)
    }

    @Test
    fun `requester details and the verbatim payload are kept alongside the item`() {
        val prepared = plan(payloadOf(request("fr_1", votes = 7))).toCreate.single()

        assertEquals(prepared.item.id, prepared.meta.itemId)
        assertEquals("Sam", prepared.meta.requesterName)
        assertEquals("sam@example.com", prepared.meta.requesterEmail)
        assertTrue(prepared.meta.contactOptIn)
        assertEquals(7, prepared.meta.votes)
        assertEquals("projectmate", prepared.meta.portalSlug)

        // Verbatim storage is what protects fields this version doesn't model.
        assertTrue(prepared.meta.rawPayloadJson.contains("\"id\":\"fr_1\""))
    }

    @Test
    fun `createdAt is when the request was submitted, not when it was imported`() {
        val prepared = plan(payloadOf(request("fr_1"))).toCreate.single()
        val submitted = FeatureRequestParser.parseTimestamp("2026-08-01T10:00:00Z")

        assertEquals(submitted, prepared.item.sync.createdAt)
        assertEquals(now, prepared.item.sync.updatedAt)
    }

    @Test
    fun `priority hints map onto the app's own priorities`() {
        val result = plan(
            payloadOf(
                request("low", priority = "low"),
                request("normal", priority = "normal"),
                request("high", priority = "high"),
            ),
        )

        val byId = result.toCreate.associateBy { it.dto.id }
        assertEquals(Priority.LOW, byId.getValue("low").item.priority)
        assertEquals(Priority.NORMAL, byId.getValue("normal").item.priority)
        assertEquals(Priority.URGENT, byId.getValue("high").item.priority)
    }

    @Test
    fun `imported requests keep the order the export listed them in`() {
        val result = plan(payloadOf(request("a"), request("b"), request("c")))

        val keys = result.toCreate.map { it.item.sortKey }
        assertEquals(keys.sorted(), keys, "sort keys should ascend with export order")
        assertEquals(3, keys.toSet().size, "sort keys must be distinct")
    }

    @Test
    fun `imported requests sort after work already on the board`() {
        val result = plan(payloadOf(request("a")), lastSortKey = "a0")

        assertTrue(result.toCreate.single().item.sortKey > "a0")
    }

    @Test
    fun `tags are normalised so they match existing ones case-insensitively`() {
        val prepared = plan(
            payloadOf(request("fr_1", tags = """["UI", "ui", " Android ", ""]""")),
        ).toCreate.single()

        assertEquals(listOf("ui", "android"), prepared.tagNames)
    }

    @Test
    fun `each created item gets a distinct id`() {
        val result = plan(payloadOf(request("a"), request("b"), request("c")))

        assertEquals(3, result.toCreate.map { it.item.id }.toSet().size)
    }

    // ------------------------------------------------------------------- dedup

    @Test
    fun `re-importing the same payload does nothing`() {
        // The property the unique index on externalRequestId exists to protect.
        val payload = payloadOf(request("fr_1"))
        val existing = listOf(ExistingRequest("item-1", "fr_1", importedUpdatedAt = null))

        val result = plan(payload, existing)

        assertTrue(result.toCreate.isEmpty())
        assertTrue(result.toUpdate.isEmpty())
        assertEquals(listOf("fr_1"), result.unchanged.map { it.id })
        assertTrue(result.isEmpty)
    }

    @Test
    fun `a newer updatedAt refreshes the request`() {
        val payload = payloadOf(request("fr_1", updatedAt = "2026-08-05T09:00:00Z", votes = 12))
        val older = FeatureRequestParser.parseTimestamp("2026-08-02T09:00:00Z")
        val existing = listOf(ExistingRequest("item-1", "fr_1", importedUpdatedAt = older))

        val result = plan(payload, existing)

        val update = result.toUpdate.single()
        assertEquals("item-1", update.itemId)
        assertEquals(12, update.votes)
        assertTrue(result.toCreate.isEmpty())
    }

    @Test
    fun `an older updatedAt is ignored`() {
        val payload = payloadOf(request("fr_1", updatedAt = "2026-08-01T09:00:00Z"))
        val newer = FeatureRequestParser.parseTimestamp("2026-08-10T09:00:00Z")
        val existing = listOf(ExistingRequest("item-1", "fr_1", importedUpdatedAt = newer))

        val result = plan(payload, existing)

        assertTrue(result.toUpdate.isEmpty())
        assertEquals(1, result.unchanged.size)
    }

    /**
     * The portal supplies requests; it does not own your triage. A re-import
     * that silently reverted a retitle or a status change would make the app
     * untrustworthy.
     */
    @Test
    fun `an update produces no item, so local title and status edits survive`() {
        // The portal supplies requests; it does not own your triage. A
        // re-import that silently reverted a retitle or a status change would
        // make the app untrustworthy, so updates carry portal-owned metadata
        // only — there is no Item on the update path at all.
        val payload = payloadOf(request("fr_1", title = "Renamed upstream", updatedAt = "2026-08-05T09:00:00Z"))
        val existing = listOf(ExistingRequest("item-1", "fr_1", importedUpdatedAt = 0L))

        val result = plan(payload, existing)

        assertTrue(result.toCreate.isEmpty(), "an existing request must not be recreated")

        val update = result.toUpdate.single()
        assertEquals("item-1", update.itemId)
        assertNotNull(update.rawPayloadJson)
        // The new title is reachable for display in the preview, but nothing in
        // the update is applied to the item's own title or status.
        assertEquals("Renamed upstream", update.dto.title)
    }

    @Test
    fun `mixed payloads split into create, update and unchanged`() {
        val payload = payloadOf(
            request("brand_new"),
            request("refreshed", updatedAt = "2026-08-09T09:00:00Z"),
            request("stale", updatedAt = "2026-08-01T09:00:00Z"),
        )
        val existing = listOf(
            ExistingRequest("item-r", "refreshed", importedUpdatedAt = 0L),
            ExistingRequest("item-s", "stale", importedUpdatedAt = Long.MAX_VALUE),
        )

        val result = plan(payload, existing)

        assertEquals(listOf("brand_new"), result.toCreate.map { it.dto.id })
        assertEquals(listOf("refreshed"), result.toUpdate.map { it.dto.id })
        assertEquals(listOf("stale"), result.unchanged.map { it.id })
    }

    @Test
    fun `rejections from parsing survive into the plan for the preview`() {
        val raw = """
            {
              "schemaVersion": 1,
              "kind": "projectmate.featureRequests",
              "requests": [
                { "projectSlug": "p", "title": "No id", "submittedAt": "2026-08-01T10:00:00Z" },
                ${request("fr_ok")}
              ]
            }
        """.trimIndent()
        val outcome = FeatureRequestParser.parse(raw)
        assertIs<ParseOutcome.Success>(outcome)

        val result = plan(outcome.payload)

        assertEquals(1, result.toCreate.size)
        assertEquals(1, result.rejected.size)
        assertEquals(2, result.totalConsidered)
    }
}
