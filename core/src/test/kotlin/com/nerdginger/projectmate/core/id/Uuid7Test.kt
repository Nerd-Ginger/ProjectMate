package com.nerdginger.projectmate.core.id

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Uuid7Test {

    @Test
    fun `generates well-formed version 7 uuids`() {
        repeat(200) {
            val id = Uuid7.generate()
            val parsed = UUID.fromString(id)

            assertEquals(7, parsed.version(), "expected a v7 UUID, got $id")
            assertEquals(2, parsed.variant(), "expected the RFC 4122 variant, got $id")
            assertEquals(36, id.length)
        }
    }

    @Test
    fun `ids are unique`() {
        val ids = List(10_000) { Uuid7.generate() }

        assertEquals(10_000, ids.toSet().size)
    }

    /**
     * The point of v7 over v4. Ids minted in a tight loop land in the same
     * millisecond, so this only holds because the counter field is monotonic.
     */
    @Test
    fun `ids sort in creation order even within a single millisecond`() {
        val ids = List(5_000) { Uuid7.generate() }

        assertEquals(ids, ids.sorted(), "v7 ids should already be in ascending order")
    }

    @Test
    fun `the embedded timestamp is the creation time`() {
        val before = System.currentTimeMillis()
        val id = Uuid7.generate()
        val after = System.currentTimeMillis()

        val timestamp = Uuid7.timestampOf(id)

        assertTrue(
            timestamp in (before - 1)..(after + 1),
            "timestamp $timestamp should fall within $before..$after",
        )
    }

    @Test
    fun `validates its own output and rejects other uuids`() {
        assertTrue(Uuid7.isValid(Uuid7.generate()))
        assertFalse(Uuid7.isValid(UUID.randomUUID().toString()), "a v4 UUID is not a v7 UUID")
        assertFalse(Uuid7.isValid("not-a-uuid"))
        assertFalse(Uuid7.isValid(""))
    }

    @Test
    fun `ids from later moments sort after earlier ones`() {
        val first = Uuid7.generate()
        Thread.sleep(2)
        val second = Uuid7.generate()

        assertTrue(first < second, "$first should sort before $second")
        assertTrue(Uuid7.timestampOf(first) < Uuid7.timestampOf(second))
    }
}
