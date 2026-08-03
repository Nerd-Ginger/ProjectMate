package com.nerdginger.projectmate.core.sort

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SortKeyTest {

    @Test
    fun `first key of an empty list`() {
        assertEquals("a0", SortKey.between(null, null))
    }

    @Test
    fun `appending produces ascending keys`() {
        val keys = mutableListOf<String>()
        var last: String? = null
        repeat(500) {
            val next = SortKey.append(last)
            keys.add(next)
            last = next
        }
        assertStrictlyAscending(keys)
    }

    @Test
    fun `appending stays short instead of growing per item`() {
        // The integer-part length header is the reason for this: naive schemes
        // grow one character per append.
        var last: String? = null
        repeat(1_000) { last = SortKey.append(last) }
        assertTrue(last!!.length <= 4, "append key grew to '${last}' (${last!!.length} chars)")
    }

    @Test
    fun `prepending produces descending keys`() {
        val keys = mutableListOf<String>()
        var first: String? = null
        repeat(500) {
            val next = SortKey.between(null, first)
            keys.add(next)
            first = next
        }
        assertStrictlyAscending(keys.reversed())
    }

    @Test
    fun `key between two neighbours sorts strictly between them`() {
        val a = SortKey.between(null, null)
        val b = SortKey.append(a)
        val mid = SortKey.between(a, b)

        assertTrue(a < mid, "$a should sort before $mid")
        assertTrue(mid < b, "$mid should sort before $b")
    }

    /**
     * The case that kills `Double` positions: always inserting into the same
     * gap. Doubles run out of precision after roughly fifty of these.
     */
    @Test
    fun `a thousand inserts into the same gap stay ordered and distinct`() {
        val low = SortKey.between(null, null)
        val high = SortKey.append(low)

        var upper = high
        val inserted = mutableListOf<String>()
        repeat(1_000) {
            val next = SortKey.between(low, upper)
            assertTrue(low < next, "$next should sort after $low")
            assertTrue(next < upper, "$next should sort before $upper")
            inserted.add(next)
            upper = next
        }

        assertEquals(1_000, inserted.toSet().size, "keys must all be distinct")
        assertStrictlyAscending(inserted.reversed())
    }

    @Test
    fun `interleaving inserts from both ends stays ordered`() {
        val ordered = mutableListOf(SortKey.between(null, null))
        repeat(200) { i ->
            val at = (i * 7) % (ordered.size + 1)
            val before = ordered.getOrNull(at - 1)
            val after = ordered.getOrNull(at)
            ordered.add(at, SortKey.between(before, after))
        }
        assertStrictlyAscending(ordered)
    }

    @Test
    fun `sequence returns ascending keys inside the bounds`() {
        val low = SortKey.between(null, null)
        val high = SortKey.append(low)

        val keys = SortKey.sequence(low, high, 25)

        assertEquals(25, keys.size)
        assertStrictlyAscending(keys)
        assertTrue(low < keys.first())
        assertTrue(keys.last() < high)
    }

    @Test
    fun `sequence handles open ends and zero count`() {
        assertEquals(emptyList(), SortKey.sequence(null, null, 0))
        assertStrictlyAscending(SortKey.sequence(null, null, 10))
        assertStrictlyAscending(SortKey.sequence(null, SortKey.between(null, null), 5))
    }

    @Test
    fun `generated keys are always valid`() {
        var last: String? = null
        repeat(200) {
            val next = SortKey.append(last)
            assertTrue(SortKey.isValid(next), "'$next' should be a valid key")
            last = next
        }
    }

    @Test
    fun `out of order bounds are rejected`() {
        val a = SortKey.between(null, null)
        val b = SortKey.append(a)

        assertFailsWith<IllegalArgumentException> { SortKey.between(b, a) }
        assertFailsWith<IllegalArgumentException> { SortKey.between(a, a) }
    }

    @Test
    fun `malformed keys are rejected`() {
        assertFalse(SortKey.isValid(""))
        // Trailing zero: a second spelling of the same position.
        assertFalse(SortKey.isValid("a0"

            .plus("10")))
        // Header claims a longer integer part than the key has.
        assertFalse(SortKey.isValid("b0"))
        // Not a base-62 character.
        assertFalse(SortKey.isValid("a-"))
    }

    @Test
    fun `keys order the same way SQLite compares them`() {
        // Byte order and digit order must agree, or ORDER BY sortKey is wrong.
        val keys = buildList {
            var last: String? = null
            repeat(300) {
                last = SortKey.append(last)
                add(last!!)
            }
        }

        val byBytes = keys.sortedWith { l, r ->
            val a = l.toByteArray(Charsets.US_ASCII)
            val b = r.toByteArray(Charsets.US_ASCII)
            var i = 0
            while (i < minOf(a.size, b.size)) {
                val cmp = (a[i].toInt() and 0xFF).compareTo(b[i].toInt() and 0xFF)
                if (cmp != 0) return@sortedWith cmp
                i++
            }
            a.size.compareTo(b.size)
        }

        assertEquals(keys, byBytes)
    }

    private fun assertStrictlyAscending(keys: List<String>) {
        keys.zipWithNext { a, b ->
            assertTrue(a < b, "expected '$a' < '$b' in $keys")
        }
    }
}
