package com.nerdginger.projectmate.core.id

import java.security.SecureRandom
import java.util.UUID

/**
 * UUIDv7 — a time-ordered UUID (RFC 9562).
 *
 * Every record in ProjectMate gets one of these, generated on-device. The
 * reason for client-side IDs is sync: autoincrement integers collide across
 * devices and would force a migration rewriting every foreign key the day a
 * server appears. See docs/DECISIONS.md D-003.
 *
 * Version 7 rather than 4 because the first 48 bits are a millisecond
 * timestamp, so keys generated over time sort in creation order and cluster in
 * the index instead of scattering across it.
 *
 * ```
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    unix_ts_ms (48 bits)                       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  ver (4)  |   counter / rand_a (12)   | var (2) | rand_b (62) |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * ```
 *
 * Within a single millisecond the 12-bit `rand_a` field is used as a monotonic
 * counter, so IDs minted in a tight loop still sort in creation order rather
 * than arbitrarily. If that counter overflows — more than 4096 IDs in one
 * millisecond — generation borrows from the next millisecond instead of
 * breaking ordering.
 */
object Uuid7 {

    private const val COUNTER_BITS = 12
    private const val MAX_COUNTER = (1 shl COUNTER_BITS) - 1
    private const val VERSION_7 = 0x7000L

    /** Top two bits of the low half must be `0b10`. */
    private const val VARIANT_RFC = Long.MIN_VALUE // bit 63 set
    private const val VARIANT_CLEAR_MASK = 0x3FFF_FFFF_FFFF_FFFFL // clears bits 63 and 62

    private val random = SecureRandom()

    private var lastTimestamp = -1L
    private var counter = 0

    /** A new UUIDv7 as a canonical 36-character string. */
    fun generate(): String = generateUuid().toString()

    @Synchronized
    fun generateUuid(): UUID {
        var timestamp = System.currentTimeMillis()

        when {
            timestamp > lastTimestamp -> {
                lastTimestamp = timestamp
                // Start somewhere random in the lower half so IDs minted in
                // the same millisecond on different devices don't correlate,
                // while leaving headroom to count upwards.
                counter = random.nextInt(MAX_COUNTER / 2)
            }

            timestamp == lastTimestamp -> {
                counter++
                if (counter > MAX_COUNTER) {
                    // More than 4096 in one millisecond. Roll into the next
                    // one rather than emit an out-of-order ID.
                    lastTimestamp++
                    timestamp = lastTimestamp
                    counter = 0
                }
            }

            // Clock moved backwards (NTP correction, timezone-independent).
            // Keep issuing increasing IDs from the last timestamp we used.
            else -> {
                timestamp = lastTimestamp
                counter++
                if (counter > MAX_COUNTER) {
                    lastTimestamp++
                    timestamp = lastTimestamp
                    counter = 0
                }
            }
        }

        val mostSignificant =
            (timestamp and 0xFFFF_FFFF_FFFFL shl 16) or
                VERSION_7 or
                (counter.toLong() and MAX_COUNTER.toLong())

        val leastSignificant = (random.nextLong() and VARIANT_CLEAR_MASK) or VARIANT_RFC

        return UUID(mostSignificant, leastSignificant)
    }

    /** The millisecond timestamp encoded in a v7 UUID. */
    fun timestampOf(uuid: String): Long = timestampOf(UUID.fromString(uuid))

    fun timestampOf(uuid: UUID): Long {
        require(uuid.version() == 7) { "not a version 7 UUID: $uuid" }
        return uuid.mostSignificantBits ushr 16 and 0xFFFF_FFFF_FFFFL
    }

    fun isValid(value: String): Boolean =
        runCatching { UUID.fromString(value).version() == 7 }.getOrDefault(false)
}
